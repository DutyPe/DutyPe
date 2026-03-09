package com.example.dutype.models

import androidx.annotation.Keep

/**
 * Simplified location data for job search application.
 * Optimized based on LinkedIn, Indeed, and Naukri best practices.
 * 
 * For job search apps, only these fields are needed:
 * - lat/lng for distance calculation (nearby jobs)
 * - city for city-level filtering
 * - area for neighborhood-level filtering (useful in India)
 * - address for display
 * 
 * Removed unnecessary fields:
 * - buildingName, streetName, landmark (not needed for job matching)
 * - district (redundant with city)
 * - postalCode (not used for job filtering)
 */
@Keep
data class LocationData(
    // Core fields for job search
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val address: String,
    
    // Optional fields
    val area: String? = null,      // Neighborhood/locality (useful in India)
    val state: String? = null,     // State for broader filtering
    val country: String? = "India",
    
    // Metadata
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get formatted short address (Area, City)
     * Example: "Nallagandla, Serilingampalle"
     */
    fun getShortAddress(): String {
        return when {
            !area.isNullOrBlank() && !city.isNullOrBlank() -> "$area, $city"
            !city.isNullOrBlank() -> city
            !area.isNullOrBlank() -> area
            address.isNotBlank() -> address.split(",").firstOrNull()?.trim() ?: address
            else -> "Location unavailable"
        }
    }
    
    /**
     * Get formatted medium address (Area, City, State)
     * Example: "Nallagandla, Serilingampalle, Telangana"
     */
    fun getMediumAddress(): String {
        val parts = mutableListOf<String>()
        area?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        city?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        state?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
    
    /**
     * Get full address for display
     * Example: "Nallagandla, Serilingampalle, Telangana, India"
     */
    fun getFullAddress(): String {
        val parts = mutableListOf<String>()
        area?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        city?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        state?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        country?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
    
    /**
     * Get display address for UI (Area, City, State)
     * Example: "Nallagandla, Serilingampalle, Telangana"
     */
    fun getDisplayAddress(): String = getMediumAddress()
    
    /**
     * Check if location has valid coordinates
     */
    fun hasValidCoordinates(): Boolean = latitude != 0.0 || longitude != 0.0
    
    /**
     * Check if location is accurate (within 100 meters)
     */
    fun isAccurate(): Boolean = accuracy > 0 && accuracy <= 100f
}
