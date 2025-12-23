package com.example.dutype.repositories

import com.example.dutype.models.JobListing
import com.example.dutype.services.FirestoreService
import com.example.dutype.location.calculateDistance
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FirestoreJobRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val auth: FirebaseAuth
) {
    
    /**
     * Create a new job posting
     */
    fun createJob(jobData: Map<String, Any>): Flow<Result<String>> = flow {
        try {
            val result = firestoreService.createJob(jobData)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get all active jobs (for workers) with saved status and pagination
     * Uses Firestore with built-in offline caching
     */
    fun getAllJobs(limit: Long = 50L, lastCreatedAt: Long? = null): Flow<Result<List<JobListing>>> = flow {
        // Fetch from Firestore (has built-in offline caching)
        try {
            val result = firestoreService.getAllJobs(limit, lastCreatedAt)
            result.fold(
                onSuccess = { jobsData ->
                    val jobListings = jobsData.map { convertMapToJobListing(it) }
                    
                    // Get current user's saved jobs to mark them as saved
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        Timber.d("🔍 DEBUG: Getting saved jobs for user: ${currentUser.uid}")
                        val savedJobsResult = firestoreService.getSavedJobs(currentUser.uid)
                        savedJobsResult.fold(
                            onSuccess = { savedJobsData ->
                                val savedJobIds = savedJobsData.mapNotNull { it["jobId"] as? String }.toSet()
                                Timber.d("🔍 DEBUG: Found ${savedJobIds.size} saved jobs: $savedJobIds")
                                val updatedJobListings = jobListings.map { job ->
                                    val isJobSaved = savedJobIds.contains(job.id)
                                    job.copy(isSaved = isJobSaved)
                                }
                                emit(Result.success(updatedJobListings))
                            },
                            onFailure = { exception ->
                                Timber.e("❌ DEBUG: Failed to get saved jobs: ${exception.message}")
                                // If we can't get saved jobs, emit jobs without saved status
                                emit(Result.success(jobListings))
                            }
                        )
                    } else {
                        Timber.w("❌ DEBUG: No current user found")
                        // No user logged in, emit jobs without saved status
                        emit(Result.success(jobListings))
                    }
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get jobs posted by a specific employer with REAL-TIME updates
     * Application count changes are reflected immediately
     */
    fun getJobsByEmployer(employerId: String): Flow<Result<List<JobListing>>> = 
        firestoreService.getJobsByEmployerRealtime(employerId).map { result ->
            result.map { jobsData ->
                jobsData.map { convertMapToJobListing(it) }
            }
        }.flowOn(Dispatchers.IO)
    
    /**
     * Get a specific job by ID with saved status
     */
    fun getJobById(jobId: String): Flow<Result<JobListing?>> = flow {
        try {
            val result = firestoreService.getJobById(jobId)
            result.fold(
                onSuccess = { jobData ->
                    val jobListing = jobData?.let { convertMapToJobListing(it) }
                    
                    // Check if this job is saved by the current user
                    val currentUser = auth.currentUser
                    if (currentUser != null && jobListing != null) {
                        val isSavedResult = firestoreService.isJobSaved(currentUser.uid, jobId)
                        isSavedResult.fold(
                            onSuccess = { isSaved ->
                                val updatedJob = jobListing.copy(isSaved = isSaved)
                                emit(Result.success(updatedJob))
                            },
                            onFailure = { exception ->
                                // If we can't check saved status, emit job without saved status
                                emit(Result.success(jobListing))
                            }
                        )
                    } else {
                        emit(Result.success(jobListing))
                    }
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update a job posting
     */
    fun updateJob(jobId: String, updates: Map<String, Any>): Flow<Result<Unit>> = flow {
        try {
            val result = firestoreService.updateJob(jobId, updates)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Delete a job posting
     */
    fun deleteJob(jobId: String): Flow<Result<Unit>> = flow {
        try {
            val result = firestoreService.deleteJob(jobId)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search jobs by query
     */
    fun searchJobs(query: String, limit: Long = 20L): Flow<Result<List<JobListing>>> = flow {
        try {
            val result = firestoreService.searchJobs(query, limit)
            result.fold(
                onSuccess = { jobsData ->
                    val jobListings = jobsData.map { convertMapToJobListing(it) }
                    emit(Result.success(jobListings))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get jobs by category
     */
    fun getJobsByCategory(category: String, limit: Long = 20L): Flow<Result<List<JobListing>>> = flow {
        try {
            val result = firestoreService.getJobsByCategory(category, limit)
            result.fold(
                onSuccess = { jobsData ->
                    val jobListings = jobsData.map { convertMapToJobListing(it) }
                    emit(Result.success(jobListings))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get jobs by location
     */
    fun getJobsByLocation(location: String, limit: Long = 20L): Flow<Result<List<JobListing>>> = flow {
        try {
            val result = firestoreService.getJobsByLocation(location, limit)
            result.fold(
                onSuccess = { jobsData ->
                    val jobListings = jobsData.map { convertMapToJobListing(it) }
                    emit(Result.success(jobListings))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Increment job view count
     */
    fun incrementJobViewCount(jobId: String): Flow<Result<Unit>> = flow {
        try {
            val result = firestoreService.incrementJobViewCount(jobId)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Convert Map<String, Any> to JobListing
     */
    private fun convertMapToJobListing(jobData: Map<String, Any>): JobListing {
        return JobListing(
            id = jobData["jobId"] as? String ?: "",
            jobId = jobData["jobId"] as? String ?: "",
            employerId = jobData["employerId"] as? String ?: "",
            title = jobData["title"] as? String ?: "",
            companyName = jobData["companyName"] as? String ?: "",
            company = jobData["company"] as? String ?: "",
            location = jobData["location"] as? String ?: "",
            specificLocation = jobData["specificLocation"] as? String ?: "",
            locationNearby = jobData["locationNearby"] as? String ?: "",
            area = jobData["area"] as? String,
            city = jobData["city"] as? String,
            latitude = (jobData["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (jobData["longitude"] as? Number)?.toDouble() ?: 0.0,
            payRate = (jobData["payRate"] as? Number)?.toDouble() ?: 0.0,
            payAmount = jobData["payAmount"] as? String ?: "",
            payType = jobData["payType"] as? String ?: "",
            payPeriod = jobData["payPeriod"] as? String ?: "",
            timing = jobData["timing"] as? String ?: "",
            shiftTiming = jobData["shiftTiming"] as? String ?: "",
            description = jobData["description"] as? String ?: "",
            preferences = (jobData["preferences"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            benefits = (jobData["benefits"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            requirements = (jobData["requirements"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            skills = (jobData["skills"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            vacancies = (jobData["vacancies"] as? Number)?.toInt() ?: 0,
            isActive = jobData["isActive"] as? Boolean ?: true,
            isTrending = jobData["isTrending"] as? Boolean ?: false,
            isRemote = jobData["isRemote"] as? Boolean ?: false,
            isVerified = jobData["isVerified"] as? Boolean ?: false,
            isSaved = false, // Will be updated separately by checking saved jobs
            postedAt = (jobData["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            postedTime = (jobData["createdAt"] as? Number)?.toLong()?.toString() ?: "",
            postedDate = (jobData["createdAt"] as? Number)?.toLong()?.toString() ?: "",
            imageUrl = jobData["imageUrl"] as? String ?: "",
            phoneNumber = jobData["phoneNumber"] as? String ?: "",
            contactNumber = jobData["contactNumber"] as? String ?: "",
            contactInfo = jobData["contactInfo"] as? String ?: "",
            category = jobData["category"] as? String ?: "",
            jobType = jobData["jobType"] as? String ?: "",
            experienceLevel = jobData["experienceLevel"] as? String ?: "",
            experienceRequired = jobData["experienceRequired"] as? String ?: "",
            workingHours = jobData["workingHours"] as? String ?: "",
            applicationDeadline = jobData["applicationDeadline"] as? String ?: "",
            ageRange = jobData["ageRange"] as? String ?: "",
            gender = jobData["gender"] as? String ?: "",
            companySize = jobData["companySize"] as? String ?: "",
            industry = jobData["industry"] as? String ?: "",
            urgency = jobData["urgency"] as? String ?: "",
            viewCount = (jobData["viewCount"] as? Number)?.toLong() ?: 0L,
            applicationCount = (jobData["applicationCount"] as? Number)?.toLong() ?: 0L
        )
    }
    
    /**
     * Calculate distance for a job based on user's location
     */
    fun calculateJobDistance(job: JobListing, userLat: Double, userLon: Double): JobListing {
        if (job.latitude == 0.0 && job.longitude == 0.0) {
            // Job doesn't have coordinates, return as is
            return job
        }
        if (userLat == 0.0 && userLon == 0.0) {
            // User doesn't have coordinates, return as is
            return job
        }
        val distance = calculateDistance(userLat, userLon, job.latitude, job.longitude)
        return job.copy(distance = distance)
    }
    
    /**
     * Calculate distances for a list of jobs based on user's location
     */
    fun calculateJobsDistances(jobs: List<JobListing>, userLat: Double, userLon: Double): List<JobListing> {
        return jobs.map { job -> calculateJobDistance(job, userLat, userLon) }
    }
}
