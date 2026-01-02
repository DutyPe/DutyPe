package com.example.dutype.di

import android.content.Context
import androidx.room.Room
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
import com.example.dutype.services.ApplicationManagementService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.services.NotificationService
import com.example.dutype.services.RatingService
import com.example.dutype.services.RazorpayService
import com.example.dutype.services.WorkVerificationService
import com.example.dutype.services.BlacklistService
import com.example.dutype.services.ActivityTrackingService
import com.example.dutype.services.ChatService
import com.example.dutype.services.DeviceFingerprintService
import com.example.dutype.services.JobShareImageGenerator
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.repositories.FirestoreSavedJobRepository
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.state.AppStateManager
import com.example.dutype.state.SavedJobsStateManager
import com.example.dutype.state.ProfileSetupStateManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    // ==========================================
    // STATE MANAGERS (In-memory state)
    // ==========================================

    @Provides
    @Singleton
    fun provideProfileSetupStateManager(
        @ApplicationContext context: Context
    ): ProfileSetupStateManager {
        return ProfileSetupStateManager(context)
    }

    @Provides
    @Singleton
    fun provideSavedJobsStateManager(): SavedJobsStateManager {
        return SavedJobsStateManager()
    }

    @Provides
    @Singleton
    fun provideApplicationStateManager(): ApplicationStateManager {
        return ApplicationStateManager()
    }

    @Provides
    @Singleton
    fun provideAppStateManager(
        savedJobsStateManager: SavedJobsStateManager,
        applicationStateManager: ApplicationStateManager,
        profileSetupStateManager: ProfileSetupStateManager
    ): AppStateManager {
        return AppStateManager(savedJobsStateManager, applicationStateManager, profileSetupStateManager)
    }

    // ==========================================
    // ROOM DATABASE (Offline Mode Foundation)
    // ==========================================

    @Provides
    @Singleton
    fun provideDutyPeDatabase(
        @ApplicationContext context: Context
    ): DutyPeDatabase {
        return Room.databaseBuilder(
            context,
            DutyPeDatabase::class.java,
            DutyPeDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
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
        auth: FirebaseAuth
    ): FCMTokenManager {
        return FCMTokenManager(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideProfileCompletionService(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage,
        auth: FirebaseAuth
    ): ProfileCompletionService {
        return ProfileCompletionService(firestore, storage, auth)
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
    fun provideDeviceFingerprintService(
        firestore: FirebaseFirestore
    ): DeviceFingerprintService {
        return DeviceFingerprintService(firestore)
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
        firebaseAuth: FirebaseAuth
    ): AuthManager {
        return AuthManager(context, fcmTokenManager, profileSetupStateManager, appStateManager, firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authManager: AuthManager
    ): com.example.dutype.repositories.AuthRepository {
        return com.example.dutype.repositories.AuthRepository(authManager)
    }

    // ==========================================
    // BUSINESS SERVICES (with Firestore injection)
    // ==========================================

    @Provides
    @Singleton
    fun provideJobApplicationService(
        firestore: FirebaseFirestore,
        notificationService: NotificationService,
        profileCompletionService: ProfileCompletionService,
        applicationStateManager: ApplicationStateManager,
        metadataManager: MetadataManager,
        workVerificationService: WorkVerificationService
    ): JobApplicationService {
        return JobApplicationService(
            firestore,
            notificationService,
            profileCompletionService,
            applicationStateManager,
            metadataManager,
            workVerificationService
        )
    }

    @Provides
    @Singleton
    fun provideApplicationManagementService(
        firestore: FirebaseFirestore,
        notificationService: NotificationService,
        workVerificationService: WorkVerificationService
    ): ApplicationManagementService {
        return ApplicationManagementService(firestore, notificationService, workVerificationService)
    }

    @Provides
    @Singleton
    fun provideRatingService(
        firestore: FirebaseFirestore
    ): RatingService {
        return RatingService(firestore)
    }

    @Provides
    @Singleton
    fun provideRazorpayService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): RazorpayService {
        return RazorpayService(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideWorkVerificationService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): WorkVerificationService {
        return WorkVerificationService(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideBlacklistService(
        firestore: FirebaseFirestore
    ): BlacklistService {
        return BlacklistService(firestore)
    }

    @Provides
    @Singleton
    fun provideActivityTrackingService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): ActivityTrackingService {
        return ActivityTrackingService(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideChatService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): ChatService {
        return ChatService(firestore, auth)
    }

    @Provides
    @Singleton
    fun provideJobShareImageGenerator(): JobShareImageGenerator {
        return JobShareImageGenerator()
    }

    @Provides
    @Singleton
    fun provideReportingService(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): com.example.dutype.services.ReportingService {
        return com.example.dutype.services.ReportingService(firestore, auth)
    }

    // ==========================================
    // REPOSITORIES (compose services with caching)
    // ==========================================

    @Provides
    @Singleton
    fun provideFirestoreJobRepository(
        firestoreService: FirestoreService,
        auth: FirebaseAuth,
        cacheManager: JobCacheManager
    ): FirestoreJobRepository {
        return FirestoreJobRepository(firestoreService, auth, cacheManager)
    }

    @Provides
    @Singleton
    fun provideFirestoreSavedJobRepository(
        firestoreService: FirestoreService,
        cacheManager: JobCacheManager,
        auth: FirebaseAuth
    ): FirestoreSavedJobRepository {
        return FirestoreSavedJobRepository(firestoreService, cacheManager, auth)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firestoreService: FirestoreService,
        auth: FirebaseAuth,
        cacheManager: JobCacheManager
    ): com.example.dutype.repositories.UserRepository {
        return com.example.dutype.repositories.UserRepository(firestoreService, auth, cacheManager)
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
    fun provideLocationPreferences(
        @ApplicationContext context: Context
    ): com.example.dutype.location.LocationPreferences {
        return com.example.dutype.location.LocationPreferences(context)
    }

    @Provides
    @Singleton
    fun provideImagePreloader(): com.example.dutype.utils.ImagePreloader {
        return com.example.dutype.utils.ImagePreloader()
    }

    // ==========================================
    // METADATA SERVICES (with Firestore injection)
    // ==========================================

    @Provides
    @Singleton
    fun provideAppMetadata(
        firestore: FirebaseFirestore
    ): AppMetadata {
        return AppMetadata(firestore)
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
}
