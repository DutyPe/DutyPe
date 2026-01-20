package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.ui.theme.MeeshoFontFamily
import kotlinx.coroutines.delay

/**
 * Clean & Fast DutyPe Splash Screen
 * Pure white background with black text - no animations, instant display
 * Shows for 1 second only
 */
@Composable
fun DutyPeSplashScreen(
    navController: NavController,
    onSplashComplete: () -> Unit = {},
    duration: Long = 1000L // 1 second - fast launch
) {
    // Simple timer - no animations
    LaunchedEffect(Unit) {
        delay(duration)
        onSplashComplete()
    }

    // Colors - Clean white theme
    val backgroundColor = Color.White
    val primaryBlack = Color(0xFF1F2937)
    val secondaryGray = Color(0xFF6B7280)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Main content - static, no animations
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // DutyPe Text - Clean black
            Text(
                text = "DutyPe",
                style = TextStyle(
                    fontFamily = MeeshoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    color = primaryBlack
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline - Subtle gray
            Text(
                text = "Find Work. Find Workers.",
                style = TextStyle(
                    fontFamily = MeeshoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = secondaryGray,
                    letterSpacing = 1.sp
                )
            )
        }

        // Bottom branding - minimal
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Made with 💙 in India",
                style = TextStyle(
                    fontFamily = MeeshoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = secondaryGray.copy(alpha = 0.7f)
                )
            )
        }
    }
}
