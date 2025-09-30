package com.example.partimes.services

import com.example.partimes.worker.models.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling file uploads
 * Provides progress tracking and error handling
 */
@Singleton
class FileUploadService @Inject constructor() {
    
    /**
     * Upload a document with progress tracking
     */
    fun uploadDocument(
        document: Document,
        fileBytes: ByteArray,
        onProgress: (Float) -> Unit = {}
    ): Flow<Result<Document>> = flow {
        try {
            // Simulate upload progress
            for (progress in 0..100 step 10) {
                onProgress(progress / 100f)
                kotlinx.coroutines.delay(100) // Simulate upload time
            }
            
            // Simulate successful upload
            val uploadedDocument = document.copy(
                // Add any upload-specific fields here
            )
            emit(Result.success(uploadedDocument))
            
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get file size in human readable format
     */
    fun getFileSizeString(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes bytes"
        }
    }
    
    /**
     * Validate file type
     */
    fun isValidFileType(fileName: String, allowedTypes: List<String> = listOf("pdf", "doc", "docx", "jpg", "jpeg", "png")): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return allowedTypes.contains(extension)
    }
    
    /**
     * Validate file size (max 10MB)
     */
    fun isValidFileSize(fileSize: Long, maxSizeBytes: Long = 10 * 1024 * 1024): Boolean {
        return fileSize <= maxSizeBytes
    }
}
