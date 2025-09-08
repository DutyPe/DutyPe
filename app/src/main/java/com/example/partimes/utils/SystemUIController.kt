package com.example.partimes.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity

/**
 * Modern utility composable to handle system UI colors for edge-to-edge display
 * Uses non-deprecated APIs for Android 11+ compatibility
 */
@Composable
fun SystemUIController(
    statusBarColor: Color = Color.Transparent,
    navigationBarColor: Color = Color.Black,
    statusBarDarkIcons: Boolean = false,
    navigationBarDarkIcons: Boolean = false
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Enable edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Set system bar colors using modern approach
            window.statusBarColor = statusBarColor.value.toInt()
            window.navigationBarColor = navigationBarColor.value.toInt()

            // Configure icon appearances
            insetsController.isAppearanceLightStatusBars = statusBarDarkIcons
            insetsController.isAppearanceLightNavigationBars = navigationBarDarkIcons
        }
    }
}

/**
 * Predefined system UI configurations for different app sections
 */
object SystemUIConfigs {
    @Composable
    fun JobseekerProfile() {
        SystemUIController(
            statusBarColor = Color(0xFF0066FF),
            navigationBarColor = Color.Black, // BLACK navigation bar
            statusBarDarkIcons = false,
            navigationBarDarkIcons = false
        )
    }

    @Composable
    fun EmployerProfile() {
        SystemUIController(
            statusBarColor = Color(0xFF1976D2),
            navigationBarColor = Color.Black, // BLACK navigation bar
            statusBarDarkIcons = false,
            navigationBarDarkIcons = false
        )
    }

    @Composable
    fun JobseekerHome() {
        SystemUIController(
            statusBarColor = Color(0xFF0066FF),
            navigationBarColor = Color.Black, // BLACK navigation bar
            statusBarDarkIcons = false,
            navigationBarDarkIcons = false
        )
    }

    @Composable
    fun EmployerHome() {
        SystemUIController(
            statusBarColor = Color(0xFF1976D2),
            navigationBarColor = Color.Black, // BLACK navigation bar
            statusBarDarkIcons = false,
            navigationBarDarkIcons = false
        )
    }

    @Composable
    fun Default() {
        SystemUIController(
            statusBarColor = Color.Transparent,
            navigationBarColor = Color.Black, // BLACK navigation bar
            statusBarDarkIcons = true,
            navigationBarDarkIcons = true
        )
    }
}
