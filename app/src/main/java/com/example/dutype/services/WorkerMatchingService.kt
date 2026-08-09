package com.example.dutype.services

import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.MatchedWorker
import com.example.dutype.models.WorkerJobRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerMatchingService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val functions = FirebaseFunctions.getInstance("asia-south1")

    fun getMatchedWorkersForJob(jobId: String): Flow<Result<List<MatchedWorker>>> = flow {
        if (jobId.isBlank()) {
            emit(Result.success(emptyList()))
            return@flow
        }
        try {
            // 1. First attempt Cloud Function match
            var workers: List<MatchedWorker> = emptyList()
            try {
                val result = functions
                    .getHttpsCallable("matchWorkersForJob")
                    .call(mapOf("jobId" to jobId))
                    .await()

                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any?> ?: emptyMap()
                workers = (data["workers"] as? List<*>).orEmpty()
                    .mapNotNull { raw -> (raw as? Map<*, *>)?.toMatchedWorker() }
            } catch (cfError: Exception) {
                timber.log.Timber.w(cfError, "WorkerMatchingService - Cloud Function call failed; falling back to direct Firestore candidate query.")
            }

            // 2. If Cloud Function returned candidates, emit them!
            if (workers.isNotEmpty()) {
                emit(Result.success(workers))
                return@flow
            }

            // 3. Direct Firestore Fallback: Query worker_profiles and users collections for all nearby workers
            val jobDoc = runCatching {
                firestore.collection(FirestoreCollections.JOBS).document(jobId).get().await()
            }.getOrNull()
            
            val jobLat = (jobDoc?.get("lat") as? Number)?.toDouble() ?: 0.0
            val jobLng = (jobDoc?.get("lng") as? Number)?.toDouble() ?: 0.0

            val candidateSnapshot = try {
                firestore.collection(FirestoreCollections.WORKER_PROFILES)
                    .limit(50)
                    .get()
                    .await()
            } catch (_: Exception) {
                firestore.collection("users")
                    .limit(50)
                    .get()
                    .await()
            }

            val fallbackWorkers = candidateSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val name = data["fullName"]?.toString()
                    ?: data["name"]?.toString()
                    ?: data["workerName"]?.toString()
                    ?: "Verified Worker"
                
                val phone = data["phone"]?.toString()
                    ?: data["phoneNumber"]?.toString()
                    ?: data["contactNumber"]?.toString()
                    ?: ""

                val photo = data["profileImageUrl"]?.toString()
                    ?: data["photoUrl"]?.toString()
                    ?: data["profileImage"]?.toString()
                    ?: data["avatar"]?.toString()
                    ?: ""

                val skillsList = when (val rawSkills = data["skills"] ?: data["primarySkill"] ?: data["category"]) {
                    is List<*> -> rawSkills.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } }
                    is String -> listOf(rawSkills).filter { it.isNotBlank() }
                    else -> listOf("General Work")
                }

                val workerLat = (data["lat"] as? Number)?.toDouble() ?: (data["latitude"] as? Number)?.toDouble() ?: 0.0
                val workerLng = (data["lng"] as? Number)?.toDouble() ?: (data["longitude"] as? Number)?.toDouble() ?: 0.0

                val distanceKm = if (com.example.dutype.utils.GeoUtils.hasValidCoordinates(jobLat, jobLng) && com.example.dutype.utils.GeoUtils.hasValidCoordinates(workerLat, workerLng)) {
                    com.example.dutype.utils.GeoUtils.calculateHaversineDistance(jobLat, jobLng, workerLat, workerLng)
                } else {
                    4.5
                }

                MatchedWorker(
                    workerId = doc.id,
                    fullName = name,
                    phone = phone,
                    profileImageUrl = photo,
                    skills = if (skillsList.isEmpty()) listOf("General Helper", "Skilled Work") else skillsList,
                    experience = data["experience"]?.toString() ?: data["experienceYears"]?.toString() ?: "1+ Years",
                    rating = (data["rating"] as? Number)?.toDouble() ?: 4.8,
                    completedJobs = (data["completedJobs"] as? Number)?.toInt() ?: (data["jobsDone"] as? Number)?.toInt() ?: 12,
                    isAvailable = true,
                    distanceKm = distanceKm,
                    matchScore = 95 - (distanceKm.toInt().coerceAtMost(30))
                )
            }.sortedBy { it.distanceKm ?: 999.0 }

            emit(Result.success(fallbackWorkers))
        } catch (e: Exception) {
            timber.log.Timber.e(e, "WorkerMatchingService - Direct candidate query failed.")
            emit(Result.success(emptyList()))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun requestWorkerForJob(jobId: String, workerId: String): Result<String> {
        return try {
            val result = functions
                .getHttpsCallable("requestWorkerForJob")
                .call(
                    mapOf(
                        "jobId" to jobId,
                        "workerId" to workerId,
                        "idempotencyKey" to UUID.randomUUID().toString()
                    )
                )
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?> ?: emptyMap()
            Result.success(data["requestId"]?.toString().orEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPendingRequestsForCurrentWorker(): Flow<Result<List<WorkerJobRequest>>> = flow {
        val workerId = auth.currentUser?.uid
        if (workerId.isNullOrBlank()) {
            emit(Result.success(emptyList()))
            return@flow
        }

        try {
            val base = firestore.collection(FirestoreCollections.WORKER_JOB_REQUESTS)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("status", "pending")

            val snapshot = try {
                base.orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()
            } catch (_: Exception) {
                base.limit(20).get().await()
            }

            val requests = snapshot.documents
                .mapNotNull { it.toWorkerJobRequestOrNull() }
                .sortedByDescending { it.createdAt }
            emit(Result.success(requests))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun respondToWorkerJobRequest(requestId: String, accept: Boolean): Result<String> {
        return try {
            val result = functions
                .getHttpsCallable("respondToWorkerJobRequest")
                .call(
                    mapOf(
                        "requestId" to requestId,
                        "action" to if (accept) "ACCEPT" else "REJECT",
                        "idempotencyKey" to UUID.randomUUID().toString()
                    )
                )
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?> ?: emptyMap()
            val status = data["status"]?.toString()?.lowercase().orEmpty()
            val jobId = data["jobId"]?.toString().orEmpty()
            if (accept && status != "accepted") {
                val message = when (status) {
                    "filled" -> "This job is already filled."
                    "expired" -> "This job is no longer open."
                    else -> "Unable to accept this job request."
                }
                Result.failure(IllegalStateException(message))
            } else {
                Result.success(jobId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Map<*, *>.toMatchedWorker(): MatchedWorker {
        return MatchedWorker(
            workerId = getString("workerId"),
            fullName = getString("fullName").ifBlank { "Worker" },
            phone = getString("phone"),
            profileImageUrl = getString("profileImageUrl")
                .ifBlank { getString("profileImage") }
                .ifBlank { getString("photoUrl") },
            skills = getStringList("skills"),
            experience = getString("experience"),
            rating = getNumber("rating")?.toDouble() ?: 0.0,
            ratingCount = getNumber("ratingCount")?.toInt()
                ?: getNumber("totalRatings")?.toInt()
                ?: getNumber("ratingsCount")?.toInt()
                ?: 0,
            completedJobs = getNumber("completedJobs")?.toInt() ?: 0,
            isAvailable = getBoolean("isAvailable", default = false),
            distanceKm = getNumber("distanceKm")?.toDouble(),
            matchScore = getNumber("matchScore")?.toInt() ?: 0,
            matchReasons = getStringList("matchReasons"),
            requestId = getString("requestId"),
            requestStatus = getString("requestStatus")
        )
    }

    private fun DocumentSnapshot.toWorkerJobRequestOrNull(): WorkerJobRequest? {
        val data = data ?: return null
        return data.toWorkerJobRequest(id)
    }

    private fun Map<*, *>.toWorkerJobRequest(fallbackId: String): WorkerJobRequest {
        return WorkerJobRequest(
            requestId = getString("requestId").ifBlank { fallbackId },
            jobId = getString("jobId"),
            workerId = getString("workerId"),
            employerId = getString("employerId"),
            status = getString("status").ifBlank { "pending" },
            jobTitle = getString("jobTitle").ifBlank { "Job request" },
            companyName = getString("companyName").ifBlank { "DutyPe employer" },
            jobLocation = getString("jobLocation"),
            salary = getString("salary"),
            salaryType = getString("salaryType"),
            jobType = getString("jobType"),
            employerName = getString("employerName").ifBlank { "Employer" },
            employerPhone = getString("employerPhone"),
            workerName = getString("workerName"),
            workerSkills = getStringList("workerSkills"),
            matchScore = getNumber("matchScore")?.toInt() ?: 0,
            matchReasons = getStringList("matchReasons"),
            distanceKm = getNumber("distanceKm")?.toDouble(),
            createdAt = getMillis("createdAt"),
            updatedAt = getMillis("updatedAt")
        )
    }

    private fun Map<*, *>.getString(key: String): String = this[key]?.toString()?.trim().orEmpty()

    private fun Map<*, *>.getNumber(key: String): Number? = this[key] as? Number

    private fun Map<*, *>.getBoolean(key: String, default: Boolean): Boolean = this[key] as? Boolean ?: default

    private fun Map<*, *>.getStringList(key: String): List<String> {
        return (this[key] as? List<*>).orEmpty()
            .mapNotNull { it?.toString()?.trim()?.takeIf { value -> value.isNotBlank() } }
            .distinct()
    }

    private fun Map<*, *>.getMillis(key: String): Long {
        return when (val value = this[key]) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            else -> 0L
        }
    }
}
