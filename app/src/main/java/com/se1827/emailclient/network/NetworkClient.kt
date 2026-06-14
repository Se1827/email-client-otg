package com.se1827.emailclient.network

import android.content.Context
import com.se1827.emailclient.BuildConfig
import com.se1827.emailclient.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton that provides the [Retrofit] instance and [ApiService].
 *
 * Must be initialised once via [init] before first use (typically in
 * [MainActivity.onCreate]) so that the [AuthInterceptor] can read
 * the stored Bearer token from [TokenStore].
 */
object NetworkClient {

    @Volatile
    private var tokenStore: TokenStore? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    /**
     * OkHttp [Interceptor] that injects the `Authorization: Bearer <token>`
     * header into every request except the login endpoint itself.
     */
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = tokenStore?.getToken()

        val request = if (token != null && !original.url.encodedPath.contains("api/auth/login")) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    /**
     * Initialise the network layer with the application [Context] so the
     * [AuthInterceptor] can look up the persisted token.
     *
     * Safe to call multiple times; only the first call has an effect.
     */
    fun init(context: Context) {
        if (tokenStore == null) {
            synchronized(this) {
                if (tokenStore == null) {
                    tokenStore = TokenStore(context.applicationContext)
                }
            }
        }
    }
}
