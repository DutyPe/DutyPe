package com.example.dutype.services

import com.example.dutype.models.WorkLocation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory, process-scoped store for an employer's "saved work locations"
 * quick-pick list. Replaces the previous `WorkLocationManager` Firestore
 * stub that silently returned empty results because the underlying
 * `work_locations` collection was decommissioned.
 *
 * Persistence is intentionally session-only — saved entries live for the
 * lifetime of the process. If durable persistence is needed later, swap
 * the backing `MutableStateFlow` for a DataStore-backed source without
 * changing call sites.
 */
@Singleton
class SavedWorkLocationsStore @Inject constructor() {

    private val _locations = MutableStateFlow<List<WorkLocation>>(emptyList())
    val locations: StateFlow<List<WorkLocation>> = _locations.asStateFlow()

    fun snapshot(): List<WorkLocation> = _locations.value

    /**
     * Adds a new location, or bumps `usageCount` on an existing entry that
     * matches by address (case-insensitive). Returns the resulting entry.
     */
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
            _locations.value = current.map { if (it.id == existing.id) updated else it }
            updated
        } else {
            val created = WorkLocation(
                id = "loc_$now",
                label = cleanLabel,
                address = cleanAddress,
                latitude = latitude,
                longitude = longitude,
                addedAt = now,
                usageCount = 1
            )
            _locations.value = current + created
            created
        }
    }

    fun remove(id: String) {
        if (id.isBlank()) return
        _locations.value = _locations.value.filterNot { it.id == id }
    }
}
