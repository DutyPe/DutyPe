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

/** [skill] BASIC (cleaning, help, wash: anyone careful) or SKILLED (wiring, AC, plumbing...: skill-checked). */
data class ServiceCategory(
    val id: String,
    val name: String,
    val te: String,
    val hi: String,
    val skill: String = "SKILLED",
    val imageUrl: String? = null
) {
    val isSkilled: Boolean get() = skill != "BASIC"

    val resolvedImageUrl: String
        get() = if (!imageUrl.isNullOrBlank()) imageUrl
        else "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/categories%2F${id.lowercase()}.webp?alt=media"

    fun label(lang: String): String = when (lang) {
        "te" -> te.ifBlank { name }
        "hi" -> hi.ifBlank { name }
        else -> name
    }
}

data class ServiceOption(
    val id: String,
    val title: String,
    val price: Int,
    val originalPrice: Int = 0,
    val durationMin: Int = 0,
    val description: String = ""
) {
    val mrp: Int get() = if (originalPrice > price) originalPrice else (price * 1.25).toInt()
    val discountPercent: Int get() = if (mrp > price) Math.round(((mrp - price).toFloat() / mrp) * 100) else 0
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
    val includes: String,
    /** Item ids the customer keeps ready (broom, mop, ladder...). */
    val provide: List<String> = emptyList(),
    /** Item ids the partner brings (tool kit, tester...). */
    val bring: List<String> = emptyList(),
    val imageUrl: String? = null,
    val originalPrice: Int = 0,
    val rating: Double = 4.8,
    val ratingCount: String = "12k+",
    val options: List<ServiceOption> = emptyList(),
    val inclusions: List<String> = emptyList(),
    val exclusions: List<String> = emptyList()
) {
    val resolvedImageUrl: String
        get() = if (!imageUrl.isNullOrBlank()) imageUrl
        else "https://firebasestorage.googleapis.com/v0/b/dutype-860ac.firebasestorage.app/o/services%2F${id.lowercase()}.webp?alt=media"

    /** Real MRP / strike-through price: uses admin originalPrice if set, or defaults to 25% markup. */
    val mrp: Int get() = if (originalPrice > price) originalPrice else (price * 1.25).toInt()

    /** Percentage discount off MRP (e.g. 33% off). Returns 0 if no discount. */
    val discountPercent: Int get() = if (mrp > price) Math.round(((mrp - price).toFloat() / mrp) * 100) else 0

    /** True if this service offers multiple selectable variants/options (e.g. 1 AC vs 2 ACs) */
    val hasMultipleOptions: Boolean get() = options.size > 1

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
    val offers: List<PromoOffer> = emptyList(),
    val promoBanners: List<PromoBanner> = emptyList(),
    val noteworthyServiceIds: List<String> = emptyList(),
    val mostBookedServiceIds: List<String> = emptyList(),
    /** Item id → label per language (en / te / hi). */
    val items: Map<String, Map<String, String>> = emptyMap()
) {
    fun feeFor(service: ServiceItem): Int = if (service.inspection) inspectionFee else bookingFee

    /** Labels for item ids in [lang] (falls back to English, then the id). */
    fun itemLabels(ids: List<String>, lang: String): List<String> =
        ids.map { id -> items[id]?.let { it[lang] ?: it["en"] } ?: id.lowercase().replace('_', ' ') }

    val noteworthyServices: List<ServiceItem>
        get() {
            if (noteworthyServiceIds.isNotEmpty()) {
                val found = noteworthyServiceIds.mapNotNull { id -> services.firstOrNull { it.id.equals(id, ignoreCase = true) } }
                if (found.isNotEmpty()) return found
            }
            return services.take(6)
        }

    val mostBookedServices: List<ServiceItem>
        get() {
            if (mostBookedServiceIds.isNotEmpty()) {
                val found = mostBookedServiceIds.mapNotNull { id -> services.firstOrNull { it.id.equals(id, ignoreCase = true) } }
                if (found.isNotEmpty()) return found
            }
            return services.take(8)
        }
}

