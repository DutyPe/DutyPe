package com.example.dutype.core.error

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Enterprise-Level Error Handling System
 * 
 * Categorizes all errors into specific types for proper handling:
 * - Network errors (retryable)
 * - Authentication errors (require re-login)
 * - Validation errors (user input issues)
 * - Business logic errors (application rules)
 * - System errors (unexpected failures)
 * 
 * Benefits:
 * - Type-safe error handling
 * - Automatic retry strategies
 * - User-friendly error messages
 * - Proper logging and monitoring
 * 
 * @author DutyPe Engineering Team
 * @since 3.0.0
 */
sealed class DutyPeError {
    
    abstract val message: String
    abstract val userMessage: String
    abstract val isRetryable: Boolean
    abstract val shouldLogout: Boolean
    abstract val errorCode: String
    
    /**
     * Network-related errors (retryable)
     */
    data class NetworkError(
        override val message: String,
        val httpCode: Int? = null,
        val cause: Throwable? = null
    ) : DutyPeError() {
        override val userMessage: String = when {
            cause is UnknownHostException -> "No internet connection. Please check your network."
            cause is SocketTimeoutException -> "Request timed out. Please try again."
            httpCode == 429 -> "Too many requests. Please wait a moment."
            httpCode in 500..599 -> "Server is temporarily unavailable. Please try again later."
            else -> "Network error. Please check your connection."
        }
        override val isRetryable: Boolean = true
        override val shouldLogout: Boolean = false
        override val errorCode: String = "NET_${httpCode ?: "UNKNOWN"}"
    }
    
    /**
     * Authentication errors (require user action)
     */
    data class AuthError(
        override val message: String,
        val reason: AuthFailureReason,
        val cause: Throwable? = null
    ) : DutyPeError() {
        override val userMessage: String = when (reason) {
            AuthFailureReason.TOKEN_EXPIRED -> "Your session has expired. Please login again."
            AuthFailureReason.INVALID_CREDENTIALS -> "Invalid phone number or OTP."
            AuthFailureReason.USER_NOT_FOUND -> "User not found. Please register first."
            AuthFailureReason.ACCOUNT_DISABLED -> "Your account has been disabled. Contact support."
            AuthFailureReason.TOO_MANY_REQUESTS -> "Too many login attempts. Please try again later."
            AuthFailureReason.PHONE_NUMBER_BLOCKED -> "This phone number is blocked. Contact support."
            AuthFailureReason.UNKNOWN -> "Authentication failed. Please try again."
        }
        override val isRetryable: Boolean = reason == AuthFailureReason.TOO_MANY_REQUESTS
        override val shouldLogout: Boolean = reason in listOf(
            AuthFailureReason.TOKEN_EXPIRED,
            AuthFailureReason.ACCOUNT_DISABLED
        )
        override val errorCode: String = "AUTH_${reason.name}"
    }
    
    /**
     * Validation errors (user input issues)
     */
    data class ValidationError(
        override val message: String,
        val fieldErrors: Map<String, String> = emptyMap(),
        val cause: Throwable? = null
    ) : DutyPeError() {
        override val userMessage: String = when {
            fieldErrors.isNotEmpty() -> fieldErrors.values.first()
            else -> message
        }
        override val isRetryable: Boolean = false
        override val shouldLogout: Boolean = false
        override val errorCode: String = "VALIDATION_ERROR"
    }
    
    /**
     * Business logic errors (application rules)
     */
    data class BusinessError(
        override val message: String,
        val code: BusinessErrorCode,
        val metadata: Map<String, Any> = emptyMap(),
        val cause: Throwable? = null
    ) : DutyPeError() {
        override val userMessage: String = when (code) {
            BusinessErrorCode.ALREADY_APPLIED -> "You have already applied to this job."
            BusinessErrorCode.JOB_EXPIRED -> "This job posting has expired."
            BusinessErrorCode.JOB_FILLED -> "This position has been filled."
            BusinessErrorCode.PROFILE_INCOMPLETE -> "Please complete your profile to continue."
            BusinessErrorCode.QUOTA_EXCEEDED -> "You've reached your monthly limit. Upgrade to Premium."
            BusinessErrorCode.INSUFFICIENT_PERMISSIONS -> "You don't have permission to perform this action."
            BusinessErrorCode.DUPLICATE_ENTRY -> "This entry already exists."
            BusinessErrorCode.RESOURCE_NOT_FOUND -> "The requested resource was not found."
            BusinessErrorCode.OPERATION_NOT_ALLOWED -> "This operation is not allowed."
        }
        override val isRetryable: Boolean = false
        override val shouldLogout: Boolean = false
        override val errorCode: String = "BIZ_${code.name}"
    }
    
