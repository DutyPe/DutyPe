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
            data["status"]?.toString() ?: ApplicationStatus.APPLIED.toFirestoreValue()
        ),
        createdAt = data["createdAt"].toEpochMillis().takeIf { it > 0L } ?: System.currentTimeMillis(),
        jobTitle = resolvedTitle,
        jobLocation = resolvedLocation,
        companyName = resolvedCompanyName,
        workerName = data["workerName"]?.toString().orEmpty(),
        workerPhone = data["workerPhone"]?.toString(),
        workerProfileImageUrl = data["workerProfileImageUrl"]?.toString(),
        coverLetter = data["coverLetter"]?.toString().orEmpty()
    )
}
