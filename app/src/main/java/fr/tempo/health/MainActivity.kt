package fr.tempo.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.tempo.health.ui.TempoHealthApp
import fr.tempo.health.ui.theme.TempoHealthTheme
import fr.tempo.health.update.AutoUpdater

class MainActivity : ComponentActivity() {
    private lateinit var autoUpdater: AutoUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        autoUpdater = AutoUpdater(this)

        setContent {
            TempoHealthTheme {
                TempoHealthApp(
                    onCheckForUpdates = autoUpdater::checkNow
                )
            }
        }

        autoUpdater.checkAtLaunch()
    }

    override fun onResume() {
        super.onResume()
        if (::autoUpdater.isInitialized) {
            autoUpdater.onResume()
        }
    }

    override fun onDestroy() {
        if (::autoUpdater.isInitialized) {
            autoUpdater.destroy()
        }
        super.onDestroy()
    }
}
