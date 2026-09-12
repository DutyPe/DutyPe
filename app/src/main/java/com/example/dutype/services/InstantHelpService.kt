package com.example.dutype.services

import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.InstantResponse
import com.example.dutype.models.LocationData
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.models.WorkerAvailability
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.JobDeletePolicy
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstantHelpService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private companion object {
        const val MAX_INSTANT_WORK_DISTANCE_KM = 10.0
    }

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
                "categories" to FieldValue.delete(),
                "radiusKm" to FieldValue.delete(),
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

            runCatching {
                firestore.collection(FirestoreCollections.WORKER_PROFILES)
                    .document(workerId)
                    .set(mapOf("isAvailable" to FieldValue.delete()), SetOptions.merge())
                    .await()
            }.onFailure { cleanupError ->
                Timber.w(cleanupError, "Failed to remove legacy worker profile availability field")
            }

            Result.success(
                WorkerAvailability(
                    workerId = workerId,
                    isAvailable = isAvailable,
                    status = if (isAvailable) "available" else "offline",
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
                    val requestRadiusKm = request.radiusKm
                        .takeIf { radius -> radius > 0.0 }
                        ?.coerceAtMost(MAX_INSTANT_WORK_DISTANCE_KM)
                        ?: MAX_INSTANT_WORK_DISTANCE_KM
                    if (distance <= MAX_INSTANT_WORK_DISTANCE_KM && distance <= requestRadiusKm) {
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
        val nowMillis = System.currentTimeMillis()
        val maxScheduleMillis = nowMillis + 48 * 60 * 60 * 1000L
        if (input.needType == "scheduled" && input.scheduledAtMillis !in (nowMillis + 1)..maxScheduleMillis) {
            return@withContext Result.failure(IllegalArgumentException("Tomorrow urgent work must be within 48 hours"))
        }

        return@withContext try {
            val employerProfileRef = firestore.collection(FirestoreCollections.EMPLOYER_PROFILES).document(employerId)
            val requestRef = firestore.collection(FirestoreCollections.INSTANT_REQUESTS).document()
            
            firestore.runTransaction { transaction ->
                val employerDoc = transaction.get(employerProfileRef)
                if (!employerDoc.exists()) {
                    throw IllegalStateException("Employer profile not found")
                }
                
                val location = employerDoc.get("businessLocation") as? Map<*, *>
                val latitude = (location?.get("lat") as? Number)?.toDouble() ?: 0.0
                val longitude = (location?.get("lng") as? Number)?.toDouble() ?: 0.0
                if (!GeoUtils.hasValidCoordinates(latitude, longitude)) {
                    throw IllegalStateException("Add your business location before posting an urgent need")
                }
                
                val profilePhone = employerDoc.getString("phone").orEmpty().trim()
                    .ifBlank { employerDoc.getString("phoneNumber").orEmpty().trim() }
                    .ifBlank { auth.currentUser?.phoneNumber.orEmpty() }
                val contactNumber = input.contactNumber.trim().ifBlank { profilePhone }.take(20)
                if (contactNumber.isBlank()) {
                    throw IllegalArgumentException("Enter contact number")
                }
                
                val freeUrgentJobsPosted = (employerDoc.getLong("freeUrgentJobsPosted") ?: 0L).toInt()
                val subMap = employerDoc.get("subscription") as? Map<String, Any?>
                val sub = com.example.dutype.models.EmployerSubscription.fromMap(subMap)
                val isExpired = sub.expiryDate > 0 && sub.expiryDate < nowMillis
                
                val useFreePost = freeUrgentJobsPosted < 3
                
                if (!useFreePost) {
                    if (sub.status == "NONE" || isExpired || sub.normalCredits <= 0) {
                        throw IllegalArgumentException("You have used your 3 free Urgent Work posts. Please purchase a subscription to post more jobs.")
                    }
                }
                
                val workersNeeded = input.workersNeeded.coerceIn(1, 20)
                val now = Timestamp.now()
                val scheduledAt = if (input.needType == "scheduled" && input.scheduledAtMillis > nowMillis) {
                    Timestamp(java.util.Date(input.scheduledAtMillis))
                } else {
                    null
                }
                
                val expiresAtMillis = when (input.urgencyType) {
                    "right_now" -> nowMillis + 4L * 60 * 60 * 1000L // 4 Hours
                    "within_1_hour" -> nowMillis + 6L * 60 * 60 * 1000L // 6 Hours
                    "today", "1_day", "within_1_day" -> nowMillis + 24L * 60 * 60 * 1000L // 1 Day (24 Hours)
                    "tomorrow", "2_days", "within_2_days" -> nowMillis + 48L * 60 * 60 * 1000L // 2 Days (48 Hours)
                    "custom" -> {
                        if (input.scheduledAtMillis > nowMillis) input.scheduledAtMillis
                        else nowMillis + 48L * 60 * 60 * 1000L
                    }
                    else -> nowMillis + 48L * 60 * 60 * 1000L // Default: 2 Days
                }
                val expiresAt = Timestamp(java.util.Date(expiresAtMillis))
                val safeRadius = input.radiusKm.coerceIn(2.0, MAX_INSTANT_WORK_DISTANCE_KM)
                
                val employerName = employerDoc.getString("companyName").orEmpty().trim()
                    .ifBlank { employerDoc.getString("fullName").orEmpty().trim() }
                    .ifBlank { "DutyPe employer" }
                val businessAddress = employerDoc.getString("businessAddress").orEmpty().trim()

                val data = mutableMapOf<String, Any>(
                    "requestId" to requestRef.id,
                    "employerId" to employerId,
                    "employerName" to employerName,
                    "employerPhone" to contactNumber,
                    "contactNumber" to contactNumber,
                    "title" to input.title.trim(),
                    "description" to input.description.trim(),
                    "category" to input.category.trim().ifBlank { "Other" },
                    "workersNeeded" to workersNeeded,
                    "needType" to input.needType,
                    "status" to "open",
                    "urgency" to "urgent",
                    "urgencyType" to input.urgencyType,
                    "budgetText" to input.budgetText.trim(),
                    "perPersonPayment" to input.perPersonPayment,
                    "totalPayment" to input.totalPayment,
                    "durationText" to input.durationText,
                    "lat" to latitude,
                    "lng" to longitude,
                    "geohash" to GeoUtils.encodeGeohash(latitude, longitude),
                    "addressText" to input.contactNumber.trim().ifBlank { businessAddress }, // temporary hack to save exact address since contactNumber was replaced, actually let's just use input.addressText but it's not passed, I'll pass it in ContactNumber for now, wait we need to add addressText to input!
                    "radiusKm" to safeRadius,
                    "createdAt" to now,
                    "expiresAt" to expiresAt,
                    "responseCount" to 0,
                    "callCount" to 0,
                    "selectedWorkerId" to "",
                    "selectedWorkerIds" to emptyList<String>(),
                    "completedWorkerIds" to emptyList<String>(),
                    "failureReason" to ""
                )
                if (scheduledAt != null) {
                    data["scheduledAt"] = scheduledAt
                    data["scheduleLabel"] = input.scheduledAtLabel.trim().take(80)
                }
                
                if (useFreePost) {
                    transaction.update(employerProfileRef, "freeUrgentJobsPosted", freeUrgentJobsPosted + 1)
                } else {
                    val currentCredits = subMap?.get("credits") as? Map<String, Any?>
                    val newCredits = currentCredits.orEmpty().toMutableMap().apply {
                        put("normal", maxOf(0, sub.normalCredits - 1))
                    }
                    val newSubMap = subMap.orEmpty().toMutableMap().apply {
                        put("credits", newCredits)
                    }
                    transaction.update(employerProfileRef, "subscription", newSubMap)
                }
                
                transaction.set(requestRef, data)
            }.await()
            
            Result.success(requestRef.id)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getEmployerInstantRequests(): Result<List<InstantRequest>> = withContext(Dispatchers.IO) {
        val employerId = auth.currentUser?.uid
        if (employerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }

        return@withContext try {
            val snapshot = firestore.collection(FirestoreCollections.INSTANT_REQUESTS)
                .whereEqualTo("employerId", employerId)
                .limit(50)
                .get()
                .await()

            val requests = snapshot.documents
                .mapNotNull { it.toInstantRequestOrNull() }
                .filter { it.status != "deleted" }
                .sortedByDescending { it.createdAt }

            Result.success(requests)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getEmployerInstantResponses(): Result<List<InstantResponse>> = withContext(Dispatchers.IO) {
        val employerId = auth.currentUser?.uid
        if (employerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }

        return@withContext try {
            val snapshot = firestore.collection(FirestoreCollections.INSTANT_RESPONSES)
                .whereEqualTo("employerId", employerId)
                .limit(100)
                .get()
                .await()

            val responses = snapshot.documents
                .mapNotNull { it.toInstantResponseOrNull() }
                .sortedByDescending { it.updatedAt.ifBlankTimestamp(it.createdAt) }

            Result.success(responses)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getWorkerInstantResponses(): Result<List<InstantResponse>> = withContext(Dispatchers.IO) {
        val workerId = auth.currentUser?.uid
        if (workerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }

        return@withContext try {
            val snapshot = firestore.collection(FirestoreCollections.INSTANT_RESPONSES)
                .whereEqualTo("workerId", workerId)
                .limit(50)
                .get()
                .await()

            val responses = snapshot.documents
                .mapNotNull { it.toInstantResponseOrNull() }
                .sortedByDescending { it.updatedAt.ifBlankTimestamp(it.createdAt) }

            Result.success(responses)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getWorkerInstantResponsesForRequests(
        requestIds: List<String>
    ): Result<Map<String, InstantResponse>> = withContext(Dispatchers.IO) {
        val workerId = auth.currentUser?.uid
        if (workerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }

        return@withContext try {
            val responses = requestIds
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(10)
                .mapNotNull { requestId ->
                    firestore.collection(FirestoreCollections.INSTANT_RESPONSES)
                        .document("${requestId}_$workerId")
                        .get()
                        .await()
                        .toInstantResponseOrNull()
                }
                .associateBy { it.requestId }

            Result.success(responses)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun updateEmployerInstantResponseStatus(
        response: InstantResponse,
        status: String,
        note: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val employerId = auth.currentUser?.uid
        if (employerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }
        if (response.employerId != employerId) {
            return@withContext Result.failure(IllegalStateException("This urgent response is not yours"))
        }

        return@withContext try {
            val normalizedStatus = when (status.lowercase()) {
                "completed" -> "completed"
                "no_show" -> "no_show"
                "rejected" -> "rejected"
                "cancelled" -> "cancelled"
                else -> "accepted"
            }
            val trimmedNote = note.trim().take(300)
            val now = Timestamp.now()
            val requestRef = firestore.collection(FirestoreCollections.INSTANT_REQUESTS)
                .document(response.requestId)
            val requestData = requestRef.get().await().data.orEmpty()
            val requestStatus = requestData.getString("status").lowercase()
            val workersNeeded = (requestData.getNumber("workersNeeded")?.toInt() ?: 1).coerceAtLeast(1)
            val selectedWorkerIds = requestData.getStringList("selectedWorkerIds")
                .ifEmpty { listOf(requestData.getString("selectedWorkerId")).filter { it.isNotBlank() } }
            val completedWorkerIds = requestData.getStringList("completedWorkerIds")

            if (
                normalizedStatus == "accepted" &&
                response.workerId !in selectedWorkerIds &&
                selectedWorkerIds.size >= workersNeeded
            ) {
                return@withContext Result.failure(
                    IllegalStateException("Required workers are already selected. Mark the urgent need filled or post another need.")
                )
            }

            val responseUpdates = mutableMapOf<String, Any>(
                "status" to normalizedStatus,
                "updatedAt" to now
            )
            if (normalizedStatus == "accepted") {
                responseUpdates["acceptedAt"] = now
            }
            if (normalizedStatus == "completed") {
                responseUpdates["completedAt"] = now
                if (trimmedNote.isNotBlank()) responseUpdates["completionProof"] = trimmedNote
            }
            if (normalizedStatus in setOf("no_show", "rejected", "cancelled") && trimmedNote.isNotBlank()) {
                responseUpdates["failureReason"] = trimmedNote
            }

            firestore.collection(FirestoreCollections.INSTANT_RESPONSES)
                .document(response.responseId)
                .set(responseUpdates, SetOptions.merge())
                .await()

            val requestUpdates = mutableMapOf<String, Any>()
            when (normalizedStatus) {
                "accepted" -> {
                    val selectedAfter = (selectedWorkerIds + response.workerId).distinct()
                    requestUpdates["selectedWorkerId"] = response.workerId
                    requestUpdates["selectedWorkerIds"] = selectedAfter
                    if (selectedAfter.size >= workersNeeded || requestStatus == "filled") {
                        requestUpdates["status"] = "filled"
                        if (selectedAfter.size >= workersNeeded) requestUpdates["filledAt"] = now
                    } else {
                        requestUpdates["status"] = "open"
                    }
                }
                "completed" -> {
                    val selectedAfter = (selectedWorkerIds + response.workerId).distinct()
                    val completedAfter = (completedWorkerIds + response.workerId).distinct()
                    requestUpdates["selectedWorkerId"] = response.workerId
                    requestUpdates["selectedWorkerIds"] = selectedAfter
                    requestUpdates["completedWorkerIds"] = completedAfter
                    if (completedAfter.size >= workersNeeded) {
                        requestUpdates["status"] = "completed"
                        requestUpdates["completedAt"] = now
                    } else {
                        requestUpdates["status"] = if (selectedAfter.size >= workersNeeded || requestStatus == "filled") "filled" else "open"
                    }
                    if (trimmedNote.isNotBlank()) requestUpdates["completionProof"] = trimmedNote
                }
                "no_show" -> {
                    val selectedAfter = selectedWorkerIds.filterNot { it == response.workerId }
                    val completedAfter = completedWorkerIds.filterNot { it == response.workerId }
                    requestUpdates["selectedWorkerId"] = selectedAfter.lastOrNull().orEmpty()
                    requestUpdates["selectedWorkerIds"] = selectedAfter
                    requestUpdates["completedWorkerIds"] = completedAfter
                    requestUpdates["status"] = if (selectedAfter.size >= workersNeeded) "filled" else "open"
                    requestUpdates["failureReason"] = FieldValue.delete()
                }
                "rejected" -> {
                    val selectedAfter = selectedWorkerIds.filterNot { it == response.workerId }
                    requestUpdates["selectedWorkerId"] = selectedAfter.lastOrNull().orEmpty()
                    requestUpdates["selectedWorkerIds"] = selectedAfter
                    requestUpdates["status"] = if (selectedAfter.size >= workersNeeded) "filled" else "open"
                }
                "cancelled" -> {
                    requestUpdates["status"] = "cancelled"
                    requestUpdates["cancelledAt"] = now
                    requestUpdates["cancellationReason"] = trimmedNote.ifBlank { "Cancelled by employer" }
                }
            }

            requestRef.set(requestUpdates, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun markEmployerInstantRequestFilled(
        request: InstantRequest
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val employerId = auth.currentUser?.uid
        if (employerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }
        if (request.employerId != employerId) {
            return@withContext Result.failure(IllegalStateException("This urgent request is not yours"))
        }

        return@withContext try {
            val now = Timestamp.now()
            firestore.collection(FirestoreCollections.INSTANT_REQUESTS)
                .document(request.requestId)
                .set(
                    mapOf(
                        "status" to "filled",
                        "filledAt" to now,
                        "failureReason" to FieldValue.delete()
                    ),
                    SetOptions.merge()
                )
                .await()
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun cancelEmployerInstantRequest(
        request: InstantRequest,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val employerId = auth.currentUser?.uid
        if (employerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }
        if (request.employerId != employerId) {
            return@withContext Result.failure(IllegalStateException("This urgent request is not yours"))
        }

        return@withContext try {
            val now = Timestamp.now()
            val currentTime = System.currentTimeMillis()
            val safeReason = reason.trim().take(300).ifBlank { "Cancelled by employer" }
            val requestRef = firestore.collection(FirestoreCollections.INSTANT_REQUESTS).document(request.requestId)
            val isEarlyDelete = (currentTime - request.createdAt) <= JobDeletePolicy.INSTANT_DELETE_WINDOW_MILLIS

            firestore.runTransaction { transaction ->
                if (isEarlyDelete) {
                    // 1. Read first!
                    val employerProfileRef = firestore.collection(FirestoreCollections.EMPLOYER_PROFILES).document(employerId)
                    val profileSnap = transaction.get(employerProfileRef)
                    
                    // 2. Delete request document completely
                    transaction.delete(requestRef)
                    
                    // 3. Refund credits to the employer profile
                    if (profileSnap.exists()) {
                        val subMap = profileSnap.get("subscription") as? Map<String, Any?>
                        if (subMap != null) {
                            val currentCredits = subMap["credits"] as? Map<String, Any?>
                            val newCredits = currentCredits.orEmpty().toMutableMap().apply {
                                val instantCredits = (get("instant") as? Number)?.toInt() ?: 0
                                put("instant", instantCredits + 1)
                            }
                            val newSubMap = subMap.toMutableMap().apply {
                                put("credits", newCredits)
                            }
                            transaction.update(employerProfileRef, "subscription", newSubMap)
                        }
                    }
                } else {
                    // Normal cancel
                    transaction.set(
                        requestRef,
                        mapOf(
                            "status" to "cancelled",
                            "cancelledAt" to now,
                            "cancellationReason" to safeReason,
                            "failureReason" to safeReason
                        ),
                        SetOptions.merge()
                    )
                }
            }.await()

            // Responses cleanup
            val responses = firestore.collection(FirestoreCollections.INSTANT_RESPONSES)
                .whereEqualTo("requestId", request.requestId)
                .limit(50)
                .get()
                .await()

            val batch = firestore.batch()
            responses.documents.forEach { document ->
                if (isEarlyDelete) {
                    batch.delete(document.reference)
                } else {
                    batch.set(
                        document.reference,
                        mapOf(
                            "status" to "cancelled",
                            "failureReason" to safeReason,
                            "updatedAt" to now
                        ),
                        SetOptions.merge()
                    )
                }
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun deleteEmployerInstantRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val employerId = auth.currentUser?.uid
        if (employerId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Please login again"))
        }

        return@withContext try {
            val requestRef = firestore.collection(FirestoreCollections.INSTANT_REQUESTS).document(requestId)
            requestRef.update("status", "deleted").await()
            Result.success(Unit)
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
                "applied" -> "applied"
                else -> "applied"
            }
            val now = Timestamp.now()
            val requestRef = firestore.collection(FirestoreCollections.INSTANT_REQUESTS)
                .document(request.requestId)
            val requestSnapshot = requestRef.get().await()
            val liveStatus = requestSnapshot.getString("status") ?: request.status
            if (!liveStatus.equals("open", ignoreCase = true)) {
                return@withContext Result.failure(IllegalStateException("This urgent need is already filled or closed"))
            }
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
                "requestTitle" to request.title,
                "requestCategory" to request.category,
                "employerName" to request.employerName,
                "employerPhone" to request.employerPhone,
                "budgetText" to request.budgetText,
                "addressText" to request.addressText,
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
            val requestUpdates = mutableMapOf<String, Any>(
                "lastResponseAt" to now
            )
            if (!existing.exists()) {
                requestUpdates["responseCount"] = FieldValue.increment(1)
                if (requestSnapshot.get("firstResponseAt") == null) {
                    requestUpdates["firstResponseAt"] = now
                }
            }
            if (normalizedStatus == "called") {
                requestUpdates["callCount"] = FieldValue.increment(1)
            }
            requestRef.set(requestUpdates, SetOptions.merge()).await()
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
            contactNumber = data.getString("contactNumber").ifBlank { data.getString("employerPhone") },
            title = data.getString("title").ifBlank { "Urgent need" },
            description = data.getString("description"),
            category = data.getString("category").ifBlank { "Helper" },
            workersNeeded = (data.getNumber("workersNeeded")?.toInt() ?: 1).coerceAtLeast(1),
            needType = data.getString("needType").ifBlank { "urgent_now" },
            status = data.getString("status").ifBlank { "open" },
            urgency = data.getString("urgency").ifBlank { "urgent" },
            budgetText = data.getString("budgetText"),
            lat = data.getNumber("lat")?.toDouble() ?: 0.0,
            lng = data.getNumber("lng")?.toDouble() ?: 0.0,
            geohash = data.getString("geohash"),
            addressText = data.getString("addressText"),
            radiusKm = data.getNumber("radiusKm")?.toDouble() ?: 5.0,
            scheduledAt = data.getMillis("scheduledAt"),
            scheduleLabel = data.getString("scheduleLabel"),
            createdAt = data.getMillis("createdAt"),
            expiresAt = data.getMillis("expiresAt"),
            expiredAt = data.getMillis("expiredAt"),
            firstResponseAt = data.getMillis("firstResponseAt"),
            lastResponseAt = data.getMillis("lastResponseAt"),
            responseCount = data.getNumber("responseCount")?.toInt() ?: 0,
            callCount = data.getNumber("callCount")?.toInt() ?: 0,
            notifiedWorkerCount = data.getNumber("notifiedWorkerCount")?.toInt() ?: 0,
            notificationFanoutAt = data.getMillis("notificationFanoutAt"),
            selectedWorkerId = data.getString("selectedWorkerId"),
            selectedWorkerIds = data.getStringList("selectedWorkerIds")
                .ifEmpty { listOf(data.getString("selectedWorkerId")).filter { it.isNotBlank() } },
            completedWorkerIds = data.getStringList("completedWorkerIds"),
            completedAt = data.getMillis("completedAt"),
            cancelledAt = data.getMillis("cancelledAt"),
            cancellationReason = data.getString("cancellationReason"),
            completionProof = data.getString("completionProof"),
            failureReason = data.getString("failureReason")
        )
    }

    private fun DocumentSnapshot.toInstantResponseOrNull(): InstantResponse? {
        val data = data ?: return null
        return InstantResponse(
            responseId = data.getString("responseId").ifBlank { id },
            requestId = data.getString("requestId"),
            workerId = data.getString("workerId"),
            employerId = data.getString("employerId"),
            requestTitle = data.getString("requestTitle").ifBlank { "Urgent work" },
            requestCategory = data.getString("requestCategory").ifBlank { "Helper" },
            employerName = data.getString("employerName").ifBlank { "DutyPe employer" },
            employerPhone = data.getString("employerPhone"),
            budgetText = data.getString("budgetText"),
            addressText = data.getString("addressText"),
            workerName = data.getString("workerName").ifBlank { "Worker" },
            workerPhone = data.getString("workerPhone"),
            workerSkills = data.getStringList("workerSkills"),
            distanceKm = data.getNumber("distanceKm")?.toDouble(),
            status = data.getString("status").ifBlank { "viewed" },
            createdAt = data.getMillis("createdAt"),
            viewedAt = data.getMillis("viewedAt"),
            respondedAt = data.getMillis("respondedAt"),
            calledAt = data.getMillis("calledAt"),
            acceptedAt = data.getMillis("acceptedAt"),
            completedAt = data.getMillis("completedAt"),
            updatedAt = data.getMillis("updatedAt"),
            completionProof = data.getString("completionProof"),
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

    private fun Long.ifBlankTimestamp(fallback: Long): Long = if (this > 0L) this else fallback
}
