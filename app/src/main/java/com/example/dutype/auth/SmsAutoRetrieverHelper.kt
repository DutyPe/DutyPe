package com.example.dutype.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import timber.log.Timber
import java.util.regex.Pattern

/**
 * Enterprise-grade SMS Auto-Retrieval helper using Google Play Services SmsRetriever API.
 * Automatically listens for incoming SMS OTPs and extracts the 6-digit verification code.
 */
class SmsAutoRetrieverHelper(
    private val context: Context,
    private val onOtpRetrieved: (String) -> Unit,
    private val onError: (String) -> Unit = {}
) {
    private var isReceiverRegistered = false
    
    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
            
            try {
                val extras = intent.extras ?: return
                @Suppress("DEPRECATION")
                val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return
                
                when (status.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                        Timber.d("📱 SMS Retriever: SMS received successfully: $message")
                        
                        if (!message.isNullOrBlank()) {
                            val otpCode = extract6DigitOtp(message)
                            if (otpCode != null) {
                                Timber.i("📱 SMS Retriever: Extracted 6-digit OTP: $otpCode")
                                onOtpRetrieved(otpCode)
                            } else {
                                Timber.w("📱 SMS Retriever: Could not extract 6-digit OTP from message")
                            }
                        }
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        Timber.w("📱 SMS Retriever: Auto-retrieval timed out")
                        onError("Auto-retrieval timed out. Please enter OTP manually.")
                    }
                    else -> {
                        Timber.w("📱 SMS Retriever failed with status code: ${status.statusCode}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "📱 Error processing SMS Retriever broadcast")
            } finally {
                unregisterReceiver()
            }
        }
    }

    fun startListening() {
        try {
            val client = SmsRetriever.getClient(context)
            val task = client.startSmsRetriever()
            
            task.addOnSuccessListener {
                Timber.i("📱 SMS Retriever started listening successfully")
                registerReceiver()
            }
            
            task.addOnFailureListener { e ->
                Timber.e(e, "📱 Failed to start SMS Retriever")
                onError("Could not start automatic SMS reader.")
            }
        } catch (e: Exception) {
            Timber.e(e, "📱 Exception starting SMS Retriever")
        }
    }

    private fun registerReceiver() {
        if (isReceiverRegistered) return
        try {
            val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    context,
                    smsReceiver,
                    intentFilter,
                    ContextCompat.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(smsReceiver, intentFilter)
            }
            isReceiverRegistered = true
            Timber.d("📱 SMS Receiver registered")
        } catch (e: Exception) {
            Timber.e(e, "📱 Error registering SMS Receiver")
        }
    }

    fun unregisterReceiver() {
        if (!isReceiverRegistered) return
        try {
            context.unregisterReceiver(smsReceiver)
            isReceiverRegistered = false
            Timber.d("📱 SMS Receiver unregistered")
        } catch (e: Exception) {
            Timber.e(e, "📱 Error unregistering SMS Receiver")
        }
    }

    companion object {
        fun extract6DigitOtp(message: String): String? {
            return try {
                // Match 6 contiguous digits in the SMS message
                val pattern = Pattern.compile("\\b(\\d{6})\\b")
                val matcher = pattern.matcher(message)
                if (matcher.find()) {
                    matcher.group(1)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
