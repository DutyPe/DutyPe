package com.example.dutype.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.metadata.MetadataManager
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.state.SavedJobsStateManager
import com.example.dutype.state.ApplicationStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber

data class FirestoreJobUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = true, // Start with loading true to show shimmer immediately
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
    val totalJobs: Int = 0,
    val lastCreatedAt: Long? = null,
    val isLoadingMore: Boolean = false,
    val isPrefetching: Boolean = false,
    val prefetchedJobs: List<JobListing> = emptyList(), // Jobs prefetched for next page
    val usingSummaries: Boolean = false // Flag to indicate if using lightweight summaries
)

/**
 * PERFORMANCE FIX: Maximum jobs to keep in memory to prevent OOM on low-end devices
 * At scale (5L+ users), this prevents memory bloat
 */
private const val MAX_JOBS_IN_MEMORY = 500

@OptIn(FlowPreview::class)
@HiltViewModel
class FirestoreJobViewModel @Inject constructor(
    private val firestoreJobRepository: FirestoreJobRepository,
    private val savedJobsStateManager: SavedJobsStateManager,
    private val applicationStateManager: ApplicationStateManager,
    private val metadataManager: MetadataManager,
    private val savedStateHandle: SavedStateHandle,
    val locationService: com.example.dutype.utils.LocationService,
    val locationPreferences: com.example.dutype.location.LocationPreferences,
    val jobShareImageGenerator: com.example.dutype.services.JobShareImageGenerator
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FirestoreJobUiState())
    val uiState: StateFlow<FirestoreJobUiState> = _uiState.asStateFlow()
    
    // User location for distance calculation - restored from SavedStateHandle
    private var userLatitude: Double = savedStateHandle.get<Double>("userLatitude") ?: 0.0
    private var userLongitude: Double = savedStateHandle.get<Double>("userLongitude") ?: 0.0
    
    // Distance filter (in km) - Double.MAX_VALUE means no filter
    private val _maxDistanceFilter = MutableStateFlow(Double.MAX_VALUE)
    
    // Guard to prevent duplicate loadJobs calls
    private var hasInitiallyLoaded = false
    
    // =============================================================================
    // PERFORMANCE FIX P0: Filtered jobs computed in ViewModel (not Composable)
    // This prevents excessive recomposition when applications list changes
    // =============================================================================
    
    /**
     * PERFORMANCE FIX P0: Filtered jobs StateFlow
     * Combines jobs with applied job IDs to filter out:
     * - Filled jobs
     * - Expired jobs  
     * - Jobs user has already applied to
     * - Jobs outside distance filter
     * 
     * This is computed in ViewModel instead of Composable to prevent recomposition storms
     */
    val filteredJobs: StateFlow<List<JobListing>> = combine(
        _uiState,
        applicationStateManager.appliedJobIds,
        _maxDistanceFilter
    ) { state, appliedIds, maxDistance ->
        if (state.isLoading || state.hasError) {
            emptyList()
        } else {
            state.jobs
                .filter { job -> !job.isFilled }
                .filter { job -> !job.isExpired() }
                .filter { job -> job.jobId !in appliedIds }
                .filter { job -> 
                    // Apply distance filter
                    maxDistance == Double.MAX_VALUE || 
                    job.distance == null || 
                    job.distance!! <= maxDistance 
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    /**
     * Set maximum distance filter for jobs
     * @param maxKm Maximum distance in kilometers, or Double.MAX_VALUE for no filter
     */
    fun setDistanceFilter(maxKm: Double) {
        _maxDistanceFilter.value = maxKm
        Timber.d("📍 Distance filter set to: ${if (maxKm == Double.MAX_VALUE) "All" else "${maxKm}km"}")
    }
    
    // =============================================================================
    // PERFORMANCE FIX P2: Vacancy statuses managed in ViewModel
    // Cleared on refresh to prevent unbounded memory growth
    // =============================================================================
    
    private val _jobVacancyStatuses = MutableStateFlow<Map<String, JobVacancyStatus>>(emptyMap())
    val jobVacancyStatuses: StateFlow<Map<String, JobVacancyStatus>> = _jobVacancyStatuses.asStateFlow()
    
    // Track which job IDs we've already loaded vacancy status for
    private val loadedVacancyJobIds = mutableSetOf<String>()
    
    /**
     * Update vacancy statuses for jobs (called from screen)
     */
    fun updateVacancyStatuses(statusMap: Map<String, JobVacancyStatus>) {
        _jobVacancyStatuses.value = _jobVacancyStatuses.value + statusMap
    }
    
    /**
     * Check if vacancy status needs loading for given job IDs
     * Returns list of job IDs that haven't been loaded yet
     */
    fun getUnloadedVacancyJobIds(jobIds: List<String>): List<String> {
        return jobIds.filter { it !in loadedVacancyJobIds }
    }
    
    /**
     * Mark job IDs as loaded for vacancy status
     */
    fun markVacancyJobIdsAsLoaded(jobIds: List<String>) {
        loadedVacancyJobIds.addAll(jobIds)
    }
    
    /**
     * Clear vacancy statuses (called on refresh to prevent memory leak)
     */
    private fun clearVacancyStatuses() {
        _jobVacancyStatuses.value = emptyMap()
        loadedVacancyJobIds.clear()
        Timber.d("🧹 Cleared vacancy statuses to prevent memory leak")
    }
    
    // =============================================================================
    // PERFORMANCE FIX P1: Location debouncing (500ms)
    // Prevents CPU spikes from rapid location updates
    // =============================================================================
    
    private val locationDebouncer = MutableStateFlow<Pair<Double, Double>?>(null)
    
    init {
        // PERFORMANCE FIX: Use lightweight summaries by default (~70% less bandwidth)
        // Full job details are fetched on-demand when user clicks a job card
        loadJobsSummary()
        
        // Listen to centralized saved jobs state and update job saved status
        viewModelScope.launch {
            combine(
                savedJobsStateManager.savedJobIds,
                savedJobsStateManager.refreshTrigger
            ) { savedJobIds, _ ->
                // Update saved status for all current jobs
                val currentJobs = _uiState.value.jobs
                val updatedJobs = currentJobs.map { job ->
                    job.copy(isSaved = savedJobIds.contains(job.id))
                }
                _uiState.value = _uiState.value.copy(jobs = updatedJobs)
            }.collect { }
        }
        
        // PERFORMANCE FIX P1: Location debouncing - prevents CPU spikes
        // Only recalculate distances after 500ms of no location updates
        viewModelScope.launch {
            locationDebouncer
                .debounce(500)
                .filterNotNull()
                .collect { (lat, lon) ->
                    Timber.d("📍 DEBOUNCED: Recalculating distances after 500ms debounce")
                    recalculateDistancesInternal(lat, lon)
                }
        }
    }
    
    /**
     * Set user location for distance calculation
     * Called when worker's location is updated for accurate distance display
     * Location is persisted in SavedStateHandle to survive process death
     * 
     * PERFORMANCE FIX P1: Uses debouncing to prevent CPU spikes from rapid updates
     */
    fun setUserLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
        
        // Persist to SavedStateHandle for process death survival
        savedStateHandle["userLatitude"] = latitude
        savedStateHandle["userLongitude"] = longitude
        
        Timber.d("📍 ViewModel: User location set - lat=$latitude, lon=$longitude (debouncing...)")
        
        // PERFORMANCE FIX P1: Debounce location updates to prevent CPU spikes
        // Distance recalculation will happen after 500ms of no updates
        if (_uiState.value.jobs.isNotEmpty()) {
            locationDebouncer.value = Pair(latitude, longitude)
        }
    }
    
    /**
     * Internal function to recalculate distances (called after debounce)
     */
    private suspend fun recalculateDistancesInternal(latitude: Double, longitude: Double) {
        if (_uiState.value.jobs.isEmpty()) return
        
        withContext(Dispatchers.Default) {
            Timber.d("📍 ViewModel: Recalculating distances for ${_uiState.value.jobs.size} jobs...")
            val jobsWithDistance = firestoreJobRepository.calculateJobsDistances(
                _uiState.value.jobs, latitude, longitude
            )
            
            // Log some sample distances for debugging
            jobsWithDistance.take(3).forEach { job ->
                Timber.d("📍 ViewModel: Job '${job.title}' - jobLat=${job.latitude}, jobLon=${job.longitude}, distance=${job.distance}km")
            }
            
            // Update UI state on main thread
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(jobs = jobsWithDistance)
                Timber.d("📍 ViewModel: Distance recalculation complete")
            }
        }
    }
    
    fun loadJobs(limit: Long = 50L) {
        // Skip if already loading or has loaded (prevents duplicate calls from recomposition)
        if (_uiState.value.isLoading && hasInitiallyLoaded) {
            Timber.d("🔍 loadJobs skipped - already loading")
            return
        }
        
        // Skip if we already have jobs and this is a duplicate call (not a refresh)
        if (hasInitiallyLoaded && _uiState.value.jobs.isNotEmpty() && !_uiState.value.isRefreshing) {
            Timber.d("🔍 loadJobs skipped - already loaded ${_uiState.value.jobs.size} jobs")
            return
        }
        
        hasInitiallyLoaded = true
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null, 
                hasError = false,
                jobs = emptyList(), // Reset list on fresh load
                lastCreatedAt = null,
                hasMore = true
            )
            
            try {
                Timber.d("🔍 Loading all jobs for workers (limit: $limit)")
                firestoreJobRepository.getAllJobs(limit).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            Timber.d("✅ Successfully loaded ${jobs.size} jobs for workers")
                            // Jobs already have saved status from repository (with caching)
                            var processedJobs = jobs
                            
                            // PERFORMANCE FIX P1: Enforce max jobs limit to prevent memory bloat
                            if (processedJobs.size > MAX_JOBS_IN_MEMORY) {
                                Timber.w("⚠️ Truncating ${processedJobs.size} jobs to $MAX_JOBS_IN_MEMORY to prevent OOM")
                                processedJobs = processedJobs.take(MAX_JOBS_IN_MEMORY)
                            }
                            
                            // Calculate distances if user location is available
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                processedJobs = firestoreJobRepository.calculateJobsDistances(
                                    processedJobs, userLatitude, userLongitude
                                )
                            }
                            
                            val lastJob = processedJobs.lastOrNull()
                            
                            _uiState.value = _uiState.value.copy(
                                jobs = processedJobs,
                                isLoading = false,
                                totalJobs = processedJobs.size,
                                hasMore = processedJobs.size >= limit && processedJobs.size < MAX_JOBS_IN_MEMORY,
                                lastCreatedAt = lastJob?.postedAt,
                                prefetchedJobs = emptyList() // Clear any stale prefetched data
                            )
                            
                            // Update job metadata with loaded jobs for category stats
                            metadataManager.updateJobMetadataFromJobs(processedJobs)
                            
                            // Start prefetching next page for faster pagination
                            prefetchNextPage(limit)
                        },
                        onFailure = { exception ->
                            Timber.w("❌ Failed to load jobs for workers: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e("❌ Exception loading jobs for workers: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs"
                )
            }
        }
    }
    
    /**
     * PERFORMANCE OPTIMIZATION: Load jobs using lightweight summaries
     * Fetches only ~15 fields instead of 50+ fields per job
     * Reduces network payload by ~70% and improves list rendering performance
     * 
     * Use this for list views where full job details aren't needed.
     * Full details are fetched on-demand when user clicks a job card.
     */
    fun loadJobsSummary(limit: Long = 50L) {
        // Skip if already loading or has loaded (prevents duplicate calls from recomposition)
        if (_uiState.value.isLoading && hasInitiallyLoaded) {
            Timber.d("🔍 loadJobsSummary skipped - already loading")
            return
        }
        
        // Skip if we already have jobs and this is a duplicate call (not a refresh)
        if (hasInitiallyLoaded && _uiState.value.jobs.isNotEmpty() && !_uiState.value.isRefreshing) {
            Timber.d("🔍 loadJobsSummary skipped - already loaded ${_uiState.value.jobs.size} jobs")
            return
        }
        
        hasInitiallyLoaded = true
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null, 
                hasError = false,
                jobs = emptyList(),
                lastCreatedAt = null,
                hasMore = true,
                usingSummaries = true
            )
            
            try {
                Timber.d("📦 Loading job summaries for workers (limit: $limit) - LIGHTWEIGHT MODE")
                firestoreJobRepository.getAllJobsSummary(limit).collect { result ->
                    result.fold(
                        onSuccess = { summaries ->
                            Timber.d("✅ Successfully loaded ${summaries.size} job summaries (~70% less data)")
                            
                            // Calculate distances if user location is available
                            var processedSummaries = summaries
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                processedSummaries = firestoreJobRepository.calculateSummaryDistances(
                                    summaries, userLatitude, userLongitude
                                )
                            }
                            
                            // Convert summaries to JobListing for UI compatibility
                            val jobs = processedSummaries.map { it.toJobListing() }
                            val lastJob = jobs.lastOrNull()
                            
                            _uiState.value = _uiState.value.copy(
                                jobs = jobs,
                                isLoading = false,
                                totalJobs = jobs.size,
                                hasMore = jobs.size >= limit,
                                lastCreatedAt = lastJob?.postedAt,
                                prefetchedJobs = emptyList(),
                                usingSummaries = true
                            )
                            
                            // Update job metadata with loaded jobs for category stats
                            metadataManager.updateJobMetadataFromJobs(jobs)
                        },
                        onFailure = { exception ->
                            Timber.w("❌ Failed to load job summaries: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e("❌ Exception loading job summaries: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs"
                )
            }
        }
    }

    fun loadMoreJobs(limit: Long = 20L) {
        // PERFORMANCE FIX P1: Don't load more if we've hit the memory limit
        if (_uiState.value.jobs.size >= MAX_JOBS_IN_MEMORY) {
            Timber.d("🔍 loadMoreJobs skipped - hit max jobs limit ($MAX_JOBS_IN_MEMORY)")
            _uiState.value = _uiState.value.copy(hasMore = false)
            return
        }
        
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            // Check if we have prefetched jobs available
            val prefetchedJobs = _uiState.value.prefetchedJobs
            if (prefetchedJobs.isNotEmpty()) {
                Timber.d("📦 Using ${prefetchedJobs.size} prefetched jobs")
                val currentJobs = _uiState.value.jobs
                
                // PERFORMANCE FIX P1: Enforce max jobs limit
                val remainingCapacity = MAX_JOBS_IN_MEMORY - currentJobs.size
                val jobsToAdd = if (prefetchedJobs.size > remainingCapacity) {
                    Timber.w("⚠️ Truncating prefetched jobs to fit memory limit")
                    prefetchedJobs.take(remainingCapacity)
                } else {
                    prefetchedJobs
                }
                
                val updatedList = currentJobs + jobsToAdd
                val lastJob = jobsToAdd.lastOrNull()
                
                _uiState.value = _uiState.value.copy(
                    jobs = updatedList,
                    totalJobs = updatedList.size,
                    hasMore = prefetchedJobs.size >= limit && updatedList.size < MAX_JOBS_IN_MEMORY,
                    lastCreatedAt = lastJob?.postedAt,
                    prefetchedJobs = emptyList() // Clear prefetched jobs
                )
                
                // Start prefetching next batch if we have capacity
                if (updatedList.size < MAX_JOBS_IN_MEMORY) {
                    prefetchNextPage(limit)
                }
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            try {
                val lastCreatedAt = _uiState.value.lastCreatedAt
                Timber.d("🔍 Loading more jobs (limit: $limit, after: $lastCreatedAt)")
                
                firestoreJobRepository.getAllJobs(limit, lastCreatedAt).collect { result ->
                    result.fold(
                        onSuccess = { newJobs ->
                            Timber.d("✅ Successfully loaded ${newJobs.size} more jobs")
                            if (newJobs.isEmpty()) {
                                _uiState.value = _uiState.value.copy(
                                    isLoadingMore = false,
                                    hasMore = false
                                )
                            } else {
                                // Jobs already have saved status from repository
                                var processedJobs = newJobs
                                
                                // Calculate distances if user location is available
                                if (userLatitude != 0.0 || userLongitude != 0.0) {
                                    processedJobs = firestoreJobRepository.calculateJobsDistances(
                                        processedJobs, userLatitude, userLongitude
                                    )
                                }
                                
                                val currentJobs = _uiState.value.jobs
                                
                                // PERFORMANCE FIX P1: Enforce max jobs limit
                                val remainingCapacity = MAX_JOBS_IN_MEMORY - currentJobs.size
                                val jobsToAdd = if (processedJobs.size > remainingCapacity) {
                                    Timber.w("⚠️ Truncating new jobs to fit memory limit")
                                    processedJobs.take(remainingCapacity)
                                } else {
                                    processedJobs
                                }
                                
                                val updatedList = currentJobs + jobsToAdd
                                val lastJob = jobsToAdd.lastOrNull()
                                
                                _uiState.value = _uiState.value.copy(
                                    jobs = updatedList,
                                    isLoadingMore = false,
                                    totalJobs = updatedList.size,
                                    hasMore = processedJobs.size >= limit && updatedList.size < MAX_JOBS_IN_MEMORY,
                                    lastCreatedAt = lastJob?.postedAt
                                )
                                
                                // Start prefetching next batch if we have capacity
                                if (updatedList.size < MAX_JOBS_IN_MEMORY) {
                                    prefetchNextPage(limit)
                                }
                            }
                        },
                        onFailure = { exception ->
                            Timber.w("❌ Failed to load more jobs: ${exception.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoadingMore = false,
                                // Don't set global error for pagination failure, maybe show toast
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e("❌ Exception loading more jobs: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false
                )
            }
        }
    }
    
    /**
     * Prefetch next page of jobs in the background
     * This improves perceived performance by having data ready before user scrolls
     */
    private fun prefetchNextPage(limit: Long = 20L) {
        if (_uiState.value.isPrefetching || !_uiState.value.hasMore || _uiState.value.prefetchedJobs.isNotEmpty()) {
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isPrefetching = true)
            
            try {
                val lastCreatedAt = _uiState.value.lastCreatedAt
                Timber.d("🔮 Prefetching next page (limit: $limit, after: $lastCreatedAt)")
                
                firestoreJobRepository.getAllJobs(limit, lastCreatedAt).collect { result ->
                    result.fold(
                        onSuccess = { newJobs ->
                            if (newJobs.isNotEmpty()) {
                                var processedJobs = newJobs
                                
                                // Calculate distances if user location is available
                                if (userLatitude != 0.0 || userLongitude != 0.0) {
                                    processedJobs = firestoreJobRepository.calculateJobsDistances(
                                        processedJobs, userLatitude, userLongitude
                                    )
                                }
                                
                                Timber.d("🔮 Prefetched ${processedJobs.size} jobs")
                                withContext(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(
                                        isPrefetching = false,
                                        prefetchedJobs = processedJobs
                                    )
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(
                                        isPrefetching = false,
                                        hasMore = false
                                    )
                                }
                            }
                        },
                        onFailure = { exception ->
                            Timber.w("🔮 Prefetch failed: ${exception.message}")
                            withContext(Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(isPrefetching = false)
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e("🔮 Prefetch exception: ${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isPrefetching = false)
                }
            }
        }
    }
    
    fun refreshJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null, hasError = false)
            
            // PERFORMANCE FIX P2: Clear vacancy statuses on refresh to prevent memory leak
            clearVacancyStatuses()
            
            try {
                // Use refreshJobs which clears cache and fetches fresh data
                firestoreJobRepository.refreshJobs(50L).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            // PERFORMANCE FIX P1: Enforce max jobs limit
                            var jobsWithDistance = if (jobs.size > MAX_JOBS_IN_MEMORY) {
                                Timber.w("⚠️ Truncating refreshed jobs to $MAX_JOBS_IN_MEMORY")
                                jobs.take(MAX_JOBS_IN_MEMORY)
                            } else {
                                jobs
                            }
                            
                            // Calculate distances if user location is available
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                jobsWithDistance = firestoreJobRepository.calculateJobsDistances(
                                    jobsWithDistance, userLatitude, userLongitude
                                )
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                jobs = jobsWithDistance,
                                isRefreshing = false,
                                currentPage = 0,
                                totalJobs = jobsWithDistance.size,
                                hasMore = jobsWithDistance.size >= 50L && jobsWithDistance.size < MAX_JOBS_IN_MEMORY
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                hasError = true,
                                error = exception.message ?: "Failed to refresh jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    hasError = true,
                    error = e.message ?: "Failed to refresh jobs"
                )
            }
        }
    }
    
    fun searchJobs(query: String, limit: Long = 20L) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.searchJobs(query, limit).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            _uiState.value = _uiState.value.copy(
                                jobs = jobs,
                                isLoading = false,
                                totalJobs = jobs.size,
                                hasMore = false // Search results don't have pagination
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to search jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to search jobs"
                )
            }
        }
    }
    
    fun getJobsByCategory(category: String, limit: Long = 20L) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.getJobsByCategory(category, limit).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            _uiState.value = _uiState.value.copy(
                                jobs = jobs,
                                isLoading = false,
                                totalJobs = jobs.size,
                                hasMore = false // Category results don't have pagination
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs by category"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs by category"
                )
            }
        }
    }
    
    fun getJobsByLocation(location: String, limit: Long = 20L) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.getJobsByLocation(location, limit).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            _uiState.value = _uiState.value.copy(
                                jobs = jobs,
                                isLoading = false,
                                totalJobs = jobs.size,
                                hasMore = false // Location results don't have pagination
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs by location"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs by location"
                )
            }
        }
    }
    
    /**
     * Get a specific job by ID
     */
    suspend fun getJobById(jobId: String): Result<JobListing?> {
        return try {
            Timber.d("🔍 Getting job by ID: $jobId")
            var result: Result<JobListing?> = Result.failure(Exception("Job not found"))
            
            firestoreJobRepository.getJobById(jobId).collect { jobResult ->
                result = jobResult
            }
            
            result.fold(
                onSuccess = { job ->
                    Timber.d("✅ Successfully retrieved job: ${job?.title}")
                },
                onFailure = { exception ->
                    Timber.w("❌ Failed to get job by ID: ${exception.message}")
                }
            )
            
            result
        } catch (e: Exception) {
            Timber.e("❌ Exception getting job by ID: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
}
