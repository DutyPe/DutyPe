package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class WorkLocation(
    val id: String = "",
    val label: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)
