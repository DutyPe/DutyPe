package com.example.dutype.di

import com.dutype.app.BuildConfig
import com.example.dutype.repositories.AIBackendRepository
import com.example.dutype.services.ai.AIBackendService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.io.IOException
import javax.inject.Named
import javax.inject.Singleton

/**
 * AI Module - Hilt Dependency Injection for AI Backend
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    internal fun configuredUrl(value: String, debug: Boolean): HttpUrl? {
        val url = value.trim().toHttpUrlOrNull() ?: return null
        if ((!debug && !url.isHttps) || url.username.isNotEmpty() || url.password.isNotEmpty() ||
            url.query != null || url.fragment != null) return null
        return url.newBuilder().apply {
            if (!url.encodedPath.endsWith("/")) addPathSegment("")
        }.build()
    }
    
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
            .addInterceptor { chain ->
                val configured = configuredUrl(BuildConfig.AI_BACKEND_URL, BuildConfig.DEBUG)
                    ?: throw IOException("AI backend is not configured with a valid URL")
                if (chain.request().url.host != configured.host ||
                    chain.request().url.scheme != configured.scheme ||
                    chain.request().url.port != configured.port) {
                    throw IOException("Unexpected AI backend destination")
                }
                chain.proceed(chain.request())
            }
            .followRedirects(false)
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
            .baseUrl(configuredUrl(BuildConfig.AI_BACKEND_URL, BuildConfig.DEBUG)
                ?: "https://backend-not-configured.invalid/".toHttpUrl())
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
