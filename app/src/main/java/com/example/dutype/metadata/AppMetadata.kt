package com.example.dutype.metadata

import android.content.Context
import android.os.Build
import com.example.dutype.utils.appVersionInfo
import com.example.dutype.utils.isDebuggableBuild
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppMetadata - Centralized metadata management for the entire app
 * 
 * This provides:
 * 1. App version and build info
 * 2. Feature flags from remote config
 * 3. Platform statistics (job counts, user counts, etc.)
 * 4. Device and session information
 * 
 * REFACTORED: Now receives FirebaseFirestore via constructor injection
 * 
 * Usage:
 * - Inject AppMetadata in ViewModels or Services
 * - Access metadata via StateFlows for reactive updates
 * - Use for analytics, feature gating, and UI decisions
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class AppMetadata @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext appContext: Context
) {
    
    // ==========================================
    // APP INFO
    // ==========================================
    
    private val packageVersion = appContext.appVersionInfo()
    val appVersion: String = packageVersion.name
    val appVersionCode: Long = packageVersion.code
    val isDebug: Boolean = appContext.isDebuggableBuild()
    val applicationId: String = appContext.packageName
    
    // ==========================================
    // DEVICE INFO
    // ==========================================
    
    val deviceModel: String = Build.MODEL
    val deviceManufacturer: String = Build.MANUFACTURER
    val androidVersion: String = Build.VERSION.RELEASE
    val sdkVersion: Int = Build.VERSION.SDK_INT
    
    // ==========================================
    // PLATFORM STATS (from Firestore)
    // ==========================================
    
    private val _platformStats = MutableStateFlow(PlatformStats())
    val platformStats: StateFlow<PlatformStats> = _platformStats.asStateFlow()
    
    // ==========================================
    // FEATURE FLAGS (from Firestore)
    // ==========================================
    
    private val _featureFlags = MutableStateFlow(FeatureFlags())
    val featureFlags: StateFlow<FeatureFlags> = _featureFlags.asStateFlow()
    
    // ==========================================
    // SESSION INFO
    // ==========================================
    
    private val _sessionInfo = MutableStateFlow(SessionInfo())
    val sessionInfo: StateFlow<SessionInfo> = _sessionInfo.asStateFlow()
    
    // ==========================================
    // LOADING STATE
    // ==========================================
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _lastUpdated = MutableStateFlow(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()
    
    /**
     * Initialize metadata - call this on app startup
     * Note: Firestore metadata requires authentication, so we only initialize session here
     * and defer Firestore calls until user is authenticated
     */
    suspend fun initialize(context: Context) {
        Timber.d("📊 Initializing AppMetadata...")
        _isLoading.value = true
        
        try {
            // Initialize session (local only, no Firestore)
            initializeSession(context)
            
            // Note: Platform stats and feature flags require authentication
            // They will be loaded when initializeWithAuth() is called after login
            
            _lastUpdated.value = System.currentTimeMillis()
            Timber.d("📊 AppMetadata initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to initialize AppMetadata")
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Initialize Firestore-dependent metadata - call after user authentication
     */
    suspend fun initializeWithAuth() {
        Timber.d("📊 Loading authenticated AppMetadata...")
        _isLoading.value = true
        
        try {
            // Registration/login path should stay lightweight.
            // Platform stats are loaded later via explicit refresh flows.
            loadFeatureFlags()
            
            _lastUpdated.value = System.currentTimeMillis()
            Timber.d("📊 AppMetadata loaded from Firestore")
        } catch (e: Exception) {
            Timber.w(e, "📊 Failed to load AppMetadata from Firestore (using defaults)")
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Refresh metadata from Firestore
     */
    suspend fun refresh() {
        Timber.d("📊 Refreshing AppMetadata...")
        _isLoading.value = true
        
        try {
            // Parallelize independent Firestore reads
            coroutineScope {
                val statsDeferred = async { loadPlatformStats() }
                val flagsDeferred = async { loadFeatureFlags() }
                statsDeferred.await()
                flagsDeferred.await()
            }
            _lastUpdated.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to refresh AppMetadata")
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Load platform statistics from Firestore
     */
    private suspend fun loadPlatformStats() {
        try {
            val startOfDay = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayStartTs = Timestamp(java.util.Date(startOfDay))

            coroutineScope {
                val totalJobsDeferred = async {
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS).count().get(AggregateSource.SERVER).await().count.toInt()
                }
                val activeJobsDeferred = async {
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS)
                        .whereEqualTo("status", "open")
                        .count()
                        .get(AggregateSource.SERVER)
                        .await()
                        .count
                        .toInt()
                }
                val workersDeferred = async {
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES)
                        .count()
                        .get(AggregateSource.SERVER)
                        .await()
                        .count
                        .toInt()
                }
                val employersDeferred = async {
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES)
                        .count()
                        .get(AggregateSource.SERVER)
                        .await()
                        .count
                        .toInt()
                }
                val totalApplicationsDeferred = async {
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.APPLICATIONS).count().get(AggregateSource.SERVER).await().count.toInt()
                }
                val jobsTodayDeferred = async {
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOBS)
                        .whereGreaterThanOrEqualTo("createdAt", dayStartTs)
                        .count()
                        .get(AggregateSource.SERVER)
                        .await()
                        .count
                        .toInt()
                }
                val applicationsTodayDeferred = async {
                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.APPLICATIONS)
                        .whereGreaterThanOrEqualTo("createdAt", dayStartTs)
                        .count()
                        .get(AggregateSource.SERVER)
                        .await()
                        .count
                        .toInt()
                }

                _platformStats.value = PlatformStats(
                    totalJobs = totalJobsDeferred.await(),
                    activeJobs = activeJobsDeferred.await(),
                    totalWorkers = workersDeferred.await(),
                    totalEmployers = employersDeferred.await(),
                    totalApplications = totalApplicationsDeferred.await(),
                    jobsPostedToday = jobsTodayDeferred.await(),
                    applicationsToday = applicationsTodayDeferred.await(),
                    averageResponseTime = 0.0,
                    topCategories = emptyList(),
                    topLocations = emptyList()
                )
            }
            Timber.d("📊 Platform stats loaded from core collections: ${_platformStats.value}")
        } catch (e: Exception) {
            Timber.e(e, "📊 Failed to load platform stats")
        }
    }
    
    /**
     * Load feature flags from Firestore
     */
    private suspend fun loadFeatureFlags() {
        _featureFlags.value = FeatureFlags()
        Timber.d("📊 Feature flags using strict defaults (metadata collection removed)")
    }
    
    /**
     * Initialize session information
     */
    private fun initializeSession(context: Context) {
        _sessionInfo.value = SessionInfo(
            sessionId = java.util.UUID.randomUUID().toString(),
            startTime = System.currentTimeMillis(),
            deviceId = getDeviceId(context),
            appVersion = appVersion,
            platform = "Android",
            osVersion = androidVersion
        )
        Timber.d("📊 Session initialized: ${_sessionInfo.value.sessionId}")
    }
    
    /**
     * Get device ID (using Android ID)
     */
    private fun getDeviceId(context: Context): String {
        return try {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Check if app needs force update
     */
    fun needsForceUpdate(): Boolean {
        val forceVersion = _featureFlags.value.forceUpdateVersion
        if (forceVersion.isEmpty()) return false
        return compareVersions(appVersion, forceVersion) < 0
    }
    
    /**
     * Check if app is in maintenance mode
     */
    fun isInMaintenanceMode(): Boolean {
        return _featureFlags.value.maintenanceMode
    }
    
    /**
     * Compare version strings (e.g., "1.2.3" vs "1.2.4")
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }
    
    /**
     * Get full device info string for analytics/debugging
     */
    fun getDeviceInfoString(): String {
        return "$deviceManufacturer $deviceModel (Android $androidVersion, SDK $sdkVersion)"
    }
    
    /**
     * Get app info string for analytics/debugging
     */
    fun getAppInfoString(): String {
        return "$applicationId v$appVersion ($appVersionCode) ${if (isDebug) "[DEBUG]" else ""}"
    }
}

/**
 * Platform statistics - aggregated data about the platform
 */
data class PlatformStats(
    val totalJobs: Int = 0,
    val activeJobs: Int = 0,
    val totalWorkers: Int = 0,
    val totalEmployers: Int = 0,
    val totalApplications: Int = 0,
    val jobsPostedToday: Int = 0,
    val applicationsToday: Int = 0,
    val averageResponseTime: Double = 0.0, // in hours
    val topCategories: List<String> = emptyList(),
    val topLocations: List<String> = emptyList()
)

/**
 * Feature flags - remote configuration for features
 */
data class FeatureFlags(
    // Feature toggles
    val isChatEnabled: Boolean = false, // Chat feature removed
    val isMapViewEnabled: Boolean = true,
    val isSubscriptionEnabled: Boolean = true,
    val isReferralEnabled: Boolean = true,
    val isWorkVerificationEnabled: Boolean = true,
    val isRatingEnabled: Boolean = true,
    val isWhatsAppApplyEnabled: Boolean = false,
    val isDigitalCardEnabled: Boolean = true,
    
    // Limits
    val maxFreeJobPosts: Int = 3,
    val maxFreeApplications: Int = 10,
    val jobExpiryDays: Int = 15,
    
    // App control
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "",
    val minAppVersion: String = "1.0.0",
    val forceUpdateVersion: String = ""
)

/**
 * Session information - current user session
 */
data class SessionInfo(
    val sessionId: String = "",
    val startTime: Long = 0L,
    val deviceId: String = "",
    val appVersion: String = "",
    val platform: String = "Android",
    val osVersion: String = ""
) {
    val sessionDurationMs: Long
        get() = System.currentTimeMillis() - startTime
    
    val sessionDurationMinutes: Long
        get() = sessionDurationMs / 60000
}
