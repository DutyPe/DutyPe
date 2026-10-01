package com.example.dutype.services

import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles.WorkLocations
import com.example.dutype.models.WorkLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The employer's saved hiring addresses (`employer_profiles/{uid}/work_locations`, at most 5).
 * Listens only after [start] (employer screens), so workers never pay for it. Writes update
 * local state immediately and persist in the background.
 */
@Singleton
class SavedWorkLocationsStore @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val _locations = MutableStateFlow<List<WorkLocation>>(emptyList())
    val locations: StateFlow<List<WorkLocation>> = _locations.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var registration: ListenerRegistration? = null
    private var activeUserId: String? = null

    init {
        auth.addAuthStateListener { if (it.currentUser?.uid != activeUserId) stop() }
    }

    /** Idempotent: starts the listener for the signed-in employer. */
    fun start() {
        val uid = auth.currentUser?.takeUnless { it.isAnonymous }?.uid ?: return
        if (uid == activeUserId && registration != null) return
        stop()
        activeUserId = uid
        registration = collection(uid)
            .orderBy(FieldPath.documentId())
            .limit(MAX_LOCATIONS.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _locations.value = snapshot.documents.map { doc ->
                    WorkLocation(
                        id = doc.id,
                        label = doc.getString(WorkLocations.LABEL).orEmpty(),
                        address = doc.getString(WorkLocations.ADDRESS).orEmpty(),
                        latitude = doc.getDouble(WorkLocations.LAT) ?: 0.0,
                        longitude = doc.getDouble(WorkLocations.LNG) ?: 0.0,
                        addedAt = doc.id.removePrefix("loc_").toLongOrNull() ?: 0L
                    )
                }
                if (snapshot.isEmpty && !snapshot.metadata.isFromCache) seedFromProfile(uid)
            }
    }

    private fun stop() {
        registration?.remove()
        registration = null
        activeUserId = null
        _locations.value = emptyList()
    }

    fun snapshot(): List<WorkLocation> = _locations.value

    fun add(label: String, address: String, latitude: Double, longitude: Double): WorkLocation {
        val cleanAddress = address.trim()
        require(cleanAddress.isNotBlank()) { "Address cannot be empty" }
        val cleanLabel = label.trim().ifBlank { cleanAddress.take(30) }
        val current = _locations.value
        val existing = current.firstOrNull { it.address.equals(cleanAddress, ignoreCase = true) }
        val saved = if (existing != null) {
            existing.copy(label = cleanLabel, latitude = latitude, longitude = longitude, usageCount = existing.usageCount + 1)
                .also { updated -> _locations.value = current.map { if (it.id == updated.id) updated else it } }
        } else {
            check(current.size < MAX_LOCATIONS) { "You can save up to $MAX_LOCATIONS work locations" }
            val now = System.currentTimeMillis()
            WorkLocation("loc_$now", cleanLabel, cleanAddress, latitude, longitude, now, 1)
                .also { _locations.value = current + it }
        }
        persist(saved)
        return saved
    }

    fun remove(id: String) {
        if (id.isBlank()) return
        _locations.value = _locations.value.filterNot { it.id == id }
        val uid = activeUserId ?: return
        scope.launch { runCatching { collection(uid).document(id).delete().await() } }
    }

    private fun persist(location: WorkLocation) {
        val uid = activeUserId ?: auth.currentUser?.uid ?: return
        scope.launch {
            runCatching {
                collection(uid).document(location.id).set(
                    mapOf(
                        WorkLocations.LABEL to location.label.take(80),
                        WorkLocations.ADDRESS to location.address.take(500),
                        WorkLocations.LAT to location.latitude,
                        WorkLocations.LNG to location.longitude
                    )
                ).await()
            }
        }
    }

    /** First use: offer the profile's business address as "Main location". */
    private fun seedFromProfile(uid: String) {
        scope.launch {
            runCatching {
                val profile = firestore.collection(EmployerProfiles.COLLECTION).document(uid).get().await()
                val address = profile.getString(EmployerProfiles.ADDRESS).orEmpty().trim()
                val lat = profile.getDouble(EmployerProfiles.LAT)
                val lng = profile.getDouble(EmployerProfiles.LNG)
                if (address.isNotBlank() && lat != null && lng != null && _locations.value.isEmpty()) {
                    add("Main location", address, lat, lng)
                }
            }
        }
    }

    private fun collection(uid: String) =
        firestore.collection(EmployerProfiles.COLLECTION).document(uid).collection(WorkLocations.COLLECTION)

    private companion object {
        const val MAX_LOCATIONS = 5
    }
}
