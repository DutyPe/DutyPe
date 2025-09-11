package com.example.partimes.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ScrollAwareBottomBar(
    isVisible: Boolean,
    windowSizeClass: WindowSizeClass? = null,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Medium || 
                   windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
    
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 120f, // Increased offset for better hiding
        animationSpec = tween(durationMillis = 300),
        label = "bottom_bar_animation"
    )

    val horizontalPadding = when {
        isTablet && isLandscape -> 48.dp
        isTablet -> 32.dp
        isLandscape -> 24.dp
        else -> 16.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetY.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        content()
    }
}
