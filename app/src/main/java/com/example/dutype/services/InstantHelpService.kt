package com.example.dutype.services

import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.InstantHelpDefaults
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.LocationData
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.models.WorkerAvailability
import com.example.dutype.utils.GeoUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstantHelpService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun getWorkerAvailability(): Result<WorkerAvailability?> = withContext(Dispatchers.IO) {
        val workerId = auth.currentUser?.uid
        if (workerId.isNullOrBlank()) return@withContext Result.success(null)

        return@withContext try {
            val snapshot = firestore.collection(FirestoreCollections.WORKER_AVAILABILITY)
                .document(workerId)
                .get()
                .await()
            Result.success(snapshot.toWorkerAvailabilityOrNull())
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun saveWorkerAvailability(
        isAvailable: Boolean,
        categories: List<String>,
        radiusKm: Double,
        currentLocation: LocationData?
    ): Result<WorkerAvailability> = withContext(Dispatchers.IO) {
        val workerId = auth.currentUser?.uid
        if (workerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }
        if (isAvailable && (currentLocation == null || !GeoUtils.hasValidCoordinates(currentLocation.latitude, currentLocation.longitude))) {
            return@withContext Result.failure(IllegalStateException("Set your location before going available"))
        }

        return@withContext try {
            val now = Timestamp.now()
            val expiresAt = Timestamp(Date(System.currentTimeMillis() + 8 * 60 * 60 * 1000L))
            val normalizedCategories = categories
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .ifEmpty { InstantHelpDefaults.defaultWorkerCategories }
            val safeRadius = radiusKm.coerceIn(2.0, 10.0)
            val latitude = currentLocation?.latitude ?: 0.0
            val longitude = currentLocation?.longitude ?: 0.0
            val geohash = if (GeoUtils.hasValidCoordinates(latitude, longitude)) {
                GeoUtils.encodeGeohash(latitude, longitude)
            } else {
                ""
            }

            val data = mapOf(
                "workerId" to workerId,
                "isAvailable" to isAvailable,
                "status" to if (isAvailable) "available" else "offline",
                "categories" to normalizedCategories,
                "radiusKm" to safeRadius,
                "lat" to latitude,
                "lng" to longitude,
                "geohash" to geohash,
                "availableUntil" to expiresAt,
                "lastSeenAt" to now,
                "updatedAt" to now
            )

            firestore.collection(FirestoreCollections.WORKER_AVAILABILITY)
                .document(workerId)
                .set(data, SetOptions.merge())
                .await()

            firestore.collection(FirestoreCollections.WORKER_PROFILES)
                .document(workerId)
                .set(
                    mapOf(
                        "isAvailable" to isAvailable,
                        "updatedAt" to now
                    ),
                    SetOptions.merge()
                )
                .await()

            Result.success(
                WorkerAvailability(
                    workerId = workerId,
                    isAvailable = isAvailable,
                    status = if (isAvailable) "available" else "offline",
                    categories = normalizedCategories,
                    radiusKm = safeRadius,
                    lat = latitude,
                    lng = longitude,
                    geohash = geohash,
                    availableUntil = expiresAt.toDate().time,
                    lastSeenAt = now.toDate().time,
                    updatedAt = now.toDate().time
                )
            )
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getOpenInstantRequestsForWorker(
        availability: WorkerAvailability,
        currentLocation: LocationData?
    ): Result<List<InstantRequest>> = withContext(Dispatchers.IO) {
        if (!availability.isAvailable || currentLocation == null) {
            return@withContext Result.success(emptyList())
        }
        if (!GeoUtils.hasValidCoordinates(currentLocation.latitude, currentLocation.longitude)) {
            return@withContext Result.success(emptyList())
        }

        return@withContext try {
            val now = System.currentTimeMillis()
            val snapshot = firestore.collection(FirestoreCollections.INSTANT_REQUESTS)
                .whereEqualTo("status", "open")
                .limit(75)
                .get()
                .await()

            val workerCategories = availability.categories.map { it.lowercase() }.toSet()
            val requests = snapshot.documents
                .mapNotNull { it.toInstantRequestOrNull() }
                .filter { it.expiresAt == 0L || it.expiresAt > now }
                .mapNotNull { request ->
                    if (!GeoUtils.hasValidCoordinates(request.lat, request.lng)) return@mapNotNull null
                    val distance = GeoUtils.calculateDistance(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        request.lat,
                        request.lng
                    )
                    val categoryMatches = workerCategories.isEmpty() || request.category.lowercase() in workerCategories
                    val withinRequestRadius = distance <= request.radiusKm
                    val withinWorkerRadius = distance <= availability.radiusKm
                    if (categoryMatches && withinRequestRadius && withinWorkerRadius) {
                        request.copy(distanceKm = distance)
                    } else {
                        null
                    }
                }
                .sortedWith(
                    compareBy<InstantRequest> { if (it.urgency == "urgent") 0 else 1 }
                        .thenBy { it.distanceKm ?: Double.MAX_VALUE }
                        .thenByDescending { it.createdAt }
                )
                .take(10)

            Result.success(requests)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun createUrgentNeed(input: QuickUrgentNeedInput): Result<String> = withContext(Dispatchers.IO) {
        val employerId = auth.currentUser?.uid
        if (employerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }
        if (input.title.trim().length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Enter what help you need"))
        }

        return@withContext try {
            val employerDoc = firestore.collection(FirestoreCollections.EMPLOYER_PROFILES)
                .document(employerId)
                .get()
                .await()
            val employerData = employerDoc.data.orEmpty()
            val location = employerData["businessLocation"] as? Map<*, *>
            val latitude = location?.getNumber("lat")?.toDouble() ?: 0.0
            val longitude = location?.getNumber("lng")?.toDouble() ?: 0.0
            if (!GeoUtils.hasValidCoordinates(latitude, longitude)) {
                return@withContext Result.failure(IllegalStateException("Add your business location before posting an urgent need"))
            }

            val now = Timestamp.now()
            val expiryMs = when (input.needType) {
                "urgent_now" -> 2 * 60 * 60 * 1000L
                "today" -> 12 * 60 * 60 * 1000L
                else -> 36 * 60 * 60 * 1000L
            }
            val expiresAt = Timestamp(Date(System.currentTimeMillis() + expiryMs))
            val requestRef = firestore.collection(FirestoreCollections.INSTANT_REQUESTS).document()
            val safeRadius = input.radiusKm.coerceIn(2.0, 10.0)
            val data = mapOf(
                "requestId" to requestRef.id,
                "employerId" to employerId,
                "employerName" to (employerData.getString("companyName").ifBlank { employerData.getString("fullName") }.ifBlank { "DutyPe employer" }),
                "employerPhone" to employerData.getString("phone"),
                "title" to input.title.trim(),
                "description" to input.description.trim(),
                "category" to input.category.trim().ifBlank { "Helper" },
                "needType" to input.needType,
                "status" to "open",
                "urgency" to if (input.needType == "urgent_now") "urgent" else "today",
                "budgetText" to input.budgetText.trim(),
                "lat" to latitude,
                "lng" to longitude,
                "geohash" to GeoUtils.encodeGeohash(latitude, longitude),
                "addressText" to employerData.getString("businessAddress"),
                "radiusKm" to safeRadius,
                "createdAt" to now,
                "expiresAt" to expiresAt,
                "responseCount" to 0,
                "callCount" to 0,
                "selectedWorkerId" to "",
                "failureReason" to ""
            )

            requestRef.set(data).await()
            Result.success(requestRef.id)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun respondToInstantRequest(request: InstantRequest, action: String): Result<Unit> = withContext(Dispatchers.IO) {
        val workerId = auth.currentUser?.uid
        if (workerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }

        return@withContext try {
            val normalizedStatus = when (action.lowercase()) {
                "called" -> "called"
                "busy" -> "busy"
                else -> "interested"
            }
            val now = Timestamp.now()
            val workerDoc = firestore.collection(FirestoreCollections.WORKER_PROFILES)
                .document(workerId)
                .get()
                .await()
            val workerData = workerDoc.data.orEmpty()
            val responseId = "${request.requestId}_$workerId"
            val responseRef = firestore.collection(FirestoreCollections.INSTANT_RESPONSES).document(responseId)
            val existing = responseRef.get().await()

            val data = mutableMapOf<String, Any>(
                "responseId" to responseId,
                "requestId" to request.requestId,
                "workerId" to workerId,
                "employerId" to request.employerId,
                "workerName" to workerData.getString("fullName").ifBlank { "Worker" },
                "workerPhone" to workerData.getString("phone"),
                "workerSkills" to workerData.getStringList("skills"),
                "status" to normalizedStatus,
                "viewedAt" to now,
                "updatedAt" to now
            )
            request.distanceKm?.let { data["distanceKm"] = it }
            if (!existing.exists()) {
                data["createdAt"] = now
            }
            if (normalizedStatus == "called") {
                data["calledAt"] = now
                data["respondedAt"] = now
            } else {
                data["respondedAt"] = now
            }

            responseRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun DocumentSnapshot.toWorkerAvailabilityOrNull(): WorkerAvailability? {
        val data = data ?: return null
        return WorkerAvailability(
            workerId = data.getString("workerId").ifBlank { id },
            isAvailable = data["isAvailable"] as? Boolean ?: false,
            status = data.getString("status").ifBlank { "offline" },
            categories = data.getStringList("categories").ifEmpty { InstantHelpDefaults.defaultWorkerCategories },
            radiusKm = data.getNumber("radiusKm")?.toDouble() ?: 5.0,
            lat = data.getNumber("lat")?.toDouble() ?: 0.0,
            lng = data.getNumber("lng")?.toDouble() ?: 0.0,
            geohash = data.getString("geohash"),
            availableUntil = data.getMillis("availableUntil"),
            lastSeenAt = data.getMillis("lastSeenAt"),
            updatedAt = data.getMillis("updatedAt")
        )
    }

    private fun DocumentSnapshot.toInstantRequestOrNull(): InstantRequest? {
        val data = data ?: return null
        return InstantRequest(
            requestId = data.getString("requestId").ifBlank { id },
            employerId = data.getString("employerId"),
            employerName = data.getString("employerName").ifBlank { "DutyPe employer" },
            employerPhone = data.getString("employerPhone"),
            title = data.getString("title").ifBlank { "Urgent need" },
            description = data.getString("description"),
            category = data.getString("category").ifBlank { "Helper" },
            needType = data.getString("needType").ifBlank { "urgent_now" },
            status = data.getString("status").ifBlank { "open" },
            urgency = data.getString("urgency").ifBlank { "urgent" },
            budgetText = data.getString("budgetText"),
            lat = data.getNumber("lat")?.toDouble() ?: 0.0,
            lng = data.getNumber("lng")?.toDouble() ?: 0.0,
            geohash = data.getString("geohash"),
            addressText = data.getString("addressText"),
            radiusKm = data.getNumber("radiusKm")?.toDouble() ?: 5.0,
            createdAt = data.getMillis("createdAt"),
            expiresAt = data.getMillis("expiresAt"),
            responseCount = data.getNumber("responseCount")?.toInt() ?: 0,
            callCount = data.getNumber("callCount")?.toInt() ?: 0,
            selectedWorkerId = data.getString("selectedWorkerId"),
            completedAt = data.getMillis("completedAt"),
            failureReason = data.getString("failureReason")
        )
    }

    private fun Map<*, *>.getString(key: String): String = this[key]?.toString()?.trim().orEmpty()

    private fun Map<*, *>.getNumber(key: String): Number? = this[key] as? Number

    private fun Map<*, *>.getStringList(key: String): List<String> {
        return (this[key] as? List<*>).orEmpty()
            .mapNotNull { it?.toString()?.trim()?.takeIf { value -> value.isNotBlank() } }
            .distinct()
    }

    private fun Map<*, *>.getMillis(key: String): Long {
        return when (val value = this[key]) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            else -> 0L
        }
    }
}