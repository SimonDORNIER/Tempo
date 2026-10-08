package fr.tempo.health.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import fr.tempo.health.BuildConfig
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class HealthRelease(
    val version: String,
    val apkUrl: String
)

class AutoUpdater(
    private val activity: Activity
) {
    private val executor = Executors.newSingleThreadExecutor()
    private var pendingRelease: HealthRelease? = null
    private var downloading = false
    private var checking = false
    private var lastCheckEpochMs = 0L

    fun checkAtLaunch() {
        checkForUpdate(showFeedback = false, force = true)
    }

    fun checkNow() {
        checkForUpdate(showFeedback = true, force = true)
    }

    fun onResume() {
        val found = pendingRelease
        if (found != null && canInstallPackages() && !downloading) {
            pendingRelease = null
            downloadAndInstall(found)
            return
        }

        checkForUpdate(showFeedback = false, force = false)
    }

    fun destroy() {
        executor.shutdownNow()
    }

    private fun checkForUpdate(
        showFeedback: Boolean,
        force: Boolean
    ) {
        if (checking || downloading) return

        val now = System.currentTimeMillis()
        if (!force && now - lastCheckEpochMs < AUTO_CHECK_INTERVAL_MS) {
            return
        }

        checking = true
        lastCheckEpochMs = now

        executor.execute {
            runCatching { fetchLatestHealthRelease() }
                .onSuccess { found ->
                    activity.runOnUiThread {
                        checking = false

                        if (found == null) {
                            if (showFeedback) {
                                toast("Aucune release Tempo Health trouvée sur GitHub.")
                            }
                            return@runOnUiThread
                        }

                        val comparison =
                            compareVersions(found.version, BuildConfig.VERSION_NAME)

                        if (comparison > 0) {
                            pendingRelease = found
                            showUpdateDialog(found)
                        } else if (showFeedback) {
                            toast(
                                "Tempo Health est à jour : " +
                                    BuildConfig.VERSION_NAME +
                                    " • GitHub " + found.version
                            )
                        }
                    }
                }
                .onFailure { error ->
                    activity.runOnUiThread {
                        checking = false
                        if (showFeedback) {
                            toast(
                                "Vérification impossible : " +
                                    (error.message ?: "erreur inconnue")
                            )
                        }
                    }
                }
        }
    }

    private fun showUpdateDialog(found: HealthRelease) {
        if (activity.isFinishing || activity.isDestroyed) return

        AlertDialog.Builder(activity)
            .setTitle("Mise à jour disponible")
            .setMessage(
                "Tempo Health " + found.version +
                    " est disponible. Version installée : " +
                    BuildConfig.VERSION_NAME + "."
            )
            .setNegativeButton("Plus tard", null)
            .setPositiveButton("Mettre à jour") { _, _ ->
                if (canInstallPackages()) {
                    pendingRelease = null
                    downloadAndInstall(found)
                } else {
                    pendingRelease = found
                    requestInstallPermission()
                }
            }
            .show()
    }

    private fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            pendingRelease?.let {
                pendingRelease = null
                downloadAndInstall(it)
            }
            return
        }

        toast(
            "Autorise Tempo Health à installer ses mises à jour, puis reviens dans l'application."
        )

        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:" + activity.packageName)
        )
        activity.startActivity(intent)
    }

    private fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            activity.packageManager.canRequestPackageInstalls()

    private fun downloadAndInstall(found: HealthRelease) {
        if (downloading) return
        downloading = true

        toast("Téléchargement de Tempo Health " + found.version + "…")

        executor.execute {
            runCatching {
                val apk = downloadApk(found)
                validateApk(apk, found)
                apk
            }.onSuccess { apk ->
                activity.runOnUiThread {
                    downloading = false
                    openInstaller(apk)
                }
            }.onFailure { error ->
                activity.runOnUiThread {
                    downloading = false
                    toast(
                        "Mise à jour impossible : " +
                            (error.message ?: "erreur inconnue")
                    )
                }
            }
        }
    }

    private fun fetchLatestHealthRelease(): HealthRelease? {
        val connection = openConnection(
            "https://api.github.com/repos/SimonDORNIER/Tempo/releases?per_page=30"
        )

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val remaining = connection.getHeaderField("X-RateLimit-Remaining")
                throw IllegalStateException(
                    "GitHub HTTP " + code +
                        if (remaining != null) " • quota " + remaining else ""
                )
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val releases = JSONArray(json)
            val candidates = mutableListOf<HealthRelease>()

            for (index in 0 until releases.length()) {
                val releaseObject = releases.getJSONObject(index)
                if (releaseObject.optBoolean("draft", false)) continue
                if (releaseObject.optBoolean("prerelease", false)) continue

                val tag = releaseObject.optString("tag_name")
                if (!tag.startsWith("health-v")) continue

                val version = tag.removePrefix("health-v").trim()
                if (version.isBlank()) continue

                val expectedName = "TempoHealth-" + version + ".apk"
                val assets = releaseObject.optJSONArray("assets") ?: continue

                for (assetIndex in 0 until assets.length()) {
                    val asset = assets.getJSONObject(assetIndex)
                    if (asset.optString("name") == expectedName) {
                        val url = asset.optString("browser_download_url")
                        if (url.isNotBlank()) {
                            candidates += HealthRelease(
                                version = version,
                                apkUrl = url
                            )
                        }
                    }
                }
            }

            return candidates.maxWithOrNull { left, right ->
                compareVersions(left.version, right.version)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadApk(found: HealthRelease): File {
        val connection = openWithRedirects(found.apkUrl)

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("téléchargement HTTP " + code)
            }

            val dir = File(activity.cacheDir, "updates")
            if (!dir.exists() && !dir.mkdirs()) {
                throw IllegalStateException("dossier de mise à jour inaccessible")
            }

            val safeVersion = found.version.replace(
                Regex("[^0-9A-Za-z._-]"),
                "_"
            )
            val apk = File(dir, "TempoHealth-" + safeVersion + ".apk")

            connection.inputStream.use { input ->
                FileOutputStream(apk).use { output ->
                    copy(input, output)
                }
            }

            return apk
        } finally {
            connection.disconnect()
        }
    }

    private fun validateApk(apk: File, found: HealthRelease) {
        if (!apk.exists() || apk.length() < 1_000_000L) {
            throw IllegalStateException("APK téléchargé invalide")
        }

        val info = activity.packageManager.getPackageArchiveInfo(
            apk.absolutePath,
            0
        ) ?: throw IllegalStateException("APK non reconnu par Android")

        if (info.packageName != activity.packageName) {
            throw IllegalStateException("package APK inattendu")
        }

        val archiveVersion = info.versionName.orEmpty()
        if (archiveVersion != found.version) {
            throw IllegalStateException(
                "version APK inattendue (" + archiveVersion + ")"
            )
        }
    }

    private fun openInstaller(apk: File) {
        val uri = FileProvider.getUriForFile(
            activity,
            activity.packageName + ".fileprovider",
            apk
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        activity.startActivity(intent)
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "TempoHealth-Android-Updater")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connect()
        }

    private fun openWithRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl

        repeat(8) {
            val connection =
                (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    useCaches = false
                    setRequestProperty(
                        "Accept",
                        "application/vnd.android.package-archive,application/octet-stream,*/*"
                    )
                    setRequestProperty(
                        "User-Agent",
                        "TempoHealth-Android-Updater"
                    )
                    connect()
                }

            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()

                if (location.isNullOrBlank()) {
                    throw IllegalStateException("redirection GitHub invalide")
                }

                currentUrl = URL(URL(currentUrl), location).toString()
            } else {
                return connection
            }
        }

        throw IllegalStateException("trop de redirections")
    }

    private fun copy(input: InputStream, output: FileOutputStream) {
        val buffer = ByteArray(16_384)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
        }
        output.flush()
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.split(".").map { it.toIntOrNull() ?: 0 }
        val b = right.split(".").map { it.toIntOrNull() ?: 0 }
        val size = maxOf(a.size, b.size)

        for (index in 0 until size) {
            val av = a.getOrElse(index) { 0 }
            val bv = b.getOrElse(index) { 0 }
            if (av != bv) return av.compareTo(bv)
        }

        return 0
    }

    private fun toast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val AUTO_CHECK_INTERVAL_MS = 5L * 60L * 1000L
    }
}
