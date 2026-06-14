package com.se1827.emailclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.se1827.emailclient.auth.AuthViewModel
import com.se1827.emailclient.network.NetworkClient
import com.se1827.emailclient.ui.AccountsScreen
import com.se1827.emailclient.ui.CalendarScreen
import com.se1827.emailclient.ui.ComposeScreen
import com.se1827.emailclient.ui.EmailDetailScreen
import com.se1827.emailclient.ui.LoginScreen
import com.se1827.emailclient.ui.SettingsScreen
import com.se1827.emailclient.ui.theme.EmailClientTheme
import kotlin.math.roundToInt
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    // Runtime notification permission request for Android 13+
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — notifications will work or be silently skipped */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialise the network layer so the auth interceptor can read the token
        NetworkClient.init(applicationContext)

        // Schedule the periodic deadline notification check (once per hour)
        DeadlineNotificationWorker.schedule(this)

        // Request POST_NOTIFICATIONS permission on Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            EmailClientTheme {
                val authViewModel: AuthViewModel = viewModel()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
                val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

                if (isLoggedIn) {
                    EmailAgentApp(onLogout = { authViewModel.logout() })
                } else {
                    LoginScreen(
                        authState = authUiState,
                        onLogin = { password -> authViewModel.login(password) }
                    )
                }
            }
        }
    }
}

