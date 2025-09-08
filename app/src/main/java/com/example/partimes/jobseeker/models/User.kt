package com.example.partimes.jobseeker.models

data class User(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val profilePicture: String? = null,
    val location: String? = null,
    val skills: List<String> = emptyList(),
    val experience: String? = null,
    val savedJobs: List<String> = emptyList(),
    val appliedJobs: List<String> = emptyList()
)
