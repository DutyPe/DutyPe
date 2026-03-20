package com.example.dutype.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * JobListing — strict target schema model.
 *
 * Firestore jobs collection:
 *   jobId (doc ID), employerId, title, jobType, salary, salaryType,
 *   location:{lat,lng}, geohash, urgency, status, createdAt, expiresAt
 *
 * job_details collection (loaded on click):
 *   jobId, description, contactNumber, addressText
 *
 * Runtime-only fields (never written to Firestore):
 *   distance, isSaved, companyName, description, location, addressText, contactNumber
 */
@Entity(tableName = "joblisting")
data class JobListing(
    @PrimaryKey
    val id: String = "",
    val employerId: String = "",
    val title: String = "",

    // --- CORE SCHEMA FIELDS ---
    val salary: Double = 0.0,
    val salaryType: String = "",        // "HOURLY" | "DAILY" | "MONTHLY"
    val jobType: String = "",
    val geohash: String = "",
    val urgency: String = "MEDIUM",     // "LOW" | "MEDIUM" | "HIGH"
    val gender: String = "Any",
    val experienceRequired: String = "No Experience Required",
    val shiftTiming: String = "Flexible",
    val isVerified: Boolean = false,
    val applicationCount: Int = 0,
    val status: String = "open",        // "open" | "closed" | "expired"
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + (30L * 24 * 60 * 60 * 1000),
    val lat: Double = 0.0,
    val lng: Double = 0.0,

    // --- RUNTIME ONLY (from job_details, never stored in jobs collection) ---
    val companyName: String = "",
    val description: String = "",
    val location: String = "",          // human-readable addressText
    val addressText: String = "",
    val contactNumber: String = "",
    val whatsappNumber: String = "",
    val vacancies: Int = 1,
    val workingHours: String = "",
    val educationRequired: String = "",
    val benefits: List<String> = emptyList(),

    // --- RUNTIME ONLY (computed, never stored) ---
    var distance: Double? = null,
    var isSaved: Boolean = false
) {
    // Stable alias used throughout the codebase
    val jobId: String get() = id

    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun getCategory(): String = com.example.dutype.utils.CategoryDetector.detectCategory(title, description)
}
