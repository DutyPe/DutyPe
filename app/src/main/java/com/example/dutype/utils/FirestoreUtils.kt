package com.example.dutype.utils

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
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
        val roleConflict: Boolean = false
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

    /**
     * Single-role-per-phone aware phone check. When [requestedRole] is
     * provided and the phone already has an account under a different role,
     * [PhoneCheckResult.roleConflict] is set to true so the UI can show a
     * precise error ("This number is registered as an employer").
     */
    suspend fun checkPhoneForRole(phoneNumber: String, requestedRole: String?): PhoneCheckResult {
        val callableResult = checkPhoneForRoleViaCallable(phoneNumber, requestedRole)
        if (callableResult.exists != PhoneExistenceResult.UNKNOWN) {
            return callableResult
        }

        if (FirebaseAuth.getInstance().currentUser == null) {
            Timber.d("Phone existence fallback skipped for guest user (users query requires auth)")
            return PhoneCheckResult(PhoneExistenceResult.UNKNOWN)
        }

        return try {
            val userDoc = checkUserExistsByPhoneNumber(phoneNumber)
            if (userDoc == null) {
                PhoneCheckResult(PhoneExistenceResult.NOT_EXISTS)
            } else {
                val existingRole = extractRole(userDoc)
                val conflict = !requestedRole.isNullOrBlank() &&
                    !existingRole.isNullOrBlank() &&
                    existingRole.uppercase() != requestedRole.uppercase()
                PhoneCheckResult(
                    exists = PhoneExistenceResult.EXISTS,
                    existingRole = existingRole,
                    roleConflict = conflict
                )
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Timber.w("Phone existence check blocked by rules. Continuing with UNKNOWN.")
                PhoneCheckResult(PhoneExistenceResult.UNKNOWN)
            } else {
                Timber.e(e, "Phone existence check failed")
                PhoneCheckResult(PhoneExistenceResult.UNKNOWN)
            }
        } catch (e: Exception) {
            Timber.e(e, "Phone existence check failed")
            PhoneCheckResult(PhoneExistenceResult.UNKNOWN)
        }
    }

    private fun extractRole(userDoc: Map<String, Any?>): String? {
        val role = (userDoc["role"] as? String)
            ?: (userDoc["activeRole"] as? String)
            ?: (userDoc["roles"] as? List<*>)?.firstOrNull()?.toString()
        return role?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
    }

    private suspend fun checkPhoneForRoleViaCallable(
        phoneNumber: String,
        requestedRole: String?
    ): PhoneCheckResult {
        val normalized = PhoneNumberUtils.normalize(phoneNumber)
        val variants = PhoneNumberUtils.getVariants(phoneNumber)
        // Batch-o #2: per-callable region map. Different callables live
        // in different regions:
        //   - `lookupPhoneRole` is wrapped in `onCallSecured` → asia-south1
        //   - legacy `checkPhoneExists` (functions/src/index.ts) is plain
        //     `functions.https.onCall` → us-central1 (default)
        // Batch-m incorrectly pinned BOTH to asia-south1 which made
        // `checkPhoneExists` start returning NOT_FOUND, so when
        // `lookupPhoneRole` was unavailable the fallback also failed
        // → result: PhoneCheckResult.UNKNOWN → user blocked at the
        // register / login screen with "Could not verify this number".
        // We now try EACH callable in its primary region first, then
        // the other region as a safety net (covers older deployments
        // where the function was published in only one of the two).
        val callableRegions = listOf(
            "lookupPhoneRole" to listOf("asia-south1", ""),
            "checkPhoneExists" to listOf("", "asia-south1")
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

                    // Defence-in-depth: derive roleConflict from
                    // existingRole vs requestedRole when the callable
                    // forgot to set it (older deploy of checkPhoneExists).
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
                    batch.set(
                        firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(normalizedPhone),
                        mapOf(
                            "phoneNumber" to normalizedPhone,
                            "role" to roleUpper,
                            "name" to currentUser.displayName.orEmpty(),
                            "uid" to userId,
                            "updatedAt" to now
                        ),
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
                val workerDoc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(uid).get().await()
                val employerDoc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(uid).get().await()
                val role = profileRoleFromCollections(workerDoc.data, employerDoc.data) ?: return@withTimeout null
                val profileData = if (role == "EMPLOYER") employerDoc.data.orEmpty() else workerDoc.data.orEmpty()
                val phone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.let(PhoneNumberUtils::normalize)
                val phoneRoleData = phone?.takeIf { FirebaseAuth.getInstance().currentUser?.uid == uid }?.let {
                    runCatching {
                        firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(it).get().await().data
                    }.getOrNull()
                }
                mergeProfileForUser(uid, role, profileData, phoneRoleData)
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
                batch.set(
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(normalizedPhone),
                    mapOf(
                        "phoneNumber" to normalizedPhone,
                        "role" to role.uppercase(),
                        "name" to currentUser.displayName.orEmpty(),
                        "uid" to userId,
                        "updatedAt" to now
                    ),
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
