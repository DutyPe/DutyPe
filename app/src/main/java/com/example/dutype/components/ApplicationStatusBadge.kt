package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.ApplicationStatus

/**
 * Centralized Application Status Badge Component
 * 
 * Use this component across all screens instead of creating duplicate StatusBadge composables.
 * This ensures consistent styling and reduces code duplication.
 * 
 * Usage:
 * ```kotlin
 * ApplicationStatusBadge(status = application.status)
 * ```
 */
@Composable
fun ApplicationStatusBadge(
    status: ApplicationStatus,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val (backgroundColor, textColor, icon) = getStatusStyle(status)
    
    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showIcon) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
        
        Text(
            text = status.getDisplayName(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                fontSize = 11.sp
            )
        )
    }
}

/**
 * Get the visual style for an ApplicationStatus
 * Returns Triple of (backgroundColor, textColor, icon)
 */
private fun getStatusStyle(status: ApplicationStatus): Triple<Color, Color, ImageVector> {
    return when (status) {
        ApplicationStatus.APPLIED -> Triple(
            Color(0xFFFEF3C7),
            Color(0xFFD97706),
            Icons.Default.Schedule
        )
        ApplicationStatus.SHORTLISTED -> Triple(
            Color(0xFFE0E7FF),
            Color(0xFF3730A3),
            Icons.Default.Visibility
        )
        ApplicationStatus.HIRED -> Triple(
            Color(0xFFD1FAE5),
            Color(0xFF059669),
            Icons.Default.CheckCircle
        )
        ApplicationStatus.REJECTED -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFFDC2626),
            Icons.Default.Close
        )
    }
}

/**
 * Extension function to get display name for ApplicationStatus
 * This is a convenience wrapper around the extension in JobApplicationModels.kt
 */
private fun ApplicationStatus.getDisplayName(): String {
    return when (this) {
        ApplicationStatus.APPLIED -> "Applied"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.HIRED -> "Hired"
        ApplicationStatus.REJECTED -> "Rejected"
    }
}
