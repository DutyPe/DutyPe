package com.example.partimes.apis

import com.example.partimes.models.JobListing
import retrofit2.http.*
import retrofit2.Call

interface ApiService {

    @POST("/api/jobs/post")
    fun postJob(@Body jobListing: JobListing): Call<JobListing>

    @GET("/api/jobs/{jobId}")
    fun getJobById(@Path("jobId") jobId: String): Call<JobListing>

    @GET("/api/jobs/all")
    suspend fun getAllJobs(): List<JobListing>

}
