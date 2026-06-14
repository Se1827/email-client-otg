package com.se1827.emailclient.wear.network

import com.google.gson.annotations.SerializedName

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