// 5 tabs — Dashboard, Triage, Deadlines, Alerts, Settings
enum class AppTab(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Dashboard", Icons.Filled.Inbox),
    Triage("triage", "AI Triage", Icons.Filled.AutoAwesome),
    Deadlines("deadlines", "Deadlines", Icons.Filled.Schedule),
    Alerts("alerts", "Alerts", Icons.Filled.Notifications),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailAgentApp(viewModel: MainViewModel = viewModel(), onLogout: () -> Unit = {}) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppTab.Dashboard.route

    val dashboardState by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val emailsState by viewModel.emails.collectAsStateWithLifecycle()
    val alertsState by viewModel.alerts.collectAsStateWithLifecycle()

    val stats = (dashboardState as? UiState.Success)?.data ?: DashboardStats(0, 0, 0, 0, 0, 0f, 0)
    val emails = (emailsState as? UiState.Success)?.data ?: emptyList()
    val alerts = (alertsState as? UiState.Success)?.data ?: emptyList()

    // Share a single DeadlineViewModel across nav so badge stays up-to-date
    val deadlineViewModel: DeadlineViewModel = viewModel()
    val deadlinesState by deadlineViewModel.deadlines.collectAsStateWithLifecycle()
    val overdueCount = (deadlinesState as? UiState.Success)?.data
        ?.count { it.isOverdue } ?: 0

    val currentTab = AppTab.entries.find { it.route == currentRoute }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (currentTab != null) {
                AppTopBar(
                    title = currentTab.label,
                    subtitle = "Email Agent",
                    unreadCount = stats.unread,
                    scrollBehavior = scrollBehavior
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
                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        val badgeCount = when (tab) {
                            AppTab.Alerts -> alerts.size
                            AppTab.Deadlines -> overdueCount
                            else -> 0
                        }
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
                                if (badgeCount > 0) {
                                    BadgedBox(badge = {
                                        Badge { Text(badgeCount.toString()) }
                                    }) {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
                            label = {
                                Text(
                                    tab.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                when (dashboardState) {
                    is UiState.Loading -> LoadingScreen()
                    is UiState.Error -> ErrorScreen((dashboardState as UiState.Error).message)
                    else -> DashboardScreen(stats, alerts, emails) { emailId ->
                        navController.navigate("email_detail/$emailId")
                    }
                }
            }
            composable(AppTab.Triage.route) {
                when (emailsState) {
                    is UiState.Loading -> LoadingScreen()
                    is UiState.Error -> ErrorScreen((emailsState as UiState.Error).message)
                    else -> TriageScreen(emails) { emailId ->
                        navController.navigate("email_detail/$emailId")
                    }
                }
            }
            composable(AppTab.Deadlines.route) {
                DeadlinesScreen(viewModel = deadlineViewModel)
            }
            composable(AppTab.Alerts.route) {
                // FIX: Properly handle Loading/Error states to prevent crashes
                when (val aState = alertsState) {
                    is UiState.Loading -> LoadingScreen()
                    is UiState.Error -> ErrorScreen(aState.message)
                    is UiState.Success -> {
                        val deadlinesList = (deadlinesState as? UiState.Success)?.data ?: emptyList()
                        UnifiedAlertsScreen(
                            alerts = aState.data,
                            deadlines = deadlinesList,
                            onDismissAlert = viewModel::dismissAlert,
                            onRefresh = {
                                viewModel.fetchAlerts()
                                deadlineViewModel.refresh()
                            }
                        )
                    }
                }
            }
            composable(AppTab.Settings.route) {
                SettingsScreen(
                    onNavigateToAccounts = { navController.navigate("accounts") },
                    onNavigateToCalendar = { navController.navigate("calendar") },
                    onLogout = onLogout
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

// ─── Shared loading / error screens ────────────────────────────────────────

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Top App Bar ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    subtitle: String,
    unreadCount: Int,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    MediumTopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        title = {
            Column {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge { Text(unreadCount.toString()) }
                    }
                }
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

// ─── Dashboard ───────────────────────────────────────────────────────────────

@Composable
private fun DashboardScreen(
    stats: DashboardStats,
    alerts: List<AgentAlert>,
    emails: List<EmailUi>,
    onEmailClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { InboxHealthCard(stats) }
        item { MetricRail(stats) }
        if (alerts.isNotEmpty()) {
            item { SectionHeader("Priority alerts", "AI signals that need movement") }
            items(alerts.take(2), key = { it.id }) { alert ->
                AlertCard(alert = alert, onDismiss = null)
            }
        }
        val priorityEmails = emails.filter { it.hasDraft || it.priority == Priority.Critical }.take(4)
        if (priorityEmails.isNotEmpty()) {
            item { SectionHeader("Move next", "Draft-ready conversations") }
            items(priorityEmails, key = { it.id }) {
                EmailCard(email = it, compact = true, onClick = { onEmailClick(it.id) })
            }
        }
    }
}

@Composable
private fun InboxHealthCard(stats: DashboardStats) {
    val animatedReadiness by animateFloatAsState(
        targetValue = stats.responseReadiness,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "readiness"
    )
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Inbox health", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${(animatedReadiness * 100).roundToInt()}% ready for quick action",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            LinearProgressIndicator(
                progress = { animatedReadiness },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            "${stats.unread} unread",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Filled.MarkEmailUnread, contentDescription = null) }
                )
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            "${stats.pendingDrafts} drafts",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = { Icon(Icons.Filled.Drafts, contentDescription = null) }
                )
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
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(metrics) { metric ->
            MetricCard(metric)
        }
    }
}

private data class Metric(val label: String, val value: String, val icon: ImageVector)

@Composable
private fun MetricCard(metric: Metric) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.width(136.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                metric.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                metric.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                metric.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Triage ─────────────────────────────────────────────────────────────────

@Composable
private fun TriageScreen(emails: List<EmailUi>, onEmailClick: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader("Smart queue", "Critical first, drafts close behind") }
        items(emails, key = { it.id }) { email ->
            EmailCard(email = email, compact = false, onClick = { onEmailClick(email.id) })
        }
        if (emails.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "All caught up!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "No emails in the triage queue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─── Unified Alerts / Proactive Surface ──────────────────────────────────────

/**
 * Sealed interface representing items shown in the unified alerts surface.
 * Merges server-side alerts with client-side deadline intelligence.
 */
private sealed interface ProactiveItem {
    val sortKey: Int  // lower = higher priority

    data class AlertItem(val alert: AgentAlert) : ProactiveItem {
        override val sortKey = when (alert.severity) {
            AlertSeverity.Critical -> 0
            AlertSeverity.Warning -> 2
            AlertSeverity.Info -> 4
        }
    }

    data class DeadlineItem(val deadline: DeadlineUi) : ProactiveItem {
        override val sortKey = when {
            deadline.isOverdue -> 0
            deadline.group == DeadlineGroup.TODAY -> 1
            deadline.group == DeadlineGroup.THIS_WEEK -> 3
            else -> 5
        }
    }
}

@Composable
private fun UnifiedAlertsScreen(
    alerts: List<AgentAlert>,
    deadlines: List<DeadlineUi>,
    onDismissAlert: (String) -> Unit,
    onRefresh: () -> Unit
) {
    // Split into sections
    val needsAttention = mutableListOf<ProactiveItem>()
    val comingUp = mutableListOf<ProactiveItem>()
    val insights = mutableListOf<ProactiveItem>()

    // Classify alerts
    for (alert in alerts) {
        when (alert.severity) {
            AlertSeverity.Critical -> needsAttention.add(ProactiveItem.AlertItem(alert))
            AlertSeverity.Warning -> comingUp.add(ProactiveItem.AlertItem(alert))
            AlertSeverity.Info -> insights.add(ProactiveItem.AlertItem(alert))
        }
    }

    // Classify deadlines
    for (dl in deadlines) {
        when {
            dl.isOverdue -> needsAttention.add(ProactiveItem.DeadlineItem(dl))
            dl.group == DeadlineGroup.TODAY -> comingUp.add(ProactiveItem.DeadlineItem(dl))
            dl.group == DeadlineGroup.THIS_WEEK -> comingUp.add(ProactiveItem.DeadlineItem(dl))
            else -> insights.add(ProactiveItem.DeadlineItem(dl))
        }
    }

    // Sort each section by urgency
    needsAttention.sortBy { it.sortKey }
    comingUp.sortBy { it.sortKey }
    insights.sortBy { it.sortKey }

    val isEmpty = needsAttention.isEmpty() && comingUp.isEmpty() && insights.isEmpty()

    if (isEmpty) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyAlertsCard()
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Refresh button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalIconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        }

        // ── NEEDS ATTENTION ────────────────────────────────────────────────
        if (needsAttention.isNotEmpty()) {
            item {
                ProactiveSectionHeader(
                    emoji = "⚡",
                    title = "Needs attention",
                    subtitle = "Overdue & critical — act now",
                    count = needsAttention.size,
                    isUrgent = true
                )
            }
            items(
                needsAttention,
                key = { item ->
                    when (item) {
                        is ProactiveItem.AlertItem -> "alert_${item.alert.id}"
                        is ProactiveItem.DeadlineItem -> "deadline_${item.deadline.id}"
                    }
                }
            ) { item ->
                ProactiveCard(item, onDismissAlert)
            }
        }

        // ── COMING UP ─────────────────────────────────────────────────────
        if (comingUp.isNotEmpty()) {
            item {
                ProactiveSectionHeader(
                    emoji = "📅",
                    title = "Coming up",
                    subtitle = "Today & this week",
                    count = comingUp.size,
                    isUrgent = false
                )
            }
            items(
                comingUp,
                key = { item ->
                    when (item) {
                        is ProactiveItem.AlertItem -> "alert_${item.alert.id}"
                        is ProactiveItem.DeadlineItem -> "deadline_${item.deadline.id}"
                    }
                }
            ) { item ->
                ProactiveCard(item, onDismissAlert)
            }
        }

        // ── AI INSIGHTS ───────────────────────────────────────────────────
        if (insights.isNotEmpty()) {
            item {
                ProactiveSectionHeader(
                    emoji = "🔔",
                    title = "AI insights",
                    subtitle = "Info & upcoming items",
                    count = insights.size,
                    isUrgent = false
                )
            }
            items(
                insights,
                key = { item ->
                    when (item) {
                        is ProactiveItem.AlertItem -> "alert_${item.alert.id}"
                        is ProactiveItem.DeadlineItem -> "deadline_${item.deadline.id}"
                    }
                }
            ) { item ->
                ProactiveCard(item, onDismissAlert)
            }
        }
    }
}

@Composable
private fun ProactiveSectionHeader(
    emoji: String,
    title: String,
    subtitle: String,
    count: Int,
    isUrgent: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "section_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isUrgent) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "section_pulse_anim"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    ) {
        Text(
            "$emoji  $title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = if (isUrgent) Modifier.scale(pulse) else Modifier
        )
        Surface(
            shape = CircleShape,
            color = if (isUrgent) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                color = if (isUrgent) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProactiveCard(item: ProactiveItem, onDismissAlert: (String) -> Unit) {
    when (item) {
        is ProactiveItem.AlertItem -> AlertCard(
            alert = item.alert,
            onDismiss = { onDismissAlert(item.alert.id) }
        )
        is ProactiveItem.DeadlineItem -> InlineDeadlineAlertCard(deadline = item.deadline)
    }
}

/**
 * A compact deadline card styled to match the alerts aesthetic.
 * Shows urgency chip, countdown, and type icon inline.
 */
@Composable
private fun InlineDeadlineAlertCard(deadline: DeadlineUi) {
    val chipColor = urgencyChipColor(deadline.urgencyFraction, deadline.isOverdue)
    val accentColor = deadlinePriorityColor(deadline.priority)

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = lerp(
                accentColor,
                MaterialTheme.colorScheme.surfaceContainerHigh,
                0.85f
            )
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            Surface(shape = CircleShape, color = accentColor, contentColor = Color.White) {
                Icon(
                    imageVector = when (deadline.type) {
                        "event" -> Icons.Filled.CalendarMonth
                        "action" -> Icons.Filled.Task
                        else -> Icons.Filled.Email
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    deadline.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        deadline.formattedDue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (!deadline.location.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocationOn, contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            deadline.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // Countdown chip
            Surface(
                shape = RoundedCornerShape(50),
                color = lerp(chipColor, MaterialTheme.colorScheme.surface, 0.25f)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (deadline.isOverdue) {
                        Icon(
                            Icons.Filled.Warning, contentDescription = null,
                            modifier = Modifier.size(12.dp), tint = chipColor
                        )
                    }
                    Text(
                        deadline.timeRemainingLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = chipColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─── Shared section header ───────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Alert card ──────────────────────────────────────────────────────────────

@Composable
private fun AlertCard(alert: AgentAlert, onDismiss: (() -> Unit)?) {
    val color = severityColor(alert.severity)
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = lerp(color, MaterialTheme.colorScheme.surfaceContainerHigh, 0.80f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(shape = CircleShape, color = color, contentColor = Color.White) {
                Icon(
                    imageVector = when (alert.severity) {
                        AlertSeverity.Critical -> Icons.Filled.PriorityHigh
                        AlertSeverity.Warning -> Icons.Filled.Schedule
                        AlertSeverity.Info -> Icons.Filled.AutoAwesome
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    alert.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Dismiss alert",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─── Email card ───────────────────────────────────────────────────────────────

@Composable
private fun EmailCard(email: EmailUi, compact: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PriorityMarker(email.priority)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Sender row — weight ensures time stamp never collides with sender name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            email.sender,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            email.time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Text(
                        email.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        email.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            email.category.take(12),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            email.priority.name.lowercase(),
                            maxLines = 1
                        )
                    }
                )
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

// ─── Priority marker ─────────────────────────────────────────────────────────

@Composable
private fun PriorityMarker(priority: Priority) {
    val color = priorityColor(priority)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
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
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Empty alerts card ────────────────────────────────────────────────────────

@Composable
private fun EmptyAlertsCard() {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "All clear",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "No active AI alerts right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
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

// ─── Deadline color helpers ───────────────────────────────────────────────────

@Composable
private fun deadlinePriorityColor(priority: String): Color = when (priority) {
    "critical" -> MaterialTheme.colorScheme.error
    "high"     -> Color(0xFFE87900)
    "low"      -> Color(0xFF2E7D32)
    else       -> MaterialTheme.colorScheme.primary
}

@Composable
private fun urgencyChipColor(fraction: Float, isOverdue: Boolean): Color {
    val safe   = Color(0xFF2E7D32)
    val warn   = Color(0xFFE87900)
    val danger = MaterialTheme.colorScheme.error
    return when {
        isOverdue      -> danger
        fraction > 0.7 -> lerp(warn, danger, (fraction - 0.7f) / 0.3f)
        fraction > 0.3 -> lerp(safe, warn, (fraction - 0.3f) / 0.4f)
        else           -> safe
    }
}

// ─── Deadlines Screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinesScreen(viewModel: DeadlineViewModel = viewModel()) {
    val state by viewModel.deadlines.collectAsStateWithLifecycle()
    val windowDays by viewModel.windowDays.collectAsStateWithLifecycle()

    when (state) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Error   -> ErrorScreen((state as UiState.Error).message)
        is UiState.Success -> {
            val all = (state as UiState.Success<List<DeadlineUi>>).data
            DeadlineContent(all, windowDays, viewModel::setWindowDays, viewModel::refresh)
        }
    }
}

@Composable
private fun DeadlineContent(
    deadlines: List<DeadlineUi>,
    windowDays: Int,
    onWindowChange: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    val grouped = deadlines.groupBy { it.group }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Window filter chips ──────────────────────────────────────────────
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(7, 14, 30).forEach { days ->
                    FilterChip(
                        selected = windowDays == days,
                        onClick = { onWindowChange(days) },
                        label = { Text("${days}d", maxLines = 1) }
                    )
                }
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        }

        // ── Empty state ──────────────────────────────────────────────────────
        if (deadlines.isEmpty()) {
            item { DeadlinesEmptyState() }
            return@LazyColumn
        }

        // ── OVERDUE group ────────────────────────────────────────────────────
        grouped[DeadlineGroup.OVERDUE]?.takeIf { it.isNotEmpty() }?.let { list ->
            item { DeadlineGroupHeader("🔴  Overdue", list.size, isOverdue = true) }
            items(list, key = { it.id }) { DeadlineCard(it) }
        }

        // ── TODAY group ──────────────────────────────────────────────────────
        grouped[DeadlineGroup.TODAY]?.takeIf { it.isNotEmpty() }?.let { list ->
            item { DeadlineGroupHeader("📅  Today", list.size) }
            items(list, key = { it.id }) { DeadlineCard(it) }
        }

        // ── THIS WEEK group ──────────────────────────────────────────────────
        grouped[DeadlineGroup.THIS_WEEK]?.takeIf { it.isNotEmpty() }?.let { list ->
            item { DeadlineGroupHeader("📆  This week", list.size) }
            items(list, key = { it.id }) { DeadlineCard(it) }
        }

        // ── LATER group ─────────────────────────────────────────────────────
        grouped[DeadlineGroup.LATER]?.takeIf { it.isNotEmpty() }?.let { list ->
            item { DeadlineGroupHeader("🗓  Later", list.size) }
            items(list, key = { it.id }) { DeadlineCard(it) }
        }
    }
}

@Composable
private fun DeadlineGroupHeader(label: String, count: Int, isOverdue: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOverdue) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = if (isOverdue) Modifier.scale(pulse) else Modifier
        )
        Surface(
            shape = CircleShape,
            color = if (isOverdue) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                color = if (isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun DeadlineCard(deadline: DeadlineUi) {
    val priorityColor = deadlinePriorityColor(deadline.priority)
    val chipColor = urgencyChipColor(deadline.urgencyFraction, deadline.isOverdue)

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // ── Left priority stripe ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .background(priorityColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Type badge + title row ───────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type icon chip
                    val (typeIcon, typeLabel) = when (deadline.type) {
                        "event"          -> Icons.Filled.CalendarMonth to "Event"
                        "action"         -> Icons.Filled.Task to "Action"
                        else             -> Icons.Filled.Email to "Email"
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(typeIcon, contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                            Text(typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Text(
                        deadline.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Due date + countdown chip ────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        deadline.formattedDue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // ── Countdown chip ───────────────────────────────────────
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = lerp(chipColor, MaterialTheme.colorScheme.surface, 0.25f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (deadline.isOverdue) {
                                Icon(Icons.Filled.Warning, contentDescription = null,
                                    modifier = Modifier.size(12.dp), tint = chipColor)
                            }
                            Text(
                                deadline.timeRemainingLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = chipColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                // ── Location (calendar events) ───────────────────────────────
                if (!deadline.location.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            deadline.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlinesEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                "Nothing due soon!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "No deadlines or events in the selected window.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
