package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.User
import com.example.dutype.network.ApiClient
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
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val result = ApiClient.getApiService().getProfile()
                if (result.isSuccessful && result.body()?.get("success") == true) {
                    val userMap = result.body()?.get("user") as? Map<String, Any>
                    if (userMap != null) {
                        val user = mapToUser(userMap)
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
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasError = true,
                        error = result.body()?.get("message") as? String ?: "Failed to load profile"
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
                val result = ApiClient.getApiService().updateProfile(user)
                if (result.isSuccessful && result.body()?.get("success") == true) {
                    val userMap = result.body()?.get("user") as? Map<String, Any>
                    if (userMap != null) {
                        val updatedUser = mapToUser(userMap)
                        _uiState.value = _uiState.value.copy(
                            user = updatedUser,
                            isUpdating = false
                        )
                        loadProfileCompletion()
                        callback(true, null)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            hasError = true,
                            error = "Updated user data not found"
                        )
                        callback(false, "Updated user data not found")
                    }
                } else {
                    val errorMessage = result.body()?.get("message") as? String ?: "Failed to update profile"
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        hasError = true,
                        error = errorMessage
                    )
                    callback(false, errorMessage)
                }
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
                // Create multipart file from URI
                val file = createMultipartFileFromUri(imageUri)
                val result = ApiClient.getApiService().uploadProfileImage(file)
                
                if (result.isSuccessful && result.body()?.success == true) {
                    val imageUrl = result.body()?.data?.get("imageUrl") as? String
                    val userMap = result.body()?.data?.get("user") as? Map<String, Any>
                    
                    if (userMap != null) {
                        val updatedUser = mapToUser(userMap)
                        _uiState.value = _uiState.value.copy(
                            user = updatedUser,
                            isUploadingImage = false
                        )
                        loadProfileCompletion()
                        callback(true, imageUrl)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isUploadingImage = false,
                            hasError = true,
                            error = "User data not found after upload"
                        )
                        callback(false, "User data not found after upload")
                    }
                } else {
                    val errorMessage = result.body()?.message ?: "Failed to upload image"
                    _uiState.value = _uiState.value.copy(
                        isUploadingImage = false,
                        hasError = true,
                        error = errorMessage
                    )
                    callback(false, errorMessage)
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
                val result = ApiClient.getApiService().deleteProfileImage()
                if (result.isSuccessful && result.body()?.success == true) {
                    // Reload profile to get updated data
                    loadProfile()
                    callback(true, null)
                } else {
                    val errorMessage = result.body()?.message ?: "Failed to delete image"
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        hasError = true,
                        error = errorMessage
                    )
                    callback(false, errorMessage)
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
                val result = ApiClient.getApiService().getProfileCompletion()
                if (result.isSuccessful && result.body()?.success == true) {
                    val completionPercentage = result.body()?.data?.get("completionPercentage") as? Int ?: 0
                    val missingFields = (result.body()?.data?.get("missingFields") as? List<String>) ?: emptyList()
                    
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
    
    private suspend fun getAuthToken(): String {
        return authManager.getToken() ?: ""
    }
    
    private fun createMultipartFileFromUri(uri: String): okhttp3.MultipartBody.Part {
        // This would need to be implemented based on your file handling needs
        // For now, return a placeholder
        return okhttp3.MultipartBody.Part.createFormData("file", "profile.jpg", okhttp3.RequestBody.create(null, ""))
    }
    
    private fun mapToUser(map: Map<String, Any>): User {
        return User(
            id = map["id"] as? String ?: "",
            email = map["email"] as? String ?: "",
            fullName = map["fullName"] as? String ?: "",
            phoneNumber = map["phoneNumber"] as? String,
            role = com.example.dutype.models.UserRole.valueOf((map["role"] as? String ?: "WORKER").uppercase()),
            isVerified = map["isVerified"] as? Boolean ?: false,
            isActive = map["enabled"] as? Boolean ?: true,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            lastLoginAt = (map["lastLoginAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            profileImageUrl = map["profileImageUrl"] as? String,
            bio = map["bio"] as? String,
            location = map["currentAddress"] as? String,
            dateOfBirth = map["dateOfBirth"] as? String,
            gender = map["gender"] as? String,
            skills = (map["skills"] as? List<String>),
            experience = map["experienceLevel"] as? String,
            education = map["educationLevel"] as? String,
            resumeUrl = map["resumeUrl"] as? String,
            coverLetter = map["coverLetterUrl"] as? String,
            companyName = map["companyName"] as? String,
            companyDescription = map["companyDescription"] as? String,
            companyWebsite = map["companyWebsite"] as? String,
            companyLogoUrl = map["companyLogoUrl"] as? String,
            industry = map["industry"] as? String,
            companySize = map["companySize"] as? String,
            emailNotifications = true,
            pushNotifications = true,
            smsNotifications = false
        )
    }
}
