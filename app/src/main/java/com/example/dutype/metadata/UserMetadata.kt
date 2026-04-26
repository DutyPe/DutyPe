package com.example.dutype.metadata

import com.example.dutype.models.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserMetadata - Metadata and statistics for the current user
 * 
 * This provides:
 * 1. User activity statistics
 * 2. Application history summary
 * 3. Profile completion status
 * 4. Trust/reputation scores
 * 5. Usage limits and quotas
 * 
 * REFACTORED: Now receives Firebase dependencies via constructor injection
 * 
 * Usage:
 * - Show user stats on profile screen
 * - Check limits before actions
 * - Display badges and achievements
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class UserMetadata @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val allowedUserFields = setOf(
        "phone",
        "fullName",
        "profileImageUrl",
        "role",
        "location",
        "geohash",
        "fcmToken",
        "language",
        "createdAt",
        "referralCode",
        "referredByCode",
        "referredByUserId"
    )

    
    // ==========================================
    // USER STATS
    // ==========================================
    
    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()
    
    // ==========================================
    // WORKER STATS (for workers)
    // ==========================================
    
    private val _workerStats = MutableStateFlow(WorkerStats())
    val workerStats: StateFlow<WorkerStats> = _workerStats.asStateFlow()
    
    // ==========================================
    // EMPLOYER STATS (for employers)
    // ==========================================
    
    private val _employerStats = MutableStateFlow(EmployerStats())
    val employerStats: StateFlow<EmployerStats> = _employerStats.asStateFlow()
    
    // ==========================================
    // USAGE LIMITS
    // ==========================================
    
    private val _usageLimits = MutableStateFlow(UsageLimits())
    val usageLimits: StateFlow<UsageLimits> = _usageLimits.asStateFlow()
    
    // ==========================================
    // LOADING STATE
    // ==========================================
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()
    
    /**
     * LIGHTWEIGHT: Load only basic user profile (name, phone, image)
     * Use this for profile screen - no heavy stats loading
     */
    suspend fun loadBasicProfile() {
        val userId = auth.currentUser?.uid ?: return
        
        Timber.d("📊 Loading basic profile for user: $userId")
        
        try {
            loadUserStats(userId)
            Timber.d("📊 Basic profile loaded successfully")
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to load basic profile")
        }
    }
    
    /**
     * Initialize FULL user metadata - call only when needed (stats screens, etc.)
     * For profile screen, use loadBasicProfile() instead
     */
    suspend fun initialize(userRole: UserRole) {
        val userId = auth.currentUser?.uid ?: return
        
        Timber.d("📊 Initializing FULL UserMetadata for user: $userId, role: $userRole")
        _isLoading.value = true
        
        try {
            loadUserStats(userId)
            
            when (userRole) {
                UserRole.WORKER -> loadWorkerStats(userId)
                UserRole.EMPLOYER -> loadEmployerStats(userId)
                UserRole.ADMIN -> {
                    // Admin can see both worker and employer stats
                    loadWorkerStats(userId)
                    loadEmployerStats(userId)
                }
            }
            
            loadUsageLimits(userId)
            
            _lastUpdated.value = System.currentTimeMillis()
            Timber.d("📊 FULL UserMetadata initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to initialize UserMetadata")
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Refresh user metadata
     */
    suspend fun refresh(userRole: UserRole) {
        initialize(userRole)
    }
    
    /**
     * Clear user metadata on logout
     */
    fun clear() {
        _userStats.value = UserStats()
        _workerStats.value = WorkerStats()
        _employerStats.value = EmployerStats()
        _usageLimits.value = UsageLimits()
        _lastUpdated.value = 0L
        Timber.d("📊 UserMetadata cleared")
    }
    
    /**
     * Check if user can apply for more jobs (within limits)
     */
    fun canApplyForJob(): Boolean {
        val limits = _usageLimits.value
        val stats = _workerStats.value
        return !limits.hasReachedApplicationLimit || stats.applicationsThisMonth < limits.maxApplicationsPerMonth
    }
    
    /**
     * Check if employer can post more jobs (within limits)
     */
    fun canPostJob(): Boolean {
        val limits = _usageLimits.value
        val stats = _employerStats.value
        return !limits.hasReachedJobPostLimit || stats.activeJobs < limits.maxActiveJobs
    }
    
    /**
     * Get remaining applications for worker
     */
    fun getRemainingApplications(): Int {
        val limits = _usageLimits.value
        val stats = _workerStats.value
        return maxOf(0, limits.maxApplicationsPerMonth - stats.applicationsThisMonth)
    }
    
    /**
     * Get remaining job posts for employer
     */
    fun getRemainingJobPosts(): Int {
        val limits = _usageLimits.value
        val stats = _employerStats.value
        return maxOf(0, limits.maxActiveJobs - stats.activeJobs)
    }
    
    /**
     * Increment application count (call after successful application)
     */
    fun incrementApplicationCount() {
        _workerStats.value = _workerStats.value.copy(
            totalApplications = _workerStats.value.totalApplications + 1,
            applicationsThisMonth = _workerStats.value.applicationsThisMonth + 1
        )
    }
    
    /**
     * Increment job post count (call after successful job post)
     */
    fun incrementJobPostCount() {
        _employerStats.value = _employerStats.value.copy(
            totalJobsPosted = _employerStats.value.totalJobsPosted + 1,
            activeJobs = _employerStats.value.activeJobs + 1
        )
    }
    
    // ==========================================
    // PRIVATE METHODS
    // ==========================================
    
    private suspend fun loadUserStats(userId: String) {
        try {
            val authPhoneNumber = auth.currentUser?.phoneNumber ?: ""
            val normalizedPhone = auth.currentUser?.phoneNumber
                ?.let(com.example.dutype.utils.PhoneNumberUtils::normalize)
                .orEmpty()
            val phoneRoleDoc = normalizedPhone.takeIf { auth.currentUser?.uid == userId && it.isNotBlank() }
                ?.let { firestore.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(it).get().await() }
            val role = (phoneRoleDoc?.getString("role") ?: "").uppercase()
            val preferredCollection = when (role) {
                "EMPLOYER" -> com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES
                else -> com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES
            }
            var profileDoc = firestore.collection(preferredCollection).document(userId).get().await()
            if (!profileDoc.exists() && preferredCollection != com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES) {
                profileDoc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(userId).get().await()
            }
            if (!profileDoc.exists() && preferredCollection != com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES) {
                profileDoc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(userId).get().await()
            }

            if (profileDoc.exists() || phoneRoleDoc?.exists() == true) {
                val firestorePhone = profileDoc.getString("phone") ?: phoneRoleDoc?.getString("phoneNumber") ?: ""
                val phoneToUse = firestorePhone.ifBlank { authPhoneNumber }

                val resolvedFullName = profileDoc.getString("fullName") ?: phoneRoleDoc?.getString("name") ?: ""
                val resolvedProfileImageUrl = profileDoc.getString("profileImageUrl") ?: ""

                _userStats.value = UserStats(
                    userId = userId,
                    fullName = resolvedFullName,
                    phone = phoneToUse,
                    profileImageUrl = resolvedProfileImageUrl,
                    createdAt = getEpochMillis(profileDoc, "createdAt", System.currentTimeMillis()),
                    isVerified = profileDoc.getBoolean("isVerified") ?: false
                )
                Timber.d("📊 UserStats loaded: name=${_userStats.value.fullName}, phone=${_userStats.value.phone}, image=${_userStats.value.profileImageUrl.isNotBlank()}")
            } else {
                // User document doesn't exist yet (new user) - use Firebase Auth phone
                _userStats.value = UserStats(
                    userId = userId,
                    fullName = "",
                    phone = authPhoneNumber,
                    profileImageUrl = "",
                    createdAt = System.currentTimeMillis(),
                    isVerified = false
                )
                Timber.d("📊 New user - using Auth phone: $authPhoneNumber")
            }
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to load user stats")
            // Even on error, try to get phone from Firebase Auth
            val authPhoneNumber = auth.currentUser?.phoneNumber ?: ""
            if (authPhoneNumber.isNotBlank()) {
                _userStats.value = UserStats(
                    userId = userId,
                    phone = authPhoneNumber
                )
                Timber.d("📊 Error fallback - using Auth phone: $authPhoneNumber")
            }
        }
    }

    private suspend fun enforceStrictUsersSchemaIfNeeded(
        userId: String,
        doc: DocumentSnapshot,
        authPhoneNumber: String
    ) {
        // Disabled intentionally: profile reads must not perform schema writes.
        // Strict-schema cleanup is handled by auth/profile services.
        return
    }
    
    private suspend fun loadWorkerStats(userId: String) {
        try {
            // Final schema: use canonical applications collection
            val applicationsQuery = firestore.collection(com.example.dutype.firestore.FirestoreCollections.APPLICATIONS)
                .whereEqualTo("workerId", userId)
                .limit(500)
                .get()
                .await()
            
            val totalApplications = applicationsQuery.size()
            val acceptedApplications = applicationsQuery.documents.count { 
                val status = it.getString("status")
                status == "accepted" || status == "in_progress" || status == "completed"
            }
            val completedJobs = acceptedApplications
            
            // Get this month's applications
            val startOfMonth = getStartOfMonth()
            val thisMonthApplications = applicationsQuery.documents.count {
                (it.getTimestamp("createdAt")?.toDate()?.time ?: 0L) >= startOfMonth
            }
            
            val savedJobsQuery = firestore.collection(com.example.dutype.firestore.FirestoreCollections.SAVED_JOBS)
                .whereEqualTo("userId", userId)
                .limit(200)
                .get()
                .await()
            val savedJobsList = savedJobsQuery.size()
            
            _workerStats.value = WorkerStats(
                totalApplications = totalApplications,
                acceptedApplications = acceptedApplications,
                completedJobs = completedJobs,
                applicationsThisMonth = thisMonthApplications,
                savedJobsCount = savedJobsList,
                responseRate = if (totalApplications > 0) {
                    (acceptedApplications.toDouble() / totalApplications * 100).toInt()
                } else 0
            )
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to load worker stats")
        }
    }
    
    private suspend fun loadEmployerStats(userId: String) {
        try {
            // Get jobs posted
            val jobsQuery = firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS)
                .whereEqualTo("employerId", userId)
                .limit(200)
                .get()
                .await()
            
            val totalJobs = jobsQuery.size()
            val activeJobs = jobsQuery.documents.count {
                it.getString("status") == "open"
            }
            val closedJobs = jobsQuery.documents.count {
                it.getString("status") == "closed"
            }
            
            // Get total applications received
            var totalApplicationsReceived = 0
            var totalHires = 0
            
            val applicationsQuery = firestore.collection(com.example.dutype.firestore.FirestoreCollections.APPLICATIONS)
                .whereEqualTo("employerId", userId)
                .limit(500)
                .get()
                .await()
            totalApplicationsReceived = applicationsQuery.size()
            totalHires = applicationsQuery.documents.count {
                val status = it.getString("status")
                status == "accepted" || status == "in_progress" || status == "completed"
            }
            val hiredJobCount = applicationsQuery.documents
                .asSequence()
                .filter {
                    val status = it.getString("status")
                    status == "accepted" || status == "in_progress" || status == "completed"
                }
                .mapNotNull { it.getString("jobId") }
                .distinct()
                .count()
            val filledJobs = maxOf(closedJobs, hiredJobCount)
            
            _employerStats.value = EmployerStats(
                totalJobsPosted = totalJobs,
                activeJobs = activeJobs,
                filledJobs = filledJobs,
                totalApplicationsReceived = totalApplicationsReceived,
                totalHires = totalHires,
                averageTimeToHire = 0.0, // TODO: Calculate from data
                responseRate = if (totalApplicationsReceived > 0) {
                    (totalHires.toDouble() / totalApplicationsReceived * 100).toInt()
                } else 0
            )
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to load employer stats")
        }
    }
    
    private suspend fun loadUsageLimits(userId: String) {
        try {
            // Default to free limits (subscriptions collection removed)
            _usageLimits.value = UsageLimits(
                maxApplicationsPerMonth = 10,
                maxActiveJobs = 3,
                maxSavedJobs = 20,
                hasReachedApplicationLimit = _workerStats.value.applicationsThisMonth >= 10,
                hasReachedJobPostLimit = _employerStats.value.activeJobs >= 3,
                isPremium = false
            )
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to load usage limits")
            _usageLimits.value = UsageLimits()
        }
    }
    
    private fun getStartOfMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEpochMillis(doc: DocumentSnapshot, fieldName: String, defaultValue: Long): Long {
        return doc.getTimestamp(fieldName)?.toDate()?.time
            ?: doc.getLong(fieldName)
            ?: defaultValue
    }
}

