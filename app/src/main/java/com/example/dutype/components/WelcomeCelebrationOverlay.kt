package com.example.dutype.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.delay
import kotlin.math.cos
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
    durationMs: Int = 2800
) {
    // Auto-dismiss
    LaunchedEffect(visible) {
        if (visible) {
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

    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing)
        ),
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
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    val yFrac = p.startY + progress * (1.2f + p.speed)
                    val xOffset = p.wobble * sin(progress * 6f + p.x * 10f)
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

            // Center celebration card
            Column(
                modifier = Modifier
                    .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "You're all set! 🎉",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Welcome to DutyPe.\nYour account is ready.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF475569),
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✨  Check your wallet for your welcome gift",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
