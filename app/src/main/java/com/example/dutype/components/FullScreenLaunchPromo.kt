package com.example.dutype.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.dutype.navigation.Routes
import com.example.dutype.repositories.DynamicFeaturesConfig
import kotlinx.coroutines.delay

object FullScreenLaunchPromoTracker {
    var hasShownThisSession: Boolean = false
    var isCurrentlyActive: Boolean = false
}

/**
 * Blinkit/BigBasket/Zepto-style full-screen launch event/deal promo on app open.
 * Takes over the screen immediately when `launchPromoEnabled` is true in dynamic configuration.
 *
 * Supports configurable skip duration (3 to 10 seconds), optional floating CTA button,
 * and exact edge-to-edge full screen display.
 */
@Composable
fun FullScreenLaunchPromo(
    dynamicConfig: DynamicFeaturesConfig,
    navController: NavHostController
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    val initialDuration = dynamicConfig.launchPromoDurationSec.coerceIn(3, 10)
    var countdown by remember { mutableIntStateOf(initialDuration) }

    val hasMedia = dynamicConfig.launchPromoBannerUrl.isNotBlank() || dynamicConfig.launchPromoAnimationUrl.isNotBlank()
    val isAnimation = dynamicConfig.launchPromoMediaType.equals("ANIMATION", ignoreCase = true) &&
        dynamicConfig.launchPromoAnimationUrl.isNotBlank()

    LaunchedEffect(dynamicConfig.launchPromoEnabled, hasMedia) {
        if (dynamicConfig.launchPromoEnabled && hasMedia && !FullScreenLaunchPromoTracker.hasShownThisSession) {
            FullScreenLaunchPromoTracker.hasShownThisSession = true
            FullScreenLaunchPromoTracker.isCurrentlyActive = true
            countdown = dynamicConfig.launchPromoDurationSec.coerceIn(3, 10)
            isVisible = true

            // Countdown timer: 5s, 4s, 3s, 2s, 1s -> auto dismiss like splash screen
            while (countdown > 0) {
                delay(1000L)
                countdown -= 1
            }
            delay(250L)
            isVisible = false
            FullScreenLaunchPromoTracker.isCurrentlyActive = false
        }
    }

    if (!isVisible) return

    val dismissPromo = {
        FullScreenLaunchPromoTracker.isCurrentlyActive = false
        isVisible = false
    }

    val onPromoTap = {
        dismissPromo()
        val route = dynamicConfig.launchPromoActionRoute.trim()
        if (route.isNotBlank()) {
            handlePromoNavigation(context, navController, route)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(220))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPromoTap
                )
        ) {
            // Full Screen Image / Animation (Edge-to-edge exact screen fill)
            if (isAnimation) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.Url(dynamicConfig.launchPromoAnimationUrl.trim())
                )
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(dynamicConfig.launchPromoBannerUrl.ifBlank { dynamicConfig.launchPromoAnimationUrl })
                        .crossfade(true)
                        .build(),
                    contentDescription = "Launch Promo Banner",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Optional Floating CTA Button at bottom (only shown if configured)
            if (dynamicConfig.launchPromoButtonText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(bottom = 36.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = onPromoTap,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B00),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .height(52.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text(
                            text = dynamicConfig.launchPromoButtonText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Launch banner behaves like splash screen without a skip button
        }
    }
}

private fun handlePromoNavigation(context: Context, navController: NavHostController, target: String) {
    if (target.isBlank()) return
    try {
        if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("dutype://")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
            context.startActivity(intent)
        } else {
            val resolvedRoute = when (target.lowercase().trim()) {
                "dynamic_campaign", "campaign", "dynamic_screen", "promo_screen", "ad" -> Routes.promoShowcaseRoute("active_campaign")
                "services", "service" -> Routes.SERVICES
                "refer", "refer_earn", "referral" -> Routes.WORKER_REFER_EARN
                "earnings", "wallet" -> Routes.WORKER_EARNINGS
                "jobs", "browse_jobs" -> Routes.WORKER_ALL_JOBS
                "post_job", "post-job" -> Routes.EMPLOYER_POST_JOB
                "partner", "partner-apply" -> Routes.PARTNER
                "hungr", "hungr-survey" -> {
                    com.example.dutype.announcements.AnnouncementManager.openHungrSurvey()
                    return
                }
                else -> target
            }
            navController.navigate(resolvedRoute)
        }
    } catch (_: Exception) {
        // Fallback silently if route is invalid
    }
}
