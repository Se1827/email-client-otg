package com.se1827.emailclient.wear.auth

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Replicates the Python backend's PBKDF2-based authentication cryptography locally.
 *
 * Use this when the Android app needs to generate the auth token offline
 * (e.g. to decrypt Fernet-encrypted local files, or for peer-to-peer auth
 * without hitting /api/auth/login).
 *
 * Crypto parameters (must match backend .auth.json):
 *   Algorithm:   PBKDF2 with HMAC-SHA256
 *   Iterations:  260,000
 *   Key length:  256 bits (32 bytes)
 *   Encoding:    URL-safe Base64 with padding
 *   Verifier:    hex(SHA-256("email-agent-auth:<token>"))
 */
object AuthCrypto {

    private const val ITERATIONS = 260_000
    private const val KEY_LENGTH_BITS = 256
    private const val VERIFIER_PREFIX = "email-agent-auth:"

    /**
     * Derives the Fernet-compatible token from a plaintext password and
     * the URL-safe Base64-encoded salt stored in `.auth.json`.
     *
     * @param password  The user's plaintext password.
     * @param b64Salt   The salt value from `.auth.json`, encoded as URL-safe Base64.
     * @return          The derived token as a URL-safe Base64 string (with padding).
     */
    fun deriveToken(password: String, b64Salt: String): String {
        // 1. Decode the URL-safe base64 salt
        val saltBytes = Base64.decode(b64Salt, Base64.URL_SAFE or Base64.NO_WRAP)

        // 2. Run PBKDF2-HMAC-SHA256
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded

        // 3. Encode the resulting 32-byte key to URL-safe Base64 (with padding)
        return Base64.encodeToString(keyBytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Generates the verifier hash that can be compared against the `verifier`
     * field stored in `.auth.json`.
     *
     * @param token  The derived token (URL-safe Base64 string).
     * @return       Lowercase hex string of SHA-256("email-agent-auth:<token>").
     */
    fun generateVerifier(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest("$VERIFIER_PREFIX$token".toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validates a password locally against a stored salt and verifier
     * (from `.auth.json`), without making any network call.
     *
     * @param password        The user's plaintext password.
     * @param storedSalt      The `salt` field from `.auth.json` (URL-safe Base64).
     * @param storedVerifier  The `verifier` field from `.auth.json` (hex string).
     * @return                The derived token on success, or `null` if the password is wrong.
     */
    fun loginLocally(password: String, storedSalt: String, storedVerifier: String): String? {
        val token = deriveToken(password, storedSalt)
        val verifier = generateVerifier(token)
        return if (verifier == storedVerifier) token else null
    }
}
