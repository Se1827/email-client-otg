package com.se1827.emailclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.se1827.emailclient.ui.EmailDetailScreen
import com.se1827.emailclient.ui.ComposeScreen
import com.se1827.emailclient.ui.CalendarScreen
import com.se1827.emailclient.ui.AccountsScreen
import com.se1827.emailclient.ui.SettingsScreen
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.se1827.emailclient.ui.theme.EmailClientTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            EmailClientTheme {
                EmailAgentApp()
            }
        }
    }
}

enum class AppTab(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Filled.Dashboard),
    Triage("triage", "AI Triage", Icons.Filled.Inbox),
    Alerts("alerts", "Alerts", Icons.Filled.Notifications),
    SmartCards("cards", "Cards", Icons.Filled.AutoAwesome),
    Settings("settings", "Settings", Icons.Filled.Settings)
}

data class DashboardStats(
    val totalEmails: Int,
    val unread: Int,
    val classified: Int,
    val starred: Int,
    val pendingDrafts: Int,
    val responseReadiness: Float,
    val criticalCount: Int
)

data class AgentAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val time: String
)

enum class AlertSeverity { Critical, Warning, Info }
enum class Priority { Critical, High, Normal, Low }

data class EmailUi(
    val id: String,
    val sender: String,
    val subject: String,
    val preview: String,
    val time: String,
    val priority: Priority,
    val category: String,
    val isUnread: Boolean,
    val isStarred: Boolean,
    val hasDraft: Boolean
)

@Composable
private fun EmailAgentApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppTab.Dashboard.route
    
    val dashboardState by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val emailsState by viewModel.emails.collectAsStateWithLifecycle()
    val alertsState by viewModel.alerts.collectAsStateWithLifecycle()
    val smartCardsState by viewModel.smartCards.collectAsStateWithLifecycle()

    val stats = (dashboardState as? UiState.Success)?.data ?: DashboardStats(0, 0, 0, 0, 0, 0f, 0)
    val emails = (emailsState as? UiState.Success)?.data ?: emptyList()
    val alerts = (alertsState as? UiState.Success)?.data ?: emptyList()
    val smartCards = (smartCardsState as? UiState.Success)?.data ?: emptyList()

    val currentTab = AppTab.entries.find { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (currentTab != null) {
                AppTopBar(
                    title = currentTab.label,
                    subtitle = "Email Agent",
                    unreadCount = stats.unread
                )
            }
        },
        floatingActionButton = {
            if (currentRoute == AppTab.Dashboard.route || currentRoute == AppTab.Triage.route) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("compose") },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    text = { Text("Compose") },
                    shape = RoundedCornerShape(28.dp)
                )
            }
        },
        bottomBar = {
            if (currentTab != null) {
                NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Box {
                                    Icon(tab.icon, contentDescription = null)
                                    if (tab == AppTab.Alerts && alerts.isNotEmpty()) {
                                        Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                                            Text(alerts.size.toString())
                                        }
                                    }
                                }
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppTab.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            composable(AppTab.Dashboard.route) {
                if (dashboardState is UiState.Loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (dashboardState is UiState.Error) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((dashboardState as UiState.Error).message) }
                } else {
                    DashboardScreen(stats, alerts, emails) { emailId -> navController.navigate("email_detail/$emailId") }
                }
            }
            composable(AppTab.Triage.route) {
                if (emailsState is UiState.Loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (emailsState is UiState.Error) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((emailsState as UiState.Error).message) }
                } else {
                    TriageScreen(emails) { emailId -> navController.navigate("email_detail/$emailId") }
                }
            }
            composable(AppTab.Alerts.route) {
                if (alertsState is UiState.Loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (alertsState is UiState.Error) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((alertsState as UiState.Error).message) }
                } else {
                    AlertsScreen(alerts, viewModel::dismissAlert)
                }
            }
            composable(AppTab.SmartCards.route) {
                if (smartCardsState is UiState.Loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (smartCardsState is UiState.Error) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text((smartCardsState as UiState.Error).message) }
                } else {
                    SmartCardsScreen(smartCards)
                }
            }
            composable(AppTab.Settings.route) {
                SettingsScreen(
                    onNavigateToAccounts = { navController.navigate("accounts") },
                    onNavigateToCalendar = { navController.navigate("calendar") }
                )
            }
            composable("compose") {
                ComposeScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "email_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: return@composable
                EmailDetailScreen(emailId = id, onBack = { navController.popBackStack() })
            }
            composable("calendar") {
                CalendarScreen()
            }
            composable("accounts") {
                AccountsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: String, subtitle: String, unreadCount: Int) {
    LargeTopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        title = {
            Column {
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
                if (unreadCount > 0) {
                    Badge { Text(unreadCount.toString()) }
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun DashboardScreen(stats: DashboardStats, alerts: List<AgentAlert>, emails: List<EmailUi>, onEmailClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { InboxHealthCard(stats) }
        item { MetricRail(stats) }
        item { SectionHeader("Priority alerts", "AI signals that need movement") }
        items(alerts.take(2), key = { it.id }) { alert -> AlertCard(alert = alert, onDismiss = null) }
        item { SectionHeader("Move next", "Draft-ready conversations") }
        items(emails.filter { it.hasDraft || it.priority == Priority.Critical }.take(4), key = { it.id }) {
            EmailCard(email = it, compact = true, onClick = { onEmailClick(it.id) })
        }
    }
}

@Composable
private fun InboxHealthCard(stats: DashboardStats) {
    val animatedReadiness by animateFloatAsState(stats.responseReadiness, label = "readiness")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.padding(12.dp))
                }
                Column {
                    Text("Inbox health", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${(animatedReadiness * 100).roundToInt()}% ready for quick action",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            LinearProgressIndicator(
                progress = { animatedReadiness },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(onClick = { }, label = { Text("${stats.unread} unread") }, leadingIcon = {
                    Icon(Icons.Filled.MarkEmailUnread, contentDescription = null)
                })
                AssistChip(onClick = { }, label = { Text("${stats.pendingDrafts} drafts") }, leadingIcon = {
                    Icon(Icons.Filled.Drafts, contentDescription = null)
                })
            }
        }
    }
}

