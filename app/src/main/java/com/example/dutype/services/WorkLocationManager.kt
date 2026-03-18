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
                    latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
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
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val existing = getCached(userId).find { it.address == address }

            if (existing != null) {
                val newCount = existing.usageCount + 1
                firestore.collection(COLLECTION).document(userId)
                    .collection("locations").document(existing.id)
                    .update("usageCount", newCount).await()
                invalidate()
                return Result.success(existing.copy(usageCount = newCount))
            }

            val all = getCached(userId)
            if (all.size >= MAX_WORK_LOCATIONS) {
                val leastUsed = all.minByOrNull { it.usageCount }
                leastUsed?.let { removeWorkLocation(it.id) }
            }

            val newLocation = WorkLocation(
                id = UUID.randomUUID().toString(),
                label = label,
                address = address,
                latitude = latitude,
                longitude = longitude,
                addedAt = System.currentTimeMillis(),
                usageCount = 1
            )
            firestore.collection(COLLECTION).document(userId)
                .collection("locations").document(newLocation.id)
                .set(mapOf(
                    "label" to newLocation.label,
                    "address" to newLocation.address,
                    "latitude" to newLocation.latitude,
                    "longitude" to newLocation.longitude,
                    "addedAt" to newLocation.addedAt,
                    "usageCount" to newLocation.usageCount
                )).await()
            invalidate()
            Timber.d("📍 WorkLocation: Saved $label")
            Result.success(newLocation)
        } catch (e: Exception) {
            Timber.e(e, "❌ WorkLocation: Failed to save")
            Result.failure(e)
        }
    }

    suspend fun getWorkLocations(): Result<List<WorkLocation>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val locations = getCached(userId).sortedByDescending { it.usageCount }
            Result.success(locations)
        } catch (e: Exception) {
            Timber.e(e, "❌ WorkLocation: Failed to get")
            Result.failure(e)
        }
    }

    suspend fun removeWorkLocation(locationId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection(COLLECTION).document(userId)
                .collection("locations").document(locationId).delete().await()
            invalidate()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ WorkLocation: Failed to remove")
            Result.failure(e)
        }
    }

    suspend fun isLocationSaved(address: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            getCached(userId).any { it.address == address }
        } catch (e: Exception) { false }
    }
}
