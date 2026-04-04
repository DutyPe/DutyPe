package com.example.dutype.employer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

private data class EmployerPublicJobPreview(
    val id: String,
    val title: String,
    val status: String,
    val createdAt: Long
)

@Composable
fun EmployerPublicProfileScreen(
    navController: NavController,
    employerId: String,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var employerDisplayName by remember { mutableStateOf("Employer") }
    var jobs by remember { mutableStateOf<List<EmployerPublicJobPreview>>(emptyList()) }

    LaunchedEffect(Unit) {
        onStatusBarColorChange?.invoke(Color.White)
    }

    LaunchedEffect(employerId) {
        isLoading = true
        errorMessage = null

        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection(com.example.dutype.firestore.FirestoreCollections.JOBS)
                .whereEqualTo("employerId", employerId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            val loadedJobs = snapshot.documents.map { doc ->
                val data = doc.data.orEmpty()
                employerDisplayName = employerDisplayName.takeUnless { it == "Employer" }
                    ?: sequenceOf(
                        data["companyName"]?.toString(),
                        data["employerName"]?.toString(),
                        data["company"]?.toString()
                    ).firstOrNull { !it.isNullOrBlank() }
                    ?: "Employer"

                EmployerPublicJobPreview(
                    id = doc.id,
                    title = data["title"]?.toString()?.ifBlank { "Job Opening" } ?: "Job Opening",
                    status = data["status"]?.toString()?.uppercase() ?: "OPEN",
                    createdAt = when (val createdAt = data["createdAt"]) {
                        is Timestamp -> createdAt.toDate().time
                        is Number -> createdAt.toLong()
                        else -> 0L
                    }
                )
            }

            jobs = loadedJobs
            if (loadedJobs.isEmpty()) {
                errorMessage = "No public jobs found for this employer right now."
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load public employer profile for $employerId")
            errorMessage = e.message ?: "Could not load this employer profile."
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmployerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = "Employer Profile",
            onBackClick = { navController.popBackStack() },
            backgroundColor = EmployerColors.CardBackground
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmployerColors.Primary)
                }
            }

            errorMessage != null && jobs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Unable to load employer profile.",
                        style = AppTypography.bodyLarge,
                        color = EmployerColors.TextSecondary
                    )
                }
            }

            else -> {
                val openJobs = jobs.count { it.status == "OPEN" }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(EmployerColors.Primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = EmployerColors.Primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = employerDisplayName,
                                    style = AppTypography.cardTitle.copy(fontWeight = FontWeight.Bold),
                                    color = EmployerColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Public hiring profile",
                                    style = AppTypography.bodyMedium,
                                    color = EmployerColors.TextSecondary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    EmployerProfileMetric(
                                        label = "Jobs Listed",
                                        value = jobs.size.toString()
                                    )
                                    EmployerProfileMetric(
                                        label = "Open Jobs",
                                        value = openJobs.toString()
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        item {
                            Text(
                                text = errorMessage ?: "",
                                style = AppTypography.bodyMedium,
                                color = EmployerColors.TextSecondary
                            )
                        }
                    }

                    if (jobs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent Job Openings",
                                style = AppTypography.sectionHeader,
                                color = EmployerColors.TextPrimary
                            )
                        }
                    }

                    items(jobs, key = { it.id }) { job ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Work,
                                        contentDescription = null,
                                        tint = EmployerColors.Primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = job.title,
                                            style = AppTypography.cardTitle,
                                            color = EmployerColors.TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatEmployerJobTimestamp(job.createdAt),
                                            style = AppTypography.caption,
                                            color = EmployerColors.TextSecondary
                                        )
                                    }
                                    EmployerJobStatusPill(status = job.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployerProfileMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = EmployerColors.Primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = AppTypography.caption,
            color = EmployerColors.TextSecondary
        )
    }
}

@Composable
private fun EmployerJobStatusPill(status: String) {
    val normalizedStatus = status.uppercase()
    val background = when (normalizedStatus) {
        "OPEN" -> Color(0xFFD1FAE5)
        "CLOSED" -> Color(0xFFFEE2E2)
        "EXPIRED" -> Color(0xFFE5E7EB)
        else -> Color(0xFFDBEAFE)
    }
    val foreground = when (normalizedStatus) {
        "OPEN" -> Color(0xFF047857)
        "CLOSED" -> Color(0xFFB91C1C)
        "EXPIRED" -> Color(0xFF4B5563)
        else -> EmployerColors.Primary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = normalizedStatus.lowercase().replaceFirstChar { it.titlecase(Locale.ROOT) },
            style = AppTypography.caption.copy(fontWeight = FontWeight.SemiBold),
            color = foreground
        )
    }
}

private fun formatEmployerJobTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Recently posted"
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
