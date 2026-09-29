package com.example.dutype.utils

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Utility functions for Firestore database operations.
 */
object FirestoreUtils {

    enum class PhoneExistenceResult {
        EXISTS,
        NOT_EXISTS,
        UNKNOWN
    }

    /**
     * Rich result for single-role-per-phone enforcement.
     *
     * [existingRole] is the role ("WORKER" | "EMPLOYER") already registered
     * against this phone number, or null when unknown / not registered.
     * [roleConflict] is true when an account exists for this phone but under
     * a different role than the one the caller is trying to use.
     */
    data class PhoneCheckResult(
        val exists: PhoneExistenceResult,
        val existingRole: String? = null,
        val roleConflict: Boolean = false,
        /**
         * True when no phoneRoles doc exists and the legacy-user Cloud Function fallback did
         * not answer within [PHONE_FALLBACK_DEADLINE_MS]. [exists] is then an ASSUMPTION (see
         * `assumeRegisteredWhenFallbackSlow`); the authoritative role-conflict check runs after
         * sign-in in AuthFlowService.resolveLogin. Such results are never cached.
         */
        val fallbackDeferred: Boolean = false
    )

    /**
    * Ensures canonical identity/profile documents exist.
     */
    suspend fun ensureMinimalUserDocument(
        userId: String,
        role: String,
        phoneNumber: String? = null,
        fullName: String? = null
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val roleUpper = role.uppercase()
        val resolvedPhone = phoneNumber
            ?.takeIf { it.isNotBlank() }
            ?.let(PhoneNumberUtils::normalize)
            ?: FirebaseAuth.getInstance().currentUser?.phoneNumber?.let(PhoneNumberUtils::normalize)
        val resolvedName = fullName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: FirebaseAuth.getInstance().currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }

        if (resolvedPhone.isNullOrBlank() || resolvedName.isNullOrBlank()) {
            throw IllegalStateException("Refusing to create identity docs for $userId without fullName and phone")
        }

