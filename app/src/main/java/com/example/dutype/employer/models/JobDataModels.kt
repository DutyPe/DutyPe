package com.example.dutype.employer.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Employer-specific data models for job posting and management.
 * 
 * NOTE: JobApplication model has been consolidated into models/JobApplicationModels.kt
 * Use com.example.dutype.models.JobApplication instead.
 * 
 * @see com.example.dutype.models.JobApplication
 * @see com.example.dutype.models.ApplicationStatus
 */

/**
 * Pay Information for Employer job posting
 */
data class EmployerPayInfo(
    val amount: String,
    val type: PayType,
    val period: String,
    val isNegotiable: Boolean = false,
    val currency: String = "₹"
) {
    fun getDisplayText(): String = "$currency$amount/$period"
    fun getTypeIcon(): ImageVector = when (type) {
        PayType.HOURLY -> Icons.Default.AccessTime
        PayType.DAILY -> Icons.Default.CalendarToday
        PayType.MONTHLY -> Icons.Default.Work
        PayType.TASK -> Icons.Default.Build
    }
    fun getTypeEmoji(): String = when (type) {
        PayType.HOURLY -> "⏰"
        PayType.DAILY -> "📅"
        PayType.MONTHLY -> "💼"
        PayType.TASK -> "🛠️"
    }
}

/**
 * Location Information for Job Posting
 */
data class EmployerLocationInfo(
    val area: String,
    val city: String,
    val state: String,
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distance: Double = 0.0,
    val isGPSLocation: Boolean = false
) {
    fun getDisplayText(): String = "$area, $city"
    fun getFullAddress(): String = if (address.isNotEmpty()) address else "$area, $city, $state"
}

// =============================================================================
// DEPRECATED: JobApplication has been moved to models/JobApplicationModels.kt
// =============================================================================
// 
// The JobApplication data class that was previously here has been consolidated
// into the canonical location at com.example.dutype.models.JobApplication.
// 
// Migration:
// - Replace: import com.example.dutype.employer.models.data.JobApplication
// - With:    import com.example.dutype.models.JobApplication
//
// - Replace: import com.example.dutype.employer.models.enums.ApplicationStatus
// - With:    import com.example.dutype.models.ApplicationStatus
// =============================================================================
