package com.example.dutype.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dutype.app.R
import com.example.dutype.repositories.AppUpdateConfig
import timber.log.Timber

@Composable
fun AppUpdatePrompt(
    config: AppUpdateConfig,
    currentVersionCode: Long,
    userRole: String
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTelugu = remember(configuration) {
        configuration.locales[0]?.language.equals("te", ignoreCase = true)
    }
    val shouldPrompt = config.shouldPromptFor(currentVersionCode, userRole)
    val promptKey = config.dismissalKey
    var dismissedPromptKey by rememberSaveable { mutableStateOf<String?>(null) }
    val isVisible = shouldPrompt && dismissedPromptKey != promptKey

    if (!isVisible) return

    val fallbackTitle = stringResource(R.string.app_update_prompt_title)
    val fallbackMessage = stringResource(R.string.app_update_prompt_message)
    val fallbackButton = stringResource(R.string.app_update_prompt_update)
    val title = localizedConfigText(isTelugu, config.title, config.titleTe, fallbackTitle)
    val message = localizedConfigText(isTelugu, config.message, config.messageTe, fallbackMessage)
    val buttonText = localizedConfigText(isTelugu, config.buttonText, config.buttonTextTe, fallbackButton)

    AlertDialog(
        onDismissRequest = {
            dismissedPromptKey = promptKey
        },
        title = { Text(text = title) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!openAppUpdatePage(context, config.playStoreUrl)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.app_update_open_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {
                Text(text = buttonText)
            }
        },
        dismissButton = {
            TextButton(onClick = { dismissedPromptKey = promptKey }) {
                Text(text = stringResource(R.string.app_update_prompt_later))
            }
        }
    )
}

private fun localizedConfigText(
    isTelugu: Boolean,
    english: String,
    telugu: String,
    fallback: String
): String {
    return if (isTelugu) {
        telugu.ifBlank { fallback }
    } else {
        english.ifBlank { fallback }
    }
}


private fun openAppUpdatePage(context: Context, configuredUrl: String): Boolean {
    val packageName = context.packageName
    val fallbackUrl = "https://play.google.com/store/apps/details?id=$packageName"
    val trimmedUrl = configuredUrl.trim()

    if (trimmedUrl.isBlank() || trimmedUrl.contains("play.google.com/store/apps/details", ignoreCase = true)) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            return true
        } catch (_: ActivityNotFoundException) {
            // Fall through to the web URL below.
        }
    }

    return try {
        val uri = Uri.parse(trimmedUrl.ifBlank { fallbackUrl })
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        true
    } catch (error: Exception) {
        Timber.e(error, "Failed to open app update page")
        false
    }
}
