package com.example.dutype.components

import com.dutype.app.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.services.ReportType
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.dutype.ui.theme.WorkerColors

/**
 * Report Job Bottom Sheet - Improved Design
 * 
 * P1 Feature: Community Reporting
 * 3 reports = auto-hide job
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportJobSheet(
    jobTitle: String,
    companyName: String,
    onDismiss: () -> Unit,
    onReport: suspend (ReportType, String) -> Result<com.example.dutype.services.ReportResult>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf<ReportType?>(null) }
    var additionalDetails by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        // Success State
        if (showSuccess) {
            SuccessContent(
                message = resultMessage,
                onDismiss = onDismiss
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Header with icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(WorkerColors.ErrorLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = WorkerColors.Error,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.auto_report_job),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.auto_help_us_keep_dutype_safe),
                                style = MaterialTheme.typography.bodySmall,
                                color = WorkerColors.TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(WorkerColors.ChipBackground, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = WorkerColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Job info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.ChipBackground),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(WorkerColors.CardBackground, CircleShape)
                                .border(1.dp, WorkerColors.Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                tint = WorkerColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = jobTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = companyName,
                                style = MaterialTheme.typography.bodySmall,
                                color = WorkerColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Report type selection header
                Text(
                    text = stringResource(R.string.auto_what_s_the_issue),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = WorkerColors.TextSecondary
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Scrollable content area with fixed height
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // NON_PAYMENT is raised from its own sheet because it needs an amount and work date.
                    items(ReportType.entries.filter { it != ReportType.NON_PAYMENT }) { type ->
                        ImprovedReportTypeOption(
                            type = type,
                            isSelected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                    }
                }
                
                // Additional details (shown when type selected)
                AnimatedVisibility(visible = selectedType != null) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = additionalDetails,
                            onValueChange = { additionalDetails = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { 
                                Text(
                                    "Add more details (optional)",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextTertiary)
                                ) 
                            },
                            minLines = 2,
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WorkerColors.Error,
                                unfocusedBorderColor = WorkerColors.Border,
                                focusedContainerColor = WorkerColors.ErrorLight,
                                unfocusedContainerColor = WorkerColors.ChipBackground
                            )
                        )
                    }
                }
                
                // Error message
                AnimatedVisibility(visible = showError) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = WorkerColors.ErrorLight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = WorkerColors.Error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = WorkerColors.Error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Submit button - Always visible
                Button(
                    onClick = {
                        selectedType?.let { type ->
                            scope.launch {
                                isSubmitting = true
                                showError = false
                                
                                val result = onReport(type, additionalDetails)
                                
                                result.fold(
                                    onSuccess = { reportResult ->
                                        if (reportResult.success) {
                                            resultMessage = reportResult.message
                                            showSuccess = true
                                        } else {
                                            errorMessage = reportResult.message
                                            showError = true
                                        }
                                    },
                                    onFailure = { e ->
                                        errorMessage = e.message ?: "Failed to submit report"
                                        showError = true
                                    }
                                )
                                
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = selectedType != null && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WorkerColors.Error,
                        disabledContainerColor = WorkerColors.Error.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.auto_submit_report),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Info text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = WorkerColors.TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.auto_your_report_is_anonymous),
                        style = MaterialTheme.typography.bodySmall,
                        color = WorkerColors.TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ImprovedReportTypeOption(
    type: ReportType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (type) {
        ReportType.SCAM -> Icons.Default.Warning
        ReportType.FAKE -> Icons.Default.Block
        ReportType.NON_PAYMENT -> Icons.Default.MoneyOff
        ReportType.INAPPROPRIATE -> Icons.Default.RemoveCircle
        ReportType.DUPLICATE -> Icons.Default.ContentCopy
        ReportType.MISLEADING -> Icons.Default.Info
        ReportType.HARASSMENT -> Icons.Default.PersonOff
        ReportType.SPAM -> Icons.Default.Report
        ReportType.OTHER -> Icons.Default.MoreHoriz
    }
    
    val iconColor = when (type) {
        ReportType.SCAM -> WorkerColors.Error
        ReportType.FAKE -> WorkerColors.Warning
        ReportType.NON_PAYMENT -> WorkerColors.Error
        ReportType.INAPPROPRIATE -> WorkerColors.Error
        ReportType.DUPLICATE -> WorkerColors.TextSecondary
        ReportType.MISLEADING -> WorkerColors.Info
        ReportType.HARASSMENT -> Color(0xFF7C3AED)
        ReportType.SPAM -> WorkerColors.Warning
        ReportType.OTHER -> WorkerColors.TextSecondary
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) WorkerColors.ErrorLight else WorkerColors.CardBackground
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, WorkerColors.Error)
        else 
            androidx.compose.foundation.BorderStroke(1.dp, WorkerColors.Border),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) iconColor.copy(alpha = 0.15f)
                        else WorkerColors.ChipBackground
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) iconColor else WorkerColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) WorkerColors.Error else WorkerColors.TextPrimary
                )
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = WorkerColors.TextSecondary,
                    lineHeight = 16.sp
                )
            }
            
            // Radio button style indicator
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(
                        width = if (isSelected) 6.dp else 2.dp,
                        color = if (isSelected) WorkerColors.Error else WorkerColors.TextTertiary,
                        shape = CircleShape
                    )
                    .background(WorkerColors.CardBackground, CircleShape)
            )
        }
    }
}

@Composable
private fun SuccessContent(
    message: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated checkmark
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(WorkerColors.Success.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(WorkerColors.Success, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = stringResource(R.string.auto_report_submitted),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = WorkerColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.auto_thank_you_for_helping_keep_dutype_safe),
            style = MaterialTheme.typography.bodySmall,
            color = WorkerColors.Success,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.done), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/**
 * Report Job Icon Button - For use in job cards/details
 */
@Composable
fun ReportJobIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = WorkerColors.TextTertiary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Flag,
            contentDescription = "Report Job",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
