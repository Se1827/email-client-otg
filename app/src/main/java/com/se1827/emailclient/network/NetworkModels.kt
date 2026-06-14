package com.se1827.emailclient.network

import com.google.gson.annotations.SerializedName

// ─── Core DTOs ────────────────────────────────────────────────────────────────

data class EmailDto(
    val id: String,
    val uid: Int?,
    val uidvalidity: Int?,
    val inbox: String?,
    @SerializedName("account_id") val accountId: String?,
    val sender: String,
    val recipients: List<String>,
    val cc: List<String>,
    val bcc: List<String>,
    val subject: String,
    val body: String,
    @SerializedName("html_body") val htmlBody: String?,
    val timestamp: String,
    @SerializedName("thread_id") val threadId: String?,
    @SerializedName("message_id") val messageId: String?,
    @SerializedName("in_reply_to") val inReplyTo: String?,
    val references: List<String>,
    @SerializedName("is_sent") val isSent: Boolean,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("is_starred") val isStarred: Boolean,
    val labels: List<String>,
    val classification: ClassificationDto?,
    @SerializedName("draft_reply") val draftReply: DraftReplyDto?,
    @SerializedName("storage_origin") val storageOrigin: String?
)

data class ClassificationDto(
    val priority: String,
    val category: String,
    val confidence: Float,
    val reasoning: String
)

data class DraftReplyDto(
    val body: String,
    val tone: String,
    val quality: String,
    @SerializedName("pii_redacted") val piiRedacted: Boolean,
    @SerializedName("redacted_types") val redactedTypes: List<String>
)

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

data class AccountSummaryDto(
    val id: String,
    val name: String,
    val email: String,
    val provider: String,
    @SerializedName("email_count") val emailCount: Int = 0,
    @SerializedName("unread_count") val unreadCount: Int = 0
)

data class AccountConfigDto(
    val id: String,
    val name: String,
    val email: String,
    val provider: String,
    @SerializedName("imap_host") val imapHost: String,
    @SerializedName("imap_port") val imapPort: Int,
    @SerializedName("imap_user") val imapUser: String,
    @SerializedName("imap_mailbox") val imapMailbox: String,
    @SerializedName("imap_use_ssl") val imapUseSsl: Boolean,
    @SerializedName("smtp_host") val smtpHost: String,
    @SerializedName("smtp_port") val smtpPort: Int,
    @SerializedName("smtp_user") val smtpUser: String,
    @SerializedName("smtp_use_ssl") val smtpUseSsl: Boolean,
    @SerializedName("smtp_use_tls") val smtpUseTls: Boolean,
    @SerializedName("has_smtp") val hasSmtp: Boolean,
    val color: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("has_password") val hasPassword: Boolean
)

data class CalendarEventDto(
    val id: String?,
    val title: String,
    val start: String,
    val end: String,
    val description: String,
    val location: String,
    val color: String,
    val attendees: List<String>,
    @SerializedName("account_id") val accountId: String?,
    @SerializedName("is_all_day") val isAllDay: Boolean,
    val recurrence: String?
)

data class DashboardStatsDto(
    @SerializedName("total_emails") val totalEmails: Int,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("classified_count") val classifiedCount: Int,
    @SerializedName("starred_count") val starredCount: Int,
    @SerializedName("priority_breakdown") val priorityBreakdown: Map<String, Int>,
    @SerializedName("category_breakdown") val categoryBreakdown: Map<String, Int>,
    val accounts: List<AccountSummaryDto>,
    @SerializedName("upcoming_events") val upcomingEvents: List<CalendarEventDto>,
    val notifications: List<NotificationDto>,
    @SerializedName("recent_activity") val recentActivity: List<Map<String, Any?>>? = null,
    @SerializedName("storage_stats") val storageStats: StorageStatsDto? = null
)

data class GraphStatusDto(
    val mode: String,
    @SerializedName("user_email") val userEmail: String?,
    val tip: String?
)

data class GraphConfigDto(
    @SerializedName("use_mock") val useMock: Boolean,
    @SerializedName("tenant_id") val tenantId: String?,
    @SerializedName("client_id") val clientId: String?
)

data class StorageStatsDto(
    val emails: Int?,
    val classifications: Int?,
    val drafts: Int?,
    val events: Int?,
    val accounts: Int?,
    val notifications: Int?
)

// ─── Request Bodies ───────────────────────────────────────────────────────────

data class DraftQualityRequest(val quality: String)

data class ComposeEmailRequest(
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val subject: String,
    val body: String,
    @SerializedName("account_id") val accountId: String?
)

