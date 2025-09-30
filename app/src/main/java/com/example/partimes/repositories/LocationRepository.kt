package com.example.partimes.repositories

import com.example.partimes.models.JobListing
import com.example.partimes.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor() {
    
    suspend fun getJobsNearLocation(city: String, radiusKm: Double = 25.0): Result<List<JobListing>> {
        return try {
            val response = ApiClient.getApiService().getJobsNearLocation(city, radiusKm)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val jobsData = response.body()?.data?.get("jobs") as? List<Map<String, Any>>
                val jobs = jobsData?.map { mapToJobListing(it) } ?: emptyList()
                Result.success(jobs)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get nearby jobs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getJobsByDistance(
        userCity: String,
        userLat: Double,
        userLon: Double,
        maxDistanceKm: Double = 50.0
    ): Result<Map<String, Any>> {
        return try {
            val response = ApiClient.getApiService().getJobsByDistance(userCity, userLat, userLon, maxDistanceKm)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val jobsData = response.body()?.data?.get("jobs") as? List<Map<String, Any>>
                val distances = response.body()?.data?.get("distances") as? Map<String, Double>
                val jobs = jobsData?.map { mapToJobListing(it) } ?: emptyList()
                
                val result = mapOf(
                    "jobs" to jobs,
                    "distances" to (distances ?: emptyMap())
                )
                Result.success(result)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get jobs by distance"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLocationBasedJobStats(city: String): Result<Map<String, Any>> {
        return try {
            val response = ApiClient.getApiService().getLocationBasedJobStats(city)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val stats = response.body()?.data?.get("stats") as? Map<String, Any>
                Result.success(stats ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get location stats"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getNearbyCities(city: String, radiusKm: Double = 100.0): Result<List<String>> {
        return try {
            val response = ApiClient.getApiService().getNearbyCities(city, radiusKm)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val nearbyCities = response.body()?.data?.get("nearbyCities") as? List<String>
                Result.success(nearbyCities ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get nearby cities"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getJobsInMultipleCities(cities: List<String>, radiusKm: Double = 25.0): Result<List<JobListing>> {
        return try {
            val request = mapOf(
                "cities" to cities,
                "radiusKm" to radiusKm
            )
            val response = ApiClient.getApiService().getJobsInMultipleCities(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val jobsData = response.body()?.data?.get("jobs") as? List<Map<String, Any>>
                val jobs = jobsData?.map { mapToJobListing(it) } ?: emptyList()
                Result.success(jobs)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get jobs in multiple cities"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPopularCities(): Result<List<String>> {
        return try {
            val response = ApiClient.getApiService().getPopularCities()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val popularCities = response.body()?.data?.get("popularCities") as? List<String>
                Result.success(popularCities ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get popular cities"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLocationInsights(): Result<Map<String, Any>> {
        return try {
            val response = ApiClient.getApiService().getLocationInsights()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val insights = response.body()?.data?.get("insights") as? Map<String, Any>
                Result.success(insights ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get location insights"))
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
