package com.example.dutype.services

import com.example.dutype.models.generateReferralCode
import com.example.dutype.models.normalizeReferralCode
import com.example.dutype.utils.PhoneNumberUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthFlowService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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
                val snapshot = firestore.collection(COLLECTION_REFERRAL_CODES)
                    .document(normalizedReferralCode)
                    .get()
                    .await()

                if (!snapshot.exists()) {
                    return Result.failure(IllegalArgumentException("Referral code not found"))
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

                val existingRoles = (existingData["roles"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
                val isExistingCompleteUser =
                    !(existingData["phone"] as? String).isNullOrBlank() &&
                    !(existingData["fullName"] as? String).isNullOrBlank() &&
                    existingRoles.isNotEmpty() &&
                    !(existingData["referralCode"] as? String).isNullOrBlank()

                if (existingUser.exists() && isExistingCompleteUser) {
                    throw IllegalStateException("Account already exists")
                }

                val ownReferralCode = reserveUniqueReferralCode(transaction)
                val now = Timestamp.now()
                val referrerUserId = referrerSnapshot?.getString("userId").orEmpty()

                val userData = linkedMapOf<String, Any>(
                    "userId" to currentUser.uid,
                    "phone" to normalizedPhone,
                    "fullName" to trimmedName,
                    "roles" to listOf(role),
                    "activeRole" to role,
                    "isVerified" to false,
                    "isActive" to true,
                    "referralCode" to ownReferralCode,
                    "createdAt" to ((existingData["createdAt"] as? Timestamp) ?: now),
                    "lastActiveAt" to now
                )

                if (normalizedReferralCode != null && referrerUserId.isNotBlank()) {
                    userData["referredByCode"] = normalizedReferralCode
                    userData["referredByUserId"] = referrerUserId
                }

                transaction.set(userRef, userData)
                transaction.set(
                    firestore.collection(COLLECTION_REFERRAL_CODES).document(ownReferralCode),
                    linkedMapOf<String, Any>(
                        "code" to ownReferralCode,
                        "userId" to currentUser.uid,
                        "userRole" to role,
                        "userName" to trimmedName,
                        "isActive" to true,
                        "createdAt" to now
                    )
                )

                if (normalizedReferralCode != null && referrerUserId.isNotBlank()) {
                    val referralRef = firestore.collection(COLLECTION_REFERRALS)
                        .document("${referrerUserId}_${currentUser.uid}")

                    if (transaction.get(referralRef).exists()) {
                        throw IllegalStateException("Referral already applied for this account")
                    }

                    transaction.set(
                        referralRef,
                        linkedMapOf<String, Any>(
                            "referrerId" to referrerUserId,
                            "referredUserId" to currentUser.uid,
                            "referralCode" to normalizedReferralCode,
                            "status" to "pending",
                            "createdAt" to now
                        )
                    )
                }

                RegistrationResolution(userData, ownReferralCode)
            }.await()

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

    private fun reserveUniqueReferralCode(transaction: Transaction): String {
        repeat(10) {
            val candidate = generateReferralCode()
            if (!transaction.get(firestore.collection(COLLECTION_REFERRAL_CODES).document(candidate)).exists()) {
                return candidate
            }
        }
        throw IllegalStateException("Unable to reserve a unique referral code")
    }
}
