package com.example.partimes.jobseeker.api

import com.example.partimes.models.JobListing
import retrofit2.http.*
import retrofit2.Call

interface JobSeekerApiService {

    @GET("/api/jobs/{jobId}")
    fun getJobById(@Path("jobId") jobId: String): Call<JobListing>

    @GET("/api/jobs/all")
    suspend fun getAllJobs(): List<JobListing>

    @GET("/api/jobs/search")
    suspend fun searchJobs(@Query("query") query: String): List<JobListing>

    @POST("/api/jobs/apply")
    fun applyToJob(@Body applicationData: Map<String, Any>): Call<Any>

    @GET("/api/jobs/saved/{userId}")
    suspend fun getSavedJobs(@Path("userId") userId: String): List<JobListing>

    @POST("/api/jobs/save")
    fun saveJob(@Body saveData: Map<String, String>): Call<Any>
}
