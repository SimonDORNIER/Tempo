package fr.tempo.sport;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CapacitorPlugin(name = "TempoUpdater")
public class TempoUpdaterPlugin extends Plugin {
    private static final String VERSION_URL =
            "https://raw.githubusercontent.com/SimonDORNIER/Tempo/main/release-version.txt";
    private static final String RELEASE_BASE =
            "https://github.com/SimonDORNIER/Tempo/releases/download/";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @PluginMethod
    public void checkLatest(PluginCall call) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL versionUrl = new URL(VERSION_URL + "?t=" + System.currentTimeMillis());
                connection = (HttpURLConnection) versionUrl.openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setUseCaches(false);
                connection.setRequestProperty("User-Agent", "Tempo-Android-Updater");
                connection.connect();

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("Version indisponible (" + code + ")");
                }

                String latest;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    latest = reader.readLine();
                }

                if (latest == null || latest.trim().isEmpty()) {
                    throw new IllegalStateException("Version GitHub vide");
                }

                latest = latest.trim();
                String apkUrl = RELEASE_BASE + "v" + latest + "/Tempo-" + latest + ".apk";

                JSObject result = new JSObject();
                result.put("latest", latest);
                result.put("url", apkUrl);
                call.resolve(result);
            } catch (Exception e) {
                call.reject("Impossible de vérifier la mise à jour", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @PluginMethod
    public void installFromUrl(PluginCall call) {
        String url = call.getString("url");
        String version = call.getString("version", "update");

        if (url == null || url.trim().isEmpty()) {
            call.reject("URL de mise à jour absente");
            return;
        }

        Context context = getContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !context.getPackageManager().canRequestPackageInstalls()) {
            Intent settingsIntent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName())
            );
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
            call.reject("Autorisation d'installation requise");
            return;
        }

        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = openWithRedirects(url);

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new IllegalStateException("Téléchargement impossible (" + code + ")");
                }

                File dir = new File(context.getCacheDir(), "updates");
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IllegalStateException("Dossier de mise à jour inaccessible");
                }

                String safeVersion = version == null
                        ? "update"
                        : version.replaceAll("[^0-9A-Za-z._-]", "_");
                File apk = new File(dir, "Tempo-" + safeVersion + ".apk");

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(apk)) {
                    byte[] buffer = new byte[16384];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                    output.flush();
                }

                Uri uri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".fileprovider",
                        apk
                );

                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                getActivity().runOnUiThread(() -> {
                    try {
                        context.startActivity(installIntent);
                        call.resolve();
                    } catch (Exception e) {
                        call.reject("Impossible d'ouvrir l'installateur Android", e);
                    }
                });
            } catch (Exception e) {
                call.reject("Mise à jour impossible", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private HttpURLConnection openWithRedirects(String initialUrl) throws Exception {
        String nextUrl = initialUrl;

        for (int i = 0; i < 6; i++) {
            HttpURLConnection connection =
                    (HttpURLConnection) new URL(nextUrl).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);
            connection.setRequestProperty(
                    "Accept",
                    "application/vnd.android.package-archive,application/octet-stream,*/*"
            );
            connection.setRequestProperty("User-Agent", "Tempo-Android-Updater");
            connection.connect();

            int code = connection.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.trim().isEmpty()) {
                    throw new IllegalStateException("Redirection de téléchargement invalide");
                }
                nextUrl = new URL(new URL(nextUrl), location).toString();
                continue;
            }

            return connection;
        }

        throw new IllegalStateException("Trop de redirections");
    }

    @Override
    protected void handleOnDestroy() {
        executor.shutdownNow();
        super.handleOnDestroy();
    }
}
