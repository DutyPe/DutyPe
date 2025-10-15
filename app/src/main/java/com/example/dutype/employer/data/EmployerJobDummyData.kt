package com.example.dutype.employer.data

import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.enums.*

object EmployerJobDummyData {
    fun getMyPostedJobs(): List<JobPostingModel> {
        // Return empty list - no dummy data
        return emptyList()
    }

    // Helper function to get jobs by employer
    fun getJobsByEmployer(employerId: String): List<JobPostingModel> {
        return getMyPostedJobs().filter { it.employerId == employerId }
    }

    // Helper function to get jobs by category
    fun getJobsByCategory(category: JobCategory): List<JobPostingModel> {
        return getMyPostedJobs().filter { it.category == category }
    }

    // Helper function to get recent jobs
    fun getRecentJobs(days: Int = 7): List<JobPostingModel> {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000)
        return getMyPostedJobs().filter { it.postedTime >= cutoffTime }
    }

    // Helper function to get verified jobs
    fun getVerifiedJobs(): List<JobPostingModel> {
        return getMyPostedJobs().filter { it.isVerified }
    }
}
