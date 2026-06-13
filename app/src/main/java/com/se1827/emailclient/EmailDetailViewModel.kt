package com.se1827.emailclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.data.EmailRepository
import com.se1827.emailclient.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmailDetailUiState(
    val email: EmailDto? = null,
    val thread: List<EmailDto> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingThread: Boolean = false,
    val isClassifying: Boolean = false,
    val isDrafting: Boolean = false,
    val isApproving: Boolean = false,
    val isTogglingStar: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

class EmailDetailViewModel(
    private val repository: EmailRepository = EmailRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailDetailUiState())
    val uiState: StateFlow<EmailDetailUiState> = _uiState.asStateFlow()

    fun loadEmail(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getEmailById(id).onSuccess { email ->
                _uiState.value = _uiState.value.copy(email = email, isLoading = false)
                if (!email.isRead) markRead(id)
                loadThread(id)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = it.message ?: "Failed to load email"
                )
            }
        }
    }

    private fun loadThread(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingThread = true)
            repository.getEmailThread(id).onSuccess { thread ->
                _uiState.value = _uiState.value.copy(thread = thread, isLoadingThread = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingThread = false)
            }
        }
    }

    fun classify(force: Boolean = false) {
        val emailId = _uiState.value.email?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClassifying = true)
            repository.classifyEmail(emailId, force).onSuccess { classification ->
                val updated = _uiState.value.email?.copy(classification = classification)
                _uiState.value = _uiState.value.copy(
                    email = updated,
                    isClassifying = false,
                    snackbarMessage = "Classified: ${classification.priority} / ${classification.category}"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isClassifying = false,
                    snackbarMessage = "Classification failed: ${it.message}"
                )
            }
        }
    }

    fun generateDraft(quality: String = "balanced") {
        val emailId = _uiState.value.email?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDrafting = true)
            repository.generateDraft(emailId, quality).onSuccess { draft ->
                val updated = _uiState.value.email?.copy(draftReply = draft)
                _uiState.value = _uiState.value.copy(
                    email = updated,
                    isDrafting = false,
                    snackbarMessage = "Draft generated"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isDrafting = false,
                    snackbarMessage = "Draft failed: ${it.message}"
                )
            }
        }
    }

    fun approveDraft() {
        val emailId = _uiState.value.email?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApproving = true)
            repository.approveDraft(emailId).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isApproving = false,
                    snackbarMessage = "Reply sent successfully"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isApproving = false,
                    snackbarMessage = "Send failed: ${it.message}"
                )
            }
        }
    }

    fun toggleStar() {
        val email = _uiState.value.email ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTogglingStar = true)
            repository.toggleStar(email.id).onSuccess { response ->
                val updated = email.copy(isStarred = response.isStarred)
                _uiState.value = _uiState.value.copy(
                    email = updated,
                    isTogglingStar = false,
                    snackbarMessage = if (response.isStarred) "Starred" else "Unstarred"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isTogglingStar = false)
            }
        }
    }

    private fun markRead(id: String) {
        viewModelScope.launch { repository.markRead(id) }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
