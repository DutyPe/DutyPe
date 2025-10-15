package com.example.dutype.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Professional pull-to-refresh functionality
 * Provides smooth and intuitive refresh gestures
 */

/**
 * Pull-to-refresh state
 */
data class PullToRefreshState(
    val isRefreshing: Boolean = false,
    val isPulling: Boolean = false,
    val pullProgress: Float = 0f,
    val canRefresh: Boolean = false
)

/**
 * Pull-to-refresh component
 */
@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshThreshold: Float = 80f,
    maxPullDistance: Float = 120f,
    refreshContent: @Composable (PullToRefreshState) -> Unit = { DefaultRefreshContent(it) },
    content: @Composable () -> Unit
) {
    var pullProgress by remember { mutableStateOf(0f) }
    var isPulling by remember { mutableStateOf(false) }
    var canRefresh by remember { mutableStateOf(false) }
    
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { refreshThreshold.dp.toPx() }
    val maxPullDistancePx = with(density) { maxPullDistance.dp.toPx() }
    
    val pullToRefreshState = PullToRefreshState(
        isRefreshing = isRefreshing,
        isPulling = isPulling,
        pullProgress = pullProgress,
        canRefresh = canRefresh
    )
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                return if (delta < 0 && !isRefreshing) {
                    val newPullProgress = (pullProgress - delta / refreshThresholdPx).coerceAtLeast(0f)
                    pullProgress = newPullProgress
                    isPulling = newPullProgress > 0f
                    canRefresh = newPullProgress >= 1f
                    Offset(0f, delta)
                } else {
                    Offset.Zero
                }
            }
            
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isRefreshing) return Offset.Zero
                
                val delta = available.y
                if (delta > 0 && pullProgress > 0f) {
                    val newPullProgress = (pullProgress - delta / refreshThresholdPx).coerceAtLeast(0f)
                    pullProgress = newPullProgress
                    isPulling = newPullProgress > 0f
                    canRefresh = newPullProgress >= 1f
                    
                    if (newPullProgress <= 0f) {
                        isPulling = false
                        canRefresh = false
                    }
                    
                    return Offset(0f, delta)
                }
                
                return Offset.Zero
            }
        }
    }
    
    Box(
        modifier = modifier.nestedScroll(nestedScrollConnection)
    ) {
        // Main content
        content()
        
        // Pull-to-refresh indicator
        if (isPulling || isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { (pullProgress * refreshThresholdPx).toDp() })
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                refreshContent(pullToRefreshState)
            }
        }
    }
    
    // Handle refresh trigger
    LaunchedEffect(canRefresh, isRefreshing) {
        if (canRefresh && !isRefreshing) {
            onRefresh()
            pullProgress = 0f
            isPulling = false
            canRefresh = false
        }
    }
}

/**
 * Default refresh content
 */
@Composable
fun DefaultRefreshContent(
    state: PullToRefreshState,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (state.isRefreshing) 360f else 0f,
        animationSpec = if (state.isRefreshing) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(200)
        },
        label = "refresh_rotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (state.canRefresh) 1.2f else 1f,
        animationSpec = tween(200),
        label = "refresh_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (state.isPulling) 1f else 0f,
        animationSpec = tween(200),
        label = "refresh_alpha"
    )
    
    Card(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .alpha(alpha),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Custom refresh content with text
 */
@Composable
fun TextRefreshContent(
    state: PullToRefreshState,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (state.isRefreshing) 360f else 0f,
        animationSpec = if (state.isRefreshing) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(200)
        },
        label = "text_refresh_rotation"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (state.isPulling) 1f else 0f,
        animationSpec = tween(200),
        label = "text_refresh_alpha"
    )
    
    Row(
        modifier = modifier.alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = when {
                state.isRefreshing -> "Refreshing..."
                state.canRefresh -> "Release to refresh"
                else -> "Pull to refresh"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Animated refresh content
 */
@Composable
fun AnimatedRefreshContent(
    state: PullToRefreshState,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (state.isRefreshing) 360f else 0f,
        animationSpec = if (state.isRefreshing) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(200)
        },
        label = "animated_refresh_rotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (state.canRefresh) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animated_refresh_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (state.isPulling) 1f else 0f,
        animationSpec = tween(200),
        label = "animated_refresh_alpha"
    )
    
    Box(
        modifier = modifier
            .alpha(alpha)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        )
        
        // Inner icon
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Pull-to-refresh with haptic feedback
 */
@Composable
fun PullToRefreshWithHaptic(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshThreshold: Float = 80f,
    onHapticFeedback: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        refreshThreshold = refreshThreshold,
        refreshContent = { state ->
            // Trigger haptic feedback when can refresh
            LaunchedEffect(state.canRefresh) {
                if (state.canRefresh && !hasTriggeredHaptic) {
                    onHapticFeedback()
                    hasTriggeredHaptic = true
                } else if (!state.canRefresh) {
                    hasTriggeredHaptic = false
                }
            }
            
            DefaultRefreshContent(state)
        },
        content = content
    )
}

/**
 * Swipe-to-refresh for horizontal lists
 */
@Composable
fun SwipeToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshThreshold: Float = 80f,
    content: @Composable () -> Unit
) {
    var pullProgress by remember { mutableStateOf(0f) }
    var isPulling by remember { mutableStateOf(false) }
    var canRefresh by remember { mutableStateOf(false) }
    
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { refreshThreshold.dp.toPx() }
    
    val pullToRefreshState = PullToRefreshState(
        isRefreshing = isRefreshing,
        isPulling = isPulling,
        pullProgress = pullProgress,
        canRefresh = canRefresh
    )
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (canRefresh && !isRefreshing) {
                            onRefresh()
                            pullProgress = 0f
                            isPulling = false
                            canRefresh = false
                        }
                    }
                ) { _, dragAmount ->
                    if (!isRefreshing) {
                        val newPullProgress = (pullProgress + dragAmount.x / refreshThresholdPx).coerceAtLeast(0f)
                        pullProgress = newPullProgress
                        isPulling = newPullProgress > 0f
                        canRefresh = newPullProgress >= 1f
                    }
                }
            }
    ) {
        content()
        
        if (isPulling || isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(density) { (pullProgress * refreshThresholdPx).toDp() })
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                DefaultRefreshContent(pullToRefreshState)
            }
        }
    }
}

/**
 * Refresh indicator for lists
 */
@Composable
fun RefreshIndicator(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    if (isRefreshing) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Refreshing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Pull-to-refresh state manager
 */
@Composable
fun rememberPullToRefreshState(): MutableState<Boolean> {
    return remember { mutableStateOf(false) }
}

/**
 * Refresh action with delay
 */
@Composable
fun RefreshAction(
    onRefresh: () -> Unit,
    delay: Long = 1000L
) {
    LaunchedEffect(Unit) {
        delay(delay)
        onRefresh()
    }
}
