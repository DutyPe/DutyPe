package com.example.dutype.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.example.dutype.utils.findActivity
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
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

