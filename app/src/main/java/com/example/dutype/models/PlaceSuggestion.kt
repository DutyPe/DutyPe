package com.example.dutype.models

import androidx.compose.runtime.Immutable

/**
 * Place suggestion from Google Places Autocomplete API
 */
@Immutable
data class PlaceSuggestion(
    val placeId: String,
    val description: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
