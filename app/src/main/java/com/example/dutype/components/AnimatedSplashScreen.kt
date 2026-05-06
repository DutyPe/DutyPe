package com.example.dutype.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dutype.app.R
import kotlinx.coroutines.delay

/**
 * Single visible Compose splash screen.
 * Shows the app icon with DutyPe text on the brand-blue background.
 */
@Composable
fun AnimatedSplashScreen(
    onAnimationEnd: () -> Unit
) {
    val splashBlue = colorResource(id = R.color.splash_blue)
    val logoProgress = remember { Animatable(0.92f) }
    var visible by remember { mutableStateOf(true) }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatus = controller?.isAppearanceLightStatusBars
        val previousLightNav = controller?.isAppearanceLightNavigationBars
        val previousStatusColor = window?.statusBarColor
        val previousNavColor = window?.navigationBarColor
        window?.statusBarColor = Color.White.toArgb()
        window?.navigationBarColor = splashBlue.toArgb()
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            previousStatusColor?.let { window.statusBarColor = it }
            previousNavColor?.let { window.navigationBarColor = it }
            previousLightStatus?.let { controller.isAppearanceLightStatusBars = it }
            previousLightNav?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }

    LaunchedEffect(Unit) {
        logoProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
        )
        delay(260L)
        visible = false
        delay(100L)
        onAnimationEnd()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(0)),
        exit = fadeOut(animationSpec = tween(durationMillis = 100, easing = LinearOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(splashBlue),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .alpha(logoProgress.value)
                    .graphicsLayer {
                        scaleX = logoProgress.value
                        scaleY = logoProgress.value
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp)
                )
                Text(
                    text = "DutyPe",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
