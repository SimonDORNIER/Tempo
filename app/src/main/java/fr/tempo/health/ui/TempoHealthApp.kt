package fr.tempo.health.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.tempo.health.BuildConfig
import fr.tempo.health.data.DailyCheckInEntity
import fr.tempo.health.data.DailyHealthEntity
import fr.tempo.health.data.HealthConnectAvailability
import fr.tempo.health.domain.RecoveryLevel
import fr.tempo.health.domain.RecoveryResult
import java.text.DecimalFormat

private data class Destination(
    val route: String,
    val label: String,
    val emoji: String
)

private val mainDestinations = listOf(
    Destination("today", "Aujourd'hui", "🏠"),
    Destination("health", "Santé", "❤️"),
    Destination("training", "Entraînement", "🏋️"),
    Destination("progress", "Progression", "📈"),
    Destination("coach", "Coach", "🧠")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoHealthApp(
    healthViewModel: HealthViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val selectedDestination = mainDestinations.firstOrNull { it.route == currentRoute }

    val recentDays by healthViewModel.recentDays.collectAsStateWithLifecycle()
    val checkIn by healthViewModel.todayCheckIn.collectAsStateWithLifecycle()
    val recovery by healthViewModel.recovery.collectAsStateWithLifecycle()
    val availability by healthViewModel.availability.collectAsStateWithLifecycle()
    val hasPermissions by healthViewModel.hasPermissions.collectAsStateWithLifecycle()
    val syncing by healthViewModel.syncing.collectAsStateWithLifecycle()
    val message by healthViewModel.message.collectAsStateWithLifecycle()
    val workoutState by workoutViewModel.state.collectAsStateWithLifecycle()
    val soundVolume by workoutViewModel.soundVolume.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        healthViewModel.onPermissionsResult(granted)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (currentRoute) {
                                "settings" -> "Paramètres"
                                "workout" -> "Séance"
                                else -> selectedDestination?.label ?: "Tempo Health"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        if (currentRoute != "settings" && currentRoute != "workout") {
                            Text(
                                text = "Tempo Health v" + BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute != "settings" && currentRoute != "workout") {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (selectedDestination != null) {
                NavigationBar {
                    mainDestinations.forEach { destination ->
                        val selected = currentEntry?.destination?.hierarchy
                            ?.any { it.route == destination.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo("today") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Text(
                                    text = destination.emoji,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(padding)
        ) {
            composable("today") {
                TodayScreen(
                    latest = recentDays.firstOrNull(),
                    checkIn = checkIn,
                    recovery = recovery,
                    hasPermissions = hasPermissions,
                    syncing = syncing,
                    requestPermissions = {
                        permissionLauncher.launch(healthViewModel.requiredPermissions)
                    },
                    sync = healthViewModel::sync,
                    saveCheckIn = healthViewModel::saveCheckIn
                )
            }

            composable("health") {
                HealthScreen(
                    days = recentDays,
                    availability = availability,
                    hasPermissions = hasPermissions,
                    syncing = syncing,
                    message = message,
                    requestPermissions = {
                        permissionLauncher.launch(healthViewModel.requiredPermissions)
                    },
                    sync = healthViewModel::sync
                )
            }

            composable("training") {
                TrainingScreen(
                    recovery = recovery,
                    onStart = {
                        workoutViewModel.prepare(recovery)
                        navController.navigate("workout")
                    }
                )
            }

            composable("workout") {
                WorkoutSessionScreen(
                    state = workoutState,
                    onStart = workoutViewModel::start,
                    onPause = workoutViewModel::togglePause,
                    onSkip = workoutViewModel::skip,
                    onExit = {
                        workoutViewModel.stop()
                        navController.popBackStack()
                    }
                )
            }

            composable("progress") {
                ProgressScreen(recentDays)
            }

            composable("coach") {
                CoachScreen(recovery)
            }

            composable("settings") {
                SettingsScreen(
                    availability = availability,
                    hasPermissions = hasPermissions,
                    localDays = recentDays.size,
                    soundVolume = soundVolume,
                    onSoundVolumeChange = workoutViewModel::setSoundVolume
                )
            }
        }
    }
}

@Composable
private fun Screen(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { content() }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun StatusCard(
    emoji: String,
    title: String,
    value: String,
    detail: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TodayScreen(
    latest: DailyHealthEntity?,
    checkIn: DailyCheckInEntity?,
    recovery: RecoveryResult,
    hasPermissions: Boolean,
    syncing: Boolean,
    requestPermissions: () -> Unit,
    sync: () -> Unit,
    saveCheckIn: (Int, Int) -> Unit
) {
    val energy = checkIn?.energy ?: 3
    val pain = checkIn?.pain ?: 0
    val energyIcons = listOf("😫", "😕", "😐", "🙂", "😁")
    val painLabels = listOf("Aucune", "Légère", "Moyenne", "Importante")

    Screen(
        title = "Aujourd'hui",
        subtitle = "Récupération, ressenti et décision du jour."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RecoveryCard(recovery)

            if (latest == null) {
                StatusCard(
                    emoji = "🔗",
                    title = "Santé Connect",
                    value = if (hasPermissions) "Prêt à synchroniser" else "Connexion nécessaire",
                    detail = "Le score devient pertinent après synchronisation."
                )

                Button(
                    onClick = if (hasPermissions) sync else requestPermissions,
                    enabled = !syncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (syncing) {
                            "Synchronisation…"
                        } else if (hasPermissions) {
                            "Synchroniser"
                        } else {
                            "Autoriser Santé Connect"
                        }
                    )
                }
            } else {
                StatusCard(
                    emoji = "😴",
                    title = "Sommeil",
                    value = formatMinutes(latest.sleepMinutes),
                    detail = sleepDetail(latest)
                )

                StatusCard(
                    emoji = "❤️",
                    title = "Cardio",
                    value = latest.hrvRmssdMs?.let {
                        "HRV " + oneDecimal(it) + " ms"
                    } ?: "HRV —",
                    detail = "FC repos " + bpm(latest.restingHeartRate) +
                        " • nuit " + bpm(latest.overnightHeartRate)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Check-in rapide", fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Énergie",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        energyIcons.forEachIndexed { index, icon ->
                            val level = index + 1
                            AssistChip(
                                onClick = { saveCheckIn(level, pain) },
                                label = {
                                    Text(
                                        text = if (energy == level) "• " + icon else icon,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            )
                        }
                    }

                    Text(
                        text = "Niveau " + energy + "/5",
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Douleurs",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        painLabels.forEachIndexed { index, label ->
                            AssistChip(
                                onClick = { saveCheckIn(energy, index) },
                                label = {
                                    Text(
                                        if (pain == index) "• " + label else label
                                    )
                                }
                            )
                        }
                    }

                    Text(
                        "Enregistré localement et intégré au score.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SessionRecommendationCard(recovery)
        }
    }
}

@Composable
private fun RecoveryCard(recovery: RecoveryResult) {
    val emoji = when (recovery.level) {
        RecoveryLevel.GREEN -> "🟢"
        RecoveryLevel.ORANGE -> "🟠"
        RecoveryLevel.RED -> "🔴"
        RecoveryLevel.UNKNOWN -> "⚪"
    }

    StatusCard(
        emoji = emoji,
        title = "Récupération",
        value = recovery.score?.let { it.toString() + " / 100" } ?: "À calibrer",
        detail = recovery.reasons.joinToString(" • ")
    )
}

@Composable
private fun SessionRecommendationCard(recovery: RecoveryResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Séance conseillée",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(6.dp))

            Text(
                recovery.sessionTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                "⏱ " + recovery.sessionMinutes + " min   •   ⚡ Intensité " +
                    recovery.intensity + "/10",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "Le moteur Tempo sera connecté à cette recommandation dans la prochaine étape.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HealthScreen(
    days: List<DailyHealthEntity>,
    availability: HealthConnectAvailability,
    hasPermissions: Boolean,
    syncing: Boolean,
    message: String?,
    requestPermissions: () -> Unit,
    sync: () -> Unit
) {
    val latest = days.firstOrNull()

    Screen(
        title = "Santé",
        subtitle = "Santé Connect → base locale → références personnelles."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val connectionLabel = when (availability) {
                HealthConnectAvailability.AVAILABLE ->
                    if (hasPermissions) "Connecté" else "Autorisations requises"
                HealthConnectAvailability.UPDATE_REQUIRED -> "Mise à jour requise"
                HealthConnectAvailability.UNAVAILABLE -> "Indisponible"
            }

            StatusCard(
                emoji = "🔗",
                title = "Santé Connect",
                value = connectionLabel,
                detail = if (hasPermissions) {
                    days.size.toString() + " jours enregistrés localement"
                } else {
                    "Lecture uniquement"
                }
            )

            if (availability == HealthConnectAvailability.AVAILABLE) {
                Button(
                    onClick = if (hasPermissions) sync else requestPermissions,
                    enabled = !syncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (syncing) {
                            "Synchronisation en cours…"
                        } else if (hasPermissions) {
                            "Synchroniser 28 jours"
                        } else {
                            "Autoriser les données santé"
                        }
                    )
                }
            }

            if (!message.isNullOrBlank()) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            StatusCard(
                "😴",
                "Sommeil",
                latest?.sleepMinutes?.let(::formatMinutes) ?: "—",
                latest?.let(::sleepDetail) ?: "Aucune donnée"
            )

            StatusCard(
                "❤️",
                "Cœur & HRV",
                latest?.hrvRmssdMs?.let { oneDecimal(it) + " ms" } ?: "—",
                latest?.let {
                    "FC repos " + bpm(it.restingHeartRate) +
                        " • respiration " + rate(it.respiratoryRate)
                } ?: "Aucune donnée"
            )

            StatusCard(
                "🏃",
                "Activité",
                latest?.steps?.let { it.toString() + " pas" } ?: "—",
                latest?.let {
                    distance(it.distanceMeters) + " • " +
                        kcal(it.caloriesKcal) + " • " +
                        (it.exerciseMinutes ?: 0L) + " min sport"
                } ?: "Aucune donnée"
            )

            StatusCard(
                "⚖️",
                "Corps & cardio",
                latest?.weightKg?.let { oneDecimal(it) + " kg" } ?: "Poids —",
                "VO₂ max " + (latest?.vo2Max?.let(::oneDecimal) ?: "—")
            )
        }
    }
}

@Composable
private fun TrainingScreen(
    recovery: RecoveryResult,
    onStart: () -> Unit
) {
    val plan = fr.tempo.health.domain.WorkoutPlanner.build(recovery)

    Screen(
        title = "Entraînement",
        subtitle = "La séance du jour est prête à démarrer."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SessionRecommendationCard(recovery)

            StatusCard(
                "📋",
                "Programme",
                plan.items.size.toString() + " exercices",
                plan.items.take(4).joinToString(" • ") { it.exercise.name } +
                    if (plan.items.size > 4) "…" else ""
            )

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("COMMENCER")
            }

            StatusCard(
                "⏱️",
                "Moteur Tempo natif",
                "Prêt",
                "Préparation 5 s • travail • repos • son mi-parcours • chrono global"
            )
        }
    }
}

@Composable
private fun ProgressScreen(days: List<DailyHealthEntity>) {
    val week = days.take(7)
    val month = days.take(28)

    Screen(
        title = "Progression",
        subtitle = "Tes références 7 et 28 jours servent maintenant au score."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "📅",
                "Moyenne 7 jours",
                averageMinutes(week.mapNotNull { it.sleepMinutes }),
                "HRV " + averageDouble(week.mapNotNull { it.hrvRmssdMs }, " ms")
            )

            StatusCard(
                "📊",
                "Moyenne 28 jours",
                averageMinutes(month.mapNotNull { it.sleepMinutes }),
                "FC repos " +
                    averageDouble(month.mapNotNull { it.restingHeartRate }, " bpm")
            )

            StatusCard(
                "🚶",
                "Activité 7 jours",
                averageLong(week.mapNotNull { it.steps }) + " pas/j",
                "Ces références sont personnelles, pas des normes de population."
            )
        }
    }
}

@Composable
private fun CoachScreen(recovery: RecoveryResult) {
    Screen(
        title = "Coach",
        subtitle = "L'IA reste une couche explicative facultative."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "🧠",
                "Moteur local",
                recovery.score?.let { it.toString() + " / 100" } ?: "En attente",
                "La décision fonctionne déjà sans Internet et sans IA."
            )

            StatusCard(
                "✨",
                "Coach ChatGPT",
                "Optionnel",
                "Plus tard, il expliquera le score et les tendances sans piloter aveuglément la séance."
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    availability: HealthConnectAvailability,
    hasPermissions: Boolean,
    localDays: Int,
    soundVolume: Int,
    onSoundVolumeChange: (Int) -> Unit
) {
    Screen(
        title = "Paramètres",
        subtitle = "État technique, confidentialité et sons."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "📦",
                "Version",
                BuildConfig.VERSION_NAME,
                "Package : " + BuildConfig.APPLICATION_ID
            )

            StatusCard(
                "🔗",
                "Santé Connect",
                if (hasPermissions) "Autorisé" else availabilityLabel(availability),
                "Lecture uniquement • aucun envoi automatique"
            )

            StatusCard(
                "💾",
                "Base locale",
                localDays.toString() + " jours",
                "Room / SQLite sur le téléphone"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Volume des sons", fontWeight = FontWeight.Bold)
                    Text(
                        soundVolume.toString() + " %",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = soundVolume.toFloat(),
                        onValueChange = { onSoundVolumeChange(it.toInt()) },
                        valueRange = 0f..100f
                    )
                    Text(
                        "Le son de test est joué pendant le réglage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            StatusCard(
                "📴",
                "Fonctionnement hors ligne",
                "Oui",
                "Santé locale, score et moteur d'entraînement fonctionnent sans serveur"
            )
        }
    }
}

@Composable
private fun WorkoutSessionScreen(
    state: WorkoutUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onExit: () -> Unit
) {
    val plan = state.plan

    if (plan == null) {
        Screen(
            title = "Séance",
            subtitle = "Aucune séance préparée."
        ) {
            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("RETOUR")
            }
        }
        return
    }

    val item = plan.items.getOrNull(state.exerciseIndex)

    Screen(
        title = plan.title,
        subtitle = "Exercice " +
            (state.exerciseIndex + 1).coerceAtMost(plan.items.size) +
            " / " + plan.items.size
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LinearProgressIndicator(
                progress = {
                    if (plan.items.isEmpty()) 0f
                    else (state.exerciseIndex.toFloat() / plan.items.size.toFloat())
                        .coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth()
            )

            when (state.phase) {
                WorkoutPhase.READY -> {
                    StatusCard(
                        "🏁",
                        "Prêt",
                        plan.estimatedMinutes.toString() + " min environ",
                        plan.items.joinToString(" • ") { it.exercise.name }
                    )

                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Text("DÉMARRER LA SÉANCE")
                    }
                }

                WorkoutPhase.COMPLETE -> {
                    StatusCard(
                        "✅",
                        "Séance terminée",
                        formatClock(state.elapsedSeconds),
                        "Bien joué. L'historique détaillé arrivera à l'étape suivante."
                    )

                    Button(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TERMINER")
                    }
                }

                else -> {
                    val phaseLabel = when (state.phase) {
                        WorkoutPhase.PREPARE -> "PRÉPARE-TOI"
                        WorkoutPhase.WORK -> "EXERCICE"
                        WorkoutPhase.REST -> "REPOS"
                        else -> ""
                    }

                    val phaseEmoji = when (state.phase) {
                        WorkoutPhase.PREPARE -> "👀"
                        WorkoutPhase.WORK -> "🔥"
                        WorkoutPhase.REST -> "💧"
                        else -> "⏱️"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.phase == WorkoutPhase.WORK) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                phaseEmoji + "  " + phaseLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                state.remainingSeconds.toString(),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "secondes",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (item != null) {
                        StatusCard(
                            "🏋️",
                            if (state.phase == WorkoutPhase.REST) {
                                "Prochain : " +
                                    (plan.items.getOrNull(state.exerciseIndex + 1)
                                        ?.exercise?.name ?: "Fin")
                            } else {
                                item.exercise.name
                            },
                            if (state.phase == WorkoutPhase.WORK) {
                                item.workSeconds.toString() + " s"
                            } else {
                                "Préparation"
                            },
                            if (state.phase == WorkoutPhase.REST) {
                                "Respire et relâche les tensions."
                            } else {
                                item.exercise.instructions
                            }
                        )

                        if (state.phase != WorkoutPhase.REST) {
                            Text(
                                "💡 " + item.exercise.cue,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    StatusCard(
                        "⏱️",
                        "Chrono global",
                        formatClock(state.elapsedSeconds),
                        if (state.paused) "Séance en pause" else "Séance en cours"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPause,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (state.paused) "REPRENDRE" else "PAUSE")
                        }

                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PASSER")
                        }
                    }

                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("QUITTER LA SÉANCE")
                    }
                }
            }
        }
    }
}

private fun formatClock(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return minutes.toString().padStart(2, '0') + ":" +
        rest.toString().padStart(2, '0')
}

private fun formatMinutes(minutes: Long?): String {
    if (minutes == null) return "—"
    val hours = minutes / 60
    val rest = minutes % 60
    return hours.toString() + " h " + rest.toString().padStart(2, '0')
}

private fun sleepDetail(day: DailyHealthEntity): String =
    "Profond " + (day.deepMinutes ?: 0L) + " min • REM " +
        (day.remMinutes ?: 0L) + " min • éveil " +
        (day.awakeMinutes ?: 0L) + " min"

private fun bpm(value: Double?): String =
    value?.let { oneDecimal(it) + " bpm" } ?: "—"

private fun rate(value: Double?): String =
    value?.let { oneDecimal(it) + "/min" } ?: "—"

private fun distance(value: Double?): String =
    value?.let { oneDecimal(it / 1000.0) + " km" } ?: "—"

private fun kcal(value: Double?): String =
    value?.let { it.toInt().toString() + " kcal" } ?: "—"

private fun oneDecimal(value: Double): String =
    DecimalFormat("0.0").format(value)

private fun averageMinutes(values: List<Long>): String =
    if (values.isEmpty()) "—" else formatMinutes(values.average().toLong())

private fun averageDouble(values: List<Double>, suffix: String): String =
    if (values.isEmpty()) "—" else oneDecimal(values.average()) + suffix

private fun averageLong(values: List<Long>): String =
    if (values.isEmpty()) "—" else values.average().toLong().toString()

private fun availabilityLabel(value: HealthConnectAvailability): String =
    when (value) {
        HealthConnectAvailability.AVAILABLE -> "Non autorisé"
        HealthConnectAvailability.UPDATE_REQUIRED -> "Mise à jour requise"
        HealthConnectAvailability.UNAVAILABLE -> "Indisponible"
    }
