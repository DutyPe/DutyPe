package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import com.example.dutype.jobs.JobQuery
import com.example.dutype.jobs.JobRepository
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.JobListing
import com.example.dutype.utils.LocationService
import com.example.dutype.utils.toJobListing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** One job opened by a worker (details, apply) and "similar jobs nearby". */
@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val locationPreferences: LocationPreferences,
    val locationService: LocationService
) : ViewModel() {

    /** Card + details; served from the session cache when the job was opened before. */
    suspend fun getJob(jobId: String, refresh: Boolean = false): Result<JobListing?> =
        jobRepository.getJob(jobId, refresh)

    /** Nearest open jobs of the same category (then any category), excluding [current]. */
    suspend fun similarJobs(current: JobListing, limit: Int = 5): Result<List<JobListing>> {
        val location = locationPreferences.getSavedLocation()?.takeIf { it.hasValidCoordinates() }
        val lat = location?.latitude ?: current.lat
        val lng = location?.longitude ?: current.lng
        val notCurrent = JobQuery(maxRadiusKm = SIMILAR_RADIUS_KM, matches = { it.id != current.id })
        return runCatching {
            val sameCategory = jobRepository.pager(lat, lng, notCurrent.copy(category = current.category.ifBlank { null }))
                .nextPage(limit).getOrThrow().jobs
            val jobs = sameCategory.ifEmpty { jobRepository.pager(lat, lng, notCurrent).nextPage(limit).getOrThrow().jobs }
            jobs.map { it.toJobListing() }
        }
    }

    private companion object {
        const val SIMILAR_RADIUS_KM = 20.0
    }
}
