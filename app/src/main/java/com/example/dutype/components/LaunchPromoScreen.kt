package com.example.dutype.components

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.dutype.promo.ResolvedPromo
import kotlinx.coroutines.delay

/**
 * Full-screen launch promo. Driven by the cached launch config (see promo/PromoOverlayHost).
 *
 * The image is guaranteed to be in Coil's disk cache before this composable is ever shown, and the
 * request itself never touches the network, so there is no spinner and no blank promo: the overlay
 * stays fully transparent until the (local) image is decoded, and silently dismisses on any error.
 *
 * @param onShown called once when the promo is actually visible (impression).
 * @param onClose called when the user taps close / back / the CTA, or the timer ends.
 * @param autoDismissMillis hard cap: the promo always closes by itself after this time.
 */
@Composable
fun LaunchPromoScreen(
    promo: ResolvedPromo,
    onShown: () -> Unit,
    onClose: (userAction: Boolean) -> Unit,
    onCta: () -> Unit,
    autoDismissMillis: Long = 6000L
) {
    val view = LocalView.current
    val systemBarColor = Color.Black.fg()
    var loaded by remember(promo.id) { mutableStateOf(false) }

    DisposableEffect(systemBarColor) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatusBarColor = window?.statusBarColor
        val previousNavigationBarColor = window?.navigationBarColor
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller?.isAppearanceLightNavigationBars

        window?.statusBarColor = systemBarColor.toArgb()
        window?.navigationBarColor = systemBarColor.toArgb()
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false

        onDispose {
            if (window != null) {
                previousStatusBarColor?.let { window.statusBarColor = it }
                previousNavigationBarColor?.let { window.navigationBarColor = it }
            }
            if (controller != null) {
                previousLightStatusBars?.let { controller.isAppearanceLightStatusBars = it }
                previousLightNavigationBars?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }

    LaunchedEffect(promo.id) {
        delay(autoDismissMillis)
        onClose(false)
    }

    // Back closes a dismissible promo; a non-dismissible one just swallows back until it times out.
    BackHandler(enabled = true) { if (promo.dismissible) onClose(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (loaded) Color.Black.bg() else Color.Transparent)
            .pointerInput(Unit) { } // swallow touches so the screen below is not tapped through
    ) {
        PromoImage(
            url = promo.imageUrl,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            heightPx = null,
            onSuccess = {
                if (!loaded) {
                    loaded = true
                    onShown()
                }
            },
            onError = { onClose(false) }
        )

        if (loaded && promo.dismissible) {
            PromoCloseButton(
                onClick = { onClose(true) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            )
        }

        if (loaded && promo.ctaLabel.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(24.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.bg())
                    .clickable(onClick = onCta),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = promo.ctaLabel,
                    color = Color.Black.fg(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Lightweight, flat banner overlay (top of the screen). Image is optional; when present it is
 * already in the disk cache.
 */
@Composable
fun LaunchPromoBanner(
    promo: ResolvedPromo,
    onShown: () -> Unit,
    onClose: (userAction: Boolean) -> Unit,
    onCta: () -> Unit,
    autoDismissMillis: Long = 10_000L
) {
    var imageOk by remember(promo.id) { mutableStateOf(promo.imageUrl.isBlank()) }

    LaunchedEffect(promo.id) {
        delay(autoDismissMillis)
        onClose(false)
    }
    LaunchedEffect(imageOk) {
        if (imageOk) onShown()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The card is laid out immediately (so its local image can load) but stays fully
        // transparent until the image is ready: no blank slot, no spinner.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .alpha(if (imageOk) 1f else 0f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.bg())
                .border(BorderStroke(1.dp, Color(0xFFE5E7EB).bd()), RoundedCornerShape(16.dp))
                .clickable(enabled = imageOk, onClick = onCta)
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (promo.imageUrl.isNotBlank()) {
                Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) {
                    PromoImage(
                        url = promo.imageUrl,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        heightPx = 64,
                        onSuccess = { imageOk = true },
                        onError = { onClose(false) }
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                if (promo.title.isNotBlank()) {
                    Text(
                        text = promo.title,
                        color = Color(0xFF111827).fg(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (promo.body.isNotBlank()) {
                    Text(
                        text = promo.body,
                        color = Color(0xFF4B5563).fg(),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (promo.ctaLabel.isNotBlank()) {
                    Text(
                        text = promo.ctaLabel,
                        color = Color(0xFF1E3A8A).fg(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (promo.dismissible && imageOk) {
                PromoCloseButton(onClick = { onClose(true) }, dark = true)
            }
        }
    }
}

@Composable
private fun PromoCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false
) {
    // 44dp touch target, flat circle (no shadow).
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (dark) Color.Transparent else Color(0x66000000).bg())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = if (dark) Color(0xFF6B7280).fg() else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Coil image that is served strictly from the local disk cache (network disabled), downsampled
 * to roughly the screen width so memory stays small.
 */
@Composable
private fun PromoImage(
    url: String,
    contentScale: ContentScale,
    modifier: Modifier,
    heightPx: Int?,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val widthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }.coerceAtLeast(1)
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }.coerceAtLeast(1)
    val request = remember(url, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(url)
            .diskCacheKey(url)
            .networkCachePolicy(CachePolicy.DISABLED)
            .size(if (heightPx != null) heightPx * 2 else widthPx, heightPx?.let { it * 2 } ?: screenHeightPx)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
        onSuccess = { onSuccess() },
        onError = { onError() }
    )
}
