package com.se1827.emailclient.wear.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Securely persists the authentication token and display name using
 * [EncryptedSharedPreferences] (AES-256-SIV for keys, AES-256-GCM for values).
 *
 * Falls back to regular [SharedPreferences] if the crypto setup fails
 * (e.g. on very old devices or during Robolectric tests).
 */
class TokenStore(context: Context) {

    companion object {
        private const val TAG = "TokenStore"
        private const val PREFS_NAME = "email_agent_auth"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_DISPLAY_NAME = "display_name"
    }

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back to plain prefs", e)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Persist a successful login session. */
    fun saveSession(token: String, displayName: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
    }

    /** Returns the stored Bearer token, or `null` if not logged in. */
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** Returns the display name received from the backend on login. */
    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    /** Whether a valid session exists (i.e. a token is stored). */
    fun isLoggedIn(): Boolean = getToken() != null

    /** Wipe the stored session (logout). */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
