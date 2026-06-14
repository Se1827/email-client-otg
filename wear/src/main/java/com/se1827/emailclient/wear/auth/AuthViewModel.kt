package com.se1827.emailclient.wear.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.se1827.emailclient.wear.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Represents the transient state of the login form / flow. */
sealed class AuthUiState {
    /** Initial state — waiting for user input. */
    data object Idle : AuthUiState()

    /** A login request is in flight. */
    data object Loading : AuthUiState()

    /** Login succeeded. */
    data class Success(val displayName: String) : AuthUiState()

    /** Login failed with a human-readable [message]. */
    data class Error(val message: String) : AuthUiState()
}

/**
 * ViewModel that manages authentication state for the app.
 *
 * Uses [AndroidViewModel] (rather than plain [ViewModel]) because
 * [TokenStore] and [AuthRepository] require an application [Context].
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStore = TokenStore(application)
    private val repository = AuthRepository(tokenStore)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(repository.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _displayName = MutableStateFlow(repository.getDisplayName() ?: "")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    /**
     * Attempt to log in with the given [password].
     * On success the token is persisted and [isLoggedIn] flips to `true`.
     */
    fun login(password: String) {
        if (password.isBlank()) {
            _uiState.value = AuthUiState.Error("Password cannot be empty")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.login(password)
                .onSuccess { response ->
                    _displayName.value = response.displayName
                    _isLoggedIn.value = true
                    _uiState.value = AuthUiState.Success(response.displayName)
                }
                .onFailure { throwable ->
                    val msg = throwable.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "Login failed. Check your password and server."
                    _uiState.value = AuthUiState.Error(msg)
                }
        }
    }

    /** Clear the stored session and return to the login screen. */
    fun logout() {
        repository.logout()
        _isLoggedIn.value = false
        _displayName.value = ""
        _uiState.value = AuthUiState.Idle
    }
}