    /**
     * Firestore-specific errors
     */
    data class FirestoreError(
        override val message: String,
        val firestoreCode: FirebaseFirestoreException.Code? = null,
        val cause: Throwable? = null
    ) : DutyPeError() {
        override val userMessage: String = when (firestoreCode) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                "You don't have permission to access this data."
            FirebaseFirestoreException.Code.NOT_FOUND -> 
                "The requested data was not found."
            FirebaseFirestoreException.Code.ALREADY_EXISTS -> 
                "This data already exists."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> 
                "Service quota exceeded. Please try again later."
            FirebaseFirestoreException.Code.UNAVAILABLE -> 
                "Service temporarily unavailable. Please try again."
            else -> "Database error. Please try again."
        }
        override val isRetryable: Boolean = firestoreCode in listOf(
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED
        )
        override val shouldLogout: Boolean = false
        override val errorCode: String = "FIRESTORE_${firestoreCode?.name ?: "UNKNOWN"}"
    }
    
    /**
     * System/Unknown errors
     */
    data class SystemError(
        override val message: String,
        val cause: Throwable? = null
    ) : DutyPeError() {
        override val userMessage: String = 
            "An unexpected error occurred. Our team has been notified."
        override val isRetryable: Boolean = true
        override val shouldLogout: Boolean = false
        override val errorCode: String = "SYSTEM_ERROR"
    }
    
    companion object {
        /**
         * Convert any exception to DutyPeError
         */
        fun from(throwable: Throwable): DutyPeError {
            return when (throwable) {
                // Network errors
                is UnknownHostException, 
                is SocketTimeoutException,
                is IOException -> NetworkError(
                    message = throwable.message ?: "Network error",
                    cause = throwable
                )
                
                is FirebaseNetworkException -> NetworkError(
                    message = "Firebase network error",
                    cause = throwable
                )
                
                // Auth errors
                is FirebaseAuthException -> AuthError(
                    message = throwable.message ?: "Authentication error",
                    reason = mapAuthException(throwable),
                    cause = throwable
                )
                
                // Firestore errors
                is FirebaseFirestoreException -> FirestoreError(
                    message = throwable.message ?: "Firestore error",
                    firestoreCode = throwable.code,
                    cause = throwable
                )
                
                // Firebase errors
                is FirebaseException -> SystemError(
                    message = throwable.message ?: "Firebase error",
                    cause = throwable
                )
                
                // Default to system error
                else -> SystemError(
                    message = throwable.message ?: "Unknown error",
                    cause = throwable
                )
            }
        }
        
        private fun mapAuthException(exception: FirebaseAuthException): AuthFailureReason {
            return when (exception.errorCode) {
                "ERROR_INVALID_VERIFICATION_CODE" -> AuthFailureReason.INVALID_CREDENTIALS
                "ERROR_SESSION_EXPIRED" -> AuthFailureReason.TOKEN_EXPIRED
                "ERROR_USER_NOT_FOUND" -> AuthFailureReason.USER_NOT_FOUND
                "ERROR_USER_DISABLED" -> AuthFailureReason.ACCOUNT_DISABLED
                "ERROR_TOO_MANY_REQUESTS" -> AuthFailureReason.TOO_MANY_REQUESTS
                else -> AuthFailureReason.UNKNOWN
            }
        }
    }
}

/**
 * Authentication failure reasons
 */
enum class AuthFailureReason {
    TOKEN_EXPIRED,
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    ACCOUNT_DISABLED,
    TOO_MANY_REQUESTS,
    PHONE_NUMBER_BLOCKED,
    UNKNOWN
}

/**
 * Business error codes
 */
enum class BusinessErrorCode {
    ALREADY_APPLIED,
    JOB_EXPIRED,
    JOB_FILLED,
    PROFILE_INCOMPLETE,
    QUOTA_EXCEEDED,
    INSUFFICIENT_PERMISSIONS,
    DUPLICATE_ENTRY,
    RESOURCE_NOT_FOUND,
    OPERATION_NOT_ALLOWED
}
