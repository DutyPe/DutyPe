package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.*
import com.example.dutype.services.ReferralService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Referral screens (Worker & Employer)
 * Handles all referral-related operations
 */
@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val referralService: ReferralService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()
    
    /**
     * Load referral data for current user
     */
    fun loadReferralData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load stats
                val statsResult = referralService.getCurrentUserReferralStats()
                statsResult.fold(
                    onSuccess = { stats ->
                        _uiState.value = _uiState.value.copy(stats = stats)
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to load referral stats")
                    }
                )
                
                // Load history
                val historyResult = referralService.getCurrentUserReferralHistory()
                historyResult.fold(
                    onSuccess = { history ->
                        _uiState.value = _uiState.value.copy(referralHistory = history)
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to load referral history")
                    }
                )
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Timber.e(e, "Error loading referral data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load referral data"
                )
            }
        }
    }

    /**
     * Request withdrawal
     */
    fun requestWithdrawal(amount: Double, upiId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingWithdrawal = true, withdrawalError = null)
            
            try {
                val result = referralService.requestWithdrawal(
                    amount = amount,
                    paymentMethod = PaymentMethod.UPI,
                    upiId = upiId
                )
                
                result.fold(
                    onSuccess = { withdrawalResult ->
                        _uiState.value = _uiState.value.copy(
                            isProcessingWithdrawal = false,
                            withdrawalSuccess = true,
                            lastWithdrawalId = withdrawalResult.withdrawalId
                        )
                        // Reload data to update balance
                        loadReferralData()
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isProcessingWithdrawal = false,
                            withdrawalError = e.message ?: "Withdrawal failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error requesting withdrawal")
                _uiState.value = _uiState.value.copy(
                    isProcessingWithdrawal = false,
                    withdrawalError = e.message ?: "Withdrawal failed"
                )
            }
        }
    }
    
    /**
     * Check free job postings for employer
     */
    fun checkFreeJobPostings(userId: String) {
        viewModelScope.launch {
            try {
                val result = referralService.checkFreeJobPostings(userId)
                result.fold(
                    onSuccess = { (count, expiry) ->
                        _uiState.value = _uiState.value.copy(
                            freeJobPostings = count,
                            freeJobPostingsExpiry = expiry
                        )
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to check free job postings")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error checking free job postings")
            }
        }
    }
    
    /**
     * Use a free job posting
     */
    suspend fun useFreeJobPosting(userId: String): Boolean {
        return try {
            val result = referralService.useFreeJobPosting(userId)
            result.getOrNull() ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error using free job posting")
            false
        }
    }
    
    /**
     * Get top referrers for leaderboard
     */
    fun loadLeaderboard(role: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLeaderboard = true)
            
            try {
                val result = referralService.getTopReferrers(role, 10)
                result.fold(
                    onSuccess = { topReferrers ->
                        _uiState.value = _uiState.value.copy(
                            leaderboard = topReferrers,
                            isLoadingLeaderboard = false
                        )
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to load leaderboard")
                        _uiState.value = _uiState.value.copy(isLoadingLeaderboard = false)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading leaderboard")
                _uiState.value = _uiState.value.copy(isLoadingLeaderboard = false)
            }
        }
    }
    
    /**
     * Clear withdrawal success state
     */
    fun clearWithdrawalSuccess() {
        _uiState.value = _uiState.value.copy(withdrawalSuccess = false, lastWithdrawalId = null)
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, withdrawalError = null)
    }
}

/**
 * UI State for Referral screens
 */
data class ReferralUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val stats: ReferralStats? = null,
    val referralHistory: List<Referral> = emptyList(),
    val leaderboard: List<ReferralStats> = emptyList(),
    val isLoadingLeaderboard: Boolean = false,
    val isProcessingWithdrawal: Boolean = false,
    val withdrawalError: String? = null,
    val withdrawalSuccess: Boolean = false,
    val lastWithdrawalId: String? = null,
    val freeJobPostings: Int = 0,
    val freeJobPostingsExpiry: Long? = null
)
