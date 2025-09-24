package com.example.partimes.models

import java.time.LocalDateTime

data class JobApplication(
    val applicationId: String? = null,
    val jobId: String,
    val jobseekerId: String? = null,
    val jobseekerEmail: String? = null,
    val jobseekerName: String? = null,
    val jobseekerPhone: String? = null,
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    
    // Personal information
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val location: String? = null,
    
    // Experience and skills
    val experience: String? = null,
    val skills: List<String>? = null,
    val education: String? = null,
    val resumeUrl: String? = null,
    val coverLetter: String? = null,
    
    // Additional documents
    val documentUrls: List<String>? = null,
    
    // Application metadata
    val appliedAt: String? = null,
    val updatedAt: String? = null,
    val notes: String? = null,
    val rejectionReason: String? = null,
    
    // Interview details
    val interviewScheduledAt: String? = null,
    val interviewLocation: String? = null,
    val interviewNotes: String? = null
)

enum class ApplicationStatus {
    PENDING,
    REVIEWED,
    SHORTLISTED,
    INTERVIEW_SCHEDULED,
    INTERVIEWED,
    SELECTED,
    REJECTED,
    WITHDRAWN
}
