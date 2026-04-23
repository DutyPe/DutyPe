package com.example.dutype.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash overlay shown on top of the app on cold start.
 *
 * Design:
 *  - Pure black background (matches the system splash so there is no flash
 *    between the system splash and this Compose splash).
 *  - "DutyPe" rendered letter-by-letter in white. Each letter fades in and
 *    slides up with a small stagger — a clean, premium fade-up reveal
 *    similar to Linear / Notion / Vercel.
 *  - System status-bar icons (battery, network, time) are forced to light /
 *    white while the splash is visible so they remain readable on the black
 *    background, then reverted on dispose.
 *  - The whole splash fades out after a short hold so the underlying app
 *    appears beneath it.
 */
@Composable
fun AnimatedSplashScreen(
    onAnimationEnd: () -> Unit
) {
    val word = "DutyPe"
    val letterAnims = remember { List(word.length) { Animatable(0f) } }
    var visible by remember { mutableStateOf(true) }

    // Force light (white) status- and nav-bar icons while the black splash is
    // showing, then restore the app's normal dark-on-white setup on dispose.
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
        // Smooth premium reveal \u2014 slightly relaxed easing so it reads as
        // deliberate instead of "instant snap".
        val staggerMs = 55L
        val perLetterMs = 260
        letterAnims.forEachIndexed { index, anim ->
            launch {
                delay(index * staggerMs)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = perLetterMs,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
        // Wait for the FULL word to finish revealing before holding/fading.
        val fullRevealMs = (letterAnims.lastIndex * staggerMs) + perLetterMs
        delay(fullRevealMs)
        // Very brief hold so the user perceives the complete word, then fade.
        delay(350L)
        visible = false
        // Match the AnimatedVisibility fade-out below before signalling completion.
        delay(180L)
        onAnimationEnd()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(0)),
        exit = fadeOut(animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                word.forEachIndexed { index, char ->
                    val progress = letterAnims[index].value
                    Text(
                        text = char.toString(),
                        color = Color.White,
                        fontSize = 47.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .alpha(progress)
                            .graphicsLayer {
                                // Pure fade-up: characters slide up into place, no scale.
                                translationY = (1f - progress) * 24.dp.toPx()
                            }
                    )
                }
            }
        }
    }
}