package com.example.dutype.services

import com.example.dutype.models.WorkLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkLocationManager — manages saved work locations in a separate collection.
 *
 * Schema: work_locations/{userId}/locations/{locationId}
 *   { id, label, address, latitude, longitude, addedAt, usageCount }
 *
 * workLocations is NOT stored in the users doc (not in target schema).
 */
@Singleton
class WorkLocationManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val MAX_WORK_LOCATIONS = 10
        private const val COLLECTION = "work_locations"
        private const val CACHE_TTL_MS = 30_000L
    }

    private var cache: Pair<Long, List<WorkLocation>>? = null
    private var cacheUserId: String? = null

    private suspend fun getCached(userId: String): List<WorkLocation> {
        val c = cache
        if (c != null && cacheUserId == userId && (System.currentTimeMillis() - c.first) < CACHE_TTL_MS) {
            return c.second
        }
        val snapshot = firestore.collection(COLLECTION).document(userId)
            .collection("locations").get().await()
        val locations = snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data ?: return@mapNotNull null
                WorkLocation(
                    id = doc.id,
                    label = data["label"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    latitude = (data["lat"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (data["lng"] as? Number)?.toDouble() ?: 0.0,
                    addedAt = (data["addedAt"] as? Number)?.toLong() ?: 0L,
                    usageCount = (data["usageCount"] as? Number)?.toInt() ?: 0
                )
            } catch (e: Exception) { null }
        }
        cache = System.currentTimeMillis() to locations
        cacheUserId = userId
        return locations
    }

    private fun invalidate() { cache = null; cacheUserId = null }

    suspend fun saveWorkLocation(
        label: String,
        address: String,
        latitude: Double,
        longitude: Double
    ): Result<WorkLocation> {
        Timber.d("📍 WorkLocation strict mode: save skipped")
        return Result.failure(Exception("Work locations are disabled in strict schema mode"))
    }

    suspend fun getWorkLocations(): Result<List<WorkLocation>> {
        return Result.success(emptyList())
    }

    suspend fun removeWorkLocation(locationId: String): Result<Unit> {
        Timber.d("📍 WorkLocation strict mode: remove skipped")
        return Result.success(Unit)
    }

    suspend fun isLocationSaved(address: String): Boolean {
        return false
    }
}
