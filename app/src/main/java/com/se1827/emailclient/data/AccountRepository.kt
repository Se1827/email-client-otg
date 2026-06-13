package com.se1827.emailclient.data

import com.se1827.emailclient.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountRepository(private val apiService: ApiService = NetworkClient.apiService) {

    suspend fun getAccounts(): Result<List<AccountConfigDto>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getAccounts() }
    }

    suspend fun createAccount(request: AccountConfigRequest): Result<AccountConfigDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.createAccount(request) }
    }

    suspend fun updateAccount(id: String, request: AccountConfigRequest): Result<AccountConfigDto> = withContext(Dispatchers.IO) {
        runCatching { apiService.updateAccount(id, request) }
    }

    suspend fun deleteAccount(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { apiService.deleteAccount(id) }
    }
}
