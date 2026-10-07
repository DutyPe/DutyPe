package com.example.dutype.services

import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.example.dutype.firestore.FirestoreSchema.PhoneRoles
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.firestore.FirestoreSchema.WorkerProfiles
import com.example.dutype.utils.AuthPerf
import com.example.dutype.utils.PhoneNumberUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sign-up and sign-in resolution after OTP.
 *
 * Registration is one Cloud Function call (`completeRegistration`): it creates
 * phoneRoles/{+91…} {uid, role}, the role profile, the wallet and the referral atomically on the
 * server. Login reads phoneRoles (role) and the role profile (name, completeness): two reads.
 */
@Singleton
class AuthFlowService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {
    /** Who signed in: enough to cache the session and route. */
    data class SessionUser(
        val uid: String,
        val role: String,
        val name: String,
        val phone: String,
        val photoUrl: String?
    )

    data class RegistrationResolution(val user: SessionUser, val ownReferralCode: String, val referralError: String?)

    data class LoginResolution(val user: SessionUser, val shouldRouteToProfileSetup: Boolean)

    private fun phoneKey(fallbackPhone: String? = null): String? =
        auth.currentUser?.phoneNumber?.takeIf { it.isNotBlank() }?.let(PhoneNumberUtils::normalize)
            ?: fallbackPhone?.takeIf { it.isNotBlank() }?.let(PhoneNumberUtils::normalize)

    /** Live role from phoneRoles/{phone}; null when signed out or not registered. */
    fun observeActiveRole(): Flow<String?> = callbackFlow {
        val phone = phoneKey()
        if (phone == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = firestore.collection(PhoneRoles.COLLECTION).document(phone)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Timber.w(err, "phoneRoles listener failed")
                    trySend(null)
                } else {
                    trySend(snap?.getString(PhoneRoles.ROLE))
                }
            }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    /** [employerType] (employers only): INDIVIDUAL → [name] is the owner; COMPANY → it is the business name. */
    suspend fun completeRegistration(
        requestedRole: String,
        name: String,
        referralCode: String?,
        employerType: String? = null,
        knownPhone: String? = null
    ): Result<RegistrationResolution> =
        runCatching {
            val user = auth.currentUser ?: error("User not authenticated")
            val role = requestedRole.trim().uppercase()
            val phone = phoneKey(knownPhone).orEmpty()
            runCatching { user.getIdToken(true).await() }
            @Suppress("UNCHECKED_CAST")
            val data = functions.getHttpsCallable("completeRegistration").call(
                mapOf(
                    "uid" to user.uid,
                    "role" to role,
                    "name" to name.trim(),
                    "phone" to phone,
                    "referralCode" to referralCode?.trim().orEmpty(),
                    "employerType" to employerType.orEmpty()
                )
            ).await().data as? Map<String, Any?> ?: emptyMap()
            // The server set the role claim; refresh so Firestore rules see it now.
            runCatching { user.getIdToken(true).await() }
            com.example.dutype.utils.FirestoreUtils.invalidatePhoneCheckCache()
            RegistrationResolution(
                user = SessionUser(user.uid, role, name.trim(), phone, null),
                ownReferralCode = data["referralCode"] as? String ?: "",
                referralError = data["referralError"] as? String
            )
        }.onFailure { Timber.e(it, "completeRegistration failed") }

    /**
     * After OTP on the login path. Enforces one role per phone: a number registered with the other
     * role is signed out and fails with `phone-already-registered-as:<ROLE>`.
     */
    suspend fun resolveLogin(requestedRole: String, knownPhone: String? = null): Result<LoginResolution> = runCatching {
        val perfStart = AuthPerf.now()
        val user = auth.currentUser ?: error("User not authenticated")
        val phone = phoneKey(knownPhone) ?: error("Phone number is required")
        val role = requestedRole.trim().uppercase()
        val (roleDoc, profile) = coroutineScope {
            val roleRead = async { withTimeout(READ_TIMEOUT_MS) { firestore.collection(PhoneRoles.COLLECTION).document(phone).get().await() } }
            val profileRead = async {
                val collection = if (role == Values.Role.EMPLOYER) EmployerProfiles.COLLECTION else WorkerProfiles.COLLECTION
                withTimeout(READ_TIMEOUT_MS) { firestore.collection(collection).document(user.uid).get().await() }
            }
            roleRead.await() to profileRead.await()
        }
        if (!roleDoc.exists()) {
            AuthPerf.log("login_resolve_end", perfStart, "result=account_not_found")
            throw IllegalStateException("account-not-found")
        }
        val existingRole = roleDoc.getString(PhoneRoles.ROLE).orEmpty()
        if (existingRole != role) {
            runCatching { auth.signOut() }
            throw IllegalStateException("phone-already-registered-as:$existingRole")
        }
        val isEmployer = role == Values.Role.EMPLOYER
        val name = if (isEmployer) {
            profile.getString(EmployerProfiles.OWNER_NAME)?.takeIf { it.isNotBlank() }
                ?: profile.getString(EmployerProfiles.BUSINESS_NAME)
        } else profile.getString(WorkerProfiles.NAME)
        // Employers: the same completeness rule as the profile setup (company needs owner, business name and type).
        val complete = if (isEmployer) com.example.dutype.profile.EmployerProfile.from(user.uid, profile.data.orEmpty()).isComplete else
            (profile.get(WorkerProfiles.SKILLS) as? List<*>).orEmpty().isNotEmpty()
        AuthPerf.log("login_resolve_end", perfStart, "result=ok setup=${!complete}")
        LoginResolution(
            user = SessionUser(
                uid = user.uid,
                role = role,
                name = name.orEmpty(),
                phone = phone,
                photoUrl = profile.getString(if (isEmployer) EmployerProfiles.PHOTO_URL else WorkerProfiles.PHOTO_URL)
            ),
            shouldRouteToProfileSetup = !complete
        )
    }.onFailure { Timber.w(it, "resolveLogin failed") }

    private companion object {
        const val READ_TIMEOUT_MS = 6_000L
    }
}