/** Dynamic promotional banner configurable by admin for header carousel. */
data class PromoBanner(
    val id: String = "",
    val headline: String = "",
    val subheadline: String = "",
    val cta: String = "Know more",
    val imageUrl: String? = null,
    val bgStartColor: String = "#0F172A",
    val bgEndColor: String = "#1E3A8A",
    val targetServiceId: String? = null,
    val targetCategoryId: String? = null,
    val imageScale: Float = 1.0f
)

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
    val completedAt: Long = 0,
    val cancellationFee: Int = 0,
    val cancellationReason: String = "",
    val cancellationNotice: String = ""
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
    val discount: Int = 0,
    val bring: List<String> = emptyList(),
    val provide: List<String> = emptyList()
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
    fun getCachedCatalog(): ServicesCatalog? = cachedCatalog?.second

    val uid: String? get() = auth.currentUser?.uid

    private suspend fun call(name: String, data: Map<String, Any?> = emptyMap()): Map<String, Any?> =
        functions.getHttpsCallable(name).call(data).await().data.asMap()

    suspend fun catalog(force: Boolean = false): ServicesCatalog {
        cachedCatalog?.takeIf { !force && System.currentTimeMillis() - it.first < CATALOG_TTL_MS }?.let { return it.second }
        val d = call("getServiceCatalog")

        // Real-time overlay from Firestore app_config/services (images, prices, originalPrice)
        val firestoreConfig = runCatching {
            firestore.collection("app_config").document("services").get().await()
        }.getOrNull()

        val catMap = mutableMapOf<String, Map<String, Any?>>()
        val svcOverrides = mutableMapOf<String, Map<String, Any?>>()
        if (firestoreConfig != null && firestoreConfig.exists()) {
            val rawCats = firestoreConfig.get("categories") as? List<*>
            rawCats?.forEach { c ->
                val m = c as? Map<*, *> ?: return@forEach
                val id = m["id"] as? String ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                catMap[id.uppercase()] = m as Map<String, Any?>
            }
            val rawSvcs = firestoreConfig.get("services") as? List<*>
            rawSvcs?.forEach { s ->
                val m = s as? Map<*, *> ?: return@forEach
                val id = m["id"] as? String ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                svcOverrides[id.lowercase()] = m as Map<String, Any?>
            }
        }

        fun parseOptions(raw: Any?): List<ServiceOption> {
            val list = raw as? List<*> ?: return emptyList()
            return list.mapNotNull { item ->
                val m = item as? Map<*, *> ?: return@mapNotNull null
                val optId = m["id"] as? String ?: m["optionId"] as? String ?: return@mapNotNull null
                val title = m["title"] as? String ?: m["name"] as? String ?: optId
                val price = (m["price"] as? Number)?.toInt() ?: 0
                val origPrice = (m["originalPrice"] as? Number)?.toInt() ?: 0
                val duration = (m["durationMin"] as? Number)?.toInt() ?: 0
                val desc = m["description"] as? String ?: ""
                ServiceOption(
                    id = optId,
                    title = title,
                    price = price,
                    originalPrice = origPrice,
                    durationMin = duration,
                    description = desc
                )
            }
        }

        val seenCatIds = mutableSetOf<String>()
        val categories = mutableListOf<ServiceCategory>()

        @Suppress("UNCHECKED_CAST")
        (d["categories"] as? List<Any?>).orEmpty().map { it.asMap() }.forEach { catRaw ->
            val catId = catRaw.str("id")
            val catOv = catMap[catId.uppercase()]
            val img = (catOv?.get("imageUrl") as? String)?.ifBlank { null } ?: catRaw.str("imageUrl").ifBlank { null }
            val name = (catOv?.get("name") as? String)?.ifBlank { null } ?: catRaw.str("name")
            val te = (catOv?.get("te") as? String)?.ifBlank { null } ?: catRaw.str("te")
            val hi = (catOv?.get("hi") as? String)?.ifBlank { null } ?: catRaw.str("hi")
            val skill = (catOv?.get("skill") as? String)?.ifBlank { null } ?: catRaw.str("skill").ifBlank { "SKILLED" }
            categories.add(
                ServiceCategory(
                    id = catId,
                    name = name,
                    te = te,
                    hi = hi,
                    skill = skill,
                    imageUrl = img
                )
            )
            seenCatIds.add(catId.uppercase())
        }

        // Merge any newly added categories from Firestore that weren't in cloud function!
        catMap.forEach { (catIdUpper, catData) ->
            if (!seenCatIds.contains(catIdUpper)) {
                val catId = (catData["id"] as? String) ?: catIdUpper
                val img = (catData["imageUrl"] as? String)?.ifBlank { null }
                val name = (catData["name"] as? String) ?: catId
                val te = (catData["te"] as? String) ?: name
                val hi = (catData["hi"] as? String) ?: name
                val skill = (catData["skill"] as? String) ?: "SKILLED"
                categories.add(
                    ServiceCategory(
                        id = catId,
                        name = name,
                        te = te,
                        hi = hi,
                        skill = skill,
                        imageUrl = img
                    )
                )
                seenCatIds.add(catIdUpper)
            }
        }

        val seenSvcIds = mutableSetOf<String>()
        val services = mutableListOf<ServiceItem>()

        @Suppress("UNCHECKED_CAST")
        (d["services"] as? List<Any?>).orEmpty().map { it.asMap() }.forEach { svcRaw ->
            val svcId = svcRaw.str("id")
            val svcOv = svcOverrides[svcId.lowercase()]
            val overrideImg = (svcOv?.get("imageUrl") as? String)?.ifBlank { null }
            val overridePrice = (svcOv?.get("price") as? Number)?.toInt()
            val overrideOrigPrice = (svcOv?.get("originalPrice") as? Number)?.toInt()
            val img = overrideImg ?: svcRaw.str("imageUrl").ifBlank { null }
            val parsedOptions = parseOptions(svcOv?.get("options") ?: svcRaw["options"])
            val inclusions = (svcOv?.get("inclusions") as? List<*>)?.mapNotNull { it?.toString() }
                ?: (svcRaw["inclusions"] as? List<*>)?.mapNotNull { it?.toString() }
                ?: emptyList()
            val exclusions = (svcOv?.get("exclusions") as? List<*>)?.mapNotNull { it?.toString() }
                ?: (svcRaw["exclusions"] as? List<*>)?.mapNotNull { it?.toString() }
                ?: emptyList()
            val rating = (svcOv?.get("rating") as? Number)?.toDouble() ?: 4.8
            val ratingCount = (svcOv?.get("ratingCount") as? String) ?: "12k+"

            services.add(
                ServiceItem(
                    id = svcId,
                    category = (svcOv?.get("category") as? String) ?: svcRaw.str("category"),
                    name = (svcOv?.get("name") as? String) ?: svcRaw.str("name"),
                    te = (svcOv?.get("te") as? String) ?: svcRaw.str("te"),
                    hi = (svcOv?.get("hi") as? String) ?: svcRaw.str("hi"),
                    price = overridePrice ?: svcRaw.int("price"),
                    durationMin = (svcOv?.get("durationMin") as? Number)?.toInt() ?: svcRaw.int("durationMin"),
                    inspection = (svcOv?.get("inspection") as? Boolean) ?: (svcRaw["inspection"] == true),
                    includes = (svcOv?.get("includes") as? String) ?: svcRaw.str("includes"),
                    provide = (svcRaw["provide"] as? List<Any?>).orEmpty().map { x -> x.toString() },
                    bring = (svcRaw["bring"] as? List<Any?>).orEmpty().map { x -> x.toString() },
                    imageUrl = img,
                    originalPrice = overrideOrigPrice ?: svcRaw.int("originalPrice"),
                    rating = rating,
                    ratingCount = ratingCount,
                    options = parsedOptions,
                    inclusions = inclusions,
                    exclusions = exclusions
                )
            )
            seenSvcIds.add(svcId.lowercase())
        }

        // Merge any newly added services from Firestore that weren't in cloud function!
        svcOverrides.forEach { (svcIdLower, svcData) ->
            if (!seenSvcIds.contains(svcIdLower)) {
                val svcId = (svcData["id"] as? String) ?: svcIdLower
                val img = (svcData["imageUrl"] as? String)?.ifBlank { null }
                val price = (svcData["price"] as? Number)?.toInt() ?: 299
                val origPrice = (svcData["originalPrice"] as? Number)?.toInt() ?: 0
                val duration = (svcData["durationMin"] as? Number)?.toInt() ?: 60
                val parsedOptions = parseOptions(svcData["options"])
                val inclusions = (svcData["inclusions"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val exclusions = (svcData["exclusions"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                services.add(
                    ServiceItem(
                        id = svcId,
                        category = (svcData["category"] as? String) ?: "CLEANING",
                        name = (svcData["name"] as? String) ?: svcId,
                        te = (svcData["te"] as? String) ?: (svcData["name"] as? String) ?: svcId,
                        hi = (svcData["hi"] as? String) ?: (svcData["name"] as? String) ?: svcId,
                        price = price,
                        durationMin = duration,
                        inspection = svcData["inspection"] == true,
                        includes = (svcData["includes"] as? String) ?: "",
                        provide = emptyList(),
                        bring = emptyList(),
                        imageUrl = img,
                        originalPrice = origPrice,
                        rating = 4.8,
                        ratingCount = "New",
                        options = parsedOptions,
                        inclusions = inclusions,
                        exclusions = exclusions
                    )
                )
                seenSvcIds.add(svcIdLower)
            }
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
            offers = offersOf(firestoreConfig?.get("offers") ?: d["offers"]),
            promoBanners = bannersOf(firestoreConfig?.get("promoBanners") ?: firestoreConfig?.get("banners") ?: d["promoBanners"] ?: d["banners"]),
            noteworthyServiceIds = (firestoreConfig?.get("noteworthyServiceIds") as? List<*>)?.mapNotNull { it?.toString() }
                ?: (d["noteworthyServiceIds"] as? List<*>)?.mapNotNull { it?.toString() }
                ?: emptyList(),
            mostBookedServiceIds = (firestoreConfig?.get("mostBookedServiceIds") as? List<*>)?.mapNotNull { it?.toString() }
                ?: (d["mostBookedServiceIds"] as? List<*>)?.mapNotNull { it?.toString() }
                ?: emptyList(),
            items = d["items"].asMap().mapValues { (_, v) -> v.asMap().mapValues { (_, t) -> t.toString() } }
        )
        cachedCatalog = System.currentTimeMillis() to catalog
        return catalog
    }

    fun invalidateCatalogCache() {
        cachedCatalog = null
    }

    /** Real-time flow of app_config/services document for instantaneous updates on banners and offers */
    fun observeFirestoreServicesConfig(): Flow<DocumentSnapshot?> = callbackFlow {
        val reg = firestore.collection("app_config").document("services")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Timber.w(err, "app_config/services listener error")
                    return@addSnapshotListener
                }
                trySend(snap)
            }
        awaitClose { reg.remove() }
    }

    fun bannersFromSnapshot(snap: DocumentSnapshot): List<PromoBanner> =
        bannersOf(snap.get("promoBanners") ?: snap.get("banners"))

    fun mostBookedServiceIdsFromSnapshot(snap: DocumentSnapshot): List<String> =
        (snap.get("mostBookedServiceIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

    fun offersFromSnapshot(snap: DocumentSnapshot): List<PromoOffer> =
        offersOf(snap.get("offers"))

    @Suppress("UNCHECKED_CAST")
    internal fun bannersOf(raw: Any?): List<PromoBanner> = (raw as? List<Any?>).orEmpty().map { it.asMap() }.map {
        PromoBanner(
            id = it.str("id"),
            headline = it.str("headline"),
            subheadline = it.str("subheadline"),
            cta = it.str("cta").ifBlank { "Know more" },
            imageUrl = it["imageUrl"] as? String,
            bgStartColor = it.str("bgStartColor").ifBlank { "#0F172A" },
            bgEndColor = it.str("bgEndColor").ifBlank { "#1E3A8A" },
            targetServiceId = it["targetServiceId"] as? String,
            targetCategoryId = it["targetCategoryId"] as? String,
            imageScale = (it["imageScale"] as? Number)?.toFloat() ?: 1.0f
        )
    }.filter { it.headline.isNotBlank() }

    @Suppress("UNCHECKED_CAST")
    internal fun offersOf(raw: Any?): List<PromoOffer> = (raw as? List<Any?>).orEmpty().map { it.asMap() }.map {
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

    suspend fun cancel(bookingId: String, reason: String = "") {
        val payload = mutableMapOf<String, Any?>("bookingId" to bookingId)
        if (reason.isNotBlank()) payload["reason"] = reason
        call("cancelServiceBooking", payload)
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

    suspend fun apply(
        categories: List<String>, experienceYears: Int, area: String, note: String, lat: Double, lng: Double,
        skillProof: String = "", acceptGuidelines: Boolean = false
    ) {
        call(
            "applyServicePartner",
            mapOf(
                "categories" to categories, "experienceYears" to experienceYears, "area" to area, "note" to note,
                "lat" to lat, "lng" to lng, "skillProof" to skillProof, "acceptGuidelines" to acceptGuidelines
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
            discount = d.int("discount"),
            bring = (d["bring"] as? List<Any?>).orEmpty().map { it.toString() },
            provide = (d["provide"] as? List<Any?>).orEmpty().map { it.toString() }
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
        completedAt = d.getTimestamp("completedAt")?.toDate()?.time ?: 0L,
        cancellationFee = (d.getLong("cancellationFee") ?: 0L).toInt(),
        cancellationReason = d.getString("cancelReason").orEmpty(),
        cancellationNotice = d.getString("cancellationNotice").orEmpty()
    )

    companion object {
        const val BOOKINGS = "service_bookings"
        const val PARTNERS = "service_partners"
        const val CATALOG_TTL_MS = 10 * 60 * 1000L
        @Volatile private var cachedCatalog: Pair<Long, ServicesCatalog>? = null
    }
}
