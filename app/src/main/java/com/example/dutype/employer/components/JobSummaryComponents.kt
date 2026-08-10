package com.example.dutype.employer.components

import com.dutype.app.R
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
import androidx.compose.material.icons.filled.Work
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
import com.example.dutype.ui.theme.EmployerColors

@Composable
fun JobSummaryCard(
    title: String,
    category: JobCategory,
    payAmount: String,
    payType: PayType,
    location: String,
    vacancies: String,
    shiftTiming: ShiftTiming,
    description: String
) {
    val primaryBlue = Color(0xFF2563EB)
    
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
                    text = stringResource(R.string.auto_job_preview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EmployerColors.TextPrimary
                )
            }

            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with generic job icon and title. Category stays internal.
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
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = null,
                                tint = primaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = title.ifBlank { "Job Title" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmployerColors.TextPrimary
                            )
                        }
                    }

                    HorizontalDivider(color = EmployerColors.Divider)

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

                    // Perks & Benefits section removed.

                    // Description preview
                    if (description.isNotBlank()) {
                        Text(
                            text = description.take(80) + if (description.length > 80) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmployerColors.TextSecondary,
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
            tint = EmployerColors.IconSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = EmployerColors.TextSecondary,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = EmployerColors.TextPrimary
        )
    }
}

