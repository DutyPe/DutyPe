package com.example.dutype.worker.models

import com.example.dutype.models.DocumentType as MainDocumentType

/**
 * Worker-specific data models for job applications.
 * 
 * Canonical models (ApplicationStatus, WorkExperience, DocumentType) live in
 * com.example.dutype.models to maintain single source of truth.
 */

/**
 * Personal information for job applications
 */
data class PersonalInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val emergencyContact: String = "",
    val emergencyPhone: String = ""
)

/**
 * Document model for worker uploads
 */
data class Document(
    val id: String = "",
    val name: String = "",
    val type: MainDocumentType,
    val url: String = "",
    val uploadedAt: Long = System.currentTimeMillis(),
    val size: Long = 0L,
    val mimeType: String = "",
    val localPath: String? = null
)
