package com.example.fyp_fitledger

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import kotlinx.coroutines.Deferred

//Makes HTTP requests to OpenAI API (using Retrofit, OkHttp, or Axios).
interface ChatGPTApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.openai.com/"

    val api: ChatGPTApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatGPTApiService::class.java)
    }
}
