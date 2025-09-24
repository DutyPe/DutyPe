package com.example.partimes.profile.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.profile.models.ResumeUploadState
import com.example.partimes.profile.models.ParsedResumeData
import com.example.partimes.profile.services.ResumeUploadService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResumeUploadViewModel @Inject constructor(
    private val resumeService: ResumeUploadService
) : ViewModel() {
    
    private val _uploadState = MutableStateFlow<ResumeUploadState>(ResumeUploadState.NoFile)
    val uploadState: StateFlow<ResumeUploadState> = _uploadState.asStateFlow()
    
    private val _parsedData = MutableStateFlow<ParsedResumeData?>(null)
    val parsedData: StateFlow<ParsedResumeData?> = _parsedData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun selectFile() {
        // This would typically trigger a file picker
        // For now, we'll simulate file selection
        viewModelScope.launch {
            _uploadState.value = ResumeUploadState.Uploading(0)
            uploadFile("sample_resume.pdf")
        }
    }
    
    private fun uploadFile(fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Simulate upload progress
                for (progress in 0..100 step 10) {
                    _uploadState.value = ResumeUploadState.Uploading(progress)
                    kotlinx.coroutines.delay(200)
                }
                
                // Simulate successful upload
                _uploadState.value = ResumeUploadState.Uploaded(fileName)
                
                // Parse the resume
                val uploadResult = resumeService.uploadResume("current_user_id", ByteArray(0), fileName)
                if (uploadResult.isSuccess) {
                    _parsedData.value = uploadResult.getOrNull()?.parsedData
                }
                
            } catch (e: Exception) {
                _uploadState.value = ResumeUploadState.Error(e.message ?: "Upload failed")
                _error.value = e.message ?: "Failed to upload resume"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun applyToProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val parsed = _parsedData.value
                if (parsed != null) {
                    // TODO: Implement applyParsedData method in service
                    // resumeService.applyParsedData("current_user_id", parsed)
                    // Navigate back or show success message
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to apply data to profile"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun retryUpload() {
        _uploadState.value = ResumeUploadState.NoFile
        _parsedData.value = null
        _error.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
}
