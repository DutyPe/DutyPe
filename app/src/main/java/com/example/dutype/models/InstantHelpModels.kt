package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

object InstantHelpDefaults {
    val categories = listOf("Cook", "Maid", "Helper", "Electrician", "Plumber", "Driver")
    val defaultWorkerCategories = listOf("Cook", "Maid", "Helper")
    val radiusOptionsKm = listOf(2.0, 5.0, 10.0)
}

@Keep
@Immutable
data class WorkerAvailability(
    val workerId: String = "",
    val isAvailable: Boolean = false,
    val status: String = "offline",
    val categories: List<String> = InstantHelpDefaults.defaultWorkerCategories,
    val radiusKm: Double = 5.0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geohash: String = "",
    val availableUntil: Long = 0L,
    val lastSeenAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Keep
@Immutable
data class InstantRequest(
    val requestId: String = "",
    val employerId: String = "",
    val employerName: String = "DutyPe employer",
    val employerPhone: String = "",
    val title: String = "Urgent need",
    val description: String = "",
    val category: String = "Helper",
    val needType: String = "urgent_now",
    val status: String = "open",
    val urgency: String = "urgent",
    val budgetText: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geohash: String = "",
    val addressText: String = "",
    val radiusKm: Double = 5.0,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val responseCount: Int = 0,
    val callCount: Int = 0,
    val selectedWorkerId: String = "",
    val completedAt: Long = 0L,
    val failureReason: String = "",
    val distanceKm: Double? = null
)

@Keep
@Immutable
data class InstantResponse(
    val responseId: String = "",
    val requestId: String = "",
    val workerId: String = "",
    val employerId: String = "",
    val workerName: String = "Worker",
    val workerPhone: String = "",
    val workerSkills: List<String> = emptyList(),
    val distanceKm: Double? = null,
    val status: String = "viewed",
    val createdAt: Long = 0L,
    val viewedAt: Long = 0L,
    val respondedAt: Long = 0L,
    val calledAt: Long = 0L,
    val acceptedAt: Long = 0L,
    val completedAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class QuickUrgentNeedInput(
    val title: String,
    val description: String,
    val category: String,
    val needType: String,
    val budgetText: String,
    val radiusKm: Double
)