package com.example.dutype.employer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.employer.models.*
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@Composable
fun JobSummaryCard(
    title: String,
    category: JobCategory,
    payAmount: String,
    payType: PayType,
    location: String,
    vacancies: String,
    urgency: JobUrgency,
    shiftTiming: ShiftTiming,
    description: String,
    selectedPerks: Set<JobPerk> = emptySet()
) {
    val primaryBlue = Color(0xFF2563EB)
    val successGreen = Color(0xFF10B981)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = primaryBlue.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Preview,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Job Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
            }

            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with category icon and title
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    primaryBlue.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.icon,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = title.ifBlank { "Job Title" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Job details grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryDetailRow(
                            icon = Icons.Default.CurrencyRupee,
                            label = stringResource(R.string.pay_label),
                            value = "Rs. ${payAmount.ifBlank { "---" }} ${payType.displayName}"
                        )
                        SummaryDetailRow(
                            icon = Icons.Default.LocationOn,
                            label = stringResource(R.string.location),
                            value = location.ifBlank { "Not set" }
                        )
                        SummaryDetailRow(
                            icon = Icons.Default.People,
                            label = stringResource(R.string.positions_label),
                            value = "${vacancies.ifBlank { "1" }} opening(s)"
                        )
                        SummaryDetailRow(
                            icon = Icons.Default.AccessTime,
                            label = stringResource(R.string.shift_label),
                            value = shiftTiming.displayName
                        )
                    }

                    // Urgency badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val urgencyColor = when (urgency) {
                            JobUrgency.IMMEDIATE -> Color(0xFFDC2626)
                            JobUrgency.URGENT -> Color(0xFFF59E0B)
                            JobUrgency.NORMAL -> primaryBlue
                            JobUrgency.WITHIN_MONTH -> Color(0xFF14B8A6)
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    urgencyColor.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = urgency.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = urgencyColor
                            )
                        }
                    }
                    
                    // Perks & Benefits section
                    if (selectedPerks.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        
                        Text(
                            text = "Perks & Benefits",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedPerks.toList()) { perk ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            successGreen.copy(alpha = 0.1f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = perk.icon, fontSize = 12.sp)
                                        Text(
                                            text = perk.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = successGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Description preview
                    if (description.isNotBlank()) {
                        Text(
                            text = description.take(80) + if (description.length > 80) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        )
    }
}

