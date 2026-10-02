package com.example.dutype.homeservices

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DutyPe Services (Urban Company style home services). Server: functions/src/services.ts.
 *
 * Customers (employer accounts) book fixed-price services; verified partners (worker accounts)
 * get offers, do the job and pay DutyPe's booking fee + ₹19 partner fee from prepaid credits.
 * Offers: the first booking has no booking fee; admin coupons (festive offers) can replace that.
 * All writes go through Cloud Functions; the app only reads its own bookings and partner data.
 */

object BookingStatus {
    const val SEARCHING = "SEARCHING"
    const val ASSIGNED = "ASSIGNED"
    const val ON_THE_WAY = "ON_THE_WAY"
    const val STARTED = "STARTED"
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
    const val NO_PARTNER = "NO_PARTNER"

    val OPEN = setOf(SEARCHING, ASSIGNED, ON_THE_WAY, STARTED)
}

object PartnerStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
    const val SUSPENDED = "SUSPENDED"
}

data class ServiceCategory(val id: String, val name: String, val te: String, val hi: String) {
    fun label(lang: String): String = when (lang) {
        "te" -> te.ifBlank { name }
        "hi" -> hi.ifBlank { name }
        else -> name
    }
}

data class ServiceItem(
    val id: String,
    val category: String,
    val name: String,
    val te: String,
    val hi: String,
    val price: Int,
    val durationMin: Int,
    val inspection: Boolean,
    val includes: String
) {
    fun label(lang: String): String = when (lang) {
        "te" -> te.ifBlank { name }
        "hi" -> hi.ifBlank { name }
        else -> name
    }
}

data class ServicesCatalog(
    val city: String,
    val bookingFee: Int,
    val inspectionFee: Int,
    val commissionPct: Int,
    val upiId: String,
    val upiName: String,
    val minTopup: Int,
    val categories: List<ServiceCategory>,
    val services: List<ServiceItem>,
    val partnerFee: Int = 19,
    val partnerFirstJobFree: Boolean = true,
    val firstBookingFeeFree: Boolean = false,
    val offers: List<PromoOffer> = emptyList()
) {
    fun feeFor(service: ServiceItem): Int = if (service.inspection) inspectionFee else bookingFee
}

/** A coupon / festive offer the app can show. [type] FLAT (₹[value]) or PCT ([value]%, up to ₹[maxOff]). */
data class PromoOffer(
    val code: String,
    val title: String,
    val type: String = "FLAT",
    val value: Int = 0,
    val maxOff: Int = 0,
    val minOrder: Int = 0,
    val validTo: Long = 0,
    val firstBookingOnly: Boolean = false,
    val categories: List<String> = emptyList()
) {
    /** "₹50 OFF" / "20% OFF" (empty when the server sent only a title). */
    val headline: String get() = when {
        value <= 0 -> ""
        type == "PCT" -> "$value% OFF"
        else -> "₹$value OFF"
    }
}

/** The price the customer will pay, from previewServiceQuote. */
data class ServiceQuote(
    val price: Int,
    val bookingFee: Int,
    val discount: Int,
    val discountLabel: String,
    val couponCode: String,
    val total: Int,
    val couponError: String,
    val couponNote: String,
    val firstBooking: Boolean,
    val offers: List<PromoOffer>
)

/** A live list with its load state, so screens can tell "loading", "empty" and "failed" apart. */
data class BookingsLoad(val loaded: Boolean, val list: List<ServiceBooking>, val error: String?)

data class BookingLoad(val loaded: Boolean, val booking: ServiceBooking?, val error: String?)

data class ServiceBooking(
    val id: String,
    val status: String,
    val category: String,
    val serviceName: String,
    val price: Int,
    val bookingFee: Int,
    val total: Int,
    val extras: Int,
    val extrasNote: String,
    val inspection: Boolean,
    val addressText: String,
    val area: String,
    val lat: Double,
    val lng: Double,
    val note: String,
    val whenType: String,
    val scheduledAt: Long,
    val customerName: String,
    val customerPhone: String,
    val partnerId: String,
    val partnerName: String,
    val partnerPhone: String,
    val partnerRating: Double,
    val rating: Int,
    val createdAt: Long,
    val discount: Int = 0,
    val discountLabel: String = "",
    val couponCode: String = "",
    val partnerFee: Int = -1,
    val assignedAt: Long = 0,
    val startedAt: Long = 0,
    val completedAt: Long = 0
) {
    val isOpen: Boolean get() = status in BookingStatus.OPEN
}

