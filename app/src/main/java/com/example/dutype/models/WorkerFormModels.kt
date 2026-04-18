package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

/**
 * Local/runtime-only models for worker profile enrichment.
 * These are NEVER written to Firestore — they are enriched at runtime
 * from worker_profiles, users, and other collections.
 */

@Keep
@Immutable
data class WorkExperience(
    val company: String = "",
    val position: String = "",
    val startDate: String = "",
    val endDate: String? = null,
    val description: String = "",
    val location: String? = null,
    val isCurrent: Boolean = false
)

