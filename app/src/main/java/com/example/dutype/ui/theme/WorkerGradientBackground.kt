package com.example.dutype.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Worker screen background wrapper.
 * 
 * Currently applies a simple white background. This wrapper exists for:
 * 1. Future theming support (gradient backgrounds, dark mode, etc.)
 * 2. Consistent background styling across all worker screens
 * 3. Easy global background changes without modifying individual screens
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
            .background(Color.White)
    ) {
        content()
    }
}
