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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.Announcement
import com.example.dutype.models.AnnouncementType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Get announcement styling based on type
 */
private data class AnnouncementStyle(
    val gradient: Brush,
    val iconColor: Color,
    val icon: ImageVector,
    val emoji: String
)

@Composable
private fun getAnnouncementStyle(type: AnnouncementType): AnnouncementStyle {
    return when (type) {
        AnnouncementType.INFO -> AnnouncementStyle(
            gradient = Brush.horizontalGradient(
                colors = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
            ),
            iconColor = Color.White,
            icon = Icons.Default.Info,
            emoji = "ℹ️"
        )
        AnnouncementType.SUCCESS -> AnnouncementStyle(
            gradient = Brush.horizontalGradient(
                colors = listOf(Color(0xFF10B981), Color(0xFF059669))
            ),
            iconColor = Color.White,
            icon = Icons.Default.CheckCircle,
            emoji = "✅"
        )
        AnnouncementType.WARNING -> AnnouncementStyle(
            gradient = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
            ),
            iconColor = Color.White,
            icon = Icons.Default.Warning,
            emoji = "⚠️"
        )
        AnnouncementType.ERROR -> AnnouncementStyle(
            gradient = Brush.horizontalGradient(
                colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
            ),
            iconColor = Color.White,
            icon = Icons.Default.Error,
            emoji = "❌"
        )
        AnnouncementType.FEATURE -> AnnouncementStyle(
            gradient = Brush.horizontalGradient(
                colors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
            ),
            iconColor = Color.White,
            icon = Icons.Default.Star,
            emoji = "✨"
        )
        AnnouncementType.PROMOTION -> AnnouncementStyle(
            gradient = Brush.horizontalGradient(
                colors = listOf(Color(0xFFF97316), Color(0xFFEA580C))
            ),
            iconColor = Color.White,
            icon = Icons.Default.LocalOffer,
            emoji = "🎁"
        )
    }
}

/**
 * Beautiful Announcement Card with gradient background
 */
@Composable
fun AnnouncementCard(
    announcement: Announcement,
    onDismiss: () -> Unit,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val style = getAnnouncementStyle(announcement.type)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(style.gradient)
                .padding(16.dp)
        ) {
            // Dismiss button
            if (announcement.isDismissible) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (announcement.isDismissible) 32.dp else 0.dp)
                ) {
                    Text(
                        text = announcement.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    if (announcement.message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = announcement.message,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 18.sp
                        )
                    }
                    
                    // Action button
                    if (announcement.actionText != null && onAction != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = style.gradient.let { Color(0xFF1F2937) }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = announcement.actionText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
        // Carousel
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
        
        // Page indicators
        if (announcements.size > 1) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(announcements.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(
                                width = if (isSelected) 24.dp else 8.dp,
                                height = 8.dp
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.Gray.copy(alpha = 0.3f)
                            )
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
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
