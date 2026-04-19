package com.example.dutype.services

import com.example.dutype.models.normalizeReferralCode
import com.example.dutype.utils.PhoneNumberUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthFlowService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_WORKER_PROFILES = "worker_profiles"
        private const val COLLECTION_EMPLOYER_PROFILES = "employer_profiles"
        private const val COLLECTION_REFERRALS = "referrals"
        private const val COLLECTION_REFERRAL_CODES = "referral_codes"
        private const val LOGIN_READ_TIMEOUT_MS = 5_000L
        private val VALID_ROLES = setOf("WORKER", "EMPLOYER")
    }

    data class RegistrationResolution(
        val userData: Map<String, Any>,
        val ownReferralCode: String
    )

    data class LoginResolution(
        val userData: Map<String, Any>?,
        val shouldRouteToProfileSetup: Boolean,
        val roleForFcm: String
    )

    /**
     * Live snapshot of `users/{uid}` keyed to the currently-authenticated user.
     * Emits null when signed out or when the doc is missing. Use this as the
     * single source of truth for `activeRole`, `roles[]`, and `workerProfileScore`
     * instead of navigation arguments — a role switch from another device or
     * an admin tool will propagate to every screen within one snapshot tick.
     */
    fun observeCurrentUser(): Flow<Map<String, Any>?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = firestore.collection(COLLECTION_USERS).document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Timber.w(err, "AuthFlowService.observeCurrentUser listener failed")
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snap?.data)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Emits the user's active role derived from the live `users/{uid}` doc.
     * Falls back to the first entry of `roles[]` when `activeRole` is absent.
     */
    fun observeActiveRole(): Flow<String?> = observeCurrentUser()
        .map { data ->
            if (data == null) return@map null
            val active = (data["activeRole"] as? String)?.uppercase()?.takeIf { it.isNotBlank() }
            if (active != null) return@map active
            val roles = (data["roles"] as? List<*>)
                ?.mapNotNull { it?.toString()?.uppercase() }
                .orEmpty()
            roles.firstOrNull()
        }
        .distinctUntilChanged()

    private suspend fun findReferralCodeDocument(rawCode: String): DocumentSnapshot? {
        val normalizedCode = normalizeReferralCode(rawCode)
        if (normalizedCode.isBlank()) return null

        val candidates = linkedSetOf(
            normalizedCode,
            normalizedCode.lowercase()
        )

        for (candidate in candidates) {
            try {
                val snapshot = firestore.collection(COLLECTION_REFERRAL_CODES)
                    .document(candidate)
                    .get()
                    .await()
                if (snapshot.exists()) {
                    return snapshot
                }
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Timber.w("AuthFlowService: referral code lookup denied for %s", candidate)
                    continue
                }
                throw e
            }
        }

        return null
    }

    suspend fun completeRegistration(
        requestedRole: String,
        fullName: String,
        referralCode: String?
    ): Result<RegistrationResolution> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val role = normalizeRole(requestedRole)
            val trimmedName = fullName.trim()
            val normalizedPhone = currentUser.phoneNumber
                ?.takeIf { it.isNotBlank() }
                ?.let(PhoneNumberUtils::normalize)

            if (trimmedName.isBlank()) {
                return Result.failure(IllegalArgumentException("Full name is required"))
            }
            if (normalizedPhone.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("Phone number is required"))
            }

            val normalizedReferralCode = referralCode
                ?.takeIf { it.isNotBlank() }
                ?.let(::normalizeReferralCode)
                ?.takeIf { it.isNotBlank() }

            val referrerSnapshot = if (normalizedReferralCode != null) {
                val snapshot = findReferralCodeDocument(normalizedReferralCode)
                    ?: return Result.failure(IllegalArgumentException("Referral code not found"))

                val isActive = snapshot.getBoolean("isActive") ?: true
                if (!isActive) {
                    return Result.failure(IllegalArgumentException("This referral code is no longer active"))
                }

                val referrerUserId = snapshot.getString("userId").orEmpty()
                if (referrerUserId.isBlank()) {
                    return Result.failure(IllegalArgumentException("Referral code is invalid"))
                }
                if (referrerUserId == currentUser.uid) {
                    return Result.failure(IllegalArgumentException("You cannot use your own referral code"))
                }

                val existingReferral = firestore.collection(COLLECTION_REFERRALS)
                    .whereEqualTo("referredUserId", currentUser.uid)
                    .limit(1)
                    .get()
                    .await()
                if (!existingReferral.isEmpty) {
                    return Result.failure(IllegalStateException("Referral already applied for this account"))
                }

                snapshot
            } else {
                null
            }

            val resolution = firestore.runTransaction { transaction ->
                val userRef = firestore.collection(COLLECTION_USERS).document(currentUser.uid)
                val existingUser = transaction.get(userRef)
                val existingData = existingUser.data.orEmpty()

                val existingRoles = (existingData["roles"] as? List<*>)
                    ?.mapNotNull { it?.toString()?.trim()?.uppercase() }
                    .orEmpty()
                val mergedRoles = (existingRoles + role).distinct()
                val existingReferralCode = (existingData["referralCode"] as? String)?.trim().orEmpty()
                val alreadyHasRequestedRole = role in existingRoles
                val isExistingCompleteUser =
                    !(existingData["phone"] as? String).isNullOrBlank() &&
                    !(existingData["fullName"] as? String).isNullOrBlank() &&
                    existingRoles.isNotEmpty()

                if (existingUser.exists() && isExistingCompleteUser && alreadyHasRequestedRole) {
                    throw IllegalStateException("Account already exists")
                }

                if (existingUser.exists() && isExistingCompleteUser && !alreadyHasRequestedRole && normalizedReferralCode != null) {
                    throw IllegalStateException("Referral code can only be used on your first registration")
                }

                val ownReferralCode = existingReferralCode
                val now = Timestamp.now()
                val referrerUserId = referrerSnapshot?.getString("userId").orEmpty()
                val resolvedFullName = (existingData["fullName"] as? String)?.trim()
                    .takeUnless { it.isNullOrBlank() }
                    ?: trimmedName
                val resolvedPhone = (existingData["phone"] as? String)?.trim()
                    .takeUnless { it.isNullOrBlank() }
                    ?: normalizedPhone

                val userData = linkedMapOf<String, Any>(
                    "userId" to currentUser.uid,
                    "phone" to resolvedPhone,
                    "fullName" to resolvedFullName,
                    "roles" to mergedRoles,
                    "activeRole" to role,
                    "createdAt" to ((existingData["createdAt"] as? Timestamp) ?: now),
                    "lastActiveAt" to now
                )

                if (ownReferralCode.isNotBlank()) {
                    userData["referralCode"] = ownReferralCode
                }

                if (normalizedReferralCode != null && referrerUserId.isNotBlank()) {
                    userData["referredByCode"] = normalizedReferralCode
                    userData["referredByUserId"] = referrerUserId
                } else {
                    (existingData["referredByCode"] as? String)?.takeIf { it.isNotBlank() }?.let {
                        userData["referredByCode"] = it
                    }
                    (existingData["referredByUserId"] as? String)?.takeIf { it.isNotBlank() }?.let {
                        userData["referredByUserId"] = it
                    }
                }

                // Preserve optional canonical fields when present while rebuilding the user doc.
                (existingData["profileImageUrl"] as? String)?.takeIf { it.isNotBlank() }?.let {
                    userData["profileImageUrl"] = it
                }
                (existingData["fcmToken"] as? String)?.takeIf { it.isNotBlank() }?.let {
                    userData["fcmToken"] = it
                }
                val existingLocation = existingData["location"] as? Map<*, *>
                val lat = (existingLocation?.get("lat") as? Number)?.toDouble()
                val lng = (existingLocation?.get("lng") as? Number)?.toDouble()
                if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                    userData["location"] = mapOf("lat" to lat, "lng" to lng)
                }
                (existingData["geohash"] as? String)?.takeIf { it.isNotBlank() }?.let {
                    userData["geohash"] = it
                }

                // Overwrite with canonical shape to drop legacy keys that can block strict-rule updates.
                transaction.set(userRef, userData)

                // Referral reward attachment is handled by Cloud Function applyReferralCode
                // from the registration flow, with profile-setup fallback for retries.

                RegistrationResolution(userData, ownReferralCode)
            }.await()

            queueOwnReferralCodeEnsure(
                userRole = role,
                userName = (resolution.userData["fullName"] as? String)?.trim().orEmpty().ifBlank { trimmedName }
            )

            Result.success(resolution)
        } catch (e: Exception) {
            Timber.e(e, "AuthFlowService.completeRegistration failed")
            Result.failure(e)
        }
    }

    suspend fun resolveLogin(requestedRole: String): Result<LoginResolution> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val role = normalizeRole(requestedRole)
            val userRef = firestore.collection(COLLECTION_USERS).document(currentUser.uid)

            val userSnapshot = withTimeout(LOGIN_READ_TIMEOUT_MS) {
                userRef.get().await()
            }

            if (!userSnapshot.exists()) {
                return Result.success(LoginResolution(null, true, role))
            }

            val userData = userSnapshot.data.orEmpty().toMutableMap()
            val roles = (userData["roles"] as? List<*>)?.mapNotNull { it?.toString()?.uppercase() }.orEmpty()
            val activeRole = (userData["activeRole"] as? String)?.uppercase()
            val roleExists = roles.contains(role)

            val hasCoreFields =
                !(userData["phone"] as? String).isNullOrBlank() &&
                !(userData["fullName"] as? String).isNullOrBlank() &&
                roles.isNotEmpty()

            val hasRoleProfile = when {
                !roleExists -> false
                role == "WORKER" -> withTimeout(LOGIN_READ_TIMEOUT_MS) {
                    firestore.collection(COLLECTION_WORKER_PROFILES)
                        .document(currentUser.uid)
                        .get()
                        .await()
                        .exists()
                }
                else -> withTimeout(LOGIN_READ_TIMEOUT_MS) {
                    firestore.collection(COLLECTION_EMPLOYER_PROFILES)
                        .document(currentUser.uid)
                        .get()
                        .await()
                        .exists()
                }
            }

            val updates = linkedMapOf<String, Any>(
                "lastActiveAt" to Timestamp.now()
            )
            if (roleExists) {
                updates["activeRole"] = role
                userData["activeRole"] = role
            }

            userRef.update(updates).await()
            userData["lastActiveAt"] = updates["lastActiveAt"] as Timestamp

            val roleForFcm = when {
                roleExists -> role
                !activeRole.isNullOrBlank() -> activeRole
                roles.isNotEmpty() -> roles.first()
                else -> role
            }

            Result.success(
                LoginResolution(
                    userData = userData,
                    shouldRouteToProfileSetup = !(hasCoreFields && hasRoleProfile),
                    roleForFcm = roleForFcm
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "AuthFlowService.resolveLogin failed")
            Result.failure(e)
        }
    }

    private fun normalizeRole(role: String): String {
        val normalized = role.trim().uppercase()
        require(normalized in VALID_ROLES) { "Unsupported role: $role" }
        return normalized
    }

    private fun queueOwnReferralCodeEnsure(
        userRole: String,
        userName: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val ensuredCode = withTimeoutOrNull(2_500L) {
                ensureOwnReferralCode(userRole, userName)
            }

            if (ensuredCode.isNullOrBlank()) {
                Timber.d("AuthFlowService: referral code ensure deferred or timed out")
            } else {
                Timber.d("AuthFlowService: referral code ensured asynchronously")
            }
        }
    }

    private suspend fun ensureOwnReferralCode(
        userRole: String,
        userName: String
    ): String {
        return try {
            val payload = hashMapOf<String, Any>(
                "userRole" to userRole,
                "userName" to userName
            )

            val result = functions
                .getHttpsCallable("ensureUserReferralCode")
                .call(payload)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.data as? Map<String, Any?> ?: emptyMap()
            val isSuccess = response["success"] as? Boolean ?: false
            if (!isSuccess) {
                return ""
            }

            normalizeReferralCode(response["referralCode"]?.toString().orEmpty())
        } catch (e: Exception) {
            Timber.w(e, "AuthFlowService: ensureUserReferralCode failed")
            ""
        }
    }
}
