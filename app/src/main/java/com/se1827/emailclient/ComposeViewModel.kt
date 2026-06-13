package com.se1827.emailclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.data.AccountRepository
import com.se1827.emailclient.data.EmailRepository
import com.se1827.emailclient.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComposeUiState(
    val accounts: List<AccountConfigDto> = emptyList(),
    val selectedAccountId: String? = null,
    val aiDraft: String = "",
    val isSending: Boolean = false,
    val isAiComposing: Boolean = false,
    val sendSuccess: Boolean = false,
    val snackbarMessage: String? = null
)

class ComposeViewModel(
    private val emailRepository: EmailRepository = EmailRepository(),
    private val accountRepository: AccountRepository = AccountRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComposeUiState())
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    init { loadAccounts() }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAccounts().onSuccess { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    selectedAccountId = accounts.firstOrNull()?.id
                )
            }
        }
    }

    fun selectAccount(id: String) {
        _uiState.value = _uiState.value.copy(selectedAccountId = id)
    }

    fun aiCompose(prompt: String, quality: String = "balanced") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiComposing = true, aiDraft = "")
            emailRepository.aiComposeEmail(prompt, quality).onSuccess { resp ->
                _uiState.value = _uiState.value.copy(
                    isAiComposing = false,
                    aiDraft = resp.draft,
                    snackbarMessage = "AI draft ready"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isAiComposing = false,
                    snackbarMessage = "AI compose failed: ${it.message}"
                )
            }
        }
    }

    fun send(to: String, cc: String, bcc: String, subject: String, body: String) {
        val toList = to.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (toList.isEmpty()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "At least one recipient required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            val req = ComposeEmailRequest(
                to = toList,
                cc = cc.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                bcc = bcc.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                subject = subject,
                body = body,
                accountId = _uiState.value.selectedAccountId
            )
            emailRepository.composeEmail(req).onSuccess {
                _uiState.value = _uiState.value.copy(isSending = false, sendSuccess = true, snackbarMessage = "Email sent!")
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSending = false, snackbarMessage = "Send failed: ${it.message}")
            }
        }
    }

    fun clearSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
