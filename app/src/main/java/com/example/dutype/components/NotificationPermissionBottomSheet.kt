package com.example.dutype.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.utils.LocaleHelper

private val SheetBlue = Color(0xFF0077F0)
private val MintBackdrop = Color(0xFFD6F5E5)
private val PhoneBorder = Color(0xFF1E293B)
private val BellOrange = Color(0xFFFF9500)
private val SwitchGreen = Color(0xFF22C55E)
private val ArrowCyan = Color(0xFF38BDF8)
private val BannerBlue = Color(0xFF3B82F6)
private val CheckGreen = Color(0xFF16A34A)
private val CloseDark = Color(0xFF383B40)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF475569)

private val EaseOutCascade = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * 3D Basic Position Z Cascade Item
 * Replicates the After Effects 3D Z Cascade text animator preset:
 * - Text cascades along the Z axis (scale 0.78f -> 1.0f)
 * - 3D Perspective tilt (rotationX 32f -> 0f with 14f * density cameraDistance)
 * - Vertical glide (translationY 22f -> 0f)
 * - Opacity fade (alpha 0f -> 1f)
 * - Snappy, energetic cascade timing (~200ms per element, staggered by 40ms)
 */
@Composable
private fun Cascade3DItem(
    animationStarted: Boolean,
    delayMillis: Int,
    durationMillis: Int = 210,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = (durationMillis * 0.8f).toInt(), delayMillis = delayMillis, easing = EaseOutCascade),
        label = "cascadeAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.78f,
        animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = EaseOutCascade),
        label = "cascadeScale"
    )
    val rotationX by animateFloatAsState(
        targetValue = if (animationStarted) 0f else 32f,
        animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = EaseOutCascade),
        label = "cascadeRotX"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (animationStarted) 0f else 22f,
        animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = EaseOutCascade),
        label = "cascadeOffsetY"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
            this.rotationX = rotationX
            this.translationY = offsetY
            this.cameraDistance = 14f * density
        }
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onEnableNotifications: () -> Unit,
    userRole: String = "worker",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU

    val title = if (isTelugu) "పుష్ నోటిఫికేషన్‌లను ప్రారంభించండి" else "Enable Push Notification"
    val bullet1 = if (isTelugu) {
        "చెల్లింపుల ధృవీకరణ మరియు అలర్ట్‌ల కోసం"
    } else {
        "For uninterrupted speech confirmation for payments"
    }
    val bullet2 = if (isTelugu) {
        "సెటిల్‌మెంట్ నోటిఫికేషన్ల కోసం"
    } else {
        "For settlement notifications"
    }
    val bullet3 = if (isTelugu) {
        "రాబోయే ఆఫర్‌లు మరియు ఫీచర్ల సమాచారం కోసం"
    } else {
        "To be notified of upcoming offers and features"
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Snappy After Effects 3D Z Cascade entrance state
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            kotlinx.coroutines.delay(25)
            animationStarted = true
        } else {
            animationStarted = false
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier,
            dragHandle = null,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Right Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CloseDark)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Central Phone + Bell + Switch Illustration with 3D entrance
                Cascade3DItem(
                    animationStarted = animationStarted,
                    delayMillis = 0,
                    durationMillis = 190
                ) {
                    NotificationPhoneIllustration()
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Heading with After Effects 3D Z Cascade
                Cascade3DItem(
                    animationStarted = animationStarted,
                    delayMillis = 35,
                    durationMillis = 210
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Benefits List with After Effects 3D Z Cascade
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Cascade3DItem(
                        animationStarted = animationStarted,
                        delayMillis = 75,
                        durationMillis = 210,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BenefitRow(text = bullet1)
                    }
                    Cascade3DItem(
                        animationStarted = animationStarted,
                        delayMillis = 115,
                        durationMillis = 210,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BenefitRow(text = bullet2)
                    }
                    Cascade3DItem(
                        animationStarted = animationStarted,
                        delayMillis = 155,
                        durationMillis = 210,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BenefitRow(text = bullet3)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Full-width pill "OK" Button with After Effects 3D Z Cascade
                Cascade3DItem(
                    animationStarted = animationStarted,
                    delayMillis = 195,
                    durationMillis = 210,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            onEnableNotifications()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F0F0F),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "OK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BenefitRow(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = CheckGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Normal,
            color = TextMuted,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun NotificationPhoneIllustration() {
    Box(
        modifier = Modifier
            .size(width = 230.dp, height = 126.dp)
            .clip(RoundedCornerShape(63.dp))
            .background(MintBackdrop),
        contentAlignment = Alignment.Center
    ) {
        // Looping Dashed Arrow from Bell to Toggle Switch
        Canvas(modifier = Modifier.fillMaxSize()) {
            val arrowPath = Path().apply {
                moveTo(size.width * 0.38f, size.height * 0.32f)
                cubicTo(
                    size.width * 0.40f, size.height * 0.12f,
                    size.width * 0.72f, size.height * 0.12f,
                    size.width * 0.68f, size.height * 0.42f
                )
                cubicTo(
                    size.width * 0.65f, size.height * 0.60f,
                    size.width * 0.54f, size.height * 0.52f,
                    size.width * 0.56f, size.height * 0.45f
                )
            }
            drawPath(
                path = arrowPath,
                color = ArrowCyan,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            )
        }

        // Phone Frame
        Box(
            modifier = Modifier
                .size(width = 74.dp, height = 98.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(2.8.dp, PhoneBorder, RoundedCornerShape(14.dp))
                .background(Color.White)
        ) {
            // Speaker / Camera notch
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp)
                    .size(width = 18.dp, height = 3.dp)
                    .background(PhoneBorder, RoundedCornerShape(2.dp))
            )

            // Blue Notification Banner Pill
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(16.dp)
                    .background(BannerBlue, RoundedCornerShape(4.dp))
            ) {
                // White placeholder inside banner
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .size(width = 22.dp, height = 4.dp)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                )
            }
        }

        // Orange Notification Bell (tilted top-left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 58.dp, y = 14.dp)
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                // Sound wave arcs
                drawArc(
                    color = BellOrange.copy(alpha = 0.7f),
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(-4f, -4f),
                    size = androidx.compose.ui.geometry.Size(24f, 24f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = BellOrange,
                modifier = Modifier
                    .size(38.dp)
                    .rotate(-18f)
            )
        }

        // Green Toggle Switch (ON state) on right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-46).dp, y = 34.dp)
                .size(width = 34.dp, height = 18.dp)
                .background(SwitchGreen, RoundedCornerShape(9.dp))
                .padding(2.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            // White knob thumb
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/**
 * Helper function to open app settings for notification permissions
 */
fun openNotificationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            context.startActivity(intent)
        } catch (e2: Exception) {
            android.widget.Toast.makeText(
                context,
                "Please enable notifications in your device settings",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
}
