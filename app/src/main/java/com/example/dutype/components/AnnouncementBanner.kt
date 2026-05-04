package com.example.dutype.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dutype.app.R
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
    return when (type) {
        AnnouncementType.INFO -> AnnouncementStyle(
            containerColor = Color(0xFFEFF6FF),
            accentColor = Color(0xFF2563EB),
            iconContainerColor = Color(0xFFDBEAFE),
            icon = Icons.Default.Info,
        )
        AnnouncementType.SUCCESS -> AnnouncementStyle(
            containerColor = Color(0xFFECFDF5),
            accentColor = Color(0xFF059669),
            iconContainerColor = Color(0xFFD1FAE5),
            icon = Icons.Default.CheckCircle,
        )
        AnnouncementType.WARNING -> AnnouncementStyle(
            containerColor = Color(0xFFFFFBEB),
            accentColor = Color(0xFFD97706),
            iconContainerColor = Color(0xFFFEF3C7),
            icon = Icons.Default.Warning,
        )
        AnnouncementType.ERROR -> AnnouncementStyle(
            containerColor = Color(0xFFFEF2F2),
            accentColor = Color(0xFFDC2626),
            iconContainerColor = Color(0xFFFEE2E2),
            icon = Icons.Default.Error,
        )
        AnnouncementType.FEATURE -> AnnouncementStyle(
            containerColor = Color(0xFFF5F3FF),
            accentColor = Color(0xFF7C3AED),
            iconContainerColor = Color(0xFFEDE9FE),
            icon = Icons.Default.Star,
        )
        AnnouncementType.PROMOTION -> AnnouncementStyle(
            containerColor = Color(0xFFFFF7ED),
            accentColor = Color(0xFFEA580C),
            iconContainerColor = Color(0xFFFFEDD5),
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
            .padding(horizontal = 16.dp)
            .then(actionModifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(style.containerColor, Color.White)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .height(72.dp)
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

                    if (onAction != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = style.iconContainerColor,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = style.accentColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(5.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = WorkerColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
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
    autoScrollDuration: Long = 5000L // 5 seconds per announcement
) {
    if (announcements.isEmpty()) return
    
    if (announcements.size == 1) {
        // Single announcement - no carousel needed
        AnnouncementCard(
            announcement = announcements[0],
            onDismiss = { onDismiss(announcements[0].id) },
            onAction = if (announcements[0].actionRoute != null) {
                { onAction(announcements[0]) }
            } else null,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }
    
    // Multiple announcements - show carousel
    val pagerState = rememberPagerState(pageCount = { announcements.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll effect
    LaunchedEffect(pagerState.currentPage) {
        delay(autoScrollDuration)
        val nextPage = (pagerState.currentPage + 1) % announcements.size
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
            val announcement = announcements[page]
            AnnouncementCard(
                announcement = announcement,
                onDismiss = { onDismiss(announcement.id) },
                onAction = if (announcement.actionRoute != null) {
                    { onAction(announcement) }
                } else null
            )
        }
        

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            announcements.forEachIndexed { index, announcement ->
                val isSelected = pagerState.currentPage == index
                val dotStyle = getAnnouncementStyle(announcement.type)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = if (isSelected) 18.dp else 7.dp, height = 7.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) dotStyle.accentColor else Color(0xFFE5E7EB))
                )
            }
        }
    }
}

/**
 * Legacy support - Announcement List (non-carousel)
 */
@Composable
fun AnnouncementList(
    announcements: List<Announcement>,
    onDismiss: (String) -> Unit,
    onAction: (Announcement) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use carousel instead
    AnnouncementCarousel(
        announcements = announcements,
        onDismiss = onDismiss,
        onAction = onAction,
        modifier = modifier
    )
}
