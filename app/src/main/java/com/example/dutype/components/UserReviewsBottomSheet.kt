package com.example.dutype.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dutype.services.Rating
import com.example.dutype.ui.theme.WorkerColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserReviewsBottomSheet(
    isVisible: Boolean,
    title: String,
    averageRating: Float,
    totalRatings: Int,
    reviews: List<Rating>,
    givenReviews: List<Rating> = emptyList(),
    isLoading: Boolean,
    isGivenLoading: Boolean = false,
    receivedTabTitle: String = "Rated you",
    givenTabTitle: String = "You rated",
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WorkerColors.TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = WorkerColors.Warning,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = String.format("%.1f", averageRating),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = WorkerColors.TextPrimary
                    )
                )
                Text(
                    text = "($totalRatings ${if (totalRatings == 1) "review" else "reviews"})",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = WorkerColors.TextSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(receivedTabTitle) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(givenTabTitle) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val selectedReviews = if (selectedTab == 0) reviews else givenReviews
            val selectedLoading = if (selectedTab == 0) isLoading else isGivenLoading
            val emptyText = if (selectedTab == 0) "No ratings received yet" else "No ratings given yet"

            when {
                selectedLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
                    }
                }
                selectedReviews.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = WorkerColors.TextSecondary
                            )
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = selectedReviews,
                            key = { index, review -> "${review.id.ifBlank { "rev" }}_$index" }
                        ) { _, review ->
                            ReviewItem(
                                review = review,
                                showTarget = selectedTab == 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(
    review: Rating,
    showTarget: Boolean
) {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val primaryName = if (showTarget) {
        review.targetName.ifBlank { review.targetCompanyName }.ifBlank { "Rated user" }
    } else {
        review.raterName.ifBlank { review.raterCompanyName }.ifBlank { "Anonymous" }
    }
    val companyName = if (showTarget) review.targetCompanyName else review.raterCompanyName
    val displayLabel = when {
        companyName.isBlank() -> primaryName
        primaryName.equals(companyName, ignoreCase = true) -> primaryName
        else -> "$primaryName • $companyName"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.ChipBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = WorkerColors.TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatter.format(Date(review.createdAt)),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WorkerColors.TextTertiary
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = WorkerColors.Warning,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (review.review.isNotBlank()) {
                Text(
                    text = review.review,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WorkerColors.TextSecondary
                    )
                )
            }

            if (review.tags.isNotEmpty()) {
                Text(
                    text = review.tags.joinToString("  •  "),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = WorkerColors.TextSecondary
                    )
                )
            }
        }
    }
}
