package com.example.dutype.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dutype.app.R

enum class WelcomeGiftVariant {
    AMOUNT_FIRST,
    ILLUSTRATION_FIRST
}

@Composable
fun GuestWelcomeBonusCard(
    title: String,
    message: String,
    buttonText: String,
    onClick: () -> Unit,
    rewardAmountText: String = extractRewardAmount(message),
    variant: WelcomeGiftVariant = if ((title + message).hashCode() % 2 == 0) {
        WelcomeGiftVariant.AMOUNT_FIRST
    } else {
        WelcomeGiftVariant.ILLUSTRATION_FIRST
    },
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

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFEDD5), Color(0xFFFDE68A), Color(0xFFFFF7ED))
    )

    // Bouncing gift animation
    val infiniteTransition = rememberInfiniteTransition(label = "gift")
    val giftBob by infiniteTransition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "giftBob"
    )
    val giftScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "giftScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 240.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Animated bouncing gift icon
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                translationY = giftBob
                                scaleX = giftScale
                                scaleY = giftScale
                            }
                    )
                    Column {
                        Text(
                            text = urgencyText,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (variant == WelcomeGiftVariant.AMOUNT_FIRST) {
                    RewardFirstLayout(
                        title = title,
                        message = message,
                        rewardAmountText = rewardAmountText,
                        trustText = trustText
                    )
                } else {
                    IllustrationFirstLayout(
                        title = title,
                        message = message,
                        rewardAmountText = rewardAmountText,
                        trustText = trustText
                    )
                }

                Button(
                    onClick = {
                        onVariantClick(variant)
                        onClick()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = buttonText)
                }

                Text(
                    text = ctaSubtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
private fun RewardFirstLayout(
    title: String,
    message: String,
    rewardAmountText: String,
    trustText: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (rewardAmountText.isNotBlank()) {
            Text(
                text = rewardAmountText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF16A34A)
            )
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF334155),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = trustText,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF475569)
        )
    }
}

@Composable
private fun IllustrationFirstLayout(
    title: String,
    message: String,
    rewardAmountText: String,
    trustText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BrandedGiftVisual(
            icon = Icons.Default.CardGiftcard,
            rewardAmountText = rewardAmountText,
            modifier = Modifier.size(68.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF334155),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = trustText,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF475569)
            )
        }
    }
}

@Composable
private fun BrandedGiftVisual(
    icon: ImageVector,
    rewardAmountText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF59E0B), Color(0xFFF97316))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (rewardAmountText.isNotBlank()) {
            Text(
                text = rewardAmountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

private fun extractRewardAmount(message: String): String {
    val regex = Regex("₹\\s*[0-9]+")
    val match = regex.find(message)?.value.orEmpty()
    return match.replace(" ", "")
}
