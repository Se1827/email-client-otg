package com.se1827.emailclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.data.ActionsRepository
import com.se1827.emailclient.network.ActionItemDto
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ActionUi(
    val id: String,
    val description: String,
    val status: String,          // "pending", "in_progress", "completed", "dismissed"
    val priority: String,
    val emailId: String?,
    val sourceSubject: String?,
    val isOverdue: Boolean,
    val hoursOverdue: Int,
    val dueDateFormatted: String?,
    val timeRemainingLabel: String?
)

class ActionsViewModel(
    private val repo: ActionsRepository = ActionsRepository()
) : ViewModel() {

    private val _actions = MutableStateFlow<UiState<List<ActionUi>>>(UiState.Loading)
    val actions: StateFlow<UiState<List<ActionUi>>> = _actions.asStateFlow()

    /** Status filter — null = all */
    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    init { fetch() }

    fun fetch() {
        _actions.value = UiState.Loading
        viewModelScope.launch {
            repo.getActions().onSuccess { dtos ->
                _actions.value = UiState.Success(dtos.map { it.toUi() })
            }.onFailure {
                _actions.value = UiState.Error(it.message ?: "Failed to load actions")
            }
        }
    }

    fun setFilter(status: String?) {
        _statusFilter.value = status
    }

    fun updateStatus(id: String, newStatus: String) {
        // Optimistic UI update
        val current = (_actions.value as? UiState.Success)?.data ?: return
        _actions.value = UiState.Success(current.map {
            if (it.id == id) it.copy(status = newStatus) else it
        })
        viewModelScope.launch {
            repo.updateActionStatus(id, newStatus).onFailure { fetch() } // revert on failure
        }
    }

    private fun ActionItemDto.toUi(): ActionUi {
        val now = OffsetDateTime.now(ZoneId.of("UTC"))

        val dueDateFormatted = dueDate?.let { raw ->
            try {
                val dt = OffsetDateTime.parse(raw)
                val days = ChronoUnit.DAYS.between(now.toLocalDate(), dt.toLocalDate())
                when {
                    days == 0L -> "Today · ${dt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                    days == 1L -> "Tomorrow · ${dt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                    else -> dt.format(DateTimeFormatter.ofPattern("EEE d MMM"))
                }
            } catch (e: Exception) { raw }
        }

        val timeRemainingLabel = dueDate?.let { raw ->
            try {
                val dt = OffsetDateTime.parse(raw)
                val h = ChronoUnit.HOURS.between(now, dt)
                val d = ChronoUnit.DAYS.between(now.toLocalDate(), dt.toLocalDate())
                when {
                    isOverdue -> if (hoursOverdue >= 24) "Overdue ${hoursOverdue / 24}d" else "Overdue ${hoursOverdue}h"
                    h < 1 -> "< 1h remaining"
                    h < 24 -> "${h}h remaining"
                    else -> "${d}d remaining"
                }
            } catch (e: Exception) { null }
        }

        return ActionUi(
            id = id,
            description = description,
            status = status,
            priority = priority,
            emailId = emailId,
            sourceSubject = sourceSubject,
            isOverdue = isOverdue,
            hoursOverdue = hoursOverdue,
            dueDateFormatted = dueDateFormatted,
            timeRemainingLabel = timeRemainingLabel
        )
    }
}
