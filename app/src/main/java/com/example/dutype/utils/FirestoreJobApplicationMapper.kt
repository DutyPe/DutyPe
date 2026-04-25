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

fun DocumentSnapshot.toJobApplicationOrNull(): JobApplication? {
    val data = data ?: return null
    val skills = (data["workerSkills"] as? List<*>)
        ?.mapNotNull { it?.toString()?.trim()?.takeIf { v -> v.isNotBlank() } }
        .orEmpty()
    return JobApplication(
        id = id,
        jobId = data["jobId"]?.toString().orEmpty(),
        workerId = data["workerId"]?.toString().orEmpty(),
        employerId = data["employerId"]?.toString().orEmpty(),
        status = ApplicationStatus.fromFirestoreValue(
            data["status"]?.toString() ?: ApplicationStatus.APPLIED.toFirestoreValue()
        ),
        createdAt = data["createdAt"].toEpochMillis().takeIf { it > 0L } ?: System.currentTimeMillis(),
        jobTitle = data["jobTitle"]?.toString().orEmpty(),
        jobLocation = data["jobLocation"]?.toString().orEmpty(),
        companyName = data["companyName"]?.toString().orEmpty(),
        workerName = data["workerName"]?.toString().orEmpty(),
        workerPhone = (data["workerPhone"] as? String)?.takeIf { it.isNotBlank() },
        workerEmail = (data["workerEmail"] as? String)?.takeIf { it.isNotBlank() },
        workerProfileImageUrl = (data["workerProfileImageUrl"] as? String)?.takeIf { it.isNotBlank() },
        workerSkills = skills,
        employerPhone = (data["employerPhone"] as? String)?.takeIf { it.isNotBlank() },
        jobStatus = data["jobStatus"]?.toString()?.takeIf { it.isNotBlank() } ?: "open"
    )
}
