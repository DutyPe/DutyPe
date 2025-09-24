package com.example.partimes.apis

import com.example.partimes.jobseeker.api.JobSeekerApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://partimes-1746101452374.azurewebsites.net/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val jobSeekerApiService: JobSeekerApiService by lazy {
        retrofit.create(JobSeekerApiService::class.java)
    }
}
