package com.example.dutype.employer.components.postjob

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R

/**
 * P2 PERFORMANCE FIX: Extracted StepProgressIndicator composable
 * 
 * Reduces recomposition scope - only this component recomposes when
 * step changes, not the entire PostJobScreen.
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */
@Composable
fun PostJobStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val stepLabels = listOf(
        stringResource(R.string.step_job_details),
        stringResource(R.string.step_pay_location),
        stringResource(R.string.step_contact_info),
        stringResource(R.string.step_review_post)
    )
    val stepDescriptions = listOf(
        "Title, category & description",
        "Salary, work hours & address",
        "How workers can reach you",
        "Verify and publish your job"
    )
    val stepIcons = listOf("📝", "💰", "📞", "✅")
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Current step info card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8FAFC))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stepIcons.getOrElse(currentStep - 1) { "📝" },
                    fontSize = 24.sp
                )
                Column {
                    Text(
                        text = stepLabels.getOrElse(currentStep - 1) { "" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stepDescriptions.getOrElse(currentStep - 1) { "" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            // Step counter badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryColor.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "$currentStep/$totalSteps",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress bar with dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSteps) { index ->
                val stepNumber = index + 1
                val isCompleted = stepNumber < currentStep
                val isCurrent = stepNumber == currentStep
                
                val barColor by animateColorAsState(
                    targetValue = when {
                        isCompleted -> primaryColor
                        isCurrent -> primaryColor.copy(alpha = 0.5f)
                        else -> Color(0xFFE5E7EB)
                    },
                    animationSpec = tween(300),
                    label = "stepColor"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                )
            }
        }
    }
}
