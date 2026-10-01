package com.example.dutype.services

import com.example.dutype.models.MatchedWorker
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nearest suitable workers for one of the employer's jobs or urgent requests, ranked by the
 * `matchWorkersForJob` Cloud Function from the public worker cards (distance, skill,
 * availability, activity, rating). Phones are never included; reveal one with getWorkerContact.
 */
@Singleton
class WorkerMatchingService @Inject constructor(
    private val functions: FirebaseFunctions
) {
    fun getMatchedWorkersForJob(jobId: String): Flow<Result<List<MatchedWorker>>> = flow {
        if (jobId.isBlank()) return@flow emit(Result.success(emptyList()))
        emit(runCatching {
            @Suppress("UNCHECKED_CAST")
            val data = functions.getHttpsCallable("matchWorkersForJob")
                .call(mapOf("jobId" to jobId)).await().data as? Map<String, Any?> ?: emptyMap()
            (data["workers"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.toMatchedWorker() }
        })
    }

    private fun Map<*, *>.toMatchedWorker(): MatchedWorker = MatchedWorker(
        workerId = this["workerId"] as? String ?: "",
        fullName = (this["fullName"] as? String).orEmpty().ifBlank { "Worker" },
        phone = this["phone"] as? String ?: "",
        profileImageUrl = this["profileImageUrl"] as? String ?: "",
        skills = (this["skills"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        experience = this["experience"] as? String ?: "",
        rating = (this["rating"] as? Number)?.toDouble() ?: 0.0,
        ratingCount = (this["ratingCount"] as? Number)?.toInt() ?: 0,
        completedJobs = (this["completedJobs"] as? Number)?.toInt() ?: 0,
        isAvailable = this["isAvailable"] as? Boolean ?: false,
        distanceKm = (this["distanceKm"] as? Number)?.toDouble(),
        matchScore = (this["matchScore"] as? Number)?.toInt() ?: 0,
        matchReasons = (this["matchReasons"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    )
}
