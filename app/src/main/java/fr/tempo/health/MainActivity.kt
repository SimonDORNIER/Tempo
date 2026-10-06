package fr.tempo.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.tempo.health.ui.TempoHealthApp
import fr.tempo.health.ui.theme.TempoHealthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TempoHealthTheme {
                TempoHealthApp()
            }
        }
    }
}
