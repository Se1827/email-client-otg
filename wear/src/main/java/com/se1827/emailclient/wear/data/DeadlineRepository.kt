package com.se1827.emailclient.wear.data

import com.se1827.emailclient.wear.network.ApiService
import com.se1827.emailclient.wear.network.DeadlineResponseDto
import com.se1827.emailclient.wear.network.NetworkClient
import com.se1827.emailclient.wear.network.NotificationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeadlineRepository(private val api: ApiService = NetworkClient.apiService) {

    suspend fun getDeadlines(days: Int = 7): Result<DeadlineResponseDto> =
        withContext(Dispatchers.IO) { runCatching { api.getDeadlines(days) } }

    suspend fun getNotifications(): Result<List<NotificationDto>> =
        withContext(Dispatchers.IO) { runCatching { api.getNotifications() } }

    suspend fun dismissNotification(id: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { api.dismissNotification(id) } }
}
