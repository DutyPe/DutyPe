package com.example.dutype.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.UserRole
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun RoleSwitchSection(
    currentRole: UserRole,
    onRoleSwitch: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showRoleSwitchLoader by remember { mutableStateOf(false) }
    var targetRole by remember { mutableStateOf<UserRole?>(null) }
    
    // Handle role switch after loader completes
    LaunchedEffect(showRoleSwitchLoader, targetRole) {
        if (showRoleSwitchLoader && targetRole != null) {
            Timber.d("🔄 RoleSwitchSection - Loader started for role: $targetRole")
            // Wait for loader animation to complete (3 seconds)
            delay(3000)
            Timber.d("🔄 RoleSwitchSection - Loader completed, calling onRoleSwitch with: $targetRole")
            // Now perform the actual role switch
            onRoleSwitch(targetRole!!)
            showRoleSwitchLoader = false
            targetRole = null
            Timber.d("🔄 RoleSwitchSection - Role switch callback completed")
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF3B82F6).copy(alpha = 0.3f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
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
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Role",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Switch Role",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    // Current role badge
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (currentRole) {
                                UserRole.WORKER -> Color(0xFF10B981).copy(alpha = 0.1f)
                                UserRole.EMPLOYER -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                                else -> Color(0xFF6B7280).copy(alpha = 0.1f)
                            }
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = currentRole.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = when (currentRole) {
                                UserRole.WORKER -> Color(0xFF10B981)
                                UserRole.EMPLOYER -> Color(0xFF3B82F6)
                                else -> Color(0xFF6B7280)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Switch between Worker and Employer modes to access different features",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color(0xFF64748B)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Role Switch with Switch Component
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current role with brackets
                    Text(
                        text = "(${currentRole.name})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = when (currentRole) {
                            UserRole.WORKER -> Color(0xFF10B981)
                            UserRole.EMPLOYER -> Color(0xFF3B82F6)
                            else -> Color(0xFF6B7280)
                        }
                    )
                    
                    // Switch Component
                    Switch(
                        checked = currentRole == UserRole.EMPLOYER, // true for employer, false for worker
                        onCheckedChange = { isEmployer ->
                            val newRole = if (isEmployer) UserRole.EMPLOYER else UserRole.WORKER
                            targetRole = newRole
                            showConfirmationDialog = true
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF3B82F6), // Blue for employer
                            checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color(0xFF10B981), // Green for worker
                            uncheckedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f)
                        ),
                        thumbContent = {
                            Icon(
                                imageVector = when (currentRole) {
                                    UserRole.WORKER -> Icons.Default.Person
                                    UserRole.EMPLOYER -> Icons.Default.Business
                                    else -> Icons.Default.SwapHoriz
                                },
                                contentDescription = "Switch Role",
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                tint = Color.White
                            )
                        }
                    )
                }
            }
        }
    }
    
    // Confirmation Dialog
    if (showConfirmationDialog && targetRole != null) {
        RoleSwitchConfirmationDialog(
            currentRole = currentRole,
            targetRole = targetRole!!,
            onConfirm = {
                showConfirmationDialog = false
                showRoleSwitchLoader = true
                // Don't call onRoleSwitch immediately - let the loader handle it
            },
            onDismiss = {
                showConfirmationDialog = false
                targetRole = null
            }
        )
    }
    
    // Role Switch Loader
    RoleSwitchLoader(
        isVisible = showRoleSwitchLoader,
        currentRole = currentRole,
        targetRole = targetRole ?: currentRole,
        onDismiss = {
            showRoleSwitchLoader = false
            targetRole = null
        }
    )
}


@Composable
private fun RoleSwitchConfirmationDialog(
    currentRole: UserRole,
    targetRole: UserRole,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Switch Role",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Are you sure you want to switch from ${currentRole.name} to ${targetRole.name}?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "This will:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                        
                        Text(
                            text = "• Navigate you to the ${targetRole.name} home screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                        
                        Text(
                            text = "• Give you access to ${targetRole.name} features",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                        
                        Text(
                            text = "• Your profile data will be preserved",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (targetRole) {
                        UserRole.WORKER -> Color(0xFF10B981)
                        UserRole.EMPLOYER -> Color(0xFF3B82F6)
                        else -> Color(0xFF6B7280)
                    }
                )
            ) {
                Text("Switch to ${targetRole.name}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
