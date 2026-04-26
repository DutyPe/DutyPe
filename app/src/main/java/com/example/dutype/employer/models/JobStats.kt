package com.example.dutype.employer.models

/**
 * Canonical JobStats data class for employer job statistics
 * Used across EmployerHomeScreen, AnalyticsScreen, and employer history views.
 * 
 * SINGLE SOURCE OF TRUTH for job statistics
 */
data class JobStats(
    val activeJobs: Int = 0,
    val totalApplications: Int = 0,
    val todayJobs: Int = 0,
    val totalJobs: Int = 0
)