        val now = Timestamp.now()
        val batch = firestore.batch()
        batch.set(
            firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(resolvedPhone),
            mapOf(
                "phoneNumber" to resolvedPhone,
                "role" to roleUpper,
                "name" to resolvedName,
                "uid" to userId,
                "updatedAt" to now
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
        batch.set(
            firestore.collection(profileCollectionForRole(roleUpper)).document(userId),
            mapOf(
                "userId" to userId,
                "phone" to resolvedPhone,
                "fullName" to resolvedName,
                "role" to roleUpper,
                "updatedAt" to now
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
        batch.commit().await()
    }

    private fun profileCollectionForRole(role: String): String =
        if (role.uppercase() == "EMPLOYER") {
            com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES
        } else {
            com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES
        }

    private fun profileRoleFromCollections(
        workerProfile: Map<String, Any>?,
        employerProfile: Map<String, Any>?
    ): String? = when {
        !workerProfile.isNullOrEmpty() -> "WORKER"
        !employerProfile.isNullOrEmpty() -> "EMPLOYER"
        else -> null
    }

    private fun mergeProfileForUser(
        userId: String,
        role: String,
        profileData: Map<String, Any>,
        phoneRoleData: Map<String, Any>?
    ): Map<String, Any> {
        return linkedMapOf<String, Any>(
            "userId" to userId,
            "role" to role
        ).apply {
            putAll(profileData)
            (phoneRoleData?.get("phoneNumber") as? String)?.let { put("phone", it) }
            (phoneRoleData?.get("name") as? String)?.let { put("fullName", it) }
        }
    }

    /**
     * Check if a user exists by normalized phone number in phoneRoles.
     */
    suspend fun checkUserExistsByPhoneNumber(phoneNumber: String): Map<String, Any?>? {
        val firestore = FirebaseFirestore.getInstance()
        val normalized = PhoneNumberUtils.normalize(phoneNumber)
        return try {
            val doc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES)
                .document(normalized)
                .get()
                .await()
            doc.data
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Timber.e(e, "PhoneRoles existence check failed")
            }
            null
        }
    }

    suspend fun doesUserExist(phoneNumber: String): Boolean {
        return checkUserExistsByPhoneNumber(phoneNumber) != null
    }

    suspend fun checkPhoneExistence(phoneNumber: String): PhoneExistenceResult {
        return checkPhoneForRole(phoneNumber, requestedRole = null).exists
    }

    private data class CachedPhoneCheck(val atMs: Long, val result: PhoneCheckResult)

    /** A legacy-fallback callable already running for a phone|role key (shared by concurrent callers). */
    private class InFlightFallback(val startedAtMs: Long, val deferred: Deferred<PhoneCheckResult>)

    // Short-lived cache of DEFINITIVE phone checks. The login/register screens run the
    // check right before OtpViewModel.sendOtp() re-runs it (defense in depth); the cache
    // makes the second call free instead of repeating 2-4 network round trips.
    private val phoneCheckCache = java.util.concurrent.ConcurrentHashMap<String, CachedPhoneCheck>()
    private const val PHONE_CHECK_CACHE_TTL_MS = 30_000L
    private const val PHONE_DIRECT_READ_TIMEOUT_MS = 4_000L

    /**
     * Max time the pre-OTP path waits for the Cloud Function fallback. The callable is only a
     * fallback for legacy users that have NO phoneRoles doc; it must never delay the SMS.
     */
    private const val PHONE_FALLBACK_DEADLINE_MS = 1_500L

    /** Hard cap for the background callable itself (it keeps running after the pre-send deadline). */
    private const val PHONE_CALLABLE_HARD_TIMEOUT_MS = 8_000L

    private val fallbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlightFallbacks = java.util.concurrent.ConcurrentHashMap<String, InFlightFallback>()

    /** Drop cached phone checks (call after an account is created/changed). */
    fun invalidatePhoneCheckCache() {
        phoneCheckCache.clear()
    }

    /**
     * Single-role-per-phone aware phone check. When [requestedRole] is
     * provided and the phone already has an account under a different role,
     * [PhoneCheckResult.roleConflict] is set to true so the UI can show a
     * precise error ("This number is registered as an employer").
     *
     * Source of truth: the `phoneRoles/{id}` doc(s) (all id variants read in parallel).
     * The `lookupPhoneRole` callable is ONLY a fallback for legacy users when no phoneRoles doc
     * exists, and is waited on for at most [PHONE_FALLBACK_DEADLINE_MS]. If it is slower:
     *  - [assumeRegisteredWhenFallbackSlow] = true (LOGIN callers): returns EXISTS without a
     *    conflict so the OTP is sent; a real conflict / missing account is enforced right after
     *    sign-in by AuthFlowService.resolveLogin.
     *  - false (default, REGISTER callers): returns NOT_EXISTS so registration proceeds
     *    (completeRegistration re-validates server-side).
     * Both results carry [PhoneCheckResult.fallbackDeferred] = true and are not cached.
     */
    suspend fun checkPhoneForRole(
        phoneNumber: String,
        requestedRole: String?,
        assumeRegisteredWhenFallbackSlow: Boolean = false
    ): PhoneCheckResult {
        val startedAt = AuthPerf.now()
        AuthPerf.log("phone_check_start", detail = "role=${requestedRole.orEmpty()}")
        val normalized = PhoneNumberUtils.normalize(phoneNumber)
        val cacheKey = normalized + "|" + requestedRole?.trim()?.uppercase().orEmpty()
        val cached = phoneCheckCache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.atMs < PHONE_CHECK_CACHE_TTL_MS) {
            AuthPerf.log("phone_check_end", startedAt, "source=cache result=${cached.result.exists}")
            return cached.result
        }
        val result = checkPhoneForRoleUncached(
            phoneNumber, requestedRole, normalized, cacheKey, assumeRegisteredWhenFallbackSlow
        )
        if (result.exists != PhoneExistenceResult.UNKNOWN && !result.fallbackDeferred) {
            phoneCheckCache[cacheKey] = CachedPhoneCheck(System.currentTimeMillis(), result)
        }
        AuthPerf.log(
            "phone_check_end", startedAt,
            "result=${result.exists} conflict=${result.roleConflict} deferred=${result.fallbackDeferred}"
        )
        return result
    }

