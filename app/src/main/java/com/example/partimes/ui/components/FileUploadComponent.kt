package com.example.partimes.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import android.content.ContentResolver
import android.net.Uri
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun FileUploadComponent(
    onFileSelected: (String, String, Long) -> Unit,
    onUploadProgress: (Float) -> Unit,
    onUploadComplete: (String) -> Unit,
    onUploadError: (String) -> Unit,
    modifier: Modifier = Modifier,
    acceptedTypes: List<String> = listOf("image/*", "application/pdf", "application/msword"),
    maxFileSize: Long = 10 * 1024 * 1024, // 10MB
    label: String = "Upload File"
) {
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<Long?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fileUri ->
            scope.launch {
                try {
                    isUploading = true
                    uploadProgress = 0f
                    
                    // Get file info
                    val fileName = context.contentResolver.getFileName(fileUri)
                    val fileSize = context.contentResolver.getFileSize(fileUri)
                    
                    selectedFileName = fileName
                    selectedFileSize = fileSize
                    
                    // Validate file size
                    if (fileSize > maxFileSize) {
                        onUploadError("File size exceeds ${maxFileSize / (1024 * 1024)}MB limit")
                        return@launch
                    }
                    
                    // Simulate upload progress
                    for (progress in 0..100 step 5) {
                        uploadProgress = progress / 100f
                        onUploadProgress(uploadProgress)
                        kotlinx.coroutines.delay(100)
                    }
                    
                    // Call success callback
                    onUploadComplete(fileUri.toString())
                    onFileSelected(fileName, fileUri.toString(), fileSize)
                    
                } catch (e: Exception) {
                    onUploadError("Upload failed: ${e.message}")
                } finally {
                    isUploading = false
                }
            }
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                if (!isUploading) {
                    filePickerLauncher.launch("*/*")
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isUploading) Color(0xFFF3F4F6) else Color.White
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isUploading) Color(0xFF3B82F6) else Color(0xFFE5E7EB)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isUploading) {
                // Upload progress
                CircularProgressIndicator(
                    progress = uploadProgress,
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFF3B82F6),
                    strokeWidth = 4.dp
                )
                
                Text(
                    text = "Uploading... ${(uploadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
                
                LinearProgressIndicator(
                    progress = uploadProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF3B82F6),
                    trackColor = Color(0xFFE5E7EB)
                )
                
            } else {
                // Upload icon and text
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Upload",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF3B82F6)
                )
                
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF1F2937)
                )
                
                Text(
                    text = "Tap to select file",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
                
                // File info if selected
                selectedFileName?.let { fileName ->
                    selectedFileSize?.let { fileSize ->
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFF0F9FF),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "File",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF1F2937),
                                    maxLines = 1
                                )
                                
                                Text(
                                    text = formatFileSize(fileSize),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    selectedFileName = null
                                    selectedFileSize = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageUploadComponent(
    onImageSelected: (String) -> Unit,
    onUploadProgress: (Float) -> Unit,
    onUploadComplete: (String) -> Unit,
    onUploadError: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Upload Image"
) {
    FileUploadComponent(
        onFileSelected = { fileName, uri, size -> onImageSelected(uri) },
        onUploadProgress = onUploadProgress,
        onUploadComplete = onUploadComplete,
        onUploadError = onUploadError,
        modifier = modifier,
        acceptedTypes = listOf("image/*"),
        maxFileSize = 5 * 1024 * 1024, // 5MB for images
        label = label
    )
}

@Composable
fun DocumentUploadComponent(
    onDocumentSelected: (String, String, Long) -> Unit,
    onUploadProgress: (Float) -> Unit,
    onUploadComplete: (String) -> Unit,
    onUploadError: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Upload Document"
) {
    FileUploadComponent(
        onFileSelected = onDocumentSelected,
        onUploadProgress = onUploadProgress,
        onUploadComplete = onUploadComplete,
        onUploadError = onUploadError,
        modifier = modifier,
        acceptedTypes = listOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        maxFileSize = 10 * 1024 * 1024, // 10MB for documents
        label = label
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

// Extension functions for ContentResolver
private fun ContentResolver.getFileName(uri: Uri): String {
    var fileName = "Unknown"
    val cursor = query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = it.getString(nameIndex) ?: "Unknown"
            }
        }
    }
    return fileName
}

private fun ContentResolver.getFileSize(uri: Uri): Long {
    var fileSize = 0L
    val cursor = query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex != -1) {
                fileSize = it.getLong(sizeIndex)
            }
        }
    }
    return fileSize
}
