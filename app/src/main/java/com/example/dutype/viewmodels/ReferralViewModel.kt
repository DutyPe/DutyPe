package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.*
import com.example.dutype.services.ReferralService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
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
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker,
    appConfigRepository: com.example.dutype.repositories.AppConfigRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()
    private var statsObserverJob: Job? = null
    private var historyObserverJob: Job? = null

    /** Admin-editable referral config. UI consumes this for reward amounts. */
    val referralConfig: StateFlow<com.example.dutype.repositories.ReferralConfig> =
        appConfigRepository.referralConfig
    
    private val _analytics = MutableStateFlow<ReferralAnalytics?>(null)
    val analytics: StateFlow<ReferralAnalytics?> = _analytics.asStateFlow()
    
    private val _successStories = MutableStateFlow<List<ReferralSuccessStory>>(emptyList())
    val successStories: StateFlow<List<ReferralSuccessStory>> = _successStories.asStateFlow()

    private val _referrerInfo = MutableStateFlow(ReferrerInfo())
    val referrerInfo: StateFlow<ReferrerInfo> = _referrerInfo.asStateFlow()
    
    /**
     * Load referral data for current user.
     *
     * Implementation: this used to do a one-shot `getCurrentUserReferralStats()` +
     * `getCurrentUserReferralHistory()` fetch in addition to starting the
     * realtime observers. The observers already do a cache-first + server
     * read via addSnapshotListener, so the one-shots were pure duplicate
     * traffic. We now just ensure observers are running.
     */
    fun loadReferralData() {
        ensureRealtimeObservers()
        loadWithdrawalHistory()
        loadReferrerInfo()
    }

    fun refreshReferralStats() {
        loadReferralData()
    }

    private fun ensureRealtimeObservers() {
        if (statsObserverJob == null) {
            statsObserverJob = viewModelScope.launch {
                referralService.getCurrentUserReferralStatsFlow().collect { stats ->
                    if (stats != null) {
                        _uiState.value = _uiState.value.copy(
                            stats = stats,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }
        }

        if (historyObserverJob == null) {
            historyObserverJob = viewModelScope.launch {
                referralService.getReferralHistoryFlow().collect { history ->
                    _uiState.value = _uiState.value.copy(
                        referralHistory = history,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadWithdrawalHistory() {
        viewModelScope.launch {
            try {
                val history = referralService.getWithdrawalHistory()
                _uiState.value = _uiState.value.copy(withdrawalHistory = history)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load withdrawal history")
            }
        }
    }

    private fun loadReferrerInfo() {
        viewModelScope.launch {
            try {
                val result = referralService.getCurrentUserReferrerInfo()
                result.fold(
                    onSuccess = { info -> _referrerInfo.value = info },
                    onFailure = { e -> Timber.e(e, "Failed to load referrer info") }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading referrer info")
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
     * Request withdrawal
     */
    fun requestWithdrawal(upiId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingWithdrawal = true, withdrawalError = null)
            
            try {
                val amount = _uiState.value.stats?.availableBalance ?: 0.0
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
    
    override fun onCleared() {
        statsObserverJob?.cancel()
        historyObserverJob?.cancel()
        super.onCleared()
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
    val isLoading: Boolean = true,
    val error: String? = null,
    val stats: ReferralStats? = null,
    val referralHistory: List<Referral> = emptyList(),
    val withdrawalHistory: List<WithdrawalRequest> = emptyList(),
    val leaderboard: List<ReferralStats> = emptyList(),
    val isLoadingLeaderboard: Boolean = false,
    val isProcessingWithdrawal: Boolean = false,
    val withdrawalError: String? = null,
    val withdrawalSuccess: Boolean = false,
    val lastWithdrawalId: String? = null
)
