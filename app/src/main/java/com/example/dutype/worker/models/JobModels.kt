package com.example.dutype.worker.models

import com.example.dutype.models.JobListing
import com.example.dutype.models.ApplicationStatus

data class AppliedJob(
    val jobListing: JobListing,
    val status: ApplicationStatus,
    val lastUpdated: Long,
    val employerMessage: String? = null
)
