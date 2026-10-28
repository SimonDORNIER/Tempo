package fr.tempo.health.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import fr.tempo.health.ui.theme.TempoPanelCard
import fr.tempo.health.ui.theme.TempoColors as C
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import fr.tempo.health.data.FullBackupManager
import fr.tempo.health.data.HealthConnectAvailability
import fr.tempo.health.data.WorkoutHistoryEntity
import fr.tempo.health.domain.WorkoutPlan
import fr.tempo.health.domain.DailyCoachEngine
import fr.tempo.health.domain.Equipment
import fr.tempo.health.domain.Exercise
import fr.tempo.health.domain.ExerciseCategory
import fr.tempo.health.domain.ExerciseLibrary
import fr.tempo.health.domain.TrainingGoal
import fr.tempo.health.domain.TrainingPreferences
import fr.tempo.health.domain.WeeklyProgram
import fr.tempo.health.domain.ProgramDayKind
import fr.tempo.health.domain.RecoveryFactorState
import fr.tempo.health.domain.RecoveryLevel
import fr.tempo.health.domain.RecoveryResult
import fr.tempo.health.reminders.ReminderSettings
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ConfirmationRequest(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val action: () -> Unit
)

@Composable
private fun ConfirmSensitiveAction(
    request: ConfirmationRequest?,
    onDismiss: () -> Unit
) {
    if (request == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(request.title) },
        text = { Text(request.message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    request.action()
                }
            ) { Text(request.confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANNULER") }
        }
    )
}

