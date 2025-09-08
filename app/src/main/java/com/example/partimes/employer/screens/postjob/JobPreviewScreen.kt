package com.example.partimes.employer.screens.postjob

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.utils.BackNavigationTopBar
import com.example.partimes.employer.models.JobPostingModel
import com.example.partimes.employer.components.EmployerJobCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobPreviewScreen(
    navController: NavController,
    jobPosting: JobPostingModel
) {
    var showPostDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackNavigationTopBar(
                title = "Job Preview",
                navController = navController
            )
        },
        bottomBar = {
            PreviewBottomBar(
                onPostClick = { showPostDialog = true },
                onShareClick = { /* Share preview */ }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview Header
            item {
                PreviewHeader()
            }

            // Job Card Preview using EmployerJobCard
            item {
                EmployerJobCard(
                    jobPosting = jobPosting,
                    showActions = false
                )
            }

            // Job Statistics Preview
            item {
                JobStatsPreview(jobPosting)
            }

            // Employer Tips
            item {
                EmployerTipsCard()
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Post Confirmation Dialog
    if (showPostDialog) {
        PostConfirmationDialog(
            jobPosting = jobPosting,
            onConfirm = {
                showPostDialog = false
                // Post the job
                navController.popBackStack()
            },
            onDismiss = { showPostDialog = false }
        )
    }
}

@Composable
private fun PreviewHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF6366F1).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Preview,
                contentDescription = null,
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Job Preview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                )
                Text(
                    text = "This is how your job will appear to jobseekers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
private fun JobStatsPreview(jobPosting: JobPostingModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Expected Performance",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatPreviewCard(
                    icon = Icons.Default.People,
                    value = "10-25",
                    label = "Expected Applications",
                    description = "Within 24 hours"
                )

                StatPreviewCard(
                    icon = Icons.Default.Timeline,
                    value = "2-5",
                    label = "Days to Fill",
                    description = "Average hiring time"
                )

                StatPreviewCard(
                    icon = Icons.Default.Work,
                    value = "${jobPosting.vacancies}",
                    label = "Positions",
                    description = "Available vacancies"
                )
            }
        }
    }
}

@Composable
private fun StatPreviewCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981)
            )
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmployerTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10B981).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF10B981)
                )
                Text(
                    text = "💡 Tips to Get More Applications",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val tips = listOf(
                "Add clear job requirements and benefits",
                "Set competitive salary for your area",
                "Respond to applications quickly",
                "Use urgent hiring for immediate needs",
                "Add perks like meals or transport",
                "Verify your employer profile"
            )

            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBottomBar(
    onPostClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onShareClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share")
            }

            Button(
                onClick = onPostClick,
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Post Job")
            }
        }
    }
}

@Composable
private fun PostConfirmationDialog(
    jobPosting: JobPostingModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color(0xFF6366F1)
                )
                Text("Post Job?")
            }
        },
        text = {
            Column {
                Text("Are you ready to post this job?")

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Job Summary:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text("• Title: ${jobPosting.title}")
                        Text("• Pay: ₹${jobPosting.payAmount} ${jobPosting.payType.displayName}")
                        Text("• Location: ${jobPosting.location}")
                        Text("• Positions: ${jobPosting.vacancies}")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Once posted, jobseekers will be able to see and apply for this job.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                )
            ) {
                Text("Post Job")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
