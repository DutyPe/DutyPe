package com.example.partimes.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DeviceLayoutInfo(
    val isCompact: Boolean,
    val isMedium: Boolean,
    val isExpanded: Boolean,
    val isLandscape: Boolean,
    val isPortrait: Boolean,
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isTablet: Boolean,
    val isPhone: Boolean
)

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun getDeviceLayoutInfo(windowSizeClass: WindowSizeClass): DeviceLayoutInfo {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
    
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    
    // Determine if it's a tablet based on screen size and density
    val isTablet = screenWidth >= 600.dp || (isMedium && !isLandscape)
    val isPhone = !isTablet
    
    return DeviceLayoutInfo(
        isCompact = isCompact,
        isMedium = isMedium,
        isExpanded = isExpanded,
        isLandscape = isLandscape,
        isPortrait = isPortrait,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        isTablet = isTablet,
        isPhone = isPhone
    )
}

@Composable
fun getResponsiveContentPadding(deviceInfo: DeviceLayoutInfo): PaddingValues {
    return when {
        deviceInfo.isTablet && deviceInfo.isLandscape -> {
            PaddingValues(
                horizontal = 48.dp,
                vertical = 24.dp
            )
        }
        deviceInfo.isTablet -> {
            PaddingValues(
                horizontal = 32.dp,
                vertical = 16.dp
            )
        }
        deviceInfo.isLandscape -> {
            PaddingValues(
                horizontal = 24.dp,
                vertical = 12.dp
            )
        }
        else -> {
            PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        }
    }
}

@Composable
fun getResponsiveBottomBarHeight(deviceInfo: DeviceLayoutInfo): Dp {
    return when {
        deviceInfo.isTablet -> 80.dp
        deviceInfo.isLandscape -> 60.dp
        else -> 70.dp
    }
}

@Composable
fun getResponsiveTopBarHeight(deviceInfo: DeviceLayoutInfo): Dp {
    return when {
        deviceInfo.isTablet -> 80.dp
        deviceInfo.isLandscape -> 60.dp
        else -> 70.dp
    }
}
