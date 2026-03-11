package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.ApplicationAnalytics
import com.example.dutype.services.JobApplicationService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Enterprise-level Employer Application Management ViewModel
 * Handles all application-related operations for employers
 * 
 * SCALABILITY: Worker profile data is fetched dynamically when viewing application details
 * instead of storing redundant data in each application document.
 * 
 * PERFORMANCE FIX P0: LRU cache with bounded size for worker profiles
 */
@HiltViewModel
class EmployerApplicationViewModel @Inject constructor(
    private val jobApplicationService: JobApplicationService,
    private val profileCompletionService: com.example.dutype.services.ProfileCompletionService,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker
) : ViewModel() {
    
    companion object {
        // P0 FIX: Maximum worker profiles to cache (prevents unbounded memory growth)
        private const val MAX_WORKER_PROFILE_CACHE_SIZE = 100
    }
    
    private val _uiState = MutableStateFlow(EmployerApplicationUiState())
    val uiState: StateFlow<EmployerApplicationUiState> = _uiState.asStateFlow()
    
    private val _stats = MutableStateFlow(ApplicationStats())
    val stats: StateFlow<ApplicationStats> = _stats.asStateFlow()
    
    private val _analytics = MutableStateFlow(ApplicationAnalytics())
    val analytics: StateFlow<ApplicationAnalytics> = _analytics.asStateFlow()
    
    // P0 FIX: LRU cache for worker profiles with bounded size
    // Uses LinkedHashMap with accessOrder=true for LRU eviction
    private val workerProfileCache = object : LinkedHashMap<String, Map<String, Any?>>(
        MAX_WORKER_PROFILE_CACHE_SIZE, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Map<String, Any?>>?): Boolean {
            val shouldRemove = size > MAX_WORKER_PROFILE_CACHE_SIZE
            if (shouldRemove) {
                Timber.d("[EmployerVM] 🧹 LRU evicting oldest worker profile from cache (size: $size)")
            }
            return shouldRemove
        }
    }
    
    private val auth = FirebaseAuth.getInstance()
    
    // Note: Don't load applications in init - let the screen decide what to load
    // based on whether it's viewing all applications or job-specific applications
    
    /**
     * Load all applications for current employer
     * SCALABILITY: Enriches applications with worker profile data dynamically
     */
    fun loadEmployerApplications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                hasError = true,
                error = "Employer not authenticated"
            )
            return
        }
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            com.example.dutype.performance.MainThreadChecker.assertMainThread("EmployerApplicationViewModel.loadEmployerApplications")
            
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            
            try {
                Timber.d("[EmployerVM] Loading employer applications for ${currentUser.uid}")
                
                jobApplicationService.getEmployerApplications(currentUser.uid).collect { result ->
                    result.fold(
                        onSuccess = { applications ->
                            val duration = System.currentTimeMillis() - startTime
                            performanceTracker.trackApiCall("load_employer_applications", duration, success = true)
                            
                            Timber.d("[EmployerVM] Loaded ${applications.size} applications for employer in ${duration}ms")
                            
                            // P1 FIX: Batch worker profile enrichment in parallel (was sequential N+1)
                            val enrichedApplications = coroutineScope {
                                applications.map { app ->
                                    async { enrichApplicationWithWorkerProfile(app) }
                                }.awaitAll()
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                applications = enrichedApplications,
                                allApplications = enrichedApplications,
                                isLoading = false,
                                hasError = false,
                                error = null
                            )
                            
                            // Update statistics and analytics
                            updateApplicationStats(enrichedApplications)
                            loadApplicationAnalytics()
                        },
                        onFailure = { error ->
                            val duration = System.currentTimeMillis() - startTime
                            performanceTracker.trackApiCall("load_employer_applications", duration, success = false)
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = error.message ?: "Failed to load applications"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                performanceTracker.trackApiCall("load_employer_applications", duration, success = false)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Load applications for a specific job
     * SCALABILITY: Enriches applications with worker profile data dynamically
     */
    fun loadJobApplications(jobId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            
            try {
                Timber.d("[EmployerApplicationViewModel] Loading job applications for jobId=$jobId")
                jobApplicationService.getJobApplications(jobId).collect { result ->
                    result.fold(
                        onSuccess = { applications ->
                            Timber.d("[EmployerApplicationViewModel] Successfully loaded ${applications.size} applications for job $jobId")
                            
                            // P1 FIX: Batch worker profile enrichment in parallel
                            val enrichedApplications = coroutineScope {
                                applications.map { app ->
                                    async { enrichApplicationWithWorkerProfile(app) }
                                }.awaitAll()
                            }
                            
                            _uiState.value = _uiState.value.copy(
                                applications = enrichedApplications,
                                allApplications = enrichedApplications,
                                isLoading = false,
                                hasError = false,
                                error = null
                            )
                        },
                        onFailure = { error ->
                            Timber.e("[EmployerApplicationViewModel] Failed to load job applications for $jobId: ${error.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = error.message ?: "Failed to load job applications"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e("[EmployerApplicationViewModel] Exception loading job applications for $jobId: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Load a single application by ID
     */
    fun loadApplicationById(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)
            
            try {
                Timber.d("[EmployerApplicationViewModel] Loading application by ID: $applicationId")
                val result = jobApplicationService.getApplicationById(applicationId)
                
                result.fold(
                    onSuccess = { application ->
                        if (application != null) {
                            Timber.d("[EmployerApplicationViewModel] Successfully loaded application: ${application.id}")
                            
                            // SCALABILITY: Enrich application with worker profile data
                            val enrichedApplication = enrichApplicationWithWorkerProfile(application)
                            
                            // Add to applications list if not already present
                            val currentApps = _uiState.value.applications.toMutableList()
                            val existingIndex = currentApps.indexOfFirst { it.applicationId == applicationId }
                            if (existingIndex >= 0) {
                                currentApps[existingIndex] = enrichedApplication
                            } else {
                                currentApps.add(enrichedApplication)
                            }
                            _uiState.value = _uiState.value.copy(
                                applications = currentApps,
                                isLoading = false,
                                hasError = false,
                                error = null
                            )
                        } else {
                            Timber.w("[EmployerApplicationViewModel] Application not found: $applicationId")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = "Application not found"
                            )
                        }
                    },
                    onFailure = { error ->
                        Timber.e("[EmployerApplicationViewModel] Failed to load application $applicationId: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = error.message ?: "Failed to load application"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e("[EmployerApplicationViewModel] Exception loading application $applicationId: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * SCALABILITY: Enrich application with worker profile data
     * Fetches worker profile dynamically instead of storing redundant data
     * Uses caching to avoid repeated fetches for the same worker
     */
    private suspend fun enrichApplicationWithWorkerProfile(application: JobApplication): JobApplication {
        val workerId = application.workerId
        if (workerId.isBlank()) return application
        
        // Check cache first
        val cachedProfile = workerProfileCache[workerId]
        if (cachedProfile != null) {
            return applyWorkerProfileToApplication(application, cachedProfile)
        }
        
        // Fetch worker profile
        return try {
            val profileResult = profileCompletionService.getUserProfile(workerId)
            profileResult.fold(
                onSuccess = { profile ->
                    // Cache the profile
                    workerProfileCache[workerId] = profile
                    Timber.d("[EmployerVM] Enriched application with worker profile for workerId=$workerId")
                    applyWorkerProfileToApplication(application, profile)
                },
                onFailure = { error ->
                    Timber.w("[EmployerVM] Failed to fetch worker profile for $workerId: ${error.message}")
                    application // Return original application if profile fetch fails
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "[EmployerVM] Error enriching application with worker profile")
            application
        }
    }
    
    /**
     * Apply worker profile data to application object
     */
    private fun applyWorkerProfileToApplication(
        application: JobApplication,
        profile: Map<String, Any?>
    ): JobApplication {
        val workerName = profile["fullName"] as? String 
            ?: profile["name"] as? String 
            ?: profile["displayName"] as? String 
            ?: application.workerName
        
        val workerPhone = profile["phone"] as? String 
            ?: profile["phoneNumber"] as? String 
            ?: application.workerPhone
        
        val workerEmail = profile["email"] as? String ?: application.workerEmail
        val workerProfileImageUrl = profile["profileImageUrl"] as? String ?: application.workerProfileImageUrl
        val workerLocation = profile["address"] as? String 
            ?: profile["location"] as? String 
            ?: application.workerLocation
        val workerGender = profile["gender"] as? String ?: application.workerGender
        val workerDateOfBirth = profile["dateOfBirth"] as? String ?: application.workerDateOfBirth
        
        // Skills
        val skills = when (val skillsData = profile["skills"]) {
            is List<*> -> skillsData.filterIsInstance<String>()
            is String -> skillsData.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> application.skills
        }
        
        val skillsText = when (val skillsData = profile["skills"]) {
            is String -> skillsData
            is List<*> -> skillsData.filterIsInstance<String>().joinToString(", ")
            else -> application.skillsText
        }
        
        // Experience
        val workExperienceText = when (val expData = profile["experience"]) {
            is String -> expData
            else -> application.workExperienceText
        }
        
        val expectedSalary = profile["expectedSalary"] as? String ?: application.expectedSalary
        val availability = profile["availability"] as? String ?: application.availability
        
        return application.copy(
            workerName = workerName,
            workerPhone = workerPhone,
            workerEmail = workerEmail,
            workerProfileImageUrl = workerProfileImageUrl,
            workerLocation = workerLocation,
            workerGender = workerGender,
            workerDateOfBirth = workerDateOfBirth,
            skills = skills,
            skillsText = skillsText,
            workExperienceText = workExperienceText,
            expectedSalary = expectedSalary,
            availability = availability
        )
    }
    
    /**
     * Update application status with enterprise features
     */
    fun updateApplicationStatus(
        applicationId: String,
        newStatus: ApplicationStatus,
        notes: String? = null
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                hasError = true,
                error = "Employer not authenticated"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)
            
            try {
                val result = jobApplicationService.updateApplicationStatus(
                    applicationId = applicationId,
                    newStatus = newStatus,
                    updatedBy = currentUser.uid,
                    notes = notes
                )
                
                result.fold(
                    onSuccess = { updatedApplication ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            hasError = false,
                            error = null
                        )
                        
                        // Refresh applications to show updated status
                        loadEmployerApplications()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            hasError = true,
                            error = error.message ?: "Failed to update application status"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    hasError = true,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Mark application as viewed (for analytics)
     */
    fun markApplicationAsViewed(applicationId: String) {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                jobApplicationService.markApplicationAsViewed(applicationId, currentUser.uid)
            } catch (e: Exception) {
                // Silent fail for analytics
                Timber.w("Failed to mark application as viewed: ${e.message}")
            }
        }
    }
    
    /**
     * Mark application as under review when employer opens it
     */
    fun markApplicationAsUnderReview(applicationId: String) {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                jobApplicationService.markApplicationAsUnderReview(applicationId, currentUser.uid)
                Timber.d("Application $applicationId marked as under review")
            } catch (e: Exception) {
                Timber.w("Failed to mark application as under review: ${e.message}")
            }
        }
    }
    
    /**
     * Load application analytics
     */
    private fun loadApplicationAnalytics() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                val result = jobApplicationService.getApplicationAnalytics(currentUser.uid)
                result.fold(
                    onSuccess = { analytics ->
                        _analytics.value = analytics
                    },
                    onFailure = { error ->
                        Timber.e("Failed to load analytics: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Timber.e("Failed to load analytics: ${e.message}")
            }
        }
    }
    
    /**
     * Update application statistics
     */
    private fun updateApplicationStats(applications: List<JobApplication>) {
        val stats = ApplicationStats(
            totalApplications = applications.size,
            pendingApplications = applications.count { it.status == ApplicationStatus.PENDING },
            reviewedApplications = applications.count { it.status == ApplicationStatus.UNDER_REVIEW },
            shortlistedApplications = applications.count { it.status == ApplicationStatus.ACCEPTED },
            rejectedApplications = applications.count { it.status == ApplicationStatus.REJECTED },
            hiredApplications = applications.count { it.status == ApplicationStatus.ACCEPTED },
            recentApplications = applications.take(5)
        )
        
        _stats.value = stats
    }
    
    /**
     * Filter applications by status
     */
    fun filterApplicationsByStatus(status: ApplicationStatus?) {
        val currentApplications = _uiState.value.allApplications
        val filteredApplications = if (status != null) {
            currentApplications.filter { it.status == status }
        } else {
            currentApplications
        }
        
        _uiState.value = _uiState.value.copy(
            applications = filteredApplications,
            selectedStatusFilter = status
        )
    }
    
    /**
     * Search applications
     */
    fun searchApplications(query: String) {
        val currentApplications = _uiState.value.allApplications
        val filteredApplications = if (query.isBlank()) {
            currentApplications
        } else {
            currentApplications.filter { application ->
                application.workerName.contains(query, ignoreCase = true) ||
                application.jobTitle.contains(query, ignoreCase = true) ||
                application.companyName.contains(query, ignoreCase = true)
            }
        }
        
        _uiState.value = _uiState.value.copy(
            applications = filteredApplications,
            searchQuery = query
        )
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(hasError = false, error = null)
    }
    
    /**
     * Hire an applicant (accept application)
     * Checks vacancy limit before accepting
     */
    fun hireApplicant(
        applicationId: String,
        jobId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError("Employer not authenticated")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)
            
            try {
                // Use the acceptApplication method which checks vacancy limits
                val result = jobApplicationService.acceptApplication(applicationId, currentUser.uid)
                
                result.fold(
                    onSuccess = { updatedApplication ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            hasError = false,
                            error = null
                        )
                        
                        // Refresh applications to show updated status
                        loadJobApplications(jobId)
                        onSuccess()
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            hasError = true,
                            error = error.message ?: "Failed to hire applicant"
                        )
                        onError(error.message ?: "Failed to hire applicant")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    hasError = true,
                    error = e.message ?: "Unknown error occurred"
                )
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Check if more applicants can be hired for a job
     */
    fun canHireMoreApplicants(jobId: String, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val canAcceptResult = jobApplicationService.canAcceptMoreApplications(jobId)
                val remainingResult = jobApplicationService.getRemainingVacancies(jobId)
                
                val canAccept = canAcceptResult.getOrNull() ?: false
                val remaining = remainingResult.getOrNull() ?: 0
                
                onResult(canAccept, remaining)
            } catch (e: Exception) {
                Timber.e(e, "Error checking vacancy availability")
                onResult(false, 0)
            }
        }
    }
    
    /**
     * Refresh all data
     */
    fun refresh() {
        loadEmployerApplications()
    }
    
    // ==================== FINTECH: CONTACT UNLOCK ====================
    // First 3 applicants free, 4th+ requires payment
    
    /**
     * Check if contact is unlocked for an application
     * First 3 applications are automatically unlocked (free)
     */
    fun isContactUnlocked(applicationId: String, applicationIndex: Int): Boolean {
        // First 3 applications are free
        if (applicationIndex < 3) return true
        // Check if manually unlocked
        return _uiState.value.unlockedContacts.contains(applicationId)
    }
    
    /**
     * Unlock contact for an application (simulated payment)
     * In production, this would integrate with Razorpay/Stripe
     */
    fun unlockContact(applicationId: String, onSuccess: () -> Unit, onPaymentRequired: () -> Unit) {
        val currentUnlocked = _uiState.value.unlockedContacts
        val freeRemaining = _uiState.value.freeContactsRemaining
        
        // Check if already unlocked
        if (currentUnlocked.contains(applicationId)) {
            onSuccess()
            return
        }
        
        // Check if free unlocks remaining
        if (freeRemaining > 0) {
            // Use free unlock
            _uiState.update { state ->
                state.copy(
                    unlockedContacts = state.unlockedContacts + applicationId,
                    freeContactsRemaining = state.freeContactsRemaining - 1
                )
            }
            Timber.d("💰 CONTACT UNLOCK: Free unlock used. Remaining: ${freeRemaining - 1}")
            onSuccess()
        } else {
            // Requires payment
            Timber.d("💰 CONTACT UNLOCK: Payment required for applicationId=$applicationId")
            onPaymentRequired()
        }
    }
    
    /**
     * Process payment and unlock contact
     * In production, this would be called after successful Razorpay payment
     */
    fun processContactUnlockPayment(applicationId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Timber.d("💰 CONTACT UNLOCK: Processing payment for applicationId=$applicationId")
                
                // Simulate payment processing delay
                kotlinx.coroutines.delay(500)
                
                // For MVP: Allow contact unlock without actual payment gateway integration
                val success = true
                
                if (success) {
                    // Add to unlocked contacts
                    _uiState.update { state ->
                        state.copy(
                            unlockedContacts = state.unlockedContacts + applicationId
                        )
                    }
                    Timber.d("💰 CONTACT UNLOCK: Payment successful, contact unlocked")
                    onSuccess()
                } else {
                    onFailure("Payment failed. Please try again.")
                }
            } catch (e: Exception) {
                Timber.e(e, "💰 CONTACT UNLOCK: Payment failed")
                onFailure(e.message ?: "Payment failed")
            }
        }
    }
    
    /**
     * Get the unlock price for contacts
     */
    fun getContactUnlockPrice(): Int = 29 // ₹29 per contact unlock
    
    /**
     * P0 FIX: Clear cache on ViewModel destruction to prevent memory leaks
     */
    override fun onCleared() {
        super.onCleared()
        workerProfileCache.clear()
        Timber.d("[EmployerVM] 🧹 onCleared: Worker profile cache cleared")
    }
}

/**
 * UI State for Employer Application Management
 */
data class EmployerApplicationUiState(
    val applications: List<JobApplication> = emptyList(),
    val allApplications: List<JobApplication> = emptyList(), // Unfiltered list
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedStatusFilter: ApplicationStatus? = null,
    // FINTECH: Contact Unlock - first 3 free, 4th+ requires payment
    val unlockedContacts: Set<String> = emptySet(), // Set of applicationIds with unlocked contacts
    val freeContactsRemaining: Int = 3 // Employer gets 3 free contact unlocks per job
)
