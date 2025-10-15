package com.example.dutype.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.dutype.models.User
import com.example.dutype.state.ProfileSetupStateManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "current_user"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
    
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    fun getAuthHeader(): String? {
        val token = getToken()
        return if (token != null) "Bearer $token" else null
    }
    
    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER, userJson).apply()
    }
    
    fun getCurrentUser(): User? {
        val userJson = prefs.getString(KEY_USER, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getToken() != null
    }
    
    fun saveRefreshToken(refreshToken: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply()
    }
    
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }
    
    fun logout() {
        prefs.edit().clear().apply()
        
        // Reset profile setup state
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profileSetupStateManager = ProfileSetupStateManager(context)
                profileSetupStateManager.resetProfileSetupState()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun updateUser(user: User) {
        saveUser(user)
    }
    
    fun getUserId(): String? {
        return getCurrentUser()?.id
    }
    
    fun getUserEmail(): String? {
        return getCurrentUser()?.email
    }
    
    fun getUserRole(): String? {
        return getCurrentUser()?.role?.name
    }
}