data class PartnerProfile(
    val status: String,
    val name: String,
    val categories: List<String>,
    val online: Boolean,
    val creditsPaise: Long,
    val activeBookingId: String,
    val ratingAvg: Double,
    val ratingCount: Int,
    val jobsCompleted: Int,
    val rejectionReason: String
)

data class PartnerTopup(val id: String, val amountPaise: Long, val utr: String, val status: String, val createdAt: Long)

data class ServiceOffer(
    val available: Boolean,
    val mine: Boolean,
    val status: String,
    val serviceName: String,
    val price: Int,
    val bookingFee: Int,
    val earning: Int,
    val requiredCreditsPaise: Long,
    val creditsPaise: Long,
    val inspection: Boolean,
    val area: String,
    val note: String,
    val whenType: String,
    val scheduledAt: Long,
    val distanceKm: Double?,
    /** What the customer pays the partner (after any offer). */
    val customerTotal: Int = 0,
    val discount: Int = 0
)

/** accepted | taken | closed | busy | low_credits | not_partner */
data class AcceptResult(val result: String)

/** Callable results are JSON objects: read them as String-keyed maps. */
@Suppress("UNCHECKED_CAST")
private fun Any?.asMap(): Map<String, Any?> = this as? Map<String, Any?> ?: emptyMap()

private fun Map<String, Any?>.str(key: String): String = this[key] as? String ?: ""
private fun Map<String, Any?>.int(key: String): Int = (this[key] as? Number)?.toInt() ?: 0
private fun Map<String, Any?>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L

