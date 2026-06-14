package com.se1827.emailclient.wear.network

import com.google.gson.annotations.SerializedName

// ─── Email DTOs ───────────────────────────────────────────────────────────────

data class EmailDto(
    val id: String,
    @SerializedName("account_id") val accountId: String?,
    val sender: String,
    val subject: String,
    val body: String,
    val timestamp: String,
    @SerializedName("is_read") val isRead: Boolean,
    val classification: ClassificationDto?,
    @SerializedName("draft_reply") val draftReply: DraftReplyDto?
)

data class ClassificationDto(
    val priority: String,
    val category: String
)

data class DraftReplyDto(
    val body: String
)

data class ApproveResponse(
    val status: String,
    val preview: String?,
    @SerializedName("sent_email") val sentEmail: EmailDto?
)

data class ReadResponse(
    val id: String,
    @SerializedName("is_read") val isRead: Boolean
)

// ─── Auth DTOs ────────────────────────────────────────────────────────────────

data class LoginRequest(
    val password: String
)

data class LoginResponse(
    val token: String,
    @SerializedName("display_name") val displayName: String
)

// ─── Deadline DTOs ────────────────────────────────────────────────────────────

data class DeadlineItemDto(
    val type: String,                                    // "action", "event", "email_deadline"
    val id: String,
    val title: String,
    val due: String,                                     // ISO 8601
    @SerializedName("is_overdue") val isOverdue: Boolean,
    @SerializedName("hours_overdue") val hoursOverdue: Int,
    val priority: String,                                // "critical", "high", "normal", "low"
    @SerializedName("email_id") val emailId: String?,
    val source: String,                                  // "action_item", "calendar", "email"
    @SerializedName("is_all_day") val isAllDay: Boolean? = null,
    val location: String? = null,
    val sender: String? = null
)

data class DeadlineResponseDto(
    val deadlines: List<DeadlineItemDto>,
    val count: Int,
    @SerializedName("window_days") val windowDays: Int,
    @SerializedName("generated_at") val generatedAt: String
)

// ─── Notification DTOs ────────────────────────────────────────────────────────

data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val severity: String,
    @SerializedName("related_id") val relatedId: String?,
    @SerializedName("related_type") val relatedType: String?,
    val timestamp: String,
    @SerializedName("is_read") val isRead: Boolean
)
