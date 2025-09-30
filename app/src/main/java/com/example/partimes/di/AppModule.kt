package com.example.partimes.di

import android.content.Context
import com.example.partimes.apis.ApiService
import com.example.partimes.network.ApiClient
import com.example.partimes.repositories.JobRepository
import com.example.partimes.repositories.AuthRepository
import com.example.partimes.repositories.ApplicationRepository
import com.example.partimes.repositories.SavedJobRepository
import com.example.partimes.repositories.JobRecommendationRepository
import com.example.partimes.repositories.JobSharingRepository
import com.example.partimes.repositories.LocationRepository
import com.example.partimes.data.ApplicationFormDataStore
import com.example.partimes.auth.AuthManager
import com.example.partimes.auth.GoogleSignInManager
import com.example.partimes.services.FileUploadService
import com.example.partimes.services.FirestoreService
import com.example.partimes.notifications.services.NotificationService
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
    fun provideApplicationRepository(apiService: ApiService, authManager: AuthManager): ApplicationRepository {
        return ApplicationRepository(apiService, authManager)
    }

    @Provides
    @Singleton
    fun provideSavedJobRepository(authManager: AuthManager): SavedJobRepository {
        return SavedJobRepository(authManager)
    }

    @Provides
    @Singleton
    fun provideJobRecommendationRepository(authManager: AuthManager): JobRecommendationRepository {
        return JobRecommendationRepository(authManager)
    }

    @Provides
    @Singleton
    fun provideJobSharingRepository(): JobSharingRepository {
        return JobSharingRepository()
    }

    @Provides
    @Singleton
    fun provideLocationRepository(): LocationRepository {
        return LocationRepository()
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
    fun provideFileUploadService(): FileUploadService {
        return FileUploadService()
    }

    @Provides
    @Singleton
    fun provideNotificationService(): NotificationService {
        return NotificationService()
    }
}
