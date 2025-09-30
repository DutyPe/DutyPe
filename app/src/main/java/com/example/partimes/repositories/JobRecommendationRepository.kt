package com.example.partimes.repositories

import com.example.partimes.auth.AuthManager
import com.example.partimes.models.JobListing
import com.example.partimes.network.ApiClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRecommendationRepository @Inject constructor(
    private val authManager: AuthManager
) {
    
    suspend fun getPersonalizedRecommendations(limit: Int = 10): Result<List<JobListing>> {
        return try {
            val token = authManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = ApiClient.getApiService().getPersonalizedRecommendations(limit)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val recommendationsData = response.body()?.data?.get("recommendations") as? List<Map<String, Any>>
                val recommendations = recommendationsData?.map { mapToJobListing(it) } ?: emptyList()
                Result.success(recommendations)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get recommendations"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSimilarJobs(jobId: String, limit: Int = 5): Result<List<JobListing>> {
        return try {
            val response = ApiClient.getApiService().getSimilarJobs(jobId, limit)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val similarJobsData = response.body()?.data?.get("similarJobs") as? List<Map<String, Any>>
                val similarJobs = similarJobsData?.map { mapToJobListing(it) } ?: emptyList()
                Result.success(similarJobs)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get similar jobs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPopularJobs(limit: Int = 10): Result<List<JobListing>> {
        return try {
            val response = ApiClient.getApiService().getPopularJobs(limit)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val popularJobsData = response.body()?.data?.get("popularJobs") as? List<Map<String, Any>>
                val popularJobs = popularJobsData?.map { mapToJobListing(it) } ?: emptyList()
                Result.success(popularJobs)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get popular jobs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTrendingJobs(limit: Int = 10): Result<List<JobListing>> {
        return try {
            val response = ApiClient.getApiService().getTrendingJobs(limit)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val trendingJobsData = response.body()?.data?.get("trendingJobs") as? List<Map<String, Any>>
                val trendingJobs = trendingJobsData?.map { mapToJobListing(it) } ?: emptyList()
                Result.success(trendingJobs)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get trending jobs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLocationBasedRecommendations(location: String, limit: Int = 10): Result<List<JobListing>> {
        return try {
            val token = authManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = ApiClient.getApiService().getLocationBasedRecommendations(location, limit)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val recommendationsData = response.body()?.data?.get("recommendations") as? List<Map<String, Any>>
                val recommendations = recommendationsData?.map { mapToJobListing(it) } ?: emptyList()
                Result.success(recommendations)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get location-based recommendations"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun mapToJobListing(map: Map<String, Any>): JobListing {
        return JobListing(
            id = map["jobId"] as? String ?: "",
            title = map["title"] as? String ?: "",
            company = map["company"] as? String ?: "",
            location = map["location"] as? String ?: "",
            salary = map["salary"] as? String ?: "",
            description = map["description"] as? String ?: "",
            requirements = (map["requirements"] as? List<String>) ?: emptyList(),
            benefits = (map["benefits"] as? List<String>) ?: emptyList(),
            postedDate = map["postedDate"] as? String ?: "",
            jobType = map["jobType"] as? String ?: "",
            experienceLevel = map["experienceLevel"] as? String ?: "",
            isRemote = map["isRemote"] as? Boolean ?: false,
            urgency = map["urgency"] as? String ?: "NORMAL",
            category = map["category"] as? String ?: "",
            skills = (map["skills"] as? List<String>) ?: emptyList(),
            contactInfo = map["contactInfo"] as? String ?: "",
            workingHours = map["workingHours"] as? String ?: "",
            isVerified = map["isVerified"] as? Boolean ?: false,
            viewCount = (map["viewCount"] as? Number)?.toLong() ?: 0L,
            applicationCount = (map["applicationCount"] as? Number)?.toLong() ?: 0L,
            // isBookmarked and isApplied are worker-specific and handled separately
        )
    }
}
