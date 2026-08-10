package com.example.dutype.components

import com.dutype.app.R
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class WelcomeGiftVariant {
    COPY_FIRST,
    ILLUSTRATION_FIRST
}

@Composable
fun GuestWelcomeBonusCard(
    title: String,
    message: String,
    buttonText: String,
    onClick: () -> Unit,
    variant: WelcomeGiftVariant = WelcomeGiftVariant.COPY_FIRST,
    urgencyText: String = stringResource(R.string.guest_welcome_limited_benefit),
    trustText: String = stringResource(R.string.guest_welcome_new_account_trust),
    ctaSubtext: String = stringResource(R.string.guest_welcome_takes_30_seconds),
    onVariantImpression: (WelcomeGiftVariant) -> Unit = {},
    onVariantClick: (WelcomeGiftVariant) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var impressionTracked by remember(variant) { mutableStateOf(false) }

    LaunchedEffect(variant, impressionTracked) {
        if (!impressionTracked) {
            onVariantImpression(variant)
            impressionTracked = true
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onVariantClick(variant)
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, WorkerColors.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF3A3A40), Color(0xFF0F0F0F))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        style = AppTypography.cardTitle.copy(
                            fontWeight = FontWeight.Bold,
                            color = WorkerColors.TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = message,
                        style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = {
                    onVariantClick(variant)
                    onClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorkerColors.Primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = buttonText,
                    style = AppTypography.buttonLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RibbonBadge(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF7A35F4), Color(0xFF9F4EF7))
                )
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFE500),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OneTimeBadge(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFFF4FFFA).copy(alpha = 0.95f))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color(0xFF008253),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            color = Color(0xFF0F172A),
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SurpriseCopy(
    title: String,
    message: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = Color(0xFF071229),
            fontSize = 28.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = message,
            color = Color(0xFF14213D),
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MysteryPanel() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.76f))
            .border(1.dp, Color.White.copy(alpha = 0.82f), RoundedCornerShape(13.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF83E7A7)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = Color(0xFF7C2DEB),
                modifier = Modifier.size(28.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.guest_welcome_mystery_label),
                color = Color(0xFF071229),
                fontSize = 13.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.guest_welcome_secret_value),
                color = Color(0xFF07913B),
                fontSize = 21.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GiftRevealVisual(
    modifier: Modifier,
    bob: Float,
    scale: Float
) {
    Box(
        modifier = modifier.graphicsLayer {
            translationY = bob
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            drawOval(
                color = Color(0xFF26A874).copy(alpha = 0.32f),
                topLeft = Offset(canvasWidth * 0.12f, canvasHeight * 0.79f),
                size = Size(canvasWidth * 0.74f, canvasHeight * 0.15f)
            )
            drawCircle(
                color = Color(0xFFFFE66D).copy(alpha = 0.34f),
                radius = canvasWidth * 0.32f,
                center = Offset(canvasWidth * 0.47f, canvasHeight * 0.48f)
            )
            drawRoundRect(
                color = Color(0xFFF5B71E),
                topLeft = Offset(canvasWidth * 0.22f, canvasHeight * 0.48f),
                size = Size(canvasWidth * 0.52f, canvasHeight * 0.34f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            drawRoundRect(
                color = Color(0xFFFFD34D),
                topLeft = Offset(canvasWidth * 0.16f, canvasHeight * 0.39f),
                size = Size(canvasWidth * 0.64f, canvasHeight * 0.15f),
                cornerRadius = CornerRadius(13f, 13f)
            )
            drawRoundRect(
                color = Color(0xFF7C2DEB),
                topLeft = Offset(canvasWidth * 0.45f, canvasHeight * 0.39f),
                size = Size(canvasWidth * 0.12f, canvasHeight * 0.43f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = Color(0xFF9B4BFF),
                topLeft = Offset(canvasWidth * 0.16f, canvasHeight * 0.46f),
                size = Size(canvasWidth * 0.64f, canvasHeight * 0.08f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawOval(
                color = Color(0xFF9B4BFF),
                topLeft = Offset(canvasWidth * 0.32f, canvasHeight * 0.24f),
                size = Size(canvasWidth * 0.22f, canvasHeight * 0.19f)
            )
            drawOval(
                color = Color(0xFF7C2DEB),
                topLeft = Offset(canvasWidth * 0.50f, canvasHeight * 0.24f),
                size = Size(canvasWidth * 0.22f, canvasHeight * 0.19f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.34f),
                topLeft = Offset(canvasWidth * 0.24f, canvasHeight * 0.53f),
                size = Size(canvasWidth * 0.46f, canvasHeight * 0.05f),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }

        Text(
            text = "?",
            color = Color(0xFFFFF176),
            fontSize = 68.sp,
            lineHeight = 68.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 1.dp)
        )

        WowBadge(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp, top = 30.dp)
        )
    }
}

@Composable
private fun WowBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(Color(0xFF7C2DEB))
            .border(2.dp, Color.White, CircleShape)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.guest_welcome_inside_badge),
            color = Color.White,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrustFeatureBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(alpha = 0.92f), RoundedCornerShape(13.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrustFeature(
            icon = Icons.Default.FlashOn,
            title = stringResource(R.string.guest_welcome_instant_credit),
            subtitle = stringResource(R.string.guest_welcome_after_signup),
            iconBackground = Color(0xFFD9FBE7),
            iconTint = Color(0xFF029E57),
            modifier = Modifier.weight(1f)
        )
        FeatureDivider()
        TrustFeature(
            icon = Icons.Default.Security,
            title = stringResource(R.string.guest_welcome_safe_title),
            subtitle = stringResource(R.string.guest_welcome_safe_subtitle),
            iconBackground = Color(0xFFE3FBEA),
            iconTint = Color(0xFF179447),
            modifier = Modifier.weight(1f)
        )
        FeatureDivider()
        TrustFeature(
            icon = Icons.Default.Person,
            title = stringResource(R.string.guest_welcome_only_for),
            subtitle = stringResource(R.string.guest_welcome_new_users),
            iconBackground = Color(0xFFCFF7E5),
            iconTint = Color(0xFF17A873),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SlidingSurpriseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "surprise_cta_slide")
    val shineProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "surprise_cta_shine"
    )
    val arrowShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "surprise_cta_arrow"
    )
    val shape = RoundedCornerShape(14.dp)

    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFF071229)
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFFF171),
                            Color(0xFFFFDF1F),
                            Color(0xFFFFBD11)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val shineOffset = (-92).dp + (maxWidth + 184.dp) * shineProgress
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = shineOffset)
                    .width(74.dp)
                    .height(80.dp)
                    .graphicsLayer { rotationZ = -18f }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularIconBadge(
                    icon = Icons.Default.CardGiftcard,
                    backgroundColor = Color.White,
                    iconTint = Color(0xFF7C2DEB)
                )
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF071229),
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(modifier = Modifier.graphicsLayer { translationX = arrowShift }) {
                    CircularIconBadge(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        backgroundColor = Color(0xFF007B58),
                        iconTint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustFeature(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBackground: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(35.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(21.dp)
            )
        }
        Text(
            text = title,
            color = Color(0xFF071229),
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            color = Color(0xFF4B5563),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FeatureDivider() {
    Spacer(
        modifier = Modifier
            .height(44.dp)
            .width(1.dp)
            .background(Color(0xFFB7E4D5))
    )
}

@Composable
private fun CircularIconBadge(
    icon: ImageVector,
    backgroundColor: Color,
    iconTint: Color
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun LimitedOfferFooter(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF149759).copy(alpha = 0.65f))
        )
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFF087A3B),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            color = Color(0xFF087A3B),
            fontSize = 12.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFF087A3B),
            modifier = Modifier.size(13.dp)
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFF149759).copy(alpha = 0.65f))
        )
    }
}