data class AiComposeRequest(val prompt: String, val quality: String = "balanced")

data class SendReplyRequest(
    val body: String,
    val to: List<String>? = null,
    val cc: List<String>? = null,
    val bcc: List<String>? = null,
    val action: String = "reply"
)

data class CreateEventRequest(
    val title: String,
    val start: String,
    val end: String,
    val description: String = "",
    val location: String = "",
    val color: String = "#6366F1",
    val attendees: List<String> = emptyList(),
    @SerializedName("account_id") val accountId: String? = null,
    @SerializedName("is_all_day") val isAllDay: Boolean = false,
    val recurrence: String? = null,
    @SerializedName("sync_to_graph") val syncToGraph: Boolean = false
)

data class AccountConfigRequest(
    val name: String,
    val email: String,
    val provider: String,
    @SerializedName("imap_host") val imapHost: String,
    @SerializedName("imap_port") val imapPort: Int,
    @SerializedName("imap_user") val imapUser: String,
    @SerializedName("imap_mailbox") val imapMailbox: String = "INBOX",
    @SerializedName("imap_use_ssl") val imapUseSsl: Boolean = true,
    @SerializedName("smtp_host") val smtpHost: String,
    @SerializedName("smtp_port") val smtpPort: Int,
    @SerializedName("smtp_user") val smtpUser: String,
    @SerializedName("smtp_use_ssl") val smtpUseSsl: Boolean = false,
    @SerializedName("smtp_use_tls") val smtpUseTls: Boolean = true,
    val password: String? = null,
    val color: String = "#6366F1",
    @SerializedName("is_active") val isActive: Boolean = true
)

// ─── Response Bodies ──────────────────────────────────────────────────────────

data class RefreshResponse(
    val status: String,
    @SerializedName("new_count") val newCount: Int,
    @SerializedName("total_count") val totalCount: Int
)

data class StarResponse(
    val id: String,
    @SerializedName("is_starred") val isStarred: Boolean
)

data class ReadResponse(
    val id: String,
    @SerializedName("is_read") val isRead: Boolean
)

data class ApproveResponse(
    val status: String,
    val preview: String?,
    @SerializedName("sent_email") val sentEmail: EmailDto?
)

data class AiComposeResponse(val draft: String)

data class ClassifyAllItemDto(
    @SerializedName("email_id") val emailId: String,
    val classification: ClassificationDto
)

data class SyncResponse(val status: String, val message: String? = null)

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
    val sender: String? = null,
    @SerializedName("deadline_terms") val deadlineTerms: List<String>? = null
)

data class DeadlineResponseDto(
    val deadlines: List<DeadlineItemDto>,
    val count: Int,
    @SerializedName("window_days") val windowDays: Int,
    @SerializedName("generated_at") val generatedAt: String
)

// ─── Action Item DTOs ─────────────────────────────────────────────────────────

data class ActionItemDto(
    val id: String,
    val description: String,
    @SerializedName("due_date") val dueDate: String?,
    val status: String,                                  // "pending", "in_progress", "completed", "dismissed"
    val priority: String,
    @SerializedName("email_id") val emailId: String?,
    @SerializedName("is_overdue") val isOverdue: Boolean,
    @SerializedName("hours_overdue") val hoursOverdue: Int,
    @SerializedName("source_subject") val sourceSubject: String?
)

data class UpdateActionStatusRequest(val status: String)

// ─── App Settings DTO ─────────────────────────────────────────────────────────

data class AppSettingsDto(
    @SerializedName("groq_api_key") val groqApiKey: String,
    @SerializedName("groq_api_key_set") val groqApiKeySet: Boolean,
    @SerializedName("groq_model") val groqModel: String,
    @SerializedName("email_source") val emailSource: String,
    @SerializedName("pii_mode") val piiMode: String,
    @SerializedName("default_draft_quality") val defaultDraftQuality: String,
    @SerializedName("storage_enabled") val storageEnabled: Boolean,
    @SerializedName("database_url") val databaseUrl: String? = null,
    @SerializedName("otel_enabled") val otelEnabled: Boolean,
    @SerializedName("otel_service_name") val otelServiceName: String,
    @SerializedName("otel_exporter_otlp_endpoint") val otelExporterOtlpEndpoint: String
)

// ─── Auth ─────────────────────────────────────────────────────────────────────

data class LoginRequest(val password: String)

data class LoginResponse(
    val status: String,
    val token: String,
    @SerializedName("display_name") val displayName: String
)
