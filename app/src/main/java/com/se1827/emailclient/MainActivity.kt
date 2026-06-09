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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
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

private enum class AppTab(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Filled.Dashboard),
    Triage("Triage", Icons.Filled.Inbox),
    Alerts("Alerts", Icons.Filled.Notifications)
}

private data class DashboardStats(
    val totalEmails: Int,
    val unread: Int,
    val classified: Int,
    val starred: Int,
    val pendingDrafts: Int,
    val responseReadiness: Float
)

private data class AgentAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val time: String
)

private enum class AlertSeverity { Critical, Warning, Info }
private enum class Priority { Critical, High, Normal, Low }

private data class EmailUi(
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
private fun EmailAgentApp() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Dashboard) }
    val emails = remember { mutableStateListOf<EmailUi>().apply { addAll(sampleEmails) } }
    val alerts = remember { mutableStateListOf<AgentAlert>().apply { addAll(sampleAlerts) } }
    val stats = remember(emails.size, alerts.size) {
        DashboardStats(
            totalEmails = emails.size,
            unread = emails.count { it.isUnread },
            classified = emails.size,
            starred = emails.count { it.isStarred },
            pendingDrafts = emails.count { it.hasDraft },
            responseReadiness = emails.count { it.hasDraft || it.priority == Priority.Low } / emails.size.toFloat()
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = when (selectedTab) {
                    AppTab.Dashboard -> "On-the-Go"
                    AppTab.Triage -> "AI Triage"
                    AppTab.Alerts -> "Alerts"
                },
                subtitle = "Email Agent",
                unreadCount = stats.unread
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { selectedTab = AppTab.Triage },
                icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                text = { Text("Refresh inbox") },
                shape = RoundedCornerShape(28.dp)
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
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
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            label = "tab-content",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) { tab ->
            when (tab) {
                AppTab.Dashboard -> DashboardScreen(stats, alerts, emails)
                AppTab.Triage -> TriageScreen(emails)
                AppTab.Alerts -> AlertsScreen(alerts)
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
private fun DashboardScreen(stats: DashboardStats, alerts: List<AgentAlert>, emails: List<EmailUi>) {
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
            EmailCard(email = it, compact = true)
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
        Metric("Critical", sampleEmails.count { it.priority == Priority.Critical }.toString(), Icons.Filled.PriorityHigh)
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
private fun TriageScreen(emails: List<EmailUi>) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Smart queue", "Critical first, drafts close behind") }
        items(emails, key = { it.id }) { email ->
            EmailCard(email = email, compact = false)
        }
    }
}

@Composable
private fun AlertsScreen(alerts: MutableList<AgentAlert>) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("Live notifications", "Deadlines, urgent mail, and AI insight") }
        items(alerts, key = { it.id }) { alert ->
            AlertCard(alert = alert, onDismiss = { alerts.remove(alert) })
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
private fun EmailCard(email: EmailUi, compact: Boolean) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
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

private val sampleAlerts = listOf(
    AgentAlert(
        id = "a1",
        title = "Contract deadline today",
        message = "A vendor approval thread has a 5 PM deadline and no sent reply yet.",
        severity = AlertSeverity.Critical,
        time = "12 min ago"
    ),
    AgentAlert(
        id = "a2",
        title = "Meeting context ready",
        message = "The AI matched three unread mails to your 3:30 PM roadmap sync.",
        severity = AlertSeverity.Warning,
        time = "28 min ago"
    ),
    AgentAlert(
        id = "a3",
        title = "Inbox pattern spotted",
        message = "Most low-priority status updates can be summarized into one digest.",
        severity = AlertSeverity.Info,
        time = "1 hr ago"
    )
)

private val sampleEmails = listOf(
    EmailUi(
        id = "e1",
        sender = "Aarav Mehta",
        subject = "Approval needed: CapGemini demo deck",
        preview = "Can you confirm the final flow before we lock the client walkthrough?",
        time = "Now",
        priority = Priority.Critical,
        category = "action-required",
        isUnread = true,
        isStarred = true,
        hasDraft = true
    ),
    EmailUi(
        id = "e2",
        sender = "Nisha Rao",
        subject = "Calendar shift for architecture review",
        preview = "Moving the API review to 3:30 PM. The agenda includes auth and notification retries.",
        time = "18m",
        priority = Priority.High,
        category = "meeting",
        isUnread = true,
        isStarred = false,
        hasDraft = true
    ),
    EmailUi(
        id = "e3",
        sender = "Backend Agent",
        subject = "New classification batch complete",
        preview = "42 messages classified with 96 percent average confidence.",
        time = "44m",
        priority = Priority.Normal,
        category = "ai-insight",
        isUnread = false,
        isStarred = false,
        hasDraft = false
    ),
    EmailUi(
        id = "e4",
        sender = "Procurement Desk",
        subject = "Invoice follow-up",
        preview = "Please share the updated GST details so the reimbursement can be processed.",
        time = "2h",
        priority = Priority.High,
        category = "deadline",
        isUnread = true,
        isStarred = true,
        hasDraft = false
    ),
    EmailUi(
        id = "e5",
        sender = "Team Digest",
        subject = "Daily build and test summary",
        preview = "No failing critical jobs. Two flaky UI tests are being monitored.",
        time = "4h",
        priority = Priority.Low,
        category = "info",
        isUnread = false,
        isStarred = false,
        hasDraft = false
    )
)
