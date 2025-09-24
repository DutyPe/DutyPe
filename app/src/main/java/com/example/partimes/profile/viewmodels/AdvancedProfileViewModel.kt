package com.example.partimes.profile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.profile.models.AdvancedProfile
import com.example.partimes.profile.repository.AdvancedProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvancedProfileViewModel @Inject constructor(
    private val repository: AdvancedProfileRepository
) : ViewModel() {
    
    private val _profile = MutableStateFlow(AdvancedProfile(
        userId = "",
        personalInfo = com.example.partimes.profile.models.PersonalInfo()
    ))
    val profile: StateFlow<AdvancedProfile> = _profile.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val userProfile = repository.getUserProfile("current_user_id").first()
                _profile.value = userProfile ?: AdvancedProfile(
                    userId = "current_user_id",
                    personalInfo = com.example.partimes.profile.models.PersonalInfo()
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load profile"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateProfile(updatedProfile: AdvancedProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                repository.updateProfile(updatedProfile)
                _profile.value = updatedProfile
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update profile"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
