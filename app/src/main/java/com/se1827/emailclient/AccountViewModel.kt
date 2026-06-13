package com.se1827.emailclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.data.AccountRepository
import com.se1827.emailclient.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val accounts: List<AccountConfigDto> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null
)

class AccountViewModel(
    private val repository: AccountRepository = AccountRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init { loadAccounts() }

    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getAccounts().onSuccess { accts ->
                _uiState.value = _uiState.value.copy(accounts = accts, isLoading = false)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Failed to load accounts")
            }
        }
    }

    fun createAccount(request: AccountConfigRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.createAccount(request).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, snackbarMessage = "Account added")
                loadAccounts()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, snackbarMessage = "Failed: ${it.message}")
            }
        }
    }

    fun updateAccount(id: String, request: AccountConfigRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.updateAccount(id, request).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, snackbarMessage = "Account updated")
                loadAccounts()
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false, snackbarMessage = "Failed: ${it.message}")
            }
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            repository.deleteAccount(id).onSuccess {
                _uiState.value = _uiState.value.copy(
                    accounts = _uiState.value.accounts.filter { it.id != id },
                    snackbarMessage = "Account deleted"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(snackbarMessage = "Failed: ${it.message}")
            }
        }
    }

    fun clearSnackbar() { _uiState.value = _uiState.value.copy(snackbarMessage = null) }
}
