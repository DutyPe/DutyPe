package com.example.dutype.api.employer

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object JobPostingApiClient {
    private const val BASE_URL = "http://192.168.7.6:8080/api/" // Use 10.0.2.2 for Android emulator

    val api: JobPostingApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JobPostingApi::class.java)
    }
}

