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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.dutype.navigation.Routes
import com.example.dutype.repositories.DynamicFeaturesConfig
import kotlinx.coroutines.delay

/**
 * Blinkit/Swiggy/Zepto-style vertical poster launch promotion dialog.
 * Appears once per app session when `launchPromoEnabled` is true in dynamic config.
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

    // Only allow showing on primary landing screens (worker home, employer home, or services)
    val isEligibleScreen = currentRoute in listOf(
        Routes.WORKER_HOME,
        Routes.EMPLOYER_HOME,
        Routes.SERVICES
    )

    LaunchedEffect(dynamicConfig.launchPromoEnabled, dynamicConfig.launchPromoBannerUrl, isEligibleScreen) {
        if (dynamicConfig.launchPromoEnabled &&
            dynamicConfig.launchPromoBannerUrl.isNotBlank() &&
            isEligibleScreen &&
            !DynamicLaunchPromoSessionTracker.hasDismissedThisSession
        ) {
            // Short delay after screen transitions so the home screen settles smoothly
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
        val route = dynamicConfig.launchPromoActionRoute.trim()
        if (route.isNotBlank()) {
            handleNavigationTarget(context, navController, route)
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
                .background(Color.Black.copy(alpha = 0.68f))
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
                    animationSpec = tween(durationMillis = 320)
                ) + fadeIn(animationSpec = tween(280)),
                exit = scaleOut(
                    targetScale = 0.90f,
                    animationSpec = tween(durationMillis = 200)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                // Vertical Poster Card with Floating 'X' Close Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .widthIn(max = 340.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onActionClick
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top-right Close Button above card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        IconButton(
                            onClick = dismissPromo,
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(4.dp, CircleShape)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close promo",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Poster Card (WebP image)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White)
                    ) {
                        Column {
                            // Vertical Aspect Ratio Poster Image
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(dynamicConfig.launchPromoBannerUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = dynamicConfig.launchPromoTitle.ifBlank { "Special Offer" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                            )

                            // Optional Title / CTA Bar below image if configured
                            if (dynamicConfig.launchPromoTitle.isNotBlank() || dynamicConfig.launchPromoActionRoute.isNotBlank()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (dynamicConfig.launchPromoTitle.isNotBlank()) {
                                        Text(
                                            text = dynamicConfig.launchPromoTitle,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    Button(
                                        onClick = onActionClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0F172A),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = "Check Now",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleNavigationTarget(context: Context, navController: NavHostController, target: String) {
    if (target.isBlank()) return
    try {
        if (target.startsWith("http://") || target.startsWith("https://") || target.startsWith("dutype://")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
            context.startActivity(intent)
        } else {
            // Map common shorthand routes
            val resolvedRoute = when (target.lowercase().trim()) {
                "services", "service" -> Routes.SERVICES
                "refer", "refer_earn", "referral" -> Routes.WORKER_REFER_EARN
                "earnings", "wallet" -> Routes.WORKER_EARNINGS
                "jobs", "browse_jobs" -> Routes.WORKER_ALL_JOBS
                "post_job" -> Routes.EMPLOYER_POST_JOB
                else -> target
            }
            navController.navigate(resolvedRoute)
        }
    } catch (_: Exception) {
        // Fallback silently if route is invalid
    }
}
