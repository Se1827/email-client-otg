package com.se1827.emailclient.wear.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ─── Auth ─────────────────────────────────────────────────────────────────
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // ─── Emails ───────────────────────────────────────────────────────────────
    @GET("api/emails")
    suspend fun getEmails(): List<EmailDto>

    @POST("api/emails/{id}/approve")
    suspend fun approveDraft(@Path("id") id: String): ApproveResponse

    @POST("api/emails/{id}/read")
    suspend fun markRead(@Path("id") id: String): ReadResponse

    // ─── Deadlines ────────────────────────────────────────────────────────────
    @GET("api/deadlines")
    suspend fun getDeadlines(@Query("days") days: Int = 7): DeadlineResponseDto

    // ─── Notifications ────────────────────────────────────────────────────────
    @GET("api/notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @POST("api/notifications/{id}/dismiss")
    suspend fun dismissNotification(@Path("id") id: String)
}
