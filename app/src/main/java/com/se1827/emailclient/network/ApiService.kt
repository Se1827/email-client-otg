package com.se1827.emailclient.network

import retrofit2.http.*

interface ApiService {

    // ─── Emails ───────────────────────────────────────────────────────────────
    @GET("api/emails")
    suspend fun getEmails(@Query("account_id") accountId: String? = null): List<EmailDto>

    @GET("api/emails/{id}")
    suspend fun getEmailById(@Path("id") id: String): EmailDto

    @GET("api/emails/{id}/thread")
    suspend fun getEmailThread(@Path("id") id: String): List<EmailDto>

    @POST("api/emails/{id}/classify")
    suspend fun classifyEmail(
        @Path("id") id: String,
        @Query("force") force: Boolean = false
    ): ClassificationDto

    @POST("api/emails/{id}/draft")
    suspend fun generateDraft(
        @Path("id") id: String,
        @Body request: DraftQualityRequest,
        @Query("force") force: Boolean = false
    ): DraftReplyDto

    @POST("api/emails/{id}/approve")
    suspend fun approveDraft(@Path("id") id: String): ApproveResponse

    @POST("api/emails/{id}/send-reply")
    suspend fun sendReply(@Path("id") id: String, @Body request: SendReplyRequest): EmailDto

    @POST("api/emails/compose")
    suspend fun composeEmail(@Body request: ComposeEmailRequest): EmailDto

    @POST("api/emails/ai-compose")
    suspend fun aiComposeEmail(@Body request: AiComposeRequest): AiComposeResponse

    @POST("api/emails/{id}/star")
    suspend fun toggleStar(@Path("id") id: String): StarResponse

    @POST("api/emails/{id}/read")
    suspend fun markRead(@Path("id") id: String): ReadResponse

    @POST("api/emails/classify-all")
    suspend fun classifyAll(@Query("account_id") accountId: String? = null): List<ClassifyAllItemDto>

    @POST("api/emails/refresh")
    suspend fun refreshEmails(): RefreshResponse

    // ─── Dashboard & Notifications ─────────────────────────────────────────────
    @GET("api/dashboard")
    suspend fun getDashboardStats(): DashboardStatsDto

    @GET("api/notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @POST("api/notifications/{id}/dismiss")
    suspend fun dismissNotification(@Path("id") id: String)

    // ─── Calendar ─────────────────────────────────────────────────────────────
    @GET("api/calendar")
    suspend fun getCalendarEvents(): List<CalendarEventDto>

    @GET("api/calendar/upcoming")
    suspend fun getUpcomingEvents(@Query("days") days: Int = 7): List<CalendarEventDto>

    @POST("api/calendar/events")
    suspend fun createCalendarEvent(@Body request: CreateEventRequest): CalendarEventDto

    @PUT("api/calendar/events/{id}")
    suspend fun updateCalendarEvent(@Path("id") id: String, @Body request: CreateEventRequest): CalendarEventDto

    @DELETE("api/calendar/events/{id}")
    suspend fun deleteCalendarEvent(@Path("id") id: String)

    @POST("api/calendar/sync")
    suspend fun syncCalendar(): SyncResponse

    // ─── Accounts ─────────────────────────────────────────────────────────────
    @GET("api/accounts")
    suspend fun getAccounts(): List<AccountConfigDto>

    @POST("api/accounts")
    suspend fun createAccount(@Body account: AccountConfigRequest): AccountConfigDto

    @PUT("api/accounts/{id}")
    suspend fun updateAccount(@Path("id") id: String, @Body account: AccountConfigRequest): AccountConfigDto

    @DELETE("api/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: String)

    // ─── Microsoft Graph ──────────────────────────────────────────────────────
    @GET("api/graph/status")
    suspend fun getGraphStatus(): GraphStatusDto

    @GET("api/graph/config")
    suspend fun getGraphConfig(): GraphConfigDto

    @POST("api/graph/config")
    suspend fun updateGraphConfig(@Body config: GraphConfigDto): GraphConfigDto

    // ─── Storage ──────────────────────────────────────────────────────────────
    @GET("api/storage/stats")
    suspend fun getStorageStats(): StorageStatsDto

    @DELETE("api/storage")
    suspend fun wipeAllStorage(): Map<String, Any>

    @DELETE("api/storage/emails/{id}")
    suspend fun wipeEmailStorage(@Path("id") id: String): Map<String, Any>
}
