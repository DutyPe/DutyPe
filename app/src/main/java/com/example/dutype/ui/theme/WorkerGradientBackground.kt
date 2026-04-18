package com.example.dutype.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Wrapper that fills the available space with the active role's solid screen
 * background pulled from [LocalRoleColors]. The historical name is kept so
 * existing call-sites continue to compile, but the implementation now respects
 * the role theme instead of hard-coding the worker palette.
 *
 * Prefer reading `LocalRoleColors.current.screenBackground` directly in new
 * code; this helper is here only for backwards compatibility.
 */
@Composable
fun WorkerGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val roleColors = LocalRoleColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(roleColors.screenBackground)
    ) {
        content()
    }
}
