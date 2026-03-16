package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * RoleManagementViewModel - Manages dual-role functionality
 * 
 * Handles:
 * - Toggling roles on/off
 * - Switching active role
 * - Real-time user data updates
 * 
 * Architecture inspired by Airbnb/Uber dual-role systems
 */
@HiltViewModel
class RoleManagementViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val profileSetupStateManager: com.example.dutype.state.ProfileSetupStateManager,
    private val authManager: com.example.dutype.auth.AuthManager
) : ViewModel() {
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadCurrentUser()
    }
    
    /**
     * Load current user data from Firestore
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                
                if (userDoc.exists()) {
                    val userData = userDoc.data ?: return@launch
                    
                    // Parse roles array
                    @Suppress("UNCHECKED_CAST")
                    val rolesArray = userData["roles"] as? List<String> ?: emptyList()
                    
                    // Parse active role
                    val activeRoleStr = userData["activeRole"] as? String
                    val activeRole = try {
                        if (activeRoleStr != null) {
                            UserRole.valueOf(activeRoleStr.uppercase())
                        } else {
                            // Fallback to old role field
                            val oldRole = userData["role"] as? String
                            if (oldRole != null) {
                                UserRole.valueOf(oldRole.uppercase())
                            } else {
                                UserRole.WORKER
                            }
                        }
                    } catch (e: Exception) {
                        UserRole.WORKER
                    }
                    
                    // Create User object
                    val user = User(
                        id = userId,
                        email = userData["email"] as? String ?: "",
                        fullName = userData["fullName"] as? String ?: "",
                        phone = userData["phone"] as? String ?: "",
                        roles = rolesArray,
                        activeRole = activeRole,
                        profileCompleted = userData["profileCompleted"] as? Boolean ?: false,
                        profileImageUrl = userData["profileImageUrl"] as? String,
                        bio = userData["bio"] as? String,
                        address = userData["address"] as? String ?: "",
                        latitude = (userData["latitude"] as? Number)?.toDouble() ?: 0.0,
                        longitude = (userData["longitude"] as? Number)?.toDouble() ?: 0.0
                    )
                    
                    _currentUser.value = user
                    Timber.d("✅ ROLE: Loaded user - roles=${user.roles}, activeRole=${user.activeRole}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected when ViewModel is cleared during role switch - don't log as error
                Timber.d("🔄 ROLE: User load cancelled (expected during role switch)")
                throw e // Re-throw to properly cancel the coroutine
            } catch (e: Exception) {
                Timber.e(e, "❌ ROLE: Error loading current user")
                _error.value = "Failed to load user data: ${e.message}"
            }
        }
    }
    
    /**
     * Toggle a role on/off for the current user
     * Cannot disable the last remaining role
     * DUAL-ROLE: Syncs with phone_roles collection
     */
    fun toggleRole(role: UserRole, enabled: Boolean) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
                val user = _currentUser.value ?: throw IllegalStateException("No user data")
                
                val currentRoles = user.roles.toMutableList()
                
                if (enabled) {
                    // Enable role
                    if (!currentRoles.contains(role.name)) {
                        currentRoles.add(role.name)
                        Timber.d("✅ ROLE: Enabling role $role")
                        
                        // Update user document
                        firestore.collection("users").document(userId)
                            .update(
                                mapOf(
                                    "roles" to currentRoles,
                                    "activeRole" to role.name // Set new role as active
                                )
                            )
                            .await()
                        
                        // Sync with phone_roles collection
                        val phone = user.phone
                        if (phone.isNotEmpty()) {
                            syncPhoneRoles(phone, currentRoles, role.name)
                        }
                        
                        Timber.d("✅ ROLE: Role enabled and synced successfully")
                    }
                } else {
                    // Disable role
                    if (currentRoles.size > 1) {
                        currentRoles.remove(role.name)
                        Timber.d("✅ ROLE: Disabling role $role")
                        
                        // If disabling active role, switch to remaining role
                        val newActiveRole = if (user.activeRole == role) {
                            val newActiveRoleString = currentRoles.first()
                            try {
                                UserRole.valueOf(newActiveRoleString)
                            } catch (e: Exception) {
                                UserRole.WORKER
                            }
                        } else {
                            user.activeRole
                        }
                        
                        firestore.collection("users").document(userId)
                            .update(
                                mapOf(
                                    "roles" to currentRoles,
                                    "activeRole" to newActiveRole.name
                                )
                            )
                            .await()
                        
                        // Sync with phone_roles collection
                        val phone = user.phone
                        if (phone.isNotEmpty()) {
                            syncPhoneRoles(phone, currentRoles, newActiveRole.name)
                        }
                        
                        Timber.d("✅ ROLE: Switched active role to $newActiveRole")
                    } else {
                        throw IllegalStateException("Cannot disable last role")
                    }
                }
                
                loadCurrentUser()
            } catch (e: Exception) {
                Timber.e(e, "❌ ROLE: Error toggling role")
                _error.value = e.message ?: "Failed to toggle role"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Sync phone_roles collection with user's roles
     * Ensures consistency between user document and phone_roles
     */
    private suspend fun syncPhoneRoles(phone: String, roles: List<String>, activeRole: String) {
        try {
            val cleanPhone = com.example.dutype.utils.PhoneNumberUtils.normalizePhone(phone)
            
            firestore.collection("phone_roles")
                .document(cleanPhone)
                .set(
                    mapOf(
                        "roles" to roles,
                        "activeRole" to activeRole,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
            
            Timber.d("✅ ROLE: Synced phone_roles for $cleanPhone")
        } catch (e: Exception) {
            Timber.e(e, "❌ ROLE: Error syncing phone_roles")
            // Don't throw - this is a non-critical sync operation
        }
    }
    
    /**
     * Switch the active role
     * Returns the new active role for navigation
     * DUAL-ROLE: Syncs with phone_roles collection
     */
    suspend fun switchActiveRole(newRole: UserRole): UserRole {
        try {
            _isLoading.value = true
            _error.value = null
            
            val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
            val user = _currentUser.value ?: throw IllegalStateException("No user data")
            
            // Verify user has this role enabled
            if (!user.hasRole(newRole)) {
                throw IllegalStateException("User does not have $newRole role enabled")
            }
            
            // Update Firestore
            firestore.collection("users").document(userId)
                .update("activeRole", newRole.name)
                .await()
            
            Timber.d("✅ ROLE: Active role switched to $newRole in Firestore")
            
            // Sync with phone_roles collection
            val phone = user.phone
            if (phone.isNotEmpty()) {
                syncPhoneRoles(phone, user.roles, newRole.name)
            }
            
            // CRITICAL FIX: Update local DataStore so app reopens to correct home screen
            // This fixes the bug where app reopens to wrong home after role switch
            profileSetupStateManager.saveUserRole(newRole)
            Timber.d("✅ ROLE: Active role saved to DataStore for app restart persistence")
            
            // Update cached user in AuthManager
            val updatedUser = user.copy(activeRole = newRole)
            authManager.saveUser(updatedUser)
            Timber.d("✅ ROLE: Active role updated in AuthManager cache")
            
            // Reload user data from Firestore to ensure UI reflects the change
            loadCurrentUser()
            
            return newRole
        } catch (e: Exception) {
            Timber.e(e, "❌ ROLE: Error switching active role")
            _error.value = e.message ?: "Failed to switch role"
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Get the current active role
     */
    fun getActiveRole(): UserRole {
        return _currentUser.value?.activeRole ?: UserRole.WORKER
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Refresh user data
     */
    fun refresh() {
        loadCurrentUser()
    }
    
    /**
     * Switch role with complete cleanup
     * This is the enterprise-grade method that should be used by RoleSwitchManager
     */
    suspend fun switchRoleWithCleanup(newRole: UserRole): UserRole {
        Timber.d("🔄 ROLE_MGMT: Starting role switch with cleanup to $newRole")
        
        val oldRole = getActiveRole()
        
        // Switch the active role in Firestore
        val result = switchActiveRole(newRole)
        
        Timber.d("✅ ROLE_MGMT: Role switch completed from $oldRole to $newRole")
        
        return result
    }
    
    /**
     * Clear role-specific data (called by RoleSwitchManager)
     * This method is a hook for future enhancements
     */
    fun clearRoleSpecificData(oldRole: UserRole) {
        Timber.d("🧹 ROLE_MGMT: Clearing data for role $oldRole")
        // Additional cleanup logic can be added here
        // For example: clearing in-memory caches, canceling pending operations, etc.
    }
    
    /**
     * Initialize role data (called by RoleSwitchManager)
     * This method is a hook for future enhancements
     */
    fun initializeRoleData(newRole: UserRole) {
        Timber.d("🚀 ROLE_MGMT: Initializing data for role $newRole")
        // Additional initialization logic can be added here
        // For example: pre-fetching role-specific data, setting up listeners, etc.
    }
}
