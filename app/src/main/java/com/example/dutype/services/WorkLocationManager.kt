package com.example.dutype.services

import com.example.dutype.models.WorkLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkLocationManager - Manages saved work locations
 * Industry standard pattern (Uber, Swiggy, Zomato, Google Maps)
 * 
 * Features:
 * - Save frequently used work locations
 * - Auto-suggest saved locations
 * - Track usage count for smart suggestions
 * - Limit to 10 saved locations (industry standard)
 */
@Singleton
class WorkLocationManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val MAX_WORK_LOCATIONS = 10 // Industry standard limit
        private const val USERS_COLLECTION = "users"
        private const val LOCATIONS_CACHE_TTL_MS = 30_000L // 30 seconds
    }

    // In-memory cache to avoid repeated reads of the same user doc within a session
    private var locationsCache: Pair<Long, List<Map<String, Any>>>? = null
    private var locationsCacheUserId: String? = null

    @Suppress("UNCHECKED_CAST")
    private suspend fun getCachedLocations(userId: String): List<Map<String, Any>> {
        val cached = locationsCache
        if (cached != null && locationsCacheUserId == userId &&
            (System.currentTimeMillis() - cached.first) < LOCATIONS_CACHE_TTL_MS) {
            return cached.second
        }
        val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
        val locations = userDoc.get("workLocations") as? List<Map<String, Any>> ?: emptyList()
        locationsCache = System.currentTimeMillis() to locations
        locationsCacheUserId = userId
        return locations
    }

    private fun invalidateLocationsCache() {
        locationsCache = null
        locationsCacheUserId = null
    }

    /**
     * Save a new work location or update existing one
     * Following Uber/Swiggy pattern: Save location with label for quick access
     */
    suspend fun saveWorkLocation(
        label: String,
        address: String,
        latitude: Double,
        longitude: Double
    ): Result<WorkLocation> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            // Check if location already exists (by address)
            val existingLocations = getCachedLocations(userId)
            
            val existingLocation = existingLocations.find { 
                (it["address"] as? String) == address 
            }
            
            if (existingLocation != null) {
                // Update usage count for existing location - reuse already-fetched data
                val locationId = existingLocation["id"] as? String ?: ""
                incrementLocationUsage(userId, locationId, existingLocations)
                
                val updatedLocation = WorkLocation(
                    id = locationId,
                    label = label,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    addedAt = existingLocation["addedAt"] as? Long ?: System.currentTimeMillis(),
                    usageCount = (existingLocation["usageCount"] as? Long)?.toInt()?.plus(1) ?: 1
                )
                
                Timber.d("📍 WorkLocation: Updated existing location - $label")
                return Result.success(updatedLocation)
            }
            
            // Check if we've reached the limit
            if (existingLocations.size >= MAX_WORK_LOCATIONS) {
                // Remove the least used location
                val leastUsed = existingLocations.minByOrNull { 
                    (it["usageCount"] as? Long) ?: 0 
                }
                leastUsed?.let {
                    val leastUsedId = it["id"] as? String
                    if (leastUsedId != null) {
                        removeWorkLocation(leastUsedId)
                    }
                }
            }
            
            // Create new work location
            val newLocation = WorkLocation(
                id = UUID.randomUUID().toString(),
                label = label,
                address = address,
                latitude = latitude,
                longitude = longitude,
                addedAt = System.currentTimeMillis(),
                usageCount = 1
            )
            
            // Add to Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("workLocations", FieldValue.arrayUnion(
                    mapOf(
                        "id" to newLocation.id,
                        "label" to newLocation.label,
                        "address" to newLocation.address,
                        "latitude" to newLocation.latitude,
                        "longitude" to newLocation.longitude,
                        "addedAt" to newLocation.addedAt,
                        "usageCount" to newLocation.usageCount
                    )
                ))
                .await()
            invalidateLocationsCache()
            
            Timber.d("📍 WorkLocation: Saved new location - $label at $address")
            Result.success(newLocation)
        } catch (e: Exception) {
            Timber.e(e, "❌ WorkLocation: Failed to save location")
            Result.failure(e)
        }
    }

    /**
     * Get all saved work locations for current user
     * Sorted by usage count (most used first) - Smart suggestion pattern
     */
    suspend fun getWorkLocations(): Result<List<WorkLocation>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            val locationsData = getCachedLocations(userId)
            
            val locations = locationsData.mapNotNull { data ->
                try {
                    WorkLocation(
                        id = data["id"] as? String ?: "",
                        label = data["label"] as? String ?: "",
                        address = data["address"] as? String ?: "",
                        latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                        longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                        addedAt = (data["addedAt"] as? Number)?.toLong() ?: 0L,
                        usageCount = (data["usageCount"] as? Number)?.toInt() ?: 0
                    )
                } catch (e: Exception) {
                    Timber.e(e, "❌ WorkLocation: Failed to parse location")
                    null
                }
            }.sortedByDescending { it.usageCount } // Most used first
            
            Timber.d("📍 WorkLocation: Retrieved ${locations.size} saved locations")
            Result.success(locations)
        } catch (e: Exception) {
            Timber.e(e, "❌ WorkLocation: Failed to get locations")
            Result.failure(e)
        }
    }

    /**
     * Remove a saved work location
     */
    suspend fun removeWorkLocation(locationId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            
            val locationsData = getCachedLocations(userId)
            
            val locationToRemove = locationsData.find { it["id"] == locationId }
            
            if (locationToRemove != null) {
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .update("workLocations", FieldValue.arrayRemove(locationToRemove))
                    .await()
                invalidateLocationsCache()
                
                Timber.d("📍 WorkLocation: Removed location - $locationId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ WorkLocation: Failed to remove location")
            Result.failure(e)
        }
    }

    /**
     * Increment usage count for a location
     * Used for smart suggestions (most used locations appear first)
     * Accepts pre-fetched locations data to avoid redundant reads
     */
    private suspend fun incrementLocationUsage(userId: String, locationId: String, prefetchedLocations: List<Map<String, Any>>) {
        try {
            val updatedLocations = prefetchedLocations.map { location ->
                if (location["id"] == locationId) {
                    location.toMutableMap().apply {
                        this["usageCount"] = ((location["usageCount"] as? Long) ?: 0) + 1
                    }
                } else {
                    location
                }
            }
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("workLocations", updatedLocations)
                .await()
            invalidateLocationsCache()
            
            Timber.d("📍 WorkLocation: Incremented usage count for $locationId")
        } catch (e: Exception) {
            Timber.e(e, "❌ WorkLocation: Failed to increment usage count")
        }
    }

    /**
     * Check if a location is already saved
     */
    suspend fun isLocationSaved(address: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            val locationsData = getCachedLocations(userId)
            
            locationsData.any { (it["address"] as? String) == address }
        } catch (e: Exception) {
            Timber.e(e, "❌ WorkLocation: Failed to check if location is saved")
            false
        }
    }
}
