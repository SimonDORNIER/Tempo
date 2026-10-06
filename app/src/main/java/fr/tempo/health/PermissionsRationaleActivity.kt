package fr.tempo.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.tempo.health.ui.theme.TempoHealthTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TempoHealthTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Confidentialité Tempo Health",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "Tempo Health lit uniquement les données Santé Connect que tu autorises afin de calculer localement tes tendances de sommeil, récupération, activité et entraînement."
                    )
                    Text(
                        "Les données synchronisées sont stockées dans la base locale de l'application. Elles ne sont envoyées vers aucun serveur par cette version."
                    )
                }
            }
        }
    }
}
