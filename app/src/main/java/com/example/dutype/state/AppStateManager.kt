package com.example.dutype.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared in-memory session state that outlives individual ViewModels.
 *
 * Scope is intentionally narrow:
 *   - saved job IDs (shared between SavedJobsViewModel and job-list VMs so a
 *     save on one screen is visible on another without re-hitting Firestore).
 *   - applied job IDs, delegated to [ApplicationStateManager].
 *   - logout-time cleanup.
 *
 * Do NOT add user session fields (uid, role, isLoggedIn) here — AuthManager
 * already owns those. Do NOT add profile-completion fields — that lives in
 * [ProfileSetupStateManager] (DataStore-backed).
 */
@Singleton
class AppStateManager @Inject constructor(
    private val applicationStateManager: ApplicationStateManager,
    private val profileSetupStateManager: ProfileSetupStateManager
) {

    // ==================== SAVED JOBS (shared across VMs) ====================

    private val _savedJobIds = MutableStateFlow<Set<String>>(emptySet())
    val savedJobIds: StateFlow<Set<String>> = _savedJobIds.asStateFlow()

    /** Applied job IDs — authoritative store is [ApplicationStateManager]. */
    val appliedJobIds: StateFlow<Set<String>> = applicationStateManager.appliedJobIds

    // ==================== SAVED JOBS MUTATORS ====================

    fun saveJob(jobId: String) {
        _savedJobIds.value = _savedJobIds.value + jobId
    }

    fun unsaveJob(jobId: String) {
        _savedJobIds.value = _savedJobIds.value - jobId
    }

    fun isJobSaved(jobId: String): Boolean = _savedJobIds.value.contains(jobId)

    fun setSavedJobIds(jobIds: Set<String>) {
        _savedJobIds.value = jobIds
    }

    /** Prime applied-job state from lightweight jobId lookups. */
    fun setAppliedJobIds(jobIds: Set<String>) {
        applicationStateManager.setAppliedJobs(jobIds)
    }

    // ==================== SESSION CLEANUP ====================

    /** Wipe all session-scoped state. Called from AuthManager on logout. */
    suspend fun clearSession() {
        Timber.d("AppStateManager: Clearing session")
        _savedJobIds.value = emptySet()
        applicationStateManager.clearAll()
        profileSetupStateManager.resetProfileSetupState()
    }
}