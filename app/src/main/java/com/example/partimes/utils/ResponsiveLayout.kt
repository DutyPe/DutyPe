package com.example.partimes.utils

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val activity = androidx.compose.ui.platform.LocalContext.current as android.app.Activity
    return calculateWindowSizeClass(activity)
}

@Composable
fun getResponsivePadding(windowSizeClass: WindowSizeClass): ResponsivePadding {
    return when {
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact -> {
            ResponsivePadding(
                horizontal = 16.dp,
                vertical = 8.dp,
                small = 4.dp
            )
        }
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> {
            ResponsivePadding(
                horizontal = 24.dp,
                vertical = 12.dp,
                small = 6.dp
            )
        }
        else -> {
            ResponsivePadding(
                horizontal = 32.dp,
                vertical = 16.dp,
                small = 8.dp
            )
        }
    }
}

data class ResponsivePadding(
    val horizontal: androidx.compose.ui.unit.Dp,
    val vertical: androidx.compose.ui.unit.Dp,
    val small: androidx.compose.ui.unit.Dp
)

@Composable
fun getResponsiveFontSize(windowSizeClass: WindowSizeClass): ResponsiveFontSize {
    return when {
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact -> {
            ResponsiveFontSize(
                title = 24.dp,
                subtitle = 18.dp,
                body = 14.dp,
                caption = 12.dp
            )
        }
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> {
            ResponsiveFontSize(
                title = 28.dp,
                subtitle = 20.dp,
                body = 16.dp,
                caption = 14.dp
            )
        }
        else -> {
            ResponsiveFontSize(
                title = 32.dp,
                subtitle = 24.dp,
                body = 18.dp,
                caption = 16.dp
            )
        }
    }
}

data class ResponsiveFontSize(
    val title: androidx.compose.ui.unit.Dp,
    val subtitle: androidx.compose.ui.unit.Dp,
    val body: androidx.compose.ui.unit.Dp,
    val caption: androidx.compose.ui.unit.Dp
)