    /**
     * Runs (or joins) the Cloud Function fallback for [cacheKey] on an app-level scope so it keeps
     * going, and caches its definitive answer, even when the pre-send path stops waiting for it.
     * Returns null when it did not answer with a definitive result within the remaining
     * [PHONE_FALLBACK_DEADLINE_MS] budget (measured from when the callable was first started).
     */
    private suspend fun awaitLegacyFallback(
        phoneNumber: String,
        requestedRole: String?,
        cacheKey: String
    ): PhoneCheckResult? {
        val entry = inFlightFallbacks.compute(cacheKey) { _, current ->
            if (current != null && !current.deferred.isCompleted) {
                current
            } else {
                InFlightFallback(
                    startedAtMs = System.currentTimeMillis(),
                    deferred = fallbackScope.async {
                        val r = withTimeoutOrNull(PHONE_CALLABLE_HARD_TIMEOUT_MS) {
                            checkPhoneForRoleViaCallable(phoneNumber, requestedRole)
                        } ?: PhoneCheckResult(PhoneExistenceResult.UNKNOWN)
                        if (r.exists != PhoneExistenceResult.UNKNOWN) {
                            phoneCheckCache[cacheKey] = CachedPhoneCheck(System.currentTimeMillis(), r)
                        }
                        r
                    }
                )
            }
        }!!
        entry.deferred.invokeOnCompletion { inFlightFallbacks.remove(cacheKey, entry) }
        val remaining = (PHONE_FALLBACK_DEADLINE_MS - (System.currentTimeMillis() - entry.startedAtMs))
            .coerceAtLeast(0L)
        val answer = if (entry.deferred.isCompleted) {
            entry.deferred.await()
        } else {
            withTimeoutOrNull(remaining) { entry.deferred.await() }
        }
        return answer?.takeIf { it.exists != PhoneExistenceResult.UNKNOWN }
    }

    private fun phoneCheckFromDoc(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        requestedRole: String?
    ): PhoneCheckResult {
        val existingRole = doc.getString("role")?.trim()?.uppercase()
            ?: (doc.get("roles") as? List<*>)?.firstOrNull()?.toString()?.trim()?.uppercase()
        val conflict = !requestedRole.isNullOrBlank() &&
            !existingRole.isNullOrBlank() &&
            existingRole != requestedRole.uppercase()
        return PhoneCheckResult(
            exists = PhoneExistenceResult.EXISTS,
            existingRole = existingRole,
            roleConflict = conflict
        )
    }

    private suspend fun checkPhoneForRoleUncached(
        phoneNumber: String,
        requestedRole: String?,
        normalized: String,
        cacheKey: String,
        assumeRegisteredWhenFallbackSlow: Boolean
    ): PhoneCheckResult {
        // 1. Direct Firestore check on phoneRoles. The canonical id and all number-format
        // variants are read IN PARALLEL (one round trip instead of up to 3 sequential ones).
        // Enabled by firestore.rules (allow get: if true; allow list: if false;)
        try {
            val firestore = FirebaseFirestore.getInstance()
            val variants = PhoneNumberUtils.getVariants(phoneNumber).filter { it != normalized }
            val phoneRolesCol = firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES)
            val docs = withTimeoutOrNull(PHONE_DIRECT_READ_TIMEOUT_MS) {
                coroutineScope {
                    val primary = async { phoneRolesCol.document(normalized).get().await() }
                    val variantReads = variants.map { variant ->
                        async {
                            try {
                                phoneRolesCol.document(variant).get().await()
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.d("Variant lookup ignored for $variant: ${e.message}")
                                null
                            }
                        }
                    }
                    val primaryDoc = primary.await()
                    val variantDocs = variantReads.awaitAll()
                    listOf(primaryDoc) + variantDocs.filterNotNull()
                }
            }

            if (docs != null) {
                val found = docs.firstOrNull { it.exists() }
                if (found != null) {
                    return phoneCheckFromDoc(found, requestedRole)
                }

                // No phoneRoles doc: legacy-user fallback via the callable (it also checks the
                // legacy users collection). Bounded to PHONE_FALLBACK_DEADLINE_MS so a
                // cold-starting function can never hold back the OTP.
                val callableResult = awaitLegacyFallback(phoneNumber, requestedRole, cacheKey)
                if (callableResult != null) {
                    return callableResult
                }

                Timber.tag("AuthPerf").w("phone fallback exceeded %dms; deferring to post-sign-in check", PHONE_FALLBACK_DEADLINE_MS)
                return if (assumeRegisteredWhenFallbackSlow) {
                    // LOGIN: proceed to OTP; resolveLogin enforces conflict / missing account.
                    PhoneCheckResult(PhoneExistenceResult.EXISTS, fallbackDeferred = true)
                } else {
                    // REGISTER: proceed as a new number; completeRegistration re-validates.
                    PhoneCheckResult(PhoneExistenceResult.NOT_EXISTS, fallbackDeferred = true)
                }
            } else {
                Timber.w("Direct phoneRoles lookup timed out; falling back to callable")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Timber.w(e, "Direct phoneRoles lookup encountered Firestore error: ${e.code}")
        } catch (e: Exception) {
            Timber.w(e, "Direct phoneRoles lookup failed")
        }

        // 2. Direct read failed/timed out: fall back to Cloud Function lookupPhoneRole
        // (same bounded, shared in-flight call).
        val callableResult = awaitLegacyFallback(phoneNumber, requestedRole, cacheKey)
        if (callableResult != null) {
            return callableResult
        }

        return PhoneCheckResult(PhoneExistenceResult.UNKNOWN)
    }

