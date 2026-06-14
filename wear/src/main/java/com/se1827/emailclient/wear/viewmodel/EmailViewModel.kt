package com.se1827.emailclient.wear.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.wear.data.EmailRepository
import com.se1827.emailclient.wear.data.model.EmailItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed class UiState {
    object Loading : UiState()
    data class Success(val emails: List<EmailItem>) : UiState()
    object Empty : UiState()
    data class NetworkError(val message: String) : UiState()
    data class BackendUnavailable(val message: String) : UiState()
    data class AuthError(val message: String) : UiState()
    data class UnknownError(val message: String) : UiState()
}

class EmailViewModel(
    private val repository: EmailRepository = EmailRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun fetchPendingDrafts() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                Log.d("EmailViewModel", "Fetching pending drafts from GET /api/emails")
                val drafts = repository.getPendingDrafts()
                if (drafts.isEmpty()) {
                    _uiState.value = UiState.Empty
                } else {
                    _uiState.value = UiState.Success(drafts)
                }
            } catch (e: IOException) {
                Log.e("EmailViewModel", "Network error fetching drafts: ${e.message}", e)
                _uiState.value = UiState.NetworkError("Network error: Please check your connection.")
            } catch (e: HttpException) {
                Log.e("EmailViewModel", "HTTP error fetching drafts. Code: ${e.code()}", e)
                _uiState.value = when (e.code()) {
                    401, 403 -> UiState.AuthError("Authentication required.")
                    in 500..599 -> UiState.BackendUnavailable("Backend is currently unavailable.")
                    else -> UiState.UnknownError("An unexpected error occurred.")
                }
            } catch (e: Exception) {
                Log.e("EmailViewModel", "Unknown error fetching drafts", e)
                _uiState.value = UiState.UnknownError(e.message ?: "Unknown error")
            }
        }
    }

    fun approveDraft(id: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                Log.d("EmailViewModel", "Approving draft via POST /api/emails/$id/approve")
                repository.approveDraft(id)
                fetchPendingDrafts()
            } catch (e: Exception) {
                Log.e("EmailViewModel", "Error approving draft $id", e)
                // Optionally show a toast or error message in UI
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun dismissLocally(id: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                Log.d("EmailViewModel", "Skipping draft via POST /api/emails/$id/read")
                repository.skipDraft(id)
                fetchPendingDrafts()
            } catch (e: Exception) {
                Log.e("EmailViewModel", "Error skipping draft $id", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun getEmailById(id: String): EmailItem? {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            return currentState.emails.firstOrNull { it.id == id }
        }
        return null
    }
}
