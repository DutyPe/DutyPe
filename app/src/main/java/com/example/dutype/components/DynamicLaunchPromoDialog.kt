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

    // Check if in-app dialog is enabled
    val isEnabled = dynamicConfig.inAppDialogEnabled ||
        (!dynamicConfig.launchPromoEnabled && dynamicConfig.launchPromoBannerUrl.isNotBlank())

    val imageUrl = dynamicConfig.inAppDialogImageUrl.ifBlank { dynamicConfig.launchPromoBannerUrl }
    val title = dynamicConfig.inAppDialogTitle.ifBlank { dynamicConfig.launchPromoTitle }
    val actionRoute = dynamicConfig.inAppDialogActionRoute.ifBlank { dynamicConfig.launchPromoActionRoute }.trim()
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
        if (actionRoute.isNotBlank()) {
            handleInAppNavigation(context, navController, actionRoute)
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

                    // Poster Card (Exact image fit, no edges cut off!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Image container with ContentScale.Fit so entire flyer/poster is preserved
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 460.dp)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = title.ifBlank { "Announcement Flyer" },
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 460.dp)
                                )
                            }

                            // Optional Title or CTA Bar (Rendered ONLY if title or route is present)
                            if (title.isNotBlank() || actionRoute.isNotBlank()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (title.isNotBlank()) {
                                        Text(
                                            text = title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            textAlign = TextAlign.Center
                                        )
                                        if (actionRoute.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }

                                    // Action button rendered only when route path is NOT empty
                                    if (actionRoute.isNotBlank()) {
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
                                                text = if (actionRoute.contains("hungr", ignoreCase = true)) "Join VIP Waitlist" else "Check Now",
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
