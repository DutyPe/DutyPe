package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.PaymentRequest
import com.example.dutype.models.QrCode
import com.example.dutype.models.EmployerSubscription
import com.example.dutype.metadata.UserMetadata
import com.example.dutype.repositories.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val userMetadata: UserMetadata
) : ViewModel() {

    val activeSubscription: StateFlow<EmployerSubscription> = userMetadata.subscription

    private val _activeQrCodes = MutableStateFlow<List<QrCode>>(emptyList())
    val activeQrCodes: StateFlow<List<QrCode>> = _activeQrCodes.asStateFlow()

    private val _paymentRequests = MutableStateFlow<List<PaymentRequest>>(emptyList())
    val paymentRequests: StateFlow<List<PaymentRequest>> = _paymentRequests.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    init {
        loadActiveQrCodes()
        observePaymentRequests()
    }

    private fun loadActiveQrCodes() {
        viewModelScope.launch {
            subscriptionRepository.getActiveQrCodes().collectLatest { qrs ->
                _activeQrCodes.value = qrs
            }
        }
    }

    private fun observePaymentRequests() {
        viewModelScope.launch {
            userMetadata.userStats.collectLatest { stats ->
                if (stats.userId.isNotEmpty()) {
                    subscriptionRepository.getPaymentRequests(stats.userId).collectLatest { requests ->
                        _paymentRequests.value = requests
                    }
                }
            }
        }
    }

    fun submitPayment(planId: String, amount: Double, utrNumber: String, screenshotUrl: String) {
        val employerId = userMetadata.userStats.value.userId
        val employerPhone = userMetadata.userStats.value.phone

        if (utrNumber.trim().length != 12) {
            _submitState.value = SubmitState.Error("UTR number must be exactly 12 digits")
            return
        }

        viewModelScope.launch {
            _submitState.value = SubmitState.Loading
            val request = PaymentRequest(
                employerId = employerId,
                employerPhone = employerPhone,
                planId = planId,
                amount = amount,
                utrNumber = utrNumber.trim(),
                screenshotUrl = screenshotUrl,
                status = "PENDING",
                requestTimestamp = System.currentTimeMillis()
            )
            val result = subscriptionRepository.submitPaymentRequest(request)
            result.fold(
                onSuccess = {
                    _submitState.value = SubmitState.Success
                },
                onFailure = { error ->
                    _submitState.value = SubmitState.Error(error.message ?: "Failed to submit request")
                }
            )
        }
    }

    fun resetSubmitState() {
        _submitState.value = SubmitState.Idle
    }
}

sealed interface SubmitState {
    object Idle : SubmitState
    object Loading : SubmitState
    object Success : SubmitState
    data class Error(val message: String) : SubmitState
}
