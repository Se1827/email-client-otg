package com.se1827.emailclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.data.CalendarRepository
import com.se1827.emailclient.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalendarUiState(
    val events: List<CalendarEventDto> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

class CalendarViewModel(
    private val repository: CalendarRepository = CalendarRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init { loadEvents() }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getUpcomingEvents(30).onSuccess { events ->
                _uiState.value = _uiState.value.copy(events = events, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Failed to load calendar")
            }
        }
    }

    fun syncCalendar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            repository.syncCalendar().onSuccess {
                _uiState.value = _uiState.value.copy(isSyncing = false, snackbarMessage = "Calendar synced")
                loadEvents()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSyncing = false, snackbarMessage = "Sync failed: ${it.message}")
            }
        }
    }

    fun createEvent(request: CreateEventRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true)
            repository.createEvent(request).onSuccess {
                _uiState.value = _uiState.value.copy(isCreating = false, snackbarMessage = "Event created")
                loadEvents()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isCreating = false, snackbarMessage = "Failed: ${it.message}")
            }
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            repository.deleteEvent(id).onSuccess {
                _uiState.value = _uiState.value.copy(
                    events = _uiState.value.events.filter { it.id != id },
                    snackbarMessage = "Event deleted"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Delete failed: ${it.message}")
            }
        }
    }

    fun clearSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
