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
        try {
            val result = functions
                .getHttpsCallable("matchWorkersForJob")
                .call(mapOf("jobId" to jobId))
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?> ?: emptyMap()
            val workers = (data["workers"] as? List<*>).orEmpty()
                .mapNotNull { raw -> (raw as? Map<*, *>)?.toMatchedWorker() }
            emit(Result.success(workers))
        } catch (e: Exception) {
            emit(Result.failure(e))
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
            Result.success(data["jobId"]?.toString().orEmpty())
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
