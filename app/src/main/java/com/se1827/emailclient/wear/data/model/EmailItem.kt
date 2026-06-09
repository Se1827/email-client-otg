package com.se1827.emailclient.wear.data.model

data class EmailItem(
    val id: String,
    val senderDisplayName: String,
    val draftPreview: String,
    val fullDraftBody: String,
    val priority: String,
    val category: String
)
