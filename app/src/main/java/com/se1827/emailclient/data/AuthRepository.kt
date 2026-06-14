package com.se1827.emailclient.data

import com.se1827.emailclient.auth.TokenStore
import com.se1827.emailclient.network.LoginRequest
import com.se1827.emailclient.network.LoginResponse
import com.se1827.emailclient.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that bridges the `/api/auth/login` endpoint with local
 * token storage ([TokenStore]).
 */
class AuthRepository(
    private val tokenStore: TokenStore
) {
    private val apiService get() = NetworkClient.apiService

    /**
     * Authenticate with the backend. On success the token and display
     * name are persisted in [TokenStore] so subsequent app launches
     * can skip the login screen.
     */
    suspend fun login(password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiService.login(LoginRequest(password))
            tokenStore.saveSession(response.token, response.displayName)
            response
        }
    }

    /** Clear the stored session and return to the unauthenticated state. */
    fun logout() {
        tokenStore.clearSession()
    }

    /** Whether a persisted token exists. */
    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    /** The display name received on the last successful login, if any. */
    fun getDisplayName(): String? = tokenStore.getDisplayName()
}
