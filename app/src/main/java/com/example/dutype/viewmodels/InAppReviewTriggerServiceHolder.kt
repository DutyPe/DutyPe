package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import com.example.dutype.services.InAppReviewTriggerService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel wrapper to inject InAppReviewTriggerService into Compose screens
 * This is a workaround since Compose doesn't support direct service injection
 */
@HiltViewModel
class InAppReviewTriggerServiceHolder @Inject constructor(
    val service: InAppReviewTriggerService
) : ViewModel()
