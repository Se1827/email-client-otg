package com.se1827.emailclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.data.GraphRepository
import com.se1827.emailclient.data.StorageRepository
import com.se1827.emailclient.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val graphStatus: GraphStatusDto? = null,
    val storageStats: StorageStatsDto? = null,
    val isLoading: Boolean = true,
    val isWiping: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

class SettingsViewModel(
    private val graphRepository: GraphRepository = GraphRepository(),
    private val storageRepository: StorageRepository = StorageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            graphRepository.getGraphStatus().onSuccess { status ->
                _uiState.value = _uiState.value.copy(graphStatus = status)
            }
            storageRepository.getStorageStats().onSuccess { stats ->
                _uiState.value = _uiState.value.copy(storageStats = stats)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun wipeAllStorage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWiping = true)
            storageRepository.wipeAllStorage().onSuccess {
                _uiState.value = _uiState.value.copy(isWiping = false, snackbarMessage = "All storage wiped")
                loadSettings()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isWiping = false, snackbarMessage = "Wipe failed: ${it.message}")
            }
        }
    }

    fun clearSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
