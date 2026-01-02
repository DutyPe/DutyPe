package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import com.example.dutype.notifications.InAppNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Hilt ViewModel for InAppNotificationManager
 * 
 * Moved to viewmodels package for consistency with other ViewModels.
 * Provides access to the InAppNotificationManager singleton via Hilt injection.
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@HiltViewModel
class InAppNotificationManagerViewModel @Inject constructor(
    val notificationManager: InAppNotificationManager
) : ViewModel()
