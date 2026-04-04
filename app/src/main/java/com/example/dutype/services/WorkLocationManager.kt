package com.example.dutype.services

import com.example.dutype.models.WorkLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale

class WorkLocationManager(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val COLLECTION_WORK_LOCATIONS = "work_locations"
        private const val SUBCOLLECTION_LOCATIONS = "locations"
        private const val FIELD_LABEL = "label"
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_NORMALIZED_ADDRESS = "normalizedAddress"
        private const val FIELD_LATITUDE = "latitude"
        private const val FIELD_LONGITUDE = "longitude"
        private const val FIELD_ADDED_AT = "addedAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_USAGE_COUNT = "usageCount"
        private const val MAX_WORK_LOCATIONS = 10
    }

    private fun userLocations(userId: String) = firestore
        .collection(COLLECTION_WORK_LOCATIONS)
        .document(userId)
        .collection(SUBCOLLECTION_LOCATIONS)

    private fun requireUserIdForWrite(): Result<String> {
        val userId = auth.currentUser?.uid
        return if (userId.isNullOrBlank()) {
            Result.failure(IllegalStateException("Please log in to manage saved locations"))
        } else {
            Result.success(userId)
        }
    }

    private fun normalizeAddress(address: String): String {
        return address
            .trim()
            .lowercase(Locale.ROOT)
            .replace("\\s+".toRegex(), " ")
    }

    private fun DocumentSnapshot.toWorkLocation(): WorkLocation {
        return WorkLocation(
            id = id,
            label = getString(FIELD_LABEL).orEmpty(),
            address = getString(FIELD_ADDRESS).orEmpty(),
            latitude = getDouble(FIELD_LATITUDE) ?: 0.0,
            longitude = getDouble(FIELD_LONGITUDE) ?: 0.0,
            addedAt = getLong(FIELD_ADDED_AT) ?: System.currentTimeMillis(),
            usageCount = getLong(FIELD_USAGE_COUNT)?.toInt() ?: 0
        )
    }

    suspend fun saveWorkLocation(
        label: String,
        address: String,
        latitude: Double,
        longitude: Double
    ): Result<WorkLocation> {
        val userId = requireUserIdForWrite().getOrElse { return Result.failure(it) }
        val cleanAddress = address.trim()
        if (cleanAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("Address cannot be empty"))
        }

        val cleanLabel = label.trim().ifBlank { cleanAddress.take(30) }
        val normalizedAddress = normalizeAddress(cleanAddress)
        val now = System.currentTimeMillis()

        return try {
            val locationsRef = userLocations(userId)
            val matchingDocs = locationsRef
                .whereEqualTo(FIELD_NORMALIZED_ADDRESS, normalizedAddress)
                .get()
                .await()
                .documents
                .sortedBy { it.getLong(FIELD_ADDED_AT) ?: Long.MAX_VALUE }

            if (matchingDocs.isNotEmpty()) {
                val primaryDoc = matchingDocs.first()
                val mergedUsageCount = matchingDocs
                    .sumOf { (it.getLong(FIELD_USAGE_COUNT)?.toInt() ?: 0).coerceAtLeast(0) }
                    .coerceAtLeast(0) + 1
                val addedAt = matchingDocs
                    .mapNotNull { it.getLong(FIELD_ADDED_AT) }
                    .minOrNull() ?: now

                val updatedLocation = mapOf(
                    FIELD_LABEL to cleanLabel,
                    FIELD_ADDRESS to cleanAddress,
                    FIELD_NORMALIZED_ADDRESS to normalizedAddress,
                    FIELD_LATITUDE to latitude,
                    FIELD_LONGITUDE to longitude,
                    FIELD_ADDED_AT to addedAt,
                    FIELD_UPDATED_AT to now,
                    FIELD_USAGE_COUNT to mergedUsageCount
                )

                val batch = firestore.batch()
                batch.set(primaryDoc.reference, updatedLocation, SetOptions.merge())
                matchingDocs.drop(1).forEach { duplicateDoc ->
                    batch.delete(duplicateDoc.reference)
                }
                batch.commit().await()

                val result = WorkLocation(
                    id = primaryDoc.id,
                    label = cleanLabel,
                    address = cleanAddress,
                    latitude = latitude,
                    longitude = longitude,
                    addedAt = addedAt,
                    usageCount = mergedUsageCount
                )
                Timber.d("Saved existing work location ${result.id} for user $userId")
                Result.success(result)
            } else {
                val existingLocations = locationsRef.get().await()
                if (existingLocations.size() >= MAX_WORK_LOCATIONS) {
                    return Result.failure(
                        IllegalStateException("You can save up to $MAX_WORK_LOCATIONS work locations")
                    )
                }

                val docRef = locationsRef.document()
                val newLocation = WorkLocation(
                    id = docRef.id,
                    label = cleanLabel,
                    address = cleanAddress,
                    latitude = latitude,
                    longitude = longitude,
                    addedAt = now,
                    usageCount = 1
                )
                docRef.set(
                    mapOf(
                        FIELD_LABEL to newLocation.label,
                        FIELD_ADDRESS to newLocation.address,
                        FIELD_NORMALIZED_ADDRESS to normalizedAddress,
                        FIELD_LATITUDE to newLocation.latitude,
                        FIELD_LONGITUDE to newLocation.longitude,
                        FIELD_ADDED_AT to newLocation.addedAt,
                        FIELD_UPDATED_AT to now,
                        FIELD_USAGE_COUNT to newLocation.usageCount
                    )
                ).await()

                Timber.d("Saved new work location ${newLocation.id} for user $userId")
                Result.success(newLocation)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to save work location for user $userId")
            Result.failure(e)
        }
    }

    suspend fun getWorkLocations(): Result<List<WorkLocation>> {
        val userId = auth.currentUser?.uid ?: return Result.success(emptyList())

        return try {
            val locations = userLocations(userId)
                .get()
                .await()
                .documents
                .map { it.toWorkLocation() }
                .sortedWith(
                    compareByDescending<WorkLocation> { it.usageCount }
                        .thenByDescending { it.addedAt }
                )

            Result.success(locations)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load work locations for user $userId")
            Result.failure(e)
        }
    }

    suspend fun removeWorkLocation(locationId: String): Result<Unit> {
        val userId = requireUserIdForWrite().getOrElse { return Result.failure(it) }
        if (locationId.isBlank()) {
            return Result.failure(IllegalArgumentException("Location ID cannot be empty"))
        }

        return try {
            userLocations(userId).document(locationId).delete().await()
            Timber.d("Removed work location $locationId for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove work location $locationId for user $userId")
            Result.failure(e)
        }
    }

    suspend fun isLocationSaved(address: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val normalizedAddress = normalizeAddress(address)
        if (normalizedAddress.isBlank()) return false

        return try {
            userLocations(userId)
                .whereEqualTo(FIELD_NORMALIZED_ADDRESS, normalizedAddress)
                .limit(1)
                .get()
                .await()
                .documents
                .isNotEmpty()
        } catch (e: Exception) {
            Timber.e(e, "Failed to check saved work location for user $userId")
            false
        }
    }
}
