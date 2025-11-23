package com.example.dutype.utils

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.profileinstaller.ProfileInstaller

/**
 * Initialization helper for Baseline Profile
 * Should be called in the main Activity's onCreate() before setContent()
 */
object ProfileInstallerInitializer {
    
    fun installProfileInstaller(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Verify the profile is installed and ready
            ProfileInstaller.writeProfile(activity.applicationContext)
        }
    }
}
