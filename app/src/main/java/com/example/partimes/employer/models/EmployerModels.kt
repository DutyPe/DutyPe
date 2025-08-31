package com.example.partimes.models

data class EmployerJobPost(
    val id: String,
    val title: String,
    val location: String,
    val applicationsReceived: Int,
    val status: JobPostStatus,
    val postedOn: Long
)

enum class JobPostStatus {
    ACTIVE,
    PAUSED,
    CLOSED
}
