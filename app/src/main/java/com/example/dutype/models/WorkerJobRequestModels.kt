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
    val isAvailable: Boolean = true,
    val distanceKm: Double? = null,
    val matchScore: Int = 0,
    val matchReasons: List<String> = emptyList(),
    val requestId: String = "",
    val requestStatus: String = ""
)

@Keep
@Immutable
data class WorkerJobRequest(
    val requestId: String = "",
    val jobId: String = "",
    val workerId: String = "",
    val employerId: String = "",
    val status: String = "pending",
    val jobTitle: String = "Job request",
    val companyName: String = "DutyPe employer",
    val jobLocation: String = "",
    val salary: String = "",
    val salaryType: String = "",
    val jobType: String = "",
    val employerName: String = "Employer",
    val employerPhone: String = "",
    val workerName: String = "",
    val workerSkills: List<String> = emptyList(),
    val matchScore: Int = 0,
    val matchReasons: List<String> = emptyList(),
    val distanceKm: Double? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
