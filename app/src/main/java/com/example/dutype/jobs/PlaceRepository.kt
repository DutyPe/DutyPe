package com.example.dutype.jobs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.utils.LocaleHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * District / state for a point, and the worker's "where am I now" bookkeeping.
 *
 * The answer comes from the `resolvePlace` function and is cached on the phone per ~5 km cell
 * (geohash5), so it costs one call per new area a user visits — not per app open.
 *
 * For a signed-in worker, entering a new cell also:
 *  - records the cell and district on their worker card (employers there can find them), and
 *  - moves the phone to that cell's urgent-need push topic (`urgent_{cell}_{lang}`), so urgent
 *    needs posted nearby reach them with no database reads.
 *
 * Urgent offers follow the worker's **Online** switch (worker_profiles.available): the phone is
 * subscribed to its area's offer topic only while Online, and offers that still arrive are not
 * shown when Offline.
 */
@Singleton
class PlaceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) {
    data class Place(
        val cell: String,
        val districtId: Long?,
        val district: String,
        val stateId: Long?,
        val state: String
    )

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val memory = HashMap<String, Place>()
    private val mutex = Mutex()

    private val _online = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean(KEY_ONLINE, false))
    /** The worker's Online switch (urgent offers on / off). */
    val online: kotlinx.coroutines.flow.StateFlow<Boolean> = _online

    /**
     * The worker flips the Online switch. Saved on their profile (employers' matching reads it
     * from the card) and applied to the offer topic straight away.
     */
    suspend fun setOnline(online: Boolean): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Please log in")
        firestore.collection(com.example.dutype.firestore.FirestoreSchema.WorkerProfiles.COLLECTION).document(uid)
            .update(
                com.example.dutype.firestore.FirestoreSchema.WorkerProfiles.AVAILABLE, online,
                com.example.dutype.firestore.FirestoreSchema.WorkerProfiles.UPDATED_AT, com.google.firebase.Timestamp.now()
            ).await()
        applyOnline(online)
        runCatching {
            val fn = com.google.firebase.functions.FirebaseFunctions.getInstance("asia-south1")
            fn.getHttpsCallable("setPartnerOnline").call(mapOf("online" to online)).await()
        }
    }

    /** The profile loaded (another phone may have changed the switch): follow it without writing. */
    fun syncOnline(available: Boolean) {
        if (available != _online.value) applyOnline(available)
    }

    private fun applyOnline(online: Boolean) {
        _online.value = online
        val messaging = FirebaseMessaging.getInstance()
        val editor = prefs.edit().putBoolean(KEY_ONLINE, online)
        if (online) {
            // Join the last known area now; the next GPS fix moves it if the worker has moved.
            prefs.getString(KEY_LAST_CELL, null)?.let { cell ->
                val topic = urgentTopic(cell, LocaleHelper.getLanguage(context))
                messaging.subscribeToTopic(topic)
                editor.putString(KEY_URGENT_TOPIC, topic)
            }
        } else {
            prefs.getString(KEY_URGENT_TOPIC, null)?.let { messaging.unsubscribeFromTopic(it) }
            editor.remove(KEY_URGENT_TOPIC)
        }
        editor.remove(KEY_TRACKED).apply()
    }

    /** Only approximate location was allowed (Android 12+ choice): distances are shown as "~3 km". */
    fun isApproximate(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED

    /** District / state for a point; cached per cell. Throws when offline and not cached. */
    suspend fun placeFor(lat: Double, lng: Double): Place = resolve(lat, lng, track = false)

    /**
     * The worker's phone is at this point (feed load / refresh). Calls the server only when the
     * worker enters another ~5 km cell, changes app language, or on the first open of the day (so
     * employers' matching sees them as active); guests and employers are ignored.
     */
    suspend fun onWorkerLocation(lat: Double, lng: Double) {
        val uid = auth.currentUser?.uid ?: return
        val cell = Geohash.encode(lat, lng, CELL_PRECISION)
        val topic = urgentTopic(cell, LocaleHelper.getLanguage(context))
        // IST day number (works on Android 7, unlike java.time).
        val today = ((System.currentTimeMillis() + IST_OFFSET_MS) / DAY_MS).toString()
        val online = _online.value
        val tracked = "$uid|$topic|$today|$online"
        prefs.edit().putString(KEY_LAST_CELL, cell).apply()
        if (prefs.getString(KEY_TRACKED, null) == tracked) return
        runCatching {
            val role = auth.currentUser?.getIdToken(false)?.await()?.claims?.get("role") as? String
            if (role != Values.Role.WORKER) return
            resolve(lat, lng, track = true)
            val messaging = FirebaseMessaging.getInstance()
            val current = prefs.getString(KEY_URGENT_TOPIC, null)
            if (online) {
                current?.takeIf { it != topic }?.let { messaging.unsubscribeFromTopic(it) }
                messaging.subscribeToTopic(topic).await()
                prefs.edit().putString(KEY_URGENT_TOPIC, topic).putString(KEY_TRACKED, tracked).apply()
            } else {
                current?.let { messaging.unsubscribeFromTopic(it) }
                prefs.edit().remove(KEY_URGENT_TOPIC).putString(KEY_TRACKED, tracked).apply()
            }
        }.onFailure { Timber.w(it, "PlaceRepository: could not record the worker's area") }
    }

    private suspend fun resolve(lat: Double, lng: Double, track: Boolean): Place {
        val cell = Geohash.encode(lat, lng, CELL_PRECISION)
        if (!track) cached(cell)?.let { return it }
        @Suppress("UNCHECKED_CAST")
        val data = functions.getHttpsCallable("resolvePlace")
            .call(mapOf("lat" to lat, "lng" to lng, "track" to track))
            .await().data as? Map<String, Any?> ?: emptyMap()
        val place = Place(
            cell = cell,
            districtId = (data["districtId"] as? Number)?.toLong(),
            district = data["district"] as? String ?: "",
            stateId = (data["stateId"] as? Number)?.toLong(),
            state = data["state"] as? String ?: ""
        )
        store(place)
        return place
    }

    private suspend fun cached(cell: String): Place? = mutex.withLock {
        memory[cell] ?: prefs.getString(KEY_PLACE + cell, null)?.let { raw ->
            runCatching {
                val o = JSONObject(raw)
                Place(
                    cell = cell,
                    districtId = o.optLong("d", -1).takeIf { it >= 0 },
                    district = o.optString("dn"),
                    stateId = o.optLong("s", -1).takeIf { it >= 0 },
                    state = o.optString("sn")
                )
            }.getOrNull()?.also { memory[cell] = it }
        }
    }

    private suspend fun store(place: Place) = mutex.withLock {
        memory[place.cell] = place
        val json = JSONObject()
            .put("d", place.districtId ?: -1)
            .put("dn", place.district)
            .put("s", place.stateId ?: -1)
            .put("sn", place.state)
        val editor = prefs.edit().putString(KEY_PLACE + place.cell, json.toString())
        // Keep the cache small: people visit few cells; drop everything past the cap.
        val cachedCells = prefs.all.keys.filter { it.startsWith(KEY_PLACE) }
        if (cachedCells.size > MAX_CACHED_CELLS) cachedCells.take(cachedCells.size - MAX_CACHED_CELLS).forEach { editor.remove(it) }
        editor.apply()
    }

    companion object {
        const val PREFS = "places"
        const val KEY_URGENT_TOPIC = "urgent_topic"
        private const val KEY_TRACKED = "tracked"
        private const val KEY_ONLINE = "online"
        private const val KEY_LAST_CELL = "last_cell"
        private const val KEY_PLACE = "p_"
        private const val MAX_CACHED_CELLS = 50
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val IST_OFFSET_MS = 330 * 60 * 1000L
        const val CELL_PRECISION = 5

        /** Mirrors the server's `urgentTopic` (instant.ts). */
        fun urgentTopic(cell: String, lang: String): String = "urgent_${cell}_$lang"

        /** On sign-out: stop urgent offers for the old account's area and go Offline. */
        fun clearUrgentTopic(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.getString(KEY_URGENT_TOPIC, null)?.let { FirebaseMessaging.getInstance().unsubscribeFromTopic(it) }
            prefs.edit().remove(KEY_URGENT_TOPIC).remove(KEY_TRACKED).putBoolean(KEY_ONLINE, false).apply()
        }

        /** Read on the push path (no Firestore): show urgent offers only while Online. */
        fun isOnline(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ONLINE, false)
    }
}
