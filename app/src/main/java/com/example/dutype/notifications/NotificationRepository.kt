package com.example.dutype.notifications

import android.content.Context
import com.example.dutype.firestore.FirestoreSchema.Notifications
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** The signed-in user's inbox (`notifications`, written by the server; TTL on expireAt). */
@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    /** Unread badge count: returns server unread for signed in users, local store count for guests. */
    suspend fun unreadCount(): Int {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return LocalNotificationStore.getUnreadCount(context)
        }
        val serverCount = runCatching {
            firestore.collection(Notifications.COLLECTION)
                .whereEqualTo(Notifications.RECIPIENT_ID, uid)
                .whereEqualTo(Notifications.READ, false)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count.toInt()
        }.getOrDefault(0)

        // Also check any unread local broadcast push notifications
        val localCount = LocalNotificationStore.getUnreadCount(context)
        return serverCount + localCount
    }
}
