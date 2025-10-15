package com.example.dutype.repositories

import com.example.dutype.models.JobListing
import com.example.dutype.services.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSavedJobRepository @Inject constructor(
    private val firestoreService: FirestoreService
) {
    
    private val currentUser = FirebaseAuth.getInstance().currentUser
    
    fun getSavedJobs(): Flow<Result<List<JobListing>>> = flow {
        val workerId = currentUser?.uid
        if (workerId == null) {
            emit(Result.failure(Exception("User not authenticated")))
            return@flow
        }
        
        val result = firestoreService.getSavedJobs(workerId)
        result.fold(
            onSuccess = { jobMaps ->
                val jobListings = jobMaps.mapNotNull { jobMap ->
                    convertMapToJobListing(jobMap)
                }
                emit(Result.success(jobListings))
            },
            onFailure = { exception ->
                emit(Result.failure(exception))
            }
        )
    }
    
    suspend fun saveJob(jobId: String): Result<Unit> {
        val workerId = currentUser?.uid
        if (workerId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        return firestoreService.saveJob(workerId, jobId)
    }
    
    suspend fun unsaveJob(jobId: String): Result<Unit> {
        val workerId = currentUser?.uid
        if (workerId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        return firestoreService.unsaveJob(workerId, jobId)
    }
    
    suspend fun isJobSaved(jobId: String): Result<Boolean> {
        val workerId = currentUser?.uid
        if (workerId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        return firestoreService.isJobSaved(workerId, jobId)
    }
    
    private fun convertMapToJobListing(jobMap: Map<String, Any>): JobListing? {
        return try {
            JobListing(
                id = jobMap["jobId"] as? String ?: "",
                jobId = jobMap["jobId"] as? String ?: "",
                title = jobMap["title"] as? String ?: "",
                description = jobMap["description"] as? String ?: "",
                company = jobMap["company"] as? String ?: "",
                companyName = jobMap["company"] as? String ?: "",
                location = jobMap["location"] as? String ?: "",
                payAmount = jobMap["payAmount"] as? String ?: "0",
                payRate = (jobMap["payAmount"] as? Number)?.toDouble() ?: 0.0,
                payType = jobMap["payType"] as? String ?: "hourly",
                category = jobMap["category"] as? String ?: "",
                jobType = jobMap["jobType"] as? String ?: "part-time",
                isActive = jobMap["isActive"] as? Boolean ?: true,
                employerId = jobMap["employerId"] as? String ?: "",
                postedAt = (jobMap["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                viewCount = (jobMap["viewCount"] as? Number)?.toLong() ?: 0L,
                applicationCount = (jobMap["applicationCount"] as? Number)?.toLong() ?: 0L,
                requirements = (jobMap["requirements"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                benefits = (jobMap["benefits"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                workingHours = jobMap["workingHours"] as? String ?: "",
                experienceRequired = jobMap["experienceRequired"] as? String ?: "",
                ageRange = jobMap["ageRange"] as? String ?: "",
                gender = jobMap["gender"] as? String ?: "Any",
                vacancies = (jobMap["vacancies"] as? Number)?.toInt() ?: 1,
                applicationDeadline = jobMap["applicationDeadline"] as? String ?: "",
                companySize = jobMap["companySize"] as? String ?: "",
                industry = jobMap["industry"] as? String ?: "",
                phoneNumber = jobMap["phoneNumber"] as? String ?: "",
                contactNumber = jobMap["phoneNumber"] as? String ?: ""
            )
        } catch (e: Exception) {
            println("❌ Error converting job map to JobListing: ${e.message}")
            null
        }
    }
}
