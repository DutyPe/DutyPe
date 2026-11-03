package com.example.dutype.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject
import timber.log.Timber

/**
 * Isolated Activity hosting the official Razorpay Checkout SDK.
 * Renders a clean loading transition and delegates standard payment handling.
 */
class RazorpayPaymentActivity : ComponentActivity(), PaymentResultWithDataListener {

    private var hasOpenedCheckout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF0A66C2),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "Connecting to Razorpay...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Opening 100% secure standard payment gateway",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        val orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: run {
            finishWithResult(false, "Invalid order ID")
            return
        }
        val amountPaise = intent.getLongExtra(EXTRA_AMOUNT_PAISE, 0L)
        val planName = intent.getStringExtra(EXTRA_PLAN_NAME) ?: "DutyPe Service Payment"
        val userPhone = intent.getStringExtra(EXTRA_USER_PHONE) ?: ""
        val userEmail = intent.getStringExtra(EXTRA_USER_EMAIL) ?: "support@dutype.in"
        val keyId = intent.getStringExtra(EXTRA_KEY_ID) ?: "rzp_live_TvEYRD4s7DgLuj"

        if (!hasOpenedCheckout) {
            hasOpenedCheckout = true
            openRazorpay(orderId, amountPaise, planName, userPhone, userEmail, keyId)
        }
    }

    private fun openRazorpay(
        orderId: String,
        amountPaise: Long,
        planName: String,
        userPhone: String,
        userEmail: String,
        keyId: String
    ) {
        try {
            Checkout.preload(applicationContext)
            val checkout = Checkout()
            checkout.setKeyID(keyId)
            checkout.setImage(com.dutype.app.R.mipmap.ic_launcher)

            val options = JSONObject().apply {
                put("name", "DutyPe")
                put("description", planName)
                put("order_id", orderId)
                put("currency", "INR")
                put("amount", amountPaise)
                put("send_sms_hash", true)
                put("remember_customer", false)

                // Explicitly allow and show all payment options
                val methodObj = JSONObject().apply {
                    put("upi", true)
                    put("card", true)
                    put("netbanking", true)
                    put("wallet", true)
                    put("emi", true)
                }
                put("method", methodObj)

                val displayObj = JSONObject().apply {
                    val preferences = JSONObject().apply {
                        put("show_default_blocks", true)
                    }
                    put("preferences", preferences)
                }
                put("display", displayObj)

                val prefill = JSONObject().apply {
                    if (userPhone.isNotBlank()) put("contact", userPhone.replace("+91", "").trim())
                    if (userEmail.isNotBlank()) put("email", userEmail)
                }
                put("prefill", prefill)

                val theme = JSONObject().apply {
                    put("color", "#0A66C2")
                }
                put("theme", theme)

                val retry = JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 2)
                }
                put("retry", retry)
            }

            checkout.open(this, options)
        } catch (e: Exception) {
            Timber.e(e, "Error opening Razorpay checkout")
            Toast.makeText(this, "Failed to launch Razorpay: ${e.message}", Toast.LENGTH_LONG).show()
            finishWithResult(false, e.message ?: "Failed to open payment gateway")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?, paymentData: PaymentData?) {
        val orderId = paymentData?.orderId ?: intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
        val signature = paymentData?.signature.orEmpty()
        val paymentId = razorpayPaymentID ?: paymentData?.paymentId.orEmpty()

        Timber.d("Razorpay success: paymentId=$paymentId, orderId=$orderId")
        val resultIntent = Intent().apply {
            putExtra(EXTRA_PAYMENT_SUCCESS, true)
            putExtra(EXTRA_PAYMENT_ID, paymentId)
            putExtra(EXTRA_ORDER_ID_RES, orderId)
            putExtra(EXTRA_SIGNATURE, signature)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Timber.w("Razorpay error: code=$code, response=$response")
        val resultIntent = Intent().apply {
            putExtra(EXTRA_PAYMENT_SUCCESS, false)
            putExtra(EXTRA_ERROR_MESSAGE, response ?: "Payment cancelled or dismissed")
            putExtra(EXTRA_ERROR_CODE, code)
        }
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun finishWithResult(success: Boolean, message: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_PAYMENT_SUCCESS, success)
            putExtra(EXTRA_ERROR_MESSAGE, message)
        }
        setResult(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_AMOUNT_PAISE = "extra_amount_paise"
        const val EXTRA_PLAN_NAME = "extra_plan_name"
        const val EXTRA_USER_PHONE = "extra_user_phone"
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_KEY_ID = "extra_key_id"

        const val EXTRA_PAYMENT_SUCCESS = "extra_payment_success"
        const val EXTRA_PAYMENT_ID = "extra_payment_id"
        const val EXTRA_ORDER_ID_RES = "extra_order_id_res"
        const val EXTRA_SIGNATURE = "extra_signature"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val EXTRA_ERROR_CODE = "extra_error_code"

        fun createIntent(
            context: Context,
            orderId: String,
            amountPaise: Long,
            planName: String,
            userPhone: String,
            userEmail: String = "",
            keyId: String = "rzp_live_TvEYRD4s7DgLuj"
        ): Intent {
            return Intent(context, RazorpayPaymentActivity::class.java).apply {
                putExtra(EXTRA_ORDER_ID, orderId)
                putExtra(EXTRA_AMOUNT_PAISE, amountPaise)
                putExtra(EXTRA_PLAN_NAME, planName)
                putExtra(EXTRA_USER_PHONE, userPhone)
                putExtra(EXTRA_USER_EMAIL, userEmail)
                putExtra(EXTRA_KEY_ID, keyId)
            }
        }
    }
}
