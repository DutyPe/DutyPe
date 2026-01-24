package com.example.dutype.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * QR Code Generator for Referral System
 * Generates QR codes that link to the app with referral code
 */
object QRCodeGenerator {
    
    /**
     * Generate QR code bitmap from content string
     */
    suspend fun generateQRCode(content: String, size: Int = 512): Bitmap? {
        return withContext(Dispatchers.Default) {
            try {
                val hints = mapOf(
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.CHARACTER_SET to "UTF-8"
                )
                
                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
                
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
                for (x in 0 until size) {
                    for (y in 0 until size) {
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Generate referral link with code using DeepLinkHandler
     * @deprecated Use DeepLinkHandler.generateReferralWebLink() instead
     */
    @Deprecated("Use DeepLinkHandler.generateReferralWebLink() instead")
    fun generateReferralLink(referralCode: String): String {
        return com.example.dutype.utils.DeepLinkHandler.generateReferralWebLink(referralCode)
    }
}

/**
 * QR Code Display Component
 */
@Composable
fun ReferralQRCodeCard(
    referralCode: String,
    userName: String,
    userRole: String, // "Worker" or "Employer"
    modifier: Modifier = Modifier
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val referralLink = remember(referralCode) { 
        QRCodeGenerator.generateReferralLink(referralCode) 
    }
    
    // Generate QR code
    LaunchedEffect(referralLink) {
        qrBitmap = QRCodeGenerator.generateQRCode(referralLink)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Scan to Join DutyPe",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Referred by $userName",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = androidx.compose.ui.graphics.Color(0xFF6B7280)
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // QR Code
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "Referral QR Code",
                        modifier = Modifier.size(180.dp)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = androidx.compose.ui.graphics.Color(0xFF2193b0)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Referral Code Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF3F4F6)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Referral Code",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = androidx.compose.ui.graphics.Color(0xFF6B7280)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = referralCode,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFF2193b0),
                            letterSpacing = 2.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructions
            Text(
                text = "Share this QR code or referral code with friends.\nBoth of you earn rewards when they join!",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = androidx.compose.ui.graphics.Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * Compact QR Code for sharing
 */
@Composable
fun CompactReferralQR(
    referralCode: String,
    modifier: Modifier = Modifier
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val referralLink = remember(referralCode) { 
        QRCodeGenerator.generateReferralLink(referralCode) 
    }
    
    LaunchedEffect(referralLink) {
        qrBitmap = QRCodeGenerator.generateQRCode(referralLink, 256)
    }
    
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap!!.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(100.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
