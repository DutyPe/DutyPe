package com.example.dutype.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.example.dutype.viewmodels.ConnectivityViewModel

/**
 * Connectivity Aware Screen - Wrapper that automatically shows offline banner
 * 
 * Usage:
 * ```
 * ConnectivityAwareScreen {
 *     // Your screen content with its own background
 *     YourScreenContent()
 * }
 * ```
 * 
 * This will automatically show the offline banner at the top when there's no internet.
 * Banner is positioned as an overlay at the top of the screen.
 * 
 * IMPORTANT: Your content should have its own background modifier.
 * This wrapper is transparent to preserve your screen's background.
 */
@Composable
fun ConnectivityAwareScreen(
    modifier: Modifier = Modifier,
    viewModel: ConnectivityViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val isOnline by viewModel.isOnline.collectAsState()
    
    Box(modifier = modifier.fillMaxSize()) {
        // Screen content (should have its own background)
        content()
        
        // Offline banner overlay at top
        OfflineBanner(
            isOffline = !isOnline,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(999f) // Ensure it's on top
        )
    }
}
