package com.example.dutype.employer.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EmployerJobPreviewScreen
 *
 * OLX-style preview shown:
 *   1. Right after an employer publishes a job (post -> preview).
 *   2. When the employer taps a card on the "My Job Posts" screen.
 *
 * Layout:
 *   - Top: hero image (or category emoji) + back/share
 *   - Title + location block
 *   - Single collapsible "Job details" card with rows (salary, vacancies,
 *     shift, gender, experience, contact, posted-on)
 *   - Description + benefits below the collapsible card
 *   - Sticky bottom: Edit button
 */
@Composable
fun EmployerJobPreviewScreen(
    navController: NavController,
    jobId: String,
    viewModel: FirestoreEmployerJobViewModel = hiltViewModel()
) {
    var job by remember { mutableStateOf<JobListing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }

    LaunchedEffect(jobId) {
        if (jobId.isBlank()) {
            isLoading = false
            notFound = true
            return@LaunchedEffect
        }
        viewModel.getJobById(jobId) { fetched ->
            job = fetched
            notFound = fetched == null
            isLoading = false
        }
    }

    val pageBg = Color(0xFFF6F7FB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CommonHeader(
                title = "Preview",
                navController = navController,
                backgroundColor = Color.White,
                titleColor = Color(0xFF0F172A)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                    }
                }
                notFound || job == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Job not found",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    val j = job!!
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        HeroBlock(job = j)

                        Spacer(Modifier.height(12.dp))
                        TitleBlock(job = j)

                        Spacer(Modifier.height(12.dp))
                        DetailsCard(job = j)

                        if (j.description.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            DescriptionCard(description = j.description)
                        }

                        if (j.benefits.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            BenefitsCard(benefits = j.benefits)
                        }

                        // Bottom space so sticky Edit doesn't cover content
                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        }

        // Sticky bottom Edit bar — direct child of outer Box so we can align.
        if (!isLoading && job != null) {
            val j = job!!
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                StickyEditBar(
                    onEdit = { navController.navigate(Routes.editJobRoute(j.id)) }
                )
            }
        }
    }
}

@Composable
private fun HeroBlock(job: JobListing) {
    val imageUrl = job.jobImageUrl
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        color = Color(0xFF111827)
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📋",
                    fontSize = 72.sp
                )
            }
        }
    }
}

@Composable
private fun TitleBlock(job: JobListing) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = job.title.ifBlank { "Untitled job" },
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            if (job.location.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEDF2F7))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = job.location,
                        fontSize = 14.sp,
                        color = Color(0xFF374151)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsCard(job: JobListing) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF6B7280)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFEDF2F7))

                    InfoRow("Job category", job.jobType.ifBlank { "—" })
                    InfoRow("Salary", job.salary.ifBlank { "—" })
                    if (job.salaryType.isNotBlank()) {
                        InfoRow("Pay type", job.salaryType.lowercase().replaceFirstChar { it.titlecase() })
                    }
                    InfoRow("Vacancies", job.vacancies.toString())
                    InfoRow("Shift", job.shiftTiming.ifBlank { "—" })
                    InfoRow("Gender", job.gender.ifBlank { "Any" })
                    InfoRow("Experience", job.experienceRequired.ifBlank { "—" })
                    if (job.contactNumber.isNotBlank()) {
                        InfoRow("Contact", job.contactNumber)
                    }
                    InfoRow("Posted on", formatDate(job.createdAt))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A)
        )
    }
    HorizontalDivider(color = Color(0xFFF1F5F9))
}

@Composable
private fun DescriptionCard(description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Description",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color(0xFF374151),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun BenefitsCard(benefits: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Benefits",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(Modifier.height(8.dp))
            benefits.forEach { perk ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        fontSize = 14.sp,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.width(16.dp)
                    )
                    Text(
                        text = perk,
                        fontSize = 14.sp,
                        color = Color(0xFF374151)
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyEditBar(onEdit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Button(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Edit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0L) return "—"
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
}
