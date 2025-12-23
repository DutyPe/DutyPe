package com.example.dutype.employer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.employer.models.*
import com.example.dutype.employer.models.enums.*

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
    description: String
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
                Text(
                    text = "📋",
                    style = MaterialTheme.typography.titleMedium
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
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Job details grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryDetailRow(
                            icon = "💰",
                            label = "Pay",
                            value = "₹${payAmount.ifBlank { "---" }} ${payType.displayName}"
                        )
                        SummaryDetailRow(
                            icon = "📍",
                            label = "Location",
                            value = location.ifBlank { "Not set" }
                        )
                        SummaryDetailRow(
                            icon = "👥",
                            label = "Positions",
                            value = "${vacancies.ifBlank { "1" }} opening(s)"
                        )
                        SummaryDetailRow(
                            icon = "⏰",
                            label = "Shift",
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
                            JobUrgency.FLEXIBLE -> successGreen
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

                    // Description preview
                    if (description.isNotBlank()) {
                        Text(
                            text = description.take(80) + if (description.length > 80) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
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
    icon: String,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
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

@Composable
fun JobPreviewDialog(
    jobPosting: JobPostingModel,
    onDismiss: () -> Unit,
    onConfirmPost: () -> Unit
) {
    val primaryBlue = Color(0xFF2563EB)
    val successGreen = Color(0xFF10B981)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(primaryBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Preview,
                        contentDescription = null,
                        tint = primaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    "Job Preview",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(380.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "This is how your job will appear to candidates:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(primaryBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = jobPosting.category.icon,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = jobPosting.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = jobPosting.category.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "💰 ₹${jobPosting.payAmount} ${jobPosting.payType.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "📍 ${jobPosting.location}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "👥 ${jobPosting.vacancies} position(s)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF475569)
                                )
                            }

                            if (jobPosting.description.isNotBlank()) {
                                Text(
                                    text = jobPosting.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    maxLines = 3,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = successGreen.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "💡 Expected Results",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = successGreen
                            )
                            Text(
                                text = "• 10-25 applications within 24 hours",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "• Average 2-5 days to fill position",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPost,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = successGreen
                ),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, 
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Post Job", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Edit More",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
