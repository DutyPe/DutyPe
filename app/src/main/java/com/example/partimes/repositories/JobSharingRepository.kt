package com.example.partimes.repositories

import com.example.partimes.network.ApiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobSharingRepository @Inject constructor() {
    
    suspend fun getJobShareData(jobId: String): Result<Map<String, Any>> {
        return try {
            val response = ApiClient.getApiService().getJobShareData(jobId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val shareData = response.body()?.data?.get("shareData") as? Map<String, Any>
                Result.success(shareData ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get share data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getJobCollectionShareData(jobIds: List<String>, collectionName: String): Result<Map<String, Any>> {
        return try {
            val request = mapOf(
                "jobIds" to jobIds,
                "collectionName" to collectionName
            )
            val response = ApiClient.getApiService().getJobCollectionShareData(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val shareData = response.body()?.data?.get("shareData") as? Map<String, Any>
                Result.success(shareData ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get collection share data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getWhatsAppShareData(jobId: String): Result<Map<String, Any>> {
        return try {
            val response = ApiClient.getApiService().getWhatsAppShareData(jobId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get WhatsApp share data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFacebookShareData(jobId: String): Result<Map<String, Any>> {
        return try {
            val response = ApiClient.getApiService().getFacebookShareData(jobId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get Facebook share data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTwitterShareData(jobId: String): Result<Map<String, Any>> {
        return try {
            val response = ApiClient.getApiService().getTwitterShareData(jobId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get Twitter share data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLinkedInShareData(jobId: String): Result<Map<String, Any>> {
        return try {
            val response = ApiClient.getApiService().getLinkedInShareData(jobId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get LinkedIn share data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getEmailShareData(jobId: String): Result<Map<String, Any>> {
        return try {
            val response = ApiClient.getApiService().getEmailShareData(jobId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyMap())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get email share data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
