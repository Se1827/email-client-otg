package com.se1827.emailclient.wear.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("api/emails")
    suspend fun getEmails(): List<EmailDto>

    @POST("api/emails/{id}/approve")
    suspend fun approveDraft(@Path("id") id: String): ApproveResponse

    @POST("api/emails/{id}/read")
    suspend fun markRead(@Path("id") id: String): ReadResponse
}
