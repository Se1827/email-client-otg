package com.se1827.emailclient.data

import com.se1827.emailclient.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GraphRepository(private val apiService: ApiService = NetworkClient.apiService) {

    suspend fun getGraphStatus(): Result<GraphStatusDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.getGraphStatus() }
    }

    suspend fun getGraphConfig(): Result<GraphConfigDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.getGraphConfig() }
    }

    suspend fun updateGraphConfig(config: GraphConfigDto): Result<GraphConfigDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.updateGraphConfig(config) }
    }
}
