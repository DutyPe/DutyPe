package com.example.dutype.profile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.profile.models.Verification
import com.example.dutype.profile.models.VerificationType
import com.example.dutype.profile.services.VerificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val verificationService: VerificationService
) : ViewModel() {
    
    private val _verifications = MutableStateFlow<List<Verification>>(emptyList())
    val verifications: StateFlow<List<Verification>> = _verifications.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadVerifications()
    }
    
    fun loadVerifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val userVerifications = verificationService.getUserVerifications("current_user_id").first()
                _verifications.value = userVerifications
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load verifications"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun startVerification(type: VerificationType) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                when (type) {
                    com.example.dutype.profile.models.VerificationType.EMAIL -> {
                        verificationService.startEmailVerification("current_user_id", "user@example.com")
                    }
                    com.example.dutype.profile.models.VerificationType.PHONE -> {
                        verificationService.startPhoneVerification("current_user_id", "+1234567890")
                    }
                    else -> {
                        // For other types, just create a pending verification
                        val verification = com.example.dutype.profile.models.Verification(
                            id = java.util.UUID.randomUUID().toString(),
                            type = type,
                            status = com.example.dutype.profile.models.VerificationStatus.PENDING
                        )
                        // Add to user's verifications
                    }
                }
                loadVerifications() // Refresh the list
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start verification"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun retryVerification(verificationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // TODO: Implement retry verification method in service
                // verificationService.retryVerificationProcess("current_user_id", verificationId)
                loadVerifications() // Refresh the list
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to retry verification"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
