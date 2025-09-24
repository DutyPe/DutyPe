package com.example.partimes.repositories

import com.example.partimes.apis.ApiService
import com.example.partimes.auth.AuthManager
import com.example.partimes.models.JobListing
import com.example.partimes.models.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class JobRepository(
    private val apiService: ApiService,
    private val authManager: AuthManager
) {
    
    private val gson = Gson()
    
    suspend fun getAllJobs(page: Int = 0, size: Int = 20): Result<Map<String, Any>> {
        return try {
            val response = apiService.getAllJobs(page, size)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(responseBody.data ?: emptyMap())
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to fetch jobs"))
                }
            } else {
                Result.failure(Exception("Failed to fetch jobs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getJobById(jobId: String): Result<JobListing> {
        return try {
            val response = apiService.getJobById(jobId)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    val data = responseBody.data
                    if (data != null) {
                        val jobData = data["job"] as? Map<String, Any>
                        if (jobData != null) {
                            val job = gson.fromJson(
                                gson.toJson(jobData),
                                JobListing::class.java
                            )
                            Result.success(job)
                        } else {
                            Result.failure(Exception("No job data received"))
                        }
                    } else {
                        Result.failure(Exception("No data received"))
                    }
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to fetch job"))
                }
            } else {
                Result.failure(Exception("Failed to fetch job: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createJob(job: JobListing): Result<JobListing> {
        return try {
            val response = apiService.createJob(job)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    val data = responseBody.data
                    if (data != null) {
                        val jobData = data["job"] as? Map<String, Any>
                        if (jobData != null) {
                            val createdJob = gson.fromJson(
                                gson.toJson(jobData),
                                JobListing::class.java
                            )
                            Result.success(createdJob)
                        } else {
                            Result.failure(Exception("No job data received"))
                        }
                    } else {
                        Result.failure(Exception("No data received"))
                    }
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to create job"))
                }
            } else {
                Result.failure(Exception("Failed to create job: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateJob(jobId: String, job: JobListing): Result<JobListing> {
        return try {
            val response = apiService.updateJob(jobId, job)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    val data = responseBody.data
                    if (data != null) {
                        val jobData = data["job"] as? Map<String, Any>
                        if (jobData != null) {
                            val updatedJob = gson.fromJson(
                                gson.toJson(jobData),
                                JobListing::class.java
                            )
                            Result.success(updatedJob)
                        } else {
                            Result.failure(Exception("No job data received"))
                        }
                    } else {
                        Result.failure(Exception("No data received"))
                    }
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to update job"))
                }
            } else {
                Result.failure(Exception("Failed to update job: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteJob(jobId: String): Result<Unit> {
        return try {
            val response = apiService.deleteJob(jobId)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to delete job"))
                }
            } else {
                Result.failure(Exception("Failed to delete job: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchJobs(query: String, page: Int = 0, size: Int = 20): Result<Map<String, Any>> {
        return try {
            val response = apiService.searchJobs(query, page, size)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(responseBody.data ?: emptyMap())
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to search jobs"))
                }
            } else {
                Result.failure(Exception("Failed to search jobs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun filterJobs(
        city: String? = null,
        payType: String? = null,
        jobType: String? = null,
        category: String? = null,
        urgency: String? = null,
        page: Int = 0,
        size: Int = 20
    ): Result<Map<String, Any>> {
        return try {
            val response = apiService.filterJobs(city, payType, jobType, category, urgency, page, size)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(responseBody.data ?: emptyMap())
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to filter jobs"))
                }
            } else {
                Result.failure(Exception("Failed to filter jobs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getJobsByEmployer(employerId: String): Result<Map<String, Any>> {
        return try {
            val response = apiService.getJobsByEmployer(employerId)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(responseBody.data ?: emptyMap())
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to fetch employer jobs"))
                }
            } else {
                Result.failure(Exception("Failed to fetch employer jobs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVerifiedJobs(): Result<Map<String, Any>> {
        return try {
            val response = apiService.getVerifiedJobs()
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(responseBody.data ?: emptyMap())
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to fetch verified jobs"))
                }
            } else {
                Result.failure(Exception("Failed to fetch verified jobs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
