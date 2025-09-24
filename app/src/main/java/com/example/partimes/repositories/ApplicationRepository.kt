package com.example.partimes.repositories

import com.example.partimes.apis.ApiService
import com.example.partimes.auth.AuthManager
import com.example.partimes.models.JobApplication
import com.example.partimes.models.ApiResponse
import com.google.gson.Gson

class ApplicationRepository(
    private val apiService: ApiService,
    private val authManager: AuthManager
) {
    
    private val gson = Gson()
    
    suspend fun submitApplication(application: JobApplication): Result<JobApplication> {
        return try {
            val response = apiService.submitApplication(application)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    val data = responseBody.data
                    if (data != null) {
                        val applicationData = data["application"] as? Map<String, Any>
                        if (applicationData != null) {
                            val submittedApplication = gson.fromJson(
                                gson.toJson(applicationData),
                                JobApplication::class.java
                            )
                            Result.success(submittedApplication)
                        } else {
                            Result.failure(Exception("No application data received"))
                        }
                    } else {
                        Result.failure(Exception("No data received"))
                    }
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to submit application"))
                }
            } else {
                Result.failure(Exception("Failed to submit application: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMyApplications(): Result<Map<String, Any>> {
        return try {
            val response = apiService.getMyApplications()
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(responseBody.data ?: emptyMap())
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to fetch applications"))
                }
            } else {
                Result.failure(Exception("Failed to fetch applications: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApplication(applicationId: String): Result<JobApplication> {
        return try {
            val response = apiService.getApplication(applicationId)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    val data = responseBody.data
                    if (data != null) {
                        val applicationData = data["application"] as? Map<String, Any>
                        if (applicationData != null) {
                            val application = gson.fromJson(
                                gson.toJson(applicationData),
                                JobApplication::class.java
                            )
                            Result.success(application)
                        } else {
                            Result.failure(Exception("No application data received"))
                        }
                    } else {
                        Result.failure(Exception("No data received"))
                    }
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to fetch application"))
                }
            } else {
                Result.failure(Exception("Failed to fetch application: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateApplicationStatus(
        applicationId: String,
        status: String,
        notes: String? = null,
        rejectionReason: String? = null
    ): Result<JobApplication> {
        return try {
            val statusUpdate = mutableMapOf<String, String>()
            statusUpdate["status"] = status
            notes?.let { statusUpdate["notes"] = it }
            rejectionReason?.let { statusUpdate["rejectionReason"] = it }
            
            val response = apiService.updateApplicationStatus(applicationId, statusUpdate)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    val data = responseBody.data
                    if (data != null) {
                        val applicationData = data["application"] as? Map<String, Any>
                        if (applicationData != null) {
                            val updatedApplication = gson.fromJson(
                                gson.toJson(applicationData),
                                JobApplication::class.java
                            )
                            Result.success(updatedApplication)
                        } else {
                            Result.failure(Exception("No application data received"))
                        }
                    } else {
                        Result.failure(Exception("No data received"))
                    }
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to update application status"))
                }
            } else {
                Result.failure(Exception("Failed to update application status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApplicationsForJob(jobId: String): Result<Map<String, Any>> {
        return try {
            val response = apiService.getApplicationsForJob(jobId)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(responseBody.data ?: emptyMap())
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to fetch job applications"))
                }
            } else {
                Result.failure(Exception("Failed to fetch job applications: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun withdrawApplication(applicationId: String): Result<JobApplication> {
        return try {
            val response = apiService.withdrawApplication(applicationId)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    val data = responseBody.data
                    if (data != null) {
                        val applicationData = data["application"] as? Map<String, Any>
                        if (applicationData != null) {
                            val withdrawnApplication = gson.fromJson(
                                gson.toJson(applicationData),
                                JobApplication::class.java
                            )
                            Result.success(withdrawnApplication)
                        } else {
                            Result.failure(Exception("No application data received"))
                        }
                    } else {
                        Result.failure(Exception("No data received"))
                    }
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to withdraw application"))
                }
            } else {
                Result.failure(Exception("Failed to withdraw application: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getApplicationsByStatus(status: String): Result<Map<String, Any>> {
        return try {
            val response = apiService.getApplicationsByStatus(status)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(responseBody.data ?: emptyMap())
                } else {
                    Result.failure(Exception(responseBody?.message ?: "Failed to fetch applications by status"))
                }
            } else {
                Result.failure(Exception("Failed to fetch applications by status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
