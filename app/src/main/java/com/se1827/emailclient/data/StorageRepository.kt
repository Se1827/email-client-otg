package com.se1827.emailclient.data

import com.se1827.emailclient.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageRepository(private val apiService: ApiService = NetworkClient.apiService) {

    suspend fun getStorageStats(): Result<StorageStatsDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.getStorageStats() }
    }

    suspend fun wipeAllStorage(): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        runCatching { apiService.wipeAllStorage() }
    }

    suspend fun wipeEmailStorage(id: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        runCatching { apiService.wipeEmailStorage(id) }
    }
}
