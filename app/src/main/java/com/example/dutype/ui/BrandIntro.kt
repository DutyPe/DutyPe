package com.example.dutype.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dutype.ui.theme.IBMPlexSansFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WORDMARK = "DutyPe"

/**
 * The launch wordmark: "DutyPe" in white on the splash black, each letter fading up into place
 * one after another ("Fade Up Characters"), then the whole layer fades away to reveal the app,
 * which has been loading underneath. About one second; still (and shorter) when the phone's
 * animations are off. Size is in dp so the phone's font-size setting does not change the logo.
 */
@Composable
fun BrandIntro(onFinished: () -> Unit) {
    val context = LocalContext.current
    val motion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
    val letters = remember { WORDMARK.map { Animatable(if (motion) 0f else 1f) } }
    val layer = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (motion) {
            coroutineScope {
                letters.forEachIndexed { i, letter ->
                    launch {
                        delay(i * 60L)
                        letter.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
                    }
                }
            }
            delay(220)
        } else {
            delay(300)
        }
        layer.animateTo(0f, tween(durationMillis = 260))
        onFinished()
    }

    val fontSize = with(LocalDensity.current) { 48.dp.toSp() }
    val rise = with(LocalDensity.current) { 18.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = layer.value }
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Row {
            WORDMARK.forEachIndexed { i, ch ->
                Text(
                    text = ch.toString(),
                    color = Color.White,
                    fontFamily = IBMPlexSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    modifier = Modifier.graphicsLayer {
                        val p = letters[i].value
                        alpha = p
                        translationY = (1f - p) * rise
                    }
                )
            }
        }
    }
}
