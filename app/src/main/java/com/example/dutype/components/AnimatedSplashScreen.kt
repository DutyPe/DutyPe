package com.example.dutype.components

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dutype.app.R
import kotlinx.coroutines.delay

/**
 * Static splash overlay shown on top of the app on cold start.
 * Shows the app icon with "DutyPe" to its right on a blue background.
 */
@Composable
fun AnimatedSplashScreen(
    onAnimationEnd: () -> Unit
) {
    val splashBlue = Color(0xFF0066FF)

    // Keep system bars readable while the blue splash is visible.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatus = controller?.isAppearanceLightStatusBars
        val previousLightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            previousLightStatus?.let { controller.isAppearanceLightStatusBars = it }
            previousLightNav?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }

    LaunchedEffect(Unit) {
        delay(900L)
        onAnimationEnd()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBlue),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(88.dp)
            )
            Text(
                text = "DutyPe",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
