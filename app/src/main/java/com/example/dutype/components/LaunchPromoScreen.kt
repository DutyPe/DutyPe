package com.example.dutype.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay

@Composable
fun LaunchPromoScreen(
    mediaType: String,
    bannerUrl: String,
    animationUrl: String,
    backgroundColorHex: String,
    statusBarColorHex: String,
    onAnimationEnd: () -> Unit,
    displayMillis: Long = 4000L
) {
    val view = LocalView.current
    val backgroundColor = remember(backgroundColorHex) { parseColorOrDefault(backgroundColorHex, Color.White) }
    val systemBarColor = remember(statusBarColorHex) { parseColorOrDefault(statusBarColorHex, backgroundColor) }
    val isSystemBarDark = remember(systemBarColor) { systemBarColor.isDarkColor() }

    DisposableEffect(systemBarColor) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatusBarColor = window?.statusBarColor
        val previousNavigationBarColor = window?.navigationBarColor
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller?.isAppearanceLightNavigationBars

        window?.statusBarColor = systemBarColor.toArgb()
        window?.navigationBarColor = systemBarColor.toArgb()
        controller?.isAppearanceLightStatusBars = !isSystemBarDark
        controller?.isAppearanceLightNavigationBars = !isSystemBarDark

        onDispose {
            if (window != null) {
                previousStatusBarColor?.let { window.statusBarColor = it }
                previousNavigationBarColor?.let { window.navigationBarColor = it }
            }
            if (controller != null) {
                previousLightStatusBars?.let { controller.isAppearanceLightStatusBars = it }
                previousLightNavigationBars?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }

    LaunchedEffect(mediaType, bannerUrl, animationUrl, displayMillis) {
        delay(displayMillis)
        onAnimationEnd()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        val normalizedMediaType = mediaType.trim().uppercase()
        val isAnimation = normalizedMediaType == "ANIMATION" && animationUrl.isNotBlank()

        if (isAnimation) {
            val compositionResult = rememberLottieComposition(LottieCompositionSpec.Url(animationUrl))
            val composition = compositionResult.value
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )

            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                OptimizedImage(
                    imageUrl = bannerUrl,
                    contentDescription = "Launch promotional banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderColor = backgroundColor,
                    showLoadingIndicator = false,
                    crossfadeMillis = 0
                )
            }
        } else {
            OptimizedImage(
                imageUrl = bannerUrl,
                contentDescription = "Launch promotional banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholderColor = backgroundColor,
                showLoadingIndicator = false,
                crossfadeMillis = 0
            )
        }
    }
}

private fun parseColorOrDefault(colorString: String, fallback: Color): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(colorString))
    }.getOrElse { fallback }
}

private fun Color.isDarkColor(): Boolean {
    val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
    return luminance < 0.5
}