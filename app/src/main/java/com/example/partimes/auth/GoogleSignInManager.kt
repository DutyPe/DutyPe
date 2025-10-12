package com.example.partimes.auth

import android.content.Context
import com.example.partimes.models.User
import com.example.partimes.models.UserRole
import com.example.partimes.apis.ApiService
import com.example.partimes.services.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class GoogleSignInManager(
    private val context: Context,
    private val apiService: ApiService? = null
) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firestoreService: FirestoreService = FirestoreService()
    
    /**
     * Sign in with Google using ID token
     * @param idToken Google ID token
     * @param selectedRole User's selected role
     * @param phoneNumber Optional phone number for contact
     * @return Flow<Result<User>>
     */
    fun signInWithGoogle(
        idToken: String,
        selectedRole: UserRole
    ): Flow<Result<User>> = flow {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = createOrUpdateUser(firebaseUser.uid, firebaseUser, selectedRole)
                emit(Result.success(user))
            } else {
                emit(Result.failure(Exception("User data not available")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create or update user in Firestore (Primary Database)
     */
    private suspend fun createOrUpdateUser(
        userId: String,
        firebaseUser: com.google.firebase.auth.FirebaseUser,
        selectedRole: UserRole
    ): User {
        return try {
            // Check if user exists in Firestore
            val existingUserResult = firestoreService.getUserById(userId)
            
            val user = if (existingUserResult.isSuccess && existingUserResult.getOrNull() != null) {
                // User exists, update last login and role if needed
                val existingUser = existingUserResult.getOrThrow()!!
                existingUser.copy(
                    lastLoginAt = System.currentTimeMillis(),
                    role = selectedRole, // Allow role switching
                    profileImageUrl = firebaseUser.photoUrl?.toString() ?: existingUser.profileImageUrl
                )
            } else {
                // New user, create profile - check if profile is complete based on available data
                val hasBasicInfo = firebaseUser.displayName?.isNotBlank() == true && 
                                 firebaseUser.email?.isNotBlank() == true
                
                User(
                    id = userId,
                    email = firebaseUser.email ?: "",
                    fullName = firebaseUser.displayName ?: "",
                    profileImageUrl = firebaseUser.photoUrl?.toString(),
                    role = selectedRole,
                    isProfileComplete = hasBasicInfo, // Set to true if we have basic info from Google
                    isVerified = true, // Google verified
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = System.currentTimeMillis()
                )
            }
            
            // Save to Firestore using FirestoreService
            println("🔥 Attempting to save user to Firestore: ${user.email}")
            val saveResult = firestoreService.createOrUpdateUser(user)
            if (saveResult.isSuccess) {
                println("✅ User successfully saved to Firestore!")
                // Optional: Sync with backend for backup
                syncUserWithBackend(user)
                user
            } else {
                val error = saveResult.exceptionOrNull()?.message ?: "Unknown error"
                println("❌ Failed to save user to Firestore: $error")
                throw Exception("Failed to save user to Firestore: $error")
            }
        } catch (e: Exception) {
            throw Exception("Failed to create/update user: ${e.message}")
        }
    }
    
    /**
     * Sync user data with Firebase Firestore (Primary Database)
     */
    private suspend fun syncUserWithBackend(user: User) {
        try {
            // Firestore is now the primary database
            // User data is already saved in Firestore in createOrUpdateUser method
            println("User data synced with Firestore successfully")
            
            // Optional: Keep backend sync for backup (if needed)
            if (apiService != null) {
                try {
                    val createRequest = mapOf(
                        "fullName" to user.fullName,
                        "email" to user.email
                    )
                    
                    val createResponse = apiService.createUser(createRequest)
                    if (createResponse.isSuccessful) {
                        println("User also synced with backend for backup")
                    }
                } catch (e: Exception) {
                    println("Backend backup sync failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Firestore sync error: ${e.message}")
        }
    }
    
    /**
     * Check if user exists in backend by email
     */
    private suspend fun checkUserExistsInBackend(email: String): User? {
        return try {
            // Try to get profile - if successful, user exists
            val response = apiService?.getProfile()
            if (response?.isSuccessful == true) {
                val responseBody = response.body()
                if (responseBody?.get("success") == true) {
                    val userData = responseBody["user"] as? Map<String, Any>
                    if (userData != null) {
                        // Convert to User object
                        User(
                            id = userData["id"] as? String ?: "",
                            email = userData["email"] as? String ?: "",
                            fullName = userData["fullName"] as? String ?: "",
                            role = com.example.partimes.models.UserRole.valueOf(
                                (userData["role"] as? String ?: "WORKER").uppercase()
                            ),
                            isVerified = userData["isVerified"] as? Boolean ?: false,
                            isActive = userData["isActive"] as? Boolean ?: true,
                            createdAt = (userData["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            lastLoginAt = (userData["lastLoginAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            profileImageUrl = userData["profileImageUrl"] as? String,
                            isProfileComplete = userData["isProfileComplete"] as? Boolean ?: false
                        )
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            println("Error checking user existence: ${e.message}")
            null
        }
    }
    
    /**
     * Switch user role
     */
    fun switchRole(userId: String, newRole: UserRole): Flow<Result<User>> = flow {
        try {
            val userRef = firestore.collection("users").document(userId)
            val document = userRef.get().await()
            
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                val updatedUser = user?.copy(
                    role = newRole,
                    lastLoginAt = System.currentTimeMillis()
                )
                
                if (updatedUser != null) {
                    userRef.set(updatedUser).await()
                    emit(Result.success(updatedUser))
                } else {
                    emit(Result.failure(Exception("Failed to parse user data")))
                }
            } else {
                emit(Result.failure(Exception("User not found")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get current user from Firestore
     */
    fun getCurrentUser(): Flow<Result<User?>> = flow {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val document = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    emit(Result.success(user))
                } else {
                    emit(Result.success(null))
                }
            } else {
                emit(Result.success(null))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Sign out user
     */
    fun signOut(): Flow<Result<Unit>> = flow {
        try {
            auth.signOut()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Check if user is signed in
     */
    fun isSignedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Get current Firebase user
     */
    fun getCurrentFirebaseUser(): com.google.firebase.auth.FirebaseUser? {
        return auth.currentUser
    }
}
