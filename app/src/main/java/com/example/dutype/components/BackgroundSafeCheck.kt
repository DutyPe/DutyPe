package com.example.dutype.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Background Safe-Check Component
 * Shows verification status for workers applying to home-entry jobs
 * Displays: Aadhaar Verified, Jobs completed in area, Local rating
 */

data class WorkerVerificationStatus(
    val isAadhaarVerified: Boolean = false,
    val isPhoneVerified: Boolean = false,
    val isEmailVerified: Boolean = false,
    val jobsCompletedInArea: Int = 0,
    val localRating: Float = 0f,
    val totalReviews: Int = 0,
    val backgroundCheckPassed: Boolean = false,
    val identityVerified: Boolean = false
)

@Composable
fun BackgroundSafeCheckCard(
    verificationStatus: WorkerVerificationStatus,
    isHomeEntryJob: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!isHomeEntryJob) return // Only show for home-entry jobs
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Background Safe-Check",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    Text(
                        text = "Verified for home-entry jobs",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Verification Badges Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Aadhaar Verified Badge
                VerificationBadge(
                    icon = Icons.Default.Badge,
                    label = "Aadhaar",
                    isVerified = verificationStatus.isAadhaarVerified,
                    modifier = Modifier.weight(1f)
                )
                
                // Phone Verified Badge
                VerificationBadge(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    isVerified = verificationStatus.isPhoneVerified,
                    modifier = Modifier.weight(1f)
                )
                
                // Identity Verified Badge
                VerificationBadge(
                    icon = Icons.Default.Person,
                    label = "Identity",
                    isVerified = verificationStatus.identityVerified,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Jobs in Area
                StatBadge(
                    icon = Icons.Default.LocationOn,
                    value = "${verificationStatus.jobsCompletedInArea}",
                    label = "Jobs in area",
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                
                // Local Rating
                StatBadge(
                    icon = Icons.Default.Star,
                    value = if (verificationStatus.localRating > 0) 
                        String.format("%.1f", verificationStatus.localRating) 
                    else "New",
                    label = if (verificationStatus.totalReviews > 0) 
                        "${verificationStatus.totalReviews} reviews" 
                    else "No reviews",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Background Check Status
            if (verificationStatus.backgroundCheckPassed) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Background check passed",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF10B981)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationBadge(
    icon: ImageVector,
    label: String,
    isVerified: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isVerified) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFF3F4F6)
    val iconColor = if (isVerified) Color(0xFF10B981) else Color(0xFF9CA3AF)
    val textColor = if (isVerified) Color(0xFF10B981) else Color(0xFF9CA3AF)
    
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = textColor
                )
            )
            if (isVerified) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun StatBadge(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }
    }
}

/**
 * Compact verification badges row for use in cards
 */
@Composable
fun CompactVerificationBadges(
    verificationStatus: WorkerVerificationStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (verificationStatus.isAadhaarVerified) {
            CompactBadge(
                icon = Icons.Default.Badge,
                text = "Aadhaar",
                color = Color(0xFF10B981)
            )
        }
        if (verificationStatus.isPhoneVerified) {
            CompactBadge(
                icon = Icons.Default.Phone,
                text = "Phone",
                color = Color(0xFF3B82F6)
            )
        }
        if (verificationStatus.backgroundCheckPassed) {
            CompactBadge(
                icon = Icons.Default.VerifiedUser,
                text = "Verified",
                color = Color(0xFF8B5CF6)
            )
        }
    }
}

@Composable
private fun CompactBadge(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            )
        }
    }
}
