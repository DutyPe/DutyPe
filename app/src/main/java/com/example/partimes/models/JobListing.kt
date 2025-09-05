package com.example.partimes.models

data class JobListing(
    val jobId: String,
    val employerId: String,
    val title: String,
    val company: String,
    val specificLocation: String,
    val locationNearby: String,
    val wage: String,
    val timing: String,
    val description: String,
    val preferences: List<String>,
    val vacancies: Int,
    val isActive: Boolean,
    val isTrending: Boolean,
    val postedAt: Long,
    val imageUrl: String,
    val phoneNumber: String
)
