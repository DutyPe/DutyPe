package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Stable
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.utils.SalaryFormatter

/**
 * A job CARD — exactly the fields of `jobmetadata/{id}` ([com.example.dutype.firestore.FirestoreSchema.Jobs]).
 * Lists, map and search use only this model. Built by [com.example.dutype.utils.toJobListingSummary].
 */
@Keep
@Stable
data class JobListingSummary(
    val id: String = "",
    val employerId: String = "",
    val title: String = "",
    val category: String = "",               // category key, e.g. DRIVER
    val employmentType: String = Values.EmploymentType.FULL_TIME,
    val companyName: String = "",
    val photoUrl: String? = null,
    val payAmount: Long = 0L,                // whole rupees; 0 = negotiable
    val payType: String = Values.PayType.DAILY,
    val vacancies: Int = 1,
    val urgency: String = Values.Urgency.NORMAL,
    val shift: String = Values.Shift.ANY,
    val area: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val geohash: String = "",
    val district: String = "",               // set by the server from lat/lng
    val state: String = "",
    val status: String = Values.JobStatus.OPEN,
    val applicationCount: Int = 0,
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,

    // Runtime only (never stored)
    var distance: Double? = null,
    /** True when the worker allowed only approximate location: show "~3 km". */
    var distanceApprox: Boolean = false,
    /** The feed section the job was loaded in (km band, district, state). */
    var feedSection: FeedSection? = null,
    var isSaved: Boolean = false,
    var isApplied: Boolean = false
) {
    val jobId: String get() = id

    val payText: String get() = SalaryFormatter.display(payAmount, payType)

    val isUrgent: Boolean get() = urgency == Values.Urgency.HIGH

    fun isExpired(): Boolean = status == Values.JobStatus.EXPIRED ||
        (status == Values.JobStatus.OPEN && expiresAt > 0L && System.currentTimeMillis() > expiresAt)

    fun isOpen(): Boolean = status == Values.JobStatus.OPEN && !isExpired()
}