@Singleton
class HomeServicesRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    @Volatile private var cachedCatalog: Pair<Long, ServicesCatalog>? = null

    val uid: String? get() = auth.currentUser?.uid

    private suspend fun call(name: String, data: Map<String, Any?> = emptyMap()): Map<String, Any?> =
        functions.getHttpsCallable(name).call(data).await().data.asMap()

    suspend fun catalog(force: Boolean = false): ServicesCatalog {
        cachedCatalog?.takeIf { !force && System.currentTimeMillis() - it.first < CATALOG_TTL_MS }?.let { return it.second }
        val d = call("getServiceCatalog")
        @Suppress("UNCHECKED_CAST")
        val categories = (d["categories"] as? List<Any?>).orEmpty().map { it.asMap() }.map {
            ServiceCategory(it.str("id"), it.str("name"), it.str("te"), it.str("hi"))
        }
        @Suppress("UNCHECKED_CAST")
        val services = (d["services"] as? List<Any?>).orEmpty().map { it.asMap() }.map {
            ServiceItem(
                id = it.str("id"),
                category = it.str("category"),
                name = it.str("name"),
                te = it.str("te"),
                hi = it.str("hi"),
                price = it.int("price"),
                durationMin = it.int("durationMin"),
                inspection = it["inspection"] == true,
                includes = it.str("includes")
            )
        }
        val catalog = ServicesCatalog(
            city = d.str("city").ifBlank { "Khammam" },
            bookingFee = d.int("bookingFee"),
            inspectionFee = d.int("inspectionFee"),
            commissionPct = d.int("commissionPct"),
            upiId = d.str("upiId"),
            upiName = d.str("upiName").ifBlank { "DutyPe" },
            minTopup = d.int("minTopup").coerceAtLeast(1),
            categories = categories,
            services = services,
            partnerFee = if (d.containsKey("partnerFee")) d.int("partnerFee") else 19,
            partnerFirstJobFree = d["partnerFirstJobFree"] != false,
            firstBookingFeeFree = d["firstBookingFeeFree"] == true,
            offers = offersOf(d["offers"])
        )
        cachedCatalog = System.currentTimeMillis() to catalog
        return catalog
    }

    @Suppress("UNCHECKED_CAST")
    private fun offersOf(raw: Any?): List<PromoOffer> = (raw as? List<Any?>).orEmpty().map { it.asMap() }.map {
        PromoOffer(
            code = it.str("code"),
            title = it.str("title"),
            type = it.str("type").ifBlank { "FLAT" },
            value = it.int("value"),
            maxOff = it.int("maxOff"),
            minOrder = it.int("minOrder"),
            validTo = it.long("validTo"),
            firstBookingOnly = it["firstBookingOnly"] == true,
            categories = (it["categories"] as? List<Any?>).orEmpty().map { c -> c.toString() }
        )
    }.filter { it.code.isNotBlank() }

    /** What the customer pays for [serviceId] with an optional coupon (nothing is booked). */
    suspend fun previewQuote(serviceId: String, couponCode: String): ServiceQuote {
        val d = call("previewServiceQuote", mapOf("serviceId" to serviceId, "couponCode" to couponCode))
        return ServiceQuote(
            price = d.int("price"),
            bookingFee = d.int("bookingFee"),
            discount = d.int("discount"),
            discountLabel = d.str("discountLabel"),
            couponCode = d.str("couponCode"),
            total = d.int("total"),
            couponError = d.str("couponError"),
            couponNote = d.str("couponNote"),
            firstBooking = d["firstBooking"] == true,
            offers = offersOf(d["offers"])
        )
    }

    private val areaCache = mutableMapOf<String, Boolean>()

    /** Is this point inside the DutyPe Services area (Khammam district at launch)? Cached per ~1 km. */
    suspend fun isInServiceArea(lat: Double, lng: Double): Boolean {
        val key = "%.2f,%.2f".format(java.util.Locale.US, lat, lng)
        areaCache[key]?.let { return it }
        val inArea = call("getServiceCatalog", mapOf("lat" to lat, "lng" to lng))["inArea"] == true
        areaCache[key] = inArea
        return inArea
    }

    // ─────────────────────────── customer ───────────────────────────

    /** Returns the new booking id. [scheduledAt] null = "now". */
    suspend fun createBooking(
        serviceId: String,
        addressText: String,
        area: String,
        lat: Double,
        lng: Double,
        note: String,
        scheduledAt: Long?,
        couponCode: String = ""
    ): String {
        val d = call(
            "createServiceBooking",
            mapOf(
                "serviceId" to serviceId,
                "addressText" to addressText,
                "area" to area,
                "lat" to lat,
                "lng" to lng,
                "note" to note,
                "when" to if (scheduledAt != null) "scheduled" else "now",
                "scheduledAt" to scheduledAt,
                "couponCode" to couponCode
            )
        )
        return d.str("bookingId")
    }

    fun observeMyBookings(): Flow<List<ServiceBooking>> {
        val me = uid ?: return flowOf(emptyList())
        return observeList(
            firestore.collection(BOOKINGS).whereEqualTo("customerId", me)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(30)
        )
    }

    /** [observeMyBookings] with loading / error state. */
    fun observeMyBookingsLoad(): Flow<BookingsLoad> = callbackFlow {
        val me = uid
        if (me == null) {
            trySend(BookingsLoad(true, emptyList(), "Please sign in again"))
            close()
            return@callbackFlow
        }
        val reg = firestore.collection(BOOKINGS).whereEqualTo("customerId", me)
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Timber.w(err, "bookings listener failed")
                    trySend(BookingsLoad(true, emptyList(), err.message ?: "error"))
                } else {
                    trySend(BookingsLoad(true, snap?.documents.orEmpty().map(::toBooking), null))
                }
            }
        awaitClose { reg.remove() }
    }

    fun observeBookingLoad(bookingId: String): Flow<BookingLoad> = callbackFlow {
        val reg = firestore.collection(BOOKINGS).document(bookingId).addSnapshotListener { snap, err ->
            if (err != null) {
                Timber.w(err, "booking listener failed")
                trySend(BookingLoad(true, null, err.message ?: "error"))
            } else {
                trySend(BookingLoad(true, snap?.takeIf { it.exists() }?.let(::toBooking), null))
            }
        }
        awaitClose { reg.remove() }
    }

    fun observeBooking(bookingId: String): Flow<ServiceBooking?> = callbackFlow {
        val reg = firestore.collection(BOOKINGS).document(bookingId).addSnapshotListener { snap, err ->
            if (err != null) {
                Timber.w(err, "booking listener failed")
                trySend(null)
            } else {
                trySend(snap?.takeIf { it.exists() }?.let(::toBooking))
            }
        }
        awaitClose { reg.remove() }
    }

    /** The 4-digit start code (only the customer can read it). */
    suspend fun startCode(bookingId: String): String =
        firestore.collection("service_booking_secrets").document(bookingId).get().await().getString("startOtp").orEmpty()

    suspend fun cancel(bookingId: String) {
        call("cancelServiceBooking", mapOf("bookingId" to bookingId))
    }

    suspend fun rate(bookingId: String, stars: Int, review: String) {
        call("rateServiceBooking", mapOf("bookingId" to bookingId, "stars" to stars, "review" to review))
    }

    // ─────────────────────────── partner ───────────────────────────

    fun observePartner(): Flow<PartnerProfile?> = callbackFlow {
        val me = uid
        if (me == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val reg = firestore.collection(PARTNERS).document(me).addSnapshotListener { snap, err ->
            if (err != null) {
                Timber.w(err, "partner listener failed")
                trySend(null)
            } else if (snap == null || !snap.exists()) {
                trySend(null)
            } else {
                val count = (snap.getLong("ratingCount") ?: 0L).toInt()
                val sum = snap.getDouble("ratingSum") ?: 0.0
                @Suppress("UNCHECKED_CAST")
                trySend(
                    PartnerProfile(
                        status = snap.getString("status").orEmpty(),
                        name = snap.getString("name").orEmpty(),
                        categories = (snap.get("categories") as? List<String>).orEmpty(),
                        online = snap.getBoolean("online") == true,
                        creditsPaise = snap.getLong("creditsPaise") ?: 0L,
                        activeBookingId = snap.getString("activeBookingId").orEmpty(),
                        ratingAvg = if (count > 0) sum / count else 0.0,
                        ratingCount = count,
                        jobsCompleted = (snap.getLong("jobsCompleted") ?: 0L).toInt(),
                        rejectionReason = snap.getString("rejectionReason").orEmpty()
                    )
                )
            }
        }
        awaitClose { reg.remove() }
    }

    fun observePartnerJobs(): Flow<List<ServiceBooking>> {
        val me = uid ?: return flowOf(emptyList())
        return observeList(
            firestore.collection(BOOKINGS).whereEqualTo("partnerId", me)
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(30)
        )
    }

    fun observeTopups(): Flow<List<PartnerTopup>> = callbackFlow {
        val me = uid
        if (me == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = firestore.collection("partner_topups").whereEqualTo("partnerId", me)
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(10)
            .addSnapshotListener { snap, err ->
                if (err != null) Timber.w(err, "topups listener failed")
                trySend(snap?.documents.orEmpty().map {
                    PartnerTopup(
                        id = it.id,
                        amountPaise = it.getLong("amountPaise") ?: 0L,
                        utr = it.getString("utrNumber").orEmpty(),
                        status = it.getString("status").orEmpty(),
                        createdAt = it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                })
            }
        awaitClose { reg.remove() }
    }

    suspend fun apply(categories: List<String>, experienceYears: Int, area: String, note: String, lat: Double, lng: Double) {
        call(
            "applyServicePartner",
            mapOf(
                "categories" to categories, "experienceYears" to experienceYears, "area" to area, "note" to note,
                "lat" to lat, "lng" to lng
            )
        )
    }

    suspend fun setOnline(online: Boolean, lat: Double?, lng: Double?) {
        val data = mutableMapOf<String, Any?>("online" to online)
        if (lat != null && lng != null) {
            data["lat"] = lat
            data["lng"] = lng
        }
        call("setPartnerOnline", data)
    }

    suspend fun offer(bookingId: String): ServiceOffer {
        val d = call("getServiceOffer", mapOf("bookingId" to bookingId))
        return ServiceOffer(
            available = d["available"] == true,
            mine = d["mine"] == true,
            status = d.str("status"),
            serviceName = d.str("serviceName"),
            price = d.int("price"),
            bookingFee = d.int("bookingFee"),
            earning = d.int("earning"),
            requiredCreditsPaise = d.long("requiredCreditsPaise"),
            creditsPaise = d.long("creditsPaise"),
            inspection = d["inspection"] == true,
            area = d.str("area"),
            note = d.str("note"),
            whenType = d.str("when"),
            scheduledAt = d.long("scheduledAt"),
            distanceKm = (d["distanceKm"] as? Number)?.toDouble(),
            customerTotal = d.int("customerTotal").takeIf { it > 0 } ?: (d.int("price") + d.int("bookingFee")),
            discount = d.int("discount")
        )
    }

    suspend fun accept(bookingId: String): AcceptResult =
        AcceptResult(call("acceptServiceBooking", mapOf("bookingId" to bookingId)).str("result"))

    /** action: on_the_way | start | complete | cancel */
    suspend fun update(bookingId: String, action: String, otp: String = "", extras: Int = 0, extrasNote: String = "") {
        call(
            "updateServiceBooking",
            mapOf("bookingId" to bookingId, "action" to action, "otp" to otp, "extras" to extras, "extrasNote" to extrasNote)
        )
    }

    suspend fun requestTopup(amount: Int, utr: String) {
        call("requestPartnerTopup", mapOf("amount" to amount, "utr" to utr))
    }

    // ─────────────────────────── mapping ───────────────────────────

    private fun observeList(query: Query): Flow<List<ServiceBooking>> = callbackFlow {
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) Timber.w(err, "bookings listener failed")
            trySend(snap?.documents.orEmpty().map(::toBooking))
        }
        awaitClose { reg.remove() }
    }

    private fun toBooking(d: DocumentSnapshot): ServiceBooking = ServiceBooking(
        id = d.id,
        status = d.getString("status").orEmpty(),
        category = d.getString("category").orEmpty(),
        serviceName = d.getString("serviceName").orEmpty(),
        price = (d.getLong("price") ?: 0L).toInt(),
        bookingFee = (d.getLong("bookingFee") ?: 0L).toInt(),
        total = (d.getLong("total") ?: 0L).toInt(),
        extras = (d.getLong("extras") ?: 0L).toInt(),
        extrasNote = d.getString("extrasNote").orEmpty(),
        inspection = d.getBoolean("inspection") == true,
        addressText = d.getString("addressText").orEmpty(),
        area = d.getString("area").orEmpty(),
        lat = d.getDouble("lat") ?: 0.0,
        lng = d.getDouble("lng") ?: 0.0,
        note = d.getString("note").orEmpty(),
        whenType = d.getString("when").orEmpty(),
        scheduledAt = d.getTimestamp("scheduledAt")?.toDate()?.time ?: 0L,
        customerName = d.getString("customerName").orEmpty(),
        customerPhone = d.getString("customerPhone").orEmpty(),
        partnerId = d.getString("partnerId").orEmpty(),
        partnerName = d.getString("partnerName").orEmpty(),
        partnerPhone = d.getString("partnerPhone").orEmpty(),
        partnerRating = d.getDouble("partnerRating") ?: 0.0,
        rating = (d.getLong("rating") ?: 0L).toInt(),
        createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        discount = (d.getLong("discount") ?: 0L).toInt(),
        discountLabel = d.getString("discountLabel").orEmpty(),
        couponCode = d.getString("couponCode").orEmpty(),
        partnerFee = d.getLong("partnerFee")?.toInt() ?: -1,
        assignedAt = d.getTimestamp("assignedAt")?.toDate()?.time ?: 0L,
        startedAt = d.getTimestamp("startedAt")?.toDate()?.time ?: 0L,
        completedAt = d.getTimestamp("completedAt")?.toDate()?.time ?: 0L
    )

    private companion object {
        const val BOOKINGS = "service_bookings"
        const val PARTNERS = "service_partners"
        const val CATALOG_TTL_MS = 10 * 60 * 1000L
    }
}
