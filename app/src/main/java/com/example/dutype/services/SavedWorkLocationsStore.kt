package com.example.dutype.services

import com.example.dutype.models.WorkLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed store for an employer's saved work locations quick-pick
 * list. The public API stays synchronous for existing Compose call sites;
 * writes update local state immediately and persist in the background.
 */
@Singleton
class SavedWorkLocationsStore @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val _locations = MutableStateFlow<List<WorkLocation>>(emptyList())
    val locations: StateFlow<List<WorkLocation>> = _locations.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerRegistration: ListenerRegistration? = null
    private var activeUserId: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            attachToUser(firebaseAuth.currentUser?.uid)
        }
        attachToUser(auth.currentUser?.uid)
    }

    fun snapshot(): List<WorkLocation> = _locations.value

    fun add(
        label: String,
        address: String,
        latitude: Double,
        longitude: Double
    ): WorkLocation {
        val cleanAddress = address.trim()
        require(cleanAddress.isNotBlank()) { "Address cannot be empty" }
        val cleanLabel = label.trim().ifBlank { cleanAddress.take(30) }
        val now = System.currentTimeMillis()
        val userId = activeUserId

        val current = _locations.value
        val existing = current.firstOrNull {
            it.address.equals(cleanAddress, ignoreCase = true)
        }

        return if (existing != null) {
            val updated = existing.copy(
                label = cleanLabel,
                latitude = latitude,
                longitude = longitude,
                usageCount = existing.usageCount + 1
            )
            _locations.value = current.map { if (it.id == existing.id) updated else it }.take(MAX_LOCATIONS)
            persist(userId, updated)
            updated
        } else {
            if (current.size >= MAX_LOCATIONS) {
                throw IllegalStateException("You can save up to $MAX_LOCATIONS work locations")
            }
            val created = WorkLocation(
                id = "loc_$now",
                label = cleanLabel,
                address = cleanAddress,
                latitude = latitude,
                longitude = longitude,
                addedAt = now,
                usageCount = 1
            )
            _locations.value = (current + created).take(MAX_LOCATIONS)
            persist(userId, created)
            created
        }
    }

    fun remove(id: String) {
        if (id.isBlank()) return
        _locations.value = _locations.value.filterNot { it.id == id }
        val userId = activeUserId ?: return
        scope.launch {
            runCatching {
                locationsCollection(userId).document(id).delete().await()
            }
        }
    }

    private fun attachToUser(userId: String?) {
        if (activeUserId == userId) return
        listenerRegistration?.remove()
        listenerRegistration = null
        activeUserId = userId
        _locations.value = emptyList()

        if (userId.isNullOrBlank()) return

        listenerRegistration = locationsCollection(userId)
            .orderBy("addedAt")
            .limit(MAX_LOCATIONS.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _locations.value = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data.orEmpty()
                    WorkLocation(
                        id = doc.id,
                        label = (data["label"] as? String).orEmpty(),
                        address = (data["address"] as? String).orEmpty(),
                        latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                        longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                        addedAt = (data["addedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        usageCount = (data["usageCount"] as? Number)?.toInt() ?: 0
                    )
                }
                seedBusinessAddressIfNeeded(userId)
            }

        seedBusinessAddressIfNeeded(userId)
    }

    private fun persist(userId: String?, location: WorkLocation) {
        if (userId.isNullOrBlank()) return
        scope.launch {
            runCatching {
                locationsCollection(userId).document(location.id).set(location.toFirestoreMap()).await()
            }
        }
    }

    private fun seedBusinessAddressIfNeeded(userId: String) {
        if (_locations.value.isNotEmpty()) return
        scope.launch {
            runCatching {
                val profile = firestore.collection("employer_profiles").document(userId).get().await().data.orEmpty()
                val address = (profile["businessAddress"] as? String)?.trim().orEmpty()
                val location = profile["businessLocation"] as? Map<*, *>
                val lat = (location?.get("lat") as? Number)?.toDouble()
                val lng = (location?.get("lng") as? Number)?.toDouble()
                if (address.isNotBlank() && lat != null && lng != null && _locations.value.isEmpty()) {
                    add(
                        label = "Main location",
                        address = address,
                        latitude = lat,
                        longitude = lng
                    )
                }
            }
        }
    }

    private fun locationsCollection(userId: String) =
        firestore.collection("employer_profiles")
            .document(userId)
            .collection("work_locations")

    private fun WorkLocation.toFirestoreMap(): Map<String, Any> = mapOf(
        "label" to label.take(80),
        "address" to address.take(500),
        "latitude" to latitude,
        "longitude" to longitude,
        "addedAt" to addedAt,
        "usageCount" to usageCount
    )

    private companion object {
        const val MAX_LOCATIONS = 5
    }
}
