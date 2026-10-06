package fr.tempo.health.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.tempo.health.BuildConfig
import fr.tempo.health.data.DailyHealthEntity
import fr.tempo.health.data.HealthConnectAvailability
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
    healthViewModel: HealthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val selectedDestination = mainDestinations.firstOrNull { it.route == currentRoute }
    val showBottomBar = selectedDestination != null

    val recentDays by healthViewModel.recentDays.collectAsStateWithLifecycle()
    val availability by healthViewModel.availability.collectAsStateWithLifecycle()
    val hasPermissions by healthViewModel.hasPermissions.collectAsStateWithLifecycle()
    val syncing by healthViewModel.syncing.collectAsStateWithLifecycle()
    val message by healthViewModel.message.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        healthViewModel.onPermissionsResult(granted)
    }

    val requestPermissions = {
        permissionLauncher.launch(healthViewModel.requiredPermissions)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (currentRoute == "settings") "Paramètres"
                            else selectedDestination?.label ?: "Tempo Health",
                            fontWeight = FontWeight.SemiBold
                        )
                        if (currentRoute != "settings") {
                            Text(
                                text = "Tempo Health v" + BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute != "settings") {
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
            if (showBottomBar) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("today") {
                TodayScreen(
                    latest = recentDays.firstOrNull(),
                    hasPermissions = hasPermissions,
                    syncing = syncing,
                    requestPermissions = requestPermissions,
                    sync = healthViewModel::sync
                )
            }
            composable("health") {
                HealthScreen(
                    days = recentDays,
                    availability = availability,
                    hasPermissions = hasPermissions,
                    syncing = syncing,
                    message = message,
                    requestPermissions = requestPermissions,
                    sync = healthViewModel::sync
                )
            }
            composable("training") { TrainingScreen() }
            composable("progress") { ProgressScreen(recentDays) }
            composable("coach") { CoachScreen() }
            composable("settings") {
                SettingsScreen(
                    availability = availability,
                    hasPermissions = hasPermissions,
                    localDays = recentDays.size
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
    hasPermissions: Boolean,
    syncing: Boolean,
    requestPermissions: () -> Unit,
    sync: () -> Unit
) {
    var energy by remember { mutableIntStateOf(2) }
    var pain by remember { mutableIntStateOf(0) }
    val energyIcons = listOf("😫", "😕", "😐", "🙂", "😁")
    val painLabels = listOf("Aucune", "Légère", "Moyenne", "Importante")

    Screen(
        title = "Aujourd'hui",
        subtitle = "Les données utiles d'abord, le détail ensuite."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (latest == null) {
                StatusCard(
                    "🔗",
                    "Santé Connect",
                    if (hasPermissions) "Prêt à synchroniser" else "Connexion nécessaire",
                    "Autorise les données puis synchronise les 28 derniers jours."
                )
                Button(
                    onClick = if (hasPermissions) sync else requestPermissions,
                    enabled = !syncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (syncing) "Synchronisation…"
                        else if (hasPermissions) "Synchroniser"
                        else "Autoriser Santé Connect"
                    )
                }
            } else {
                StatusCard(
                    "😴",
                    "Sommeil",
                    formatMinutes(latest.sleepMinutes),
                    sleepDetail(latest)
                )
                StatusCard(
                    "❤️",
                    "Récupération cardio",
                    latest.hrvRmssdMs?.let { "HRV " + oneDecimal(it) + " ms" } ?: "HRV —",
                    "FC repos " + bpm(latest.restingHeartRate) +
                        " • nuit " + bpm(latest.overnightHeartRate)
                )
                StatusCard(
                    "🏃",
                    "Activité",
                    (latest.steps ?: 0L).toString() + " pas",
                    distance(latest.distanceMeters) +
                        " • " + (latest.exerciseMinutes ?: 0L) + " min sport"
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Check-in rapide", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text("Énergie", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        energyIcons.forEachIndexed { index, icon ->
                            AssistChip(
                                onClick = { energy = index },
                                label = {
                                    Text(
                                        icon,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            )
                        }
                    }
                    Text(
                        "Niveau " + (energy + 1) + "/5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("Douleurs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        painLabels.forEachIndexed { index, label ->
                            AssistChip(
                                onClick = { pain = index },
                                label = { Text(if (pain == index) "• " + label else label) }
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Étape suivante",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Score de récupération local",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "La base 7/28 jours est maintenant prête pour le moteur de décision.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
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
        subtitle = "Santé Connect → base locale → tendances personnelles."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "🔗",
                "Santé Connect",
                when (availability) {
                    HealthConnectAvailability.AVAILABLE ->
                        if (hasPermissions) "Connecté" else "Autorisations requises"
                    HealthConnectAvailability.UPDATE_REQUIRED -> "Mise à jour requise"
                    HealthConnectAvailability.UNAVAILABLE -> "Indisponible"
                },
                if (hasPermissions)
                    days.size.toString() + " jours enregistrés localement"
                else
                    "Les données restent sur ton téléphone."
            )

            if (availability == HealthConnectAvailability.AVAILABLE) {
                if (!hasPermissions) {
                    Button(
                        onClick = requestPermissions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Autoriser les données santé")
                    }
                } else {
                    Button(
                        onClick = sync,
                        enabled = !syncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (syncing) "Synchronisation en cours…" else "Synchroniser 28 jours")
                    }
                }
            }

            if (!message.isNullOrBlank()) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            StatusCard(
                "😴",
                "Sommeil",
                latest?.sleepMinutes?.let(::formatMinutes) ?: "—",
                latest?.let(::sleepDetail) ?: "Aucune nuit synchronisée"
            )
            StatusCard(
                "❤️",
                "Cœur & HRV",
                latest?.hrvRmssdMs?.let { oneDecimal(it) + " ms" } ?: "—",
                latest?.let {
                    "FC repos " + bpm(it.restingHeartRate) +
                        " • respiration " + rate(it.respiratoryRate)
                } ?: "Aucune donnée synchronisée"
            )
            StatusCard(
                "🏃",
                "Activité",
                latest?.steps?.let { "$it pas" } ?: "—",
                latest?.let {
                    distance(it.distanceMeters) +
                        " • " + kcal(it.caloriesKcal) +
                        " • " + (it.exerciseMinutes ?: 0L) + " min sport"
                } ?: "Aucune donnée synchronisée"
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
private fun TrainingScreen() {
    Screen(
        title = "Entraînement",
        subtitle = "Le moteur Tempo sera branché sur les données récupérées."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "🏋️",
                "Séance du jour",
                "Moteur de décision à venir",
                "La récupération et la charge récente serviront à choisir la séance."
            )
            StatusCard(
                "⏱️",
                "Moteur Tempo",
                "À porter",
                "Timers, repos, sons, chrono global et bibliothèque d'exercices."
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
        subtitle = "Références basées sur tes propres données."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "📅",
                "Moyenne 7 jours",
                averageMinutes(week.mapNotNull { it.sleepMinutes }),
                "Sommeil • HRV " + averageDouble(week.mapNotNull { it.hrvRmssdMs }, " ms")
            )
            StatusCard(
                "📊",
                "Moyenne 28 jours",
                averageMinutes(month.mapNotNull { it.sleepMinutes }),
                "Sommeil • FC repos " +
                    averageDouble(month.mapNotNull { it.restingHeartRate }, " bpm")
            )
            StatusCard(
                "🚶",
                "Activité 7 jours",
                averageLong(week.mapNotNull { it.steps }) + " pas/j",
                "Ta référence personnelle se construit automatiquement."
            )
        }
    }
}

@Composable
private fun CoachScreen() {
    Screen(
        title = "Coach",
        subtitle = "L'IA expliquera les décisions, elle ne remplacera pas les règles locales."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "🧠",
                "Moteur local",
                "Données disponibles",
                "Sommeil, HRV, FC repos et activité peuvent maintenant alimenter les règles."
            )
            StatusCard(
                "✨",
                "Coach ChatGPT",
                "Toujours optionnel",
                "Aucune donnée santé n'est envoyée automatiquement."
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    availability: HealthConnectAvailability,
    hasPermissions: Boolean,
    localDays: Int
) {
    Screen(
        title = "Paramètres",
        subtitle = "État de la synchronisation et du stockage local."
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
                "$localDays jours",
                "Room / SQLite sur le téléphone"
            )
            StatusCard(
                "📴",
                "Fonctionnement hors ligne",
                "Oui",
                "Les données déjà synchronisées restent consultables hors connexion."
            )
        }
    }
}

private fun formatMinutes(minutes: Long?): String {
    if (minutes == null) return "—"
    val h = minutes / 60
    val m = minutes % 60
    return h.toString() + " h " + m.toString().padStart(2, '0')
}

private fun sleepDetail(day: DailyHealthEntity): String =
    "Profond " + (day.deepMinutes ?: 0L) + " min" +
        " • REM " + (day.remMinutes ?: 0L) + " min" +
        " • éveil " + (day.awakeMinutes ?: 0L) + " min"

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
