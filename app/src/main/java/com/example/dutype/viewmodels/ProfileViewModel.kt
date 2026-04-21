package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.User
import com.example.dutype.services.FirestoreService
import com.example.dutype.services.ProfileCompletionService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isUploadingImage: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val profileCompletionPercentage: Int = 0,
    val missingFields: List<String> = emptyList()
)

/**
 * ProfileViewModel - Manages user profile data and operations
 * 
 * REFACTORED: Uses FirebaseAuth.currentUser?.uid directly instead of AuthManager.getUserId()
 * AuthManager is now focused on local state management only
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val firestoreService: FirestoreService,
    private val profileCompletionService: ProfileCompletionService,
    private val firebaseAuth: FirebaseAuth,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    /**
     * Get current user ID from Firebase Auth (canonical source)
     */
    private fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
    
    fun loadProfile() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            com.example.dutype.performance.MainThreadChecker.assertMainThread("ProfileViewModel.loadProfile")
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val userId = getCurrentUserId()
                if (userId != null) {
                    val result = firestoreService.getUserById(userId)
                    result.fold(
                        onSuccess = { user ->
                            val duration = System.currentTimeMillis() - startTime
                            performanceTracker.trackApiCall("load_profile", duration, success = true)
                            
                            if (user != null) {
                                _uiState.value = _uiState.value.copy(
                                    user = user,
                                    isLoading = false
                                )
                                loadProfileCompletion()
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    hasError = true,
                                    error = "User data not found"
                                )
                            }
                        },
                        onFailure = { e ->
                            val duration = System.currentTimeMillis() - startTime
                            performanceTracker.trackApiCall("load_profile", duration, success = false)
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = e.message ?: "Failed to load profile"
                            )
                        }
                    )
                } else {
                     _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasError = true,
                        error = "User not logged in"
                    )
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                performanceTracker.trackApiCall("load_profile", duration, success = false)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }
    
    fun updateProfile(user: User, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, error = null, hasError = false)
            
            try {
                // Create updates map
                val updates = mutableMapOf<String, Any>()
                updates["fullName"] = user.fullName
                user.phone.let { updates["phone"] = it }
                
                val result = firestoreService.updateUserProfile(user.id, updates)
                
                result.fold(
                    onSuccess = {
                        // Reload profile to get updated data
                        loadProfile()
                        _uiState.value = _uiState.value.copy(isUpdating = false)
                        callback(true, null)
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            hasError = true,
                            error = e.message ?: "Failed to update profile"
                        )
                        callback(false, e.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    hasError = true,
                    error = e.message ?: "Failed to update profile"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun uploadProfileImage(imageUri: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingImage = true, error = null, hasError = false)
            
            try {
                val userId = getCurrentUserId()
                val userRole = _uiState.value.user?.role?.name ?: "WORKER"
                
                if (userId != null) {
                    val uri = android.net.Uri.parse(imageUri)
                    val result = profileCompletionService.uploadProfileImage(uri, userId, userRole)
                    
                    result.fold(
                        onSuccess = { imageUrl ->
                            // Update local user state with new image URL
                            val updatedUser = _uiState.value.user?.copy(profileImageUrl = imageUrl)
                            _uiState.value = _uiState.value.copy(
                                user = updatedUser,
                                isUploadingImage = false
                            )
                            loadProfileCompletion()
                            callback(true, imageUrl)
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isUploadingImage = false,
                                hasError = true,
                                error = e.message ?: "Failed to upload image"
                            )
                            callback(false, e.message)
                        }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUploadingImage = false,
                        hasError = true,
                        error = "User not logged in"
                    )
                    callback(false, "User not logged in")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingImage = false,
                    hasError = true,
                    error = e.message ?: "Failed to upload image"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun deleteProfileImage(callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, error = null, hasError = false)
            
            try {
                val userId = getCurrentUserId()
                if (userId != null) {
                    // Update user profile to remove image URL
                    val updates = mapOf<String, Any>("profileImageUrl" to "")
                    val result = firestoreService.updateUserProfile(userId, updates)
                    
                    result.fold(
                        onSuccess = {
                            loadProfile()
                            callback(true, null)
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isUpdating = false,
                                hasError = true,
                                error = e.message ?: "Failed to delete image"
                            )
                            callback(false, e.message)
                        }
                    )
                } else {
                    callback(false, "User not logged in")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    hasError = true,
                    error = e.message ?: "Failed to delete image"
                )
                callback(false, e.message)
            }
        }
    }
    
    private fun loadProfileCompletion() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val userRole = _uiState.value.user?.role?.name ?: "WORKER"
                
                if (userId != null) {
                    val completionPercentage = profileCompletionService.getProfileCompletionPercentage(userId, userRole).getOrDefault(0)
                    val missingFields = profileCompletionService.getMissingProfileFields(userId, userRole).getOrDefault(emptyList())
                    
                    _uiState.value = _uiState.value.copy(
                        profileCompletionPercentage = completionPercentage,
                        missingFields = missingFields
                    )
                }
            } catch (e: Exception) {
                // Ignore completion loading errors
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
}
