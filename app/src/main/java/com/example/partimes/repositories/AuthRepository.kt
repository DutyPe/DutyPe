package com.example.partimes.repositories

import com.example.partimes.apis.ApiService
import com.example.partimes.auth.AuthManager
import com.example.partimes.auth.GoogleSignInManager
import com.example.partimes.models.User
import com.example.partimes.models.UserRole
import com.example.partimes.models.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val apiService: ApiService,
    private val authManager: AuthManager,
    private val googleSignInManager: GoogleSignInManager
) {
    
    private val gson = Gson()
    
    suspend fun verifyPhone(firebaseToken: String, phoneNumber: String): Result<Map<String, Any>> {
        return try {
            val request = mapOf(
                "firebaseToken" to firebaseToken,
                "phoneNumber" to phoneNumber
            )
            
            val response = apiService.verifyPhone(request)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.get("success") == true) {
                    // Save token
                    val token = responseBody["token"] as? String
                    if (token != null) {
                        authManager.saveToken(token)
                    }
                    
                    // Save user data
                    val userData = responseBody["user"] as? Map<String, Any>
                    if (userData != null) {
                        val user = gson.fromJson(
                            gson.toJson(userData),
                            User::class.java
                        )
                        authManager.saveUser(user)
                    }
                    
                    authManager.setLoggedIn(true)
                    Result.success(responseBody)
                } else {
                    Result.failure(Exception(responseBody?.get("message") as? String ?: "Phone verification failed"))
                }
            } else {
                Result.failure(Exception("Phone verification failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    
    suspend fun getProfile(): Result<User> {
        return try {
            val response = apiService.getProfile()
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.get("success") == true) {
                    val userData = responseBody["user"] as? Map<String, Any>
                    if (userData != null) {
                        val user = gson.fromJson(
                            gson.toJson(userData),
                            User::class.java
                        )
                        authManager.updateUser(user)
                        Result.success(user)
                    } else {
                        Result.failure(Exception("No user data received"))
                    }
                } else {
                    Result.failure(Exception(responseBody?.get("message") as? String ?: "Failed to get profile"))
                }
            } else {
                Result.failure(Exception("Failed to get profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateProfile(user: User): Result<User> {
        return try {
            val response = apiService.updateProfile(user)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.get("success") == true) {
                    val userData = responseBody["user"] as? Map<String, Any>
                    if (userData != null) {
                        val updatedUser = gson.fromJson(
                            gson.toJson(userData),
                            User::class.java
                        )
                        authManager.updateUser(updatedUser)
                        Result.success(updatedUser)
                    } else {
                        Result.failure(Exception("No user data received"))
                    }
                } else {
                    Result.failure(Exception(responseBody?.get("message") as? String ?: "Failed to update profile"))
                }
            } else {
                Result.failure(Exception("Failed to update profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return try {
            val response = apiService.logout()
            
            if (response.isSuccessful) {
                authManager.logout()
                Result.success(Unit)
            } else {
                // Even if server logout fails, clear local data
                authManager.logout()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // Even if server logout fails, clear local data
            authManager.logout()
            Result.success(Unit)
        }
    }
    
    fun isLoggedIn(): Boolean {
        return authManager.isLoggedIn()
    }
    
    fun getCurrentUser(): User? {
        return authManager.getCurrentUser()
    }
    
    fun getToken(): String? {
        return authManager.getToken()
    }
    
    // Google Sign-In methods
    fun signInWithGoogle(
        idToken: String,
        selectedRole: UserRole,
        phoneNumber: String? = null
    ): Flow<Result<User>> {
        return googleSignInManager.signInWithGoogle(idToken, selectedRole, phoneNumber)
    }
    
    fun switchRole(userId: String, newRole: UserRole): Flow<Result<User>> {
        return googleSignInManager.switchRole(userId, newRole)
    }
    
    fun getCurrentUserFromFirebase(): Flow<Result<User?>> {
        return googleSignInManager.getCurrentUser()
    }
    
    fun signOutFromGoogle(): Flow<Result<Unit>> {
        return googleSignInManager.signOut()
    }
    
    fun isGoogleSignedIn(): Boolean {
        return googleSignInManager.isSignedIn()
    }
}