@Composable
private fun SurprisePosterBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val center = Offset(canvasWidth * 0.69f, canvasHeight * 0.42f)

        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = canvasWidth * 0.31f,
            center = center
        )

        repeat(18) { index ->
            val angle = (index * 20.0) * PI / 180.0
            val start = Offset(
                center.x + cos(angle).toFloat() * canvasWidth * 0.08f,
                center.y + sin(angle).toFloat() * canvasWidth * 0.08f
            )
            val end = Offset(
                center.x + cos(angle).toFloat() * canvasWidth * 0.43f,
                center.y + sin(angle).toFloat() * canvasWidth * 0.43f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = start,
                end = end,
                strokeWidth = 5f
            )
        }

        val confetti = listOf(
            Triple(0.15f, 0.18f, Color(0xFF06B64F)),
            Triple(0.52f, 0.12f, Color(0xFFFFC928)),
            Triple(0.65f, 0.17f, Color(0xFF7C2DEB)),
            Triple(0.82f, 0.23f, Color(0xFF00AEEF)),
            Triple(0.91f, 0.33f, Color(0xFFFF3DAF)),
            Triple(0.61f, 0.58f, Color(0xFFFFC928)),
            Triple(0.88f, 0.58f, Color(0xFF7C2DEB)),
            Triple(0.34f, 0.35f, Color(0xFF0AAF44))
        )

        confetti.forEachIndexed { index, item ->
            val (xRatio, yRatio, color) = item
            if (index % 2 == 0) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(canvasWidth * xRatio, canvasHeight * yRatio),
                    size = Size(11f, 17f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            } else {
                drawCircle(
                    color = color,
                    radius = 6f,
                    center = Offset(canvasWidth * xRatio, canvasHeight * yRatio)
                )
            }
        }
    }
}