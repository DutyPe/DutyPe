package com.example.dutype.di

import android.content.Context
import com.example.dutype.services.BirthdayService
import com.example.dutype.services.InAppReviewTriggerService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
/**
 * Hilt entry point for services that Compose screens need but that should NOT
 * be wrapped in a ViewModel just for injection.
 *
 * Replaces the prior `BirthdayServiceHolder` / `InAppReviewTriggerServiceHolder`
 * "ViewModel-as-DI" workaround. Both services are `@Singleton`, so reaching
 * for them via `EntryPointAccessors` is equivalent to constructor-injection
 * but without spawning a per-screen `ViewModel` whose only purpose is to hold
 * a single field.
 *
 * Usage from a `@Composable`:
 * ```kotlin
 * val context = LocalContext.current
 * val reviewTriggerService = remember(context) {
 *     ComposeServiceEntryPoint.from(context).inAppReviewTriggerService()
 * }
 * ```
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ComposeServiceEntryPoint {
    fun birthdayService(): BirthdayService
    fun inAppReviewTriggerService(): InAppReviewTriggerService
    fun deepLinkBus(): com.example.dutype.navigation.DeepLinkBus

    companion object {
        fun from(context: Context): ComposeServiceEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ComposeServiceEntryPoint::class.java
            )
    }
}
