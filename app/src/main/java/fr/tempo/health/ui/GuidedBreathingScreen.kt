package fr.tempo.health.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import fr.tempo.health.ui.theme.TempoPanelCard
import fr.tempo.health.ui.theme.TempoColors as C
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.tempo.health.domain.BreathingGuides
import fr.tempo.health.domain.ExerciseLibrary
import kotlinx.coroutines.delay

@Composable
fun GuidedBreathingScreen(
    onExit: () -> Unit,
    onRecord: (String, Int, Boolean) -> Unit
) {
    val cream = C.text
    val ink = C.background
    val surface = C.surface
    val sage = C.greenGlow
    var selectedId by remember { mutableStateOf(BreathingGuides.all.first().exerciseId) }
    val guide = BreathingGuides.all.first { it.exerciseId == selectedId }
    var running by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var preparation by remember { mutableStateOf(5) }
    var elapsed by remember { mutableStateOf(0) }
    var confirmExit by remember { mutableStateOf(false) }

    fun requestExit() {
        if (running) confirmExit = true else onExit()
    }

    BackHandler(enabled = running) { confirmExit = true }

    LaunchedEffect(running, paused, selectedId) {
        while (running && !paused) {
            delay(1000)
            if (preparation > 0) {
                preparation--
            } else {
                elapsed = (elapsed + 1).coerceAtMost(guide.durationSeconds)
                if (elapsed >= guide.durationSeconds) {
                    running = false
                    finished = true
                    onRecord(guide.exerciseId, elapsed, true)
                }
            }
        }
    }

    val cycle = guide.inhaleSeconds + guide.exhaleSeconds
    val withinCycle = elapsed % cycle
    val inhale = withinCycle < guide.inhaleSeconds
    val phaseRemaining = if (inhale) {
        guide.inhaleSeconds - withinCycle
    } else {
        cycle - withinCycle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = ::requestExit) { Text("‹  Retour") }
        Text("Respiration guidée", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = cream)
        Text("Sans matériel • à ton rythme", color = C.textSecondary)

        if (!running && !finished) {
            BreathingGuides.all.forEach { option ->
                OutlinedButton(
                    onClick = { selectedId = option.exerciseId },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            (if (selectedId == option.exerciseId) "✓  " else "") + option.title,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            option.description + " • " + option.durationSeconds / 60 + " min" +
                                " • " + option.inhaleSeconds + "s / " + option.exhaleSeconds + "s",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        TempoPanelCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(guide.title, fontWeight = FontWeight.Bold, color = cream)
                Text(
                    when {
                        finished -> "Séance terminée"
                        !running -> "Prêt à commencer ?"
                        paused -> "En pause"
                        preparation > 0 -> "Prépare-toi"
                        inhale -> "Inspire doucement"
                        else -> "Expire lentement"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = cream
                )
                Text(
                    when {
                        finished -> "Terminé"
                        !running -> (guide.durationSeconds / 60).toString() + " min"
                        preparation > 0 -> preparation.toString() + " s"
                        else -> phaseRemaining.toString() + " s"
                    },
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = sage
                )
                LinearProgressIndicator(
                    progress = { elapsed.toFloat() / guide.durationSeconds },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Temps restant : " + ((guide.durationSeconds - elapsed) / 60) +
                        " min " + ((guide.durationSeconds - elapsed) % 60) + " s",
                    color = C.textSecondary
                )
                Text(
                    if (running) "Cycle " + (elapsed / cycle + 1) +
                        " • inspire " + guide.inhaleSeconds + " s, expire " +
                        guide.exhaleSeconds + " s"
                    else ExerciseLibrary.byId(guide.exerciseId).instructions,
                    color = C.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (finished) {
            Button(
                onClick = {
                    finished = false
                    elapsed = 0
                    preparation = 5
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cream, contentColor = ink)
            ) { Text("RECOMMENCER") }
            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("TERMINER")
            }
        } else if (!running) {
            Button(
                onClick = {
                    elapsed = 0
                    preparation = 5
                    paused = false
                    running = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cream, contentColor = ink)
            ) { Text("DÉMARRER") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { paused = !paused },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cream, contentColor = ink)
                ) { Text(if (paused) "REPRENDRE" else "PAUSE") }
                OutlinedButton(
                    onClick = { confirmExit = true },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("ARRÊTER") }
            }
        }

        Text(
            "Respire confortablement, sans forcer ni retenir ton souffle. " +
                "Si tu ressens un vertige, un malaise ou un essoufflement, arrête.",
            color = C.textSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Arrêter la respiration ?") },
            text = { Text("La séance en cours sera interrompue.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    running = false
                    onRecord(guide.exerciseId, elapsed, false)
                    onExit()
                }) { Text("ARRÊTER") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("CONTINUER") }
            }
        )
    }
}
