package com.example.dutype.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.Announcement
import com.example.dutype.models.AnnouncementType
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Announcement styling based on type: solid soft tint for the icon tile + accent for icon / CTA.
 */
private data class AnnouncementStyle(
    val tileColor: Color,
    val accentColor: Color,
    val icon: ImageVector,
)

@Composable
private fun getAnnouncementStyle(type: AnnouncementType): AnnouncementStyle {
    val isDark = isSystemInDarkTheme()
    val blue = Color(0xFF2563EB)
    val green = Color(0xFF16A34A)
    val amber = Color(0xFFD97706)
    val red = Color(0xFFDC2626)
    return when (type) {
        AnnouncementType.INFO -> AnnouncementStyle(
            tileColor = if (isDark) blue.copy(alpha = 0.18f) else Color(0xFFEFF6FF),
            accentColor = blue,
            icon = Icons.Outlined.Info,
        )
        AnnouncementType.SUCCESS -> AnnouncementStyle(
            tileColor = if (isDark) green.copy(alpha = 0.18f) else Color(0xFFF0FDF4),
            accentColor = green,
            icon = Icons.Outlined.CheckCircle,
        )
        AnnouncementType.WARNING -> AnnouncementStyle(
            tileColor = if (isDark) amber.copy(alpha = 0.18f) else Color(0xFFFEF3C7),
            accentColor = amber,
            icon = Icons.Outlined.WarningAmber,
        )
        AnnouncementType.ERROR -> AnnouncementStyle(
            tileColor = if (isDark) red.copy(alpha = 0.18f) else Color(0xFFFEF2F2),
            accentColor = red,
            icon = Icons.Outlined.ErrorOutline,
        )
        AnnouncementType.FEATURE -> AnnouncementStyle(
            tileColor = if (isDark) blue.copy(alpha = 0.18f) else Color(0xFFEFF6FF),
            accentColor = blue,
            icon = Icons.Outlined.Campaign,
        )
        AnnouncementType.PROMOTION -> AnnouncementStyle(
            tileColor = if (isDark) green.copy(alpha = 0.18f) else Color(0xFFF0FDF4),
            accentColor = green,
            icon = Icons.Outlined.Campaign,
        )
    }
}

/**
 * Public card: same signature as before. Adds the 20dp side gutter itself; the pager uses
 * [AnnouncementCardBody] directly because it supplies its own content padding.
 */
@Composable
fun AnnouncementCard(
    announcement: Announcement,
    onDismiss: () -> Unit,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        AnnouncementCardBody(
            announcement = announcement,
            onDismiss = onDismiss,
            onAction = onAction
        )
    }
}

/** Flat info card: icon tile, title, 2-line body, optional "View details" CTA, dismiss (x). */
@Composable
private fun AnnouncementCardBody(
    announcement: Announcement,
    onDismiss: () -> Unit,
    onAction: (() -> Unit)?
) {
    val style = getAnnouncementStyle(announcement.type)
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(16.dp)
    val surface = if (isDark) WorkerColors.CardBackground else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val actionModifier = if (onAction != null) {
        Modifier.clickable(onClick = onAction)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(surface)
            .border(1.dp, borderColor, cardShape)
            .then(actionModifier)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        AnnouncementIconTile(icon = style.icon, tint = style.tileColor, accent = style.accentColor)
        Spacer(modifier = Modifier.width(12.dp))
        AnnouncementTextColumn(
            announcement = announcement,
            accent = style.accentColor,
            showAction = onAction != null,
            modifier = Modifier.weight(1f)
        )
        AnnouncementDismissButton(onDismiss = onDismiss)
    }
}

@Composable
private fun AnnouncementTextColumn(
    announcement: Announcement,
    accent: Color,
    showAction: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) WorkerColors.TextPrimary else Color(0xFF0F0F0F)
    val bodyColor = if (isDark) WorkerColors.TextSecondary else Color(0xFF475569)
    Column(modifier = modifier) {
        Text(
            text = announcement.title,
            color = titleColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (announcement.message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = announcement.message,
                color = bodyColor,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showAction) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View details",
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AnnouncementDismissButton(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = 8.dp, y = (-8).dp)
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AnnouncementIconTile(icon: ImageVector, tint: Color, accent: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(tint, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** Pill page indicators: active 16x6 #0F0F0F, inactive 6x6 #CBD5E1. */
@Composable
private fun AnnouncementDots(count: Int, current: Int) {
    val isDark = isSystemInDarkTheme()
    val active = if (isDark) Color.White else Color(0xFF0F0F0F)
    val inactive = Color(0xFFCBD5E1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until count) {
            val isActive = i == current
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(if (isActive) 16.dp else 6.dp)
                    .height(6.dp)
                    .background(if (isActive) active else inactive, RoundedCornerShape(3.dp))
            )
        }
    }
}

/**
 * Announcement Carousel - Auto-scrolling carousel for multiple announcements
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnnouncementCarousel(
    announcements: List<Announcement>,
    onDismiss: (String) -> Unit,
    onAction: (Announcement) -> Unit,
    modifier: Modifier = Modifier,
    promoBannerUrl: String = "",
    autoScrollDuration: Long = 5000L // 5 seconds per announcement
) {
    val totalItems = announcements.size + if (promoBannerUrl.isNotBlank()) 1 else 0
    if (totalItems == 0) return

    if (totalItems == 1) {
        // Single announcement or single banner - no carousel needed
        if (promoBannerUrl.isNotBlank()) {
            PromoBannerSlide(
                promoBannerUrl = promoBannerUrl,
                modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        } else {
            AnnouncementCard(
                announcement = announcements[0],
                onDismiss = { onDismiss(announcements[0].id) },
                onAction = if (announcements[0].actionRoute != null) {
                    { onAction(announcements[0]) }
                } else null,
                modifier = modifier
            )
        }
        return
    }

    // Multiple items - show carousel with the next card peeking
    val pagerState = rememberPagerState(pageCount = { totalItems })
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll effect
    LaunchedEffect(pagerState.currentPage) {
        delay(autoScrollDuration)
        val nextPage = (pagerState.currentPage + 1) % totalItems
        coroutineScope.launch {
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Column(modifier = modifier.padding(vertical = 6.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp
        ) { page ->
            if (promoBannerUrl.isNotBlank() && page == 0) {
                PromoBannerSlide(promoBannerUrl = promoBannerUrl)
            } else {
                val index = if (promoBannerUrl.isNotBlank()) page - 1 else page
                val announcement = announcements[index]
                AnnouncementCardBody(
                    announcement = announcement,
                    onDismiss = { onDismiss(announcement.id) },
                    onAction = if (announcement.actionRoute != null) {
                        { onAction(announcement) }
                    } else null
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        AnnouncementDots(count = totalItems, current = pagerState.currentPage)
    }
}

@Composable
private fun PromoBannerSlide(promoBannerUrl: String, modifier: Modifier = Modifier) {
    com.example.dutype.components.OptimizedImage(
        imageUrl = promoBannerUrl,
        contentDescription = "Promotion banner",
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Crop,
        crossfadeMillis = 120
    )
}

/**
 * Legacy support - Announcement List (non-carousel)
 */
@Composable
fun AnnouncementList(
    announcements: List<Announcement>,
    onDismiss: (String) -> Unit,
    onAction: (Announcement) -> Unit,
    modifier: Modifier = Modifier,
    promoBannerUrl: String = ""
) {
    // Use carousel instead
    AnnouncementCarousel(
        announcements = announcements,
        onDismiss = onDismiss,
        onAction = onAction,
        modifier = modifier,
        promoBannerUrl = promoBannerUrl
    )
}
