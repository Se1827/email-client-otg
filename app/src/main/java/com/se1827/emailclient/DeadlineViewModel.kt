package com.se1827.emailclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.data.DeadlineRepository
import com.se1827.emailclient.network.DeadlineItemDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Priority order for display sorting */
private val PRIORITY_ORDER = mapOf("critical" to 0, "high" to 1, "normal" to 2, "low" to 3)

data class DeadlineUi(
    val id: String,
    val title: String,
    val type: String,           // "action", "event", "email_deadline"
    val source: String,         // "action_item", "calendar", "email"
    val priority: String,       // "critical", "high", "normal", "low"
    val emailId: String?,
    val isOverdue: Boolean,
    val hoursOverdue: Int,
    val dueIso: String,
    val formattedDue: String,   // "Mon 18 Jun · 14:30" or "Today · 14:30" or "All day"
    val timeRemainingLabel: String, // "Overdue 3h" / "2d 4h" / "Today 14:30" / "In 3h"
    val urgencyFraction: Float, // 0..1 used to tint the countdown chip
    val location: String?,
    val isAllDay: Boolean,
    val group: DeadlineGroup
)

enum class DeadlineGroup { OVERDUE, TODAY, THIS_WEEK, LATER }

class DeadlineViewModel(
    private val repo: DeadlineRepository = DeadlineRepository()
) : ViewModel() {

    private val _windowDays = MutableStateFlow(7)
    val windowDays: StateFlow<Int> = _windowDays.asStateFlow()

    private val _deadlines = MutableStateFlow<UiState<List<DeadlineUi>>>(UiState.Loading)
    val deadlines: StateFlow<UiState<List<DeadlineUi>>> = _deadlines.asStateFlow()

    init { startPolling() }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                load()
                delay(5 * 60_000L) // refresh every 5 minutes
            }
        }
    }

    fun setWindowDays(days: Int) {
        _windowDays.value = days
        viewModelScope.launch { load() }
    }

    fun refresh() { viewModelScope.launch { load() } }

    private suspend fun load() {
        repo.getDeadlines(_windowDays.value).onSuccess { response ->
            _deadlines.value = UiState.Success(response.deadlines.map { it.toUi() })
        }.onFailure {
            _deadlines.value = UiState.Error(it.message ?: "Failed to load deadlines")
        }
    }

    private fun DeadlineItemDto.toUi(): DeadlineUi {
        val now = OffsetDateTime.now(ZoneId.of("UTC"))
        val due = try { OffsetDateTime.parse(this.due) } catch (e: Exception) { now }
        val minutesUntilDue = ChronoUnit.MINUTES.between(now, due)
        val hoursUntilDue = ChronoUnit.HOURS.between(now, due)
        val daysUntilDue = ChronoUnit.DAYS.between(now.toLocalDate(), due.toLocalDate())

        val formattedDue = when {
            isAllDay == true -> due.format(DateTimeFormatter.ofPattern("EEE d MMM"))
            daysUntilDue == 0L -> "Today · ${due.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            daysUntilDue == 1L -> "Tomorrow · ${due.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            else -> due.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm"))
        }

        val timeRemainingLabel = when {
            isOverdue -> {
                val h = hoursOverdue
                if (h >= 24) "Overdue ${h / 24}d ${h % 24}h" else "Overdue ${h}h"
            }
            minutesUntilDue < 60 -> "In ${minutesUntilDue}min"
            hoursUntilDue < 24 -> "In ${hoursUntilDue}h"
            daysUntilDue == 1L -> "Tomorrow"
            else -> "${daysUntilDue}d remaining"
        }

        // urgencyFraction: 1.0 = extremely urgent (overdue / <1h), 0.0 = calm (7+ days)
        val urgencyFraction = when {
            isOverdue -> 1f
            hoursUntilDue < 1 -> 0.95f
            hoursUntilDue < 6 -> 0.85f
            hoursUntilDue < 24 -> 0.65f
            daysUntilDue <= 2 -> 0.4f
            daysUntilDue <= 5 -> 0.2f
            else -> 0.05f
        }

        val group = when {
            isOverdue -> DeadlineGroup.OVERDUE
            daysUntilDue == 0L -> DeadlineGroup.TODAY
            daysUntilDue <= 7 -> DeadlineGroup.THIS_WEEK
            else -> DeadlineGroup.LATER
        }

        return DeadlineUi(
            id = id,
            title = title,
            type = type,
            source = source,
            priority = priority,
            emailId = emailId,
            isOverdue = isOverdue,
            hoursOverdue = hoursOverdue,
            dueIso = this.due,
            formattedDue = formattedDue,
            timeRemainingLabel = timeRemainingLabel,
            urgencyFraction = urgencyFraction,
            location = location,
            isAllDay = isAllDay == true,
            group = group
        )
    }
}
