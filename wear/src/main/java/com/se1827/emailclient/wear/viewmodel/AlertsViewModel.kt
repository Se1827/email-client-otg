package com.se1827.emailclient.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.wear.data.DeadlineRepository
import com.se1827.emailclient.wear.network.DeadlineItemDto
import com.se1827.emailclient.wear.network.NotificationDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// ─── UI Models ────────────────────────────────────────────────────────────────

sealed interface WearAlertItem {
    val sortKey: Int  // lower = more urgent

    data class Deadline(
        val id: String,
        val title: String,
        val type: String,
        val priority: String,
        val isOverdue: Boolean,
        val timeLabel: String,       // "Overdue 3h" / "Today 14:30" / "Tomorrow"
        val sender: String?
    ) : WearAlertItem {
        override val sortKey = when {
            isOverdue -> 0
            priority == "critical" -> 1
            priority == "high" -> 2
            else -> 4
        }
    }

    data class Notification(
        val id: String,
        val title: String,
        val message: String,
        val severity: String
    ) : WearAlertItem {
        override val sortKey = when (severity) {
            "critical" -> 0
            "warning" -> 3
            else -> 5
        }
    }
}

sealed class WearAlertState {
    data object Loading : WearAlertState()
    data class Success(val items: List<WearAlertItem>) : WearAlertState()
    data class Error(val message: String) : WearAlertState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class AlertsViewModel(
    private val repo: DeadlineRepository = DeadlineRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<WearAlertState>(WearAlertState.Loading)
    val state: StateFlow<WearAlertState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        _state.value = WearAlertState.Loading

        val items = mutableListOf<WearAlertItem>()
        var hadError = false

        // Fetch deadlines
        repo.getDeadlines(7).onSuccess { response ->
            response.deadlines.forEach { dto ->
                items.add(dto.toWearItem())
            }
        }.onFailure { hadError = true }

        // Fetch notifications
        repo.getNotifications().onSuccess { notifications ->
            notifications
                .distinctBy { it.id }
                .forEach { dto ->
                    items.add(
                        WearAlertItem.Notification(
                            id = dto.id,
                            title = dto.title,
                            message = dto.message,
                            severity = dto.severity
                        )
                    )
                }
        }.onFailure {
            if (items.isEmpty()) hadError = true
        }

        if (hadError && items.isEmpty()) {
            _state.value = WearAlertState.Error("Could not load alerts")
        } else {
            items.sortBy { it.sortKey }
            _state.value = WearAlertState.Success(items)
        }
    }

    fun dismissNotification(id: String) {
        val current = (_state.value as? WearAlertState.Success)?.items ?: return
        _state.value = WearAlertState.Success(current.filter {
            !(it is WearAlertItem.Notification && it.id == id)
        })
        viewModelScope.launch { repo.dismissNotification(id) }
    }

    private fun DeadlineItemDto.toWearItem(): WearAlertItem.Deadline {
        val localZone = ZoneId.systemDefault()
        val now = OffsetDateTime.now(localZone)
        val dueTime = try { OffsetDateTime.parse(due) } catch (e: Exception) { now }
        val hoursUntilDue = ChronoUnit.HOURS.between(now, dueTime)
        val daysUntilDue = ChronoUnit.DAYS.between(now.toLocalDate(), dueTime.toLocalDate())

        val timeLabel = when {
            isOverdue -> {
                val h = hoursOverdue
                if (h >= 24) "Overdue ${h / 24}d ${h % 24}h" else "Overdue ${h}h"
            }
            daysUntilDue == 0L -> "Today ${dueTime.toLocalTime().toString().take(5)}"
            daysUntilDue == 1L -> "Tomorrow ${dueTime.toLocalTime().toString().take(5)}"
            hoursUntilDue < 48 -> "In ${hoursUntilDue}h"
            else -> "${daysUntilDue}d remaining"
        }

        return WearAlertItem.Deadline(
            id = id,
            title = title,
            type = type,
            priority = priority,
            isOverdue = isOverdue,
            timeLabel = timeLabel,
            sender = sender
        )
    }
}
