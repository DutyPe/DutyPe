package com.example.dutype.profile

import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.example.dutype.firestore.FirestoreSchema.WorkerProfiles
import com.example.dutype.models.EmployerSubscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in user's own profile, live, shared by every screen through ONE document listener
 * (subscription credits, name and photo update everywhere the moment they change).
 */
@Singleton
class CurrentProfileStore @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val _worker = MutableStateFlow<WorkerProfile?>(null)
    val worker: StateFlow<WorkerProfile?> = _worker.asStateFlow()

    private val _employer = MutableStateFlow<EmployerProfile?>(null)
    val employer: StateFlow<EmployerProfile?> = _employer.asStateFlow()

    private val _subscription = MutableStateFlow(EmployerSubscription())
    val subscription: StateFlow<EmployerSubscription> = _subscription.asStateFlow()

    private var registration: ListenerRegistration? = null
    private var key: String? = null

    init {
        auth.addAuthStateListener { if (it.currentUser?.uid == null) stop() }
    }

    /** Idempotent: listens to worker_profiles/{uid} or employer_profiles/{uid} for [role]. */
    fun start(role: String) {
        val uid = auth.currentUser?.takeUnless { it.isAnonymous }?.uid ?: return
        val isEmployer = role.equals("EMPLOYER", ignoreCase = true)
        val newKey = "$uid|$isEmployer"
        if (newKey == key && registration != null) return
        stop()
        key = newKey
        val collection = if (isEmployer) EmployerProfiles.COLLECTION else WorkerProfiles.COLLECTION
        registration = firestore.collection(collection).document(uid).addSnapshotListener { snap, error ->
            if (error != null) {
                Timber.w(error, "own profile listener failed")
                return@addSnapshotListener
            }
            val data = snap?.data ?: return@addSnapshotListener
            if (isEmployer) {
                val profile = EmployerProfile.from(uid, data)
                _employer.value = profile
                _subscription.value = profile.subscription
            } else {
                _worker.value = WorkerProfile.from(uid, data)
            }
        }
    }

    fun stop() {
        registration?.remove()
        registration = null
        key = null
        _worker.value = null
        _employer.value = null
        _subscription.value = EmployerSubscription()
    }
}
