package com.example.dutype.worker.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.dutype.common.screens.AirbnbModernSwitch
import com.example.dutype.models.JobListing
import com.example.dutype.utils.SalaryFormatter
import com.example.dutype.utils.ValidationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Full-screen Instagram Stories / Reel-style Job Viewer matching Mockup 2:
 * - Top segmented animated progress bars (6s per job)
 * - Header: DutyPe wordmark, Off duty/On duty switch, Notification bell, Close button
 * - Full-bleed workplace / category background photo (NO human faces/selfies)
 * - Vertical action buttons on right: Heart (favorite), Share
 * - Bottom overlay: Urgent & Vacancies badges, Large Salary, Title, Distance/Type, and "Apply now ->"
 */
@Composable
fun JobStoryViewerModal(
    jobs: List<JobListing>,
    initialIndex: Int = 0,
    isOnline: Boolean = false,
    onOnlineChange: (Boolean) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onApplyJob: (JobListing) -> Unit,
    onToggleSave: (String) -> Unit = {},
    isJobSaved: (String) -> Boolean = { false },
    onDismiss: () -> Unit
) {
    if (jobs.isEmpty()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        JobStoryViewerContent(
            jobs = jobs,
            initialIndex = initialIndex.coerceIn(0, jobs.size - 1),
            isOnline = isOnline,
            onOnlineChange = onOnlineChange,
            onNotificationClick = onNotificationClick,
            onApplyJob = onApplyJob,
            onToggleSave = onToggleSave,
            isJobSaved = isJobSaved,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun JobStoryViewerContent(
    jobs: List<JobListing>,
    initialIndex: Int = 0,
    isOnline: Boolean = false,
    onOnlineChange: (Boolean) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onApplyJob: (JobListing) -> Unit,
    onToggleSave: (String) -> Unit = {},
    isJobSaved: (String) -> Boolean = { false },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, jobs.size - 1)) }
    val currentJob = jobs[currentIndex]

    // Story progress timing (approx 6 seconds per story)
    val progressAnim = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            progressAnim.snapTo(0f)
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 6000,
                    easing = LinearEasing
                )
            )
            // Auto advance
            if (currentIndex < jobs.size - 1) {
                currentIndex++
            } else {
                onDismiss()
            }
        }
    }

    val nextStory = {
        if (currentIndex < jobs.size - 1) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    val prevStory = {
        if (currentIndex > 0) {
            currentIndex--
        }
    }

    val saved = isJobSaved(currentJob.id)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(jobs.size) {
                var totalDragY = 0f
                var totalDragX = 0f
                detectDragGestures(
                    onDragStart = {
                        totalDragY = 0f
                        totalDragX = 0f
                        isPaused = true
                    },
                    onDragEnd = {
                        isPaused = false
                        if (totalDragY < -50f || totalDragX < -50f) {
                            nextStory()
                        } else if (totalDragY > 50f || totalDragX > 50f) {
                            prevStory()
                        }
                    },
                    onDragCancel = {
                        isPaused = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount.y
                        totalDragX += dragAmount.x
                    }
                )
            }
            .pointerInput(jobs.size) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        val released = tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.35f) {
                            prevStory()
                        } else {
                            nextStory()
                        }
                    }
                )
            }
    ) {
        // Solid black status bar coverage ensuring 0% bleed-through from background screens at top edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Color.Black)
                .align(Alignment.TopCenter)
        )
        // Full bleed workplace / job image background
        val photoUrl = currentJob.photoUrl?.takeIf { it.isNotBlank() }
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = currentJob.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Elegant category themed background graphic (NO faces)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                JobCategoryIconTile(
                    category = currentJob.category,
                    title = currentJob.title,
                    description = currentJob.description,
                    size = 140.dp,
                    cornerRadius = 32.dp
                )
            }
        }

        // Top Gradient Scrim for header readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC000000),
                            Color(0x66000000),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Gradient Scrim for bottom overlay readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x88000000),
                            Color(0xDD000000),
                            Color(0xF5000000)
                        )
                    )
                )
        )

        // Top Controls: Segmented Progress + Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Segmented Progress Bars (Up to 6 segments)
            val visibleJobsCount = jobs.size.coerceAtMost(8)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until visibleJobsCount) {
                    val progress = when {
                        i < currentIndex -> 1f
                        i == currentIndex -> progressAnim.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x44FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Right-Side Vertical Floating Action Buttons: Favorite + Share (lifted slightly higher)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(end = 16.dp, bottom = 210.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heart / Favorite Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x44000000))
                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                    .clickable { onToggleSave(currentJob.id) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Save job",
                    tint = if (saved) Color(0xFFFF3B30) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Share Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x44000000))
                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                    .clickable {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            val text = "Check out this job opening on DutyPe: ${currentJob.title} at ${currentJob.companyName}. Pay: ${SalaryFormatter.display(currentJob.payAmount, currentJob.payType)}.\nDownload DutyPe: https://dutype.com"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Job via"))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share job",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom Overlay: Badges, Large Salary, Job Title, Details, Apply Now Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 48.dp)
        ) {
            // Badges row: Urgent pill + Openings pill
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentJob.isUrgent) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFF5722))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Urgent",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val vacancies = currentJob.vacancies.coerceAtLeast(1)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x55FFFFFF))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$vacancies openings",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!currentJob.shift.isNullOrBlank() && currentJob.shift != "ANY") {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${currentJob.shift} Shift",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Salary display: ₹18,500 /month
            val payAmount = currentJob.payAmount
            val payTypeLabel = when (currentJob.payType.uppercase()) {
                "MONTHLY" -> "/month"
                "DAILY", "DAY" -> "/day"
                "HOURLY" -> "/hr"
                "WEEKLY" -> "/week"
                else -> "/month"
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹${SalaryFormatter.amount(payAmount)}",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = payTypeLabel,
                    color = Color(0xFFCBD5E1),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Job Title
            Text(
                text = ValidationUtils.capitalizeWords(currentJob.title),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Distance & Employment Type alternating with "Direct Call Employer · 0 Commission" (vertical slide like home job cards)
            var showDirectCallCommission by remember { mutableStateOf(false) }
            LaunchedEffect(currentIndex) {
                while (true) {
                    delay(3000L)
                    showDirectCallCommission = !showDirectCallCommission
                }
            }

            AnimatedContent(
                targetState = showDirectCallCommission,
                transitionSpec = {
                    (slideInVertically { height -> height / 2 } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(animationSpec = tween(300)))
                },
                label = "story_detail_transition"
            ) { showDirect ->
                if (!showDirect) {
                    val distanceStr = currentJob.distance?.let { "${String.format("%.1f", it)} km away · " } ?: ""
                    val empTypeStr = if (currentJob.employmentType.contains("PART", ignoreCase = true)) "Part-time" else "Full-time"
                    val areaStr = currentJob.area.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                    Text(
                        text = "$distanceStr$empTypeStr$areaStr",
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                } else {
                    Text(
                        text = "Direct Call Employer · 0 Commission",
                        color = Color(0xFF6EE7B7),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large White Rounded Button: "Apply now  ->"
            Button(
                onClick = {
                    onDismiss()
                    onApplyJob(currentJob)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0F172A)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Apply now",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
