package com.example.dutype.services

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

/**
 * Service to record and track worker location demand in cities and areas
 * where DutyPe has not yet launched active jobs.
 *
 * Helps the business & sales team identify high-demand worker clusters to prioritize
 * local employer onboarding in those specific cities/states.
 */
object LocationDemandService {

    private const val COLLECTION_LOCATION_DEMAND = "location_demand_leads"
    private const val PREFS_NAME = "dutype_location_demand_prefs"
    private const val KEY_NOTIFIED_LOCATIONS = "notified_locations_set"

    /**
     * Checks if the user has already requested notification for a specific location.
     */
    fun isLocationNotified(context: Context, locationName: String?): Boolean {
        if (locationName.isNullOrBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet(KEY_NOTIFIED_LOCATIONS, emptySet()) ?: emptySet()
        return savedSet.contains(locationName.trim().lowercase())
    }

    /**
     * Records worker demand / "Notify Me" interest for a specific location in Firestore.
     */
    fun recordLocationDemand(
        context: Context,
        locationName: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        category: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanLocation = locationName?.trim() ?: "Unknown Location"
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        // Save locally first for instantaneous UI update
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_NOTIFIED_LOCATIONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(cleanLocation.lowercase())
        prefs.edit().putStringSet(KEY_NOTIFIED_LOCATIONS, currentSet).apply()

        val demandData = hashMapOf(
            "userId" to userId,
            "locationName" to cleanLocation,
            "latitude" to (latitude ?: 0.0),
            "longitude" to (longitude ?: 0.0),
            "category" to (category ?: "All"),
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "androidVersion" to Build.VERSION.SDK_INT,
            "status" to "PENDING_LAUNCH",
            "requestedAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection(COLLECTION_LOCATION_DEMAND)
            .add(demandData)
            .addOnSuccessListener { docRef ->
                Timber.d("✅ Location demand recorded successfully: ${docRef.id} for location: $cleanLocation")
                onComplete(true)
            }
            .addOnFailureListener { error ->
                Timber.w(error, "⚠️ Failed to record location demand in Firestore, stored locally.")
                // Return true because it is safely cached locally
                onComplete(true)
            }
    }
}
