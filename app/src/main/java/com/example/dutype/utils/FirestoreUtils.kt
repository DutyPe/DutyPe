package com.example.dutype.utils

import android.content.Context
import com.dutype.app.R
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Pre-OTP phone check (one role per phone), answered by the `lookupPhoneRole` function (App Check
 * protected, returns only "exists" and the role). No definite answer, no SMS. `phoneRoles/{+91XXXXXXXXXX}` itself is readable
 * only by the signed-in owner of that number, so nobody can look numbers up from the database.
 * Accounts are created and conflicts re-checked server-side by `completeRegistration`.
 */
object FirestoreUtils {

    enum class PhoneExistenceResult {
        EXISTS,
        NOT_EXISTS,
        UNKNOWN
    }

    /**
     * [existingRole] is the role ("WORKER" | "EMPLOYER") already registered for the phone, or null.
     * [roleConflict] is true when that role differs from the one the caller asked for.
     * [appNotVerified] (UNKNOWN only): the server refused this app (App Check), so retrying will
     * not help; otherwise UNKNOWN means the server could not be reached.
     */
    data class PhoneCheckResult(
        val exists: PhoneExistenceResult,
        val existingRole: String? = null,
        val roleConflict: Boolean = false,
        val appNotVerified: Boolean = false
    )

    private data class CachedPhoneCheck(val atMs: Long, val result: PhoneCheckResult)

    // The login/register screens check right before OtpViewModel.sendOtp() re-checks;
    // the short cache makes the second call free.
    private val phoneCheckCache = java.util.concurrent.ConcurrentHashMap<String, CachedPhoneCheck>()
    private const val PHONE_CHECK_CACHE_TTL_MS = 30_000L
    /** Covers a cold start of the function (2-4 s) on a slow network. */
    private const val CALLABLE_TIMEOUT_MS = 8_000L
    private const val RETRY_DELAY_MS = 600L

    /** Drop cached phone checks (call after an account is created). */
    fun invalidatePhoneCheckCache() {
        phoneCheckCache.clear()
    }

    /**
     * One `lookupPhoneRole` call, retried once if the server could not be reached. An SMS is sent
     * only on a definite answer: the screens send nothing on UNKNOWN, so an unregistered number
     * never costs an OTP on login, and a registered one never on sign-up.
     */
    suspend fun checkPhoneForRole(phoneNumber: String, requestedRole: String?): PhoneCheckResult {
        val phone = PhoneNumberUtils.normalize(phoneNumber)
        val role = requestedRole?.trim()?.uppercase().orEmpty()
        val cacheKey = "$phone|$role"
        phoneCheckCache[cacheKey]
            ?.takeIf { System.currentTimeMillis() - it.atMs < PHONE_CHECK_CACHE_TTL_MS }
            ?.let { return it.result }

        var result = callableCheck(phone, role)
        if (result.exists == PhoneExistenceResult.UNKNOWN && !result.appNotVerified) {
            kotlinx.coroutines.delay(RETRY_DELAY_MS)
            result = callableCheck(phone, role)
        }
        if (result.exists != PhoneExistenceResult.UNKNOWN) {
            phoneCheckCache[cacheKey] = CachedPhoneCheck(System.currentTimeMillis(), result)
        }
        return result
    }

    private fun resultFor(existingRole: String?, requestedRole: String): PhoneCheckResult =
        PhoneCheckResult(
            exists = PhoneExistenceResult.EXISTS,
            existingRole = existingRole,
            roleConflict = requestedRole.isNotBlank() && !existingRole.isNullOrBlank() && existingRole != requestedRole
        )

    private suspend fun callableCheck(phone: String, requestedRole: String): PhoneCheckResult = try {
        val response = withTimeoutOrNull(CALLABLE_TIMEOUT_MS) {
            FirebaseFunctions.getInstance("asia-south1")
                .getHttpsCallable("lookupPhoneRole")
                .call(mapOf("phone" to phone, "requestedRole" to requestedRole))
                .await()
        }
        @Suppress("UNCHECKED_CAST")
        val payload = response?.data as? Map<String, Any?>
        when (payload?.get("exists") as? Boolean) {
            true -> resultFor((payload["existingRole"] as? String)?.uppercase(), requestedRole)
            false -> PhoneCheckResult(PhoneExistenceResult.NOT_EXISTS)
            null -> PhoneCheckResult(PhoneExistenceResult.UNKNOWN)
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "lookupPhoneRole failed")
        val code = (e as? FirebaseFunctionsException)?.code
        // App Check rejected the app (failed-precondition / unauthenticated / permission-denied).
        val refused = code == FirebaseFunctionsException.Code.FAILED_PRECONDITION ||
            code == FirebaseFunctionsException.Code.UNAUTHENTICATED ||
            code == FirebaseFunctionsException.Code.PERMISSION_DENIED
        PhoneCheckResult(PhoneExistenceResult.UNKNOWN, appNotVerified = refused)
    }

    /** The message for an UNKNOWN check. */
    fun unknownMessage(context: Context, result: PhoneCheckResult): String =
        context.getString(if (result.appNotVerified) R.string.auth_app_not_verified else R.string.auth_phone_check_failed)
}
