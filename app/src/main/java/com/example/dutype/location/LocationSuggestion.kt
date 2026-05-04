package com.example.dutype.location

data class LocationSuggestion(
    val placeId: String,
    val displayName: String,
    val city: String,
    val state: String,
    val country: String,
    val area: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)