package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.LeaderboardEntry
import com.example.dutype.models.LedgerEntry
import com.example.dutype.models.Referral
import com.example.dutype.models.Wallet
import com.example.dutype.models.WithdrawalRequest
import com.example.dutype.services.ReferralService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReferralUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val wallet: Wallet? = null,
    val ledger: List<LedgerEntry> = emptyList(),
    val referralHistory: List<Referral> = emptyList(),
    val withdrawalHistory: List<WithdrawalRequest> = emptyList(),
    /** The referral I joined with (null when I joined without a code). */
    val myReferrer: Referral? = null,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val isLoadingLeaderboard: Boolean = false,
    val isProcessingWithdrawal: Boolean = false,
    val withdrawalError: String? = null,
    val withdrawalSuccess: Boolean = false,
    val lastWithdrawalPaise: Long = 0L
)

/** Refer & Earn (worker and employer): wallet, referrals, money history, withdrawals. */
@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val referralService: ReferralService,
    appConfigRepository: com.example.dutype.repositories.AppConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()

    /** Admin-editable reward amounts. */
    val referralConfig: StateFlow<com.example.dutype.repositories.ReferralConfig> = appConfigRepository.referralConfig

    private val jobs = mutableListOf<Job>()

    /** Starts the live wallet / referrals / withdrawals once for this screen. */
    fun loadReferralData() {
        if (jobs.isNotEmpty()) return
        jobs += viewModelScope.launch {
            referralService.wallet().collect { wallet -> _uiState.update { it.copy(wallet = wallet, isLoading = false) } }
        }
        jobs += viewModelScope.launch {
            referralService.myReferrals().collect { list -> _uiState.update { it.copy(referralHistory = list) } }
        }
        jobs += viewModelScope.launch {
            referralService.ledger().collect { entries -> _uiState.update { it.copy(ledger = entries) } }
        }
        jobs += viewModelScope.launch {
            referralService.withdrawals().collect { list -> _uiState.update { it.copy(withdrawalHistory = list) } }
        }
        viewModelScope.launch { _uiState.update { it.copy(myReferrer = referralService.myReferrer()) } }
    }

    fun refreshReferralStats() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        loadReferralData()
    }

    fun loadLeaderboard() {
        _uiState.update { it.copy(isLoadingLeaderboard = true) }
        viewModelScope.launch {
            val rows = referralService.leaderboard().getOrDefault(emptyList())
            _uiState.update { it.copy(leaderboard = rows, isLoadingLeaderboard = false) }
        }
    }

    fun requestWithdrawal(upiId: String) {
        if (_uiState.value.isProcessingWithdrawal) return
        _uiState.update { it.copy(isProcessingWithdrawal = true, withdrawalError = null) }
        viewModelScope.launch {
            referralService.requestWithdrawal(upiId)
                .onSuccess { paise ->
                    _uiState.update { it.copy(isProcessingWithdrawal = false, withdrawalSuccess = true, lastWithdrawalPaise = paise) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isProcessingWithdrawal = false, withdrawalError = e.message ?: "Withdrawal failed") }
                }
        }
    }

    fun shareMessage(userName: String): String =
        referralService.shareMessage(_uiState.value.wallet?.referralCode.orEmpty(), userName)

    fun clearWithdrawalSuccess() = _uiState.update { it.copy(withdrawalSuccess = false) }

    fun clearError() = _uiState.update { it.copy(error = null, withdrawalError = null) }
}
