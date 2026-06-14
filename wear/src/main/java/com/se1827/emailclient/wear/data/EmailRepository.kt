package com.se1827.emailclient.wear.data

import com.se1827.emailclient.wear.data.model.EmailItem
import com.se1827.emailclient.wear.network.ApiService
import com.se1827.emailclient.wear.network.EmailDto
import com.se1827.emailclient.wear.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmailRepository(private val apiService: ApiService = NetworkClient.apiService) {

    suspend fun getPendingDrafts(): List<EmailItem> = withContext(Dispatchers.IO) {
        val dtos = apiService.getEmails()
        dtos.filter { it.draftReply != null && !it.isRead }
            .map { it.toEmailItem() }
    }

    suspend fun approveDraft(id: String) = withContext(Dispatchers.IO) {
        apiService.approveDraft(id)
    }

    suspend fun skipDraft(id: String) = withContext(Dispatchers.IO) {
        apiService.markRead(id)
    }

    private fun EmailDto.toEmailItem(): EmailItem {
        val draftBody = draftReply?.body ?: ""
        val preview = if (draftBody.length > 50) "${draftBody.take(50)}..." else draftBody
        
        return EmailItem(
            id = id,
            senderDisplayName = sender.substringBefore("@").replaceFirstChar { it.uppercase() },
            draftPreview = preview,
            fullDraftBody = draftBody,
            priority = classification?.priority ?: "normal",
            category = classification?.category ?: "inbox"
        )
    }
}
