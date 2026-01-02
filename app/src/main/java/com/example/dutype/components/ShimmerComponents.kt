package com.example.dutype.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke

/**
 * Reusable shimmer components for loading states across the app.
 * Provides consistent, professional loading animations.
 */

// ============================================
// BASE SHIMMER COMPONENTS
// ============================================

/**
 * Base shimmer box with animated gradient effect
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val shimmerColors = listOf(
        Color(0xFFE5E7EB),
        Color(0xFFF3F4F6),
        Color(0xFFE5E7EB)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .background(brush, shape)
            .let {
                if (width != null) {
                    it.width(width)
                } else {
                    it.fillMaxWidth()
                }
            }
            .height(height)
    )
}

/**
 * Circular shimmer for profile images/avatars
 */
@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp
) {
    val shimmerColors = listOf(
        Color(0xFFE5E7EB),
        Color(0xFFF3F4F6),
        Color(0xFFE5E7EB)
    )

    val transition = rememberInfiniteTransition(label = "shimmer_circle")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_circle"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .size(size)
            .background(brush, CircleShape)
    )
}

// ============================================
// PROFILE SHIMMER COMPONENTS
// ============================================

/**
 * Full-page shimmer for profile screens (Worker/Employer)
 */
@Composable
fun ProfileShimmer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(13.dp))
        
        // Title shimmer
        ShimmerBox(width = 80.dp, height = 32.dp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Profile header section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            ShimmerCircle(size = 60.dp)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(width = 150.dp, height = 20.dp)
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(width = 120.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(width = 180.dp, height = 16.dp)
            }
            
            // Arrow
            ShimmerBox(width = 16.dp, height = 16.dp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // App Settings title
        ShimmerBox(width = 120.dp, height = 24.dp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Settings menu items
        repeat(8) {
            ProfileSettingsItemShimmer()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProfileSettingsItemShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(width = 24.dp, height = 24.dp)
        Spacer(modifier = Modifier.width(16.dp))
        ShimmerBox(modifier = Modifier.weight(1f), height = 20.dp)
        ShimmerBox(width = 16.dp, height = 16.dp)
    }
}

// ============================================
// APPLICATION SHIMMER COMPONENTS
// ============================================

/**
 * Full-page shimmer for application detail screens
 */
@Composable
fun ApplicationDetailShimmer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header shimmer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(width = 40.dp, height = 40.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(width = 150.dp, height = 20.dp)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(width = 100.dp, height = 14.dp)
            }
            ShimmerBox(width = 40.dp, height = 40.dp)
        }
        
        // Divider
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFE5E7EB)))
        
        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Worker profile card
            item { WorkerProfileCardShimmer() }
            
            // Contact card
            item { ContactCardShimmer() }
            
            // Experience card
            item { SectionCardShimmer(title = true, lines = 3) }
            
            // Skills card
            item { SkillsCardShimmer() }
            
            // Job info card
            item { SectionCardShimmer(title = true, lines = 4) }
        }
        
        // Bottom action bar
        ApplicationActionBarShimmer()
    }
}

@Composable
private fun WorkerProfileCardShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerCircle(size = 80.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(width = 150.dp, height = 24.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBox(width = 80.dp, height = 24.dp, shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerBox(width = 120.dp, height = 14.dp)
                }
            }
        }
    }
}

@Composable
private fun ContactCardShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerBox(width = 160.dp, height = 20.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email row
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerCircle(size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(width = 50.dp, height = 12.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerBox(width = 180.dp, height = 16.dp)
                }
                ShimmerBox(width = 60.dp, height = 32.dp, shape = RoundedCornerShape(8.dp))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Phone row
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerCircle(size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(width = 50.dp, height = 12.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    ShimmerBox(width = 120.dp, height = 16.dp)
                }
                ShimmerBox(width = 50.dp, height = 32.dp, shape = RoundedCornerShape(8.dp))
            }
        }
    }
}

