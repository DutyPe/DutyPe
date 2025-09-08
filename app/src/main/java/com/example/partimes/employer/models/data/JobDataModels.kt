package com.example.partimes.employer.models.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.partimes.employer.models.enums.ApplicationStatus
import com.example.partimes.employer.models.enums.EmployerPayType


// Pay Information for Employer
data class EmployerPayInfo(
    val amount: String,
    val type: EmployerPayType,
    val period: String,
    val isNegotiable: Boolean = false,
    val currency: String = "₹"
) {
    fun getDisplayText(): String = "$currency$amount/$period"
    fun getTypeIcon(): ImageVector = when (type) {
        EmployerPayType.HOURLY -> Icons.Default.AccessTime
        EmployerPayType.DAILY -> Icons.Default.CalendarToday
        EmployerPayType.MONTHLY -> Icons.Default.Work
        EmployerPayType.PER_TASK -> Icons.Default.Build
    }
    fun getTypeEmoji(): String = when (type) {
        EmployerPayType.HOURLY -> "⏰"
        EmployerPayType.DAILY -> "📅"
        EmployerPayType.MONTHLY -> "💼"
        EmployerPayType.PER_TASK -> "🛠️"
    }
}

// Location Information for Job Posting
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

// Job Application from JobSeeker
data class JobApplication(
    val applicationId: String,
    val jobId: String,
    val jobSeekerId: String,
    val jobSeekerName: String,
    val jobSeekerPhone: String,
    val appliedTime: Long = System.currentTimeMillis(),
    val status: ApplicationStatus,
    val employerMessage: String? = null,
    val jobSeekerMessage: String? = null,
    val interviewScheduled: Long? = null,
    val documentsSubmitted: List<String> = emptyList()
) {
    fun getApplicationTimeAgo(): String {
        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - appliedTime
        val diffInHours = diffInMillis / (1000 * 60 * 60)
        val diffInDays = diffInHours / 24

        return when {
            diffInHours < 1 -> "Just now"
            diffInHours < 24 -> "${diffInHours}h ago"
            diffInDays < 7 -> "${diffInDays}d ago"
            else -> "${diffInDays / 7}w ago"
        }
    }
}
