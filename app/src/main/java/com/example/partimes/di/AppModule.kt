package com.example.partimes.di

import android.content.Context
import com.example.partimes.apis.ApiService
import com.example.partimes.network.ApiClient
import com.example.partimes.repositories.JobRepository
import com.example.partimes.repositories.AuthRepository
import com.example.partimes.data.ApplicationFormDataStore
import com.example.partimes.auth.AuthManager
import com.example.partimes.auth.GoogleSignInManager
import com.example.partimes.services.FirestoreService
import com.example.partimes.services.JobApplicationService
import com.example.partimes.services.ProfileCompletionService
import com.example.partimes.services.NotificationService
import com.example.partimes.repositories.FirestoreJobRepository
import com.example.partimes.repositories.FirestoreSavedJobRepository
import com.example.partimes.state.ApplicationStateManager
import com.example.partimes.state.SavedJobsStateManager
import com.example.partimes.state.ProfileSetupStateManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return ApiClient.getApiService()
    }

    @Provides
    @Singleton
    fun provideAuthManager(@ApplicationContext context: Context): AuthManager {
        return AuthManager(context)
    }

    @Provides
    @Singleton
    fun provideFirestoreService(): FirestoreService {
        return FirestoreService()
    }

    @Provides
    @Singleton
    fun provideGoogleSignInManager(@ApplicationContext context: Context, apiService: ApiService): GoogleSignInManager {
        return GoogleSignInManager(context, apiService)
    }

    @Provides
    @Singleton
    fun provideJobRepository(apiService: ApiService, authManager: AuthManager): JobRepository {
        return JobRepository(apiService, authManager)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(apiService: ApiService, authManager: AuthManager, googleSignInManager: GoogleSignInManager): AuthRepository {
        return AuthRepository(apiService, authManager, googleSignInManager)
    }

    @Provides
    @Singleton
    fun provideApplicationFormDataStore(
        @ApplicationContext context: Context
    ): ApplicationFormDataStore {
        return ApplicationFormDataStore(context)
    }

    @Provides
    @Singleton
    fun provideSavedJobsStateManager(): SavedJobsStateManager {
        return SavedJobsStateManager()
    }
    
    @Provides
    @Singleton
    fun provideNotificationService(@ApplicationContext context: Context): NotificationService {
        return NotificationService(context, FirebaseFirestore.getInstance())
    }

    @Provides
    @Singleton
    fun provideJobApplicationService(
        notificationService: NotificationService,
        profileCompletionService: ProfileCompletionService,
        applicationStateManager: ApplicationStateManager
    ): JobApplicationService {
        return JobApplicationService(notificationService, profileCompletionService, applicationStateManager)
    }

    @Provides
    @Singleton
    fun provideProfileSetupStateManager(@ApplicationContext context: Context): ProfileSetupStateManager {
        return ProfileSetupStateManager(context)
    }

    @Provides
    @Singleton
    fun provideProfileCompletionService(
        firestoreService: FirestoreService,
        auth: FirebaseAuth,
        profileSetupStateManager: ProfileSetupStateManager
    ): ProfileCompletionService {
        return ProfileCompletionService(firestoreService, auth, profileSetupStateManager)
    }

    @Provides
    @Singleton
    fun provideApplicationStateManager(): ApplicationStateManager {
        return ApplicationStateManager()
    }


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
    fun provideFirestoreJobRepository(firestoreService: FirestoreService, auth: FirebaseAuth): FirestoreJobRepository {
        return FirestoreJobRepository(firestoreService, auth)
    }
    
    @Provides
    @Singleton
    fun provideFirestoreSavedJobRepository(firestoreService: FirestoreService): FirestoreSavedJobRepository {
        return FirestoreSavedJobRepository(firestoreService)
    }
}
