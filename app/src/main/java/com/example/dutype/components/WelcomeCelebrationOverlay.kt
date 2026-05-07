package com.example.dutype.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private const val PREFS_KEY_CELEBRATION = "show_welcome_celebration"
private const val PREFS_NAME = "dutype_prefs"

/** Write this flag from profile-setup screens after first completion. */
fun markWelcomeCelebrationPending(context: android.content.Context) {
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .edit().putBoolean(PREFS_KEY_CELEBRATION, true).apply()
}

/** Read and immediately clear the flag so it shows only once. */
fun consumeWelcomeCelebrationFlag(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    return if (prefs.getBoolean(PREFS_KEY_CELEBRATION, false)) {
        prefs.edit().putBoolean(PREFS_KEY_CELEBRATION, false).apply()
        true
    } else {
        false
    }
}

private data class ConfettiParticle(
    val x: Float,       // 0..1 fraction of width
    val startY: Float,  // -0.2..0 (starts above screen)
    val color: Color,
    val radius: Float,
    val speed: Float,   // pixels per animation unit
    val wobble: Float   // horizontal wobble amplitude
)

private val confettiColors = listOf(
    Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF3B82F6),
    Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899),
    Color(0xFF06B6D4), Color(0xFF84CC16)
)

@Composable
fun WelcomeCelebrationOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    durationMs: Int = 3200,
    bonusAmount: Int = 0
) {
    var scratchProgress by remember(visible) { mutableStateOf(0f) }
    val revealed = scratchProgress >= 1f

    fun addScratchProgress(delta: Float) {
        val next = (scratchProgress + delta).coerceIn(0f, 1f)
        scratchProgress = if (next >= 0.72f) 1f else next
    }

    LaunchedEffect(visible, revealed) {
        if (visible && revealed) {
            delay(durationMs.toLong())
            onDismiss()
        }
    }

    val particles = remember {
        List(60) {
            ConfettiParticle(
                x = Random.nextFloat(),
                startY = -0.05f - Random.nextFloat() * 0.15f,
                color = confettiColors.random(),
                radius = 5f + Random.nextFloat() * 7f,
                speed = 0.15f + Random.nextFloat() * 0.25f,
                wobble = 20f + Random.nextFloat() * 30f
            )
        }
    }

    val confettiProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMs, easing = LinearEasing),
        label = "confetti_progress"
    )

    // Card scale-in animation
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutBounce),
        label = "card_scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            // Confetti canvas
            if (revealed || confettiProgress > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    particles.forEach { p ->
                        val yFrac = p.startY + confettiProgress * (1.2f + p.speed)
                        val xOffset = p.wobble * sin(confettiProgress * 6f + p.x * 10f)
                        val cx = p.x * size.width + xOffset
                        val cy = yFrac * size.height
                        if (cy in -p.radius..size.height + p.radius) {
                            drawCircle(
                                color = p.color.copy(alpha = (1f - (yFrac / 1.2f)).coerceIn(0.2f, 1f)),
                                radius = p.radius,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }

            // Center celebration card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8FFF1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.welcome_scratch_ready_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.welcome_scratch_ready_body),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF475569),
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                ScratchRevealPanel(
                    bonusAmount = bonusAmount,
                    revealed = revealed,
                    scratchProgress = scratchProgress,
                    onScratch = ::addScratchProgress
                )
            }
        }
    }
}

@Composable
private fun ScratchRevealPanel(
    bonusAmount: Int,
    revealed: Boolean,
    scratchProgress: Float,
    onScratch: (Float) -> Unit
) {
    val coverAlpha by animateFloatAsState(
        targetValue = if (revealed) 0f else (1f - scratchProgress * 0.82f).coerceIn(0.12f, 1f),
        animationSpec = tween(220),
        label = "scratch_cover_alpha"
    )
    val rewardScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.94f,
        animationSpec = tween(300, easing = EaseOutBounce),
        label = "scratch_reward_scale"
    )
    val rewardTitle = if (bonusAmount > 0) {
        stringResource(R.string.welcome_scratch_bonus_title, bonusAmount)
    } else {
        stringResource(R.string.welcome_scratch_bonus_generic)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFEFFFF5), Color(0xFFD9FBE7), Color(0xFFFFFFFF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                scaleX = rewardScale
                scaleY = rewardScale
                alpha = if (revealed) 1f else 0.15f
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = rewardTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFF047857),
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 25.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
            Text(
                text = stringResource(R.string.welcome_scratch_wallet_hint),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
        }

        if (!revealed || coverAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha }
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFE5E7EB), Color(0xFFF8FAFC), Color(0xFFCBD5E1))
                        )
                    )
                    .pointerInput(revealed) {
                        if (!revealed) {
                            detectDragGestures(
                                onDragStart = { onScratch(0.16f) },
                                onDrag = { _, dragAmount ->
                                    val distance = abs(dragAmount.x) + abs(dragAmount.y)
                                    onScratch(distance / 900f)
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 20.dp.toPx()
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.28f),
                            start = Offset(x, 0f),
                            end = Offset(x + size.height, size.height),
                            strokeWidth = 5.dp.toPx()
                        )
                        x += step
                    }
                }
                Text(
                    text = stringResource(R.string.welcome_scratch_prompt),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
