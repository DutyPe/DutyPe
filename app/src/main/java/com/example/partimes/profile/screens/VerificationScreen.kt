package com.example.partimes.profile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.partimes.profile.viewmodels.VerificationViewModel
import com.example.partimes.ui.components.*

/**
 * Verification Screen
 * Manage verification status for email, phone, and ID documents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val viewModel: VerificationViewModel = hiltViewModel()
    val verifications by viewModel.verifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Set status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Verification",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF90D5FF)
                )
            )

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF90D5FF)
                    )
                }
            } else if (error != null) {
                ErrorScreen(
                    errorState = ErrorState(
                        type = ErrorType.UNKNOWN_ERROR,
                        title = "Error",
                        message = error!!,
                        icon = Icons.Default.Error,
                        canRetry = true,
                        retryAction = { viewModel.loadVerifications() }
                    ),
                    onRetry = { viewModel.loadVerifications() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Verification Summary
                    item {
                        VerificationSummaryCard(
                            totalVerifications = verifications.size,
                            verifiedCount = verifications.count { it.status == com.example.partimes.profile.models.VerificationStatus.VERIFIED }
                        )
                    }

                    // Verification Items
                    verifications.forEach { verification ->
                        item {
                            VerificationItem(
                                verification = verification,
                                onVerify = { viewModel.startVerification(verification.type) },
                                onRetry = { viewModel.retryVerification(verification.id) }
                            )
                        }
                    }

                    // Benefits Section
                    item {
                        VerificationBenefitsCard()
                    }

                    // Spacer for bottom padding
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationSummaryCard(
    totalVerifications: Int,
    verifiedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Verification Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VerificationStatItem(
                    icon = Icons.Outlined.Verified,
                    label = "Verified",
                    value = verifiedCount.toString(),
                    color = Color(0xFF4CAF50)
                )
                
                VerificationStatItem(
                    icon = Icons.Outlined.Pending,
                    label = "Pending",
                    value = (totalVerifications - verifiedCount).toString(),
                    color = Color(0xFFFF9800)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = if (totalVerifications > 0) verifiedCount.toFloat() / totalVerifications else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFE8F5E8)
            )
        }
    }
}

@Composable
fun VerificationStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF7F8C8D),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VerificationItem(
    verification: com.example.partimes.profile.models.Verification,
    onVerify: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getStatusColor(verification.status).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    getStatusIcon(verification.status),
                    contentDescription = verification.type.name,
                    tint = getStatusColor(verification.status),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Verification Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = verification.type.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2C3E50)
                )
                
                Text(
                    text = getStatusText(verification.status),
                    fontSize = 14.sp,
                    color = getStatusColor(verification.status)
                )
                
                if (verification.status == com.example.partimes.profile.models.VerificationStatus.REJECTED && verification.rejectionReason != null) {
                    Text(
                        text = verification.rejectionReason!!,
                        fontSize = 12.sp,
                        color = Color(0xFFE74C3C)
                    )
                }
            }
            
            // Action Button
            when (verification.status) {
                com.example.partimes.profile.models.VerificationStatus.NOT_VERIFIED -> {
                    Button(
                        onClick = onVerify,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF90D5FF)
                        )
                    ) {
                        Text("Verify", color = Color.White)
                    }
                }
                com.example.partimes.profile.models.VerificationStatus.PENDING -> {
                    Text(
                        text = "Pending",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
                com.example.partimes.profile.models.VerificationStatus.VERIFIED -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF4CAF50)
                    )
                }
                com.example.partimes.profile.models.VerificationStatus.REJECTED -> {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE74C3C)
                        )
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
                com.example.partimes.profile.models.VerificationStatus.EXPIRED -> {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("Renew", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationBenefitsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Benefits of Verification",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            BenefitItem(
                icon = Icons.Outlined.Star,
                text = "Increase your profile credibility"
            )
            
            BenefitItem(
                icon = Icons.Outlined.TrendingUp,
                text = "Get more job recommendations"
            )
            
            BenefitItem(
                icon = Icons.Outlined.Security,
                text = "Build trust with employers"
            )
            
            BenefitItem(
                icon = Icons.Outlined.Star,
                text = "Get priority in job applications"
            )
        }
    }
}

@Composable
fun BenefitItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF90D5FF),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF7F8C8D)
        )
    }
}

// Helper functions
private fun getStatusIcon(status: com.example.partimes.profile.models.VerificationStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        com.example.partimes.profile.models.VerificationStatus.NOT_VERIFIED -> Icons.Outlined.Help
        com.example.partimes.profile.models.VerificationStatus.PENDING -> Icons.Outlined.Pending
        com.example.partimes.profile.models.VerificationStatus.VERIFIED -> Icons.Default.CheckCircle
        com.example.partimes.profile.models.VerificationStatus.REJECTED -> Icons.Default.Cancel
        com.example.partimes.profile.models.VerificationStatus.EXPIRED -> Icons.Outlined.Schedule
    }
}

private fun getStatusColor(status: com.example.partimes.profile.models.VerificationStatus): Color {
    return when (status) {
        com.example.partimes.profile.models.VerificationStatus.NOT_VERIFIED -> Color(0xFF7F8C8D)
        com.example.partimes.profile.models.VerificationStatus.PENDING -> Color(0xFFFF9800)
        com.example.partimes.profile.models.VerificationStatus.VERIFIED -> Color(0xFF4CAF50)
        com.example.partimes.profile.models.VerificationStatus.REJECTED -> Color(0xFFE74C3C)
        com.example.partimes.profile.models.VerificationStatus.EXPIRED -> Color(0xFFFF9800)
    }
}

private fun getStatusText(status: com.example.partimes.profile.models.VerificationStatus): String {
    return when (status) {
        com.example.partimes.profile.models.VerificationStatus.NOT_VERIFIED -> "Not verified"
        com.example.partimes.profile.models.VerificationStatus.PENDING -> "Verification pending"
        com.example.partimes.profile.models.VerificationStatus.VERIFIED -> "Verified"
        com.example.partimes.profile.models.VerificationStatus.REJECTED -> "Verification rejected"
        com.example.partimes.profile.models.VerificationStatus.EXPIRED -> "Verification expired"
    }
}
