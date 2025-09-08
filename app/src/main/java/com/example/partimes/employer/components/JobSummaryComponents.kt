package com.example.partimes.employer.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.partimes.employer.models.*
import com.example.partimes.employer.models.enums.*

@Composable
fun JobSummaryCard(
    title: String,
    category: JobCategory,
    payAmount: String,
    payType: PayType,
    location: String,
    vacancies: String,
    urgency: JobUrgency,
    selectedPerks: Set<JobPerk>,
    shiftTiming: ShiftTiming,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📋 Job Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.icon,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title.ifBlank { "Job Title" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "💰 ₹${payAmount.ifBlank { "Amount" }} ${payType.displayName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "📍 ${location.ifBlank { "Location" }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "👥 ${vacancies.ifBlank { "1" }} position(s)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (description.isNotBlank()) {
                                Text(
                                    text = "📝 ${description.take(50)}${if (description.length > 50) "..." else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = urgency.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = when (urgency) {
                                    JobUrgency.IMMEDIATE -> Color.Red
                                    JobUrgency.URGENT -> Color(0xFFFF9800)
                                    JobUrgency.NORMAL -> Color.Blue
                                    JobUrgency.FLEXIBLE -> Color.Green
                                }
                            )
                            Text(
                                text = shiftTiming.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    if (selectedPerks.isNotEmpty()) {
                        Text(
                            text = "🎁 Perks: ${selectedPerks.joinToString { it.displayName }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobPreviewDialog(
    jobPosting: JobPostingModel,
    onDismiss: () -> Unit,
    onConfirmPost: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Preview,
                    contentDescription = null,
                    tint = Color(0xFF6366F1)
                )
                Text("Job Preview")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "This is how your job will appear to candidates:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = jobPosting.category.icon,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = jobPosting.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = jobPosting.category.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            HorizontalDivider()

                            Text(
                                text = "💰 ₹${jobPosting.payAmount} ${jobPosting.payType.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "📍 ${jobPosting.location}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "👥 ${jobPosting.vacancies} position(s)",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (jobPosting.description.isNotBlank()) {
                                Text(
                                    text = jobPosting.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 3
                                )
                            }

                            if (jobPosting.perks.isNotEmpty()) {
                                Text(
                                    text = "🎁 ${jobPosting.perks.joinToString { it.displayName }}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "💡 Expected Results:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "• 10-25 applications within 24 hours",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Average 2-5 days to fill position",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPost,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Post Job")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Edit More")
            }
        }
    )
}
