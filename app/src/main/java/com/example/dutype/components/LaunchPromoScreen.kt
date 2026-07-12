package com.example.dutype.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

/**
 * Full-screen launch promo overlay shown on app start when backend enables it.
 *
 * Smart timer: The [minDisplayMillis] countdown only begins AFTER the media
 * finishes loading, so the user always sees the banner for the full intended
 * duration regardless of network speed.
 *
 * @param displayMillis  Hard-cap — dismissed after this ms even if media never loads.
 * @param minDisplayMillis  Minimum visible time AFTER media loads.
 */
@Composable
fun LaunchPromoScreen(
    mediaType: String,
    bannerUrl: String,
    animationUrl: String,
    onAnimationEnd: () -> Unit,
    displayMillis: Long = 6000L,
    minDisplayMillis: Long = 3000L
) {
    val view = LocalView.current
    val backgroundColor = Color.Black
    val systemBarColor = Color.Black
    val isSystemBarDark = true

    // Tracks whether the media content has finished loading
    var mediaLoaded by remember { mutableStateOf(false) }

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

    // Hard-cap: always dismiss after displayMillis even if media never loads.
    LaunchedEffect(mediaType, bannerUrl, animationUrl, displayMillis) {
        delay(displayMillis)
        onAnimationEnd()
    }

    // Smart timer: also dismiss minDisplayMillis after media finishes loading,
    // whichever comes first with the hard-cap above.
    LaunchedEffect(mediaLoaded) {
        if (mediaLoaded) {
            delay(minDisplayMillis)
            onAnimationEnd()
        }
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

            // Signal loaded when Lottie composition is ready
            LaunchedEffect(composition) {
                if (composition != null && !mediaLoaded) mediaLoaded = true
            }

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
                crossfadeMillis = 0,
                onImageLoaded = { if (!mediaLoaded) mediaLoaded = true }
            )
        }
    }
}