@Composable
private fun SectionCardShimmer(
    title: Boolean = true,
    lines: Int = 3
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title) {
                ShimmerBox(width = 140.dp, height = 20.dp)
                Spacer(modifier = Modifier.height(16.dp))
            }
            repeat(lines) {
                ShimmerBox(height = 16.dp)
                if (it < lines - 1) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SkillsCardShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerBox(width = 100.dp, height = 20.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(width = 80.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
                ShimmerBox(width = 100.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
                ShimmerBox(width = 70.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
            }
        }
    }
}

@Composable
private fun ApplicationActionBarShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(
            modifier = Modifier.weight(1f),
            height = 52.dp,
            shape = RoundedCornerShape(12.dp)
        )
        ShimmerBox(
            modifier = Modifier.weight(1f),
            height = 52.dp,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

/**
 * Shimmer for application list items
 */
@Composable
fun ApplicationListItemShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerCircle(size = 50.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(width = 140.dp, height = 18.dp)
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(width = 100.dp, height = 14.dp)
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(width = 80.dp, height = 12.dp)
            }
            ShimmerBox(width = 70.dp, height = 24.dp, shape = RoundedCornerShape(12.dp))
        }
    }
}

/**
 * Full-page shimmer for application management screens
 */
@Composable
fun ApplicationManagementShimmer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(width = 40.dp, height = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            ShimmerBox(width = 150.dp, height = 24.dp)
        }
        
        // Tabs shimmer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) {
                ShimmerBox(width = 70.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Application list
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(5) {
                ApplicationListItemShimmer()
            }
        }
    }
}

// ============================================
// NOTIFICATION SHIMMER COMPONENTS
// ============================================

/**
 * Shimmer for notification list items
 */
@Composable
fun NotificationItemShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        ShimmerCircle(size = 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(width = 200.dp, height = 18.dp)
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(height = 14.dp)
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(width = 80.dp, height = 12.dp)
        }
        ShimmerBox(width = 8.dp, height = 8.dp, shape = RoundedCornerShape(4.dp))
    }
}

/**
 * Full-page shimmer for notification screens
 */
@Composable
fun NotificationShimmer(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(width = 40.dp, height = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            ShimmerBox(width = 120.dp, height = 24.dp)
            Spacer(modifier = Modifier.weight(1f))
            ShimmerBox(width = 80.dp, height = 32.dp, shape = RoundedCornerShape(8.dp))
        }
        
        // Divider
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFE5E7EB)))
        
        // Notification list
        LazyColumn {
            items(8) {
                NotificationItemShimmer()
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFF3F4F6)))
            }
        }
    }
}

// REMOVED: FullScreenLoader() and ContentCardShimmer() - Dead code, never called anywhere
// Use ProfileShimmer, ApplicationDetailShimmer, etc. for specific use cases
// Or create new shimmer components as needed

// ============================================
// JOB CARD SHIMMER COMPONENT
// ============================================

/**
 * A shimmer loading effect for job cards
 * Shows animated placeholder while jobs are being loaded
 * 
 * CONSOLIDATED: Moved from utils/JobCardShimmer.kt
 */
@Composable
fun JobCardShimmer() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    ShimmerJobCardInternal(brush = brush)
}

@Composable
private fun ShimmerJobCardInternal(brush: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Job title and company
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(
                        modifier = Modifier
                            .height(20.dp)
                            .fillMaxWidth(0.7f)
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Spacer(
                        modifier = Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.5f)
                            .background(brush)
                    )
                }

                Spacer(
                    modifier = Modifier
                        .size(40.dp)
                        .background(brush, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pay info
            Spacer(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(0.6f)
                    .background(brush, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .size(16.dp)
                        .background(brush)
                )
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                        .width(120.dp)
                        .background(brush)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Spacer(
                        modifier = Modifier
                            .height(24.dp)
                            .width(60.dp)
                            .background(brush, RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .height(40.dp)
                        .weight(1f)
                        .background(brush, RoundedCornerShape(8.dp))
                )
                Spacer(
                    modifier = Modifier
                        .height(40.dp)
                        .width(80.dp)
                        .background(brush, RoundedCornerShape(8.dp))
                )
            }
        }
    }
}
