package com.example.dutype.ui.theme

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ResponsiveDimensions(
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,
    val cornerRadiusSmall: Dp,
    val cornerRadiusMedium: Dp,
    val cornerRadiusLarge: Dp,
    val spacingSmall: Dp,
    val spacingMedium: Dp,
    val spacingLarge: Dp,
    val iconSizeSmall: Dp,
    val iconSizeMedium: Dp,
    val iconSizeLarge: Dp
)

val LocalResponsiveDimensions = compositionLocalOf {
    ResponsiveDimensions(
        paddingSmall = 8.dp,
        paddingMedium = 16.dp,
        paddingLarge = 24.dp,
        cornerRadiusSmall = 4.dp,
        cornerRadiusMedium = 8.dp,
        cornerRadiusLarge = 16.dp,
        spacingSmall = 4.dp,
        spacingMedium = 8.dp,
        spacingLarge = 16.dp,
        iconSizeSmall = 16.dp,
        iconSizeMedium = 24.dp,
        iconSizeLarge = 32.dp
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ResponsiveTheme(
    windowSizeClass: WindowSizeClass,
    content: @Composable () -> Unit
) {
    val dimensions = remember(windowSizeClass) {
        when {
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact -> {
                // Phone portrait
                ResponsiveDimensions(
                    paddingSmall = 8.dp,
                    paddingMedium = 16.dp,
                    paddingLarge = 24.dp,
                    cornerRadiusSmall = 4.dp,
                    cornerRadiusMedium = 8.dp,
                    cornerRadiusLarge = 16.dp,
                    spacingSmall = 4.dp,
                    spacingMedium = 8.dp,
                    spacingLarge = 16.dp,
                    iconSizeSmall = 16.dp,
                    iconSizeMedium = 24.dp,
                    iconSizeLarge = 32.dp
                )
            }
            windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium -> {
                // Tablet portrait or phone landscape
                ResponsiveDimensions(
                    paddingSmall = 12.dp,
                    paddingMedium = 20.dp,
                    paddingLarge = 32.dp,
                    cornerRadiusSmall = 6.dp,
                    cornerRadiusMedium = 12.dp,
                    cornerRadiusLarge = 20.dp,
                    spacingSmall = 6.dp,
                    spacingMedium = 12.dp,
                    spacingLarge = 20.dp,
                    iconSizeSmall = 20.dp,
                    iconSizeMedium = 28.dp,
                    iconSizeLarge = 36.dp
                )
            }
            else -> {
                // Tablet landscape or desktop
                ResponsiveDimensions(
                    paddingSmall = 16.dp,
                    paddingMedium = 24.dp,
                    paddingLarge = 40.dp,
                    cornerRadiusSmall = 8.dp,
                    cornerRadiusMedium = 16.dp,
                    cornerRadiusLarge = 24.dp,
                    spacingSmall = 8.dp,
                    spacingMedium = 16.dp,
                    spacingLarge = 24.dp,
                    iconSizeSmall = 24.dp,
                    iconSizeMedium = 32.dp,
                    iconSizeLarge = 40.dp
                )
            }
        }
    }

    CompositionLocalProvider(
        LocalResponsiveDimensions provides dimensions,
        content = content
    )
}
