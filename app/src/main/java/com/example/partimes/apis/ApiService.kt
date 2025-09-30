package com.example.partimes.apis

import com.example.partimes.models.JobListing
import com.example.partimes.models.User
import com.example.partimes.models.JobApplication
import com.example.partimes.models.ApiResponse
import retrofit2.http.*
import retrofit2.Call
import retrofit2.Response

interface ApiService {
    
    // Authentication endpoints
    @POST("/api/auth/verify-phone")
    suspend fun verifyPhone(@Body request: Map<String, String>): Response<Map<String, Any>>
    
    @POST("/api/auth/create-user")
    suspend fun createUser(@Body request: Map<String, String>): Response<Map<String, Any>>
    
    @GET("/api/auth/profile")
    suspend fun getProfile(): Response<Map<String, Any>>
    
    @PUT("/api/profile")
    suspend fun updateProfile(
        @Body user: User
    ): Response<Map<String, Any>>
    
    @POST("/api/auth/logout")
    suspend fun logout(): Response<Map<String, Any>>
    
    // Job endpoints
    @GET("/api/jobs")
    suspend fun getAllJobs(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/jobs/{id}")
    suspend fun getJobById(@Path("id") jobId: String): Response<ApiResponse<Map<String, Any>>>
    
    @POST("/api/jobs")
    suspend fun createJob(
        @Body job: JobListing
    ): Response<ApiResponse<Map<String, Any>>>
    
    @PUT("/api/jobs/{id}")
    suspend fun updateJob(
        @Path("id") jobId: String,
        @Body job: JobListing
    ): Response<ApiResponse<Map<String, Any>>>
    
    @DELETE("/api/jobs/{id}")
    suspend fun deleteJob(
        @Path("id") jobId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/jobs/search")
    suspend fun searchJobs(
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/jobs/filter")
    suspend fun filterJobs(
        @Query("city") city: String? = null,
        @Query("payType") payType: String? = null,
        @Query("jobType") jobType: String? = null,
        @Query("category") category: String? = null,
        @Query("urgency") urgency: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/jobs/employer/{employerId}")
    suspend fun getJobsByEmployer(@Path("employerId") employerId: String): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/jobs/verified")
    suspend fun getVerifiedJobs(): Response<ApiResponse<Map<String, Any>>>
    
    // Application endpoints
    @POST("/api/applications")
    suspend fun submitApplication(
        @Body application: JobApplication
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/applications/my-applications")
    suspend fun getMyApplications(): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/applications/{id}")
    suspend fun getApplication(
        @Path("id") applicationId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @PUT("/api/applications/{id}/status")
    suspend fun updateApplicationStatus(
        @Path("id") applicationId: String,
        @Body statusUpdate: Map<String, String>
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/applications/job/{jobId}")
    suspend fun getApplicationsForJob(@Path("jobId") jobId: String): Response<ApiResponse<Map<String, Any>>>
    
    @PUT("/api/applications/{id}/withdraw")
    suspend fun withdrawApplication(
        @Path("id") applicationId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/applications/status/{status}")
    suspend fun getApplicationsByStatus(
        @Path("status") status: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    // File upload endpoints
    @Multipart
    @POST("/api/upload/document")
    suspend fun uploadDocument(
        @Part file: okhttp3.MultipartBody.Part,
        @Part("type") type: String? = null
    ): Response<ApiResponse<Map<String, Any>>>
    
    
    @DELETE("/api/upload/{filename}")
    suspend fun deleteFile(@Path("filename") filename: String): Response<ApiResponse<Map<String, Any>>>
    
    // Notification endpoints
    @GET("/api/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/notifications/unread")
    suspend fun getUnreadNotifications(): Response<ApiResponse<Map<String, Any>>>
    
    @PUT("/api/notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Path("id") notificationId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @PUT("/api/notifications/mark-all-read")
    suspend fun markAllNotificationsAsRead(): Response<ApiResponse<Map<String, Any>>>
    
    @DELETE("/api/notifications/{id}")
    suspend fun deleteNotification(
        @Path("id") notificationId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/notifications/{id}")
    suspend fun getNotification(
        @Path("id") notificationId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    // Profile management endpoints
    
    @Multipart
    @POST("/api/profile/upload-image")
    suspend fun uploadProfileImage(
        @Part file: okhttp3.MultipartBody.Part
    ): Response<ApiResponse<Map<String, Any>>>
    
    @DELETE("/api/profile/image")
    suspend fun deleteProfileImage(): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/profile/completion")
    suspend fun getProfileCompletion(): Response<ApiResponse<Map<String, Any>>>
    
    // Job recommendations endpoints
    @GET("/api/recommendations/personalized")
    suspend fun getPersonalizedRecommendations(
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/recommendations/similar/{jobId}")
    suspend fun getSimilarJobs(
        @Path("jobId") jobId: String,
        @Query("limit") limit: Int = 5
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/recommendations/popular")
    suspend fun getPopularJobs(@Query("limit") limit: Int = 10): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/recommendations/trending")
    suspend fun getTrendingJobs(@Query("limit") limit: Int = 10): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/recommendations/location-based")
    suspend fun getLocationBasedRecommendations(
        @Query("location") location: String,
        @Query("limit") limit: Int = 10
    ): Response<ApiResponse<Map<String, Any>>>
    
    // Saved jobs endpoints
    @POST("/api/saved-jobs/{jobId}")
    suspend fun saveJob(
        @Path("jobId") jobId: String,
        @Query("notes") notes: String? = null
    ): Response<ApiResponse<Map<String, Any>>>
    
    @DELETE("/api/saved-jobs/{jobId}")
    suspend fun unsaveJob(
        @Path("jobId") jobId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/saved-jobs")
    suspend fun getSavedJobs(): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/saved-jobs/details")
    suspend fun getSavedJobDetails(): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/saved-jobs/{jobId}/status")
    suspend fun isJobSaved(
        @Path("jobId") jobId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @PUT("/api/saved-jobs/{jobId}/notes")
    suspend fun updateSavedJobNotes(
        @Path("jobId") jobId: String,
        @Query("notes") notes: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/saved-jobs/count")
    suspend fun getSavedJobCount(): Response<ApiResponse<Map<String, Any>>>
    
    // Job sharing endpoints
    @GET("/api/share/job/{jobId}")
    suspend fun getJobShareData(@Path("jobId") jobId: String): Response<ApiResponse<Map<String, Any>>>
    
    @POST("/api/share/collection")
    suspend fun getJobCollectionShareData(@Body request: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/share/whatsapp/{jobId}")
    suspend fun getWhatsAppShareData(@Path("jobId") jobId: String): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/share/facebook/{jobId}")
    suspend fun getFacebookShareData(@Path("jobId") jobId: String): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/share/twitter/{jobId}")
    suspend fun getTwitterShareData(@Path("jobId") jobId: String): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/share/linkedin/{jobId}")
    suspend fun getLinkedInShareData(@Path("jobId") jobId: String): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/share/email/{jobId}")
    suspend fun getEmailShareData(@Path("jobId") jobId: String): Response<ApiResponse<Map<String, Any>>>
    
    // Location services endpoints
    @GET("/api/location/jobs/nearby")
    suspend fun getJobsNearLocation(
        @Query("city") city: String,
        @Query("radiusKm") radiusKm: Double = 25.0
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/location/jobs/distance")
    suspend fun getJobsByDistance(
        @Query("userCity") userCity: String,
        @Query("userLat") userLat: Double,
        @Query("userLon") userLon: Double,
        @Query("maxDistanceKm") maxDistanceKm: Double = 50.0
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/location/stats/{city}")
    suspend fun getLocationBasedJobStats(@Path("city") city: String): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/location/nearby-cities")
    suspend fun getNearbyCities(
        @Query("city") city: String,
        @Query("radiusKm") radiusKm: Double = 100.0
    ): Response<ApiResponse<Map<String, Any>>>
    
    @POST("/api/location/jobs/multiple-cities")
    suspend fun getJobsInMultipleCities(@Body request: Map<String, Any>): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/location/popular-cities")
    suspend fun getPopularCities(): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/location/insights")
    suspend fun getLocationInsights(): Response<ApiResponse<Map<String, Any>>>
    
    // Job analytics endpoints
    @GET("/api/analytics/employer")
    suspend fun getEmployerAnalytics(): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/analytics/job/{jobId}")
    suspend fun getJobPerformance(
        @Path("jobId") jobId: String
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/analytics/dashboard")
    suspend fun getEmployerDashboard(): Response<ApiResponse<Map<String, Any>>>
    
    // Advanced search endpoints
    @POST("/api/jobs/advanced-search")
    suspend fun advancedSearch(
        @Body searchCriteria: Map<String, Any>,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/jobs/search-suggestions")
    suspend fun getSearchSuggestions(@Query("query") query: String): Response<ApiResponse<Map<String, Any>>>
    
    @GET("/api/jobs/search-filters")
    suspend fun getSearchFilters(): Response<ApiResponse<Map<String, Any>>>
}
