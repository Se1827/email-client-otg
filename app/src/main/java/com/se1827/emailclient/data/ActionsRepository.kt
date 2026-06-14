package com.se1827.emailclient.data

import com.se1827.emailclient.network.ActionItemDto
import com.se1827.emailclient.network.ApiService
import com.se1827.emailclient.network.NetworkClient
import com.se1827.emailclient.network.UpdateActionStatusRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActionsRepository(private val api: ApiService = NetworkClient.apiService) {

    suspend fun getActions(status: String? = null): Result<List<ActionItemDto>> =
        withContext(Dispatchers.IO) { runCatching { api.getActions(status = status) } }

    suspend fun updateActionStatus(id: String, status: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { api.updateActionStatus(id, UpdateActionStatusRequest(status)); Unit }
        }
}
