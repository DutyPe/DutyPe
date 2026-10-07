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

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale

/**
 * The employer's saved hiring addresses (`employer_profiles/{uid}/work_locations`, at most 5).
 * Listens only after [start] (employer screens), so workers never pay for it. Writes update
 * local state immediately and persist in the background. Synchronizes with local saved addresses.
 */
@Singleton
class SavedWorkLocationsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val _locations = MutableStateFlow<List<WorkLocation>>(emptyList())
    val locations: StateFlow<List<WorkLocation>> = _locations.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var registration: ListenerRegistration? = null
    private var activeUserId: String? = null
    private val prefs by lazy { context.getSharedPreferences("dutype_saved_addresses", Context.MODE_PRIVATE) }

    init {
        loadFromPreferences()
        auth.addAuthStateListener {
            val uid = it.currentUser?.takeUnless { u -> u.isAnonymous }?.uid
            if (uid != activeUserId) {
                stop()
                if (uid != null) {
                    start()
                }
            }
        }
    }

    /** Loads locally saved addresses so the screen has addresses immediately. */
    fun loadFromPreferences() {
        val localList = mutableListOf<WorkLocation>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("addr_") && value is String && value.isNotBlank()) {
                val tag = key.removePrefix("addr_")
                val area = prefs.getString("area_$tag", null)?.trim()
                val lat = prefs.getString("lat_$tag", null)?.toDoubleOrNull() ?: 17.2473
                val lng = prefs.getString("lng_$tag", null)?.toDoubleOrNull() ?: 80.1514
                val label = if (!area.isNullOrBlank()) area else tag.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                localList.add(
                    WorkLocation(
                        id = "local_$tag",
                        label = label,
                        address = value.trim(),
                        latitude = lat,
                        longitude = lng,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        // Fallback: if no custom addresses are saved yet, check LocationPreferences
        if (localList.isEmpty() && _locations.value.isEmpty()) {
            val locPrefs = context.getSharedPreferences("location_preferences", Context.MODE_PRIVATE)
            val currentAddr = locPrefs.getString("address", null)?.trim()
            val currentArea = locPrefs.getString("area", null)?.trim()
            val currentLat = locPrefs.getString("latitude", null)?.toDoubleOrNull() ?: 17.2473
            val currentLng = locPrefs.getString("longitude", null)?.toDoubleOrNull() ?: 80.1514
            if (!currentAddr.isNullOrBlank()) {
                localList.add(
                    WorkLocation(
                        id = "local_current",
                        label = if (!currentArea.isNullOrBlank()) currentArea else "Current Location",
                        address = currentAddr,
                        latitude = currentLat,
                        longitude = currentLng,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        if (localList.isNotEmpty()) {
            val current = _locations.value
            val merged = (current + localList.filter { loc ->
                current.none { it.address.equals(loc.address, ignoreCase = true) }
            }).distinctBy { it.address.lowercase(Locale.ROOT) }
            _locations.value = merged
        }
    }

    /** Idempotent: starts the listener for the signed-in employer. */
    fun start() {
        loadFromPreferences()
        val uid = auth.currentUser?.takeUnless { it.isAnonymous }?.uid ?: return
        if (uid == activeUserId && registration != null) return
        stop()
        activeUserId = uid
        registration = collection(uid)
            .orderBy(FieldPath.documentId())
            .limit(MAX_LOCATIONS.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val remote = snapshot.documents.map { doc ->
                    WorkLocation(
                        id = doc.id,
                        label = doc.getString(WorkLocations.LABEL).orEmpty(),
                        address = doc.getString(WorkLocations.ADDRESS).orEmpty(),
                        latitude = doc.getDouble(WorkLocations.LAT) ?: 0.0,
                        longitude = doc.getDouble(WorkLocations.LNG) ?: 0.0,
                        addedAt = doc.id.removePrefix("loc_").toLongOrNull() ?: 0L
                    )
                }
                // Merge remote with local preferences so addresses are never wiped
                val local = _locations.value.filter { it.id.startsWith("local_") }
                val merged = (remote + local.filter { loc ->
                    remote.none { it.address.equals(loc.address, ignoreCase = true) }
                }).distinctBy { it.address.lowercase(Locale.ROOT) }
                _locations.value = if (merged.isNotEmpty()) merged else remote
                if (snapshot.isEmpty && !snapshot.metadata.isFromCache) seedFromProfile(uid)
            }
    }

    private fun stop() {
        registration?.remove()
        registration = null
        activeUserId = null
        loadFromPreferences()
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
            val now = System.currentTimeMillis()
            WorkLocation("loc_$now", cleanLabel, cleanAddress, latitude, longitude, now, 1)
                .also { _locations.value = (current + it).takeLast(MAX_LOCATIONS) }
        }
        val tagKey = cleanLabel.lowercase(Locale.ROOT).replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        prefs.edit()
            .putString("addr_$tagKey", cleanAddress)
            .putString("area_$tagKey", cleanLabel)
            .putString("lat_$tagKey", latitude.toString())
            .putString("lng_$tagKey", longitude.toString())
            .apply()
        persist(saved)
        return saved
    }

    fun remove(id: String) {
        if (id.isBlank()) return
        val item = _locations.value.firstOrNull { it.id == id }
        _locations.value = _locations.value.filterNot { it.id == id }
        if (item != null) {
            val tagKey = item.label.lowercase(Locale.ROOT).replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
            prefs.edit()
                .remove("addr_$tagKey")
                .remove("area_$tagKey")
                .remove("lat_$tagKey")
                .remove("lng_$tagKey")
                .apply()
            // Also clean legacy tags if matching
            val legacyTags = listOf("home", "work", "parents", "shop", "other")
            val matchingLegacy = legacyTags.firstOrNull { item.label.contains(it, ignoreCase = true) }
            if (matchingLegacy != null) {
                prefs.edit().remove("addr_$matchingLegacy").remove("area_$matchingLegacy").apply()
            }
        }
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
                val employerProfile = firestore.collection(EmployerProfiles.COLLECTION).document(uid).get().await()
                var address = employerProfile.getString(EmployerProfiles.ADDRESS).orEmpty().trim()
                var area = employerProfile.getString(EmployerProfiles.AREA).orEmpty().trim()
                var lat = employerProfile.getDouble(EmployerProfiles.LAT)
                var lng = employerProfile.getDouble(EmployerProfiles.LNG)

                if (address.isBlank()) {
                    val workerProfile = firestore.collection(com.example.dutype.firestore.FirestoreSchema.WorkerProfiles.COLLECTION).document(uid).get().await()
                    if (workerProfile.exists()) {
                        address = workerProfile.getString(com.example.dutype.firestore.FirestoreSchema.WorkerProfiles.ADDRESS).orEmpty().trim()
                        area = workerProfile.getString(com.example.dutype.firestore.FirestoreSchema.WorkerProfiles.AREA).orEmpty().trim()
                        lat = workerProfile.getDouble(com.example.dutype.firestore.FirestoreSchema.WorkerProfiles.LAT)
                        lng = workerProfile.getDouble(com.example.dutype.firestore.FirestoreSchema.WorkerProfiles.LNG)
                    }
                }

                if (address.isNotBlank() && lat != null && lng != null && _locations.value.isEmpty()) {
                    add(if (area.isNotBlank()) area else "Main location", address, lat, lng)
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
