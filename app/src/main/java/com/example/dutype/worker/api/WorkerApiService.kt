package com.example.dutype.worker.api

import com.example.dutype.worker.models.JobApplication
import com.example.dutype.models.JobListing
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import retrofit2.Call
import retrofit2.Response

interface WorkerApiService {

    @GET("/api/jobs/{jobId}")
    fun getJobById(@Path("jobId") jobId: String): Call<JobListing>

    @GET("/api/jobs/all")
    suspend fun getAllJobs(): List<JobListing>

    @GET("/api/jobs/search")
    suspend fun searchJobs(@Query("query") query: String): List<JobListing>

    // Application-related endpoints
    @POST("/api/applications")
    suspend fun submitApplication(@Body application: JobApplication): Response<ApplicationResponse>

    @GET("/api/applications/{userId}")
    suspend fun getApplications(@Path("userId") userId: String): Response<List<JobApplication>>

    @PUT("/api/applications/{applicationId}/status")
    suspend fun updateApplicationStatus(
        @Path("applicationId") applicationId: String,
        @Body request: UpdateStatusRequest
    ): Response<Unit>

    @DELETE("/api/applications/{applicationId}")
    suspend fun withdrawApplication(@Path("applicationId") applicationId: String): Response<Unit>

    // File upload endpoints
    @Multipart
    @POST("/api/upload/document")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody,
        @Part("userId") userId: RequestBody
    ): Response<DocumentResponse>

    // Legacy endpoints (keeping for backward compatibility)
    @POST("/api/jobs/apply")
    fun applyToJob(@Body applicationData: Map<String, Any>): Call<Any>

    @GET("/api/jobs/saved/{userId}")
    suspend fun getSavedJobs(@Path("userId") userId: String): List<JobListing>

    @POST("/api/jobs/save")
    fun saveJob(@Body saveData: Map<String, String>): Call<Any>
}

// Response models for API calls
data class ApplicationResponse(
    val success: Boolean,
    val applicationId: String,
    val message: String
)

data class UpdateStatusRequest(
    val status: String,
    val message: String? = null
)

data class DocumentResponse(
    val success: Boolean,
    val documentId: String,
    val url: String,
    val message: String
)
