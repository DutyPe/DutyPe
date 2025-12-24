package com.example.dutype.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.system.exitProcess

/**
 * Utility to check if Developer Options are enabled
 */
object DeveloperModeChecker {
    
    /**
     * Check if developer options are enabled on the device
     */
    fun isDeveloperModeEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            // If we can't check, assume it's not enabled
            false
        }
    }
    
    /**
     * Check if USB debugging is enabled
     */
    fun isUsbDebuggingEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if mock locations are allowed
     */
    fun isMockLocationEnabled(context: Context): Boolean {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ALLOW_MOCK_LOCATION
            ) == "1"
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Device Fingerprint utility for fraud prevention
 * Generates a unique device identifier for suspension tracking
 */
object DeviceFingerprint {
    
    /**
     * Get Android ID - unique per device per app signing key
     */
    fun getAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Get device model info
     */
    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_")
    }
    
    /**
     * Get device fingerprint hash combining multiple identifiers
     * This creates a semi-unique identifier for the device
     */
    fun getDeviceFingerprint(context: Context): String {
        val androidId = getAndroidId(context)
        val deviceModel = getDeviceModel()
        val buildId = Build.ID
        val serial = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                Build.SERIAL
            }
        } catch (e: SecurityException) {
            "unknown"
        }
        
        // Create a combined fingerprint
        val combined = "$androidId|$deviceModel|$buildId|$serial"
        return combined.hashCode().toString(16) // Convert to hex string
    }
    
    /**
     * Get comprehensive device info for storage
     */
    fun getDeviceInfo(context: Context): Map<String, Any> {
        return mapOf(
            "androidId" to getAndroidId(context),
            "deviceModel" to getDeviceModel(),
            "manufacturer" to Build.MANUFACTURER,
            "brand" to Build.BRAND,
            "device" to Build.DEVICE,
            "product" to Build.PRODUCT,
            "sdkVersion" to Build.VERSION.SDK_INT,
            "fingerprint" to getDeviceFingerprint(context),
            "timestamp" to System.currentTimeMillis()
        )
    }
}

/**
 * Full-screen blocking dialog for Developer Mode warning
 * COMPLETELY NON-DISMISSIBLE - user must either disable developer mode or exit app
 * Cannot be closed by back button, touch outside, or any gesture
 */
@Composable
fun DeveloperModeWarningSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit = {} // Ignored - not dismissible
) {
    val context = LocalContext.current
    
    if (isVisible) {
        // Block back button completely
        BackHandler(enabled = true) {
            // Do nothing - cannot dismiss with back button
        }
        
        // Use Dialog with non-dismissible properties
        Dialog(
            onDismissRequest = { /* Cannot dismiss */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            // Full screen blocking surface
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    // Warning Icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Title
                    Text(
                        text = "⚠️ Security Alert",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Developer Mode Detected",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937)
                        ),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Warning message card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF2F2)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Why this matters:",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Developer Mode allows apps to fake your GPS location. This can be used for fraud and violates our terms of service.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF7F1D1D),
                                    lineHeight = 24.sp
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "To protect workers and employers from location fraud, DutyPe cannot run with Developer Mode enabled.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF7F1D1D),
                                    lineHeight = 24.sp
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Violation warning
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFDC2626).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "⚠️ Repeated violations may result in account suspension",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // How to disable section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3F4F6)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFF374151),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "How to disable Developer Mode:",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF374151)
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "1. Go to Settings → System → Developer Options\n" +
                                       "2. Toggle OFF \"Developer Options\"\n" +
                                       "3. Return to DutyPe app",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFF6B7280),
                                    lineHeight = 28.sp
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Open Settings Button
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to general settings
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF374151)
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF374151))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Open Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Exit App Button
                    Button(
                        onClick = {
                            // Close the app completely
                            (context as? Activity)?.finishAffinity()
                            exitProcess(0)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Exit App",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Footer note
                    Text(
                        text = "This security measure protects all DutyPe users from fraud.\nYou cannot use the app until Developer Mode is disabled.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
