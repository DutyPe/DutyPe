package com.example.dutype.managers

import android.content.Context
import androidx.navigation.NavController
import com.example.dutype.auth.AuthManager
import com.example.dutype.cache.RoleCacheManager
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.RoleManagementViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RoleSwitchManager - Enterprise-grade role switching coordinator
 * 
 * Inspired by Uber, Airbnb, and Fiverr dual-role architectures
 * 
 * Responsibilities:
 * 1. Show loading state during switch
 * 2. Update activeRole in Firestore
 * 3. Clear role-specific ViewModels
 * 4. Clear role-specific caches
 * 5. Clear navigation stack
 * 6. Navigate to new role's home
 * 7. Initialize new role's ViewModels
 * 8. Fetch fresh data
 * 9. Show success/error feedback
 */
@Singleton
class RoleSwitchManager @Inject constructor(
    private val roleCacheManager: RoleCacheManager,
    private val authManager: AuthManager
) {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    /**
     * Switch to a new role with complete state cleanup
     * 
     * @param context Android context
     * @param navController Navigation controller for routing
     * @param roleViewModel ViewModel for role management
     * @param oldRole Current role (to clean up)
     * @param newRole Target role (to initialize)
     * @param onSuccess Callback on successful switch
     * @param onError Callback on error
     */
    suspend fun switchRole(
        context: Context,
        navController: NavController,
        roleViewModel: RoleManagementViewModel,
        oldRole: UserRole,
        newRole: UserRole,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            _isLoading.value = true
            _error.value = null
            
            Timber.d("🔄 ROLE_SWITCH: Starting switch from $oldRole to $newRole")
            
            // Step 1: Update activeRole in Firestore
            Timber.d("🔄 ROLE_SWITCH: Step 1 - Updating Firestore")
            roleViewModel.switchActiveRole(newRole)
            
            // Step 1.5: Force refresh Firebase Auth token to ensure fresh user data
            Timber.d("🔄 ROLE_SWITCH: Step 1.5 - Refreshing Firebase Auth token")
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                currentUser?.getIdToken(true)?.await() // Force refresh token
                Timber.d("✅ ROLE_SWITCH: Firebase Auth token refreshed")
            } catch (e: Exception) {
                Timber.w(e, "⚠️ ROLE_SWITCH: Failed to refresh token, but continuing...")
            }
            
            // Step 1.6: Refresh cached user data in AuthManager
            Timber.d("🔄 ROLE_SWITCH: Step 1.6 - Refreshing AuthManager cache")
            val refreshedUser = authManager.refreshUserFromFirestore()
            if (refreshedUser != null) {
                Timber.d("✅ ROLE_SWITCH: User data refreshed - activeRole=${refreshedUser.activeRole}")
            } else {
                Timber.w("⚠️ ROLE_SWITCH: Failed to refresh user data, but continuing...")
            }
            
            // Step 2: ViewModels are scoped to their composables and clear automatically
            // when navigation removes them from composition (handled by Step 4 below).
            
            // Step 3: Clear old role's caches
            Timber.d("🔄 ROLE_SWITCH: Step 3 - Clearing caches")
            roleCacheManager.clearRoleSpecificCache(oldRole)
            
            // Step 4: Clear navigation stack and navigate to new role's home
            Timber.d("🔄 ROLE_SWITCH: Step 4 - Navigating to new role home")
            navigateToRoleHome(navController, newRole)
            
            // Step 5: Initialize new role's data (ViewModels will auto-initialize)
            Timber.d("🔄 ROLE_SWITCH: Step 5 - New role ViewModels will initialize on screen composition")
            // ViewModels will be created automatically when screens are composed
            
            Timber.d("✅ ROLE_SWITCH: Switch completed successfully")
            onSuccess()
            
        } catch (e: Exception) {
            Timber.e(e, "❌ ROLE_SWITCH: Error during role switch")
            _error.value = e.message ?: "Failed to switch role"
            onError(e.message ?: "Failed to switch role")
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Navigate to the appropriate home screen for the role
     * Clears the entire back stack to prevent cross-role navigation
     */
    private fun navigateToRoleHome(navController: NavController, role: UserRole) {
        val route = when (role) {
            UserRole.WORKER -> Routes.WORKER_HOME
            UserRole.EMPLOYER -> Routes.EMPLOYER_HOME
            else -> Routes.WORKER_HOME
        }
        
        // Clear entire back stack and navigate to new home
        // P1-7: refresh cached start destination for the new role so the next
        // cold start lands on the correct home screen instantly.
        runCatching {
            com.example.dutype.navigation.StartDestinationCache.save(navController.context, route)
        }
        navController.navigate(route) {
            // Pop everything up to and including the dynamic start destination (named-root pop)
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            // Avoid multiple copies of the same destination
            launchSingleTop = true
        }
        
        Timber.d("🔄 ROLE_SWITCH: Navigated to $route with cleared back stack")
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
}
