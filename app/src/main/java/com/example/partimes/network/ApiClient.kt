package com.example.partimes.network

import com.example.partimes.apis.ApiService
import com.example.partimes.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    // Dynamic base URL configuration for different environments
    private fun getBaseUrl(): String {
        return when {
            // Check if running on emulator
            android.os.Build.FINGERPRINT.contains("generic") -> "http://10.0.2.2:8080"
            // For real devices connected via USB - using your actual IP address
            else -> "http://10.182.188.3:8080" // Your computer's actual IP address
        }
    }

    private val BASE_URL = getBaseUrl()

    private var authManager: AuthManager? = null
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null
    
    fun initialize(authManager: AuthManager) {
        this.authManager = authManager
        // Recreate retrofit with the new auth manager
        createRetrofit()
    }
    
    private fun createRetrofit() {
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val currentUser = authManager?.getCurrentUser()
            val firebaseUid = currentUser?.id

            val newRequest = if (firebaseUid != null) {
                originalRequest.newBuilder()
                    .header("X-Firebase-UID", firebaseUid)
                    .build()
            } else {
                originalRequest
            }

            chain.proceed(newRequest)
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit?.create(ApiService::class.java)
    }
    
    fun getApiService(): ApiService {
        if (apiService == null) {
            createRetrofit()
        }
        return apiService ?: throw IllegalStateException("ApiService not initialized. Call initialize() first.")
    }
}
