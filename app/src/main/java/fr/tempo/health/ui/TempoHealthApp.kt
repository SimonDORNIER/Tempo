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
import androidx.compose.foundation.layout.weight
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.tempo.health.BuildConfig

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
fun TempoHealthApp() {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val selectedDestination = mainDestinations.firstOrNull { it.route == currentRoute }
    val showBottomBar = selectedDestination != null

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
                                text = "Fondation v" + BuildConfig.VERSION_NAME,
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
            composable("today") { TodayScreen() }
            composable("health") { HealthScreen() }
            composable("training") { TrainingScreen() }
            composable("progress") { ProgressScreen() }
            composable("coach") { CoachScreen() }
            composable("settings") { SettingsScreen() }
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
private fun TodayScreen() {
    var energy by remember { mutableIntStateOf(2) }
    var pain by remember { mutableIntStateOf(0) }
    val energyIcons = listOf("😫", "😕", "😐", "🙂", "😁")
    val painLabels = listOf("Aucune", "Légère", "Moyenne", "Importante")

    Screen(
        title = "Aujourd'hui",
        subtitle = "Ton état du jour en un coup d'œil."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            StatusCard(
                emoji = "🟢",
                title = "Récupération",
                value = "À calibrer",
                detail = "Les données Santé Connect arrivent à l'étape suivante."
            )

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
                        "Séance proposée",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Mobilité + remise en route",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "⏱ 12 min   •   ⚡ Intensité légère",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Moteur Tempo à connecter")
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthScreen() {
    Screen(
        title = "Santé",
        subtitle = "Sommeil, cœur et activité, avec tes propres références."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard("😴", "Sommeil", "—", "Aucune donnée synchronisée")
            StatusCard("❤️", "Cœur & HRV", "—", "Aucune donnée synchronisée")
            StatusCard("🏃", "Activité", "—", "Aucune donnée synchronisée")
            StatusCard(
                "🔗",
                "Santé Connect",
                "Prochaine étape",
                "La fondation est prête à recevoir le connecteur natif."
            )
        }
    }
}

@Composable
private fun TrainingScreen() {
    Screen(
        title = "Entraînement",
        subtitle = "Le futur moteur Tempo, piloté par ta récupération."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "🏋️",
                "Séance du jour",
                "Préparation automatique",
                "Durée, intensité et groupes musculaires seront calculés localement."
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
private fun ProgressScreen() {
    Screen(
        title = "Progression",
        subtitle = "Des tendances simples, pas des dizaines de chiffres."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard("📅", "7 jours", "—", "Tendance courte à venir")
            StatusCard("📊", "28 jours", "—", "Référence personnelle à venir")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Objectif", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Relier santé, récupération et entraînement pour voir ce qui te fait réellement progresser.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachScreen() {
    Screen(
        title = "Coach",
        subtitle = "L'IA expliquera les décisions, elle ne les remplacera pas."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                "🧠",
                "Moteur local",
                "Prioritaire",
                "Les règles de récupération et d'entraînement fonctionneront hors ligne."
            )
            StatusCard(
                "✨",
                "Coach ChatGPT",
                "Couche optionnelle",
                "Il recevra uniquement des données synthétisées lorsque tu le souhaites."
            )
        }
    }
}

@Composable
private fun SettingsScreen() {
    Screen(
        title = "Paramètres",
        subtitle = "État technique de cette première fondation."
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
                "Non configuré",
                "Connexion prévue pour la prochaine version."
            )
            StatusCard(
                "📴",
                "Fonctionnement hors ligne",
                "Oui",
                "Aucun serveur obligatoire dans cette fondation."
            )
        }
    }
}
