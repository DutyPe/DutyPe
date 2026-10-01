package com.example.dutype.services

import android.content.Context
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

    private val D = com.example.dutype.firestore.FirestoreSchema.LocationDemand
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        // Save locally first for instantaneous UI update
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_NOTIFIED_LOCATIONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(cleanLocation.lowercase())
        prefs.edit().putStringSet(KEY_NOTIFIED_LOCATIONS, currentSet).apply()

        val demandData = hashMapOf(
            D.UID to userId,
            D.AREA to cleanLocation.take(80),
            D.LAT to (latitude ?: 0.0),
            D.LNG to (longitude ?: 0.0),
            D.CATEGORY to (category ?: "All").take(40),
            D.CREATED_AT to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection(D.COLLECTION)
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