@Composable
private fun MetricRail(stats: DashboardStats) {
    val metrics = listOf(
        Metric("Total", stats.totalEmails.toString(), Icons.Filled.Inbox),
        Metric("Classified", stats.classified.toString(), Icons.Filled.CheckCircle),
        Metric("Starred", stats.starred.toString(), Icons.Filled.Star),
        Metric("Critical", stats.criticalCount.toString(), Icons.Filled.PriorityHigh)
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(metrics) { metric ->
            MetricCard(metric)
        }
    }
}

private data class Metric(val label: String, val value: String, val icon: ImageVector)

@Composable
private fun MetricCard(metric: Metric) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.width(148.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(metric.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(metric.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(metric.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TriageScreen(emails: List<EmailUi>, onEmailClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Smart queue", "Critical first, drafts close behind") }
        items(emails, key = { it.id }) { email ->
            EmailCard(email = email, compact = false, onClick = { onEmailClick(email.id) })
        }
    }
}

@Composable
private fun AlertsScreen(alerts: List<AgentAlert>, onDismissAlert: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Live notifications", "Deadlines, urgent mail, and AI insight") }
        items(alerts, key = { it.id }) { alert ->
            AlertCard(alert = alert, onDismiss = { onDismissAlert(alert.id) })
        }
        item {
            AnimatedVisibility(visible = alerts.isEmpty()) {
                EmptyAlertsCard()
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlertCard(alert: AgentAlert, onDismiss: (() -> Unit)?) {
    val color = severityColor(alert.severity)
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = lerp(color, MaterialTheme.colorScheme.surfaceContainerHigh, 0.78f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(shape = CircleShape, color = color) {
                Icon(
                    imageVector = when (alert.severity) {
                        AlertSeverity.Critical -> Icons.Filled.PriorityHigh
                        AlertSeverity.Warning -> Icons.Filled.Schedule
                        AlertSeverity.Info -> Icons.Filled.AutoAwesome
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(alert.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(alert.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(alert.time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Dismiss alert")
                }
            }
        }
    }
}

@Composable
private fun EmailCard(email: EmailUi, compact: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PriorityMarker(email.priority)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(email.sender, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(email.time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(email.subject, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        email.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { }, label = { Text(email.category) })
                AssistChip(onClick = { }, label = { Text(email.priority.name.lowercase()) })
                Spacer(Modifier.weight(1f))
                if (email.hasDraft) {
                    FilledIconButton(onClick = { }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send draft")
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityMarker(priority: Priority) {
    val color = priorityColor(priority)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (priority) {
                Priority.Critical -> Icons.Filled.PriorityHigh
                Priority.High -> Icons.Filled.Bolt
                Priority.Normal -> Icons.Filled.Work
                Priority.Low -> Icons.Filled.CheckCircle
            },
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
private fun EmptyAlertsCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text("All clear", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("No active AI alerts right now.", color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.Critical -> MaterialTheme.colorScheme.error
    Priority.High -> Color(0xFFE87900)
    Priority.Normal -> MaterialTheme.colorScheme.primary
    Priority.Low -> Color(0xFF2E7D32)
}

@Composable
private fun severityColor(severity: AlertSeverity): Color = when (severity) {
    AlertSeverity.Critical -> MaterialTheme.colorScheme.error
    AlertSeverity.Warning -> Color(0xFFE87900)
    AlertSeverity.Info -> MaterialTheme.colorScheme.primary
}


