package com.example.dutype.employer.screens

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.dutype.utils.DateTimeUtils
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.models.WorkVerification
import com.example.dutype.services.WorkVerificationService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Employer's Work Verification Screen
 * Code-only verification flow to start work
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerVerifyWorkScreen(
    jobId: String,
    applicationId: String,
    navController: NavController,
    workVerificationService: WorkVerificationService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var verificationCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var verifiedData by remember { mutableStateOf<WorkVerification?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    // Handle manual code verification
    fun verifyCode() {
        if (verificationCode.isBlank()) {
            error = "Please enter the verification code"
            return
        }
        
        scope.launch {
            isLoading = true
            error = null
            
            val result = withContext(Dispatchers.IO) {
                workVerificationService.verifyByCode(
                    verificationCode = verificationCode.uppercase().trim(),
                    employerId = currentUserId,
                    jobId = jobId,
                    applicationId = applicationId
                )
            }
            
            result.onSuccess { verification ->
                isVerified = true
                verifiedData = verification
                Toast.makeText(context, "✅ Work started successfully!", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                error = e.message
            }
            
            isLoading = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Use CommonHeader for consistency
        com.example.dutype.components.CommonHeader(
            title = "Verify Work Start",
            onBackClick = { navController.popBackStack() }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            when {
                isVerified -> {
                    VerificationSuccessContent(
                        verification = verifiedData,
                        onDone = { navController.popBackStack() }
                    )
                }
                
                else -> {
                    ManualCodeEntryContent(
                        verificationCode = verificationCode,
                        onCodeChange = { verificationCode = it },
                        isLoading = isLoading,
                        error = error,
                        onVerify = ::verifyCode
                    )
                }
            }
        }
    }
}


@Composable
private fun ManualCodeEntryContent(
    verificationCode: String,
    onCodeChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onVerify: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Enter Verification Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                    Text(
                        text = "Ask the worker for their code (e.g., DTP-7X9K2M)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF22C55E)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Code input field
        OutlinedTextField(
            value = verificationCode,
            onValueChange = { 
                // Auto-format: uppercase and limit length (DTP-XXXXXX = 10 chars)
                val formatted = it.uppercase().take(10)
                onCodeChange(formatted)
            },
            label = { Text("Verification Code") },
            placeholder = { Text("DTP-XXXXXX") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onVerify() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFFD1D5DB)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Code format: DTP-XXXXXX (e.g., DTP-7X9K2M)",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF)
        )
        
        // Error message
        error?.let { errorMsg ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = errorMsg,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Verify button
        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = verificationCode.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                disabledContainerColor = Color(0xFFD1D5DB)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verify & Start Work",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun VerificationSuccessContent(
    verification: WorkVerification?,
    onDone: () -> Unit
) {
    // Success animation
    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Work Started!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Worker has been verified and work has begun",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
        
        verification?.let { v ->
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = v.workerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                    Text(
                        text = "for ${v.jobTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF15803D)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Started at ${formatTime(v.verifiedAt ?: System.currentTimeMillis())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF22C55E)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Done", fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Format timestamp to readable time - delegates to centralized DateTimeUtils
 */
private fun formatTime(timestamp: Long): String {
    return DateTimeUtils.formatTime(timestamp)
}