    private fun extractRole(userDoc: Map<String, Any?>): String? {
        val role = userDoc["role"] as? String
        return role?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
    }

    private suspend fun checkPhoneForRoleViaCallable(
        phoneNumber: String,
        requestedRole: String?
    ): PhoneCheckResult {
        val normalized = PhoneNumberUtils.normalize(phoneNumber)
        val variants = PhoneNumberUtils.getVariants(phoneNumber)
        val callableRegions = listOf(
            "lookupPhoneRole" to listOf("asia-south1", "")
        )

        for ((callableName, regions) in callableRegions) {
            for (region in regions) {
                try {
                    val payloadArgs = mutableMapOf<String, Any>(
                        "phone" to normalized,
                        "variants" to variants
                    )
                    if (!requestedRole.isNullOrBlank()) {
                        payloadArgs["requestedRole"] = requestedRole.uppercase()
                    }

                    val functions = if (region.isBlank()) {
                        FirebaseFunctions.getInstance()
                    } else {
                        FirebaseFunctions.getInstance(region)
                    }
                    val response = functions
                        .getHttpsCallable(callableName)
                        .call(payloadArgs)
                        .await()

                    @Suppress("UNCHECKED_CAST")
                    val payload = response.data as? Map<String, Any?>
                    val exists = payload?.get("exists") as? Boolean
                    val existingRole = (payload?.get("existingRole") as? String)
                        ?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
                    val roleConflict = payload?.get("roleConflict") as? Boolean ?: false

                    val effectiveConflict = roleConflict || (
                        !requestedRole.isNullOrBlank() &&
                            existingRole != null &&
                            existingRole != requestedRole.uppercase()
                    )

                    when (exists) {
                        true -> return PhoneCheckResult(
                            exists = PhoneExistenceResult.EXISTS,
                            existingRole = existingRole,
                            roleConflict = effectiveConflict
                        )
                        false -> return PhoneCheckResult(PhoneExistenceResult.NOT_EXISTS)
                        null -> Timber.w("Callable $callableName ($region) returned invalid payload: $payload")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Callable $callableName unavailable at region '$region'")
                }
            }
        }

        return PhoneCheckResult(PhoneExistenceResult.UNKNOWN)
    }

    suspend fun updateUserRole(userId: String, role: String) {
        try {
            val roleUpper = role.uppercase()
            val currentUser = FirebaseAuth.getInstance().currentUser
            val firestore = FirebaseFirestore.getInstance()
            val now = Timestamp.now()

            val batch = firestore.batch()
            if (currentUser?.uid == userId) {
                val normalizedPhone = currentUser.phoneNumber?.let(PhoneNumberUtils::normalize).orEmpty()
                if (normalizedPhone.isNotBlank()) {
                    val existingName = firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES)
                        .document(normalizedPhone)
                        .get()
                        .await()
                        .getString("name")
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                    val resolvedName = currentUser.displayName?.trim()?.takeIf { it.isNotBlank() } ?: existingName
                    batch.set(
                        firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(normalizedPhone),
                        mutableMapOf<String, Any>(
                            "phoneNumber" to normalizedPhone,
                            "role" to roleUpper,
                            "uid" to userId,
                            "updatedAt" to now
                        ).apply {
                            if (!resolvedName.isNullOrBlank()) this["name"] = resolvedName
                        },
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
            }
            batch.set(
                firestore.collection(profileCollectionForRole(roleUpper)).document(userId),
                mapOf("userId" to userId, "role" to roleUpper, "updatedAt" to now),
                com.google.firebase.firestore.SetOptions.merge()
            )
            batch.commit().await()
        } catch (e: Exception) {
            Timber.e(e, "Error updating user role for $userId")
            throw e
        }
    }

    suspend fun getUserByUid(uid: String): Map<String, Any>? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            withTimeout(5_000L) {
                coroutineScope {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val phone = currentUser?.phoneNumber?.let(PhoneNumberUtils::normalize)
                    val workerDeferred = async { firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(uid).get().await() }
                    val employerDeferred = async { firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(uid).get().await() }
                    // phoneRoles is only needed to enrich the profile; read it concurrently
                    // instead of after the two profile reads.
                    val phoneRoleDeferred = async {
                        phone?.takeIf { currentUser?.uid == uid }?.let {
                            runCatching {
                                firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(it).get().await().data
                            }.getOrNull()
                        }
                    }
                    val workerDoc = workerDeferred.await()
                    val employerDoc = employerDeferred.await()
                    val phoneRoleData = phoneRoleDeferred.await()
                    val role = profileRoleFromCollections(workerDoc.data, employerDoc.data) ?: return@coroutineScope null
                    val profileData = if (role == "EMPLOYER") employerDoc.data.orEmpty() else workerDoc.data.orEmpty()
                    mergeProfileForUser(uid, role, profileData, phoneRoleData)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user by UID: $uid")
            null
        }
    }

    suspend fun saveUserPhoneNumber(userId: String, phoneNumber: String, role: String) {
        try {
            val normalizedPhone = PhoneNumberUtils.normalize(phoneNumber)
            val firestore = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser
            val now = Timestamp.now()
            val batch = firestore.batch()
            if (currentUser?.uid == userId) {
                val existingName = firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES)
                    .document(normalizedPhone)
                    .get()
                    .await()
                    .getString("name")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                val resolvedName = currentUser.displayName?.trim()?.takeIf { it.isNotBlank() } ?: existingName
                batch.set(
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(normalizedPhone),
                    mutableMapOf<String, Any>(
                        "phoneNumber" to normalizedPhone,
                        "role" to role.uppercase(),
                        "uid" to userId,
                        "updatedAt" to now
                    ).apply {
                        if (!resolvedName.isNullOrBlank()) this["name"] = resolvedName
                    },
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
            batch.set(
                firestore.collection(profileCollectionForRole(role)).document(userId),
                mapOf("userId" to userId, "phone" to normalizedPhone, "role" to role.uppercase(), "updatedAt" to now),
                com.google.firebase.firestore.SetOptions.merge()
            )
            batch.commit().await()
        } catch (e: Exception) {
            Timber.e(e, "Error saving phone number for $userId")
            throw e
        }
    }

    suspend fun saveUserFullName(userId: String, fullName: String, role: String) {
        try {
            val trimmedName = fullName.trim()
            if (trimmedName.isBlank()) {
                throw IllegalArgumentException("Full name cannot be blank")
            }
            val firestore = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser
            val now = Timestamp.now()
            val batch = firestore.batch()
            currentUser?.phoneNumber?.takeIf { currentUser.uid == userId }?.let { phone ->
                val normalizedPhone = PhoneNumberUtils.normalize(phone)
                batch.set(
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(normalizedPhone),
                    mapOf(
                        "phoneNumber" to normalizedPhone,
                        "role" to role.uppercase(),
                        "name" to trimmedName,
                        "uid" to userId,
                        "updatedAt" to now
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
            batch.set(
                firestore.collection(profileCollectionForRole(role)).document(userId),
                mapOf("userId" to userId, "fullName" to trimmedName, "role" to role.uppercase(), "updatedAt" to now),
                com.google.firebase.firestore.SetOptions.merge()
            )
            batch.commit().await()
        } catch (e: Exception) {
            Timber.e(e, "Error saving full name for $userId")
            throw e
        }
    }
}
