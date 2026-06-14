package com.se1827.emailclient.wear.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Warning
import com.se1827.emailclient.wear.viewmodel.AlertsViewModel
import com.se1827.emailclient.wear.viewmodel.WearAlertItem
import com.se1827.emailclient.wear.viewmodel.WearAlertState

@Composable
fun AlertsScreen(viewModel: AlertsViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is WearAlertState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is WearAlertState.Error -> AlertsErrorScreen(
            message = s.message,
            onRetry = { viewModel.refresh() }
        )
        is WearAlertState.Success -> {
            if (s.items.isEmpty()) {
                AlertsEmptyScreen(onRetry = { viewModel.refresh() })
            } else {
                AlertsList(
                    items = s.items,
                    onDismiss = { id -> viewModel.dismissNotification(id) },
                    onRefresh = { viewModel.refresh() }
                )
            }
        }
    }
}

@Composable
private fun AlertsList(
    items: List<WearAlertItem>,
    onDismiss: (String) -> Unit,
    onRefresh: () -> Unit
) {
    // Split into urgent (overdue/critical) and rest
    val urgent = items.filter { item ->
        when (item) {
            is WearAlertItem.Deadline -> item.isOverdue || item.priority == "critical"
            is WearAlertItem.Notification -> item.severity == "critical"
        }
    }
    val other = items - urgent.toSet()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Title
        item {
            Text(
                text = "⚡ Alerts",
                style = MaterialTheme.typography.title2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Count badge
        item {
            Text(
                text = "${items.size} active",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Urgent section ────────────────────────────────────────────────
        if (urgent.isNotEmpty()) {
            item {
                SectionLabel(
                    text = "🔴 Needs attention",
                    color = Color(0xFFEF5350)
                )
            }
            items(urgent, key = { itemKey(it) }) { item ->
                WearAlertChip(item = item, onDismiss = onDismiss)
            }
        }

        // ── Other section ─────────────────────────────────────────────────
        if (other.isNotEmpty()) {
            item {
                SectionLabel(
                    text = "📅 Coming up",
                    color = MaterialTheme.colors.secondary
                )
            }
            items(other, key = { itemKey(it) }) { item ->
                WearAlertChip(item = item, onDismiss = onDismiss)
            }
        }

        // Refresh button at bottom
        item {
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption1,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun WearAlertChip(item: WearAlertItem, onDismiss: (String) -> Unit) {
    when (item) {
        is WearAlertItem.Deadline -> DeadlineChip(item)
        is WearAlertItem.Notification -> NotificationChip(item, onDismiss)
    }
}

@Composable
private fun DeadlineChip(deadline: WearAlertItem.Deadline) {
    val priorityColor = when (deadline.priority) {
        "critical" -> Color(0xFFEF5350)
        "high" -> Color(0xFFE87900)
        "low" -> Color(0xFF4CAF50)
        else -> MaterialTheme.colors.primary
    }

    val typeIcon = when (deadline.type) {
        "event" -> Icons.Filled.CalendarMonth
        "action" -> Icons.Filled.Task
        else -> Icons.Filled.Email
    }

    Chip(
        onClick = { },
        colors = ChipDefaults.gradientBackgroundChipColors(
            startBackgroundColor = priorityColor.copy(alpha = 0.25f),
            endBackgroundColor = MaterialTheme.colors.surface
        ),
        label = {
            Text(
                deadline.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
        },
        secondaryLabel = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = if (deadline.isOverdue) Color(0xFFEF5350)
                           else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    deadline.timeLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (deadline.isOverdue) Color(0xFFEF5350)
                            else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(priorityColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (deadline.isOverdue) Icons.Filled.Warning else typeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NotificationChip(
    notification: WearAlertItem.Notification,
    onDismiss: (String) -> Unit
) {
    val severityColor = when (notification.severity) {
        "critical" -> Color(0xFFEF5350)
        "warning" -> Color(0xFFE87900)
        else -> MaterialTheme.colors.primary
    }

    Chip(
        onClick = { onDismiss(notification.id) },
        colors = ChipDefaults.secondaryChipColors(),
        label = {
            Text(
                notification.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        },
        secondaryLabel = {
            Text(
                notification.message,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(severityColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.severity) {
                        "critical" -> Icons.Filled.PriorityHigh
                        "warning" -> Icons.Filled.Warning
                        else -> Icons.Filled.Notifications
                    },
                    contentDescription = null,
                    tint = severityColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

// ─── Empty & Error States ─────────────────────────────────────────────────────

@Composable
private fun AlertsEmptyScreen(onRetry: () -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colors.secondary,
                modifier = Modifier.size(40.dp)
            )
        }
        item {
            Text(
                text = "All Clear",
                style = MaterialTheme.typography.title2,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        item {
            Text(
                text = "No deadlines or alerts right now.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AlertsErrorScreen(message: String, onRetry: () -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        item {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Retry")
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun itemKey(item: WearAlertItem): String = when (item) {
    is WearAlertItem.Deadline -> "dl_${item.id}"
    is WearAlertItem.Notification -> "nt_${item.id}"
}
