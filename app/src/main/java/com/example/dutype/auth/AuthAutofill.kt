package com.example.dutype.auth

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.example.dutype.utils.SmsRetrieverHelper
import com.example.dutype.utils.findActivity
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import timber.log.Timber

@Composable
fun rememberPhoneNumberHintRequester(
    onPhoneNumberReceived: (String) -> Unit,
    onUnavailable: (String) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val currentOnPhoneNumberReceived = rememberUpdatedState(onPhoneNumberReceived)
    val currentOnUnavailable = rememberUpdatedState(onUnavailable)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        runCatching {
            Identity.getSignInClient(context).getPhoneNumberFromIntent(result.data)
        }.onSuccess { rawPhoneNumber ->
            val normalizedPhoneNumber = rawPhoneNumber
                .filter { it.isDigit() }
                .takeLast(10)

            if (normalizedPhoneNumber.length == 10) {
                currentOnPhoneNumberReceived.value(normalizedPhoneNumber)
            } else {
                currentOnUnavailable.value("Couldn't read a valid mobile number from your device.")
            }
        }.onFailure {
            Timber.w(it, "Phone number hint parsing failed")
            currentOnUnavailable.value("Couldn't read your device number right now.")
        }
    }

    return remember(context, launcher) {
        {
            val activity = context.findActivity()
            if (activity == null) {
                currentOnUnavailable.value("Phone number suggestions are unavailable on this screen.")
            } else {
                Identity.getSignInClient(activity)
                    .getPhoneNumberHintIntent(GetPhoneNumberHintIntentRequest.builder().build())
                    .addOnSuccessListener { pendingIntent ->
                        launcher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    }
                    .addOnFailureListener { error ->
                        Timber.w(error, "Phone number hint request failed")
                        currentOnUnavailable.value("Couldn't fetch your device number right now.")
                    }
            }

            Unit
        }
    }
}

@Composable
fun OtpAutoFillEffect(
    enabled: Boolean,
    otpValue: String,
    onOtpReceived: (String) -> Unit
) {
    val context = LocalContext.current
    val applicationContext = remember(context) { context.applicationContext }
    val currentOtpValue = rememberUpdatedState(otpValue)
    val currentOnOtpReceived = rememberUpdatedState(onOtpReceived)

    DisposableEffect(applicationContext, enabled) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }

        // Keep SMS Retriever active whenever OTP input is visible.
        runCatching {
            SmsRetrieverHelper.startSmsRetriever(applicationContext)
        }.onFailure {
            Timber.w(it, "Unable to restart SMS Retriever from OTP UI effect")
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action != SmsRetriever.SMS_RETRIEVED_ACTION) return

                val extras = intent.extras ?: return
                val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

                when (status.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE).orEmpty()
                        val otpCode = SmsRetrieverHelper.extractOtpCode(message)
                        if (!otpCode.isNullOrBlank() && otpCode != currentOtpValue.value) {
                            currentOnOtpReceived.value(otpCode)
                        }
                    }

                    CommonStatusCodes.TIMEOUT -> {
                        Timber.i("SMS Retriever timed out before an OTP could be captured")
                    }
                }
            }
        }

        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            applicationContext.registerReceiver(receiver, filter)
        }

        onDispose {
            runCatching {
                applicationContext.unregisterReceiver(receiver)
            }.onFailure {
                Timber.v(it, "SMS Retriever receiver was already cleared")
            }
        }
    }
}