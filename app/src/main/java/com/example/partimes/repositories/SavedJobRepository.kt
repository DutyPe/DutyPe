package com.example.partimes.repositories

import com.example.partimes.auth.AuthManager
import com.example.partimes.models.JobListing
import com.example.partimes.network.ApiClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedJobRepository @Inject constructor(
    private val authManager: AuthManager
) {
    
    suspend fun saveJob(jobId: String, notes: String? = null): Result<Map<String, Any>> {
        return try {
            val token = authManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = ApiClient.getApiService().saveJob(jobId, notes)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to save job"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unsaveJob(jobId: String): Result<Map<String, Any>> {
        return try {
            val token = authManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = ApiClient.getApiService().unsaveJob(jobId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to unsave job"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSavedJobs(): Result<List<JobListing>> {
        return try {
            val token = authManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = ApiClient.getApiService().getSavedJobs()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val jobsData = response.body()?.data?.get("savedJobs") as? List<Map<String, Any>>
                val jobs = jobsData?.map { mapToJobListing(it) } ?: emptyList()
                Result.success(jobs)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get saved jobs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isJobSaved(jobId: String): Result<Boolean> {
        return try {
            val token = authManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = ApiClient.getApiService().isJobSaved(jobId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val isSaved = response.body()?.data?.get("isSaved") as? Boolean ?: false
                Result.success(isSaved)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to check saved status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateSavedJobNotes(jobId: String, notes: String): Result<Map<String, Any>> {
        return try {
            val token = authManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = ApiClient.getApiService().updateSavedJobNotes(jobId, notes)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to update notes"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSavedJobCount(): Result<Int> {
        return try {
            val token = authManager.getToken() ?: return Result.failure(Exception("Not authenticated"))
            val response = ApiClient.getApiService().getSavedJobCount()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val count = response.body()?.data?.get("count") as? Int ?: 0
                Result.success(count)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get saved job count"))
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
