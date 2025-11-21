package com.example.dutype.repositories

import com.example.dutype.models.ChatChannel
import com.example.dutype.models.ChatMessage
import com.example.dutype.services.FirestoreService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val firestoreService: FirestoreService
) {

    fun getChannels(): Flow<List<ChatChannel>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chat_channels")
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val channels = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatChannel::class.java)
                    }
                    trySend(channels)
                }
            }

        awaitClose { listener.remove() }
    }

    fun getMessages(channelId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chat_channels")
            .document(channelId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)
                    }
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(channelId: String, text: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            
            val messageRef = firestore.collection("chat_channels")
                .document(channelId)
                .collection("messages")
                .document()
                
            val message = ChatMessage(
                id = messageRef.id,
                channelId = channelId,
                senderId = userId,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            
            firestore.runBatch { batch ->
                batch.set(messageRef, message)
                
                val channelRef = firestore.collection("chat_channels").document(channelId)
                batch.update(channelRef, mapOf(
                    "lastMessageText" to text,
                    "lastMessageSenderId" to userId,
                    "lastMessageTimestamp" to message.timestamp
                ))
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createChannel(otherUserId: String): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
            
            // Check if channel already exists
            val existingQuery = firestore.collection("chat_channels")
                .whereArrayContains("participantIds", userId)
                .get()
                .await()
                
            val existingChannel = existingQuery.documents.find { doc ->
                val participants = doc.get("participantIds") as? List<String>
                participants?.contains(otherUserId) == true
            }
            
            if (existingChannel != null) {
                return Result.success(existingChannel.id)
            }
            
            // Create new channel
            val newChannelRef = firestore.collection("chat_channels").document()
            val channel = ChatChannel(
                id = newChannelRef.id,
                participantIds = listOf(userId, otherUserId),
                lastMessageTimestamp = System.currentTimeMillis()
            )
            
            newChannelRef.set(channel).await()
            Result.success(newChannelRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun hydrateChannel(channel: ChatChannel): ChatChannel {
        val otherUserId = channel.participantIds.find { it != auth.currentUser?.uid } ?: return channel
        val userResult = firestoreService.getUserById(otherUserId)
        val user = userResult.getOrNull()
        
        return if (user != null) {
            channel.copy(participants = listOf(user))
        } else {
            channel
        }
    }
}
