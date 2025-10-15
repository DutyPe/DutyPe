package com.example.dutype.models

/**
 * Represents location data for the application.
 */
data class LocationData(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val state: String?,
    val country: String?,
    val postalCode: String?
)
