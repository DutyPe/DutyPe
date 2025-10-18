package com.example.dutype.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dutype.R
import kotlinx.coroutines.delay

/**
 * Unified DutyPe Splash Screen Component
 * Shows the DutyPe logo with consistent styling across the app
 * Duration: 1-2 seconds for fast loading
 */
@Composable
fun DutyPeSplashScreen(
    navController: NavController,
    onSplashComplete: () -> Unit = {},
    duration: Long = 2000L // 2 seconds default (faster)
) {
    // Launch coroutine for navigation
    LaunchedEffect(Unit) {
        // Show logo for specified duration
        delay(duration)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Black background
        contentAlignment = Alignment.Center
    ) {
        // Larger logo display - perfectly centered
        Image(
            painter = painterResource(id = R.drawable.dutype),
            contentDescription = "DutyPe Logo",
            modifier = Modifier.size(240.dp), // Increased from 160dp to 240dp
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Simplified splash screen for returning users
 * Shows for 1.5 seconds for fast loading
 */
@Composable
fun DutyPeQuickSplash(
    onSplashComplete: () -> Unit = {},
    duration: Long = 1500L // 1.5 seconds for returning users (faster)
) {
    // Launch coroutine for completion
    LaunchedEffect(Unit) {
        delay(duration)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Black background
        contentAlignment = Alignment.Center
    ) {
        // Larger logo display - perfectly centered
        Image(
            painter = painterResource(id = R.drawable.dutype),
            contentDescription = "DutyPe Logo",
            modifier = Modifier.size(240.dp), // Increased from 160dp to 240dp
            contentScale = ContentScale.Fit
        )
    }
}
