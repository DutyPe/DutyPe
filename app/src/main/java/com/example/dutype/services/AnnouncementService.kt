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
        private const val COLLECTION_DISMISSED = "dismissed_announcements"
    }
    
    /**
     * Get active announcements for current user
     */
    fun getActiveAnnouncements(userRole: String): Flow<List<Announcement>> = callbackFlow {
        val now = Timestamp.now()
        
        val listener = firestore.collection(COLLECTION_ANNOUNCEMENTS)
            .whereEqualTo("isActive", true)
            .whereLessThanOrEqualTo("startDate", now)
            .orderBy("startDate", Query.Direction.DESCENDING)
            .orderBy("priority", Query.Direction.DESCENDING)
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
                    
                    // Filter by end date
                    val notExpired = announcement.endDate == null || 
                                   announcement.endDate!! > now
                    
                    Timber.d("📢 Announcement '${announcement.title}': targetRole=${announcement.targetRole}, userRole=$userRole, matches=$roleMatches, expired=${!notExpired}")
                    
                    roleMatches && notExpired
                } ?: emptyList()
                
                trySend(announcements)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Dismiss announcement for current user
     */
    suspend fun dismissAnnouncement(announcementId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            firestore.collection(COLLECTION_DISMISSED)
                .document("${userId}_$announcementId")
                .set(mapOf(
                    "userId" to userId,
                    "announcementId" to announcementId,
                    "dismissedAt" to Timestamp.now()
                ))
                .await()
            
            Timber.d("📢 Announcement dismissed: $announcementId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to dismiss announcement")
        }
    }
    
    /**
     * Check if announcement is dismissed
     */
    suspend fun isAnnouncementDismissed(announcementId: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        return try {
            val doc = firestore.collection(COLLECTION_DISMISSED)
                .document("${userId}_$announcementId")
                .get()
                .await()
            
            doc.exists()
        } catch (e: Exception) {
            Timber.e(e, "Error checking dismissed status")
            false
        }
    }
}
