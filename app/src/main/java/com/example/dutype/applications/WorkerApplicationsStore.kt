package com.example.dutype.applications

import com.example.dutype.models.JobApplication
import com.example.dutype.state.ApplicationStateManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in worker's applications, shared by Home, Find Jobs and My Jobs through ONE live
 * listener per session (the first read loads the list, later reads are only the changed
 * documents). Keeps [ApplicationStateManager] — applied ids and statuses — in sync.
 */
@Singleton
class WorkerApplicationsStore @Inject constructor(
    private val repository: ApplicationRepository,
    private val applicationStateManager: ApplicationStateManager,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var listeningUid: String? = null

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Signing out (or switching account) ends the previous user's listener.
        auth.addAuthStateListener { current ->
            if (listeningUid != null && current.currentUser?.uid != listeningUid) stop()
        }
    }

    /** Idempotent; restarts only when the signed-in worker changed. */
    fun start() {
        val uid = auth.currentUser?.takeUnless { it.isAnonymous }?.uid ?: return
        if (uid == listeningUid && job?.isActive == true) return
        stop()
        listeningUid = uid
        _isLoading.value = _applications.value.isEmpty()
        job = scope.launch {
            repository.workerApplications(uid).collect { result ->
                result.onSuccess { list ->
                    _applications.value = list
                    applicationStateManager.updateApplications(list)
                }.onFailure { Timber.w(it, "Worker applications listener failed") }
                _isLoading.value = false
            }
        }
    }

    /** Sign-out: drop the listener and the cached list. */
    fun stop() {
        job?.cancel()
        job = null
        listeningUid = null
        _applications.value = emptyList()
    }
}
