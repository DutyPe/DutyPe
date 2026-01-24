package com.example.dutype.components

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
        containerColor = Color.White,
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
                                .background(Color(0xFFFEE2E2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Report Job",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                text = "Help us keep DutyPe safe",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF3F4F6), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Job info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
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
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color(0xFFE5E7EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = jobTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937),
                                maxLines = 1
                            )
                            Text(
                                text = companyName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280),
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Report type selection header
                Text(
                    text = "What's the issue?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Scrollable content area with fixed height
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ReportType.entries) { type ->
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
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9CA3AF))
                                ) 
                            },
                            minLines = 2,
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFDC2626),
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedContainerColor = Color(0xFFFEF2F2),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFDC2626)
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
                        containerColor = Color(0xFFDC2626),
                        disabledContainerColor = Color(0xFFDC2626).copy(alpha = 0.4f)
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
                            text = "Submit Report",
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
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Your report is anonymous",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
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
        ReportType.INAPPROPRIATE -> Icons.Default.RemoveCircle
        ReportType.DUPLICATE -> Icons.Default.ContentCopy
        ReportType.MISLEADING -> Icons.Default.Info
        ReportType.HARASSMENT -> Icons.Default.PersonOff
        ReportType.SPAM -> Icons.Default.Report
        ReportType.OTHER -> Icons.Default.MoreHoriz
    }
    
    val iconColor = when (type) {
        ReportType.SCAM -> Color(0xFFDC2626)
        ReportType.FAKE -> Color(0xFFF59E0B)
        ReportType.INAPPROPRIATE -> Color(0xFFEF4444)
        ReportType.DUPLICATE -> Color(0xFF6B7280)
        ReportType.MISLEADING -> Color(0xFF3B82F6)
        ReportType.HARASSMENT -> Color(0xFF7C3AED)
        ReportType.SPAM -> Color(0xFFF97316)
        ReportType.OTHER -> Color(0xFF6B7280)
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
            containerColor = if (isSelected) Color(0xFFFEF2F2) else Color.White
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFDC2626))
        else 
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
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
                        else Color(0xFFF3F4F6)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) iconColor else Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color(0xFFDC2626) else Color(0xFF1F2937)
                )
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                    lineHeight = 16.sp
                )
            }
            
            // Radio button style indicator
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(
                        width = if (isSelected) 6.dp else 2.dp,
                        color = if (isSelected) Color(0xFFDC2626) else Color(0xFFD1D5DB),
                        shape = CircleShape
                    )
                    .background(Color.White, CircleShape)
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
                .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF10B981), CircleShape),
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
            text = "Report Submitted",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Thank you for helping keep DutyPe safe!",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF10B981),
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
    tint: Color = Color(0xFF9CA3AF)
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
