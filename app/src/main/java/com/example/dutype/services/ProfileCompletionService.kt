package com.example.dutype.services

import com.google.firebase.firestore.AggregateSource
import com.example.dutype.firestore.FirestoreSchema.Applications
import com.example.dutype.firestore.FirestoreSchema.Jobs
import android.content.Context
import android.net.Uri
import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.firestore.FirestoreSchema.WorkerProfiles
import com.example.dutype.jobs.Geohash
import com.example.dutype.profile.EmployerProfile
import com.example.dutype.profile.WorkerProfile
import com.example.dutype.utils.AreaText
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.ImageUploadUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class ProfileCompletionStatus(
    val completionPercentage: Int,
    val isCompleted: Boolean,
    val missingFields: List<String>
)

/**
 * The signed-in user's own profile (`worker_profiles/{uid}` or `employer_profiles/{uid}`):
 * typed reads, field-level saves with schema keys, photo upload and completion rules.
 * Profiles are created by the server at registration; the app only edits its own fields
 * (phone and the server fields — subscription, rating, blocked… — are not client-writable).
 */
@Singleton
class ProfileCompletionService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    @ApplicationContext private val context: Context
) {
    data class PreJobPostCheckResult(
        val canPost: Boolean,
        val isProfileComplete: Boolean,
        val completionPercentage: Int,
        val missingFields: List<String>,
        val errorMessage: String? = null
    )

    private fun workerRef(uid: String) = firestore.collection(WorkerProfiles.COLLECTION).document(uid)
    private fun employerRef(uid: String) = firestore.collection(EmployerProfiles.COLLECTION).document(uid)

    // ─────────────────────────────── reads ───────────────────────────────

    suspend fun getWorker(uid: String): Result<WorkerProfile?> = runCatching {
        readWithCacheFallback(workerRef(uid)).data?.let { WorkerProfile.from(uid, it) }
    }

    suspend fun getEmployer(uid: String): Result<EmployerProfile?> = runCatching {
        readWithCacheFallback(employerRef(uid)).data?.let { EmployerProfile.from(uid, it) }
    }

    /** Completion % for the role's profile; null when unknown (offline with nothing cached). */
    suspend fun completion(uid: String, role: String): Int? = if (role == Values.Role.EMPLOYER) {
        getEmployer(uid).fold({ it?.completionPercent ?: 0 }, { null })
    } else {
        getWorker(uid).fold({ it?.completionPercent ?: 0 }, { null })
    }

    suspend fun getProfileCompletionStatus(uid: String, role: String): ProfileCompletionStatus {
        val (percent, missing) = if (role == Values.Role.EMPLOYER) {
            getEmployer(uid).getOrNull().let { (it?.completionPercent ?: 0) to (it?.missingFields ?: listOf("Profile")) }
        } else {
            getWorker(uid).getOrNull().let { (it?.completionPercent ?: 0) to (it?.missingFields ?: listOf("Profile")) }
        }
        return ProfileCompletionStatus(percent, percent >= 100, missing)
    }

    /** Workers can apply once name, phone and a skill are set. */
    suspend fun canApplyDirectly(uid: String): Result<Boolean> = getWorker(uid).map { it?.isComplete == true }

    suspend fun isProfileComplete(uid: String, role: String): Result<Boolean> = if (role == Values.Role.EMPLOYER) {
        getEmployer(uid).map { it?.isComplete == true }
    } else {
        getWorker(uid).map { it?.isComplete == true }
    }

    suspend fun preJobPostCheck(uid: String): PreJobPostCheckResult {
        val employer = getEmployer(uid).getOrElse {
            return PreJobPostCheckResult(false, false, 0, emptyList(), it.message ?: "Could not check your profile")
        } ?: return PreJobPostCheckResult(false, false, 0, listOf("Profile"), "Complete your employer profile first")
        return PreJobPostCheckResult(
            canPost = employer.isComplete,
            isProfileComplete = employer.isComplete,
            completionPercentage = employer.completionPercent,
            missingFields = employer.missingFields,
            errorMessage = if (employer.isComplete) null else "Complete your profile to post jobs"
        )
    }

    /** The worker's public card (rating, jobs done), one read. */
    suspend fun workerCard(uid: String): com.example.dutype.models.WorkerCard? = runCatching {
        firestore.collection(com.example.dutype.firestore.FirestoreSchema.WorkerCards.COLLECTION).document(uid)
            .get().await().data?.let { com.example.dutype.models.WorkerCard.from(uid, it) }
    }.getOrNull()

    /** Open jobs and applicants waiting for a decision: two count aggregations (≈2 reads). */
    suspend fun employerCounts(uid: String): Pair<Int, Int> = runCatching {
        val openJobs = firestore.collection(Jobs.COLLECTION)
            .whereEqualTo(Jobs.EMPLOYER_ID, uid)
            .whereEqualTo(Jobs.STATUS, Values.JobStatus.OPEN)
            .count().get(AggregateSource.SERVER).await().count.toInt()
        val waiting = firestore.collection(Applications.COLLECTION)
            .whereEqualTo(Applications.EMPLOYER_ID, uid)
            .whereEqualTo(Applications.STATUS, Values.ApplicationStatus.APPLIED)
            .count().get(AggregateSource.SERVER).await().count.toInt()
        openJobs to waiting
    }.getOrElse {
        Timber.w(it, "employer counts failed")
        0 to 0
    }

    // ─────────────────────────────── writes ───────────────────────────────

    /**
     * Saves worker fields (keys from [WorkerProfiles]). An address sets the short `area`; lat/lng
     * set the `geohash`. Phone and server fields are ignored.
     */
    suspend fun saveWorker(fields: Map<String, Any?>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val update = fields.filterKeys { it in WORKER_EDITABLE }.toMutableMap()
        (update[WorkerProfiles.NAME] as? String)?.let { require(it.isNotBlank()) { "Enter your name" } }
        (update[WorkerProfiles.SKILLS] as? List<*>)?.let { skills ->
            update[WorkerProfiles.SKILLS] = skills.mapNotNull { (it as? String)?.trim()?.uppercase()?.takeIf(String::isNotEmpty) }.distinct()
        }
        addPlace(update, WorkerProfiles.ADDRESS, WorkerProfiles.AREA, WorkerProfiles.LAT, WorkerProfiles.LNG, WorkerProfiles.GEOHASH)
        update[WorkerProfiles.UPDATED_AT] = Timestamp.now()
        workerRef(uid).set(update, SetOptions.merge()).await()
        Unit
    }.onFailure { Timber.e(it, "saveWorker failed") }

    /** Saves employer fields (keys from [EmployerProfiles]); same derivations as [saveWorker]. */
    suspend fun saveEmployer(fields: Map<String, Any?>): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val update = fields.filterKeys { it in EMPLOYER_EDITABLE }.toMutableMap()
        (update[EmployerProfiles.OWNER_NAME] as? String)?.let { require(it.isNotBlank()) { "Enter your name" } }
        if (update[EmployerProfiles.EMPLOYER_TYPE] == Values.EmployerType.COMPANY) {
            require((update[EmployerProfiles.BUSINESS_NAME] as? String).orEmpty().isNotBlank()) { "Enter your business name" }
        }
        (update[EmployerProfiles.GSTIN] as? String)?.let { update[EmployerProfiles.GSTIN] = it.trim().uppercase() }
        addPlace(update, EmployerProfiles.ADDRESS, EmployerProfiles.AREA, EmployerProfiles.LAT, EmployerProfiles.LNG, EmployerProfiles.GEOHASH)
        update[EmployerProfiles.UPDATED_AT] = Timestamp.now()
        employerRef(uid).set(update, SetOptions.merge()).await()
        Unit
    }.onFailure { Timber.e(it, "saveEmployer failed") }

    /**
     * Saves the worker's current place for matching, at most once per [LOCATION_MIN_MOVE_KM] moved or
     * per day: every profile write also rewrites the public worker card, so GPS refreshes must not.
     */
    suspend fun saveWorkerLocation(lat: Double, lng: Double, address: String): Result<Unit> {
        if (!GeoUtils.hasValidCoordinates(lat, lng)) return Result.success(Unit)
        val prefs = context.getSharedPreferences("worker_location_sync", Context.MODE_PRIVATE)
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val lastLat = prefs.getFloat("lat_$uid", 0f).toDouble()
        val lastLng = prefs.getFloat("lng_$uid", 0f).toDouble()
        val lastAt = prefs.getLong("at_$uid", 0L)
        val moved = lastAt == 0L || GeoUtils.calculateDistance(lastLat, lastLng, lat, lng) >= LOCATION_MIN_MOVE_KM
        if (!moved && System.currentTimeMillis() - lastAt < LOCATION_MAX_AGE_MS) return Result.success(Unit)
        return saveWorker(
            mapOf(WorkerProfiles.LAT to lat, WorkerProfiles.LNG to lng, WorkerProfiles.ADDRESS to address)
        ).onSuccess {
            prefs.edit()
                .putFloat("lat_$uid", lat.toFloat())
                .putFloat("lng_$uid", lng.toFloat())
                .putLong("at_$uid", System.currentTimeMillis())
                .apply()
        }
    }

    /** Uploads a compressed photo and stores its URL as the profile's `photoUrl`. */
    suspend fun uploadProfileImage(imageUri: Uri, userId: String, userRole: String): Result<String> = runCatching {
        val path = "profile_images/$userId/profile_${System.currentTimeMillis()}.jpg"
        when (val upload = ImageUploadUtils.uploadWithRetry(context = context, uri = imageUri, storagePath = path)) {
            is ImageUploadUtils.UploadResult.Success -> {
                val isEmployer = userRole.uppercase() == Values.Role.EMPLOYER
                val ref = if (isEmployer) employerRef(userId) else workerRef(userId)
                val key = if (isEmployer) EmployerProfiles.PHOTO_URL else WorkerProfiles.PHOTO_URL
                ref.set(mapOf(key to upload.downloadUrl), SetOptions.merge()).await()
                upload.downloadUrl
            }
            is ImageUploadUtils.UploadResult.Failure -> throw upload.exception ?: Exception(upload.error)
            else -> error("Upload did not finish")
        }
    }

    /** The private profile of a worker who applied to one of this employer's jobs (Cloud Function). */
    suspend fun getWorkerProfileForEmployer(workerId: String, jobId: String? = null): Result<Map<String, Any?>> = runCatching {
        val payload = buildMap<String, Any> {
            put("workerId", workerId)
            if (!jobId.isNullOrBlank()) put("jobId", jobId)
        }
        @Suppress("UNCHECKED_CAST")
        val data = functions.getHttpsCallable("getWorkerProfileForEmployer").call(payload).await().data as? Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        (data?.get("profile") as? Map<String, Any?>).orEmpty()
    }

    // ─────────────────────────────── internals ───────────────────────────────

    private fun addPlace(update: MutableMap<String, Any?>, address: String, area: String, lat: String, lng: String, geohash: String) {
        (update[address] as? String)?.let { text ->
            update[address] = text.trim()
            if (update[area] == null) update[area] = AreaText.from(text)
        }
        val la = (update[lat] as? Number)?.toDouble()
        val lo = (update[lng] as? Number)?.toDouble()
        if (la != null && lo != null) {
            if (GeoUtils.hasValidCoordinates(la, lo)) update[geohash] = Geohash.encode(la, lo, 9)
            else { update.remove(lat); update.remove(lng) }
        }
    }

    private suspend fun readWithCacheFallback(ref: DocumentReference): DocumentSnapshot = try {
        withTimeout(READ_TIMEOUT_MS) { ref.get().await() }
    } catch (e: Exception) {
        val offline = e is TimeoutCancellationException ||
            (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE)
        if (!offline) throw e
        runCatching { ref.get(Source.CACHE).await() }.getOrNull()?.takeIf { it.exists() } ?: throw e
    }

    private companion object {
        const val READ_TIMEOUT_MS = 4_000L
        const val LOCATION_MIN_MOVE_KM = 0.5
        const val LOCATION_MAX_AGE_MS = 24 * 60 * 60 * 1000L
        val WORKER_EDITABLE = setOf(
            WorkerProfiles.NAME, WorkerProfiles.PHOTO_URL, WorkerProfiles.GENDER, WorkerProfiles.DATE_OF_BIRTH,
            WorkerProfiles.EDUCATION, WorkerProfiles.EXPERIENCE_YEARS, WorkerProfiles.SKILLS, WorkerProfiles.BIO,
            WorkerProfiles.ADDRESS, WorkerProfiles.AREA, WorkerProfiles.LAT, WorkerProfiles.LNG, WorkerProfiles.AVAILABLE
        )
        val EMPLOYER_EDITABLE = setOf(
            EmployerProfiles.EMPLOYER_TYPE, EmployerProfiles.OWNER_NAME, EmployerProfiles.BUSINESS_NAME,
            EmployerProfiles.BUSINESS_TYPE, EmployerProfiles.GSTIN, EmployerProfiles.PHOTO_URL,
            EmployerProfiles.ADDRESS, EmployerProfiles.AREA, EmployerProfiles.LAT, EmployerProfiles.LNG
        )
    }
}
