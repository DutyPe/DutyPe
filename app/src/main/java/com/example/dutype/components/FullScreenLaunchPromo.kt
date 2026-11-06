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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
}

/**
 * Blinkit/BigBasket/Zepto-style full-screen launch event/deal promo on app open.
 * Takes over the screen immediately when `launchPromoEnabled` is true in dynamic configuration.
 */
@Composable
fun FullScreenLaunchPromo(
    dynamicConfig: DynamicFeaturesConfig,
    navController: NavHostController
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }

    val hasMedia = dynamicConfig.launchPromoBannerUrl.isNotBlank() || dynamicConfig.launchPromoAnimationUrl.isNotBlank()
    val isAnimation = dynamicConfig.launchPromoMediaType.equals("ANIMATION", ignoreCase = true) &&
        dynamicConfig.launchPromoAnimationUrl.isNotBlank()

    LaunchedEffect(dynamicConfig.launchPromoEnabled, hasMedia) {
        if (dynamicConfig.launchPromoEnabled && hasMedia && !FullScreenLaunchPromoTracker.hasShownThisSession) {
            FullScreenLaunchPromoTracker.hasShownThisSession = true
            isVisible = true

            // Countdown timer: 3, 2, 1 -> dismiss
            while (countdown > 0) {
                delay(1000L)
                countdown -= 1
            }
            delay(300L)
            isVisible = false
        }
    }

    if (!isVisible) return

    val dismissPromo = {
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
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(250))
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
            // Full Screen Image / Animation
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
                    contentDescription = dynamicConfig.launchPromoTitle.ifBlank { "Special Launch Event" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top-right floating "Skip in 3s" pill button (Zepto/Blinkit style)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { dismissPromo() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (countdown > 0) "Skip in ${countdown}s" else "✕ Skip",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
                "services", "service" -> Routes.SERVICES
                "refer", "refer_earn", "referral" -> Routes.WORKER_REFER_EARN
                "earnings", "wallet" -> Routes.WORKER_EARNINGS
                "jobs", "browse_jobs" -> Routes.WORKER_ALL_JOBS
                "post_job", "post-job" -> Routes.EMPLOYER_POST_JOB
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
