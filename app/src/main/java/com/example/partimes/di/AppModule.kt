package com.example.partimes.di

import android.content.Context
import com.example.partimes.apis.ApiService
import com.example.partimes.apis.RetrofitClient
import com.example.partimes.database.JobDao
import com.example.partimes.database.ParTimesDatabase
import com.example.partimes.database.provideDatabase
import com.example.partimes.repository.JobRepository
import com.example.partimes.repository.SavedJobsRepository
import com.example.partimes.repository.UserRepository
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
    fun provideDatabase(@ApplicationContext context: Context): ParTimesDatabase {
        return provideDatabase(context)
    }

    @Provides
    fun provideJobDao(database: ParTimesDatabase): JobDao {
        return database.jobDao()
    }

    @Provides
    @Singleton
    fun provideJobRepository(jobDao: JobDao, apiService: ApiService): JobRepository {
        return JobRepository(jobDao, apiService)
    }

    @Provides
    @Singleton
    fun provideUserRepository(): UserRepository {
        return UserRepository()
    }

    @Provides
    @Singleton
    fun provideSavedJobsRepository(@ApplicationContext context: Context): SavedJobsRepository {
        return SavedJobsRepository(context)
    }

    @Provides
    @Singleton
    fun provideApiService() = RetrofitClient.apiService
}
