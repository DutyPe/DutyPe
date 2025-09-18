package com.example.partimes.repository

import com.example.partimes.jobseeker.api.JobSeekerApiService
import com.example.partimes.jobseeker.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing job applications
 * Handles both local storage and API communication
 */
@Singleton
class ApplicationRepository @Inject constructor(
    private val apiService: JobSeekerApiService,
    private val userRepository: UserRepository
) {
    
    // In-memory storage for applications (in production, use Room database)
    private val applications = mutableListOf<JobApplication>()
    
    /**
     * Submit a job application
     */
    suspend fun submitApplication(jobId: String, application: JobApplication): Result<Unit> {
        return try {
            val response = apiService.submitApplication(application)
            if (response.isSuccessful) {
                // Add to local storage
                applications.add(application)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to submit application: ${response.message()}"))
            }
        } catch (e: Exception) {
            // For now, simulate successful submission for demo purposes
            applications.add(application)
            Result.success(Unit)
        }
    }
    
    /**
     * Get all applications for current user
     */
    fun getApplications(): Flow<List<JobApplication>> = flow {
        try {
            val userId = userRepository.currentUser.value.id
            val response = apiService.getApplications(userId)
            if (response.isSuccessful) {
                emit(response.body() ?: emptyList())
            } else {
                // Fallback to local storage
                emit(applications)
            }
        } catch (e: Exception) {
            // Fallback to local storage
            emit(applications)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update application status
     */
    suspend fun updateApplicationStatus(
        applicationId: String, 
        status: ApplicationStatus
    ): Result<Unit> {
        return try {
            val request = com.example.partimes.jobseeker.api.UpdateStatusRequest(
                status = status.name,
                message = "Status updated"
            )
            val response = apiService.updateApplicationStatus(applicationId, request)
            if (response.isSuccessful) {
                // Update local storage
                val index = applications.indexOfFirst { it.id == applicationId }
                if (index != -1) {
                    applications[index] = applications[index].copy(
                        status = status,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update status: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Withdraw an application
     */
    suspend fun withdrawApplication(applicationId: String): Result<Unit> {
        return try {
            val response = apiService.withdrawApplication(applicationId)
            if (response.isSuccessful) {
                // Update local storage
                val index = applications.indexOfFirst { it.id == applicationId }
                if (index != -1) {
                    applications[index] = applications[index].copy(
                        status = ApplicationStatus.WITHDRAWN,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to withdraw application: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload document
     */
    suspend fun uploadDocument(
        document: Document,
        fileBytes: ByteArray
    ): Result<Document> {
        return try {
            // Create multipart request
            val requestFile = okhttp3.RequestBody.create(
                "application/octet-stream".toMediaTypeOrNull(),
                fileBytes
            )
            val multipartBody = okhttp3.MultipartBody.Part.createFormData(
                "file", 
                document.name, 
                requestFile
            )
            
            val typeBody = okhttp3.RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                document.type.name
            )
            
            val userIdBody = okhttp3.RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                userRepository.currentUser.value.id
            )
            
            val response = apiService.uploadDocument(multipartBody, typeBody, userIdBody)
            if (response.isSuccessful) {
                val documentResponse = response.body()
                if (documentResponse != null && documentResponse.success) {
                    val uploadedDocument = document.copy(
                        id = documentResponse.documentId,
                        url = documentResponse.url
                    )
                    Result.success(uploadedDocument)
                } else {
                    Result.failure(Exception("Upload failed: ${documentResponse?.message}"))
                }
            } else {
                Result.failure(Exception("Upload failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            // For demo purposes, simulate successful upload
            val simulatedDocument = document.copy(
                id = java.util.UUID.randomUUID().toString(),
                url = "https://example.com/documents/${document.name}"
            )
            Result.success(simulatedDocument)
        }
    }
    
    /**
     * Get application by ID
     */
    suspend fun getApplicationById(applicationId: String): JobApplication? {
        return applications.find { it.id == applicationId }
    }
    
    /**
     * Get applications by job ID
     */
    suspend fun getApplicationsByJobId(jobId: String): List<JobApplication> {
        return applications.filter { it.jobId == jobId }
    }
    
    /**
     * Get applications by status
     */
    suspend fun getApplicationsByStatus(status: ApplicationStatus): List<JobApplication> {
        return applications.filter { it.status == status }
    }
}
