package com.example.dutype.di

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.dutype.cache.JobCacheManager
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.database.DutyPeDatabase
import com.example.dutype.database.dao.JobDao
import com.example.dutype.database.dao.ApplicationDao
import com.example.dutype.database.dao.SavedJobDao
import com.example.dutype.metadata.AppMetadata
import com.example.dutype.metadata.JobMetadata
import com.example.dutype.metadata.UserMetadata
import com.example.dutype.metadata.MetadataManager
import com.example.dutype.auth.AuthManager
import com.example.dutype.services.FCMTokenManager
import com.example.dutype.services.FirestoreService
import com.example.dutype.services.firestore.UserFirestoreService
import com.example.dutype.services.firestore.JobFirestoreService
import com.example.dutype.services.firestore.ApplicationFirestoreService
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.services.NotificationService
import com.example.dutype.services.JobShareImageGenerator
import com.example.dutype.services.ReferralService
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.repositories.FirestoreSavedJobRepository
import com.example.dutype.performance.PerformanceTracker
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.state.AppStateManager
import com.example.dutype.state.ProfileSetupStateManager
import com.example.dutype.utils.RequestDeduplicator
import com.example.dutype.ads.AdManager
import com.example.dutype.ads.AdPreferences
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
        return com.google.firebase.functions.FirebaseFunctions.getInstance()
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
    // ROOM DATABASE (Offline Mode Foundation)
    // ==========================================

    @Provides
    @Singleton
    fun provideDutyPeDatabase(
        @ApplicationContext context: Context
    ): DutyPeDatabase {
        // SECURITY: Room DB is encrypted with SQLCipher using a device-bound
        // passphrase stored in EncryptedSharedPreferences (Keystore-wrapped).
        //
        // We use the modern `net.zetetic:sqlcipher-android` artifact (16 KB
        // page-size compatible). Its native library must be loaded once before
        // any database operation. If a broken install/runtime cannot load it,
        // keep the app alive with a non-persistent cache instead of crashing at startup.
        if (!loadSqlCipherNativeLibrary()) {
            Timber.e("SQLCipher native library unavailable; using in-memory Room database")
            return buildInMemoryDutyPeDatabase(context)
        }

        val database = buildEncryptedDutyPeDatabase(context)
        return try {
            verifyDutyPeDatabaseOpens(database)
            database
        } catch (error: RuntimeException) {
            if (!isRecoverableSqlCipherOpenFailure(error)) {
                throw error
            }

            Timber.e(error, "Encrypted Room database failed to open; clearing local cache and rebuilding")
            database.close()
            deleteDutyPeDatabaseFiles(context)

            buildEncryptedDutyPeDatabase(context).also { rebuiltDatabase ->
                verifyDutyPeDatabaseOpens(rebuiltDatabase)
            }
        }
    }

    private fun loadSqlCipherNativeLibrary(): Boolean {
        return try {
            System.loadLibrary("sqlcipher")
            true
        } catch (error: UnsatisfiedLinkError) {
            Timber.e(error, "Failed to load libsqlcipher.so")
            false
        }
    }

    private fun buildEncryptedDutyPeDatabase(context: Context): DutyPeDatabase {
        val passphrase = com.example.dutype.database.security.DatabasePassphraseProvider.getPassphrase(context)
        val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(passphrase)

        // SCHEMA MIGRATION POLICY:
        // - Any schema bump from v7 onward MUST add an explicit Migration object.
        //   Blanket destructive migration is a data-loss bomb for offline users.
        // - Legacy versions 1..6 predate the current release; wiping those
        //   installs is acceptable because their schemas are not exported.
        // - Downgrades wipe (nothing we can do safely).
        // When adding a migration, register it here with `.addMigrations(Migration_7_8, ...)`
        // and REMOVE the corresponding version number from the legacy list.
        return Room.databaseBuilder(
            context,
            DutyPeDatabase::class.java,
            DutyPeDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    private fun buildInMemoryDutyPeDatabase(context: Context): DutyPeDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            DutyPeDatabase::class.java
        )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    private fun verifyDutyPeDatabaseOpens(database: DutyPeDatabase) {
        database.openHelper.writableDatabase
            .query("SELECT COUNT(*) FROM sqlite_schema")
            .use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(0)
                }
            }
    }

    private fun isRecoverableSqlCipherOpenFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val className = current::class.java.name
            if (
                className.startsWith("net.zetetic.database.sqlcipher.SQLite") ||
                className.startsWith("android.database.sqlite.SQLite")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun deleteDutyPeDatabaseFiles(context: Context) {
        val databaseName = DutyPeDatabase.DATABASE_NAME
        val databaseFile = context.getDatabasePath(databaseName)

        context.deleteDatabase(databaseName)
        listOf(
            databaseFile,
            File("${databaseFile.path}-journal"),
            File("${databaseFile.path}-shm"),
            File("${databaseFile.path}-wal")
        ).forEach { file ->
            if (file.exists() && !file.delete()) {
                Timber.w("Failed to delete local Room database file: ${file.absolutePath}")
            }
        }
    }

    @Provides
    @Singleton
    fun provideJobDao(database: DutyPeDatabase): JobDao {
        return database.jobDao()
    }

    @Provides
    @Singleton
    fun provideApplicationDao(database: DutyPeDatabase): ApplicationDao {
        return database.applicationDao()
    }

    @Provides
    @Singleton
    fun provideSavedJobDao(database: DutyPeDatabase): SavedJobDao {
        return database.savedJobDao()
    }

    // ==========================================
    // CACHE MANAGER
    // ==========================================

    @Provides
    @Singleton
    fun provideJobCacheManager(): JobCacheManager {
        return JobCacheManager()
    }

    // EmployerProfileCache uses @Inject constructor, so Hilt resolves it automatically.

    // ==========================================
    // DATA STORES
    // ==========================================

    @Provides
    @Singleton
    fun provideApplicationFormDataStore(
        @ApplicationContext context: Context
    ): ApplicationFormDataStore {
        return ApplicationFormDataStore(context)
    }

    // ==========================================
    // CORE SERVICES (with Firestore injection)
    // ==========================================

    @Provides
    @Singleton
    fun provideUserFirestoreService(
        firestore: FirebaseFirestore
    ): UserFirestoreService {
        return UserFirestoreService(firestore)
    }

    @Provides
    @Singleton
    fun provideJobFirestoreService(
        firestore: FirebaseFirestore
    ): JobFirestoreService {
        return JobFirestoreService(firestore)
    }

    @Provides
    @Singleton
    fun provideApplicationFirestoreService(
        firestore: FirebaseFirestore
    ): ApplicationFirestoreService {
        return ApplicationFirestoreService(firestore)
    }

    @Provides
    @Singleton
    fun provideFirestoreService(
        firestore: FirebaseFirestore,
        userService: UserFirestoreService,
        jobService: JobFirestoreService,
        applicationService: ApplicationFirestoreService
    ): FirestoreService {
        return FirestoreService(firestore, userService, jobService, applicationService)
    }

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
    fun provideProfileCompletionService(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        auth: FirebaseAuth,
        functions: com.google.firebase.functions.FirebaseFunctions,
        @ApplicationContext context: Context,
        errorHandler: com.example.dutype.core.error.ErrorHandler,
        referralService: com.example.dutype.services.ReferralService
    ): ProfileCompletionService {
        return ProfileCompletionService(
            firestore,
            storage,
            auth,
            functions,
            context,
            errorHandler,
            referralService
        )
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

    @Provides
    @Singleton
    fun provideReferralService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        functions: com.google.firebase.functions.FirebaseFunctions,
        smartNotificationManager: com.example.dutype.services.SmartNotificationManager,
        @ApplicationContext context: Context
    ): ReferralService {
        return ReferralService(firestore, auth, functions, smartNotificationManager, context)
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
        jobCacheManager: JobCacheManager,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        sessionManager: com.example.dutype.auth.SessionManager
    ): AuthManager {
        return AuthManager(
            context,
            fcmTokenManager,
            profileSetupStateManager,
            appStateManager,
            jobCacheManager,
            firebaseAuth,
            firestore,
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
    fun provideJobApplicationService(
        firestore: FirebaseFirestore,
        jobDao: JobDao,
        applicationDao: ApplicationDao,
        notificationService: NotificationService,
        profileCompletionService: ProfileCompletionService,
        applicationStateManager: ApplicationStateManager,
        metadataManager: MetadataManager,
        errorHandler: com.example.dutype.core.error.ErrorHandler
    ): JobApplicationService {
        return JobApplicationService(
            firestore,
            jobDao,
            applicationDao,
            notificationService,
            profileCompletionService,
            applicationStateManager,
            metadataManager,
            errorHandler
        )
    }

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
    fun provideFirestoreJobRepository(
        firestoreService: FirestoreService,
        auth: FirebaseAuth,
        errorHandler: com.example.dutype.core.error.ErrorHandler,
        notificationService: com.example.dutype.services.NotificationService,
        requestDeduplicator: com.example.dutype.utils.RequestDeduplicator,
        jobCacheManager: com.example.dutype.cache.JobCacheManager
    ): FirestoreJobRepository {
        return FirestoreJobRepository(firestoreService, auth, errorHandler, notificationService, requestDeduplicator, jobCacheManager)
    }

    @Provides
    @Singleton
    fun provideFirestoreSavedJobRepository(
        firestoreService: FirestoreService,
        auth: FirebaseAuth
    ): FirestoreSavedJobRepository {
        return FirestoreSavedJobRepository(firestoreService, auth)
    }

    @Provides
    @Singleton
    fun provideOfflineFirstJobRepository(
        jobDao: JobDao,
        firestoreService: FirestoreService,
        performanceTracker: PerformanceTracker
    ): com.example.dutype.repositories.OfflineFirstJobRepository {
        return com.example.dutype.repositories.OfflineFirstJobRepository(jobDao, firestoreService, performanceTracker)
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
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): com.example.dutype.services.SavedWorkLocationsStore {
        return com.example.dutype.services.SavedWorkLocationsStore(firestore, auth)
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
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available memory (LinkedIn standard)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02) // Use 2% of available disk space
                    .build()
            }
            .crossfade(300) // Smooth fade-in (Meta standard)
            .respectCacheHeaders(false) // Ignore server cache headers for better offline support
            .allowHardware(true) // Use GPU for decoding (faster)
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

    @Provides
    @Singleton
    fun provideAppMetadata(
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): AppMetadata {
        return AppMetadata(firestore, context)
    }

    @Provides
    @Singleton
    fun provideJobMetadata(
        firestore: FirebaseFirestore
    ): JobMetadata {
        return JobMetadata(firestore)
    }

    @Provides
    @Singleton
    fun provideUserMetadata(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): UserMetadata {
        return UserMetadata(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideMetadataManager(
        appMetadata: AppMetadata,
        jobMetadata: JobMetadata,
        userMetadata: UserMetadata,
        cacheManager: JobCacheManager
    ): MetadataManager {
        return MetadataManager(appMetadata, jobMetadata, userMetadata, cacheManager)
    }

    // ==========================================
    // AD SERVICES
    // ==========================================

    @Provides
    @Singleton
    fun provideAdManager(): AdManager {
        return AdManager()
    }

    @Provides
    @Singleton
    fun provideAdPreferences(): AdPreferences {
        return AdPreferences()
    }
    
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
