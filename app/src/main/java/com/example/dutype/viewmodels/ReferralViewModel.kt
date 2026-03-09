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
 * Handles all referral-related operations including analytics and success stories
 */
@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val referralService: ReferralService,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()
    
    private val _analytics = MutableStateFlow<ReferralAnalytics?>(null)
    val analytics: StateFlow<ReferralAnalytics?> = _analytics.asStateFlow()
    
    private val _successStories = MutableStateFlow<List<ReferralSuccessStory>>(emptyList())
    val successStories: StateFlow<List<ReferralSuccessStory>> = _successStories.asStateFlow()
    
    /**
     * Load referral data for current user
     */
    fun loadReferralData() {
        viewModelScope.launch {
            com.example.dutype.performance.MainThreadChecker.assertMainThread()
            performanceTracker.trackOperation("loadReferralData")
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
     * Load analytics data for professional dashboard
     */
    fun loadAnalytics() {
        viewModelScope.launch {
            try {
                val result = referralService.getReferralAnalytics()
                result.fold(
                    onSuccess = { analytics ->
                        _analytics.value = analytics
                        Timber.d("🎁 REFERRAL: Analytics loaded successfully")
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to load analytics")
                        _analytics.value = ReferralAnalytics() // Default empty analytics
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading analytics")
                _analytics.value = ReferralAnalytics()
            }
        }
    }

    /**
     * Load success stories for social proof
     */
    fun loadSuccessStories() {
        viewModelScope.launch {
            try {
                val result = referralService.getSuccessStories(10)
                result.fold(
                    onSuccess = { stories ->
                        _successStories.value = stories
                        Timber.d("🎁 REFERRAL: Loaded ${stories.size} success stories")
                    },
                    onFailure = { e ->
                        Timber.e(e, "Failed to load success stories")
                        _successStories.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading success stories")
                _successStories.value = emptyList()
            }
        }
    }

    /**
     * Track referral click (for analytics)
     */
    fun trackClick(code: String, source: ShareChannel, deviceInfo: String, ipAddress: String) {
        viewModelScope.launch {
            try {
                referralService.trackReferralClick(code, source, deviceInfo, ipAddress)
                Timber.d("🎁 REFERRAL: Click tracked for $code from $source")
            } catch (e: Exception) {
                Timber.e(e, "Error tracking click (non-critical)")
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
