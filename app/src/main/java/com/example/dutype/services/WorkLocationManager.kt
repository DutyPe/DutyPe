package com.example.dutype.services

import com.example.dutype.models.WorkLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber
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
    }

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
