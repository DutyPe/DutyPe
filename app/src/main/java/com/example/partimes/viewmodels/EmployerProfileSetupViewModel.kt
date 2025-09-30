package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.employer.models.*
import com.example.partimes.viewmodels.ApplicationViewModel
import com.example.partimes.models.User
import com.example.partimes.models.UserRole
import com.example.partimes.network.ApiClient
import com.example.partimes.data.ApplicationFormDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployerProfileSetupViewModel @Inject constructor(
    private val dataStore: ApplicationFormDataStore,
    private val firestoreService: com.example.partimes.services.FirestoreService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EmployerProfileSetupUiState())
    val uiState: StateFlow<EmployerProfileSetupUiState> = _uiState.asStateFlow()
    
    val isFormValid = combine(
        _uiState
    ) { state ->
        validateForm(state[0])
    }
    
    fun updateCompanyInfo(companyInfo: CompanyInfo) {
        _uiState.value = _uiState.value.copy(companyInfo = companyInfo)
    }
    
    fun updateBusinessDetails(businessDetails: BusinessDetails) {
        _uiState.value = _uiState.value.copy(businessDetails = businessDetails)
    }
    
    fun updateVerificationDetails(verificationDetails: VerificationDetails) {
        _uiState.value = _uiState.value.copy(verificationDetails = verificationDetails)
    }
    
    fun updateCurrentStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }
    
    fun submitProfile(applicationViewModel: ApplicationViewModel) {
        viewModelScope.launch {
            println("🔥 Starting employer profile submission to Firestore...")
            println("📊 CompanyInfo: ${_uiState.value.companyInfo}")
            println("📊 BusinessDetails: ${_uiState.value.businessDetails}")
            println("📊 VerificationDetails: ${_uiState.value.verificationDetails}")
            
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            
            try {
                // Get current user ID from Firebase Auth
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    throw Exception("User not authenticated")
                }
                
                val userId = currentUser.uid
                println("👤 User ID: $userId")
                
                // Prepare employer profile data for Firestore
                val employerProfileData = mapOf(
                    // Company Information
                    "companyName" to _uiState.value.companyInfo.companyName,
                    "industry" to _uiState.value.companyInfo.industry,
                    "companySize" to _uiState.value.companyInfo.companySize,
                    "website" to _uiState.value.companyInfo.website,
                    "description" to _uiState.value.companyInfo.description,
                    
                    // Business Details
                    "contactPersonName" to _uiState.value.businessDetails.contactPersonName,
                    "contactEmail" to _uiState.value.businessDetails.contactEmail,
                    "contactPhone" to _uiState.value.businessDetails.contactPhone,
                    "businessAddress" to _uiState.value.businessDetails.businessAddress,
                    "yearsInBusiness" to _uiState.value.businessDetails.yearsInBusiness,
                    
                    // Verification Details
                    "businessRegistrationNumber" to _uiState.value.verificationDetails.businessRegistrationNumber,
                    "gstNumber" to _uiState.value.verificationDetails.gstNumber,
                    "panNumber" to _uiState.value.verificationDetails.panNumber,
                    "businessType" to _uiState.value.verificationDetails.businessType,
                    "additionalNotes" to _uiState.value.verificationDetails.additionalNotes,
                    
                    // Metadata
                    "profileCompleted" to true,
                    "completedAt" to System.currentTimeMillis()
                )
                
                // Store in Firestore
                val result = firestoreService.createOrUpdateEmployerProfile(userId, employerProfileData)
                
                if (result.isSuccess) {
                    // Update main user record to mark profile as complete
                    val userUpdateData = mapOf(
                        "isProfileComplete" to true,
                        "companyName" to _uiState.value.companyInfo.companyName,
                        "companyDescription" to _uiState.value.companyInfo.description,
                        "companyWebsite" to _uiState.value.companyInfo.website,
                        "industry" to _uiState.value.companyInfo.industry,
                        "companySize" to _uiState.value.companyInfo.companySize,
                        "phoneNumber" to _uiState.value.businessDetails.contactPhone,
                        "location" to _uiState.value.businessDetails.businessAddress
                    )
                    
                    // Update user record
                    val userResult = firestoreService.createOrUpdateUser(
                        User(
                            id = userId,
                            email = currentUser.email ?: "",
                            fullName = currentUser.displayName ?: _uiState.value.businessDetails.contactPersonName,
                            phoneNumber = _uiState.value.businessDetails.contactPhone,
                            location = _uiState.value.businessDetails.businessAddress,
                            bio = _uiState.value.companyInfo.description,
                            companyName = _uiState.value.companyInfo.companyName,
                            companyDescription = _uiState.value.companyInfo.description,
                            companyWebsite = _uiState.value.companyInfo.website,
                            industry = _uiState.value.companyInfo.industry,
                            companySize = _uiState.value.companyInfo.companySize,
                            role = UserRole.EMPLOYER,
                            isProfileComplete = true,
                            isVerified = true,
                            isActive = true,
                            createdAt = System.currentTimeMillis(),
                            lastLoginAt = System.currentTimeMillis()
                        )
                    )
                    
                    if (userResult.isSuccess) {
                        println("✅ Employer profile successfully saved to Firestore!")
                        dataStore.setFormCompleted(true)
                        
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            isSubmitted = true
                        )
                    } else {
                        throw Exception("Failed to update user record: ${userResult.exceptionOrNull()?.message}")
                    }
                } else {
                    throw Exception("Failed to save employer profile: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                println("❌ Error during employer profile submission: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Failed to save profile: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun validateForm(state: EmployerProfileSetupUiState): Boolean {
        // Validate based on current step
        return when (state.currentStep) {
            0 -> validateCompanyInfo(state.companyInfo)
            1 -> validateBusinessDetails(state.businessDetails)
            2 -> validateVerificationDetails(state.verificationDetails)
            else -> false
        }
    }
    
    private fun validateCompanyInfo(companyInfo: CompanyInfo): Boolean {
        return companyInfo.companyName.isNotBlank() &&
                companyInfo.industry.isNotBlank() &&
                companyInfo.companySize.isNotBlank() &&
                companyInfo.description.isNotBlank()
    }
    
    private fun validateBusinessDetails(businessDetails: BusinessDetails): Boolean {
        return businessDetails.contactPersonName.isNotBlank() &&
                businessDetails.contactEmail.isNotBlank() &&
                businessDetails.contactPhone.isNotBlank() &&
                businessDetails.businessAddress.isNotBlank() &&
                businessDetails.yearsInBusiness.isNotBlank()
    }
    
    private fun validateVerificationDetails(verificationDetails: VerificationDetails): Boolean {
        return verificationDetails.businessRegistrationNumber.isNotBlank() &&
                verificationDetails.panNumber.isNotBlank() &&
                verificationDetails.businessType.isNotBlank()
    }
}
