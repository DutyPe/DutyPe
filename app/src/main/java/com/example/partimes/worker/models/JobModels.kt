package com.example.partimes.worker.models

import com.example.partimes.models.JobListing

// Compact Job Summary - for home screen or job cards
data class JobSummary(
    val jobId: String = "",
    val title: String = "",
    val company: String = "",
    val locationNearby: String = "", // separated
    val specificLocation: String = "", // separated
    val wage: String = "",
    val timing: String = "",
    val vacancies: Int = 1,
    val preferences: List<String> = listOf(),
    val isTrending: Boolean = false,
    val imageUrl: String = "",
    val phoneNumber: String = ""
)

// Extension function to convert JobListing to JobSummary
fun JobListing.toSummary(): JobSummary {
    return JobSummary(
        jobId = this.jobId?: "",
        title = this.title,
        company = this.company,
        locationNearby = this.locationNearby,
        specificLocation = this.specificLocation,
        wage = this.payAmount,
        timing = this.timing,
        vacancies = this.vacancies,
        preferences = this.preferences,
        isTrending = this.isTrending,
        imageUrl = this.imageUrl ?:"",
        phoneNumber = this.phoneNumber
    )
}

data class AppliedJob(
    val jobListing: JobListing,
    val status: ApplicationStatus,
    val lastUpdated: Long,
    val employerMessage: String? = null
)
