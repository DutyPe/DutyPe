package com.example.dutype.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.material.icons.filled.PlayArrow
import com.example.dutype.navigation.Routes
import androidx.compose.ui.graphics.Brush
import com.example.dutype.repositories.DynamicFeaturesConfig
import kotlinx.coroutines.delay

/**
 * In-App Targeted Dialog Banner for emergency alerts, priority redirection,
 * special promotions, shop ads, or referral campaigns.
 *
 * Supports role-based target audience selection (Worker, Employer, Services, or All).
 * The image is rendered with ContentScale.Fit so nothing is cut or cropped.
 * If the action route is left empty, the dialog is purely informational with no CTA button.
 */
object DynamicLaunchPromoSessionTracker {
    var hasDismissedThisSession: Boolean = false
}

@Composable
fun DynamicLaunchPromoDialog(
    dynamicConfig: DynamicFeaturesConfig,
    navController: NavHostController,
    currentRoute: String?
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    // Check if in-app dialog is enabled independently
    val isEnabled = dynamicConfig.inAppDialogEnabled && dynamicConfig.inAppDialogImageUrl.isNotBlank()
    val imageUrl = dynamicConfig.inAppDialogImageUrl.trim()
    val actionRoute = dynamicConfig.inAppDialogActionRoute.trim()
    val targetAudience = dynamicConfig.inAppDialogTargetAudience.trim().uppercase()

    // Determine screen eligibility based on target audience
    val isEligibleScreen = when (targetAudience) {
        "WORKER" -> currentRoute == Routes.WORKER_HOME
        "EMPLOYER" -> currentRoute == Routes.EMPLOYER_HOME
        "SERVICES" -> currentRoute == Routes.SERVICES
        else -> currentRoute in listOf(Routes.WORKER_HOME, Routes.EMPLOYER_HOME, Routes.SERVICES)
    }

    LaunchedEffect(isEnabled, imageUrl, isEligibleScreen) {
        if (isEnabled &&
            imageUrl.isNotBlank() &&
            isEligibleScreen &&
            !DynamicLaunchPromoSessionTracker.hasDismissedThisSession
        ) {
            delay(650)
            if (!DynamicLaunchPromoSessionTracker.hasDismissedThisSession) {
                isVisible = true
            }
        }
    }

    if (!isVisible) return

    val dismissPromo = {
        DynamicLaunchPromoSessionTracker.hasDismissedThisSession = true
        isVisible = false
    }

    val onActionClick = {
        dismissPromo()
        val targetToOpen = when {
            actionRoute.isNotBlank() -> actionRoute
            imageUrl.contains("youtube.com", ignoreCase = true) ||
                imageUrl.contains("youtu.be", ignoreCase = true) ||
                imageUrl.contains("instagram.com", ignoreCase = true) -> imageUrl
            else -> ""
        }
        if (targetToOpen.isNotBlank()) {
            handleInAppNavigation(context, navController, targetToOpen)
        }
    }

    Dialog(
        onDismissRequest = dismissPromo,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BackHandler { dismissPromo() }

        // Dimmed backdrop scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismissPromo
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(250)),
                exit = scaleOut(
                    targetScale = 0.90f,
                    animationSpec = tween(durationMillis = 200)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = 350.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onActionClick
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top-right Close Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        IconButton(
                            onClick = dismissPromo,
                            modifier = Modifier
                                .size(34.dp)
                                .shadow(6.dp, CircleShape)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Media Card (Supports Lottie Animations, YouTube Videos, Instagram Reels & Image Flyers)
                    val isLottie = imageUrl.contains(".json", ignoreCase = true) || imageUrl.contains("lottie", ignoreCase = true)
                    val isYouTube = imageUrl.contains("youtube.com", ignoreCase = true) || imageUrl.contains("youtu.be", ignoreCase = true)
                    val isInstagram = imageUrl.contains("instagram.com", ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(20.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isLottie) Color.White else Color.Black)
                    ) {
                        when {
                            isLottie -> {
                                val composition by rememberLottieComposition(LottieCompositionSpec.Url(imageUrl))
                                val progress by animateLottieCompositionAsState(
                                    composition = composition,
                                    iterations = LottieConstants.IterateForever
                                )
                                LottieAnimation(
                                    composition = composition,
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 240.dp, max = 380.dp)
                                        .padding(12.dp)
                                )
                            }
                            isYouTube -> {
                                val videoId = when {
                                    imageUrl.contains("v=") -> imageUrl.substringAfter("v=").substringBefore("&")
                                    imageUrl.contains("youtu.be/") -> imageUrl.substringAfter("youtu.be/").substringBefore("?")
                                    imageUrl.contains("embed/") -> imageUrl.substringAfter("embed/").substringBefore("?")
                                    imageUrl.contains("shorts/") -> imageUrl.substringAfter("shorts/").substringBefore("?")
                                    else -> ""
                                }
                                val thumbUrl = if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else imageUrl

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp, max = 320.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "YouTube Video Promo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Play button overlay
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF0000).copy(alpha = 0.92f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Play Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                            isInstagram -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 220.dp, max = 340.dp)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                                            )
                                        )
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "📸 DutyPe on Instagram",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "Tap to watch our latest reel & video updates",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color.White
                                        ) {
                                            Text(
                                                text = "Watch on Instagram ▶",
                                                color = Color(0xFF833AB4),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Announcement Flyer",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .clip(RoundedCornerShape(20.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleInAppNavigation(context: Context, navController: NavHostController, target: String) {
    if (target.isBlank()) return
    try {
        if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("dutype://")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
            context.startActivity(intent)
        } else {
            val resolvedRoute = when (target.lowercase().trim()) {
                "dynamic_campaign", "campaign", "dynamic_screen", "promo_screen", "ad" -> Routes.promoShowcaseRoute("active_campaign")
                "spin", "spin_win", "spin_and_win", "game" -> Routes.SPIN_AND_WIN
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
