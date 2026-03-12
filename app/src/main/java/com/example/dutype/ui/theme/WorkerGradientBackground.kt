package com.example.dutype.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Worker screen background wrapper.
 * Applies warm gray background (#E8E6DF) matching the web app design.
 * 
 * Used by: WorkerHomeScreen, MyJobsScreen
 */
@Composable
fun WorkerGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        content()
    }
}
