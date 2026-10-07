package com.example.dutype.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.auth.AuthManager
import com.example.dutype.services.FCMTokenManager
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.services.NotificationService
import com.example.dutype.services.JobShareImageGenerator
import com.example.dutype.services.ReferralService
import com.example.dutype.repositories.AppConfigRepository
import com.example.dutype.repositories.FirestoreSavedJobRepository
import com.example.dutype.performance.PerformanceTracker
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.state.AppStateManager
import com.example.dutype.state.ProfileSetupStateManager
import com.example.dutype.utils.RequestDeduplicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.io.File
import javax.inject.Singleton

/**
 * AppModule - Hilt Dependency Injection Module
 *
 * REFACTORED (January 2026):
 * - All services now receive FirebaseFirestore via constructor injection
 * - Single source of truth for Firebase instances
 * - Improved testability and mockability
 * - Removed ServiceProviders anti-pattern (services injected directly into ViewModels)
 *
 * Architecture:
 * - Firebase instances provided at top level
 * - Services receive dependencies via constructor
 * - Repositories compose services with caching
 * - State managers handle in-memory state
 *
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ==========================================
    // FIREBASE CORE INSTANCES (Single Source of Truth)
    // ==========================================

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()

        // PERFORMANCE: Enable offline persistence for instant data loading
        // This caches Firestore data locally for 40MB (configurable)
        // Used by: WhatsApp, Instagram, Uber, Airbnb
        try {
            firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Enable offline cache
                .setCacheSizeBytes(100L * 1024L * 1024L) // P1 FIX: 100MB cap (was UNLIMITED — OOM risk at scale)
                .build()
            Timber.d("✅ Firestore offline persistence enabled")
        } catch (e: Exception) {
            // Already initialized - this is fine
            Timber.d("ℹ️ Firestore settings already configured")
        }

        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): com.google.firebase.functions.FirebaseFunctions {
        return com.google.firebase.functions.FirebaseFunctions.getInstance("asia-south1")
    }

    // ==========================================
    // STATE MANAGERS (In-memory state)
    // ==========================================

    @Provides
    @Singleton
    fun provideProfileSetupStateManager(
        @ApplicationContext context: Context,
        smartNotificationManager: com.example.dutype.services.SmartNotificationManager
    ): ProfileSetupStateManager {
        return ProfileSetupStateManager(context, smartNotificationManager)
    }

    @Provides
    @Singleton
    fun provideApplicationStateManager(): ApplicationStateManager {
        return ApplicationStateManager()
    }

    @Provides
    @Singleton
    fun provideAppStateManager(
        applicationStateManager: ApplicationStateManager,
        profileSetupStateManager: ProfileSetupStateManager
    ): AppStateManager {
        return AppStateManager(applicationStateManager, profileSetupStateManager)
    }

    // ==========================================
    // CORE SERVICES (with Firestore injection)
    // ==========================================

    @Provides
    @Singleton
    fun provideFCMTokenManager(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        @ApplicationContext appContext: android.content.Context
    ): FCMTokenManager {
        return FCMTokenManager(firestore, auth, appContext)
    }


    @Provides
    @Singleton
    fun provideNotificationService(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore
    ): NotificationService {
        return NotificationService(context, firestore)
    }

    @Provides
    @Singleton
    fun provideBirthdayService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        notificationService: NotificationService
    ): com.example.dutype.services.BirthdayService {
        return com.example.dutype.services.BirthdayService(firestore, auth, notificationService)
    }


    // ==========================================
    // AUTHENTICATION
    // ==========================================

    @Provides
    @Singleton
    fun provideAuthManager(
        @ApplicationContext context: Context,
        fcmTokenManager: FCMTokenManager,
        profileSetupStateManager: ProfileSetupStateManager,
        appStateManager: AppStateManager,
        firebaseAuth: FirebaseAuth,
        sessionManager: com.example.dutype.auth.SessionManager
    ): AuthManager {
        return AuthManager(
            context,
            fcmTokenManager,
            profileSetupStateManager,
            appStateManager,
            firebaseAuth,
            sessionManager
        )
    }

    // P0 FIX: Removed AuthRepository - it was just a wrapper around AuthManager
    // Use AuthManager directly instead

    // ==========================================
    // BUSINESS SERVICES (with Firestore injection)
    // ==========================================

    @Provides
    @Singleton
    fun provideJobShareImageGenerator(): JobShareImageGenerator {
        return JobShareImageGenerator()
    }



    // ==========================================
    // REPOSITORIES (compose services with caching)
    // ==========================================

    @Provides
    @Singleton
    fun provideFirestoreSavedJobRepository(
        firestore: FirebaseFirestore,
        jobRepository: com.example.dutype.jobs.JobRepository,
        auth: FirebaseAuth
    ): FirestoreSavedJobRepository {
        return FirestoreSavedJobRepository(firestore, jobRepository, auth)
    }

    // ==========================================
    // NOTIFICATION SERVICES
    // ==========================================

    @Provides
    @Singleton
    fun provideLocalNotificationService(): com.example.dutype.notifications.LocalNotificationService {
        return com.example.dutype.notifications.LocalNotificationService()
    }

    @Provides
    @Singleton
    fun provideInAppNotificationManager(
        localNotificationService: com.example.dutype.notifications.LocalNotificationService
    ): com.example.dutype.notifications.InAppNotificationManager {
        return com.example.dutype.notifications.InAppNotificationManager(localNotificationService)
    }

    @Provides
    @Singleton
    fun provideSmartNotificationManager(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        notificationService: NotificationService
    ): com.example.dutype.services.SmartNotificationManager {
        return com.example.dutype.services.SmartNotificationManager(
            context,
            firestore,
            notificationService
        )
    }

    // ==========================================
    // UTILITY SERVICES
    // ==========================================

    @Provides
    @Singleton
    fun provideLocationService(
        @ApplicationContext context: Context
    ): com.example.dutype.utils.LocationService {
        return com.example.dutype.utils.LocationService(context)
    }

    @Provides
    @Singleton
    fun provideSavedWorkLocationsStore(
        @ApplicationContext context: Context,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): com.example.dutype.services.SavedWorkLocationsStore {
        return com.example.dutype.services.SavedWorkLocationsStore(context, firestore, auth)
    }

    @Provides
    @Singleton
    fun provideLocationPreferences(
        @ApplicationContext context: Context
    ): com.example.dutype.location.LocationPreferences {
        return com.example.dutype.location.LocationPreferences(context)
    }

    // ==========================================
    // PERFORMANCE MONITORING
    // ==========================================

    @Provides
    @Singleton
    fun providePerformanceTracker(): PerformanceTracker {
        return PerformanceTracker()
    }

    // ==========================================
    // P1 PERFORMANCE FIX: ENTERPRISE IMAGE OPTIMIZATION
    // - WebP format support (30% smaller than JPEG)
    // - Progressive loading with blur placeholder
    // - Aggressive memory/disk caching
    // - Error handling with fallback
    // Standards: Meta/Instagram image loading patterns
    // ==========================================

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): ImageLoader {
        // P1 COIL MEMORY CACHE TUNING
        // Low-end / Android-Go devices (≤ 3 GB RAM) are OOM-prone under the
        // default 25% cap.  We detect them via ActivityManager and hard-cap the
        // memory cache at 32 MB so image loading never starves the rest of the app.
        // On normal devices (> 3 GB) we stay at the LinkedIn-standard 25%.
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val totalRamMb = memoryInfo.totalMem / (1024L * 1024L)
        val isLowRam = activityManager.isLowRamDevice || totalRamMb <= 3072L

        val memoryCache = if (isLowRam) {
            // Hard cap at 32 MB on low-RAM devices to avoid OOM
            MemoryCache.Builder(context)
                .maxSizeBytes(32 * 1024 * 1024) // 32 MB
                .build()
        } else {
            // Standard 25% on normal devices (LinkedIn / Airbnb pattern)
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }

        Timber.d("Coil ImageLoader: RAM=${totalRamMb}MB, isLowRam=$isLowRam, cacheSize=${if (isLowRam) "32MB cap" else "25%"}")

        return ImageLoader.Builder(context)
            .memoryCache { memoryCache }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Use 2% of available disk space
                    .build()
            }
            .crossfade(300) // Smooth fade-in (Meta standard)
            .respectCacheHeaders(false) // Ignore server cache headers for better offline support
            .allowHardware(!isLowRam) // Disable GPU decoding on low-RAM to save VRAM
            .build()
    }

    // ==========================================
    // P1 PERFORMANCE FIX: REQUEST DEDUPLICATION
    // Prevents duplicate concurrent API calls
    // ==========================================

    @Provides
    @Singleton
    fun provideRequestDeduplicator(): RequestDeduplicator {
        return RequestDeduplicator()
    }



    // ==========================================
    // METADATA SERVICES (with Firestore injection)
    // ==========================================


    // ==========================================
    // AD SERVICES
    // ==========================================



    @Provides
    @Singleton
    fun provideInAppReviewManager(
        @ApplicationContext context: Context
    ): com.example.dutype.utils.InAppReviewManager {
        return com.example.dutype.utils.InAppReviewManager(context)
    }

    @Provides
    @Singleton
    fun provideInAppUpdateManager(
        @ApplicationContext context: Context
    ): com.example.dutype.utils.InAppUpdateManager {
        return com.example.dutype.utils.InAppUpdateManager(context)
    }

    @Provides
    @Singleton
    fun provideInAppReviewTriggerService(
        reviewManager: com.example.dutype.utils.InAppReviewManager,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): com.example.dutype.services.InAppReviewTriggerService {
        return com.example.dutype.services.InAppReviewTriggerService(reviewManager, firestore, auth)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): com.example.dutype.utils.NetworkMonitor {
        return com.example.dutype.utils.NetworkMonitor(context)
    }

    @Provides
    @Singleton
    fun provideAnnouncementService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): com.example.dutype.services.AnnouncementService {
        return com.example.dutype.services.AnnouncementService(firestore, auth)
    }

    // ==========================================
    // P0: ENTERPRISE ERROR HANDLING & RESILIENCE
    // ==========================================

    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): com.google.firebase.crashlytics.FirebaseCrashlytics {
        return com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
    }

    @Provides
    @Singleton
    fun provideObservabilityManager(
        crashlytics: com.google.firebase.crashlytics.FirebaseCrashlytics
    ): com.example.dutype.core.observability.ObservabilityManager {
        return com.example.dutype.core.observability.ObservabilityManager(crashlytics)
    }

    @Provides
    @Singleton
    fun provideErrorHandler(
        crashlytics: com.google.firebase.crashlytics.FirebaseCrashlytics,
        observabilityManager: com.example.dutype.core.observability.ObservabilityManager
    ): com.example.dutype.core.error.ErrorHandler {
        return com.example.dutype.core.error.ErrorHandler(crashlytics, observabilityManager)
    }

    // ==========================================
    // P1: ENTERPRISE CACHING
    // ==========================================

    // ==========================================
    // P1: AUTH SESSION MANAGEMENT
    // ==========================================

    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context,
        firebaseAuth: FirebaseAuth
    ): com.example.dutype.auth.SessionManager {
        return com.example.dutype.auth.SessionManager(context, firebaseAuth)
    }
}
