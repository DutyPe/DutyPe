package com.example.dutype.employer.components.postjob

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dutype.ui.theme.EmployerColors

/**
 * P2 PERFORMANCE FIX: Extracted PostJobNavigationButtons composable
 * 
 * Reduces recomposition scope - navigation buttons are isolated
 * from the main content.
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */
@Composable
fun PostJobNavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    isNextEnabled: Boolean,
    isSubmitting: Boolean,
    primaryColor: Color,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Previous button (only show after step 1)
        if (currentStep > 1) {
            OutlinedButton(
                onClick = onPreviousClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EmployerColors.Border),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = EmployerColors.TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Previous",
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Next/Submit button
        if (currentStep < totalSteps) {
            Button(
                onClick = onNextClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                enabled = isNextEnabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = EmployerColors.Border
                )
            ) {
                Text(
                    text = "Continue",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            // Submit button on final step
            Button(
                onClick = onSubmitClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF059669),
                    disabledContainerColor = Color(0xFF059669).copy(alpha = 0.5f)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Posting...",
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Post Job",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
