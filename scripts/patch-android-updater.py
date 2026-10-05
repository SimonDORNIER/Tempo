from pathlib import Path
import shutil

root = Path("android/app/src/main")
java_dir = root / "java/fr/tempo/sport"
xml_dir = root / "res/xml"
java_dir.mkdir(parents=True, exist_ok=True)
xml_dir.mkdir(parents=True, exist_ok=True)

shutil.copyfile("native/android/MainActivity.java", java_dir / "MainActivity.java")
shutil.copyfile("native/android/TempoUpdaterPlugin.java", java_dir / "TempoUpdaterPlugin.java")
shutil.copyfile("native/android/file_paths.xml", xml_dir / "file_paths.xml")

manifest = root / "AndroidManifest.xml"
text = manifest.read_text(encoding="utf-8")

permission = '<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />'
if permission not in text:
    start = text.find("<manifest")
    end = text.find(">", start)
    if start < 0 or end < 0:
        raise RuntimeError("Balise manifest introuvable")
    text = text[:end + 1] + "\n    " + permission + text[end + 1:]

provider = '''        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
'''

if 'androidx.core.content.FileProvider' not in text:
    if '</application>' not in text:
        raise RuntimeError("Balise application introuvable")
    text = text.replace('</application>', provider + '    </application>')

manifest.write_text(text, encoding="utf-8")
