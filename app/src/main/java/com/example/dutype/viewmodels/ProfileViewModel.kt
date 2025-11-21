package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.User
import com.example.dutype.services.FirestoreService
import com.example.dutype.services.ProfileCompletionService
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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val firestoreService: FirestoreService,
    private val profileCompletionService: ProfileCompletionService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val userId = authManager.getUserId()
                if (userId != null) {
                    val result = firestoreService.getUserById(userId)
                    result.fold(
                        onSuccess = { user ->
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
                user.phoneNumber?.let { updates["phoneNumber"] = it }
                user.bio?.let { updates["bio"] = it }
                user.location?.let { updates["currentAddress"] = it } // Map location to currentAddress
                user.dateOfBirth?.let { updates["dateOfBirth"] = it }
                user.gender?.let { updates["gender"] = it }
                user.skills?.let { updates["skills"] = it }
                user.experience?.let { updates["experienceLevel"] = it }
                user.education?.let { updates["educationLevel"] = it }
                user.resumeUrl?.let { updates["resumeUrl"] = it }
                user.coverLetter?.let { updates["coverLetterUrl"] = it }
                user.companyName?.let { updates["companyName"] = it }
                user.companyDescription?.let { updates["companyDescription"] = it }
                user.companyWebsite?.let { updates["companyWebsite"] = it }
                user.companyLogoUrl?.let { updates["companyLogoUrl"] = it }
                user.industry?.let { updates["industry"] = it }
                user.companySize?.let { updates["companySize"] = it }
                
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
                val userId = authManager.getUserId()
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
                val userId = authManager.getUserId()
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
                val userId = authManager.getUserId()
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
