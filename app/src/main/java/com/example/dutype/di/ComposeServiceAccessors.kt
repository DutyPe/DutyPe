package com.example.dutype.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.dutype.services.BirthdayService
import com.example.dutype.services.InAppReviewTriggerService

/**
 * Composable accessors for singleton services that previously required a
 * `*ServiceHolder` `HiltViewModel` wrapper just to bridge them into Compose.
 *
 * Backed by [ComposeServiceEntryPoint], so the underlying service is the same
 * singleton instance Hilt would have constructor-injected.
 */
@Composable
fun rememberBirthdayService(): BirthdayService {
    val context = LocalContext.current
    return remember(context) {
        ComposeServiceEntryPoint.from(context).birthdayService()
    }
}

@Composable
fun rememberInAppReviewTriggerService(): InAppReviewTriggerService {
    val context = LocalContext.current
    return remember(context) {
        ComposeServiceEntryPoint.from(context).inAppReviewTriggerService()
    }
}
