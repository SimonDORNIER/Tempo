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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.tempo.health.domain.BreathingGuides
import fr.tempo.health.domain.ExerciseLibrary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GuidedBreathingScreen(
    onExit: () -> Unit,
    onRecord: (String, Int, Boolean) -> Unit,
    onSoundSettingsChange: () -> Unit = {}
) {
    val cream = C.text
    val ink = C.background
    val surface = C.surface
    val sage = C.calm
    var selectedId by remember { mutableStateOf(BreathingGuides.all.first().exerciseId) }
    val guide = BreathingGuides.all.first { it.exerciseId == selectedId }
    var running by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var preparation by remember { mutableStateOf(5) }
    var elapsed by remember { mutableStateOf(0) }
    var confirmExit by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var soundSettings by remember { mutableStateOf(BreathingSoundPreferences.load(context)) }
    var showSoundSettings by remember { mutableStateOf(false) }
    val soundPlayer = remember { BreathingSoundPlayer() }
    val previewPlayer = remember { BreathingSoundPlayer() }
    val previewScope = rememberCoroutineScope()
    var previewJob by remember { mutableStateOf<Job?>(null) }
    var previewing by remember { mutableStateOf(false) }
    var previewToken by remember { mutableStateOf(0) }

    fun changeSound(next: BreathingSoundSettings) {
        soundSettings = next
        BreathingSoundPreferences.save(context, next)
        onSoundSettingsChange()
    }

    fun previewPhase(inspiration: Boolean) {
        previewJob?.cancel()
        previewToken += 1
        val token = previewToken
        previewJob = previewScope.launch {
            previewing = true
            try {
                previewPlayer.play(
                    soundSettings.copy(volume = soundSettings.volume.coerceAtLeast(10)),
                    inspiration,
                    if (inspiration) guide.inhaleSeconds else guide.exhaleSeconds,
                    preview = true
                )
            } finally {
                if (previewToken == token) previewing = false
            }
        }
    }

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
    val phaseIndex = (elapsed / cycle) * 2 + if (inhale) 0 else 1

    // La coroutine s'annule dès que l'on met en pause, coupe le son ou change de phase.
    // Ne pas mettre elapsed dans les clés : un son long doit continuer entre les secondes.
    LaunchedEffect(
        running, paused, finished, preparation == 0, selectedId,
        phaseIndex, soundSettings, previewing
    ) {
        if (running && !paused && !finished && preparation == 0 &&
            soundSettings.enabled && !previewing
        ) {
            soundPlayer.play(soundSettings, inhale, phaseRemaining)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ink)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedButton(onClick = ::requestExit) { Text("‹  Retour") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { changeSound(soundSettings.copy(enabled = !soundSettings.enabled)) }) {
                Text(if (soundSettings.enabled) "Couper" else "Activer", color = C.text)
            }
            IconButton(onClick = { showSoundSettings = true }) {
                Icon(
                    if (soundSettings.enabled) Icons.Rounded.VolumeUp
                    else Icons.Rounded.VolumeOff,
                    contentDescription = "Paramètres des sons de respiration",
                    tint = if (soundSettings.enabled) C.calm else C.textSecondary
                )
            }
        }
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

    if (showSoundSettings) {
        AlertDialog(
            onDismissRequest = {
                previewJob?.cancel()
                showSoundSettings = false
            },
            title = { Text("Sons de respiration") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 530.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Activer les sons", modifier = Modifier.weight(1f))
                        Switch(
                            checked = soundSettings.enabled,
                            onCheckedChange = { changeSound(soundSettings.copy(enabled = it)) }
                        )
                    }
                    Text("Mode sonore", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BreathingCueMode.entries.forEach { mode ->
                            FilterChip(
                                selected = soundSettings.mode == mode,
                                onClick = { changeSound(soundSettings.copy(mode = mode)) },
                                label = { Text(if (mode == BreathingCueMode.SHORT) "Courts" else "Continus") }
                            )
                        }
                    }
                    Text(
                        if (soundSettings.mode == BreathingCueMode.SHORT)
                            "Un signal au début de chaque inspiration et expiration."
                        else "Un son pendant toute la durée de chaque phase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = C.textSecondary
                    )
                    Text("Volume : ${soundSettings.volume} %", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = soundSettings.volume.toFloat(),
                        onValueChange = {
                            changeSound(soundSettings.copy(volume = (it / 5).roundToInt() * 5))
                        },
                        valueRange = 0f..100f,
                        steps = 19
                    )
                    BreathingPhaseSoundEditor(
                        title = "Inspiration",
                        pitchHz = soundSettings.inhalePitchHz,
                        timbre = soundSettings.inhaleTimbre,
                        onPitch = { changeSound(soundSettings.copy(inhalePitchHz = it)) },
                        onTimbre = { changeSound(soundSettings.copy(inhaleTimbre = it)) },
                        onPreview = { previewPhase(true) }
                    )
                    BreathingPhaseSoundEditor(
                        title = "Expiration",
                        pitchHz = soundSettings.exhalePitchHz,
                        timbre = soundSettings.exhaleTimbre,
                        onPitch = { changeSound(soundSettings.copy(exhalePitchHz = it)) },
                        onTimbre = { changeSound(soundSettings.copy(exhaleTimbre = it)) },
                        onPreview = { previewPhase(false) }
                    )
                    Text(
                        "Les réglages sont conservés. Le bouton Activer/Couper reste accessible pendant la séance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = C.textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    previewJob?.cancel()
                    showSoundSettings = false
                }) { Text("FERMER") }
            }
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

@Composable
private fun BreathingPhaseSoundEditor(
    title: String,
    pitchHz: Int,
    timbre: BreathingTimbre,
    onPitch: (Int) -> Unit,
    onTimbre: (BreathingTimbre) -> Unit,
    onPreview: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = onPreview) { Text("Écouter") }
        }
        Text("Hauteur : ${pitchHz} Hz", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = pitchHz.toFloat(),
            onValueChange = { onPitch((it / 50).roundToInt() * 50) },
            valueRange = 200f..1000f,
            steps = 15
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BreathingTimbre.entries.forEach { sound ->
                FilterChip(
                    selected = timbre == sound,
                    onClick = { onTimbre(sound) },
                    label = { Text(sound.label) }
                )
            }
        }
    }
}
