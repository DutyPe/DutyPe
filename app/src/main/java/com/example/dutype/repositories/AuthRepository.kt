package com.example.dutype.repositories

import com.example.dutype.auth.AuthManager
import com.example.dutype.auth.GoogleSignInManager
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val authManager: AuthManager,
    private val googleSignInManager: GoogleSignInManager
) {
    
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
        selectedRole: UserRole
    ): Flow<Result<User>> {
        return googleSignInManager.signInWithGoogle(idToken, selectedRole)
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
