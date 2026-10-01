package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.models.EmployerSubscription
import com.example.dutype.models.Plan
import com.example.dutype.models.QrCode
import com.example.dutype.profile.CurrentProfileStore
import com.example.dutype.repositories.PaymentRequest
import com.example.dutype.repositories.SubscriptionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val profileStore: CurrentProfileStore,
    private val auth: FirebaseAuth
) : ViewModel() {

    val activeSubscription: StateFlow<EmployerSubscription> = profileStore.subscription

    /** For the free DutyPe AI tries left. */
    val employer = profileStore.employer

    private val _activeQrCodes = MutableStateFlow<List<QrCode>>(emptyList())
    val activeQrCodes: StateFlow<List<QrCode>> = _activeQrCodes.asStateFlow()

    private val _paymentRequests = MutableStateFlow<List<PaymentRequest>>(emptyList())
    val paymentRequests: StateFlow<List<PaymentRequest>> = _paymentRequests.asStateFlow()

    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    private var purchaseDataLoaded = false

    init {
        profileStore.start(Values.Role.EMPLOYER)
    }

    /** Plan names/limits only (one read). */
    fun loadPlans() {
        if (_plans.value.isNotEmpty()) return
        viewModelScope.launch { _plans.value = subscriptionRepository.getPlans() }
    }

    /** Plans, QR codes and payment history: only the subscription screen needs them. */
    fun loadPurchaseData() {
        if (purchaseDataLoaded) return
        purchaseDataLoaded = true
        loadPlans()
        viewModelScope.launch { _activeQrCodes.value = subscriptionRepository.getActiveQrCodes() }
        auth.currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                subscriptionRepository.getPaymentRequests(uid).collect { _paymentRequests.value = it }
            }
        }
    }

    fun submitPayment(plan: Plan, utrNumber: String, screenshotUrl: String, upiIdUsed: String = "") {
        if (utrNumber.trim().length != 12) {
            _submitState.value = SubmitState.Error("UTR number must be exactly 12 digits")
            return
        }
        if (_submitState.value == SubmitState.Loading) return
        viewModelScope.launch {
            _submitState.value = SubmitState.Loading
            subscriptionRepository.submitPaymentRequest(plan.id, plan.pricePaise, upiIdUsed, utrNumber, screenshotUrl)
                .onSuccess { _submitState.value = SubmitState.Success }
                .onFailure { _submitState.value = SubmitState.Error(it.message ?: "Failed to submit request") }
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
