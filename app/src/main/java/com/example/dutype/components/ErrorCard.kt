package com.example.dutype.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors

/**
 * Reusable Error Card Component
 * 
 * Displays error messages in a consistent, visually appealing card format.
 * Used across authentication screens and other error-prone UI areas.
 * Updated with Meesho-style colors and typography.
 * 
 * @param message The error message to display (null hides the card)
 * @param modifier Optional modifier for positioning
 * @param isVisible Controls visibility with animation (defaults to message != null)
 */
@Composable
fun ErrorCard(
    message: String?,
    modifier: Modifier = Modifier,
    isVisible: Boolean = message != null
) {
    AnimatedVisibility(
        visible = isVisible && message != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = WorkerColors.ErrorLight
            ),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp, 
                brush = SolidColor(WorkerColors.Error.copy(alpha = 0.3f))
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = "Error",
                    tint = WorkerColors.Error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message ?: "",
                    style = AppTypography.bodyMedium.copy(
                        color = WorkerColors.Error,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

/**
 * Warning Card variant for non-critical messages
 */
@Composable
fun WarningCard(
    message: String?,
    modifier: Modifier = Modifier,
    isVisible: Boolean = message != null
) {
    AnimatedVisibility(
        visible = isVisible && message != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = WorkerColors.WarningLight // Light yellow background
            ),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp, 
                brush = SolidColor(WorkerColors.Warning.copy(alpha = 0.4f)) // Yellow border
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = "Warning",
                    tint = WorkerColors.Warning, // Amber icon
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message ?: "",
                    color = WorkerColors.Warning, // Amber text
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
