package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
 * Profile Rating Section - Shows ratings only if user has at least 1 rating
 * Includes clickable "See all reviews" to view who gave ratings
 */
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
    var showAllReviewsDialog by remember { mutableStateOf(false) }
    
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
    
    // Only show if user has at least 1 rating
    if (!isLoading && summary != null && summary!!.totalRatings > 0) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ratings & Reviews",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", summary!!.averageRating),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${summary!!.totalRatings} ratings from ${summary!!.totalJobs} jobs",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Star distribution
                RatingDistribution(summary!!)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category breakdown
                if (isWorker) {
                    if (summary!!.averagePunctuality > 0) {
                        RatingCategory("Punctuality", summary!!.averagePunctuality)
                    }
                    if (summary!!.averageQuality > 0) {
                        RatingCategory("Work Quality", summary!!.averageQuality)
                    }
                } else {
                    if (summary!!.averagePayment > 0) {
                        RatingCategory("Payment", summary!!.averagePayment)
                    }
                }
                if (summary!!.averageCommunication > 0) {
                    RatingCategory("Communication", summary!!.averageCommunication)
                }
                if (summary!!.averageProfessionalism > 0) {
                    RatingCategory("Professionalism", summary!!.averageProfessionalism)
                }
                
                // Top tags
                if (summary!!.topTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summary!!.topTags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
                
                // See all reviews button
                if (ratings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { showAllReviewsDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "See all ${ratings.size} reviews →",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isWorker) Color(0xFF1F2937) else Color(0xFF3B82F6)
                            )
                        )
                    }
                }
            }
        }
        
        // All Reviews Dialog
        if (showAllReviewsDialog) {
            AllReviewsDialog(
                ratings = ratings,
                isWorker = isWorker,
                onDismiss = { showAllReviewsDialog = false }
            )
        }
    }
}

/**
 * Dialog showing all reviews with rater names
 */
@Composable
private fun AllReviewsDialog(
    ratings: List<JobRating>,
    isWorker: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isWorker) "Reviews from Employers" else "Reviews from Workers",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
                
                HorizontalDivider(color = Color(0xFFE5E7EB))
                
                // Reviews list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(ratings) { rating ->
                        ReviewItem(
                            rating = rating,
                            isWorker = isWorker
                        )
                    }
                }
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
                        // Show company name for employer reviews, or "Worker" for worker reviews
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
