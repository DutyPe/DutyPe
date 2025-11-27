package com.example.dutype.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.example.dutype.services.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class GoogleSignInManager(
    private val context: Context
) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firestoreService: FirestoreService = FirestoreService()
    private val credentialManager: CredentialManager = CredentialManager.create(context)
    
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
            // Validate userId is not empty
            if (userId.isBlank()) {
                throw Exception("Firebase UID is empty or invalid")
            }
            
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
                    id = userId, // Must be set - this is the Firebase UID
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
            
            // Validate user object before saving
            if (user.id.isBlank()) {
                throw Exception("User ID is empty - cannot save to Firestore. Firebase UID: $userId")
            }
            
            // Save to Firestore using FirestoreService
            println("🔥 Attempting to save user to Firestore: ${user.email}")
            println("   User ID: ${user.id}")
            println("   Email: ${user.email}")
            println("   Role: ${user.role}")
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
        } catch (e: Exception) {
            println("Firestore sync error: ${e.message}")
        }
    }
    
    /**
     * Check if user exists in backend by email
     */
    private suspend fun checkUserExistsInBackend(email: String): User? {
        return try {
            // Firebase/Firestore is now the only backend
            null
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
     * Sign out user - clears both Firebase auth and Credential Manager state
     */
    fun signOut(): Flow<Result<Unit>> = flow {
        try {
            // Sign out from Firebase
            auth.signOut()
            
            // Clear credential state from Credential Manager (important for proper re-login)
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                println("Failed to clear credential state: ${e.message}")
                // Continue even if clearing credential state fails
            }
            
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
