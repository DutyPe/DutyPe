package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.UserRole

@Composable
fun RoleSwitchDialog(
    showDialog: Boolean,
    currentRole: UserRole,
    enabledRoles: List<UserRole>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSwitchRole: (UserRole) -> Unit
) {
    if (!showDialog) return

    var pendingSwitchRole by remember { mutableStateOf<UserRole?>(null) }

    // Confirmation AlertDialog
    pendingSwitchRole?.let { targetRole ->
        AlertDialog(
            onDismissRequest = { pendingSwitchRole = null },
            title = {
                Text(
                    text = "Switch to ${targetRole.displayName()}?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "You are switching from ${currentRole.displayName()} to ${targetRole.displayName()} mode. " +
                        "Your active role will change and you will see the ${targetRole.displayName()} dashboard.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingSwitchRole = null
                        onSwitchRole(targetRole)
                    }
                ) {
                    Text(
                        text = "Yes, Switch",
                        color = when (targetRole) {
                            UserRole.WORKER -> Color(0xFF10B981)
                            UserRole.EMPLOYER -> Color(0xFF3B82F6)
                            else -> Color(0xFF1F2937)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwitchRole = null }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color.White
        )
    }

    // Bottom sheet: 2-column square role boxes
    val allRoles = listOf(UserRole.WORKER, UserRole.EMPLOYER)

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Switch Role",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF111827)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Select your active mode",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Switching roles…", color = Color(0xFF6B7280))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    allRoles.forEach { role ->
                        RoleSquareBox(
                            role = role,
                            isCurrent = role == currentRole,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (role != currentRole) {
                                    pendingSwitchRole = role
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    color = Color(0xFF6B7280),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun RoleSquareBox(
    role: UserRole,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val workerAccent = Color(0xFF10B981)
    val employerAccent = Color(0xFF3B82F6)

    val accent = when (role) {
        UserRole.WORKER -> workerAccent
        UserRole.EMPLOYER -> employerAccent
        else -> Color(0xFF6B7280)
    }

    val icon = when (role) {
        UserRole.WORKER -> Icons.Default.Engineering
        UserRole.EMPLOYER -> Icons.Default.Business
        else -> Icons.Default.Engineering
    }

    val bgColor = if (isCurrent) accent.copy(alpha = 0.12f) else Color(0xFFF9FAFB)
    val borderColor = if (isCurrent) accent else Color(0xFFE5E7EB)
    val borderWidth = if (isCurrent) 2.dp else 1.dp

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !isCurrent, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Icon with circle bg
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (isCurrent) accent.copy(alpha = 0.18f) else Color(0xFFF3F4F6),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = role.displayName(),
                    tint = if (isCurrent) accent else Color(0xFF9CA3AF),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = role.displayName(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isCurrent) Color(0xFF111827) else Color(0xFF6B7280),
                    fontSize = 15.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isCurrent) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            } else {
                Text(
                    text = "Tap to switch",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF9CA3AF)
                    )
                )
            }
        }
    }
}

private fun UserRole.displayName(): String {
    return name.lowercase().replaceFirstChar { it.uppercase() }
}
