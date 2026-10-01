package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.jobs.JobQuery
import com.example.dutype.jobs.JobRepository
import com.example.dutype.jobs.JobFeedPager
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.JobListing
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.JobCategoryResolver
import com.example.dutype.utils.LocationService
import com.example.dutype.utils.toJobListing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val currentCategory: String = ALL
)

private const val ALL = "All"

/** Jobs of one category (or all), nearest first, 10 per page. */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    val locationPreferences: LocationPreferences,
    val locationService: LocationService
) : ViewModel() {

    private companion object {
        const val PAGE_SIZE = 10
    }

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    private var pager: JobFeedPager? = null
    private var loadJob: Job? = null
    private var generation = 0

    init {
        locationPreferences.getSavedLocation()?.takeIf { it.hasValidCoordinates() }?.let {
            userLatitude = it.latitude
            userLongitude = it.longitude
        }
    }

    fun setUserLocation(latitude: Double, longitude: Double) {
        if (!GeoUtils.hasValidCoordinates(latitude, longitude)) return
        val changed = latitude != userLatitude || longitude != userLongitude
        userLatitude = latitude
        userLongitude = longitude
        if (changed && pager != null) loadJobsForCategory(_uiState.value.currentCategory, force = true)
    }

    fun loadJobsForCategory(category: String, force: Boolean = false) {
        val normalized = if (category.equals("All Jobs", true)) ALL else category
        val state = _uiState.value
        if (!force && state.currentCategory == normalized && (state.isLoading || state.jobs.isNotEmpty())) return
        val gen = ++generation
        loadJob?.cancel()
        val newPager = jobRepository.pager(userLatitude, userLongitude, JobQuery(category = categoryKey(normalized)))
        pager = newPager
        _uiState.value = CategoriesUiState(isLoading = true, currentCategory = normalized)
        loadJob = viewModelScope.launch {
            val result = newPager.nextPage(PAGE_SIZE)
            if (gen != generation) return@launch
            result.onSuccess { page ->
                _uiState.update { it.copy(jobs = page.jobs.map { job -> job.toJobListing() }, isLoading = false, hasMore = page.hasMore) }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun refreshJobs() {
        loadJobsForCategory(_uiState.value.currentCategory, force = true)
    }

    fun loadMoreJobs() {
        val state = _uiState.value
        val active = pager ?: return
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        val gen = generation
        _uiState.update { it.copy(isLoadingMore = true) }
        loadJob = viewModelScope.launch {
            val result = active.nextPage(PAGE_SIZE)
            if (gen != generation) return@launch
            result.onSuccess { page ->
                _uiState.update { current ->
                    val known = current.jobs.mapTo(HashSet()) { it.id }
                    current.copy(
                        jobs = current.jobs + page.jobs.filter { it.id !in known }.map { it.toJobListing() },
                        isLoadingMore = false,
                        hasMore = page.hasMore
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private fun categoryKey(category: String): String? {
        if (category == ALL) return null
        return JobCategory.entries.firstOrNull { it.displayName.equals(category, true) || it.name.equals(category, true) }?.name
            ?: JobCategoryResolver.enumNameForDisplay(category)
            ?: category.uppercase().replace(' ', '_')
    }
}