private data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val mainDestinations = listOf(
    Destination("today", "Aujourd'hui", Icons.Rounded.Home),
    Destination("practice", "Pratiquer", Icons.Rounded.Spa),
    Destination("tracking", "Suivi", Icons.Rounded.BarChart),
    Destination("me", "Moi", Icons.Rounded.Person)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoHealthApp(
    healthViewModel: HealthViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel(),
    freeTimerViewModel: FreeTimerViewModel = viewModel(),
    backupViewModel: BackupViewModel = viewModel(),
    reminderViewModel: ReminderViewModel = viewModel(),
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
    val missingPermissionLabels by
        healthViewModel.missingPermissionLabels.collectAsStateWithLifecycle()
    val syncing by healthViewModel.syncing.collectAsStateWithLifecycle()
    val message by healthViewModel.message.collectAsStateWithLifecycle()
    val workoutState by workoutViewModel.state.collectAsStateWithLifecycle()
    val soundVolume by workoutViewModel.soundVolume.collectAsStateWithLifecycle()
    val startSoundEnabled by
        workoutViewModel.startSoundEnabled.collectAsStateWithLifecycle()
    val midpointSoundEnabled by
        workoutViewModel.midpointSoundEnabled.collectAsStateWithLifecycle()
    val endSoundEnabled by
        workoutViewModel.endSoundEnabled.collectAsStateWithLifecycle()
    val workoutHistory by workoutViewModel.history.collectAsStateWithLifecycle()
    val trainingPreferences by workoutViewModel.trainingPreferences.collectAsStateWithLifecycle()
    val routineProgress by workoutViewModel.routineProgress.collectAsStateWithLifecycle()
    val freeTimerState by freeTimerViewModel.state.collectAsStateWithLifecycle()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val reminderSettings by reminderViewModel.settings.collectAsStateWithLifecycle()
    val previewPlan = workoutViewModel.previewPlan(recovery)
    val weeklyProgram = workoutViewModel.weeklyProgram(recovery)

    val context = LocalContext.current
    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        healthViewModel.onPermissionsResult(granted)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    val backupDestinationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupViewModel.chooseDestination(uri)
        }
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            backupViewModel.inspectRestore(uri)
        }
    }

    Scaffold(
        containerColor = C.background,
        topBar = {
            if (selectedDestination == null && currentRoute != "workout" && currentRoute != "breathing") {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (currentRoute in setOf(
                            "settings", "library-settings", "category-settings",
                            "exercise-settings", "timer", "program", "health", "training",
                            "progress", "coach", "practice-library/{category}"
                        )
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Text("‹", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (currentRoute) {
                                "settings" -> "Paramètres"
                                "library-settings" -> "Bibliothèque"
                                "category-settings" -> "Catégories"
                                "exercise-settings" -> "Exercices"
                                "timer" -> "Minuteur"
                                "program" -> "Mon programme"
                                "workout" -> "Séance"
                                "breathing" -> "Respiration"
                                else -> selectedDestination?.label ?: "Tempo Coach"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        if (currentRoute != "settings" &&
                            currentRoute != "library-settings" &&
                            currentRoute != "category-settings" &&
                            currentRoute != "exercise-settings" &&
                            currentRoute != "timer" &&
                            currentRoute != "program" &&
                            currentRoute != "workout" &&
                            currentRoute != "breathing"
                        ) {
                            Text(
                                text = "Tempo Coach v" + BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute != "settings" &&
                        currentRoute != "exercise-settings" &&
                        currentRoute != "program" &&
                        currentRoute != "workout" &&
                        currentRoute != "breathing"
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
            }
        },
        bottomBar = {
            if (selectedDestination != null) {
                NavigationBar(containerColor = C.surface, tonalElevation = 0.dp) {
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
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = C.action,
                                selectedTextColor = C.action,
                                unselectedIconColor = C.textMuted,
                                unselectedTextColor = C.textMuted,
                                indicatorColor = C.actionDeep
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
                TodayV21Screen(
                    days = recentDays,
                    checkIn = checkIn,
                    recovery = recovery,
                    plan = previewPlan,
                    routine = routineProgress,
                    syncing = syncing,
                    hasPermissions = hasPermissions,
                    onSync = healthViewModel::sync,
                    onPermissions = {
                        permissionLauncher.launch(healthViewModel.requiredPermissions)
                    },
                    onCheckIn = healthViewModel::saveCheckIn,
                    onToggleHabit = workoutViewModel::toggleRoutineStep,
                    onStart = {
                        workoutViewModel.prepare(recovery)
                        navController.navigate("workout")
                    },
                    onCoach = { navController.navigate("coach") },
                    onHealth = { navController.navigate("health") },
                    onTracking = { navController.navigate("tracking") },
                    onPractice = { navController.navigate("practice") }
                )
            }

            composable("practice") {
                PracticeHubV21Screen(
                    plan = previewPlan,
                    onStart = {
                        workoutViewModel.prepare(recovery)
                        navController.navigate("workout")
                    },
                    onBreathing = { navController.navigate("breathing") },
                    onExplore = { category ->
                        navController.navigate("practice-library/" + category.name)
                    },
                    onProgram = { navController.navigate("program") },
                    onTimer = { navController.navigate("timer") },
                    onManage = { navController.navigate("library-settings") }
                )
            }

            composable("practice-library/{category}") { entry ->
                val category = entry.arguments?.getString("category")?.let {
                    runCatching { ExerciseCategory.valueOf(it) }.getOrNull()
                }
                ExerciseExploreV21Screen(
                    category = category,
                    preferences = trainingPreferences,
                    onStart = { exercise ->
                        workoutViewModel.prepareSingleExercise(exercise)
                        navController.navigate("workout")
                    }
                )
            }

            composable("tracking") {
                TrackingHubV21Screen(
                    days = recentDays,
                    history = workoutHistory,
                    recovery = recovery,
                    onHealth = { navController.navigate("health") },
                    onProgress = { navController.navigate("progress") }
                )
            }

            composable("me") {
                MeHubV21Screen(
                    onSettings = { navController.navigate("settings") },
                    onCategories = { navController.navigate("category-settings") },
                    onExercises = { navController.navigate("exercise-settings") },
                    onHealth = { navController.navigate("health") },
                    onTimer = { navController.navigate("timer") },
                    onUpdates = onCheckForUpdates
                )
            }

            composable("health") {
                HealthScreen(
                    days = recentDays,
                    availability = availability,
                    hasPermissions = hasPermissions,
                    missingPermissionLabels = missingPermissionLabels,
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
                    goal = trainingPreferences.goal,
                    onOpenBreathing = { navController.navigate("breathing") },
                    onOpenProgram = {
                        navController.navigate("program")
                    },
                    onStart = {
                        workoutViewModel.prepare(recovery)
                        navController.navigate("workout")
                    }
                )
            }

            composable("program") {
                ProgramScreen(
                    program = weeklyProgram,
                    selectedGoal = trainingPreferences.goal,
                    onGoalChange = workoutViewModel::setTrainingGoal,
                    onStart = {
                        workoutViewModel.prepare(recovery)
                        navController.navigate("workout")
                    }
                )
            }

            composable("breathing") {
                GuidedBreathingScreen(
                    onExit = { navController.popBackStack() },
                    onRecord = workoutViewModel::recordBreathingSession
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
                CoachScreen(
                    recovery = recovery,
                    latest = recentDays.firstOrNull(),
                    checkIn = checkIn,
                    plan = previewPlan,
                    routine = routineProgress,
                    onToggleRoutine = workoutViewModel::toggleRoutineStep
                )
            }

            composable("settings") {
                SettingsScreen(
                    availability = availability,
                    hasPermissions = hasPermissions,
                    localDays = recentDays.size,
                    soundVolume = soundVolume,
                    startSoundEnabled = startSoundEnabled,
                    midpointSoundEnabled = midpointSoundEnabled,
                    endSoundEnabled = endSoundEnabled,
                    preferences = trainingPreferences,
                    onSoundVolumeChange = workoutViewModel::setSoundVolume,
                    onStartSoundEnabled = workoutViewModel::setStartSoundEnabled,
                    onMidpointSoundEnabled = workoutViewModel::setMidpointSoundEnabled,
                    onEndSoundEnabled = workoutViewModel::setEndSoundEnabled,
                    backupState = backupState,
                    reminderSettings = reminderSettings,
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            notificationPermissionGranted = true
                        }
                    },
                    onMoveReminderEnabled = reminderViewModel::setMoveEnabled,
                    onMoveIntervalHours = reminderViewModel::setMoveIntervalHours,
                    onCheckInReminderEnabled = reminderViewModel::setCheckInEnabled,
                    onCheckInReminderHour = reminderViewModel::setCheckInHour,
                    onMobilityReminderEnabled = reminderViewModel::setMobilityEnabled,
                    onMobilityReminderHour = reminderViewModel::setMobilityHour,
                    onWorkoutReminderEnabled = reminderViewModel::setWorkoutEnabled,
                    onWorkoutReminderHour = reminderViewModel::setWorkoutHour,
                    onChooseBackupDestination = {
                        backupDestinationLauncher.launch(
                            FullBackupManager.DEFAULT_FILE_NAME
                        )
                    },
                    onBackupNow = backupViewModel::backupNow,
                    onRestoreBackup = {
                        backupRestoreLauncher.launch(
                            arrayOf("application/json", "text/plain")
                        )
                    },
                    onConfirmRestore = {
                        backupViewModel.confirmRestore { restored ->
                            if (restored) {
                                workoutViewModel.reloadFromStorage()
                            }
                        }
                    },
                    onCancelRestore = backupViewModel::cancelRestore,
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
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    TempoPanelCard(
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
    saveCheckIn: (Int, Int) -> Unit,
    onOpenTraining: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenLibrary: () -> Unit,
    weeklyProgram: WeeklyProgram,
    routine: RoutineProgress,
    history: List<WorkoutHistoryEntity>,
    onToggleRoutine: (Int) -> Unit,
    onOpenCoach: () -> Unit,
    onOpenProgram: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val energy = checkIn?.energy ?: 3
    val pain = checkIn?.pain ?: 0
    val energyIcons = listOf("😫", "😕", "😐", "🙂", "😁")
    val painLabels = listOf("Aucune", "Légère", "Moyenne", "Importante")

    Screen(
        title = "Mon tableau de bord",
        subtitle = "La santé, les séances et les habitudes au même endroit."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TempoPanelCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Accès rapide", fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onOpenTraining, modifier = Modifier.weight(1f)) {
                            Text("🏋️ Séance")
                        }
                        OutlinedButton(onClick = onOpenTimer, modifier = Modifier.weight(1f)) {
                            Text("⏱️ Minuteur")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onOpenHealth, modifier = Modifier.weight(1f)) {
                            Text("❤️ Santé")
                        }
                        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.weight(1f)) {
                            Text("📚 Exercices")
                        }
                    }
                }
            }
            RecoveryCard(recovery)
            CoachDashboardCard(
                recovery = recovery,
                plan = plan,
                weeklyProgram = weeklyProgram,
                history = history,
                routine = routine,
                onToggleRoutine = onToggleRoutine,
                onOpenCoach = onOpenCoach,
                onOpenProgram = onOpenProgram,
                onStartWorkout = onStartWorkout
            )
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

            TempoPanelCard(
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
private fun CoachDashboardCard(
    recovery: RecoveryResult,
    plan: WorkoutPlan,
    weeklyProgram: WeeklyProgram,
    history: List<WorkoutHistoryEntity>,
    routine: RoutineProgress,
    onToggleRoutine: (Int) -> Unit,
    onOpenCoach: () -> Unit,
    onOpenProgram: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val today = java.time.LocalDate.now().toString()
    val done = if (routine.date == today) routine.completed else emptySet()
    val dailyHabits = listOf(
        "Bouger régulièrement",
        if (recovery.level == RecoveryLevel.RED) "Mobilité très douce ou repos"
        else "Séance ou mobilité",
        "Préparer un coucher régulier"
    )

    TempoPanelCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "🎯 Mon plan du jour",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                weeklyProgram.todayGuidance,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Séance proposée : " + plan.title +
                    " • " + plan.estimatedMinutes + " min",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onStartWorkout, modifier = Modifier.weight(1f)) {
                    Text("▶ Démarrer")
                }
                OutlinedButton(onClick = onOpenCoach, modifier = Modifier.weight(1f)) {
                    Text("🧠 Mon coach")
                }
            }

            Text(
                "Semaine : " + weeklyProgram.completedThisWeek + "/" +
                    weeklyProgram.targetSessions + " séances",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            LinearProgressIndicator(
                progress = {
                    (weeklyProgram.completedThisWeek.toFloat() /
                        weeklyProgram.targetSessions.coerceAtLeast(1).toFloat())
                        .coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = onOpenProgram, modifier = Modifier.fillMaxWidth()) {
                Text("📅 VOIR MON PROGRAMME")
            }

            Text(
                "Habitudes : " + done.size + "/3",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            dailyHabits.forEachIndexed { index, label ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = index in done,
                        onCheckedChange = { onToggleRoutine(index) }
                    )
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            history.firstOrNull()?.let { last ->
                Text(
                    "Dernière séance : " + last.title +
                        " • " + formatClock(last.durationSeconds) +
                        if (last.completed) " ✓" else " • interrompue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
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

    val formLevel = when (recovery.level) {
        RecoveryLevel.GREEN -> "Forme favorable"
        RecoveryLevel.ORANGE -> "Forme moyenne"
        RecoveryLevel.RED -> "Récupération prioritaire"
        RecoveryLevel.UNKNOWN -> "Données insuffisantes"
    }
    StatusCard(
        emoji = emoji,
        title = "Forme & préparation",
        value = recovery.score?.let { it.toString() + " / 100" } ?: "— / 100",
        detail = formLevel +
            if (recovery.score != null) {
                " • fiabilité " + recovery.confidence + "% • " +
                    recovery.baselineDays + " j de référence"
            } else {
                " • Synchronise Santé Connect pour calculer le score"
            }
    )
}

@Composable
private fun RecoveryFactorsCard(recovery: RecoveryResult) {
    if (recovery.factors.isEmpty()) return

    TempoPanelCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Comment est calculé le score de forme ?",
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
    TempoPanelCard(
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
    missingPermissionLabels: List<String>,
    syncing: Boolean,
    message: String?,
    requestPermissions: () -> Unit,
    sync: () -> Unit
) {
    val latest = days.firstOrNull()
    val week = days.take(7)
    val month = days.drop(7).take(21)

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

            if (hasPermissions && missingPermissionLabels.isNotEmpty()) {
                StatusCard(
                    "⚠️",
                    "Données non autorisées",
                    missingPermissionLabels.size.toString() + " type(s)",
                    missingPermissionLabels.joinToString(" • ")
                )
            }

            DetailedHealthMetrics(days)

            StatusCard(
                "🏃",
                "Activité",
                latest?.steps?.let { it.toString() + " pas" } ?: "—",
                latest?.let {
                    distance(it.distanceMeters) + " • " +
                        kcal(it.caloriesKcal) + " • " +
                        (it.exerciseMinutes?.let { v -> v.toString() + " min sport" }
                            ?: "Séances —")
                } ?: "Aucune donnée"
            )

            StatusCard(
                "⚖️",
                "Corps & cardio",
                latest?.weightKg?.let { oneDecimal(it) + " kg" } ?: "Poids —",
                "VO₂ max " + (latest?.vo2Max?.let(::oneDecimal) ?: "—")
            )

            PersonalTrendInsightsCard(days)

            HealthAverageComparisonCard(
                emoji = "😴",
                title = "Sommeil • 7 j vs 21 j précédents",
                weekValues = week.mapNotNull {
                    it.sleepMinutes?.toDouble()
                },
                monthValues = month.mapNotNull {
                    it.sleepMinutes?.toDouble()
                },
                valueText = { value ->
                    formatMinutes(value.toLong())
                }
            )

            HealthAverageComparisonCard(
                emoji = "❤️",
                title = "VFC • 7 j vs 21 j précédents",
                weekValues = week.mapNotNull { it.hrvRmssdMs },
                monthValues = month.mapNotNull { it.hrvRmssdMs },
                valueText = { value ->
                    oneDecimal(value) + " ms"
                }
            )

            HealthAverageComparisonCard(
                emoji = "💓",
                title = "FC repos • 7 j vs 21 j précédents",
                weekValues = week.mapNotNull { it.restingHeartRate },
                monthValues = month.mapNotNull { it.restingHeartRate },
                valueText = { value ->
                    oneDecimal(value) + " bpm"
                },
                lowerIsBetter = true
            )

            HealthAverageComparisonCard(
                emoji = "🚶",
                title = "Pas • 7 j vs 21 j précédents",
                weekValues = week.mapNotNull {
                    it.steps?.toDouble()
                },
                monthValues = month.mapNotNull {
                    it.steps?.toDouble()
                },
                valueText = { value ->
                    value.toLong().toString() + " pas/j"
                }
            )

            TrendCard(
                emoji = "😴",
                title = "Sommeil • 7 jours",
                values = metricByCalendarDay(days, 7) { it.sleepMinutes?.toDouble() },
                valueText = { value -> formatMinutes(value.toLong()) },
                kind = TempoMetricKind.SLEEP,
                reference = days.drop(7).mapNotNull { it.sleepMinutes?.toDouble() }
            )

            TrendCard(
                emoji = "❤️",
                title = "HRV • 7 jours",
                values = metricByCalendarDay(days, 7) { it.hrvRmssdMs },
                valueText = { value -> oneDecimal(value) + " ms" },
                kind = TempoMetricKind.HRV,
                reference = days.drop(7).mapNotNull { it.hrvRmssdMs }
            )
            TrendCard(
                emoji = "💓",
                title = "FC repos • 7 jours",
                values = metricByCalendarDay(days, 7) { it.restingHeartRate },
                valueText = { value -> value.toInt().toString() + " bpm" },
                kind = TempoMetricKind.RESTING_PULSE,
                reference = days.drop(7).mapNotNull { it.restingHeartRate }
            )
            TrendCard(
                emoji = "🌬️",
                title = "Respiration • 7 jours",
                values = metricByCalendarDay(days, 7) { it.respiratoryRate },
                valueText = { value -> oneDecimal(value) + " /min" },
                kind = TempoMetricKind.OTHER
            )
        }
    }
}

@Composable
private fun TrainingScreen(
    recovery: RecoveryResult,
    plan: WorkoutPlan,
    history: List<WorkoutHistoryEntity>,
    goal: TrainingGoal,
    onOpenProgram: () -> Unit,
    onOpenBreathing: () -> Unit,
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

            OutlinedButton(
                onClick = onOpenBreathing,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("🫁 RESPIRATION GUIDÉE • 2 À 4 MIN")
            }

            OutlinedButton(
                onClick = onOpenProgram,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📅 MON PROGRAMME • " + trainingGoalLabel(goal).uppercase())
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
private fun ProgramScreen(
    program: WeeklyProgram,
    selectedGoal: TrainingGoal,
    onGoalChange: (TrainingGoal) -> Unit,
    onStart: () -> Unit
) {
    Screen(
        title = "Mon programme",
        subtitle = "Une semaine adaptée à ton objectif, ton historique et ta récupération."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mon objectif", fontWeight = FontWeight.Bold)

            TrainingGoal.entries.chunked(2).forEach { rowGoals ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowGoals.forEach { goal ->
                        AssistChip(
                            onClick = { onGoalChange(goal) },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    if (selectedGoal == goal) {
                                        "• " + trainingGoalLabel(goal)
                                    } else {
                                        trainingGoalLabel(goal)
                                    }
                                )
                            }
                        )
                    }
                    if (rowGoals.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            StatusCard(
                "🎯",
                "Semaine " + program.weekNumber,
                "Niveau " + program.level + "/4",
                program.completedThisWeek.toString() + "/" +
                    program.targetSessions + " séance(s) faite(s)" +
                    if (program.missedThisWeek > 0) {
                        " • " + program.missedThisWeek + " manquée(s)"
                    } else {
                        ""
                    }
            )

            TempoPanelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Aujourd'hui",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        program.todayGuidance,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LANCER LA SÉANCE ADAPTÉE")
                    }
                }
            }

            Text("Cette semaine", fontWeight = FontWeight.Bold)

            program.days.forEach { day ->
                val icon = when {
                    day.completed -> "✅"
                    day.missed -> "↩️"
                    day.kind == ProgramDayKind.SESSION -> "🏋️"
                    day.kind == ProgramDayKind.MOBILITY -> "🤸"
                    else -> "🌿"
                }

                StatusCard(
                    icon,
                    day.label + " • " + day.title,
                    day.minutes.toString() + " min",
                    when {
                        day.completed -> "Terminé"
                        day.missed -> "Manqué • pas besoin de doubler la prochaine séance"
                        day.kind == ProgramDayKind.SESSION -> "Séance prévue"
                        day.kind == ProgramDayKind.MOBILITY -> "Mobilité / récupération active"
                        else -> "Récupération"
                    }
                )
            }

            Text(
                "La progression monte progressivement si les séances sont régulières et bien tolérées. Une difficulté élevée réduit automatiquement le niveau suivant.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val completedAll = history.filter { it.completed }
    val totalTrainingSeconds = completedAll.sumOf { it.durationSeconds }
    val averageDifficulty = completedAll
        .mapNotNull { it.perceivedDifficulty }
        .takeIf { it.isNotEmpty() }
        ?.average()
    val longestWorkout = completedAll.maxByOrNull { it.durationSeconds }

    Screen(
        title = "Progression",
        subtitle = "Santé et entraînement sont maintenant suivis ensemble."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "🏆",
                "Bilan entraînement",
                completedAll.size.toString() + " séance(s) terminée(s)",
                "Temps total " + formatClock(totalTrainingSeconds) +
                    (averageDifficulty?.let {
                        " • difficulté moyenne " + oneDecimal(it) + "/5"
                    } ?: "")
            )

            if (longestWorkout != null) {
                StatusCard(
                    "⏱️",
                    "Plus longue séance",
                    formatClock(longestWorkout.durationSeconds),
                    longestWorkout.title
                )
            }

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
                values = metricByCalendarDay(days, 28) { it.sleepMinutes?.toDouble() },
                valueText = { value -> formatMinutes(value.toLong()) },
                kind = TempoMetricKind.SLEEP,
                reference = days.drop(7).mapNotNull { it.sleepMinutes?.toDouble() }
            )

            TrendCard(
                emoji = "📈",
                title = "HRV • 28 jours",
                values = metricByCalendarDay(days, 28) { it.hrvRmssdMs },
                valueText = { value -> oneDecimal(value) + " ms" },
                kind = TempoMetricKind.HRV,
                reference = days.drop(7).mapNotNull { it.hrvRmssdMs }
            )

            TrendCard(
                emoji = "😴",
                title = "Sommeil • 7 jours",
                values = metricByCalendarDay(days, 7) { it.sleepMinutes?.toDouble() },
                valueText = { value -> formatMinutes(value.toLong()) },
                kind = TempoMetricKind.SLEEP,
                reference = days.drop(7).mapNotNull { it.sleepMinutes?.toDouble() }
            )

            TrendCard(
                emoji = "❤️",
                title = "HRV • 7 jours",
                values = metricByCalendarDay(days, 7) { it.hrvRmssdMs },
                valueText = { value -> oneDecimal(value) + " ms" },
                kind = TempoMetricKind.HRV,
                reference = days.drop(7).mapNotNull { it.hrvRmssdMs }
            )

            TrendCard(
                emoji = "💓",
                title = "FC repos • 7 jours",
                values = metricByCalendarDay(days, 7) { it.restingHeartRate },
                valueText = { value -> value.toInt().toString() + " bpm" },
                kind = TempoMetricKind.RESTING_PULSE,
                reference = days.drop(7).mapNotNull { it.restingHeartRate }
            )

            TrendCard(
                emoji = "🚶",
                title = "Pas • 7 jours",
                values = metricByCalendarDay(days, 7) { it.steps?.toDouble() },
                valueText = { value -> value.toLong().toString() },
                kind = TempoMetricKind.STEPS
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
private fun PersonalTrendInsightsCard(days: List<DailyHealthEntity>) {
    val recent = days.take(7)
    val previous = days.drop(7).take(21)
    fun averageOrNull(values: List<Double>, minimum: Int): Double? =
        values.takeIf { it.size >= minimum }?.average()

    val recentSleep = averageOrNull(recent.mapNotNull { it.sleepMinutes?.toDouble() }, 4)
    val previousSleep = averageOrNull(previous.mapNotNull { it.sleepMinutes?.toDouble() }, 7)
    val recentHrv = averageOrNull(recent.mapNotNull { it.hrvRmssdMs }, 4)
    val previousHrv = averageOrNull(previous.mapNotNull { it.hrvRmssdMs }, 7)
    val recentPulse = averageOrNull(recent.mapNotNull { it.restingHeartRate }, 4)
    val previousPulse = averageOrNull(previous.mapNotNull { it.restingHeartRate }, 7)

    val unusualSleep = recentSleep != null && previousSleep != null &&
        recentSleep < previousSleep - 35
    val unusualHrv = recentHrv != null && previousHrv != null &&
        previousHrv > 0 && recentHrv < previousHrv * 0.85
    val unusualPulse = recentPulse != null && previousPulse != null &&
        recentPulse >= previousPulse + 5

    val guidance = when {
        listOf(recentSleep, previousSleep, recentHrv, previousHrv, recentPulse, previousPulse)
            .count { it != null } < 4 ->
            "Données encore limitées : synchronise régulièrement pour obtenir une tendance fiable."
        unusualSleep && (unusualHrv || unusualPulse) ->
            "Sommeil réduit et signaux cardio moins favorables : allège temporairement " +
                "l'entraînement et suis ton ressenti."
        unusualSleep ->
            "Ton sommeil a diminué : protège les nuits à venir et ajuste l'intensité selon ton énergie."
        unusualHrv && unusualPulse ->
            "VFC plus basse et fréquence cardiaque de repos plus haute : privilégie " +
                "une activité facile et surveille l'évolution sur plusieurs jours."
        unusualHrv || unusualPulse ->
            "Un signal cardio s'écarte de ton niveau habituel. Une journée isolée " +
                "ne suffit pas à conclure : observe la tendance."
        else ->
            "Pas de variation défavorable marquée parmi les indicateurs disponibles. " +
                "Continue à écouter ton ressenti."
    }
    StatusCard(
        emoji = "🧭",
        title = "Bilan des tendances personnelles",
        value = "7 derniers jours",
        detail = guidance + " Analyse indicative, pas un diagnostic."
    )
}

@Composable
private fun HealthAverageComparisonCard(
    emoji: String,
    title: String,
    weekValues: List<Double>,
    monthValues: List<Double>,
    valueText: (Double) -> String,
    lowerIsBetter: Boolean = false
) {
    if (weekValues.size < 4 || monthValues.size < 7) {
        StatusCard(
            emoji, title, "Référence en construction",
            "Au moins 4 mesures récentes et 7 mesures précédentes nécessaires."
        )
        return
    }

    val weekAverage = weekValues.average()
    val monthAverage = monthValues.average()
    val delta = weekAverage - monthAverage
    val threshold = kotlin.math.abs(monthAverage) * 0.03

    val trend = when {
        kotlin.math.abs(delta) <= threshold -> "≈ stable"
        (delta > 0 && !lowerIsBetter) ||
            (delta < 0 && lowerIsBetter) -> "↗ favorable"
        else -> "↘ à surveiller"
    }

    StatusCard(
        emoji,
        title,
        valueText(weekAverage),
        "21 j précédents : " + valueText(monthAverage) + " • " + trend +
            " • " + weekValues.size + "/" + monthValues.size + " mesures"
    )
}

@Composable
private fun TrendCard(
    emoji: String,
    title: String,
    values: List<Double?>,
    valueText: (Double) -> String,
    kind: TempoMetricKind,
    reference: List<Double> = emptyList()
) {
    val valid = values.filterNotNull().filter { it.isFinite() }
    val latest = values.lastOrNull { it != null && it.isFinite() }
    val usual = TempoHealthSignals.reference(reference)
    val signal = TempoHealthSignals.assess(kind, latest, usual)
    val accent = TempoHealthSignals.displayColor(kind, signal)
    val chartKind = if (kind == TempoMetricKind.STEPS ||
        kind == TempoMetricKind.ACTIVE_MINUTES || kind == TempoMetricKind.SLEEP)
        TempoChartKind.BARS else TempoChartKind.LINE

    TempoPanelCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, accent.copy(alpha = 0.36f))
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(latest?.let(valueText) ?: "—", color = C.text,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            if (valid.isEmpty()) {
                Text("Pas de mesure pour cette période",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                TempoTrendGraph(
                    values = values, metric = kind, usual = usual,
                    kind = chartKind, height = 92.dp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                val statusColor = if (signal == TempoSignal.UNKNOWN) C.unknown
                    else TempoHealthSignals.color(signal)
                Box(
                    modifier = Modifier.size(8.dp).background(statusColor, CircleShape)
                )
                Text(
                    TempoHealthSignals.label(kind, signal) + " • " +
                        valid.size.toString() + "/" + values.size + " jours",
                    style = MaterialTheme.typography.labelMedium, color = C.textSecondary
                )
            }
            if (kind == TempoMetricKind.HRV || kind == TempoMetricKind.RESTING_PULSE ||
                kind == TempoMetricKind.SLEEP) {
                Text("Couleurs relatives à ta référence personnelle, sans diagnostic.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CoachScreen(
    recovery: RecoveryResult,
    latest: DailyHealthEntity?,
    checkIn: DailyCheckInEntity?,
    plan: WorkoutPlan,
    routine: RoutineProgress,
    onToggleRoutine: (Int) -> Unit
) {
    val advice = DailyCoachEngine.build(
        recovery = recovery,
        latest = latest,
        checkIn = checkIn,
        workoutPlan = plan
    )

    Screen(
        title = "Coach du jour",
        subtitle = "Conseils locaux basés sur tes données, ton ressenti et ton historique."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                advice.emoji,
                advice.headline,
                recovery.score?.let { it.toString() + " / 100" } ?: "En attente",
                "Confiance " + recovery.confidence + "% • " +
                    recovery.baselineDays + " jour(s) de référence"
            )

            TempoPanelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val steps = listOf(
                        "Bouger 2 minutes plusieurs fois dans la journée",
                        if (recovery.level == RecoveryLevel.RED) {
                            "Faire 5 minutes de mobilité douce ou de repos"
                        } else {
                            "Prendre un moment pour une séance ou 5 minutes de mobilité"
                        },
                        "Préparer un coucher régulier et un moment sans écran"
                    )
                    val completedToday = if (routine.date == java.time.LocalDate.now().toString()) {
                        routine.completed
                    } else {
                        emptySet()
                    }
                    Text(
                        "Mes 3 habitudes du jour • " + completedToday.size + "/3",
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { completedToday.size / 3f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    steps.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = index in completedToday,
                                onCheckedChange = { onToggleRoutine(index) }
                            )
                            Text(label, modifier = Modifier.weight(1f))
                        }
                    }
                    Text(
                        "Une nouvelle liste commence chaque jour. " +
                            "Les coches sont conservées dans tes sauvegardes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            StatusCard(
                "📊",
                "Par rapport à tes habitudes",
                if (recovery.baselineDays >= 7) "Référence personnelle" else "En construction",
                advice.personalSummary
            )

            StatusCard(
                "🏋️",
                "Activité conseillée",
                plan.title,
                advice.sessionGuidance
            )

            TempoPanelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Pourquoi ?",
                        fontWeight = FontWeight.Bold
                    )
                    advice.reasons.forEach { reason ->
                        Text(
                            "• " + reason,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            TempoPanelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "À faire aujourd'hui",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    advice.actions.forEach { action ->
                        Text(
                            "✓ " + action,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            StatusCard(
                "🌙",
                "Ce soir",
                "Préparer demain",
                advice.evening
            )

            Text(
                "Tempo donne des conseils de bien-être et d'entraînement, pas un diagnostic médical.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    startSoundEnabled: Boolean,
    midpointSoundEnabled: Boolean,
    endSoundEnabled: Boolean,
    preferences: TrainingPreferences,
    onSoundVolumeChange: (Int) -> Unit,
    onStartSoundEnabled: (Boolean) -> Unit,
    onMidpointSoundEnabled: (Boolean) -> Unit,
    onEndSoundEnabled: (Boolean) -> Unit,
    backupState: BackupUiState,
    reminderSettings: ReminderSettings,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onMoveReminderEnabled: (Boolean) -> Unit,
    onMoveIntervalHours: (Int) -> Unit,
    onCheckInReminderEnabled: (Boolean) -> Unit,
    onCheckInReminderHour: (Int) -> Unit,
    onMobilityReminderEnabled: (Boolean) -> Unit,
    onMobilityReminderHour: (Int) -> Unit,
    onWorkoutReminderEnabled: (Boolean) -> Unit,
    onWorkoutReminderHour: (Int) -> Unit,
    onChooseBackupDestination: () -> Unit,
    onBackupNow: () -> Unit,
    onRestoreBackup: () -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onDurationChange: (Int) -> Unit,
    onToggleEquipment: (Equipment) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTimer: () -> Unit,
    onCheckForUpdates: () -> Unit
) {
    var pendingConfirmation by remember { mutableStateOf<ConfirmationRequest?>(null) }

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

            TempoPanelCard(
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

            TempoPanelCard(
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

                    Spacer(Modifier.height(10.dp))

                    Text("Signaux actifs", fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                onStartSoundEnabled(!startSoundEnabled)
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    if (startSoundEnabled) "• Début" else "Début"
                                )
                            }
                        )
                        AssistChip(
                            onClick = {
                                onMidpointSoundEnabled(!midpointSoundEnabled)
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    if (midpointSoundEnabled) "• Mi-temps" else "Mi-temps"
                                )
                            }
                        )
                        AssistChip(
                            onClick = {
                                onEndSoundEnabled(!endSoundEnabled)
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    if (endSoundEnabled) "• Fin" else "Fin"
                                )
                            }
                        )
                    }
                }
            }

            TempoPanelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔔 Rappels et routines", fontWeight = FontWeight.Bold)
                    Text(
                        if (notificationPermissionGranted) {
                            "Notifications autorisées"
                        } else {
                            "Notifications non autorisées"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (notificationPermissionGranted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )

                    if (!notificationPermissionGranted) {
                        OutlinedButton(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("AUTORISER LES NOTIFICATIONS")
                        }
                    }

                    ReminderSettingRow(
                        title = "Bouger",
                        detail = "Toutes les " +
                            reminderSettings.moveIntervalHours + " h • 8h–21h",
                        enabled = reminderSettings.moveEnabled,
                        onToggle = onMoveReminderEnabled
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 4).forEach { hours ->
                            AssistChip(
                                onClick = { onMoveIntervalHours(hours) },
                                modifier = Modifier.weight(1f),
                                label = {
                                    Text(
                                        if (reminderSettings.moveIntervalHours == hours) {
                                            "• " + hours + "h"
                                        } else {
                                            hours.toString() + "h"
                                        }
                                    )
                                }
                            )
                        }
                    }

                    ReminderSettingRow(
                        title = "Check-in",
                        detail = "Tous les jours à " +
                            reminderSettings.checkInHour + "h",
                        enabled = reminderSettings.checkInEnabled,
                        onToggle = onCheckInReminderEnabled
                    )
                    HourSelector(
                        hour = reminderSettings.checkInHour,
                        onHourChange = onCheckInReminderHour
                    )

                    ReminderSettingRow(
                        title = "Mobilité / étirements",
                        detail = "Tous les jours à " +
                            reminderSettings.mobilityHour + "h",
                        enabled = reminderSettings.mobilityEnabled,
                        onToggle = onMobilityReminderEnabled
                    )
                    HourSelector(
                        hour = reminderSettings.mobilityHour,
                        onHourChange = onMobilityReminderHour
                    )

                    ReminderSettingRow(
                        title = "Séance",
                        detail = "Lun / mer / ven à " +
                            reminderSettings.workoutHour + "h",
                        enabled = reminderSettings.workoutEnabled,
                        onToggle = onWorkoutReminderEnabled
                    )
                    HourSelector(
                        hour = reminderSettings.workoutHour,
                        onHourChange = onWorkoutReminderHour
                    )
                }
            }

            TempoPanelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "☁️ Sauvegarde complète",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Réglages, exercices perso, historique des séances, check-ins et cache Santé.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val backupStatusLabel = when {
                        !backupState.configured -> "À configurer"
                        !backupState.accessible -> "Fichier inaccessible"
                        backupState.stale -> "Sauvegarde trop ancienne"
                        else -> "Protégée"
                    }

                    Text(
                        "État : " + backupStatusLabel,
                        color = if (
                            backupState.configured &&
                            backupState.accessible &&
                            !backupState.stale
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    backupState.lastBackupEpochMs?.let { timestamp ->
                        Text(
                            "Dernière sauvegarde : " +
                                SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date(timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (backupState.configured) {
                                pendingConfirmation = ConfirmationRequest(
                                    title = "Changer le fichier Drive ?",
                                    message = "La prochaine sauvegarde utilisera le nouveau fichier. " +
                                        "Le fichier actuel restera présent sur Drive.",
                                    confirmLabel = "CHANGER",
                                    action = onChooseBackupDestination
                                )
                            } else {
                                onChooseBackupDestination()
                            }
                        },
                        enabled = !backupState.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (backupState.configured) {
                                "CHANGER LE FICHIER DRIVE"
                            } else {
                                "CHOISIR LE FICHIER DRIVE"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            pendingConfirmation = ConfirmationRequest(
                                title = "Remplacer la sauvegarde actuelle ?",
                                message = "Une nouvelle sauvegarde sera écrite dans le fichier " +
                                    "sélectionné. Son contenu précédent pourra être remplacé.",
                                confirmLabel = "SAUVEGARDER",
                                action = onBackupNow
                            )
                        },
                        enabled = backupState.configured && !backupState.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (backupState.busy) {
                                "SAUVEGARDE EN COURS…"
                            } else {
                                "SAUVEGARDER MAINTENANT"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = onRestoreBackup,
                        enabled = !backupState.busy &&
                            backupState.restorePreview == null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("RESTAURER DEPUIS UN FICHIER")
                    }

                    backupState.restorePreview?.let { preview ->
                        TempoPanelCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Vérification avant restauration",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    preview.healthDays.toString() + " jours santé • " +
                                        preview.checkIns + " check-in(s) • " +
                                        preview.workouts + " séance(s)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                preview.appVersion?.let { version ->
                                    Text(
                                        "Créée avec Tempo Health " + version,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onCancelRestore,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("ANNULER")
                                    }
                                    Button(
                                        onClick = {
                                            pendingConfirmation = ConfirmationRequest(
                                                title = "Restaurer cette sauvegarde ?",
                                                message = "Cette opération remplacera les données " +
                                                    "actuelles par celles du fichier sélectionné (" +
                                                    preview.healthDays + " jours santé, " +
                                                    preview.checkIns + " check-ins et " +
                                                    preview.workouts + " séances). " +
                                                    "Les modifications locales plus récentes " +
                                                    "pourraient être perdues.",
                                                confirmLabel = "RESTAURER",
                                                action = onConfirmRestore
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("RESTAURER")
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "Tempo sauvegarde automatiquement après les changements importants et quand l'application passe en arrière-plan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    backupState.message?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
    ConfirmSensitiveAction(
        request = pendingConfirmation,
        onDismiss = { pendingConfirmation = null }
    )
}

@Composable
private fun ReminderSettingRow(
    title: String,
    detail: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AssistChip(
            onClick = { onToggle(!enabled) },
            label = { Text(if (enabled) "• Actif" else "Inactif") }
        )
    }
}

@Composable
private fun HourSelector(
    hour: Int,
    onHourChange: (Int) -> Unit
) {
    Slider(
        value = hour.toFloat(),
        onValueChange = { onHourChange(it.toInt()) },
        valueRange = 6f..22f,
        steps = 15
    )
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
    var showResetConfirmation by remember { mutableStateOf(false) }

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
                onClick = { showResetConfirmation = true },
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
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Réinitialiser les personnalisations ?") },
            text = {
                Text(
                    "Les catégories seront réactivées, leurs icônes et les durées " +
                        "personnalisées seront réinitialisées. " +
                        "Les exercices créés ou modifiés, les favoris et les exercices " +
                        "à éviter seront conservés."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onReset()
                    }
                ) {
                    Text("RÉINITIALISER")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("ANNULER")
                }
            }
        )
    }
}
@Composable
private fun CategorySettingsScreen(
    preferences: TrainingPreferences,
    onToggle: (ExerciseCategory) -> Unit,
    onSetIcon: (ExerciseCategory, String) -> Unit,
    onResetIcon: (ExerciseCategory) -> Unit
) {
    var pendingConfirmation by remember { mutableStateOf<ConfirmationRequest?>(null) }

    Screen(
        title = "Modifier les catégories",
        subtitle = "Active, désactive ou change l'icône de chaque catégorie."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExerciseCategory.entries.forEach { category ->
                val active = category !in preferences.disabledCategories
                val icon = categoryIcon(category, preferences)
                val count = ExerciseLibrary.all.count { it.category == category }

                TempoPanelCard(
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
                                onClick = {
                                    pendingConfirmation = ConfirmationRequest(
                                        title = "Réinitialiser l'icône ?",
                                        message = "L'icône de « " + categoryLabel(category) +
                                            " » sera remplacée par l'icône par défaut.",
                                        confirmLabel = "RÉINITIALISER",
                                        action = { onResetIcon(category) }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = category in preferences.categoryIcons
                            ) {
                                Text("ICÔNE DÉFAUT")
                            }

                            Button(
                                onClick = {
                                    if (active) {
                                        pendingConfirmation = ConfirmationRequest(
                                            title = "Désactiver cette catégorie ?",
                                            message = "« " + categoryLabel(category) + " » ne sera " +
                                                "plus utilisée pour générer des séances. " +
                                                "Tu pourras la réactiver plus tard.",
                                            confirmLabel = "DÉSACTIVER",
                                            action = { onToggle(category) }
                                        )
                                    } else {
                                        onToggle(category)
                                    }
                                },
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
    ConfirmSensitiveAction(
        request = pendingConfirmation,
        onDismiss = { pendingConfirmation = null }
    )
}

@Composable
private fun FreeTimerScreen(
    state: FreeTimerUiState,
    onMinutesChange: (Int) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    var pendingConfirmation by remember { mutableStateOf<ConfirmationRequest?>(null) }
    val total = state.durationSeconds.coerceAtLeast(1)
    val progress = (state.remainingSeconds.toFloat() / total.toFloat())
        .coerceIn(0f, 1f)

    Screen(
        title = "Minuteur libre",
        subtitle = "Règle une durée, lance le chrono et Tempo sonne à la fin."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TempoPanelCard(
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

            TempoPanelCard(
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

                    listOf(1, 3, 5, 10, 15, 20)
                        .chunked(3)
                        .forEach { minutesRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                minutesRow.forEach { minutes ->
                                    AssistChip(
                                        onClick = { onMinutesChange(minutes) },
                                        enabled = !state.running,
                                        modifier = Modifier.weight(1f),
                                        label = {
                                            Text(minutes.toString() + " min")
                                        }
                                    )
                                }
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
                onClick = {
                    pendingConfirmation = ConfirmationRequest(
                        title = "Réinitialiser le minuteur ?",
                        message = "La durée restante et la progression du minuteur seront perdues.",
                        confirmLabel = "RÉINITIALISER",
                        action = onReset
                    )
                },
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
    ConfirmSensitiveAction(
        request = pendingConfirmation,
        onDismiss = { pendingConfirmation = null }
    )
}

@Composable
private fun ExercisePreferencesScreen(
    preferences: TrainingPreferences,
    onFavorite: (String) -> Unit,
    onAvoid: (String) -> Unit,
    onWorkSecondsChange: (String, Int) -> Unit,
    onResetWorkSeconds: (String) -> Unit,
    onSaveExercise: (Exercise) -> Unit,
    onAddExercise: () -> Exercise,
    onDeleteExercise: (String) -> Unit,
    onResetExercise: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ExerciseCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var newExerciseDraft by remember { mutableStateOf<Exercise?>(null) }

    val allExercises = ExerciseLibrary.effective(preferences)
    val editedExercise = newExerciseDraft?.takeIf { it.id == editingId }
        ?: allExercises.firstOrNull { it.id == editingId }

    if (editedExercise != null) {
        // Écran dédié : la saisie commence toujours en haut, même après avoir parcouru la liste.
        ExerciseDraftScreen(
            exercise = editedExercise,
            isNew = newExerciseDraft?.id == editedExercise.id,
            hasOverride = editedExercise.id in preferences.exerciseOverrides,
            onSave = { exercise ->
                onSaveExercise(exercise)
                editingId = null
                newExerciseDraft = null
            },
            onCancel = {
                // Ne jamais persister un brouillon fermé ou annulé.
                editingId = null
                newExerciseDraft = null
            },
            onDelete = {
                onDeleteExercise(editedExercise.id)
                editingId = null
                newExerciseDraft = null
            },
            onReset = {
                onResetExercise(editedExercise.id)
                editingId = null
                newExerciseDraft = null
            }
        )
        return
    }

    val visibleExercises = allExercises.filter { exercise ->
        (selectedCategory == null || exercise.category == selectedCategory) &&
            (searchQuery.isBlank() ||
                exercise.name.contains(searchQuery.trim(), ignoreCase = true)) &&
            (!favoritesOnly || exercise.id in preferences.favoriteExerciseIds)
    }

    Screen(
        title = "Modifier les exercices",
        subtitle = "Crée, modifie ou exclus des exercices. Enregistrement uniquement sur validation."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val draft = onAddExercise()
                    newExerciseDraft = draft
                    editingId = draft.id
                    selectedCategory = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋ AJOUTER UN EXERCICE")
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Rechercher un exercice") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            AssistChip(
                onClick = { favoritesOnly = !favoritesOnly },
                label = {
                    Text(if (favoritesOnly) "★ Favoris uniquement" else "☆ Voir mes favoris")
                }
            )
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
                TempoPanelCard(
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

                        val workSeconds = preferences.exerciseWorkSeconds[exercise.id]
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

@Composable
private fun ExerciseDraftScreen(
    exercise: Exercise,
    isNew: Boolean,
    hasOverride: Boolean,
    onSave: (Exercise) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit
) {
    // Ces états restent dans l'éditeur et ne sont écrits en base qu'après ENREGISTRER.
    var pendingConfirmation by remember(exercise.id) { mutableStateOf<ConfirmationRequest?>(null) }
    var name by remember(exercise.id) { mutableStateOf(exercise.name) }
    var instructions by remember(exercise.id) { mutableStateOf(exercise.instructions) }
    var cue by remember(exercise.id) { mutableStateOf(exercise.cue) }
    var category by remember(exercise.id) { mutableStateOf(exercise.category) }
    var equipment by remember(exercise.id) { mutableStateOf(exercise.requiredEquipment) }
    var seconds by remember(exercise.id) { mutableStateOf(exercise.defaultWorkSeconds) }

    val requestCancel = {
        val hasChanges = name != exercise.name ||
            instructions != exercise.instructions ||
            cue != exercise.cue ||
            category != exercise.category ||
            equipment != exercise.requiredEquipment ||
            seconds != exercise.defaultWorkSeconds
        if (hasChanges) {
            pendingConfirmation = ConfirmationRequest(
                title = "Abandonner les modifications ?",
                message = "Les changements non enregistrés de « " +
                    exercise.name + " » seront perdus.",
                confirmLabel = "ABANDONNER",
                action = onCancel
            )
        } else {
            onCancel()
        }
    }
    BackHandler(onBack = requestCancel)

    Screen(
        title = if (isNew) "Ajouter un exercice" else "Modifier l'exercice",
        subtitle = "Les changements ne sont conservés que si tu appuies sur Enregistrer."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                Text(if (equipment == item) "• " + label else label)
                            }
                        )
                    }
            }

            Text("Durée : " + seconds + " s", fontWeight = FontWeight.SemiBold)
            Slider(
                value = seconds.toFloat(),
                onValueChange = { seconds = it.toInt() },
                valueRange = 15f..120f,
                steps = 20
            )

            Button(
                onClick = {
                    onSave(
                        exercise.copy(
                            name = name.trim().ifBlank { "Exercice" },
                            category = category,
                            defaultWorkSeconds = seconds,
                            instructions = instructions.trim(),
                            cue = cue.trim(),
                            requiredEquipment = equipment
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ENREGISTRER")
            }

            OutlinedButton(
                onClick = requestCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ANNULER SANS ENREGISTRER")
            }

            if (!isNew && exercise.id.startsWith("user-")) {
                OutlinedButton(
                    onClick = {
                        pendingConfirmation = ConfirmationRequest(
                            title = "Supprimer cet exercice ?",
                            message = "« " + exercise.name + " » sera définitivement supprimé " +
                                "de ta bibliothèque. Cette action est irréversible.",
                            confirmLabel = "SUPPRIMER",
                            action = onDelete
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SUPPRIMER CET EXERCICE")
                }
            } else if (!isNew && hasOverride) {
                OutlinedButton(
                    onClick = {
                        pendingConfirmation = ConfirmationRequest(
                            title = "Revenir à la version d'origine ?",
                            message = "Les personnalisations enregistrées de « " +
                                exercise.name + " » seront effacées. " +
                                "Les valeurs d'origine seront restaurées.",
                            confirmLabel = "RÉINITIALISER",
                            action = onReset
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("REVENIR À LA VERSION D'ORIGINE")
                }
            }
        }
    }
    ConfirmSensitiveAction(
        request = pendingConfirmation,
        onDismiss = { pendingConfirmation = null }
    )
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
    var pendingConfirmation by remember { mutableStateOf<ConfirmationRequest?>(null) }
    BackHandler(enabled = plan != null) {
        if (state.phase == WorkoutPhase.COMPLETE || state.phase == WorkoutPhase.READY) {
            onExit()
        } else {
            pendingConfirmation = ConfirmationRequest(
                title = "Quitter la séance ?",
                message = "La séance en cours sera arrêtée. " +
                    "Tu perdras la progression restante.",
                confirmLabel = "QUITTER",
                action = onExit
            )
        }
    }

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
                        onClick = {
                                pendingConfirmation = ConfirmationRequest(
                                    title = "Remplacer cet exercice ?",
                                    message = "L'exercice sélectionné sera remplacé dans la séance.",
                                    confirmLabel = "REMPLACER",
                                    action = onReplace
                                )
                            },
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

                    TempoPanelCard(
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

                    TempoPanelCard(
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
                            onClick = {
                                pendingConfirmation = ConfirmationRequest(
                                    title = "Passer cet exercice ?",
                                    message = "L'exercice en cours sera passé et la séance avancera.",
                                    confirmLabel = "PASSER",
                                    action = onSkip
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PASSER")
                        }
                    }

                    if (state.phase != WorkoutPhase.REST) {
                        OutlinedButton(
                            onClick = {
                                pendingConfirmation = ConfirmationRequest(
                                    title = "Remplacer cet exercice ?",
                                    message = "L'exercice sélectionné sera remplacé dans la séance.",
                                    confirmLabel = "REMPLACER",
                                    action = onReplace
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔄 REMPLACER CET EXERCICE")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            pendingConfirmation = ConfirmationRequest(
                                title = "Quitter la séance ?",
                                message = "La séance en cours sera arrêtée. " +
                                    "Tu perdras la progression restante.",
                                confirmLabel = "QUITTER",
                                action = onExit
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("QUITTER LA SÉANCE")
                    }
                }
            }
        }
    }
    ConfirmSensitiveAction(
        request = pendingConfirmation,
        onDismiss = { pendingConfirmation = null }
    )
}

private fun trainingGoalLabel(goal: TrainingGoal): String =
    when (goal) {
        TrainingGoal.PAIN_FREE -> "Moins de douleurs"
        TrainingGoal.MOBILITY -> "Mobilité"
        TrainingGoal.FITNESS -> "Remise en forme"
        TrainingGoal.STRENGTH -> "Renforcement"
        TrainingGoal.RELAXATION -> "Détente"
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
        ExerciseCategory.BREATHING -> "Respiration"
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
        ExerciseCategory.BREATHING -> "🫁"
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
