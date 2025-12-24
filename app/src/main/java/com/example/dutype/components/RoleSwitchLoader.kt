package com.example.dutype.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dutype.models.UserRole
import kotlinx.coroutines.delay

@Composable
fun RoleSwitchLoader(
    isVisible: Boolean,
    currentRole: UserRole,
    targetRole: UserRole,
    onDismiss: () -> Unit = {}
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            RoleSwitchLoaderContent(
                currentRole = currentRole,
                targetRole = targetRole
            )
        }
    }
}

@Composable
private fun RoleSwitchLoaderContent(
    currentRole: UserRole,
    targetRole: UserRole
) {
    var animationPhase by remember { mutableStateOf(0) }
    
    // Animation phases: 0 = initial, 1 = switching, 2 = complete
    LaunchedEffect(Unit) {
        animationPhase = 1
        delay(2000) // Simulate role switch process
        animationPhase = 2
        delay(1000) // Show completion briefly
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC),
                            Color(0xFFE2E8F0)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Animated Logo/Icon
                AnimatedRoleIcon(
                    currentRole = currentRole,
                    targetRole = targetRole,
                    animationPhase = animationPhase
                )
                
                // Title
                AnimatedContent(
                    targetState = animationPhase,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) + 
                        slideInVertically(animationSpec = tween(500)) togetherWith
                        fadeOut(animationSpec = tween(300)) + 
                        slideOutVertically(animationSpec = tween(300))
                    },
                    label = "title_animation"
                ) { phase ->
                    Text(
                        text = when (phase) {
                            0 -> "Preparing Role Switch..."
                            1 -> "Switching to ${targetRole.name}..."
                            2 -> "Role Switched Successfully!"
                            else -> "Switching Role..."
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Description
                AnimatedContent(
                    targetState = animationPhase,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) + 
                        slideInVertically(animationSpec = tween(500)) togetherWith
                        fadeOut(animationSpec = tween(300)) + 
                        slideOutVertically(animationSpec = tween(300))
                    },
                    label = "description_animation"
                ) { phase ->
                    Text(
                        text = when (phase) {
                            0 -> "Please wait while we prepare your ${targetRole.name} interface..."
                            1 -> "Updating your profile and loading ${targetRole.name} features..."
                            2 -> "Welcome to your ${targetRole.name} dashboard!"
                            else -> "Please wait..."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Progress Indicator
                RoleSwitchProgressIndicator(
                    animationPhase = animationPhase,
                    targetRole = targetRole
                )
                
                // Role transition visualization
                if (animationPhase >= 1) {
                    RoleTransitionVisualization(
                        currentRole = currentRole,
                        targetRole = targetRole,
                        animationPhase = animationPhase
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedRoleIcon(
    currentRole: UserRole,
    targetRole: UserRole,
    animationPhase: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "role_icon")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = when (animationPhase) {
            0 -> 1f
            1 -> 1.1f
            2 -> 1f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = when (targetRole) {
                        UserRole.WORKER -> Color(0xFF10B981).copy(alpha = 0.1f)
                        UserRole.EMPLOYER -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                        else -> Color(0xFF6B7280).copy(alpha = 0.1f)
                    },
                    shape = CircleShape
                )
        )
        
        // Animated icon
        Icon(
            imageVector = when (targetRole) {
                UserRole.WORKER -> Icons.Default.Person
                UserRole.EMPLOYER -> Icons.Default.Business
                else -> Icons.Default.Person
            },
            contentDescription = "Role Switch",
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = if (animationPhase == 1) rotation else 0f
                },
            tint = when (targetRole) {
                UserRole.WORKER -> Color(0xFF10B981)
                UserRole.EMPLOYER -> Color(0xFF3B82F6)
                else -> Color(0xFF6B7280)
            }
        )
    }
}

@Composable
private fun RoleSwitchProgressIndicator(
    animationPhase: Int,
    targetRole: UserRole
) {
    val progress by animateFloatAsState(
        targetValue = when (animationPhase) {
            0 -> 0.3f
            1 -> 0.7f
            2 -> 1f
            else -> 0f
        },
        animationSpec = tween(1000),
        label = "progress"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when (targetRole) {
                UserRole.WORKER -> Color(0xFF10B981)
                UserRole.EMPLOYER -> Color(0xFF3B82F6)
                else -> Color(0xFF6B7280)
            },
            trackColor = Color(0xFFE2E8F0)
        )
        
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun RoleTransitionVisualization(
    currentRole: UserRole,
    targetRole: UserRole,
    animationPhase: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "transition")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Current role (fading out)
        AnimatedVisibility(
            visible = animationPhase < 2,
            enter = fadeIn(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            RoleTransitionItem(
                role = currentRole,
                isActive = animationPhase == 0,
                modifier = Modifier.graphicsLayer {
                    scaleX = if (animationPhase == 1) pulse else 1f
                    scaleY = if (animationPhase == 1) pulse else 1f
                }
            )
        }
        
        // Arrow transition
        if (animationPhase >= 1) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Transition",
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    },
                tint = Color(0xFF3B82F6)
            )
        }
        
        // Target role (fading in)
        AnimatedVisibility(
            visible = animationPhase >= 1,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut()
        ) {
            RoleTransitionItem(
                role = targetRole,
                isActive = animationPhase == 2,
                modifier = Modifier.graphicsLayer {
                    scaleX = if (animationPhase == 1) pulse else 1f
                    scaleY = if (animationPhase == 1) pulse else 1f
                }
            )
        }
    }
}

@Composable
private fun RoleTransitionItem(
    role: UserRole,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (role) {
        UserRole.WORKER -> Color(0xFF10B981).copy(alpha = if (isActive) 0.2f else 0.1f)
        UserRole.EMPLOYER -> Color(0xFF3B82F6).copy(alpha = if (isActive) 0.2f else 0.1f)
        else -> Color(0xFF6B7280).copy(alpha = if (isActive) 0.2f else 0.1f)
    }
    
    val textColor = when (role) {
        UserRole.WORKER -> Color(0xFF10B981)
        UserRole.EMPLOYER -> Color(0xFF3B82F6)
        else -> Color(0xFF6B7280)
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = backgroundColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (role) {
                    UserRole.WORKER -> Icons.Default.Person
                    UserRole.EMPLOYER -> Icons.Default.Business
                    else -> Icons.Default.Person
                },
                contentDescription = role.name,
                modifier = Modifier.size(24.dp),
                tint = textColor
            )
        }
        
        Text(
            text = role.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = textColor
        )
    }
}
