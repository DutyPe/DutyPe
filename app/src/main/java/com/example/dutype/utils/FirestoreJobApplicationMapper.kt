package com.example.dutype.utils

import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

private fun Any?.toEpochMillis(): Long {
    return when (this) {
        is Timestamp -> this.toDate().time
        is Number -> this.toLong()
        is Date -> this.time
        else -> 0L
    }
}

private fun Any?.toStringList(): List<String> {
    return (this as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
}

fun DocumentSnapshot.toJobApplicationOrNull(): JobApplication? {
    val data = data ?: return null
    val resolvedCompanyName = listOf("companyName", "company", "employerName")
        .firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
        .orEmpty()
    val resolvedTitle = listOf("jobTitle", "title")
        .firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
        .orEmpty()
    val resolvedLocation = listOf("jobLocation", "addressText", "location")
        .firstNotNullOfOrNull { key -> data[key]?.toString()?.trim()?.takeIf { it.isNotBlank() } }
        .orEmpty()

    return JobApplication(
        applicationId = id,
        id = id,
        jobId = data["jobId"]?.toString().orEmpty(),
        workerId = data["workerId"]?.toString().orEmpty(),
        employerId = data["employerId"]?.toString().orEmpty(),
        status = ApplicationStatus.fromFirestoreValue(
            data["status"]?.toString() ?: ApplicationStatus.PENDING.toFirestoreValue()
        ),
        createdAt = data["createdAt"].toEpochMillis().takeIf { it > 0L } ?: System.currentTimeMillis(),
        jobTitle = resolvedTitle,
        jobLocation = resolvedLocation,
        companyName = resolvedCompanyName,
        workerName = data["workerName"]?.toString().orEmpty(),
        workerEmail = data["workerEmail"]?.toString(),
        workerPhone = data["workerPhone"]?.toString(),
        workerLocation = data["workerLocation"]?.toString(),
        workerProfileImageUrl = data["workerProfileImageUrl"]?.toString(),
        workerLocalRating = (data["workerLocalRating"] as? Number)?.toFloat(),
        workerTotalReviews = (data["workerTotalReviews"] as? Number)?.toInt(),
        workerJobsInArea = (data["workerJobsInArea"] as? Number)?.toInt(),
        workerAadhaarVerified = data["workerAadhaarVerified"] as? Boolean,
        workerPhoneVerified = data["workerPhoneVerified"] as? Boolean,
        workerIdentityVerified = data["workerIdentityVerified"] as? Boolean,
        workerBackgroundCheckPassed = data["workerBackgroundCheckPassed"] as? Boolean,
        workerDateOfBirth = data["workerDateOfBirth"]?.toString(),
        workerGender = data["workerGender"]?.toString(),
        skills = data["skills"].toStringList(),
        skillsText = data["skillsText"]?.toString(),
        certifications = data["certifications"].toStringList(),
        languages = data["languages"].toStringList(),
        availability = data["availability"]?.toString(),
        expectedSalary = data["expectedSalary"]?.toString(),
        resumeUrl = data["resumeUrl"]?.toString(),
        coverLetter = data["coverLetter"]?.toString().orEmpty()
    )
}
