package com.example.dutype.models

/**
 * Place suggestion from Google Places Autocomplete API
 */
data class PlaceSuggestion(
    val placeId: String,
    val description: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
