package com.example.dutype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.dutype.navigation.MainNavGraph
import com.example.dutype.ui.theme.dutypeTheme
import com.example.dutype.ui.theme.ResponsiveTheme
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.rememberWindowSizeClass
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Request notification permission
        val permissionManager = NotificationPermissionManager(this)
        permissionManager.requestNotificationPermission { isGranted ->
            println("🔔 MainActivity - Notification permission granted: $isGranted")
        }

        setContent {
            val windowSizeClass = rememberWindowSizeClass()
            
            dutypeTheme {
                ResponsiveTheme(windowSizeClass = windowSizeClass) {
                    val navController = rememberNavController()

                    // System bar color state
                    var statusBarColor by remember { mutableStateOf(Color.White) } // White

                    // Apply system bar colors at the top level
                    LaunchedEffect(statusBarColor) {
                        window.statusBarColor = statusBarColor.toArgb()
                        window.navigationBarColor = Color.Black.toArgb()

                        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                        insetsController.isAppearanceLightStatusBars = true // Dark icons on white
                        insetsController.isAppearanceLightNavigationBars = false // Light icons on black
                    }

                    MainNavGraph(
                        navController = navController,
                        onStatusBarColorChange = { color ->
                            statusBarColor = color
                        },
                        notificationData = intent.extras?.getString("notificationId")
                    )
                }
            }
        }
    }
}
