package com.se1827.emailclient.ui

import android.Manifest
import android.widget.Toast
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.se1827.emailclient.DeadlineNotificationWorker
import com.se1827.emailclient.DeadlinePreferences
import com.se1827.emailclient.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showWipeConfirm by remember { mutableStateOf(false) }

    // Notification preferences (local SharedPreferences — no ViewModel needed)
    val context = LocalContext.current
    val deadlinePrefs = remember { DeadlinePreferences(context) }
    var notifsEnabled by remember { mutableStateOf(deadlinePrefs.notificationsEnabled) }
    var selectedInterval by remember { mutableStateOf(deadlinePrefs.remindMinutesBefore) }
    var eveningHeadsUp by remember { mutableStateOf(deadlinePrefs.eveningHeadsUpEnabled) }
    var urgentCheck by remember { mutableStateOf(deadlinePrefs.urgentCheckEnabled) }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    IconButton(onClick = { viewModel.loadSettings() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (state.error != null) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                                Text(state.error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────
                    // NOTIFICATIONS & ALERTS
                    // ──────────────────────────────────────────────────
                    item {
                        Text(
                            "Notifications & Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // ── Master toggle ────────────────────────────
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        if (notifsEnabled) Icons.Filled.NotificationsActive
                                        else Icons.Filled.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (notifsEnabled) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Deadline notifications",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            if (notifsEnabled) "Alerts are active"
                                            else "All notifications silenced",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = notifsEnabled,
                                        onCheckedChange = {
                                            notifsEnabled = it
                                            deadlinePrefs.notificationsEnabled = it
                                        }
                                    )
                                }

                                // Everything below collapses when notifications are off
                                AnimatedVisibility(
                                    visible = notifsEnabled,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                                        // ── Remind before ────────────────────────
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Filled.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                "Remind me before",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            DeadlinePreferences.INTERVAL_OPTIONS.forEach { (minutes, label) ->
                                                val isSelected = selectedInterval == minutes
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        selectedInterval = minutes
                                                        deadlinePrefs.remindMinutesBefore = minutes
                                                    },
                                                    label = {
                                                        Text(
                                                            label,
                                                            fontWeight = if (isSelected) FontWeight.Bold
                                                                         else FontWeight.Normal
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                )
                                            }
                                        }

                                        // ── Evening heads-up ─────────────────────
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    "🌙 Evening heads-up",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    "Get notified between 8 PM – midnight about tomorrow morning deadlines",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Switch(
                                                checked = eveningHeadsUp,
                                                onCheckedChange = {
                                                    eveningHeadsUp = it
                                                    deadlinePrefs.eveningHeadsUpEnabled = it
                                                }
                                            )
                                        }

                                        // ── Urgent fast-poll ─────────────────────
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    "⚡ Urgent fast-check",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    "Check every 15 min when a deadline is within 2 hours",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Switch(
                                                checked = urgentCheck,
                                                onCheckedChange = {
                                                    urgentCheck = it
                                                    deadlinePrefs.urgentCheckEnabled = it
                                                }
                                            )
                                        }

                                        // ── Test notification button ─────────────
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                        OutlinedButton(
                                            onClick = { sendTestNotification(context) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.NotificationsActive,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Send test notification")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────
                    // INTEGRATIONS
                    // ──────────────────────────────────────────────────
                    item {
                        Text("Integrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                OutlinedButton(onClick = onNavigateToAccounts, modifier = Modifier.fillMaxWidth()) {
                                    Text("Manage Accounts")
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.fillMaxWidth()) {
                                    Text("View Calendar")
                                }
                            }
                        }
                    }

                    // Graph Section
                    item {
                        Text("Microsoft Graph / Outlook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val mode = state.graphStatus?.mode ?: "unknown"
                                val modeColor = when (mode) {
                                    "live" -> Color.Green
                                    "mock" -> Color(0xFFFFA000)
                                    "error" -> MaterialTheme.colorScheme.error
                                    else -> Color.Gray
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = modeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)) {
                                        Text(mode.uppercase(), color = modeColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(state.graphStatus?.userEmail ?: "Not connected", style = MaterialTheme.typography.bodyMedium)
                                }
                                state.graphStatus?.tip?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Storage Section
                    item {
                        Text("Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    MetricChip("Emails", state.storageStats?.emails?.toString() ?: "0")
                                    MetricChip("Classified", state.storageStats?.classifications?.toString() ?: "0")
                                    MetricChip("Drafts", state.storageStats?.drafts?.toString() ?: "0")
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    MetricChip("Events", state.storageStats?.events?.toString() ?: "0")
                                    MetricChip("Accounts", state.storageStats?.accounts?.toString() ?: "0")
                                    MetricChip("Notifs", state.storageStats?.notifications?.toString() ?: "0")
                                }
                            }
                        }
                    }

                    // Sign Out
                    item {
                        Text("Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text("Sign out of your current session. You will need to re-enter your password.", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = onLogout,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sign Out")
                                }
                            }
                        }
                    }

                    // Danger Zone
                    item {
                        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text("Wipe all local data and synced emails. This action cannot be undone.", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { showWipeConfirm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Wipe All Storage")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Wipe Storage") },
            text = { Text("Are you sure you want to wipe all storage? This will delete all emails and clear the database on the backend.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.wipeAllStorage(); showWipeConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !state.isWiping
                ) {
                    if (state.isWiping) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                    else Text("Wipe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Fires a test notification so the user can verify notifications are working.
 */
private fun sendTestNotification(context: Context) {
    // Check permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context,
                "Notification permission denied. Enable it in Settings → App Info.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
    }

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Ensure the notification channel exists (it's normally created by the Worker,
    // but may not exist yet on first launch before the Worker runs)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            DeadlineNotificationWorker.CHANNEL_ID,
            "Deadline Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for upcoming deadlines and action items"
        }
        nm.createNotificationChannel(channel)
    }

    val notif = NotificationCompat.Builder(context, DeadlineNotificationWorker.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("🔔 Test notification")
        .setContentText("Deadline alerts are working! You'll be notified about upcoming deadlines.")
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText("Deadline alerts are working! You'll receive notifications based on your configured interval when deadlines approach.")
        )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    nm.notify(88888, notif)
}

@Composable
fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
