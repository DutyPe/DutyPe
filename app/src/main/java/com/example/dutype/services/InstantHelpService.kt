package com.example.dutype.services

import com.example.dutype.employer.models.JobCategory
import com.example.dutype.firestore.FirestoreSchema.EmployerCards
import com.example.dutype.firestore.FirestoreSchema.InstantRequests
import com.example.dutype.jobs.Geohash
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.InstantResponse
import com.example.dutype.models.LocationData
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.profile.EmployerProfile
import com.example.dutype.utils.AreaText
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.epochMillis
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Instant help (urgent same-day work). Every write goes through Cloud Functions (instant.ts);
 * reads are bounded: nearby open needs by geohash cells, the worker's own responses by id,
 * the employer's own needs and each need's responses subcollection.
 */
@Singleton
class InstantHelpService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {
    private val requests get() = firestore.collection(InstantRequests.COLLECTION)
    private val R = InstantRequests.Responses

    private suspend fun call(name: String, payload: Map<String, Any?>): Map<*, *>? =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *>

    // ─────────────────────────────── worker ───────────────────────────────

    /**
     * Open, unexpired urgent needs within [MAX_RADIUS_KM] of the worker, whatever the skill, nearest
     * first. Searches 5 km, then 10, then 20 km by ~5 km cells (`cell in [...]`, 30 cells and
     * [MAX_SHOWN] needs per query) and stops once [MAX_SHOWN] are found: usually 1–2 queries and a
     * handful of reads, at most ~40 reads.
     */
    suspend fun getOpenInstantRequestsForWorker(currentLocation: LocationData?): Result<List<InstantRequest>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Urgent requests are readable only when signed in (rules); guests see none.
                if (auth.currentUser == null) return@runCatching emptyList()
                val here = currentLocation?.takeIf { GeoUtils.hasValidCoordinates(it.latitude, it.longitude) }
                    ?: return@runCatching emptyList()
                val now = System.currentTimeMillis()
                val me = auth.currentUser?.uid
                val found = HashMap<String, InstantRequest>()
                val queried = HashSet<String>()
                for (radius in BANDS_KM) {
                    val cells = Geohash.covering(here.latitude, here.longitude, radius, CELL_PRECISION)
                        .filter { queried.add(it) }
                    val docs = coroutineScope {
                        cells.chunked(30).map { chunk ->
                            async {
                                requests.whereEqualTo(InstantRequests.STATUS, STATUS_OPEN)
                                    .whereIn(InstantRequests.CELL, chunk)
                                    .orderBy(InstantRequests.CREATED_AT, Query.Direction.DESCENDING)
                                    .limit(MAX_SHOWN.toLong())
                                    .get().await().documents
                            }
                        }.awaitAll().flatten()
                    }
                    docs.mapNotNull { it.toInstantRequest() }
                        .filter { it.expiresAt > now && it.employerId != me }
                        .forEach { request ->
                            val d = GeoUtils.calculateDistance(here.latitude, here.longitude, request.lat, request.lng)
                            if (d <= MAX_RADIUS_KM) found[request.requestId] = request.copy(distanceKm = d)
                        }
                    // Enough needs inside this band: nothing farther can come before them.
                    if (found.values.count { (it.distanceKm ?: Double.MAX_VALUE) <= radius } >= MAX_SHOWN) break
                }
                val nearby = found.values.sortedBy { it.distanceKm ?: Double.MAX_VALUE }.take(MAX_SHOWN)
                val names = employerNames(nearby.map { it.employerId })
                val mine = myResponses(nearby.map { it.requestId })
                nearby.map { request ->
                    val response = mine[request.requestId]
                    request.copy(
                        employerName = names[request.employerId] ?: request.employerName,
                        workerResponseId = response?.responseId.orEmpty(),
                        workerResponseStatus = response?.status.orEmpty(),
                        workerRespondedAt = response?.createdAt ?: 0L
                    )
                }
            }
        }

    // ─────────────────────────────── urgent offers ───────────────────────────────

    /** One urgent need with its employer's name (the offer page). Null when it no longer exists. */
    suspend fun getRequestForOffer(requestId: String): Result<InstantRequest?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = requests.document(requestId).get().await().toInstantRequest() ?: return@runCatching null
            val name = employerNames(listOf(request.employerId))[request.employerId]
            if (name != null) request.copy(employerName = name) else request
        }
    }

    /** The signed-in worker's response to one need ("accepted", "rejected", …), or "" when none. */
    suspend fun myResponseStatus(requestId: String): String = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext ""
        runCatching {
            requests.document(requestId).collection(R.COLLECTION).document(uid).get().await().getString(R.STATUS).orEmpty()
        }.getOrDefault("")
    }

    /** Accept an urgent offer: the first workers to accept get the places (server decides). */
    suspend fun acceptOffer(requestId: String): Result<com.example.dutype.urgent.UrgentOffers.Result> = runCatching {
        com.example.dutype.urgent.UrgentOffers.accept(functions, requestId)
    }

    /** The worker's own responses for [requestIds]: one document read each (at most 10). */
    private suspend fun myResponses(requestIds: List<String>): Map<String, InstantResponse> {
        val uid = auth.currentUser?.uid ?: return emptyMap()
        return coroutineScope {
            requestIds.map { id ->
                async {
                    runCatching { requests.document(id).collection(R.COLLECTION).document(uid).get().await() }
                        .getOrNull()?.takeIf { it.exists() }?.let { id to it.toInstantResponse(id) }
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }

    /** Urgent work history: my responses (collection group) joined with their needs. */
    suspend fun getWorkerInstantResponses(): Result<List<InstantResponse>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = auth.currentUser?.uid ?: return@runCatching emptyList()
            val responses = firestore.collectionGroup(R.COLLECTION)
                .whereEqualTo(R.WORKER_ID, uid)
                .orderBy(R.CREATED_AT, Query.Direction.DESCENDING)
                .limit(50)
                .get().await().documents
                .mapNotNull { doc -> doc.reference.parent.parent?.id?.let { doc.toInstantResponse(it) } }
            val byId = requestsById(responses.map { it.requestId })
            val names = employerNames(byId.values.map { it.employerId })
            responses.mapNotNull { response ->
                val request = byId[response.requestId] ?: return@mapNotNull null
                response.withRequest(request, names[request.employerId])
            }
        }
    }

    /** "I can come" (`applied`) or a call to the employer (`called`). */
    suspend fun respondToInstantRequest(request: InstantRequest, action: String): Result<Unit> = runCatching {
        call("respondInstantRequest", mapOf("requestId" to request.requestId, "action" to if (action == "called") "called" else "applied"))
        Unit
    }

    // ─────────────────────────────── employer ───────────────────────────────

    /** Posts a need at the employer's saved place. Returns the request id. */
    suspend fun createUrgentNeed(input: QuickUrgentNeedInput, employer: EmployerProfile?): Result<String> = runCatching {
        val placeLat = employer?.lat ?: 0.0
        val placeLng = employer?.lng ?: 0.0
        val placeAddress = employer?.address.orEmpty()

        val categoryKey = JobCategory.entries.firstOrNull {
            it.displayName.equals(input.category.trim(), ignoreCase = true) || it.name.equals(input.category.trim(), ignoreCase = true)
        }?.name ?: JobCategory.OTHER.name
        val address = input.addressText.trim().ifBlank { placeAddress }
        val area = employer?.area?.ifBlank { AreaText.from(address) }?.ifBlank { "Nearby" } ?: "Nearby"
        val reqId = UUID.randomUUID().toString()
        val phone = input.contactNumber.ifBlank { employer?.phone.orEmpty() }.filter(Char::isDigit).takeLast(10)

        val payload = mapOf(
            "requestId" to reqId,
            "title" to input.title.trim().take(80),
            "category" to categoryKey,
            "workersNeeded" to input.workersNeeded.coerceIn(1, 20),
            "payPerPerson" to input.perPersonPayment.toInt().coerceAtLeast(0),
            "durationText" to input.durationText.take(40),
            "addressText" to address.ifBlank { area }.take(200),
            "area" to area.take(60),
            "contactNumber" to phone,
            "radiusKm" to input.radiusKm,
            "lat" to placeLat,
            "lng" to placeLng,
            "window" to input.urgencyType,
            "scheduledAt" to input.scheduledAtMillis.takeIf { input.urgencyType == "custom" }
        )

        // The server checks and charges the post; its error message (e.g. no credits left) is shown as is.
        val data = call("postInstantRequest", payload)
        (data?.get("id") as? String)?.takeIf { it.isNotBlank() } ?: error("Could not post the urgent need. Please try again.")
    }

    suspend fun getEmployerInstantRequests(): Result<List<InstantRequest>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = auth.currentUser?.uid ?: return@runCatching emptyList()
            requests.whereEqualTo(InstantRequests.EMPLOYER_ID, uid)
                .orderBy(InstantRequests.CREATED_AT, Query.Direction.DESCENDING)
                .limit(30)
                .get().await().documents
                .mapNotNull { it.toInstantRequest() }
        }
    }

    /** Responses to the employer's recent needs (one subcollection query per need with responses). */
    suspend fun getEmployerInstantResponses(forRequests: List<InstantRequest>): Result<List<InstantResponse>> =
        withContext(Dispatchers.IO) {
            runCatching {
                coroutineScope {
                    forRequests.filter { it.responseCount > 0 }.map { request ->
                        async {
                            requests.document(request.requestId).collection(R.COLLECTION)
                                .orderBy(R.CREATED_AT, Query.Direction.DESCENDING)
                                .limit(50)
                                .get().await().documents
                                .map { it.toInstantResponse(request.requestId).withRequest(request, request.employerName) }
                        }
                    }.awaitAll().flatten()
                }
            }
        }

    /** accepted | rejected | completed | no_show */
    suspend fun updateEmployerInstantResponseStatus(response: InstantResponse, status: String): Result<Unit> = runCatching {
        call("setInstantResponseStatus", mapOf("requestId" to response.requestId, "workerId" to response.workerId, "status" to status))
        Unit
    }

    suspend fun markEmployerInstantRequestFilled(request: InstantRequest): Result<Unit> = runCatching {
        call("setInstantRequestStatus", mapOf("requestId" to request.requestId, "status" to "filled"))
        Unit
    }

    suspend fun cancelEmployerInstantRequest(request: InstantRequest): Result<Unit> = runCatching {
        call("setInstantRequestStatus", mapOf("requestId" to request.requestId, "status" to "cancelled"))
        Unit
    }

    suspend fun deleteEmployerInstantRequest(requestId: String): Result<Unit> = runCatching {
        call("deleteInstantRequest", mapOf("requestId" to requestId))
        Unit
    }

    /** Phone of a worker who responded to the employer's need. */
    suspend fun workerPhone(response: InstantResponse): Result<String> = runCatching {
        call("getInstantWorkerContact", mapOf("requestId" to response.requestId, "workerId" to response.workerId))
            ?.get("phone") as? String ?: error("Phone number unavailable")
    }

    // ─────────────────────────────── mapping ───────────────────────────────

    private suspend fun requestsById(ids: List<String>): Map<String, InstantRequest> = coroutineScope {
        ids.distinct().chunked(30).map { chunk ->
            async {
                requests.whereIn(FieldPath.documentId(), chunk).get().await().documents.mapNotNull { it.toInstantRequest() }
            }
        }.awaitAll().flatten().associateBy { it.requestId }
    }

    /** Display names of employers, one `in` query per 30 employers. */
    private suspend fun employerNames(ids: List<String>): Map<String, String> = runCatching {
        coroutineScope {
            ids.distinct().filter(String::isNotBlank).chunked(30).map { chunk ->
                async {
                    firestore.collection(EmployerCards.COLLECTION).whereIn(FieldPath.documentId(), chunk).limit(30)
                        .get().await().documents
                        .mapNotNull { doc -> doc.getString(EmployerCards.NAME)?.let { doc.id to it } }
                }
            }.awaitAll().flatten().toMap()
        }
    }.getOrDefault(emptyMap())

    private fun DocumentSnapshot.toInstantRequest(): InstantRequest? {
        val d = data ?: return null
        val pay = (d[InstantRequests.PAY_PER_PERSON] as? Number)?.toDouble() ?: 0.0
        val needed = (d[InstantRequests.WORKERS_NEEDED] as? Number)?.toInt() ?: 1
        val scheduledAt = d[InstantRequests.SCHEDULED_AT].epochMillis()
        val contact = d[InstantRequests.CONTACT_NUMBER] as? String ?: ""
        val category = d[InstantRequests.CATEGORY] as? String ?: JobCategory.OTHER.name
        return InstantRequest(
            requestId = id,
            employerId = d[InstantRequests.EMPLOYER_ID] as? String ?: "",
            employerPhone = contact,
            contactNumber = contact,
            title = d[InstantRequests.TITLE] as? String ?: "Urgent need",
            category = JobCategory.fromKey(category).displayName,
            workersNeeded = needed,
            needType = if (scheduledAt > 0) "scheduled" else "urgent_now",
            status = d[InstantRequests.STATUS] as? String ?: STATUS_OPEN,
            budgetText = if (pay > 0) "₹${pay.toInt()} per worker" else "",
            perPersonPayment = pay,
            totalPayment = pay * needed,
            durationText = d[InstantRequests.DURATION_TEXT] as? String ?: "",
            lat = (d[InstantRequests.LAT] as? Number)?.toDouble() ?: 0.0,
            lng = (d[InstantRequests.LNG] as? Number)?.toDouble() ?: 0.0,
            geohash = d[InstantRequests.GEOHASH] as? String ?: "",
            addressText = (d[InstantRequests.ADDRESS_TEXT] as? String).orEmpty().ifBlank { d[InstantRequests.AREA] as? String ?: "" },
            radiusKm = (d[InstantRequests.RADIUS_KM] as? Number)?.toDouble() ?: 5.0,
            scheduledAt = scheduledAt,
            createdAt = d[InstantRequests.CREATED_AT].epochMillis(),
            expiresAt = d[InstantRequests.EXPIRES_AT].epochMillis(),
            responseCount = (d[InstantRequests.RESPONSE_COUNT] as? Number)?.toInt() ?: 0,
            dispatchRadiusKm = (d[InstantRequests.DISPATCH_RADIUS_KM] as? Number)?.toInt() ?: 0,
            selectedWorkerIds = (d[InstantRequests.SELECTED_WORKER_IDS] as? List<*>)?.filterIsInstance<String>().orEmpty()
        )
    }

    private fun DocumentSnapshot.toInstantResponse(requestId: String): InstantResponse = InstantResponse(
        responseId = "${requestId}_$id",
        requestId = requestId,
        workerId = getString(R.WORKER_ID) ?: id,
        workerName = getString(R.WORKER_NAME) ?: "Worker",
        status = getString(R.STATUS) ?: "applied",
        createdAt = get(R.CREATED_AT).epochMillis(),
        respondedAt = get(R.CREATED_AT).epochMillis()
    )

    private fun InstantResponse.withRequest(request: InstantRequest, employerName: String?): InstantResponse = copy(
        employerId = request.employerId,
        requestTitle = request.title,
        requestCategory = request.category,
        employerName = employerName ?: request.employerName,
        employerPhone = request.contactNumber,
        budgetText = request.budgetText,
        addressText = request.addressText
    )

    private companion object {
        const val STATUS_OPEN = "open"
        const val MAX_RADIUS_KM = 20.0
        val BANDS_KM = listOf(5.0, 10.0, MAX_RADIUS_KM)
        const val CELL_PRECISION = 5
        const val MAX_SHOWN = 10
    }
}
