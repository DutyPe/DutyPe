package com.example.dutype.employer.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dutype.app.R
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.ui.theme.LocalRoleColors
import com.example.dutype.utils.ImageUploadUtils
import com.example.dutype.utils.JobEditPolicy
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<JobListing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeletingJob by remember { mutableStateOf(false) }

    fun uploadAndSaveHeroImage(uri: Uri) {
        val currentJob = job ?: return
        scope.launch {
            isUploadingImage = true
            val storagePath = "job_images/${currentJob.id}/hero_${System.currentTimeMillis()}.jpg"
            when (val uploadResult = ImageUploadUtils.uploadWithRetry(context, uri, storagePath)) {
                is ImageUploadUtils.UploadResult.Success -> {
                    val newUrl = uploadResult.downloadUrl
                    viewModel.updateJob(
                        currentJob.id,
                        mapOf("jobImageUrl" to newUrl)
                    ) { success, message ->
                        if (success) {
                            job = currentJob.copy(jobImageUrl = newUrl)
                            Toast.makeText(context, context.getString(R.string.job_image_updated), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                message ?: context.getString(R.string.failed_save_job_image),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                is ImageUploadUtils.UploadResult.Failure -> {
                    Toast.makeText(
                        context,
                        uploadResult.error.ifBlank { context.getString(R.string.image_upload_failed) },
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is ImageUploadUtils.UploadResult.Progress -> Unit
            }
            isUploadingImage = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadAndSaveHeroImage(uri)
        }
    }

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

    val pageBg = LocalRoleColors.current.screenBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CommonHeader(
                title = stringResource(R.string.preview),
                navController = navController
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
                notFound || job == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Job not found",
                            color = EmployerColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    val j = job!!
                    val canEditJob = JobEditPolicy.canEdit(j.createdAt)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        HeroBlock(
                            job = j,
                            isUploading = isUploadingImage,
                            onUploadClick = {
                                if (canEditJob) {
                                    imagePickerLauncher.launch("image/*")
                                } else {
                                    Toast.makeText(context, JobEditPolicy.blockedMessage(j.createdAt), Toast.LENGTH_LONG).show()
                                }
                            }
                        )

                        Spacer(Modifier.height(12.dp))
                        TitleBlock(job = j)

                        Spacer(Modifier.height(12.dp))
                        DetailsCard(job = j)

                        if (j.description.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            DescriptionCard(description = j.description)
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
                    onEdit = {
                        if (JobEditPolicy.canEdit(j.createdAt)) {
                            navController.navigate(Routes.editJobRoute(j.id))
                        } else {
                            Toast.makeText(context, JobEditPolicy.blockedMessage(j.createdAt), Toast.LENGTH_LONG).show()
                        }
                    },
                    onDelete = { showDeleteConfirm = true }
                )
            }
        }
    }

    if (showDeleteConfirm && job != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                if (!isDeletingJob) showDeleteConfirm = false
            },
            title = {
                Text(text = stringResource(R.string.delete_this_job_title))
            },
            text = {
                Text(text = stringResource(R.string.delete_this_job_body))
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentJob = job ?: return@Button
                        isDeletingJob = true
                        viewModel.deleteJob(currentJob.id) { success, message ->
                            isDeletingJob = false
                            showDeleteConfirm = false
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.job_deleted), Toast.LENGTH_SHORT).show()
                                navController.navigate(Routes.EMPLOYER_MY_JOBS) {
                                    popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                                    launchSingleTop = true
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    message ?: context.getString(R.string.failed_delete_job),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !isDeletingJob,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmployerColors.Error,
                        contentColor = Color.White
                    )
                ) {
                    if (isDeletingJob) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(R.string.delete))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = !isDeletingJob
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun HeroBlock(
    job: JobListing,
    isUploading: Boolean,
    onUploadClick: () -> Unit
) {
    val imageUrl = job.jobImageUrl
    var isImageLoading by remember(imageUrl) { mutableStateOf(!imageUrl.isNullOrBlank()) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        // Neutral background so the full image is visible without edge
        // cropping (ContentScale.Fit). Replaces the previous near-black
        // surface that visually merged with cropped edges.
        color = EmployerColors.ChipBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    // Show the full uploaded image (every corner) instead of
                    // cropping. Matches what the worker sees in JobDescriptionScreen.
                    contentScale = ContentScale.Fit,
                    onLoading = { isImageLoading = true },
                    onSuccess = { isImageLoading = false },
                    onError = { isImageLoading = false }
                )
                if (isImageLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(EmployerColors.ChipBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = EmployerColors.Primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                OutlinedButton(
                    onClick = onUploadClick,
                    enabled = !isUploading,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = "Change image")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .clickable(enabled = !isUploading) { onUploadClick() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "No job image yet",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onUploadClick,
                        enabled = !isUploading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmployerColors.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Upload image")
                    }
                }
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TitleBlock(job: JobListing) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EmployerColors.CardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = job.title.ifBlank { "Untitled job" },
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = EmployerColors.TextPrimary
            )
            if (job.location.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEDF2F7))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = EmployerColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = job.location,
                        fontSize = 14.sp,
                        color = EmployerColors.TextSecondary
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
        color = EmployerColors.CardBackground
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
                    color = EmployerColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = EmployerColors.TextSecondary
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

                    InfoRow("Work type", job.jobType.ifBlank { "—" })
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
            color = EmployerColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = EmployerColors.TextPrimary
        )
    }
    HorizontalDivider(color = EmployerColors.ChipBackground)
}

@Composable
private fun DescriptionCard(description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EmployerColors.CardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Description",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = EmployerColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = EmployerColors.TextSecondary,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun StickyEditBar(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EmployerColors.CardBackground,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmployerColors.Error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = EmployerColors.Error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Delete",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmployerColors.Primary,
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
