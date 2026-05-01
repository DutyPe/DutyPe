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
        private const val COLLECTION_PHONE_ROLES = "phoneRoles"
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

    private data class RoleProfileState(
        val workerExists: Boolean,
        val employerExists: Boolean
    ) {
        fun profileExistsFor(role: String): Boolean = when (role) {
            "WORKER" -> workerExists
            "EMPLOYER" -> employerExists
            else -> false
        }

        fun singleExistingRole(): String? = when {
            workerExists && !employerExists -> "WORKER"
            employerExists && !workerExists -> "EMPLOYER"
            else -> null
        }
    }

    private suspend fun readRoleProfileState(userId: String): RoleProfileState {
        val workerExists = withTimeout(LOGIN_READ_TIMEOUT_MS) {
            firestore.collection(COLLECTION_WORKER_PROFILES)
                .document(userId)
                .get()
                .await()
                .exists()
        }
        val employerExists = withTimeout(LOGIN_READ_TIMEOUT_MS) {
            firestore.collection(COLLECTION_EMPLOYER_PROFILES)
                .document(userId)
                .get()
                .await()
                .exists()
        }
        return RoleProfileState(workerExists = workerExists, employerExists = employerExists)
    }

    /**
     * Live snapshot of `phoneRoles/{phone}` keyed to the current auth phone.
     */
    fun observeCurrentUser(): Flow<Map<String, Any>?> = callbackFlow {
        val phone = auth.currentUser?.phoneNumber?.takeIf { it.isNotBlank() }?.let(PhoneNumberUtils::normalize)
        if (phone.isNullOrBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = firestore.collection(COLLECTION_PHONE_ROLES).document(phone)
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
    * Emits the user's role derived from the live `phoneRoles/{phone}` doc.
     */
    fun observeActiveRole(): Flow<String?> = observeCurrentUser()
        .map { data ->
            if (data == null) return@map null
            (data["role"] as? String)?.trim()?.uppercase()?.takeIf { it in VALID_ROLES }
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
                val phoneRoleRef = firestore.collection(COLLECTION_PHONE_ROLES).document(normalizedPhone)
                val profileRef = firestore.collection(
                    if (role == "WORKER") COLLECTION_WORKER_PROFILES else COLLECTION_EMPLOYER_PROFILES
                ).document(currentUser.uid)
                val existingPhoneRole = transaction.get(phoneRoleRef)
                val existingProfile = transaction.get(profileRef)
                val existingData = existingPhoneRole.data.orEmpty()
                val existingProfileData = existingProfile.data.orEmpty()

                // Single-role architecture: an existing complete account cannot
                // register again, regardless of which role is requested.
                val existingRoleRaw = (existingData["role"] as? String)
                    ?: (existingProfileData["role"] as? String)
                val existingRole = existingRoleRaw?.uppercase()
                val existingReferralCode = (existingProfileData["referralCode"] as? String)?.trim().orEmpty()
                val isExistingCompleteUser =
                    !(existingData["phoneNumber"] as? String).isNullOrBlank() &&
                    !(existingData["name"] as? String).isNullOrBlank() &&
                    !existingRole.isNullOrBlank()

                if (existingPhoneRole.exists() && isExistingCompleteUser) {
                    throw IllegalStateException("Account already exists")
                }

                val ownReferralCode = existingReferralCode
                val now = Timestamp.now()
                val referrerUserId = referrerSnapshot?.getString("userId").orEmpty()
                val resolvedFullName = (existingData["name"] as? String)?.trim()
                    .takeUnless { it.isNullOrBlank() }
                    ?: trimmedName
                val resolvedPhone = (existingData["phoneNumber"] as? String)?.trim()
                    .takeUnless { it.isNullOrBlank() }
                    ?: normalizedPhone

                val userData = linkedMapOf<String, Any>(
                    "userId" to currentUser.uid,
                    "phone" to resolvedPhone,
                    "phoneNumber" to resolvedPhone,
                    "fullName" to resolvedFullName,
                    "name" to resolvedFullName,
                    "role" to role,
                    "createdAt" to ((existingData["createdAt"] as? Timestamp) ?: now),
                    "updatedAt" to now
                )

                val profileData = linkedMapOf<String, Any>(
                    "userId" to currentUser.uid,
                    "phone" to resolvedPhone,
                    "fullName" to resolvedFullName,
                    "role" to role,
                    "createdAt" to ((existingProfileData["createdAt"] as? Timestamp)
                        ?: (existingData["createdAt"] as? Timestamp)
                        ?: now),
                    "updatedAt" to now
                )

                if (ownReferralCode.isNotBlank()) {
                    userData["referralCode"] = ownReferralCode
                    profileData["referralCode"] = ownReferralCode
                }

                if (normalizedReferralCode != null && referrerUserId.isNotBlank()) {
                    userData["referredByCode"] = normalizedReferralCode
                    userData["referredByUserId"] = referrerUserId
                    profileData["referredByCode"] = normalizedReferralCode
                    profileData["referredByUserId"] = referrerUserId
                } else {
                    (existingProfileData["referredByCode"] as? String)?.takeIf { it.isNotBlank() }?.let {
                        userData["referredByCode"] = it
                        profileData["referredByCode"] = it
                    }
                    (existingProfileData["referredByUserId"] as? String)?.takeIf { it.isNotBlank() }?.let {
                        userData["referredByUserId"] = it
                        profileData["referredByUserId"] = it
                    }
                }

                val phoneRoleData = linkedMapOf<String, Any>(
                    "phoneNumber" to resolvedPhone,
                    "role" to role,
                    "name" to resolvedFullName,
                    "uid" to currentUser.uid,
                    "createdAt" to ((existingData["createdAt"] as? Timestamp) ?: now),
                    "updatedAt" to now
                )
                transaction.set(phoneRoleRef, phoneRoleData)
                transaction.set(profileRef, profileData, com.google.firebase.firestore.SetOptions.merge())

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
            val normalizedPhone = currentUser.phoneNumber?.takeIf { it.isNotBlank() }?.let(PhoneNumberUtils::normalize)
                ?: return Result.failure(IllegalArgumentException("Phone number is required"))
            val phoneRoleRef = firestore.collection(COLLECTION_PHONE_ROLES).document(normalizedPhone)

            val userSnapshot = withTimeout(LOGIN_READ_TIMEOUT_MS) {
                phoneRoleRef.get().await()
            }

            val roleProfileState = readRoleProfileState(currentUser.uid)
            val userData = userSnapshot.data.orEmpty().toMutableMap()
            val hasAnyRoleProfile = roleProfileState.workerExists || roleProfileState.employerExists
            if (!userSnapshot.exists() && !hasAnyRoleProfile) {
                return Result.failure(IllegalStateException("account-not-found"))
            }

            val existingRole = (userData["role"] as? String)?.uppercase()
            val effectiveRole = existingRole
                ?: roleProfileState.singleExistingRole()
                ?: role
            userData["userId"] = currentUser.uid
            userData["fullName"] = userData["name"] as? String ?: ""
            userData["phone"] = userData["phoneNumber"] as? String ?: normalizedPhone
            userData["role"] = effectiveRole

            val hasCoreFields =
                !(userData["phone"] as? String).isNullOrBlank() &&
                !existingRole.isNullOrBlank()

            val hasRoleProfile = roleProfileState.profileExistsFor(effectiveRole)
            val shouldRouteToProfileSetup = !hasRoleProfile && !(hasCoreFields && userSnapshot.exists())

            // Login resolution complete — no lastActiveAt write to minimize
            // per-login write costs at scale.

            Result.success(
                LoginResolution(
                    userData = userData,
                    shouldRouteToProfileSetup = shouldRouteToProfileSetup,
                    roleForFcm = effectiveRole
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
