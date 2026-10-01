package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class MatchedWorker(
    val workerId: String = "",
    val fullName: String = "Worker",
    val phone: String = "",
    val profileImageUrl: String = "",
    val skills: List<String> = emptyList(),
    val experience: String = "",
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val completedJobs: Int = 0,
    val isAvailable: Boolean = false,
    val distanceKm: Double? = null,
    val matchScore: Int = 0,
    val matchReasons: List<String> = emptyList()
)
