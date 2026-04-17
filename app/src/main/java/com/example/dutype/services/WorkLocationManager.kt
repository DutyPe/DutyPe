package com.example.dutype.services

import com.example.dutype.models.WorkLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

class WorkLocationManager(
    private val _firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserIdForWrite(): Result<String> {
        val userId = auth.currentUser?.uid
        return if (userId.isNullOrBlank()) {
            Result.failure(IllegalStateException("Please log in to manage saved locations"))
        } else {
            Result.success(userId)
        }
    }

    suspend fun saveWorkLocation(
        label: String,
        address: String,
        latitude: Double,
        longitude: Double
    ): Result<WorkLocation> {
        requireUserIdForWrite().getOrElse { return Result.failure(it) }

        val cleanAddress = address.trim()
        if (cleanAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Address cannot be empty"))
        }

        val now = System.currentTimeMillis()
        val cleanLabel = label.trim().ifBlank { cleanAddress.take(30) }

        Timber.i("WorkLocationManager: work_locations persistence removed; skipping save")
        return Result.success(
            WorkLocation(
                id = "",
                label = cleanLabel,
                address = cleanAddress,
                latitude = latitude,
                longitude = longitude,
                addedAt = now,
                usageCount = 1
            )
        )
    }

    suspend fun getWorkLocations(): Result<List<WorkLocation>> {
        Timber.i("WorkLocationManager: work_locations persistence removed; returning empty list")
        return Result.success(emptyList())
    }

    suspend fun removeWorkLocation(locationId: String): Result<Unit> {
        requireUserIdForWrite().getOrElse { return Result.failure(it) }
        if (locationId.isBlank()) {
            return Result.failure(IllegalArgumentException("Location ID cannot be empty"))
        }

        Timber.i("WorkLocationManager: work_locations persistence removed; skipping delete")
        return Result.success(Unit)
    }

    suspend fun isLocationSaved(address: String): Boolean {
        return false
    }
}
