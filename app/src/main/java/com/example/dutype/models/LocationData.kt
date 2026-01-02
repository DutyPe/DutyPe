package com.example.dutype.models

import androidx.annotation.Keep

/**
 * Represents detailed location data for the application.
 * Enhanced with more address fields for complete address display.
 */
@Keep
data class LocationData(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val state: String?,
    val country: String?,
    val postalCode: String?,
    // Enhanced fields for detailed address
    val area: String? = null,           // Sub-locality (neighborhood/area)
    val landmark: String? = null,       // Nearby landmark
    val streetName: String? = null,     // Street/road name
    val buildingName: String? = null,   // Building/apartment name
    val district: String? = null,       // District name
    val accuracy: Float = 0f,           // Location accuracy in meters
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
     * Get formatted medium address (Area, City, State PostalCode)
     * Example: "Nallagandla, Serilingampalle (M), Telangana 500019"
     */
    fun getMediumAddress(): String {
        val parts = mutableListOf<String>()
        area?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        city?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        state?.takeIf { it.isNotBlank() }?.let { 
            val stateWithPostal = if (!postalCode.isNullOrBlank()) "$it $postalCode" else it
            parts.add(stateWithPostal)
        }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
    
    /**
     * Get full detailed address with all components
     * Example: "Road No. 10, HUDA Layout, Nallagandla, Serilingampalle (M), Telangana 500019"
     */
    fun getFullAddress(): String {
        val parts = mutableListOf<String>()
        buildingName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        streetName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        area?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        landmark?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        city?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        district?.takeIf { it.isNotBlank() && it != city }?.let { parts.add(it) }
        state?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        postalCode?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
    
    /**
     * Get complete address with coordinates
     * Example: "17.4567, 78.3456, Road No. 10, HUDA Layout, Nallagandla, Serilingampalle (M), Telangana 500019"
     */
    fun getCompleteAddressWithCoordinates(): String {
        val coordsStr = if (hasValidCoordinates()) {
            "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
        } else ""
        
        val fullAddr = getFullAddress()
        
        return when {
            coordsStr.isNotBlank() && fullAddr.isNotBlank() -> "$coordsStr, $fullAddr"
            fullAddr.isNotBlank() -> fullAddr
            coordsStr.isNotBlank() -> coordsStr
            else -> address
        }
    }
    
    /**
     * Get display address for header (Area, City, State PostalCode)
     * Example: "Nallagandla, Serilingampalle (M), Telangana 500019"
     */
    fun getDisplayAddress(): String {
        val parts = mutableListOf<String>()
        
        // Add area/locality
        area?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        
        // Add city/municipality
        city?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        
        // Add state with postal code
        val statePostal = buildString {
            state?.takeIf { it.isNotBlank() }?.let { append(it) }
            postalCode?.takeIf { it.isNotBlank() }?.let { 
                if (isNotEmpty()) append(" ")
                append(it)
            }
        }
        if (statePostal.isNotBlank()) parts.add(statePostal)
        
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address
    }
    
    /**
     * Check if location has valid coordinates
     */
    fun hasValidCoordinates(): Boolean = latitude != 0.0 || longitude != 0.0
    
    /**
     * Check if location is accurate (within 100 meters)
     */
    fun isAccurate(): Boolean = accuracy > 0 && accuracy <= 100f
}
