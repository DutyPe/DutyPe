package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.EmployerTrustTier
import com.example.dutype.models.getDisplayInfo

/**
 * Trust Badge Component
 * 
 * Displays employer trust tier badge with icon and label
 * Used on job cards, employer profiles, and application screens
 */
@Composable
fun TrustBadge(
    tier: EmployerTrustTier,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    size: TrustBadgeSize = TrustBadgeSize.MEDIUM
) {
    val info = tier.getDisplayInfo()
    val textColor = Color(info.color)
    val bgColor = Color(info.backgroundColor)
    
    val (iconSize, fontSize, paddingH, paddingV) = when (size) {
        TrustBadgeSize.SMALL -> Quadruple(12.dp, 10.sp, 6.dp, 2.dp)
        TrustBadgeSize.MEDIUM -> Quadruple(14.dp, 12.sp, 8.dp, 4.dp)
        TrustBadgeSize.LARGE -> Quadruple(18.dp, 14.sp, 12.dp, 6.dp)
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = paddingH, vertical = paddingV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icon based on tier
            Icon(
                imageVector = getTierIcon(tier),
                contentDescription = info.displayName,
                tint = textColor,
                modifier = Modifier.size(iconSize)
            )
            
            if (showLabel) {
                Text(
                    text = info.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = fontSize,
                        color = textColor
                    )
                )
            }
        }
    }
}

/**
 * Compact trust badge - just emoji and optional text
 * 
 * NOTE: This function is currently unused but kept for API completeness.
 * Consider using TrustBadge() with TrustBadgeSize.SMALL instead.
 */
// REMOVED: TrustBadgeCompact() - Dead code, never called anywhere in codebase
// If needed in future, use TrustBadge(tier, size = TrustBadgeSize.SMALL, showLabel = false)

/**
 * Trust badge with tooltip/description
 */
@Composable
fun TrustBadgeWithInfo(
    tier: EmployerTrustTier,
    modifier: Modifier = Modifier
) {
    val info = tier.getDisplayInfo()
    val textColor = Color(info.color)
    val bgColor = Color(info.backgroundColor)
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(textColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getTierIcon(tier),
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = info.emoji,
                        fontSize = 14.sp
                    )
                    Text(
                        text = info.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = textColor.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

/**
 * Get icon for trust tier
 */
private fun getTierIcon(tier: EmployerTrustTier): ImageVector {
    return when (tier) {
        EmployerTrustTier.VERIFIED -> Icons.Default.Verified
        EmployerTrustTier.TRUSTED -> Icons.Default.Star
        EmployerTrustTier.BUSINESS -> Icons.Default.Business
    }
}

enum class TrustBadgeSize {
    SMALL, MEDIUM, LARGE
}

// Helper data class for size parameters
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
