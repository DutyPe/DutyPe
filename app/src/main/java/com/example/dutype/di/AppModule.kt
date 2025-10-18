package com.example.dutype.di

import android.content.Context
import com.example.dutype.apis.ApiService
import com.example.dutype.network.ApiClient
import com.example.dutype.repositories.JobRepository
import com.example.dutype.repositories.AuthRepository
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.auth.AuthManager
import com.example.dutype.auth.GoogleSignInManager
import com.example.dutype.services.FirestoreService
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.services.NotificationService
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.repositories.FirestoreSavedJobRepository
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.state.SavedJobsStateManager
import com.example.dutype.state.ProfileSetupStateManager
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
    fun provideProfileCompletionService(): ProfileCompletionService {
        return ProfileCompletionService()
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
