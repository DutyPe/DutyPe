package com.example.dutype.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Notification Permission Manager
 * Handles notification permissions for Android 13+ devices
 */
class NotificationPermissionManager(private val activity: ComponentActivity) {
    
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null
    
    // Permission launcher for Android 13+
    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.i("Notification permission result: $isGranted")
        onPermissionResult?.invoke(isGranted)
        if (!isGranted) {
            onPermissionDenied?.invoke()
        }
    }
    
    /**
     * Check if notification permission is granted
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are enabled by default
            true
        }
    }
    
    /**
     * Request notification permission
     */
    fun requestNotificationPermission(
        onResult: (Boolean) -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        onPermissionResult = onResult
        onPermissionDenied = onDenied
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isNotificationPermissionGranted()) {
                Timber.d("Notification permission already granted")
                onResult(true)
            } else {
                Timber.d("Requesting notification permission...")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Timber.d("Android version < 13, notification permission not required")
            onResult(true)
        }
    }
    
    /**
     * Request permission and show rationale if needed
     */
    fun requestPermissionWithRationale(
        onResult: (Boolean) -> Unit,
        showRationale: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isNotificationPermissionGranted()) {
                onResult(true)
            } else {
                // Check if we should show rationale
                if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showRationale()
                } else {
                    requestNotificationPermission(onResult)
                }
            }
        } else {
            onResult(true)
        }
    }
}
