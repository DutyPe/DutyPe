package com.example.dutype.services

import com.example.dutype.models.Announcement
import com.example.dutype.models.AnnouncementPriority
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Announcement Service - Manages in-app announcements
 */
@Singleton
class AnnouncementService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    companion object {
        private const val COLLECTION_ANNOUNCEMENTS = "announcements"
    }
    
    // In-memory tracking of dismissed announcements (resets on app restart)
    private val dismissedIds = mutableSetOf<String>()
    
    /**
     * Get active announcements for current user
     */
    fun getActiveAnnouncements(userRole: String): Flow<List<Announcement>> = callbackFlow {
        val now = Timestamp.now()
        
        val listener = firestore.collection(COLLECTION_ANNOUNCEMENTS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error fetching announcements")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val announcements = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Announcement::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing announcement")
                        null
                    }
                }?.filter { announcement ->
                    // Filter by role (case-insensitive comparison)
                    val roleMatches = announcement.targetRole == null || 
                                    announcement.targetRole.equals(userRole, ignoreCase = true)
                    
                    // P0 NULL SAFETY FIX: Safe date comparison
                    val inDateRange = (announcement.startDate?.let { it <= now } ?: true) &&
                                     (announcement.endDate?.let { it > now } ?: true)
                    
                    Timber.d("📢 Announcement '${announcement.title}': targetRole=${announcement.targetRole}, userRole=$userRole, matches=$roleMatches, inDateRange=$inDateRange")
                    
                    roleMatches && inDateRange
                }?.sortedWith(
                    compareByDescending<Announcement> { 
                        // Sort by priority (URGENT > HIGH > MEDIUM/NORMAL > LOW)
                        when (it.priority) {
                            AnnouncementPriority.URGENT -> 5
                            AnnouncementPriority.HIGH -> 4
                            AnnouncementPriority.MEDIUM -> 3
                            AnnouncementPriority.NORMAL -> 2
                            AnnouncementPriority.LOW -> 1
                        }
                    }.thenByDescending { it.startDate }
                ) ?: emptyList()
                
                trySend(announcements)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Dismiss announcement for current user (in-memory)
     */
    suspend fun dismissAnnouncement(announcementId: String) {
        dismissedIds.add(announcementId)
        Timber.d("📢 Announcement dismissed: $announcementId")
    }
    
    /**
     * Check if announcement is dismissed
     */
    suspend fun isAnnouncementDismissed(announcementId: String): Boolean {
        return dismissedIds.contains(announcementId)
    }
}
