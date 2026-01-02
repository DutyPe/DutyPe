package com.example.dutype.repositories

import com.example.dutype.auth.AuthManager
import com.example.dutype.models.User

/**
 * AuthRepository - Simplified authentication repository
 * 
 * Google Sign-In has been removed. Authentication is now OTP-only via Firebase Phone Auth.
 * 
 * REFACTORED: Removed getToken() - tokens are managed by Firebase Auth directly
 */
class AuthRepository(
    private val authManager: AuthManager
) {
    
    fun isLoggedIn(): Boolean {
        return authManager.isLoggedIn()
    }
    
    fun getCurrentUser(): User? {
        return authManager.getCurrentUser()
    }
    
    fun logout() {
        authManager.logout()
    }
}
