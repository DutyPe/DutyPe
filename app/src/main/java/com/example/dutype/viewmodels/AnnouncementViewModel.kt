package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.Announcement
import com.example.dutype.services.AnnouncementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Announcement ViewModel - Manages announcements state
 */
@HiltViewModel
class AnnouncementViewModel @Inject constructor(
    private val announcementService: AnnouncementService
) : ViewModel() {
    
    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()
    
    /**
     * Load announcements for user role
     */
    fun loadAnnouncements(userRole: String) {
        viewModelScope.launch {
            timber.log.Timber.d("📢 AnnouncementViewModel: Loading announcements for role: $userRole")
            announcementService.getActiveAnnouncements(userRole).collect { list ->
                timber.log.Timber.d("📢 AnnouncementViewModel: Received ${list.size} announcements from service")
                // Filter out dismissed announcements
                val filtered = list.filter { announcement ->
                    val isDismissed = announcementService.isAnnouncementDismissed(announcement.id)
                    timber.log.Timber.d("📢 AnnouncementViewModel: ${announcement.title} - dismissed: $isDismissed")
                    !isDismissed
                }
                timber.log.Timber.d("📢 AnnouncementViewModel: After filtering dismissed: ${filtered.size} announcements")
                _announcements.value = filtered
            }
        }
    }
    
    /**
     * Dismiss announcement
     */
    fun dismissAnnouncement(announcementId: String) {
        viewModelScope.launch {
            announcementService.dismissAnnouncement(announcementId)
            // Remove from current list
            _announcements.value = _announcements.value.filter { it.id != announcementId }
        }
    }
}
