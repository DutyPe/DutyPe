package com.example.dutype.utils

import com.example.dutype.models.JobListing

data class JobFilterPayload(
    val searchQuery: String = "",
    val category: String = "ALL",
    val maxDistanceKm: Double = 50.0,
    val userLat: Double? = null,
    val userLon: Double? = null
)

/**
 * Defensive Job Filtering & Geospatial Matching Engine
 */
object JobFilterEngine {

    fun filterJobs(jobs: List<JobListing>, payload: JobFilterPayload): List<JobListing> {
        val sanitizedQuery = payload.searchQuery.trim().lowercase()
        val sanitizedCategory = payload.category.trim().uppercase()

        return jobs.filter { job ->
            // 1. Category Matching (Supports "ALL" or exact match)
            val matchesCategory = sanitizedCategory == "ALL" || 
                    job.jobType.trim().uppercase() == sanitizedCategory

            // 2. Search Query Evaluation using JobSearchMatcher tokens
            val matchesQuery = if (sanitizedQuery.isBlank()) {
                true
            } else {
                val tokens = JobSearchMatcher.queryTokens(sanitizedQuery)
                val fields = JobSearchMatcher.Fields(
                    title = job.title,
                    companyName = job.companyName,
                    companyCity = job.location,
                    addressText = job.addressText,
                    jobType = job.jobType,
                    category = job.jobType
                )
                val evalResult = JobSearchMatcher.evaluate(sanitizedQuery, tokens, fields)
                evalResult.matches || job.title.lowercase().contains(sanitizedQuery) || job.location.lowercase().contains(sanitizedQuery)
            }

            // 3. Distance Radius Matching (with defensive fallback when coordinates are missing)
            val matchesDistance = if (payload.userLat != null && payload.userLon != null && GeoUtils.hasValidCoordinates(payload.userLat, payload.userLon)) {
                val distanceKm = GeoUtils.calculateHaversineDistance(payload.userLat, payload.userLon, job.lat, job.lng)
                distanceKm <= payload.maxDistanceKm
            } else {
                true // Allow jobs when GPS location is unavailable
            }

            matchesCategory && matchesQuery && matchesDistance
        }
    }
}
