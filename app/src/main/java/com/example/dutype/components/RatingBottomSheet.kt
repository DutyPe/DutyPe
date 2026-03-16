package com.example.dutype.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Rating Bottom Sheet for rating employers/workers after job completion.
 * Supports dual-role: workers rate employers, employers rate workers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingBottomSheet(
    isVisible: Boolean,
    targetName: String,
    targetRole: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, review: String, tags: List<String>) -> Unit
) {
    if (!isVisible) return

    var selectedRating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val ratingLabel = when (targetRole) {
        "EMPLOYER" -> "Rate Employer"
        else -> "Rate Worker"
    }

    val tags = when (targetRole) {
        "EMPLOYER" -> listOf("Good Pay", "On Time", "Respectful", "Safe Workplace", "Clear Instructions", "Professional")
        else -> listOf("Punctual", "Skilled", "Hardworking", "Trustworthy", "Good Communication", "Professional")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ratingLabel,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = Color(0xFF6B7280))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "How was your experience with $targetName?",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Star rating
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    val isSelected = star <= selectedRating
                    val color by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFFFBBF24) else Color(0xFFD1D5DB),
                        label = "starColor"
                    )
                    IconButton(
                        onClick = { selectedRating = star },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star stars",
                            tint = color,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Rating label text
            if (selectedRating > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (selectedRating) {
                        1 -> "Poor"
                        2 -> "Below Average"
                        3 -> "Average"
                        4 -> "Good"
                        5 -> "Excellent"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF59E0B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tags
            Text(
                text = "What stood out?",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tag chips in a flow layout
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.chunked(3).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { tag ->
                            val isTagSelected = tag in selectedTags
                            FilterChip(
                                selected = isTagSelected,
                                onClick = {
                                    selectedTags = if (isTagSelected) selectedTags - tag else selectedTags + tag
                                },
                                label = {
                                    Text(
                                        text = tag,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFEF3C7),
                                    selectedLabelColor = Color(0xFFB45309)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Review text
            OutlinedTextField(
                value = reviewText,
                onValueChange = { if (it.length <= 300) reviewText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Write a review (optional)") },
                placeholder = { Text("Share your experience...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF59E0B),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit button
            Button(
                onClick = {
                    if (selectedRating > 0) {
                        isSubmitting = true
                        onSubmit(selectedRating, reviewText, selectedTags.toList())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = selectedRating > 0 && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    disabledContainerColor = Color(0xFFE5E7EB)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit Rating",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
