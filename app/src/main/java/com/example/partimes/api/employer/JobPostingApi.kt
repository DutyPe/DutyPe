package com.example.partimes.api.employer

import com.example.partimes.employer.models.JobPostingModel
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface JobPostingApi {
    @POST("jobs")
    suspend fun postJob(@Body job: JobPostingModel): JobPostingModel

    @GET("jobs")
    suspend fun getJobs(): List<JobPostingModel>

    @GET("jobs/{id}")
    suspend fun getJob(@Path("id") id: String): JobPostingModel
}

