package com.example.dutype.worker.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import com.example.dutype.utils.DateTimeUtils
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.models.VerificationStatus
import com.example.dutype.models.WorkVerification
import com.example.dutype.services.WorkVerificationService
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Worker's Work Start QR Code Screen
 * Shows QR code and verification code for employer to scan/enter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkStartQRScreen(
    jobId: String,
    navController: NavController,
    workVerificationService: WorkVerificationService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    var verification by remember { mutableStateOf<WorkVerification?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var timeRemaining by remember { mutableStateOf("") }
    var isVerified by remember { mutableStateOf(false) }
    
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    // Load verification
    LaunchedEffect(jobId) {
        isLoading = true
        val result = workVerificationService.getVerificationForWorker(currentUserId, jobId)
        result.onSuccess { v ->
            verification = v
            if (v != null) {
                qrBitmap = generateQRCode(v.qrCodeData)
                isVerified = v.isVerified()
            }
        }.onFailure { e ->
            error = e.message
        }
        isLoading = false
    }
    
    // Update time remaining every second
    LaunchedEffect(verification) {
        while (verification != null && !isVerified) {
            verification?.let { v ->
                timeRemaining = v.getExpiryText()
                if (v.isExpired()) {
                    error = "Verification code expired. Please request a new one."
                }
            }
            delay(1000)
            
            // Check if verified
            val result = workVerificationService.getVerificationForWorker(currentUserId, jobId)
            result.onSuccess { v ->
                if (v?.isVerified() == true) {
                    isVerified = true
                    verification = v
                }
            }
        }
    }
    
    // Pulsing animation for QR code
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Use CommonHeader for consistency
        com.example.dutype.components.CommonHeader(
            title = "Work Start Verification",
            onBackClick = { navController.popBackStack() }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF2563EB)
                    )
                }
                
                isVerified -> {
                    // Verified state
                    VerifiedContent(
                        verification = verification,
                        onDone = { navController.popBackStack() }
                    )
                }
                
                error != null -> {
                    ErrorContent(
                        error = error!!,
                        onRetry = {
                            scope.launch {
                                verification?.let { v ->
                                    isLoading = true
                                    error = null
                                    val result = workVerificationService.regenerateVerification(v.verificationId)
                                    result.onSuccess { newV ->
                                        verification = newV
                                        qrBitmap = generateQRCode(newV.qrCodeData)
                                    }.onFailure { e ->
                                        error = e.message
                                    }
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
                
                verification != null -> {
                    QRCodeContent(
                        verification = verification!!,
                        qrBitmap = qrBitmap,
                        timeRemaining = timeRemaining,
                        scale = scale,
                        onCopyCode = {
                            clipboardManager.setText(AnnotatedString(verification!!.verificationCode))
                            android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onRefresh = {
                            scope.launch {
                                isLoading = true
                                val result = workVerificationService.regenerateVerification(verification!!.verificationId)
                                result.onSuccess { newV ->
                                    verification = newV
                                    qrBitmap = generateQRCode(newV.qrCodeData)
                                    error = null
                                }.onFailure { e ->
                                    error = e.message
                                }
                                isLoading = false
                            }
                        }
                    )
                }
                
                else -> {
                    // No verification found
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No verification code found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please wait for the employer to accept your application",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QRCodeContent(
    verification: WorkVerification,
    qrBitmap: Bitmap?,
    timeRemaining: String,
    scale: Float,
    onCopyCode: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Show this to your employer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                    Text(
                        text = "They can scan the QR or enter the code",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Job info
        Text(
            text = verification.jobTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        Text(
            text = "at ${verification.employerName}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // QR Code
        Card(
            modifier = Modifier
                .scale(scale)
                .size(240.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Verification Code
        Text(
            text = "Or enter this code:",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = verification.verificationCode,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    color = Color.White
                )
                IconButton(onClick = onCopyCode) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Timer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = if (verification.getMinutesUntilExpiry() < 30) Color(0xFFEF4444) else Color(0xFF6B7280),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = timeRemaining,
                style = MaterialTheme.typography.bodyMedium,
                color = if (verification.getMinutesUntilExpiry() < 30) Color(0xFFEF4444) else Color(0xFF6B7280)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Refresh button
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate New Code")
        }
    }
}

@Composable
private fun VerifiedContent(
    verification: WorkVerification?,
    onDone: () -> Unit
) {
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
            text = "Your employer has verified your arrival",
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
                        text = v.jobTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF166534)
                    )
                    Text(
                        text = "at ${v.employerName}",
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

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 64.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFEF4444),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate New Code")
        }
    }
}

/**
 * Generate QR code bitmap from data string
 */
private fun generateQRCode(data: String, size: Int = 512): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        Timber.e(e, "Failed to generate QR code")
        null
    }
}

/**
 * Format timestamp to readable time - delegates to centralized DateTimeUtils
 */
private fun formatTime(timestamp: Long): String {
    return DateTimeUtils.formatTime(timestamp)
}
