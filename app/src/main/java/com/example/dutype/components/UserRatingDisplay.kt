package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.JobRating
import com.example.dutype.models.UserRatingSummary
import com.example.dutype.services.RatingService
import java.text.SimpleDateFormat
import java.util.*

/**
 * Compact rating display for profile cards
 */
@Composable
fun CompactRatingDisplay(
    averageRating: Float,
    totalRatings: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color(0xFFFBBF24),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (totalRatings > 0) String.format("%.1f", averageRating) else "New",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151)
            )
        )
        if (totalRatings > 0) {
            Text(
                text = "($totalRatings)",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                )
            )
        }
    }
}

/**
 * Star rating display (read-only)
 */
@Composable
fun StarRatingDisplay(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Int = 20
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (1..5).forEach { star ->
            val icon = when {
                star <= rating.toInt() -> Icons.Filled.Star
                star - 0.5f <= rating -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (star <= rating.toInt() || star - 0.5f <= rating) 
                    Color(0xFFFBBF24) else Color(0xFFD1D5DB),
                modifier = Modifier.size(starSize.dp)
            )
        }
    }
}

/**
 * Profile Rating Section - Flat menu item style like settings items
 * Always shows the menu item, opens bottom sheet with reviews or empty state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRatingSection(
    userId: String,
    isWorker: Boolean,
    ratingService: RatingService,
    modifier: Modifier = Modifier
) {
    var summary by remember { mutableStateOf<UserRatingSummary?>(null) }
    var ratings by remember { mutableStateOf<List<JobRating>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(userId) {
        isLoading = true
        // Load summary
        val summaryResult = ratingService.getUserRatingSummary(userId)
        summary = summaryResult.getOrNull()
        
        // Load individual ratings
        val ratingsResult = ratingService.getRatingsForUser(userId)
        ratings = ratingsResult.getOrNull() ?: emptyList()
        
        isLoading = false
    }
    
    val hasRatings = summary != null && summary!!.totalRatings > 0
    
    // Always show the menu item row
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showBottomSheet = true }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Star icon
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = if (hasRatings) Color(0xFFFBBF24) else Color(0xFF9CA3AF),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Rating info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ratings & Reviews",
                style = com.example.dutype.ui.theme.AppTypography.menuItemTitle,
                color = Color(0xFF1F2937)
            )
            if (isLoading) {
                Text(
                    text = "Loading...",
                    style = com.example.dutype.ui.theme.AppTypography.menuItemSubtitle,
                    color = Color(0xFF9CA3AF)
                )
            } else if (hasRatings) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format("%.1f", summary!!.averageRating),
                        style = com.example.dutype.ui.theme.AppTypography.menuItemSubtitle.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1F2937)
                    )
                    StarRatingDisplay(
                        rating = summary!!.averageRating,
                        starSize = 14
                    )
                    Text(
                        text = "(${summary!!.totalRatings} reviews)",
                        style = com.example.dutype.ui.theme.AppTypography.menuItemSubtitle,
                        color = Color(0xFF6B7280)
                    )
                }
            } else {
                Text(
                    text = "No ratings yet",
                    style = com.example.dutype.ui.theme.AppTypography.menuItemSubtitle,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
        
        // Arrow
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(20.dp)
        )
    }
    
    // Bottom Sheet for Reviews
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ReviewsBottomSheetContent(
                ratings = ratings,
                isWorker = isWorker,
                summary = summary,
                hasRatings = hasRatings
            )
        }
    }
}

/**
 * Bottom sheet content for reviews - shows reviews or empty state
 */
@Composable
private fun ReviewsBottomSheetContent(
    ratings: List<JobRating>,
    isWorker: Boolean,
    summary: UserRatingSummary?,
    hasRatings: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Title
        Text(
            text = "Ratings & Reviews",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (hasRatings && summary != null) {
            // Rating summary header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("%.1f", summary.averageRating),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                StarRatingDisplay(rating = summary.averageRating, starSize = 24)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${summary.totalRatings} reviews",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
            
            HorizontalDivider(color = Color(0xFFE5E7EB))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reviews list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ratings) { rating ->
                    ReviewItem(
                        rating = rating,
                        isWorker = isWorker
                    )
                }
            }
        } else {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = Color(0xFFD1D5DB),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No ratings yet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isWorker) {
                        "Complete jobs to receive ratings from employers"
                    } else {
                        "Hire workers to receive ratings"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF9CA3AF)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

/**
 * Individual review item showing rater name and rating details
 */
@Composable
private fun ReviewItem(
    rating: JobRating,
    isWorker: Boolean
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Rater info and rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Rater info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar placeholder
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isWorker) Color(0xFF3B82F6).copy(alpha = 0.1f)
                                else Color(0xFF1F2937).copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isWorker) Color(0xFF3B82F6) else Color(0xFF1F2937),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = if (isWorker) {
                                rating.companyName.ifEmpty { "Employer" }
                            } else {
                                "Worker"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = rating.jobTitle.ifEmpty { "Job" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Rating stars
                Column(horizontalAlignment = Alignment.End) {
                    StarRatingDisplay(
                        rating = rating.overallRating.toFloat(),
                        starSize = 16
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormatter.format(Date(rating.createdAt)),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9CA3AF),
                            fontSize = 10.sp
                        )
                    )
                }
            }
            
            // Feedback text
            if (rating.feedback.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "\"${rating.feedback}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF4B5563)
                    )
                )
            }
            
            // Tags
            if (rating.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rating.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingDistribution(summary: UserRatingSummary) {
    val total = summary.totalRatings.toFloat()
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RatingBar("5", summary.fiveStarCount, total)
        RatingBar("4", summary.fourStarCount, total)
        RatingBar("3", summary.threeStarCount, total)
        RatingBar("2", summary.twoStarCount, total)
        RatingBar("1", summary.oneStarCount, total)
    }
}

@Composable
private fun RatingBar(label: String, count: Int, total: Float) {
    val percentage = if (total > 0) count / total else 0f
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF6B7280)
            ),
            modifier = Modifier.width(16.dp)
        )
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color(0xFFFBBF24),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = Color(0xFFFBBF24),
            trackColor = Color(0xFFE5E7EB),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF9CA3AF)
            ),
            modifier = Modifier.width(24.dp)
        )
    }
}

@Composable
private fun RatingCategory(name: String, rating: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarRatingDisplay(rating = rating, starSize = 14)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
            )
        }
    }
}
