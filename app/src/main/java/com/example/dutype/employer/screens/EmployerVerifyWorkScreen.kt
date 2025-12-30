package com.example.dutype.employer.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.dutype.models.WorkVerification
import com.example.dutype.services.WorkVerificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors


/**
 * Employer's Work Verification Screen
 * Allows employer to scan QR code or enter verification code to start work
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
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Enter Code (default), 1 = QR Scan
    var verificationCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var verifiedData by remember { mutableStateOf<WorkVerification?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            selectedTab = 1 // Switch to manual entry if camera denied
        }
    }
    
    // Request camera permission on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    // Handle QR code scan result
    fun onQRCodeScanned(qrData: String) {
        if (isLoading || isVerified) return
        
        scope.launch {
            isLoading = true
            error = null
            
            val result = workVerificationService.verifyByQRCode(
                qrCodeData = qrData,
                employerId = currentUserId
            )
            
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
    
    // Handle manual code verification
    fun verifyCode() {
        if (verificationCode.isBlank()) {
            error = "Please enter the verification code"
            return
        }
        
        scope.launch {
            isLoading = true
            error = null
            
            val result = workVerificationService.verifyByCode(
                verificationCode = verificationCode.uppercase().trim(),
                employerId = currentUserId
            )
            
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
            .background(Color(0xFFF8FAFC))
    ) {
        // Use CommonHeader for consistency
        com.example.dutype.components.CommonHeader(
            title = "Verify Work Start",
            onBackClick = { navController.popBackStack() }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            when {
                isVerified -> {
                    VerificationSuccessContent(
                        verification = verifiedData,
                        onDone = { navController.popBackStack() }
                    )
                }
                
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Tab selector - Enter Code first (more commonly used)
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.White,
                            contentColor = Color(0xFF2563EB)
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Enter Code") },
                                icon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp)) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Scan QR Code") },
                                icon = { Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp)) }
                            )
                        }
                        
                        // Content based on selected tab
                        when (selectedTab) {
                            0 -> ManualCodeEntryContent(
                                verificationCode = verificationCode,
                                onCodeChange = { verificationCode = it },
                                isLoading = isLoading,
                                error = error,
                                onVerify = ::verifyCode
                            )
                            1 -> QRScannerContent(
                                hasCameraPermission = hasCameraPermission,
                                isLoading = isLoading,
                                error = error,
                                onQRCodeScanned = ::onQRCodeScanned,
                                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QRScannerContent(
    hasCameraPermission: Boolean,
    isLoading: Boolean,
    error: String?,
    onQRCodeScanned: (String) -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Pulsing animation for scanner frame
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Instructions
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
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Scan Worker's QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                    Text(
                        text = "Point your camera at the QR code on worker's phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (hasCameraPermission) {
            // Camera preview with scanner frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Camera preview
                QRCodeScanner(
                    onQRCodeScanned = onQRCodeScanned
                )
                
                // Scanner frame overlay
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .border(
                            width = 3.dp,
                            color = Color(0xFF2563EB).copy(alpha = borderAlpha),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
                
                // Loading overlay
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Verifying...",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            // No camera permission
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera permission required",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
        
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
 * QR Code Scanner using CameraX and ZXing
 */
@Composable
private fun QRCodeScanner(
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                Timber.e(e, "Error unbinding camera")
            }
        }
    }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val buffer = imageProxy.planes[0].buffer
                            val data = ByteArray(buffer.remaining())
                            buffer.get(data)
                            
                            val width = imageProxy.width
                            val height = imageProxy.height
                            
                            try {
                                val source = PlanarYUVLuminanceSource(
                                    data, width, height, 0, 0, width, height, false
                                )
                                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                                val result = MultiFormatReader().decode(binaryBitmap)
                                
                                val scannedText = result.text
                                if (scannedText != lastScannedCode && scannedText.startsWith("DUTYPE_VERIFY")) {
                                    lastScannedCode = scannedText
                                    onQRCodeScanned(scannedText)
                                }
                            } catch (e: Exception) {
                                // No QR code found in this frame - this is normal
                            }
                            
                            imageProxy.close()
                        }
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Camera binding failed")
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Format timestamp to readable time
 */
private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
