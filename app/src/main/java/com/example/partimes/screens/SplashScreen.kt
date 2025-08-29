package com.example.partimes.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import com.example.partimes.R

@Composable
fun SplashScreen(navController: NavController) {
    var startAnimation by remember { mutableStateOf(false) }
    var hideAnimation by remember { mutableStateOf(false) }

    // Enhanced animations
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0.8f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
    )

    val rotationAnim by animateFloatAsState(
        targetValue = if (startAnimation) 360f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
    )

    // Exit animations
    val textLeftAnim by animateFloatAsState(
        targetValue = if (hideAnimation) -300f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
    )

    val textRightAnim by animateFloatAsState(
        targetValue = if (hideAnimation) 600f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    val imageAlphaAnim by animateFloatAsState(
        targetValue = if (hideAnimation) 0f else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )

    // Gradient background
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    // Start animations
    LaunchedEffect(true) {
        startAnimation = true
        delay(2000)
        hideAnimation = true
        delay(500)
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with enhanced animations
            Image(
                painter = painterResource(id = R.drawable.parttimes),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(150.dp)
                    .scale(scaleAnim)
                    .alpha(imageAlphaAnim)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated text with modern typography
            Text(
                text = "ParTimes",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(x = textLeftAnim.dp)
                    .alpha(alphaAnim)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find Local Jobs",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(x = textRightAnim.dp)
                    .alpha(alphaAnim)
            )
        }
    }
}
