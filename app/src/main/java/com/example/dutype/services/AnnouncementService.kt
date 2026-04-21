package com.example.dutype.services

import com.example.dutype.models.Announcement
import com.example.dutype.models.AnnouncementType
import com.example.dutype.models.AnnouncementPriority
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        private const val COLLECTION_ANNOUNCEMENTS = com.example.dutype.firestore.FirestoreCollections.ANNOUNCEMENTS
    }

    private fun mapAnnouncement(doc: com.google.firebase.firestore.DocumentSnapshot): Announcement? {
        val data = doc.data ?: return null
        val type = try {
            AnnouncementType.valueOf((data["type"] as? String ?: "INFO").uppercase())
        } catch (_: Exception) {
            AnnouncementType.INFO
        }
        val priority = try {
            AnnouncementPriority.valueOf((data["priority"] as? String ?: "NORMAL").uppercase())
        } catch (_: Exception) {
            AnnouncementPriority.NORMAL
        }

        return Announcement(
            id = doc.id,
            title = data["title"] as? String ?: "",
            message = data["message"] as? String ?: "",
            type = type,
            priority = priority,
            targetRole = data["targetRole"] as? String,
            actionRoute = data["actionRoute"] as? String,
            expiresAt = data["expiresAt"] as? Timestamp,
            isActive = data["isActive"] as? Boolean ?: true,
            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now()
        )
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
                        mapAnnouncement(doc)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing announcement")
                        null
                    }
                }?.filter { announcement ->
                    // Filter by role (case-insensitive comparison)
                    val roleMatches = announcement.targetRole == null || 
                                    announcement.targetRole.equals(userRole, ignoreCase = true)
                    
                    // Filter out expired announcements
                    val notExpired = announcement.expiresAt?.let { it > now } ?: true
                    
                    Timber.d("📢 Announcement '${announcement.title}': targetRole=${announcement.targetRole}, userRole=$userRole, matches=$roleMatches, notExpired=$notExpired")
                    
                    roleMatches && notExpired
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
                    }.thenByDescending { it.createdAt }
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
