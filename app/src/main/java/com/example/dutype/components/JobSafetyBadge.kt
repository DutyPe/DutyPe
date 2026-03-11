package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import com.example.dutype.utils.AIScamDetector

/**
 * Job Safety Badge Component
 * 
 * Displays AI-powered scam detection results to workers.
 * Shows risk level with color-coded badges and optional details.
 * 
 * Integration Points:
 * - WorkerJobCard: Compact badge showing risk level
 * - JobDescriptionScreen: Full safety card with details
 */

/**
 * Compact safety badge for job cards
 * Shows only when risk is MEDIUM or higher
 */
@Composable
fun JobSafetyBadge(
    riskLevel: AIScamDetector.RiskLevel,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    // Only show badge for MEDIUM risk and above
    if (riskLevel == AIScamDetector.RiskLevel.SAFE || riskLevel == AIScamDetector.RiskLevel.LOW) {
        return
    }
    
    val backgroundColor: Color
    val textColor: Color
    val icon: ImageVector
    val label: String
    
    when (riskLevel) {
        AIScamDetector.RiskLevel.MEDIUM -> {
            backgroundColor = Color(0xFFFEF3C7) // Amber background
            textColor = Color(0xFFD97706) // Amber text
            icon = Icons.Default.Warning
            label = "Caution"
        }
        AIScamDetector.RiskLevel.HIGH -> {
            backgroundColor = Color(0xFFFEE2E2) // Red background
            textColor = Color(0xFFDC2626) // Red text
            icon = Icons.Default.Error
            label = "High Risk"
        }
        AIScamDetector.RiskLevel.CRITICAL -> {
            backgroundColor = Color(0xFFFEE2E2) // Red background
            textColor = Color(0xFF991B1B) // Dark red text
            icon = Icons.Default.Dangerous
            label = "Scam Alert"
        }
        else -> return
    }
    
    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )
            )
        }
    }
}

/**
 * Full safety card for job description screen
 * Shows detailed risk analysis with flags
 */
@Composable
fun JobSafetyCard(
    analysisResult: AIScamDetector.ScamAnalysisResult,
    modifier: Modifier = Modifier,
    onLearnMore: (() -> Unit)? = null
) {
    val backgroundColor: Color
    val borderColor: Color
    val iconColor: Color
    val titleColor: Color
    
    when (analysisResult.riskLevel) {
        AIScamDetector.RiskLevel.SAFE -> {
            backgroundColor = Color(0xFFECFDF5) // Green background
            borderColor = Color(0xFF10B981) // Green border
            iconColor = Color(0xFF059669) // Green icon
            titleColor = Color(0xFF065F46) // Green title
        }
        AIScamDetector.RiskLevel.LOW -> {
            backgroundColor = Color(0xFFF0FDF4) // Light green background
            borderColor = Color(0xFF84CC16) // Light green border
            iconColor = Color(0xFF65A30D) // Light green icon
            titleColor = Color(0xFF3F6212) // Light green title
        }
        AIScamDetector.RiskLevel.MEDIUM -> {
            backgroundColor = Color(0xFFFFFBEB) // Amber background
            borderColor = Color(0xFFF59E0B) // Amber border
            iconColor = Color(0xFFD97706) // Amber icon
            titleColor = Color(0xFF92400E) // Amber title
        }
        AIScamDetector.RiskLevel.HIGH -> {
            backgroundColor = Color(0xFFFEF2F2) // Red background
            borderColor = Color(0xFFEF4444) // Red border
            iconColor = Color(0xFFDC2626) // Red icon
            titleColor = Color(0xFF991B1B) // Red title
        }
        AIScamDetector.RiskLevel.CRITICAL -> {
            backgroundColor = Color(0xFFFEF2F2) // Red background
            borderColor = Color(0xFFDC2626) // Dark red border
            iconColor = Color(0xFF991B1B) // Dark red icon
            titleColor = Color(0xFF7F1D1D) // Dark red title
        }
    }
    
    val icon = when (analysisResult.riskLevel) {
        AIScamDetector.RiskLevel.SAFE -> Icons.Outlined.Shield
        AIScamDetector.RiskLevel.LOW -> Icons.Default.CheckCircle
        AIScamDetector.RiskLevel.MEDIUM -> Icons.Default.Warning
        AIScamDetector.RiskLevel.HIGH -> Icons.Default.Error
        AIScamDetector.RiskLevel.CRITICAL -> Icons.Default.Dangerous
    }
    
    val title = when (analysisResult.riskLevel) {
        AIScamDetector.RiskLevel.SAFE -> stringResource(R.string.job_safety_verified)
        AIScamDetector.RiskLevel.LOW -> stringResource(R.string.job_safety_looks_good)
        AIScamDetector.RiskLevel.MEDIUM -> stringResource(R.string.job_safety_caution)
        AIScamDetector.RiskLevel.HIGH -> stringResource(R.string.job_safety_high_risk)
        AIScamDetector.RiskLevel.CRITICAL -> stringResource(R.string.job_safety_scam_alert)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = titleColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = stringResource(R.string.job_safety_ai_analysis),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = titleColor.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
                
                // Risk Score Badge
                Box(
                    modifier = Modifier
                        .background(borderColor.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${analysisResult.riskScore}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = titleColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            // Recommendation
            Text(
                text = analysisResult.recommendation,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = titleColor.copy(alpha = 0.9f)
                )
            )
            
            // Risk Flags (show top 3)
            if (analysisResult.flags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    analysisResult.flags.take(3).forEach { flag ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(iconColor, CircleShape)
                            )
                            Text(
                                text = flag.description,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = titleColor.copy(alpha = 0.8f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            // Learn More link (optional)
            if (onLearnMore != null && analysisResult.riskLevel != AIScamDetector.RiskLevel.SAFE) {
                Text(
                    text = stringResource(R.string.job_safety_learn_more),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = iconColor,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { onLearnMore() }
                )
            }
        }
    }
}

/**
 * Helper function to analyze job and get risk level
 */
fun analyzeJobRisk(
    title: String,
    description: String,
    category: String = "",
    payAmount: String = "",
    payType: String = "",
    location: String = "",
    employerAccountAgeDays: Int = 30,
    hasVerifiedBadge: Boolean = false
): AIScamDetector.ScamAnalysisResult {
    return AIScamDetector.analyzeJob(
        title = title,
        description = description,
        category = category,
        payAmount = payAmount,
        payType = payType,
        location = location,
        employerAccountAgeDays = employerAccountAgeDays,
        hasVerifiedBadge = hasVerifiedBadge
    )
}
