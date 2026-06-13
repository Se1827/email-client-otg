package com.se1827.emailclient.data

import com.se1827.emailclient.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmailRepository(private val apiService: ApiService = NetworkClient.apiService) {

    suspend fun getEmails(accountId: String? = null): Result<List<EmailDto>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getEmails(accountId) }
    }

    suspend fun getEmailById(id: String): Result<EmailDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.getEmailById(id) }
    }

    suspend fun getEmailThread(id: String): Result<List<EmailDto>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getEmailThread(id) }
    }

    suspend fun classifyEmail(id: String, force: Boolean = false): Result<ClassificationDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.classifyEmail(id, force) }
    }

    suspend fun generateDraft(id: String, quality: String = "balanced", force: Boolean = false): Result<DraftReplyDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.generateDraft(id, DraftQualityRequest(quality), force) }
    }

    suspend fun approveDraft(id: String): Result<ApproveResponse> = withContext(Dispatchers.IO) {
        runCatching { apiService.approveDraft(id) }
    }

    suspend fun sendReply(id: String, request: SendReplyRequest): Result<EmailDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.sendReply(id, request) }
    }

    suspend fun composeEmail(request: ComposeEmailRequest): Result<EmailDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.composeEmail(request) }
    }

    suspend fun aiComposeEmail(prompt: String, quality: String = "balanced"): Result<AiComposeResponse> = withContext(Dispatchers.IO) {
        runCatching { apiService.aiComposeEmail(AiComposeRequest(prompt, quality)) }
    }

    suspend fun toggleStar(id: String): Result<StarResponse> = withContext(Dispatchers.IO) {
        runCatching { apiService.toggleStar(id) }
    }

    suspend fun markRead(id: String): Result<ReadResponse> = withContext(Dispatchers.IO) {
        runCatching { apiService.markRead(id) }
    }

    suspend fun classifyAll(accountId: String? = null): Result<List<ClassifyAllItemDto>> = withContext(Dispatchers.IO) {
        runCatching { apiService.classifyAll(accountId) }
    }

    suspend fun refreshEmails(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching { apiService.refreshEmails().newCount }
    }

    suspend fun getDashboardStats(): Result<DashboardStatsDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.getDashboardStats() }
    }

    suspend fun getNotifications(): Result<List<NotificationDto>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getNotifications() }
    }

    suspend fun dismissNotification(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { apiService.dismissNotification(id) }
    }
}
