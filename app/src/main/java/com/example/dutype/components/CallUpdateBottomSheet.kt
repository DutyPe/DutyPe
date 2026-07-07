package com.example.dutype.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import com.example.dutype.services.JobAvailabilityFeedback
import com.example.dutype.ui.theme.WorkerColors
import kotlinx.coroutines.launch

@Composable
fun CallUpdateBottomSheet(
    jobTitle: String,
    companyName: String,
    onDismiss: () -> Unit,
    onSubmit: suspend (Boolean, JobAvailabilityFeedback?, Boolean?) -> Result<Unit>,
    onSubmitted: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var spokeWithEmployer by remember { mutableStateOf<Boolean?>(null) }
    var availability by remember { mutableStateOf<JobAvailabilityFeedback?>(null) }
    var jobOfferAccepted by remember { mutableStateOf<Boolean?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WorkerColors.CardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(WorkerColors.Primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = WorkerColors.Primary, modifier = Modifier.size(21.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.quick_call_update),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = WorkerColors.TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.quick_call_update_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = WorkerColors.TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = WorkerColors.TextSecondary)
                }
            }

            if (jobTitle.isNotBlank() || companyName.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.ChipBackground),
                    elevation = CardDefaults.cardElevation(0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (jobTitle.isNotBlank()) {
                            Text(jobTitle, color = WorkerColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (companyName.isNotBlank()) {
                            Text(companyName, color = WorkerColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.did_speak_employer),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = WorkerColors.TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FeedbackChoiceButton(
                    text = stringResource(R.string.yes_spoke),
                    selected = spokeWithEmployer == true,
                    onClick = { spokeWithEmployer = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                )
                FeedbackChoiceButton(
                    text = stringResource(R.string.no_answer),
                    selected = spokeWithEmployer == false,
                    onClick = { spokeWithEmployer = false },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                )
            }

            Text(
                text = stringResource(R.string.job_still_available_question),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = WorkerColors.TextSecondary
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(JobAvailabilityFeedback.entries) { option ->
                    FeedbackChoiceButton(
                        text = jobAvailabilityFeedbackLabel(option),
                        selected = availability == option,
                        onClick = { availability = option },
                        modifier = Modifier.width(140.dp),
                        enabled = !isSubmitting
                    )
                }
            }

            Text(
                text = "Did you get selected or hired from this call?",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = WorkerColors.TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FeedbackChoiceButton(
                    text = "Yes",
                    selected = jobOfferAccepted == true,
                    onClick = { jobOfferAccepted = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                )
                FeedbackChoiceButton(
                    text = "No",
                    selected = jobOfferAccepted == false,
                    onClick = { jobOfferAccepted = false },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                )
                FeedbackChoiceButton(
                    text = "Skip",
                    selected = jobOfferAccepted == null,
                    onClick = { jobOfferAccepted = null },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                )
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = WorkerColors.Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        isSubmitting = true
                        errorMessage = null
                        val result = onSubmit(spokeWithEmployer ?: false, availability, jobOfferAccepted)
                        isSubmitting = false
                        result.fold(
                            onSuccess = { onSubmitted() },
                            onFailure = { error -> errorMessage = error.message ?: context.getString(R.string.save_feedback_failed) }
                        )
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.submit_update), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun jobAvailabilityFeedbackLabel(option: JobAvailabilityFeedback): String = when (option) {
    JobAvailabilityFeedback.STILL_AVAILABLE -> stringResource(R.string.availability_still_available)
    JobAvailabilityFeedback.FILLED -> stringResource(R.string.availability_job_filled)
    JobAvailabilityFeedback.NOT_SURE -> stringResource(R.string.availability_not_sure)
}

@Composable
private fun FeedbackChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) WorkerColors.Primary else WorkerColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) WorkerColors.Primary.copy(alpha = 0.12f) else WorkerColors.CardBackground,
            contentColor = if (selected) Color(0xFF1D4ED8) else WorkerColors.TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        if (selected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}