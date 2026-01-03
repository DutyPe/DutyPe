package com.example.dutype.di

import com.dutype.app.BuildConfig
import com.example.dutype.repositories.AIBackendRepository
import com.example.dutype.services.ai.AIBackendService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * AI Module - Hilt Dependency Injection for AI Backend
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    
    // For emulator use 10.0.2.2, for physical device use your computer's IP
    // Current: Using computer's local IP for physical device testing
    private const val AI_BACKEND_URL = "http://10.91.59.173:8000/"
    
    @Provides
    @Singleton
    @Named("AIBackendOkHttpClient")
    fun provideAIBackendOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                chain.proceed(requestBuilder.build())
            }
            .build()
    }
    
    @Provides
    @Singleton
    @Named("AIBackendRetrofit")
    fun provideAIBackendRetrofit(
        @Named("AIBackendOkHttpClient") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AI_BACKEND_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideAIBackendService(
        @Named("AIBackendRetrofit") retrofit: Retrofit
    ): AIBackendService {
        return retrofit.create(AIBackendService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideAIBackendRepository(
        aiBackendService: AIBackendService
    ): AIBackendRepository {
        return AIBackendRepository(aiBackendService)
    }
}