/**
 * General user statistics
 */
data class UserStats(
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long = 0L,
    val isVerified: Boolean = false
) {
    val accountAgeDays: Int
        get() = ((System.currentTimeMillis() - createdAt) / (24 * 60 * 60 * 1000)).toInt()
    
    val isNewUser: Boolean
        get() = accountAgeDays < 7
    
    val displayName: String
        get() = fullName.ifEmpty { "User" }
}

/**
 * Worker-specific statistics
 */
data class WorkerStats(
    val totalApplications: Int = 0,
    val acceptedApplications: Int = 0,
    val completedJobs: Int = 0,
    val applicationsThisMonth: Int = 0,
    val savedJobsCount: Int = 0,
    val responseRate: Int = 0 // Percentage of applications that got response
) {
    val successRate: Int
        get() = if (totalApplications > 0) {
            (acceptedApplications.toDouble() / totalApplications * 100).toInt()
        } else 0
}

/**
 * Employer-specific statistics
 */
data class EmployerStats(
    val totalJobsPosted: Int = 0,
    val activeJobs: Int = 0,
    val filledJobs: Int = 0,
    val totalApplicationsReceived: Int = 0,
    val totalHires: Int = 0,
    val averageTimeToHire: Double = 0.0, // in days
    val responseRate: Int = 0 // Percentage of applications responded to
) {
    val fillRate: Int
        get() = if (totalJobsPosted > 0) {
            (filledJobs.toDouble() / totalJobsPosted * 100).toInt()
        } else 0
}

/**
 * Usage limits based on subscription
 */
data class UsageLimits(
    val maxApplicationsPerMonth: Int = 10,
    val maxActiveJobs: Int = 3,
    val maxSavedJobs: Int = 20,
    val hasReachedApplicationLimit: Boolean = false,
    val hasReachedJobPostLimit: Boolean = false,
    val isPremium: Boolean = false
)
