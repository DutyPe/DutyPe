package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import com.example.dutype.services.WorkVerificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * WorkVerificationViewModel - Provides WorkVerificationService for screens that need it
 * 
 * This replaces the deprecated WorkVerificationServiceProvider pattern.
 * Screens should use this ViewModel to access WorkVerificationService.
 */
@HiltViewModel
class WorkVerificationViewModel @Inject constructor(
    val workVerificationService: WorkVerificationService
) : ViewModel()
