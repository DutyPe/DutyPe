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

    /**
     * Frictionless UPI payment recording:
     * When user initiates or completes payment via UPI app (GPay/PhonePe), DutyPe automatically
     * records the request with employer details and reference ID so the user doesn't have to enter
     * any 12-digit UTR or upload screenshots.
     */
    fun recordUpiPaymentInitiated(
        plan: Plan,
        referenceId: String,
        employerName: String = "",
        employerPhone: String = "",
        upiIdUsed: String = "RAZORPAY_GATEWAY"
    ) {
        if (_submitState.value == SubmitState.Loading) return
        viewModelScope.launch {
            _submitState.value = SubmitState.Loading
            subscriptionRepository.submitPaymentRequest(
                planId = plan.id,
                amountPaise = plan.pricePaise,
                upiIdUsed = upiIdUsed,
                utrNumber = referenceId,
                screenshotUrl = "",
                employerName = employerName,
                employerPhone = employerPhone
            )
                .onSuccess { _submitState.value = SubmitState.Success }
                .onFailure { _submitState.value = SubmitState.Error(it.message ?: "Failed to submit payment") }
        }
    }

    fun resetSubmitState() {
        _submitState.value = SubmitState.Idle
    }

    fun createRazorpayOrder(
        planId: String? = null,
        type: String = "SUBSCRIPTION",
        amountRupees: Int? = null,
        onReady: (com.example.dutype.repositories.RazorpayOrderResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _submitState.value = SubmitState.Loading
            subscriptionRepository.createRazorpayOrder(planId = planId, type = type, amountRupees = amountRupees)
                .onSuccess { order ->
                    _submitState.value = SubmitState.Idle
                    onReady(order)
                }
                .onFailure { err ->
                    val msg = err.message ?: "Failed to initiate Razorpay order"
                    _submitState.value = SubmitState.Error(msg)
                    onError(msg)
                }
        }
    }

    fun verifyRazorpayPayment(
        orderId: String,
        paymentId: String,
        signature: String,
        planId: String? = null,
        type: String = "SUBSCRIPTION",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _submitState.value = SubmitState.Loading
            subscriptionRepository.verifyRazorpayPayment(
                orderId = orderId,
                paymentId = paymentId,
                signature = signature,
                planId = planId,
                type = type
            )
                .onSuccess {
                    _submitState.value = SubmitState.Success
                    onSuccess()
                }
                .onFailure { err ->
                    val msg = err.message ?: "Payment verification failed"
                    _submitState.value = SubmitState.Error(msg)
                    onError(msg)
                }
        }
    }
}

sealed interface SubmitState {
    object Idle : SubmitState
    object Loading : SubmitState
    object Success : SubmitState
    data class Error(val message: String) : SubmitState
}
