package com.example.dutype.worker.components

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dutype.components.bouncyClickable
import com.example.dutype.models.JobListing
import com.example.dutype.utils.SalaryFormatter
import com.example.dutype.utils.ValidationUtils

/**
 * 3:4 Vertical Aspect Ratio Job Card exactly matching Image 1 Mockup:
 * - Top Image: Full-bleed workplace / category photo (NO human faces/selfies)
 * - Top-left badge: Urgent (orange) or "X openings" (translucent dark)
 * - Top-right: Frosted heart / save button
 * - Below image: Bold job title, Salary & Distance ("₹18,500 /month · 2.4 km")
 */
@Composable
fun WorkerMockupJobCard(
    job: JobListing,
    isSaved: Boolean = false,
    onSaveClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: androidx.compose.ui.unit.Dp = 168.dp,
    imageHeight: androidx.compose.ui.unit.Dp = 185.dp
) {
    val cardRadius = 12.dp
    val payAmount = job.payAmount
    val payTypeLabel = when (job.payType.uppercase()) {
        "MONTHLY" -> "/month"
        "DAILY", "DAY" -> "/day"
        "HOURLY" -> "/hr"
        "WEEKLY" -> "/week"
        else -> "/month"
    }
    val salaryText = "₹${SalaryFormatter.amount(payAmount)} $payTypeLabel"
    val distanceText = job.distance?.let { " · ${String.format("%.1f", it)} km" } ?: ""

    Column(
        modifier = modifier
            .width(cardWidth)
            .bouncyClickable { onCardClick() }
    ) {
        // Top Image Container with badges and heart
        Surface(
            modifier = Modifier
                .width(cardWidth)
                .height(imageHeight),
            shape = RoundedCornerShape(cardRadius),
            shadowElevation = 1.dp,
            color = Color(0xFFF1F5F9),
            border = BorderStroke(0.6.dp, Color(0xFFE2E8F0).copy(alpha = 0.6f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            val photoUrl = job.photoUrl?.takeIf { it.isNotBlank() }
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = job.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Category emoji FULL in place of image (vibrant pastel workplace card)
                val categoryEmoji = remember(job.category, job.title) {
                    val cat = jobCategoryEmoji(job.category)
                    if (cat != "🏢") cat else jobCategoryEmoji(job.title)
                }
                val bgBrush = remember(job.category, job.title) {
                    val key = (job.category + " " + job.title).lowercase()
                    when {
                        key.contains("cook") || key.contains("kitchen") -> Brush.verticalGradient(listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)))
                        key.contains("delivery") -> Brush.verticalGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)))
                        key.contains("electric") || key.contains("light") -> Brush.verticalGradient(listOf(Color(0xFFFEFCE8), Color(0xFFFEF08A)))
                        key.contains("driver") -> Brush.verticalGradient(listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7)))
                        key.contains("construct") || key.contains("mason") -> Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A)))
                        key.contains("clean") || key.contains("maid") || key.contains("house") -> Brush.verticalGradient(listOf(Color(0xFFF0FDF4), Color(0xFFBBF7D0)))
                        key.contains("shop") || key.contains("helper") -> Brush.verticalGradient(listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF)))
                        else -> Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)))
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categoryEmoji,
                        fontSize = 64.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Subtle top shadow overlay for badge contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x55000000),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Top-Left Badge: Urgent (orange) or X openings (translucent dark)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 10.dp)
            ) {
                if (job.isUrgent) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFF5722))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Urgent",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val vacancies = job.vacancies.coerceAtLeast(1)
                    val label = if (vacancies > 1) "$vacancies openings" else "Active"
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Top-Right Heart Button with Spring Pulse
            val heartScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSaved) 1.28f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                ),
                label = "heartPulse"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp)
                    .size(34.dp)
                    .clickable { onSaveClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Save job",
                    tint = if (isSaved) Color(0xFFFF3B30) else if (photoUrl != null) Color.White else Color(0xFF475569),
                    modifier = Modifier
                        .size(19.dp)
                        .graphicsLayer {
                            scaleX = heartScale
                            scaleY = heartScale
                        }
                )
            }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Below Image: Job Title (strictly 1 line with ellipsis)
        Text(
            text = ValidationUtils.capitalizeWords(job.title),
            color = Color(0xFF0F172A),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(1.dp))

        // Below Title: Animated Sliding Up Cycling: 0 = Salary -> 1 = Company Name -> 2 = Call employer for free
        val displayCompany = job.companyName.ifBlank { "DutyPe Partner" }
        var slidingPhase by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(3000L)
                slidingPhase = (slidingPhase + 1) % 3
            }
        }

        AnimatedContent(
            targetState = slidingPhase,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(300)))
            },
            label = "job_card_slide_up_transition"
        ) { phase ->
            when (phase) {
                0 -> {
                    Text(
                        text = "$salaryText$distanceText",
                        color = Color(0xFF64748B),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                1 -> {
                    Text(
                        text = displayCompany,
                        color = Color(0xFF64748B),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                else -> {
                    Text(
                        text = "Call employer for free",
                        color = Color(0xFF64748B),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Service Card for "Earn with DutyPe Services" row matching Image 1:
 * - Badge: "From ₹449", "From ₹129"
 * - Service workplace photo
 * - Top-right heart button
 * - Service Title & Subtitle below
 */
@Composable
fun WorkerMockupServiceCard(
    title: String,
    priceText: String,
    imageUrl: String?,
    category: String,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: androidx.compose.ui.unit.Dp = 168.dp,
    imageHeight: androidx.compose.ui.unit.Dp = 185.dp
) {
    val cardRadius = 12.dp
    var isSaved by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(cardWidth)
            .bouncyClickable { onCardClick() }
    ) {
        Surface(
            modifier = Modifier
                .width(cardWidth)
                .height(imageHeight),
            shape = RoundedCornerShape(cardRadius),
            shadowElevation = 1.dp,
            color = Color(0xFFF1F5F9),
            border = BorderStroke(0.6.dp, Color(0xFFE2E8F0).copy(alpha = 0.6f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFE0F2FE),
                                    Color(0xFFBAE6FD)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    JobCategoryIconTile(
                        category = category,
                        title = title,
                        size = 56.dp,
                        cornerRadius = 16.dp
                    )
                }
            }

            // Top-left price pill: "From ₹449"
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 10.dp)
                    .clip(CircleShape)
                    .background(Color(0xE6FFFFFF))
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text(
                    text = priceText,
                    color = Color(0xFF0F172A),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Top-right frosted heart button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 10.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x55000000))
                    .clickable { isSaved = !isSaved },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Save service",
                    tint = if (isSaved) Color(0xFFFF3B30) else Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = Color(0xFF0F172A),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "DutyPe Verified Partner",
            color = Color(0xFF64748B),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
