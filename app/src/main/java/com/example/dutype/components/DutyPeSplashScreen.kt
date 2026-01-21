package com.example.dutype.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.ui.theme.MeeshoFontFamily
import kotlinx.coroutines.delay

/**
 * DutyPe Splash Screen - Black background with logo and text
 * Shows logo icon in white rounded square with "DutyPe" text on the right
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Main content - Logo image centered (increased size)
        Image(
            painter = painterResource(id = com.dutype.app.R.drawable.dutypenewlogo),
            contentDescription = "DutyPe Logo",
            modifier = Modifier.size(280.dp)
        )

        // Bottom branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Made with love in India 💙",
                style = TextStyle(
                    fontFamily = MeeshoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}
