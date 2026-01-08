package com.example.dutype.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.JobPerk
import com.example.dutype.employer.models.JobUrgency
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.utils.JobValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * P2 PERFORMANCE FIX: PostJobViewModel
 * 
 * Moves all state management from PostJobScreen composable to ViewModel.
 * This prevents recomposition storms when any field changes.
 * 
 * Benefits:
 * - State survives configuration changes
 * - Reduces recomposition scope
 * - Enables proper validation flow
 * - Supports draft saving
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */

data class PostJobUiState(
    // Step 1: Job Details
    val title: String = "",
    val description: String = "",
    val selectedCategory: JobCategory? = null,
    val selectedPerks: Set<JobPerk> = emptySet(),
    val vacancies: Int = 1,
    val experienceRequired: String = "Fresher",
    val gender: String = "Any",
    
    // Step 2: Pay & Location
    val payAmount: String = "",
    val payType: PayType = PayType.DAILY,
    val location: String = "",
    val locationLatitude: Double = 0.0,
    val locationLongitude: Double = 0.0,
    val shiftTiming: ShiftTiming = ShiftTiming.FLEXIBLE,
    val workingDays: String = "Monday - Saturday",
    
    // Step 3: Contact Info
    val contactNumber: String = "",
    val whatsappNumber: String = "",
    val useWhatsappSameAsContact: Boolean = true,
    
    // Step 4: Review
    val urgency: JobUrgency = JobUrgency.NORMAL,
    val jobImageUri: Uri? = null,
    val jobImageUrl: String = "",
    
    // UI State
    val currentStep: Int = 1,
    val isSubmitting: Boolean = false,
    val isCheckingProfile: Boolean = false,
    val showScamWarningDialog: Boolean = false,
    val showPayRateWarningDialog: Boolean = false,
    val showProfileIncompleteDialog: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PostJobViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(PostJobUiState())
    val uiState: StateFlow<PostJobUiState> = _uiState.asStateFlow()
    
    private val totalSteps = 4
    
    // ==========================================
    // STEP 1: Job Details
    // ==========================================
    
    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }
    
    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }
    
    fun updateCategory(category: JobCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    
    fun togglePerk(perk: JobPerk) {
        _uiState.update { state ->
            val newPerks = if (state.selectedPerks.contains(perk)) {
                state.selectedPerks - perk
            } else {
                state.selectedPerks + perk
            }
            state.copy(selectedPerks = newPerks)
        }
    }
    
    fun updateVacancies(vacancies: Int) {
        _uiState.update { it.copy(vacancies = vacancies.coerceIn(1, 100)) }
    }
    
    fun updateExperienceRequired(experience: String) {
        _uiState.update { it.copy(experienceRequired = experience) }
    }
    
    fun updateGender(gender: String) {
        _uiState.update { it.copy(gender = gender) }
    }
    
    // ==========================================
    // STEP 2: Pay & Location
    // ==========================================
    
    fun updatePayAmount(amount: String) {
        _uiState.update { it.copy(payAmount = amount) }
    }
    
    fun updatePayType(payType: PayType) {
        _uiState.update { it.copy(payType = payType) }
    }
    
    fun updateLocation(location: String, latitude: Double = 0.0, longitude: Double = 0.0) {
        _uiState.update { it.copy(
            location = location,
            locationLatitude = latitude,
            locationLongitude = longitude
        )}
    }
    
    fun updateShiftTiming(timing: ShiftTiming) {
        _uiState.update { it.copy(shiftTiming = timing) }
    }
    
    fun updateWorkingDays(days: String) {
        _uiState.update { it.copy(workingDays = days) }
    }
    
    // ==========================================
    // STEP 3: Contact Info
    // ==========================================
    
    fun updateContactNumber(number: String) {
        _uiState.update { it.copy(
            contactNumber = number,
            whatsappNumber = if (it.useWhatsappSameAsContact) number else it.whatsappNumber
        )}
    }
    
    fun updateWhatsappNumber(number: String) {
        _uiState.update { it.copy(whatsappNumber = number) }
    }
    
    fun toggleWhatsappSameAsContact(same: Boolean) {
        _uiState.update { it.copy(
            useWhatsappSameAsContact = same,
            whatsappNumber = if (same) it.contactNumber else it.whatsappNumber
        )}
    }
    
    // ==========================================
    // STEP 4: Review
    // ==========================================
    
    fun updateUrgency(urgency: JobUrgency) {
        _uiState.update { it.copy(urgency = urgency) }
    }
    
    fun updateJobImage(uri: Uri?) {
        _uiState.update { it.copy(jobImageUri = uri) }
    }
    
    fun updateJobImageUrl(url: String) {
        _uiState.update { it.copy(jobImageUrl = url) }
    }
    
    // ==========================================
    // NAVIGATION
    // ==========================================
    
    fun goToNextStep(): Boolean {
        val state = _uiState.value
        
        // Validate current step
        if (!validateCurrentStep()) {
            return false
        }
        
        // Check for scam keywords on step 1
        if (state.currentStep == 1) {
            val scamResult = JobValidationUtils.validateAgainstScamKeywords(state.title, state.description)
            if (!scamResult.isValid) {
                _uiState.update { it.copy(showScamWarningDialog = true) }
                return false
            }
        }
        
        // Check pay rate on step 2
        if (state.currentStep == 2 && state.selectedCategory != null) {
            val payValidation = JobValidationUtils.validatePayRate(
                state.selectedCategory,
                state.payType,
                state.payAmount
            )
            if (!payValidation.isValid && (payValidation.isTooHigh || payValidation.isTooLow)) {
                _uiState.update { it.copy(showPayRateWarningDialog = true) }
                return false
            }
        }
        
        if (state.currentStep < totalSteps) {
            _uiState.update { it.copy(currentStep = state.currentStep + 1) }
            return true
        }
        return false
    }
    
    fun goToPreviousStep() {
        val state = _uiState.value
        if (state.currentStep > 1) {
            _uiState.update { it.copy(currentStep = state.currentStep - 1) }
        }
    }
    
    fun forceNextStep() {
        val state = _uiState.value
        if (state.currentStep < totalSteps) {
            _uiState.update { it.copy(
                currentStep = state.currentStep + 1,
                showScamWarningDialog = false,
                showPayRateWarningDialog = false
            )}
        }
    }
    
    // ==========================================
    // VALIDATION
    // ==========================================
    
    fun validateCurrentStep(): Boolean {
        val state = _uiState.value
        return when (state.currentStep) {
            1 -> state.title.isNotBlank() && state.description.isNotBlank()
            2 -> state.payAmount.isNotBlank() && state.location.isNotBlank()
            3 -> state.contactNumber.length >= 10
            4 -> true
            else -> false
        }
    }
    
    fun isNextEnabled(): Boolean {
        val state = _uiState.value
        return when (state.currentStep) {
            1 -> state.title.isNotBlank() && state.description.isNotBlank()
            2 -> state.payAmount.isNotBlank() && state.location.isNotBlank()
            3 -> state.contactNumber.length >= 10
            4 -> true
            else -> false
        }
    }
    
    // ==========================================
    // DIALOGS
    // ==========================================
    
    fun dismissScamWarningDialog() {
        _uiState.update { it.copy(showScamWarningDialog = false) }
    }
    
    fun dismissPayRateWarningDialog() {
        _uiState.update { it.copy(showPayRateWarningDialog = false) }
    }
    
    fun dismissProfileIncompleteDialog() {
        _uiState.update { it.copy(showProfileIncompleteDialog = false) }
    }
    
    fun dismissSuccessDialog() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }
    
    fun showProfileIncompleteDialog() {
        _uiState.update { it.copy(showProfileIncompleteDialog = true) }
    }
    
    // ==========================================
    // SUBMISSION
    // ==========================================
    
    fun setSubmitting(submitting: Boolean) {
        _uiState.update { it.copy(isSubmitting = submitting) }
    }
    
    fun setCheckingProfile(checking: Boolean) {
        _uiState.update { it.copy(isCheckingProfile = checking) }
    }
    
    fun showSuccess() {
        _uiState.update { it.copy(showSuccessDialog = true, isSubmitting = false) }
    }
    
    fun setError(message: String?) {
        _uiState.update { it.copy(errorMessage = message, isSubmitting = false) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    // ==========================================
    // RESET
    // ==========================================
    
    fun resetForm() {
        _uiState.value = PostJobUiState()
        Timber.d("📝 PostJobVM: Form reset")
    }
}
