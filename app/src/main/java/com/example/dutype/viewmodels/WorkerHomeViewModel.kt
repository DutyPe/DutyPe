package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.models.toPrivacyFriendlyName
import com.example.dutype.models.toRelativeTime
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.services.JobApplicationService
import com.example.dutype.state.SavedJobsStateManager
import com.example.dutype.utils.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * P2 PERFORMANCE FIX: WorkerHomeViewModel
 * 
 * Dedicated ViewModel for WorkerHomeScreen to improve state management
 * and reduce recomposition scope.
 * 
 * Benefits:
 * - Centralized state management
 * - Computed properties as StateFlow
 * - Proper lifecycle handling
 * - Reduced recomposition
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */

data class WorkerHomeUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val unreadNotificationCount: Int = 0,
    val isLocationLoading: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val showNotificationBottomSheet: Boolean = false,
    val recentHires: List<com.example.dutype.models.RecentHire> = emptyList()  // Recently hired workers
)

@HiltViewModel
class WorkerHomeViewModel @Inject constructor(
    private val firestoreJobRepository: FirestoreJobRepository,
    private val savedJobsStateManager: SavedJobsStateManager,
    private val jobApplicationService: JobApplicationService,
    val locationService: LocationService,
    val locationPreferences: LocationPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkerHomeUiState())
    val uiState: StateFlow<WorkerHomeUiState> = _uiState.asStateFlow()
    
    // Vacancy statuses for jobs (managed state)
    private val _jobVacancyStatuses = MutableStateFlow<Map<String, JobVacancyStatus>>(emptyMap())
    val jobVacancyStatuses: StateFlow<Map<String, JobVacancyStatus>> = _jobVacancyStatuses.asStateFlow()
    
    // Track which job IDs have had vacancy status loaded
    private val loadedVacancyJobIds = mutableSetOf<String>()
    
    // User location
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    
    // User skills for job matching
    private val _userSkills = MutableStateFlow<List<String>>(emptyList())
    
    /**
     * Filtered jobs - excludes filled jobs and jobs user has applied to
     */
    val filteredJobs: StateFlow<List<JobListing>> = combine(
        _uiState,
        _jobVacancyStatuses
    ) { state, vacancyStatuses ->
        state.jobs.filter { job ->
            vacancyStatuses[job.jobId] != JobVacancyStatus.FILLED
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    /**
     * Skill-matched jobs for "Jobs For You" section
     */
    val skillMatchedJobs: StateFlow<List<JobListing>> = combine(
        filteredJobs,
        _userSkills
    ) { jobs, skills ->
        if (skills.isEmpty()) {
            // No skills set, sort by distance
            jobs.sortedBy { it.distance ?: Double.MAX_VALUE }.take(3)
        } else {
            // Score jobs based on skill match
            val scoredJobs = jobs.map { job ->
                val jobCategory = job.getCategory().uppercase()
                val skillMatch = skills.any { skill ->
                    val normalizedSkill = skill.uppercase().replace("_", " ")
                    jobCategory.contains(normalizedSkill) ||
                    normalizedSkill.contains(jobCategory) ||
                    job.title.uppercase().contains(normalizedSkill)
                }
                Pair(job, if (skillMatch) 0 else 1)
            }
            
            scoredJobs
                .sortedWith(compareBy({ it.second }, { it.first.distance ?: Double.MAX_VALUE }))
                .map { it.first }
                .take(3)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        // Listen to saved jobs state
        viewModelScope.launch {
            savedJobsStateManager.savedJobIds.collect { savedJobIds ->
                val currentJobs = _uiState.value.jobs
                if (currentJobs.isNotEmpty()) {
                    val updatedJobs = currentJobs.map { job ->
                        job.copy(isSaved = savedJobIds.contains(job.id))
                    }
                    _uiState.update { it.copy(jobs = updatedJobs) }
                }
            }
        }
    }
    
    // ==========================================
    // JOB LOADING
    // ==========================================
    
    fun loadJobsForHome() {
        if (_uiState.value.isLoading && _uiState.value.jobs.isNotEmpty()) {
            return // Already loading or loaded
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, hasError = false) }
            
            try {
                firestoreJobRepository.getAllJobsSummary(5L, null).collect { result ->
                    result.fold(
                        onSuccess = { summaries ->
                            var jobs = summaries.map { it.toJobListing() }
                            
                            // Calculate distances if location available
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                jobs = firestoreJobRepository.calculateJobsDistances(
                                    jobs, userLatitude, userLongitude
                                )
                            }
                            
                            _uiState.update { it.copy(
                                jobs = jobs,
                                isLoading = false
                            )}
                            
                            Timber.d("🏠 WorkerHomeVM: Loaded ${jobs.size} jobs for home")
                        },
                        onFailure = { exception ->
                            _uiState.update { it.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load jobs"
                            )}
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs"
                )}
            }
        }
    }
    
    fun refreshJobs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null, hasError = false) }
            
            // Clear vacancy statuses on refresh
            _jobVacancyStatuses.value = emptyMap()
            loadedVacancyJobIds.clear()
            
            try {
                firestoreJobRepository.refreshJobs(5L).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            var processedJobs = jobs
                            
                            if (userLatitude != 0.0 || userLongitude != 0.0) {
                                processedJobs = firestoreJobRepository.calculateJobsDistances(
                                    jobs, userLatitude, userLongitude
                                )
                            }
                            
                            _uiState.update { it.copy(
                                jobs = processedJobs,
                                isRefreshing = false
                            )}
                        },
                        onFailure = { exception ->
                            _uiState.update { it.copy(
                                isRefreshing = false,
                                hasError = true,
                                error = exception.message
                            )}
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isRefreshing = false,
                    hasError = true,
                    error = e.message
                )}
            }
        }
    }
    
    // ==========================================
    // RECENTLY HIRED FEED (Social Proof)
    // ==========================================
    
    /**
     * Load recently hired workers for social proof
     * Ultra-lightweight: Only fetches 5 records with minimal fields
     */
    fun loadRecentlyHired() {
        viewModelScope.launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                // Query last 5 accepted applications
                val snapshot = firestore.collection("applications")
                    .whereEqualTo("status", "ACCEPTED")
                    .orderBy("acceptedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()
                
                val recentHires = snapshot.documents.mapNotNull { doc ->
                    try {
                        val workerName = doc.getString("workerName") ?: return@mapNotNull null
                        val jobTitle = doc.getString("jobTitle") ?: return@mapNotNull null
                        val acceptedAt = doc.getLong("acceptedAt") ?: return@mapNotNull null
                        
                        com.example.dutype.models.RecentHire(
                            workerName = workerName.toPrivacyFriendlyName(),
                            jobTitle = jobTitle,
                            timeAgo = acceptedAt.toRelativeTime(),
                            acceptedAt = acceptedAt
                        )
                    } catch (e: Exception) {
                        Timber.w("Failed to parse recent hire: ${e.message}")
                        null
                    }
                }
                
                _uiState.update { it.copy(recentHires = recentHires) }
                Timber.d("🔥 Loaded ${recentHires.size} recent hires")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load recent hires")
                // Don't show error - this is optional feature
            }
        }
    }
    
    // ==========================================
    // VACANCY STATUS
    // ==========================================
    
    fun getUnloadedVacancyJobIds(jobIds: List<String>): List<String> {
        return jobIds.filter { !loadedVacancyJobIds.contains(it) }
    }
    
    fun markVacancyJobIdsAsLoaded(jobIds: List<String>) {
        loadedVacancyJobIds.addAll(jobIds)
    }
    
    fun updateVacancyStatuses(statuses: Map<String, JobVacancyStatus>) {
        _jobVacancyStatuses.update { current ->
            current + statuses
        }
    }
    
    fun loadVacancyStatuses(jobIds: List<String>) {
        val unloadedIds = getUnloadedVacancyJobIds(jobIds)
        if (unloadedIds.isEmpty()) return
        
        markVacancyJobIdsAsLoaded(unloadedIds)
        
        viewModelScope.launch {
            try {
                jobApplicationService.getJobVacancyStatusBatch(unloadedIds)
                    .onSuccess { statusMap ->
                        updateVacancyStatuses(statusMap)
                        Timber.d("🏠 WorkerHomeVM: Loaded ${statusMap.size} vacancy statuses")
                    }
                    .onFailure { e ->
                        Timber.w("🏠 WorkerHomeVM: Failed to load vacancy statuses: ${e.message}")
                    }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Timber.w("🏠 WorkerHomeVM: Error loading vacancy statuses: ${e.message}")
                }
            }
        }
    }
    
    // ==========================================
    // LOCATION
    // ==========================================
    
    fun setUserLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
        
        // Recalculate distances for existing jobs
        if (_uiState.value.jobs.isNotEmpty()) {
            viewModelScope.launch {
                val jobsWithDistance = withContext(Dispatchers.Default) {
                    firestoreJobRepository.calculateJobsDistances(
                        _uiState.value.jobs, latitude, longitude
                    )
                }
                _uiState.update { it.copy(jobs = jobsWithDistance) }
            }
        }
    }
    
    fun setLocationLoading(loading: Boolean) {
        _uiState.update { it.copy(isLocationLoading = loading) }
    }
    
    fun setLocationPermission(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
    }
    
    // ==========================================
    // NOTIFICATIONS
    // ==========================================
    
    fun setNotificationPermission(granted: Boolean) {
        _uiState.update { it.copy(hasNotificationPermission = granted) }
    }
    
    fun setUnreadNotificationCount(count: Int) {
        _uiState.update { it.copy(unreadNotificationCount = count) }
    }
    
    fun showNotificationBottomSheet() {
        _uiState.update { it.copy(showNotificationBottomSheet = true) }
    }
    
    fun dismissNotificationBottomSheet() {
        _uiState.update { it.copy(showNotificationBottomSheet = false) }
    }
    
    // ==========================================
    // USER SKILLS
    // ==========================================
    
    fun setUserSkills(skills: List<String>) {
        _userSkills.value = skills
    }
    
    // ==========================================
    // ERROR HANDLING
    // ==========================================
    
    fun clearError() {
        _uiState.update { it.copy(error = null, hasError = false) }
    }
}
