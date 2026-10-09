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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import fr.tempo.health.data.WorkoutHistoryEntity
import fr.tempo.health.domain.WorkoutPlan
import fr.tempo.health.domain.Equipment
import fr.tempo.health.domain.Exercise
import fr.tempo.health.domain.ExerciseCategory
import fr.tempo.health.domain.ExerciseLibrary
import fr.tempo.health.domain.TrainingPreferences
import fr.tempo.health.domain.RecoveryFactorState
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
    Destination("training", "Séance", "🏋️"),
    Destination("progress", "Progression", "📈"),
    Destination("coach", "Coach", "🧠")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoHealthApp(
    healthViewModel: HealthViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel(),
    freeTimerViewModel: FreeTimerViewModel = viewModel(),
    onCheckForUpdates: () -> Unit = {}
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
    val workoutHistory by workoutViewModel.history.collectAsStateWithLifecycle()
    val trainingPreferences by workoutViewModel.trainingPreferences.collectAsStateWithLifecycle()
    val freeTimerState by freeTimerViewModel.state.collectAsStateWithLifecycle()
    val previewPlan = workoutViewModel.previewPlan(recovery)

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
                                "library-settings" -> "Bibliothèque"
                                "category-settings" -> "Catégories"
                                "exercise-settings" -> "Exercices"
                                "timer" -> "Minuteur"
                                "workout" -> "Séance"
                                else -> selectedDestination?.label ?: "Tempo Health"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        if (currentRoute != "settings" &&
                            currentRoute != "library-settings" &&
                            currentRoute != "category-settings" &&
                            currentRoute != "exercise-settings" &&
                            currentRoute != "timer" &&
                            currentRoute != "workout"
                        ) {
                            Text(
                                text = "Tempo Health v" + BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute != "settings" &&
                        currentRoute != "exercise-settings" &&
                        currentRoute != "workout"
                    ) {
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
                    plan = previewPlan,
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
                    plan = previewPlan,
                    history = workoutHistory,
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
                    onReplace = workoutViewModel::replaceCurrentExercise,
                    onDifficulty = workoutViewModel::rateDifficulty,
                    onExit = {
                        workoutViewModel.stop()
                        navController.popBackStack()
                    }
                )
            }

            composable("progress") {
                ProgressScreen(
                    days = recentDays,
                    history = workoutHistory
                )
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
                    preferences = trainingPreferences,
                    onSoundVolumeChange = workoutViewModel::setSoundVolume,
                    onDurationChange = workoutViewModel::setDurationMinutes,
                    onToggleEquipment = workoutViewModel::toggleEquipment,
                    onOpenLibrary = {
                        navController.navigate("library-settings")
                    },
                    onOpenTimer = {
                        navController.navigate("timer")
                    },
                    onCheckForUpdates = onCheckForUpdates
                )
            }

            composable("timer") {
                FreeTimerScreen(
                    state = freeTimerState,
                    onMinutesChange = freeTimerViewModel::setMinutes,
                    onToggle = freeTimerViewModel::toggleRunning,
                    onReset = freeTimerViewModel::reset
                )
            }

            composable("library-settings") {
                LibrarySettingsScreen(
                    preferences = trainingPreferences,
                    onOpenCategories = {
                        navController.navigate("category-settings")
                    },
                    onOpenExercises = {
                        navController.navigate("exercise-settings")
                    },
                    onReset = workoutViewModel::resetLibraryCustomizations
                )
            }

            composable("category-settings") {
                CategorySettingsScreen(
                    preferences = trainingPreferences,
                    onToggle = workoutViewModel::toggleCategory,
                    onSetIcon = workoutViewModel::setCategoryIcon,
                    onResetIcon = workoutViewModel::resetCategoryIcon
                )
            }

            composable("exercise-settings") {
                ExercisePreferencesScreen(
                    preferences = trainingPreferences,
                    onFavorite = workoutViewModel::toggleFavoriteExercise,
                    onAvoid = workoutViewModel::toggleAvoidedExercise,
                    onWorkSecondsChange = workoutViewModel::setExerciseWorkSeconds,
                    onResetWorkSeconds = workoutViewModel::resetExerciseWorkSeconds,
                    onSaveExercise = workoutViewModel::saveExercise,
                    onAddExercise = workoutViewModel::addCustomExercise,
                    onDeleteExercise = workoutViewModel::deleteCustomExercise,
                    onResetExercise = workoutViewModel::resetExerciseOverride
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
    plan: WorkoutPlan,
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
            RecoveryFactorsCard(recovery)

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

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        painLabels.chunked(2).forEachIndexed { rowIndex, labels ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                labels.forEachIndexed { columnIndex, label ->
                                    val index = rowIndex * 2 + columnIndex
                                    AssistChip(
                                        onClick = { saveCheckIn(energy, index) },
                                        modifier = Modifier.weight(1f),
                                        label = {
                                            Text(
                                                if (pain == index) "• " + label else label
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "Enregistré localement et intégré au score.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SessionRecommendationCard(recovery, plan)
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
        detail = (
            recovery.reasons.joinToString(" • ") +
                if (recovery.score != null) {
                    " • confiance " + recovery.confidence + "%"
                } else {
                    ""
                }
            )
    )
}

@Composable
private fun RecoveryFactorsCard(recovery: RecoveryResult) {
    if (recovery.factors.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Pourquoi ce score ?",
                fontWeight = FontWeight.Bold
            )

            Text(
                recovery.baselineDays.toString() +
                    " jour(s) de référence • confiance " +
                    recovery.confidence + "%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            recovery.factors.take(6).forEach { factor ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (factor.state) {
                            RecoveryFactorState.POSITIVE -> "↗"
                            RecoveryFactorState.NEUTRAL -> "→"
                            RecoveryFactorState.NEGATIVE -> "↘"
                            RecoveryFactorState.MISSING -> "?"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            factor.label,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            factor.value + " • réf. " + factor.reference,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (factor.impact != 0) {
                        Text(
                            (if (factor.impact > 0) "+" else "") +
                                factor.impact.toString(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRecommendationCard(
    recovery: RecoveryResult,
    plan: WorkoutPlan
) {
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
                plan.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                "⏱ " + plan.estimatedMinutes + " min   •   ⚡ Intensité " +
                    plan.intensity + "/10",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "Le moteur Tempo adapte maintenant le contenu selon ta récupération et ton historique récent.",
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
                    days.size.toString() + " jours locaux • " +
                        formatSyncAge(latest?.syncedAtEpochMs)
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

            TrendCard(
                emoji = "😴",
                title = "Sommeil • 7 jours",
                values = days.take(7).reversed().map { it.sleepMinutes?.toDouble() },
                valueText = { value -> formatMinutes(value.toLong()) }
            )

            TrendCard(
                emoji = "❤️",
                title = "HRV • 7 jours",
                values = days.take(7).reversed().map { it.hrvRmssdMs },
                valueText = { value -> oneDecimal(value) + " ms" }
            )
        }
    }
}

@Composable
private fun TrainingScreen(
    recovery: RecoveryResult,
    plan: WorkoutPlan,
    history: List<WorkoutHistoryEntity>,
    onStart: () -> Unit
) {
    val last = history.firstOrNull()

    Screen(
        title = "Entraînement",
        subtitle = "La séance tient compte de ta récupération et des groupes récemment travaillés."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                recoveryEmoji(recovery),
                "Séance conseillée",
                plan.title,
                "⏱ " + plan.estimatedMinutes + " min • ⚡ Intensité " +
                    plan.intensity + "/10"
            )

            StatusCard(
                "📋",
                "Programme",
                plan.items.size.toString() + " exercices",
                plan.items.take(5).joinToString(" • ") { it.exercise.name } +
                    if (plan.items.size > 5) "…" else ""
            )

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("COMMENCER")
            }

            if (last != null) {
                StatusCard(
                    if (last.completed) "✅" else "⏹️",
                    "Dernière séance",
                    last.title,
                    formatClock(last.durationSeconds) +
                        " • " + last.completedExercises + "/" +
                        last.plannedExercises + " exercices" +
                        (last.perceivedDifficulty?.let { " • difficulté " + it + "/5" } ?: "")
                )
            } else {
                StatusCard(
                    "🆕",
                    "Historique",
                    "Première séance",
                    "Après ta séance, sa durée, les groupes travaillés et ton ressenti seront mémorisés."
                )
            }

            StatusCard(
                "🧠",
                "Programmation locale",
                "Historique actif",
                "Le moteur évite si possible les groupes sollicités dans les dernières 48 h."
            )
        }
    }
}

@Composable
private fun ProgressScreen(
    days: List<DailyHealthEntity>,
    history: List<WorkoutHistoryEntity>
) {
    val week = days.take(7)
    val month = days.take(28)
    val weekCutoff = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
    val recentWorkouts = history.filter { it.startedAtEpochMs >= weekCutoff }
    val completedWorkouts = recentWorkouts.filter { it.completed }

    Screen(
        title = "Progression",
        subtitle = "Santé et entraînement sont maintenant suivis ensemble."
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

            TrendCard(
                emoji = "🗓️",
                title = "Sommeil • 28 jours",
                values = month.reversed().map { it.sleepMinutes?.toDouble() },
                valueText = { value -> formatMinutes(value.toLong()) }
            )

            TrendCard(
                emoji = "📈",
                title = "HRV • 28 jours",
                values = month.reversed().map { it.hrvRmssdMs },
                valueText = { value -> oneDecimal(value) + " ms" }
            )

            TrendCard(
                emoji = "😴",
                title = "Sommeil • 7 jours",
                values = week.reversed().map { it.sleepMinutes?.toDouble() },
                valueText = { value -> formatMinutes(value.toLong()) }
            )

            TrendCard(
                emoji = "❤️",
                title = "HRV • 7 jours",
                values = week.reversed().map { it.hrvRmssdMs },
                valueText = { value -> oneDecimal(value) + " ms" }
            )

            TrendCard(
                emoji = "💓",
                title = "FC repos • 7 jours",
                values = week.reversed().map { it.restingHeartRate },
                valueText = { value -> value.toInt().toString() + " bpm" },
                lowerIsBetter = true
            )

            TrendCard(
                emoji = "🚶",
                title = "Pas • 7 jours",
                values = week.reversed().map { it.steps?.toDouble() },
                valueText = { value -> value.toLong().toString() }
            )

            StatusCard(
                "🏋️",
                "Séances 7 jours",
                completedWorkouts.size.toString() + " terminée(s)",
                "Temps cumulé " + formatClock(
                    completedWorkouts.sumOf { it.durationSeconds }
                )
            )

            history.take(4).forEach { workout ->
                StatusCard(
                    if (workout.completed) "✅" else "⏹️",
                    workout.title,
                    formatClock(workout.durationSeconds),
                    workout.completedExercises.toString() + "/" +
                        workout.plannedExercises + " exercices • " +
                        muscleGroupsLabel(workout.muscleGroupsCsv) +
                        (workout.perceivedDifficulty?.let {
                            " • difficulté " + it + "/5"
                        } ?: "")
                )
            }
        }
    }
}

@Composable
private fun TrendCard(
    emoji: String,
    title: String,
    values: List<Double?>,
    valueText: (Double) -> String,
    lowerIsBetter: Boolean = false
) {
    val valid = values.mapNotNull { it }
    if (valid.isEmpty()) return

    val min = valid.minOrNull() ?: 0.0
    val max = valid.maxOrNull() ?: min
    val span = (max - min).takeIf { it > 0.0 } ?: 1.0
    val latest = values.lastOrNull { it != null }
    val first = values.firstOrNull { it != null }

    val direction = if (latest != null && first != null) {
        val change = latest - first
        when {
            kotlin.math.abs(change) < span * 0.08 -> "stable"
            (change > 0 && !lowerIsBetter) || (change < 0 && lowerIsBetter) -> "↗ favorable"
            else -> "↘ à surveiller"
        }
    } else {
        "tendance incomplète"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(
                        latest?.let(valueText) ?: "—",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    direction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    if (values.size > 14) 2.dp else 6.dp
                ),
                verticalAlignment = Alignment.Bottom
            ) {
                values.forEach { value ->
                    val normalized = if (value == null) {
                        0.05
                    } else {
                        0.18 + 0.82 * ((value - min) / span).coerceIn(0.0, 1.0)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((88.0 * normalized).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (value == null) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                    )
                }
            }
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
    preferences: TrainingPreferences,
    onSoundVolumeChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onToggleEquipment: (Equipment) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTimer: () -> Unit,
    onCheckForUpdates: () -> Unit
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

            OutlinedButton(
                onClick = onCheckForUpdates,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 VÉRIFIER LES MISES À JOUR")
            }

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
                    Text("Durée disponible", fontWeight = FontWeight.Bold)
                    Text(
                        preferences.durationMinutes.toString() + " min",
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10, 15, 20, 30).forEach { minutes ->
                            AssistChip(
                                onClick = { onDurationChange(minutes) },
                                label = {
                                    Text(
                                        if (preferences.durationMinutes == minutes) {
                                            "• " + minutes
                                        } else {
                                            minutes.toString()
                                        }
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("Matériel disponible", fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { onToggleEquipment(Equipment.MAT) },
                            label = {
                                Text(
                                    if (Equipment.MAT in preferences.availableEquipment) {
                                        "• Tapis"
                                    } else {
                                        "Tapis"
                                    }
                                )
                            }
                        )

                        AssistChip(
                            onClick = { onToggleEquipment(Equipment.CHAIR) },
                            label = {
                                Text(
                                    if (Equipment.CHAIR in preferences.availableEquipment) {
                                        "• Chaise"
                                    } else {
                                        "Chaise"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onOpenLibrary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🛠 GÉRER LA BIBLIOTHÈQUE")
            }

            OutlinedButton(
                onClick = onOpenTimer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⏱ MINUTEUR LIBRE")
            }

            Text(
                preferences.favoriteExerciseIds.size.toString() + " favori(s) • " +
                    preferences.avoidedExerciseIds.size + " à éviter",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun LibrarySettingsScreen(
    preferences: TrainingPreferences,
    onOpenCategories: () -> Unit,
    onOpenExercises: () -> Unit,
    onReset: () -> Unit
) {
    val disabledExercises = preferences.avoidedExerciseIds.size
    val customDurations = preferences.exerciseWorkSeconds.size

    Screen(
        title = "Bibliothèque",
        subtitle = "L'utilisation reste séparée de l'édition."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "🗂️",
                "Catégories",
                (ExerciseCategory.entries.size - preferences.disabledCategories.size)
                    .toString() + "/" + ExerciseCategory.entries.size + " actives",
                "Icônes et catégories utilisées par le générateur."
            )

            Button(
                onClick = onOpenCategories,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("MODIFIER LES CATÉGORIES")
            }

            StatusCard(
                "🏋️",
                "Exercices",
                ExerciseLibrary.all.size.toString() + " exercices",
                disabledExercises.toString() + " à éviter • " +
                    customDurations + " durée(s) personnalisée(s)"
            )

            Button(
                onClick = onOpenExercises,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("MODIFIER LES EXERCICES")
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("RÉINITIALISER LES PERSONNALISATIONS")
            }

            Text(
                "Les favoris et les exercices « à éviter » sont conservés lors de cette réinitialisation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategorySettingsScreen(
    preferences: TrainingPreferences,
    onToggle: (ExerciseCategory) -> Unit,
    onSetIcon: (ExerciseCategory, String) -> Unit,
    onResetIcon: (ExerciseCategory) -> Unit
) {
    Screen(
        title = "Modifier les catégories",
        subtitle = "Active, désactive ou change l'icône de chaque catégorie."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExerciseCategory.entries.forEach { category ->
                val active = category !in preferences.disabledCategories
                val icon = categoryIcon(category, preferences)
                val count = ExerciseLibrary.all.count { it.category == category }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(icon, style = MaterialTheme.typography.titleLarge)
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    categoryLabel(category),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    count.toString() + " exercice(s) • " +
                                        if (active) "active" else "désactivée",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Icône",
                            fontWeight = FontWeight.SemiBold
                        )

                        val availableIcons = listOf(
                            "🔥", "💪", "🦵", "🧠", "🧘", "🤸",
                            "⚡", "❤️", "🏃", "🫁", "🦴", "🎯"
                        )

                        availableIcons.chunked(6).forEach { iconRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                iconRow.forEach { candidate ->
                                    AssistChip(
                                        onClick = {
                                            onSetIcon(category, candidate)
                                        },
                                        modifier = Modifier.weight(1f),
                                        label = {
                                            Text(
                                                if (candidate == icon) {
                                                    "• " + candidate
                                                } else {
                                                    candidate
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onResetIcon(category) },
                                modifier = Modifier.weight(1f),
                                enabled = category in preferences.categoryIcons
                            ) {
                                Text("ICÔNE DÉFAUT")
                            }

                            Button(
                                onClick = { onToggle(category) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (active) "DÉSACTIVER" else "ACTIVER")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeTimerScreen(
    state: FreeTimerUiState,
    onMinutesChange: (Int) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val total = state.durationSeconds.coerceAtLeast(1)
    val progress = (state.remainingSeconds.toFloat() / total.toFloat())
        .coerceIn(0f, 1f)

    Screen(
        title = "Minuteur libre",
        subtitle = "Règle une durée, lance le chrono et Tempo sonne à la fin."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.finished) {
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
                        when {
                            state.finished -> "✅ TERMINÉ"
                            state.running -> "⏱ EN COURS"
                            else -> "⏱ PRÊT"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        formatClock(state.remainingSeconds),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Durée", fontWeight = FontWeight.Bold)
                    Text(
                        (state.durationSeconds / 60).toString() + " min",
                        color = MaterialTheme.colorScheme.primary
                    )

                    Slider(
                        value = (state.durationSeconds / 60).toFloat(),
                        onValueChange = { onMinutesChange(it.toInt()) },
                        valueRange = 1f..60f,
                        steps = 58,
                        enabled = !state.running
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 5, 10, 15, 20).forEach { minutes ->
                            AssistChip(
                                onClick = { onMinutesChange(minutes) },
                                enabled = !state.running,
                                label = { Text(minutes.toString()) }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text(
                    when {
                        state.finished -> "RELANCER"
                        state.running -> "PAUSE"
                        state.remainingSeconds < state.durationSeconds -> "REPRENDRE"
                        else -> "DÉMARRER"
                    }
                )
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("RÉINITIALISER")
            }

            Text(
                "Le volume du signal de fin suit le réglage « Volume des sons ».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExercisePreferencesScreen(
    preferences: TrainingPreferences,
    onFavorite: (String) -> Unit,
    onAvoid: (String) -> Unit,
    onWorkSecondsChange: (String, Int) -> Unit,
    onResetWorkSeconds: (String) -> Unit,
    onSaveExercise: (Exercise) -> Unit,
    onAddExercise: () -> String,
    onDeleteExercise: (String) -> Unit,
    onResetExercise: (String) -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf<ExerciseCategory?>(null)
    }
    var editingId by remember { mutableStateOf<String?>(null) }

    val allExercises = ExerciseLibrary.effective(preferences)
    val visibleExercises = allExercises.filter { exercise ->
        selectedCategory == null || exercise.category == selectedCategory
    }

    Screen(
        title = "Modifier les exercices",
        subtitle = "Crée, modifie ou exclus des exercices. Les changements sont locaux."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    editingId = onAddExercise()
                    selectedCategory = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋ AJOUTER UN EXERCICE")
            }

            Text("Filtrer par catégorie", fontWeight = FontWeight.Bold)

            val filterOptions = listOf<ExerciseCategory?>(null) +
                ExerciseCategory.entries

            filterOptions.chunked(2).forEach { filterRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterRow.forEach { category ->
                        AssistChip(
                            onClick = { selectedCategory = category },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    if (category == null) {
                                        if (selectedCategory == null) "• Toutes" else "Toutes"
                                    } else {
                                        val base = categoryIcon(category, preferences) +
                                            " " + categoryLabel(category)
                                        if (selectedCategory == category) "• " + base else base
                                    }
                                )
                            }
                        )
                    }
                    if (filterRow.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Text(
                visibleExercises.size.toString() + " exercice(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            visibleExercises.forEach { exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(exercise.name, fontWeight = FontWeight.Bold)
                        Text(
                            categoryIcon(exercise.category, preferences) + " " +
                                categoryLabel(exercise.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        if (editingId == exercise.id) {
                            var name by remember(exercise.id, exercise.name) {
                                mutableStateOf(exercise.name)
                            }
                            var instructions by remember(
                                exercise.id,
                                exercise.instructions
                            ) {
                                mutableStateOf(exercise.instructions)
                            }
                            var cue by remember(exercise.id, exercise.cue) {
                                mutableStateOf(exercise.cue)
                            }
                            var category by remember(
                                exercise.id,
                                exercise.category
                            ) {
                                mutableStateOf(exercise.category)
                            }
                            var equipment by remember(
                                exercise.id,
                                exercise.requiredEquipment
                            ) {
                                mutableStateOf(exercise.requiredEquipment)
                            }
                            var seconds by remember(
                                exercise.id,
                                exercise.defaultWorkSeconds
                            ) {
                                mutableStateOf(exercise.defaultWorkSeconds)
                            }

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nom") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = instructions,
                                onValueChange = { instructions = it },
                                label = { Text("Explications") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            OutlinedTextField(
                                value = cue,
                                onValueChange = { cue = it },
                                label = { Text("Conseil coach") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            Text("Catégorie", fontWeight = FontWeight.SemiBold)
                            ExerciseCategory.entries.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { item ->
                                        AssistChip(
                                            onClick = { category = item },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Text(
                                                    if (category == item) {
                                                        "• " + categoryLabel(item)
                                                    } else {
                                                        categoryLabel(item)
                                                    }
                                                )
                                            }
                                        )
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }

                            Text("Matériel", fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf<Equipment?>(null, Equipment.MAT, Equipment.CHAIR)
                                    .forEach { item ->
                                        AssistChip(
                                            onClick = { equipment = item },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                val label = when (item) {
                                                    null -> "Aucun"
                                                    Equipment.MAT -> "Tapis"
                                                    Equipment.CHAIR -> "Chaise"
                                                }
                                                Text(
                                                    if (equipment == item) "• " + label else label
                                                )
                                            }
                                        )
                                    }
                            }

                            Text(
                                "Durée : " + seconds + " s",
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = seconds.toFloat(),
                                onValueChange = { seconds = it.toInt() },
                                valueRange = 15f..120f,
                                steps = 20
                            )

                            Button(
                                onClick = {
                                    onSaveExercise(
                                        exercise.copy(
                                            name = name.trim().ifBlank { "Exercice" },
                                            category = category,
                                            defaultWorkSeconds = seconds,
                                            instructions = instructions.trim(),
                                            cue = cue.trim(),
                                            requiredEquipment = equipment
                                        )
                                    )
                                    editingId = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ENREGISTRER")
                            }

                            OutlinedButton(
                                onClick = { editingId = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ANNULER")
                            }

                            if (exercise.id.startsWith("user-")) {
                                OutlinedButton(
                                    onClick = {
                                        onDeleteExercise(exercise.id)
                                        editingId = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SUPPRIMER CET EXERCICE")
                                }
                            } else if (exercise.id in preferences.exerciseOverrides) {
                                OutlinedButton(
                                    onClick = {
                                        onResetExercise(exercise.id)
                                        editingId = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("REVENIR À LA VERSION D'ORIGINE")
                                }
                            }
                        } else {
                            val workSeconds =
                                preferences.exerciseWorkSeconds[exercise.id]
                                    ?: exercise.defaultWorkSeconds

                            Text(
                                workSeconds.toString() + " s • " +
                                    when (exercise.requiredEquipment) {
                                        null -> "sans matériel"
                                        Equipment.MAT -> "tapis"
                                        Equipment.CHAIR -> "chaise"
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = { editingId = exercise.id },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("✏ Modifier") }
                                )
                                AssistChip(
                                    onClick = { onFavorite(exercise.id) },
                                    modifier = Modifier.weight(1f),
                                    label = {
                                        Text(
                                            if (exercise.id in preferences.favoriteExerciseIds) {
                                                "★ Favori"
                                            } else {
                                                "☆ Favori"
                                            }
                                        )
                                    }
                                )
                            }

                            AssistChip(
                                onClick = { onAvoid(exercise.id) },
                                label = {
                                    Text(
                                        if (exercise.id in preferences.avoidedExerciseIds) {
                                            "⛔ Évité"
                                        } else {
                                            "Éviter"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSessionScreen(
    state: WorkoutUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onReplace: () -> Unit,
    onDifficulty: (Int) -> Unit,
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

                    OutlinedButton(
                        onClick = onReplace,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔄 REMPLACER LE PREMIER EXERCICE")
                    }
                }

                WorkoutPhase.COMPLETE -> {
                    StatusCard(
                        "✅",
                        "Séance terminée",
                        formatClock(state.elapsedSeconds),
                        state.completedExercises.toString() + "/" +
                            plan.items.size + " exercices enregistrés localement."
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                "Difficulté ressentie",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Ce retour ajustera légèrement la prochaine séance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (1..5).forEach { value ->
                                    AssistChip(
                                        onClick = { onDifficulty(value) },
                                        label = {
                                            Text(
                                                if (state.perceivedDifficulty == value) {
                                                    "• " + value
                                                } else {
                                                    value.toString()
                                                }
                                            )
                                        }
                                    )
                                }
                            }

                            Text(
                                when (state.perceivedDifficulty) {
                                    1 -> "Très facile"
                                    2 -> "Facile"
                                    3 -> "Bien dosée"
                                    4 -> "Difficile"
                                    5 -> "Très difficile"
                                    else -> "Choisis de 1 à 5."
                                },
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

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

                    if (state.phase != WorkoutPhase.REST) {
                        OutlinedButton(
                            onClick = onReplace,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔄 REMPLACER CET EXERCICE")
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

private fun categoryLabel(category: ExerciseCategory): String =
    when (category) {
        ExerciseCategory.WARMUP -> "Échauffement"
        ExerciseCategory.FULL_BODY -> "Corps entier"
        ExerciseCategory.UPPER_BODY -> "Haut du corps"
        ExerciseCategory.CORE -> "Gainage"
        ExerciseCategory.LOWER_BODY -> "Bas du corps"
        ExerciseCategory.MOBILITY -> "Mobilité"
        ExerciseCategory.STRETCHING -> "Étirements"
    }

private fun categoryIcon(
    category: ExerciseCategory,
    preferences: TrainingPreferences
): String =
    preferences.categoryIcons[category] ?: when (category) {
        ExerciseCategory.WARMUP -> "🔥"
        ExerciseCategory.FULL_BODY -> "⚡"
        ExerciseCategory.UPPER_BODY -> "💪"
        ExerciseCategory.CORE -> "🧠"
        ExerciseCategory.LOWER_BODY -> "🦵"
        ExerciseCategory.MOBILITY -> "🤸"
        ExerciseCategory.STRETCHING -> "🧘"
    }

private fun recoveryEmoji(recovery: RecoveryResult): String =
    when (recovery.level) {
        RecoveryLevel.GREEN -> "🟢"
        RecoveryLevel.ORANGE -> "🟠"
        RecoveryLevel.RED -> "🔴"
        RecoveryLevel.UNKNOWN -> "⚪"
    }

private fun muscleGroupsLabel(csv: String): String {
    if (csv.isBlank()) return "Mobilité"

    return csv.split(",")
        .map { group ->
            when (group) {
                "UPPER_BODY" -> "haut"
                "LOWER_BODY" -> "bas"
                "CORE" -> "gainage"
                else -> group.lowercase()
            }
        }
        .joinToString(" + ")
}

private fun formatSyncAge(epochMs: Long?): String {
    if (epochMs == null) return "jamais synchronisé"

    val ageMinutes = (
        (System.currentTimeMillis() - epochMs)
            .coerceAtLeast(0L) / 60_000L
        )

    return when {
        ageMinutes < 2 -> "mis à jour à l'instant"
        ageMinutes < 60 -> "mis à jour il y a " + ageMinutes + " min"
        ageMinutes < 24 * 60 -> "mis à jour il y a " +
            (ageMinutes / 60) + " h"
        else -> "mis à jour il y a " + (ageMinutes / (24 * 60)) + " j"
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
