package com.example.dutype.worker.components.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * P2 PERFORMANCE FIX: Extracted HomePromiseCarousel composable
 * 
 * Reduces recomposition scope - animation runs independently
 * without affecting parent composables.
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */
@Composable
fun HomePromiseCarousel(
    modifier: Modifier = Modifier
) {
    // Animation state
    var animationProgress by remember { mutableStateOf(0f) }
    var showBorderAnimation by remember { mutableStateOf(true) }
    
    // Animation cycle: animate fast → hide → pause → repeat
    LaunchedEffect(Unit) {
        while (true) {
            // Phase 1: Show and animate (1.5 seconds)
            showBorderAnimation = true
            animationProgress = 0f
            
            val animDuration = 1500L
            val startTime = System.currentTimeMillis()
            
            while (System.currentTimeMillis() - startTime < animDuration) {
                val elapsed = System.currentTimeMillis() - startTime
                animationProgress = (elapsed.toFloat() / animDuration).coerceIn(0f, 1f)
                kotlinx.coroutines.delay(8) // ~120fps
            }
            animationProgress = 1f
            
            // Phase 2: Hide the animated line
            showBorderAnimation = false
            
            // Phase 3: Pause for 2.5 seconds
            kotlinx.coroutines.delay(2500)
            
            // Reset for next cycle
            animationProgress = 0f
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Animated border
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 1.dp.toPx()
            val cornerRadius = 16.dp.toPx()
            
            // Create rounded rect path
            val rect = androidx.compose.ui.geometry.Rect(
                strokeWidth / 2,
                strokeWidth / 2,
                size.width - strokeWidth / 2,
                size.height - strokeWidth / 2
            )
            val roundRect = androidx.compose.ui.geometry.RoundRect(
                rect,
                CornerRadius(cornerRadius)
            )
            val path = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(roundRect)
            }
            
            // Get actual path length
            val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
            pathMeasure.setPath(path, false)
            val pathLength = pathMeasure.length
            
            // Light gray background border (always visible)
            drawRoundRect(
                color = Color(0xFFE8E8E8),
                cornerRadius = CornerRadius(cornerRadius),
                style = Stroke(width = strokeWidth)
            )
            
            // Animated gradient line
            if (showBorderAnimation) {
                val dashLength = pathLength * 0.25f
                val gapLength = pathLength * 0.75f
                val phase = animationProgress * pathLength
                
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF667EEA),
                            Color(0xFF764BA2),
                            Color(0xFFf093fb),
                        )
                    ),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(dashLength, gapLength),
                            phase = -phase
                        )
                    )
                )
            }
        }
        
        // Clean white card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PromiseItem(
                    icon = Icons.Default.CheckCircle,
                    title = "100% Free",
                    iconColor = Color(0xFF10B981)
                )
                
                PromiseItem(
                    icon = Icons.Default.Star,
                    title = "Verified Jobs",
                    iconColor = Color(0xFF3B82F6)
                )
                
                PromiseItem(
                    icon = Icons.Default.Lock,
                    title = "Secure Pay",
                    iconColor = Color(0xFF8B5CF6)
                )
                
                PromiseItem(
                    icon = Icons.Default.Chat,
                    title = "24/7 Support",
                    iconColor = Color(0xFFF59E0B)
                )
            }
        }
    }
}

@Composable
private fun PromiseItem(
    icon: ImageVector,
    title: String,
    iconColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(78.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151),
                fontSize = 11.sp,
                lineHeight = 14.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
