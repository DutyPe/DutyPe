package com.example.partimes.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import com.example.partimes.R

@Composable
fun SplashScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }
    var hideAnimation by remember { mutableStateOf(false) } // Track exit animation

    // Alpha animation (Fade In & Out)
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1.1f else 0.8f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    // **Exit Animations** (Move Left & Right)
    val textLeftAnim by animateFloatAsState(
        targetValue = if (hideAnimation) -300f else 0f, // Move Left
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
    )

    val textRightAnim by animateFloatAsState(
        targetValue = if (hideAnimation) 600f else 0f, // Move further right
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    val imageAlphaAnim by animateFloatAsState(
        targetValue = if (hideAnimation) 0f else 1f, // Fade Out Image
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing) // Faster
    )

    // Start animations
    LaunchedEffect(true) {
        startAnimation = true
        delay(1000) // Show splash for 1 second
        hideAnimation = true // Start exit animation
        delay(500) // Wait for exit animation
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true } // Remove splash from backstack
        }
    }

    // 🔥 UI Content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.parttimes),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(scaleAnim)
                    .alpha(imageAlphaAnim) // Fade out image
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Move Text Left
            Text(
                text = "Local Jobs",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(x = textLeftAnim.dp)
                    .alpha(alphaAnim)
            )

            // Move Text Right
            Text(
                text = "Find flexible jobs that fit your schedule!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(x = textRightAnim.dp)
                    .alpha(alphaAnim)
            )
        }
    }
}
