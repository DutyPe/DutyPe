package com.example.dutype.utils

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Remember the WindowSizeClass for responsive layouts
 * 
 * This composable calculates the window size class based on the current activity's window.
 * Use this to adapt UI layouts for different screen sizes (phones, tablets, foldables).
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val context = LocalContext.current
    val activity = context as Activity
    return calculateWindowSizeClass(activity)
}
