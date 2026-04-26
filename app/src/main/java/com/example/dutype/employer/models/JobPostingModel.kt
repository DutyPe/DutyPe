package com.example.dutype.employer.models

import java.util.UUID

/**
 * JobPostingModel - For EMPLOYERS to POST jobs
 * Minimal fields used by the employer job-posting form and EmployerJobCard display.
 */
data class JobPostingModel(
    val jobId: String = UUID.randomUUID().toString(),
    val title: String,                              // Job Title (Cook, Driver, Helper)
    val payAmount: String,                          // 400, 10,000, etc.
    val payType: PayType,                           // DAILY, HOURLY, MONTHLY, TASK
    val location: String,                           // Area/Street/City
    val description: String,                        // Short description
    val contactNumber: String,                      // Direct contact
    val category: JobCategory,                      // Category (Cook, Maid, Driver, etc.)
    val shiftTiming: ShiftTiming = ShiftTiming.FLEXIBLE,
    val shiftTimingText: String? = null,
    val urgency: JobUrgency = JobUrgency.NORMAL,
    val vacancies: Int = 1,                         // Default 1
    val postedTime: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,                // From employer verification
    val employerId: String? = null,
    val employerName: String = "",
    val applicationsReceived: Int = 0,              // For card display
    val isFilled: Boolean = false,                  // For vacancy status
    val status: String = "open",
    val expiresAt: Long? = null,
    val imageUrl: String? = null                    // Bug #5: hero image for the card
)

