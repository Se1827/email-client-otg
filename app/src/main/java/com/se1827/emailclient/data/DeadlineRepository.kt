package com.se1827.emailclient.data

import com.se1827.emailclient.network.DeadlineResponseDto
import com.se1827.emailclient.network.NetworkClient
import com.se1827.emailclient.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeadlineRepository(private val api: ApiService = NetworkClient.apiService) {

    suspend fun getDeadlines(days: Int = 7): Result<DeadlineResponseDto> =
        withContext(Dispatchers.IO) { runCatching { api.getDeadlines(days) } }
}
