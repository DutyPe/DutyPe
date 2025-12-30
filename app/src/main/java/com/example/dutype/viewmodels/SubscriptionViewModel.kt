package com.example.dutype.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.SubscriptionPlan
import com.example.dutype.models.SubscriptionPlans
import com.example.dutype.models.UserSubscription
import com.example.dutype.services.RazorpayService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val currentSubscription: UserSubscription? = null,
    val plans: List<SubscriptionPlan> = SubscriptionPlans.ALL_PLANS,
    val selectedPlan: SubscriptionPlan? = null,
    val isYearly: Boolean = false,
    val canPostJob: Boolean = true,
    val jobPostsRemaining: String = "",
    val paymentSuccess: Boolean = false,
    val paymentError: String? = null,
    val error: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val razorpayService: RazorpayService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Store pending payment info
    private var pendingPlan: SubscriptionPlan? = null
    private var pendingIsYearly: Boolean = false
    
    init {
        loadCurrentSubscription()
        checkJobPostLimit()
    }
    
    fun loadCurrentSubscription() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val subscription = razorpayService.getCurrentSubscription()
                _uiState.value = _uiState.value.copy(
                    currentSubscription = subscription,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading subscription")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun checkJobPostLimit() {
        viewModelScope.launch {
            try {
                val (canPost, message) = razorpayService.canPostJob()
                _uiState.value = _uiState.value.copy(
                    canPostJob = canPost,
                    jobPostsRemaining = message
                )
            } catch (e: Exception) {
                Timber.e(e, "Error checking job post limit")
            }
        }
    }
    
    fun selectPlan(plan: SubscriptionPlan) {
        _uiState.value = _uiState.value.copy(selectedPlan = plan)
    }
    
    fun setBillingCycle(isYearly: Boolean) {
        _uiState.value = _uiState.value.copy(isYearly = isYearly)
    }
    
    /**
     * Initialize payment for selected plan
     */
    fun initializePayment(
        activity: Activity,
        plan: SubscriptionPlan,
        isYearly: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, paymentError = null)
            
            try {
                // Store pending payment info
                pendingPlan = plan
                pendingIsYearly = isYearly
                
                // Get user info
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                val userDoc = firestore.collection("users").document(userId).get().await()
                
                val userName = userDoc.getString("name") ?: userDoc.getString("businessName") ?: "User"
                val userEmail = userDoc.getString("email") ?: auth.currentUser?.email ?: ""
                val userPhone = userDoc.getString("phone") ?: auth.currentUser?.phoneNumber ?: ""
                
                // Initialize Razorpay
                razorpayService.initializeRazorpay(activity)
                
                // Start payment
                razorpayService.startPayment(
                    activity = activity,
                    plan = plan,
                    isYearly = isYearly,
                    userEmail = userEmail,
                    userPhone = userPhone,
                    userName = userName,
                    onSuccess = { paymentId, orderId, signature ->
                        handlePaymentSuccess(paymentId, orderId, signature)
                    },
                    onError = { errorCode, errorMessage ->
                        handlePaymentError(errorCode, errorMessage)
                    }
                )
                
            } catch (e: Exception) {
                Timber.e(e, "Error initializing payment")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    paymentError = e.message
                )
            }
        }
    }
    
    /**
     * Handle successful payment from Razorpay callback
     */
    fun handlePaymentSuccess(paymentId: String, orderId: String?, signature: String?) {
        viewModelScope.launch {
            try {
                val plan = pendingPlan ?: throw Exception("No pending plan")
                val isYearly = pendingIsYearly
                
                val result = razorpayService.handlePaymentSuccess(
                    paymentId = paymentId,
                    orderId = orderId,
                    signature = signature,
                    plan = plan,
                    isYearly = isYearly
                )
                
                result.onSuccess { subscription ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentSubscription = subscription,
                        paymentSuccess = true,
                        paymentError = null
                    )
                    
                    // Clear pending
                    pendingPlan = null
                    pendingIsYearly = false
                    
                    // Refresh job post limit
                    checkJobPostLimit()
                }
                
                result.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        paymentError = e.message
                    )
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error handling payment success")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    paymentError = e.message
                )
            }
        }
    }
    
    /**
     * Handle payment error from Razorpay callback
     */
    fun handlePaymentError(errorCode: Int, errorMessage: String) {
        Timber.e("Payment error: $errorCode - $errorMessage")
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            paymentError = errorMessage
        )
        
        // Clear pending
        pendingPlan = null
        pendingIsYearly = false
    }
    
    /**
     * Cancel subscription
     */
    fun cancelSubscription() {
        viewModelScope.launch {
            val subscriptionId = _uiState.value.currentSubscription?.id ?: return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                razorpayService.cancelSubscription(subscriptionId)
                loadCurrentSubscription()
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling subscription")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Increment job post usage after posting a job
     */
    fun incrementJobPostUsage() {
        viewModelScope.launch {
            try {
                razorpayService.incrementJobPostUsage()
                checkJobPostLimit()
            } catch (e: Exception) {
                Timber.e(e, "Error incrementing job post usage")
            }
        }
    }
    
    fun clearPaymentSuccess() {
        _uiState.value = _uiState.value.copy(paymentSuccess = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, paymentError = null)
    }
}
