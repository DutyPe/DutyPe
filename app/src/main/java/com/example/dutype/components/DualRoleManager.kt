package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.dutype.app.R

/**
 * Dual Role Manager - Enhanced UI (Airbnb/Uber/Fiverr Pattern)
 * 
 * Shows current active role with a clean switch button
 * Users can enable additional roles and switch between them seamlessly
 * 
 * ARCHITECTURE:
 * - Single account can have BOTH roles simultaneously
 * - `roles` field stores all enabled roles
 * - `activeRole` field tracks which role is currently active in UI
 * - Notifications sent based on ALL enabled roles
 */
@Composable
fun DualRoleManager(
    user: User,
    onRoleToggle: (role: UserRole, enabled: Boolean) -> Unit,
    onActiveRoleSwitch: (newActiveRole: UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSwitchRoleDialog by remember { mutableStateOf(false) }
    var showEnableRoleDialog by remember { mutableStateOf(false) }
    var targetRole by remember { mutableStateOf<UserRole?>(null) }
    
    val enabledRoles = user.roles.map { UserRole.valueOf(it) }
    val hasWorkerRole = user.hasRole(UserRole.WORKER)
    val hasEmployerRole = user.hasRole(UserRole.EMPLOYER)
    val isDualRole = user.isDualRole()
    val currentRole = user.activeRole
    
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
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Role Management",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Role Management",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    // Status badge
                    if (isDualRole) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF8B5CF6).copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "DUAL ROLE",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Current Active Role Card
                CurrentRoleCard(
                    role = currentRole,
                    isDualRole = isDualRole,
                    onSwitchClick = {
                        if (isDualRole) {
                            showSwitchRoleDialog = true
                        }
                    }
                )
                
                // Enable Additional Role Section (only show if not dual role)
                if (!isDualRole) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val roleToEnable = if (hasWorkerRole) UserRole.EMPLOYER else UserRole.WORKER
                    
                    EnableRoleCard(
                        role = roleToEnable,
                        onEnableClick = {
                            targetRole = roleToEnable
                            showEnableRoleDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Switch Role Dialog (for dual role users)
    if (showSwitchRoleDialog) {
        RoleSwitchDialog(
            currentRole = currentRole,
            availableRoles = enabledRoles,
            onRoleSelected = { selectedRole ->
                if (selectedRole != currentRole) {
                    onActiveRoleSwitch(selectedRole)
                }
                showSwitchRoleDialog = false
            },
            onDismiss = {
                showSwitchRoleDialog = false
            }
        )
    }
    
    // Enable Role Dialog
    if (showEnableRoleDialog && targetRole != null) {
        EnableRoleDialog(
            role = targetRole!!,
            onConfirm = {
                onRoleToggle(targetRole!!, true)
                showEnableRoleDialog = false
                targetRole = null
            },
            onDismiss = {
                showEnableRoleDialog = false
                targetRole = null
            }
        )
    }
}

@Composable
private fun CurrentRoleCard(
    role: UserRole,
    isDualRole: Boolean,
    onSwitchClick: () -> Unit
) {
    val roleColor = when (role) {
        UserRole.WORKER -> Color(0xFF10B981)
        UserRole.EMPLOYER -> Color(0xFF3B82F6)
        else -> Color(0xFF6B7280)
    }
    
    val roleIcon = when (role) {
        UserRole.WORKER -> Icons.Default.Person
        UserRole.EMPLOYER -> Icons.Default.Business
        else -> Icons.Default.AccountCircle
    }
    
    val roleDescription = when (role) {
        UserRole.WORKER -> "Finding and applying to jobs"
        UserRole.EMPLOYER -> "Posting jobs and hiring workers"
        else -> ""
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = roleColor.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, roleColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(roleColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = role.name,
                            tint = roleColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Current Role",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = role.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = Color(0xFF1E293B)
                        )
                    }
                }
                
                // Switch button (only show if dual role)
                if (isDualRole) {
                    Button(
                        onClick = onSwitchClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = roleColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Switch",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = roleDescription,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun EnableRoleCard(
    role: UserRole,
    onEnableClick: () -> Unit
) {
    val roleColor = when (role) {
        UserRole.WORKER -> Color(0xFF10B981)
        UserRole.EMPLOYER -> Color(0xFF3B82F6)
        else -> Color(0xFF6B7280)
    }
    
    val roleIcon = when (role) {
        UserRole.WORKER -> Icons.Default.Person
        UserRole.EMPLOYER -> Icons.Default.Business
        else -> Icons.Default.AccountCircle
    }
    
    val roleDescription = when (role) {
        UserRole.WORKER -> "Apply to jobs and track your applications"
        UserRole.EMPLOYER -> "Post jobs and hire workers"
        else -> ""
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = role.name,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Enable ${role.name.lowercase().replaceFirstChar { it.uppercase() }} Role",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = roleDescription,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onEnableClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = roleColor
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Enable",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enable ${role.name.lowercase().replaceFirstChar { it.uppercase() }} Role",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * PERFORMANCE OPTIMIZED: Minimal Switch Role Dialog
 * 
 * Uses ModalBottomSheet for instant response and clean UX
 * - Professional row layout for role selection
 * - Simple card-based selection
 * - Instant feedback with haptics
 * - Follows enterprise standards (Instagram/Uber pattern)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSwitchDialog(
    currentRole: UserRole,
    availableRoles: List<UserRole>,
    onRoleSelected: (UserRole) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var loadingRole by remember { mutableStateOf<UserRole?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = if (isLoading) { {} } else onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header with close icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Switch Role",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = Color(0xFF1E293B)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Choose your active role",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
                
                // Close icon
                IconButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Role cards in a row - professional layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                availableRoles.forEach { role ->
                    val roleColor = when (role) {
                        UserRole.WORKER -> Color(0xFF10B981)
                        UserRole.EMPLOYER -> Color(0xFF3B82F6)
                        else -> Color(0xFF6B7280)
                    }
                    
                    val roleIcon = when (role) {
                        UserRole.WORKER -> Icons.Default.Person
                        UserRole.EMPLOYER -> Icons.Default.Business
                        else -> Icons.Default.AccountCircle
                    }
                    
                    val roleTitle = when (role) {
                        UserRole.WORKER -> "Worker"
                        UserRole.EMPLOYER -> "Employer"
                        else -> role.name
                    }
                    
                    val roleDescription = when (role) {
                        UserRole.WORKER -> "Find jobs"
                        UserRole.EMPLOYER -> "Hire workers"
                        else -> ""
                    }
                    
                    val isCurrentRole = role == currentRole
                    
                    val isLoadingThisRole = loadingRole == role
                    
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp),
                        onClick = { 
                            if (!isCurrentRole && loadingRole == null) {
                                loadingRole = role
                                android.widget.Toast.makeText(
                                    context,
                                    "Switching to ${roleTitle}...",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                onRoleSelected(role)
                            }
                        },
                        enabled = loadingRole == null,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentRole) roleColor else Color(0xFFF8FAFC),
                            disabledContainerColor = if (isCurrentRole) roleColor.copy(alpha = 0.7f) else Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = if (!isCurrentRole) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0)) else null,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isCurrentRole) 4.dp else 0.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = if (isCurrentRole) Color.White.copy(alpha = 0.2f) else roleColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = roleIcon,
                                        contentDescription = roleTitle,
                                        tint = if (isCurrentRole) Color.White else roleColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Title
                                Text(
                                    text = roleTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = if (isCurrentRole) Color.White else Color(0xFF1E293B)
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Description
                                Text(
                                    text = roleDescription,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp
                                    ),
                                    color = if (isCurrentRole) Color.White.copy(alpha = 0.9f) else Color(0xFF64748B)
                                )
                                
                                // Active indicator
                                if (isCurrentRole) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            
                            // Loading indicator overlay
                            if (isLoadingThisRole) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = Color.White,
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EnableRoleDialog(
    role: UserRole,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enable ${role.name.lowercase().replaceFirstChar { it.uppercase() }} Role",
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
                    text = "You're about to enable the ${role.name.lowercase()} role. This will allow you to:",
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
                        when (role) {
                            UserRole.WORKER -> {
                                Text("• Browse and apply to jobs", style = MaterialTheme.typography.bodySmall)
                                Text("• Track your applications", style = MaterialTheme.typography.bodySmall)
                                Text("• Receive worker notifications", style = MaterialTheme.typography.bodySmall)
                                Text("• Build your work profile", style = MaterialTheme.typography.bodySmall)
                            }
                            UserRole.EMPLOYER -> {
                                Text("• Post job listings", style = MaterialTheme.typography.bodySmall)
                                Text("• Review applications", style = MaterialTheme.typography.bodySmall)
                                Text("• Receive employer notifications", style = MaterialTheme.typography.bodySmall)
                                Text("• Manage your company profile", style = MaterialTheme.typography.bodySmall)
                            }
                            else -> {}
                        }
                    }
                }
                
                Text(
                    text = "✅ Your existing profile data will be preserved",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF10B981)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (role) {
                        UserRole.WORKER -> Color(0xFF10B981)
                        UserRole.EMPLOYER -> Color(0xFF3B82F6)
                        else -> Color(0xFF6B7280)
                    }
                )
            ) {
                Text("Enable ${role.name.lowercase().replaceFirstChar { it.uppercase() }}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
