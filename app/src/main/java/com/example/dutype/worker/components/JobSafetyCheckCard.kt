package com.example.dutype.worker.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.viewmodels.WorkerChatbotViewModel
import kotlinx.coroutines.launch

/**
 * Job Safety Check Card
 * 
 * Shows AI-powered safety assessment for a job posting.
 * Can be embedded in JobDescriptionScreen.
 * 
 * Features:
 * - Quick safety check button
 * - Risk level indicator (LOW/MEDIUM/HIGH/CRITICAL)
 * - Warning list
 * - Red flags
 * - Link to AI chatbot for more details
 */
@Composable
fun JobSafetyCheckCard(
    jobData: Map<String, Any>,
    employerData: Map<String, Any>? = null,
    onChatWithAI: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkerChatbotViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Set job context
    LaunchedEffect(jobData) {
        viewModel.setJobContext(jobData, employerData)
    }
    
    val safetyResponse = uiState.currentJobSafety
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (safetyResponse?.riskLevel) {
                "LOW" -> Color(0xFFE8F5E9)
                "MEDIUM" -> Color(0xFFFFF8E1)
                "HIGH" -> Color(0xFFFFEBEE)
                "CRITICAL" -> Color(0xFFFFCDD2)
                else -> Color(0xFFF5F5F5)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = when (safetyResponse?.riskLevel) {
                            "LOW" -> Color(0xFF4CAF50)
                            "MEDIUM" -> Color(0xFFFFA000)
                            "HIGH" -> Color(0xFFE53935)
                            "CRITICAL" -> Color(0xFFB71C1C)
                            else -> Color(0xFF757575)
                        }
                    )
                    Text(
                        text = "Job Safety Check",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                if (safetyResponse != null) {
                    // Risk level badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (safetyResponse.riskLevel) {
                            "LOW" -> Color(0xFF4CAF50)
                            "MEDIUM" -> Color(0xFFFFA000)
                            "HIGH" -> Color(0xFFE53935)
                            "CRITICAL" -> Color(0xFFB71C1C)
                            else -> Color(0xFF757575)
                        }
                    ) {
                        Text(
                            text = safetyResponse.riskLevel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Safety check button or results
            if (safetyResponse == null) {
                // Show check button
                Button(
                    onClick = {
                        isChecking = true
                        viewModel.checkJobSafety()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isChecking && !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A237E)
                    )
                ) {
                    if (isChecking || uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking safety...")
                    } else {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Job Safety")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "AI will analyze this job for potential scams and risks",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            } else {
                // Show results
                LaunchedEffect(safetyResponse) {
                    isChecking = false
                }
                
                // Safety emoji and message
                val safetyEmoji = when (safetyResponse.riskLevel) {
                    "LOW" -> "✅"
                    "MEDIUM" -> "⚠️"
                    "HIGH" -> "🚨"
                    "CRITICAL" -> "⛔"
                    else -> "❓"
                }
                
                Text(
                    text = "$safetyEmoji ${safetyResponse.explanation}",
                    fontSize = 14.sp,
                    color = Color(0xFF424242)
                )
                
                // Expandable warnings and red flags
                if (safetyResponse.warnings.isNotEmpty() || safetyResponse.redFlags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Text(
                            text = if (isExpanded) "Hide details" else "Show details",
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            // Warnings
                            if (safetyResponse.warnings.isNotEmpty()) {
                                Text(
                                    text = "⚠️ Warnings:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFFA000)
                                )
                                safetyResponse.warnings.forEach { warning ->
                                    Text(
                                        text = "• $warning",
                                        fontSize = 12.sp,
                                        color = Color(0xFF757575),
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            // Red flags
                            if (safetyResponse.redFlags.isNotEmpty()) {
                                Text(
                                    text = "🚩 Red Flags:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE53935)
                                )
                                safetyResponse.redFlags.forEach { flag ->
                                    Text(
                                        text = "• $flag",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE53935).copy(alpha = 0.8f),
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Chat with AI button
                OutlinedButton(
                    onClick = onChatWithAI,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI Assistant")
                }
            }
        }
    }
}

/**
 * Compact safety indicator for job cards
 * Shows just the risk level badge
 */
@Composable
fun CompactSafetyBadge(
    riskLevel: String?,
    modifier: Modifier = Modifier
) {
    if (riskLevel == null) return
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = when (riskLevel) {
            "LOW" -> Color(0xFF4CAF50)
            "MEDIUM" -> Color(0xFFFFA000)
            "HIGH" -> Color(0xFFE53935)
            "CRITICAL" -> Color(0xFFB71C1C)
            else -> Color(0xFF757575)
        }.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when (riskLevel) {
                    "LOW" -> Icons.Default.CheckCircle
                    "MEDIUM" -> Icons.Default.Warning
                    "HIGH", "CRITICAL" -> Icons.Default.Error
                    else -> Icons.Default.Help
                },
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = when (riskLevel) {
                    "LOW" -> Color(0xFF4CAF50)
                    "MEDIUM" -> Color(0xFFFFA000)
                    "HIGH" -> Color(0xFFE53935)
                    "CRITICAL" -> Color(0xFFB71C1C)
                    else -> Color(0xFF757575)
                }
            )
            Text(
                text = when (riskLevel) {
                    "LOW" -> "Safe"
                    "MEDIUM" -> "Caution"
                    "HIGH" -> "Risky"
                    "CRITICAL" -> "Danger"
                    else -> "Unknown"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = when (riskLevel) {
                    "LOW" -> Color(0xFF4CAF50)
                    "MEDIUM" -> Color(0xFFFFA000)
                    "HIGH" -> Color(0xFFE53935)
                    "CRITICAL" -> Color(0xFFB71C1C)
                    else -> Color(0xFF757575)
                }
            )
        }
    }
}
