package com.example.partimes.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "joblisting")
data class JobListing(
    @PrimaryKey
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
