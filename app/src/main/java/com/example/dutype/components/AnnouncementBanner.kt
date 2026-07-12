package com.example.dutype.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.dutype.models.Announcement
import com.example.dutype.models.AnnouncementType
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Get announcement styling based on type
 */
private data class AnnouncementStyle(
    val containerColor: Color,
    val accentColor: Color,
    val iconContainerColor: Color,
    val icon: ImageVector,
)

@Composable
private fun getAnnouncementStyle(type: AnnouncementType): AnnouncementStyle {
    val isDark = isSystemInDarkTheme()
    return when (type) {
        AnnouncementType.INFO -> AnnouncementStyle(
            containerColor = WorkerColors.InfoLight,
            accentColor = WorkerColors.Primary,
            iconContainerColor = WorkerColors.InfoLight,
            icon = Icons.Default.Info,
        )
        AnnouncementType.SUCCESS -> AnnouncementStyle(
            containerColor = WorkerColors.SuccessLight,
            accentColor = WorkerColors.Success,
            iconContainerColor = WorkerColors.SuccessLight,
            icon = Icons.Default.CheckCircle,
        )
        AnnouncementType.WARNING -> AnnouncementStyle(
            containerColor = WorkerColors.WarningLight,
            accentColor = WorkerColors.Warning,
            iconContainerColor = WorkerColors.WarningLight,
            icon = Icons.Default.Warning,
        )
        AnnouncementType.ERROR -> AnnouncementStyle(
            containerColor = WorkerColors.ErrorLight,
            accentColor = WorkerColors.Error,
            iconContainerColor = WorkerColors.ErrorLight,
            icon = Icons.Default.Error,
        )
        AnnouncementType.FEATURE -> AnnouncementStyle(
            containerColor = if (isDark) Color(0xFF2E1065) else Color(0xFFF5F3FF),
            accentColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
            iconContainerColor = if (isDark) Color(0xFF4C1D95) else Color(0xFFEDE9FE),
            icon = Icons.Default.Star,
        )
        AnnouncementType.PROMOTION -> AnnouncementStyle(
            containerColor = WorkerColors.WarningLight,
            accentColor = WorkerColors.Warning,
            iconContainerColor = WorkerColors.WarningLight,
            icon = Icons.Default.LocalOffer,
        )
    }
}

/**
 * Beautiful Announcement Card with gradient background
 * Entire card is clickable if actionRoute is provided
 */
@Composable
fun AnnouncementCard(
    announcement: Announcement,
    onDismiss: () -> Unit,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val style = getAnnouncementStyle(announcement.type)
    val actionModifier = if (onAction != null) {
        Modifier.clickable(onClick = onAction)
    } else {
        Modifier
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(115.dp)
            .then(actionModifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WorkerColors.Border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(style.containerColor, WorkerColors.CardBackground)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxHeight()
                    .width(5.dp)
                    .background(style.accentColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 14.dp, end = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(style.iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = announcement.title,
                        style = AppTypography.cardTitle.copy(color = WorkerColors.TextPrimary)
                    )
                    
                    if (announcement.message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = announcement.message,
                            style = AppTypography.bodySmall.copy(color = WorkerColors.TextSecondary)
                        )
                    }
                }
            }
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
            PromoBannerSlide(promoBannerUrl = promoBannerUrl, modifier = modifier)
        } else {
            AnnouncementCard(
                announcement = announcements[0],
                onDismiss = { onDismiss(announcements[0].id) },
                onAction = if (announcements[0].actionRoute != null) {
                    { onAction(announcements[0]) }
                } else null,
                modifier = modifier.padding(vertical = 8.dp)
            )
        }
        return
    }
    
    // Multiple items - show carousel
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
    
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            if (promoBannerUrl.isNotBlank() && page == 0) {
                PromoBannerSlide(promoBannerUrl = promoBannerUrl)
            } else {
                val index = if (promoBannerUrl.isNotBlank()) page - 1 else page
                val announcement = announcements[index]
                AnnouncementCard(
                    announcement = announcement,
                    onDismiss = { onDismiss(announcement.id) },
                    onAction = if (announcement.actionRoute != null) {
                        { onAction(announcement) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun PromoBannerSlide(promoBannerUrl: String, modifier: Modifier = Modifier) {
    com.example.dutype.components.OptimizedImage(
        imageUrl = promoBannerUrl,
        contentDescription = "Promotion banner",
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(115.dp) // Match AnnouncementCard height
            .clip(RoundedCornerShape(16.dp)),
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
