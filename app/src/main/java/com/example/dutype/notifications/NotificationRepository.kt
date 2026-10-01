package com.example.dutype.notifications

import com.example.dutype.firestore.FirestoreSchema.Notifications
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** The signed-in user's inbox (`notifications`, written by the server; TTL on expireAt). */
@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /** Unread badge count: one aggregation read, no documents downloaded. */
    suspend fun unreadCount(): Int {
        val uid = auth.currentUser?.uid ?: return 0
        return runCatching {
            firestore.collection(Notifications.COLLECTION)
                .whereEqualTo(Notifications.RECIPIENT_ID, uid)
                .whereEqualTo(Notifications.READ, false)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count.toInt()
        }.getOrDefault(0)
    }
}
