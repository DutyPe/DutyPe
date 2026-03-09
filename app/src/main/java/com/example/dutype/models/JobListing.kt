package com.example.dutype.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * JobListing - MINIMAL MODEL (12 core fields)
 * Based on Urban Company/TaskRabbit patterns
 */
@Entity(tableName = "joblisting")
data class JobListing(
    @PrimaryKey
    val id: String = "",
    val employerId: String = "",
    val title: String = "",
    val companyName: String = "",
    val description: String = "",
    
    // Location (3 fields)
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    
    // Pay (2 fields)
    val payAmount: String = "", // "400", "15000"
    val payType: String = "", // "HOURLY", "DAILY", "MONTHLY"
    
    // Timing
    val shiftTiming: String = "",
    
    // Status (3 fields)
    val isActive: Boolean = true,
    val isFilled: Boolean = false,
    val postedAt: Long = System.currentTimeMillis(),
    
    // Contact
    val contactNumber: String = "",
    
    // Optional details
    val vacancies: Int = 1,
    val jobType: String = "FULL_TIME",
    val gender: String = "ANY",
    
    // Runtime (not in Firestore)
    var distance: Double? = null,
    var isSaved: Boolean = false
) {
    companion object {
        const val EXPIRY_DAYS = 30
    }
    
    // Computed properties for backward compatibility
    val jobId: String get() = id
    
    fun getCategory(): String = com.example.dutype.utils.CategoryDetector.detectCategory(title, description)
    fun getExpiresAt(): Long = postedAt + (EXPIRY_DAYS * 24 * 60 * 60 * 1000L)
    fun isExpired(): Boolean = System.currentTimeMillis() > getExpiresAt()
}
