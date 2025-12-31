package com.example.dutype.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chat Service - P1 FIX #8
 * 
 * Real-time Firebase chat between workers and employers.
 * Uses Cloud Functions for message sending and Firestore for real-time updates.
 */
@Singleton
class ChatService @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // ==========================================
    // DATA CLASSES
    // ==========================================
    
    data class Conversation(
        val id: String = "",
        val participants: List<String> = emptyList(),
        val participantDetails: Map<String, ParticipantInfo> = emptyMap(),
        val jobId: String? = null,
        val lastMessage: String? = null,
        val lastMessageAt: Long? = null,
        val lastMessageBy: String? = null,
        val unreadCount: Map<String, Int> = emptyMap(),
        val createdAt: Long = 0
    )
    
    data class ParticipantInfo(
        val name: String = "",
        val profileImage: String? = null,
        val role: String = ""
    )
    
    data class ChatMessage(
        val id: String = "",
        val conversationId: String = "",
        val senderId: String = "",
        val recipientId: String = "",
        val message: String = "",
        val type: MessageType = MessageType.TEXT,
        val isRead: Boolean = false,
        val createdAt: Long = 0,
        val readAt: Long? = null
    )
    
    enum class MessageType {
        TEXT, IMAGE, LOCATION, JOB_CARD
    }
    
    // ==========================================
    // CONVERSATION MANAGEMENT
    // ==========================================
    
    /**
     * Get or create a conversation with another user
     */
    suspend fun getOrCreateConversation(
        otherUserId: String,
        jobId: String? = null
    ): Result<String> {
        return try {
            Timber.d("💬 CHAT: Getting/creating conversation with $otherUserId")
            
            val data = hashMapOf(
                "otherUserId" to otherUserId,
                "jobId" to jobId
            )
            
            val result = functions
                .getHttpsCallable("getOrCreateConversation")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            val conversationId = response["conversationId"] as String
            
            Timber.d("💬 CHAT: Got conversation ID: $conversationId")
            Result.success(conversationId)
            
        } catch (e: Exception) {
            Timber.e(e, "💬 CHAT: Error getting/creating conversation")
            Result.failure(e)
        }
    }
    
    /**
     * Get all conversations for current user as a Flow
     */
    fun getConversationsFlow(): Flow<List<Conversation>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("conversations")
            .whereArrayContains("participants", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "💬 CHAT: Error listening to conversations")
                    return@addSnapshotListener
                }
                
                val conversations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val docData = doc.data ?: return@mapNotNull null
                        Conversation(
                            id = doc.id,
                            participants = (docData["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            participantDetails = parseParticipantDetails(docData["participantDetails"]),
                            jobId = docData["jobId"] as? String,
                            lastMessage = docData["lastMessage"] as? String,
                            lastMessageAt = (docData["lastMessageAt"] as? com.google.firebase.Timestamp)?.toDate()?.time,
                            lastMessageBy = docData["lastMessageBy"] as? String,
                            unreadCount = parseUnreadCount(docData["unreadCount"]),
                            createdAt = (docData["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "💬 CHAT: Error parsing conversation ${doc.id}")
                        null
                    }
                } ?: emptyList()
                
                trySend(conversations)
            }
        
        awaitClose { listener.remove() }
    }
    
    // ==========================================
    // MESSAGE MANAGEMENT
    // ==========================================
    
    /**
     * Send a message in a conversation
     */
    suspend fun sendMessage(
        conversationId: String,
        message: String,
        type: MessageType = MessageType.TEXT
    ): Result<String> {
        return try {
            Timber.d("💬 CHAT: Sending message to conversation $conversationId")
            
            val data = hashMapOf(
                "conversationId" to conversationId,
                "message" to message,
                "type" to type.name
            )
            
            val result = functions
                .getHttpsCallable("sendChatMessage")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            val messageId = response["messageId"] as String
            
            Timber.d("💬 CHAT: ✅ Message sent: $messageId")
            Result.success(messageId)
            
        } catch (e: Exception) {
            Timber.e(e, "💬 CHAT: Error sending message")
            Result.failure(e)
        }
    }
    
    /**
     * Get messages for a conversation as a Flow (real-time updates)
     */
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        Timber.d("💬 CHAT: Listening to messages in $conversationId")
        
        val listener = firestore.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "💬 CHAT: Error listening to messages")
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val docData = doc.data ?: return@mapNotNull null
                        ChatMessage(
                            id = doc.id,
                            conversationId = docData["conversationId"] as? String ?: "",
                            senderId = docData["senderId"] as? String ?: "",
                            recipientId = docData["recipientId"] as? String ?: "",
                            message = docData["message"] as? String ?: "",
                            type = try { MessageType.valueOf(docData["type"] as? String ?: "TEXT") } catch (e: Exception) { MessageType.TEXT },
                            isRead = docData["isRead"] as? Boolean ?: false,
                            createdAt = (docData["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
                            readAt = (docData["readAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "💬 CHAT: Error parsing message ${doc.id}")
                        null
                    }
                } ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Mark all messages in a conversation as read
     */
    suspend fun markMessagesAsRead(conversationId: String): Result<Int> {
        return try {
            Timber.d("💬 CHAT: Marking messages as read in $conversationId")
            
            val data = hashMapOf("conversationId" to conversationId)
            
            val result = functions
                .getHttpsCallable("markMessagesAsRead")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as Map<String, Any>
            val markedCount = (response["markedCount"] as? Number)?.toInt() ?: 0
            
            Timber.d("💬 CHAT: Marked $markedCount messages as read")
            Result.success(markedCount)
            
        } catch (e: Exception) {
            Timber.e(e, "💬 CHAT: Error marking messages as read")
            Result.failure(e)
        }
    }
    
    // ==========================================
    // UTILITY FUNCTIONS
    // ==========================================
    
    /**
     * Get the other participant's info from a conversation
     */
    fun getOtherParticipant(conversation: Conversation): ParticipantInfo? {
        val currentUserId = auth.currentUser?.uid ?: return null
        val otherUserId = conversation.participants.find { it != currentUserId } ?: return null
        return conversation.participantDetails[otherUserId]
    }
    
    /**
     * Get unread count for current user
     */
    fun getUnreadCount(conversation: Conversation): Int {
        val currentUserId = auth.currentUser?.uid ?: return 0
        return conversation.unreadCount[currentUserId] ?: 0
    }
    
    /**
     * Get total unread messages across all conversations
     */
    suspend fun getTotalUnreadCount(): Int {
        val userId = auth.currentUser?.uid ?: return 0
        
        return try {
            val conversations = firestore.collection("conversations")
                .whereArrayContains("participants", userId)
                .get()
                .await()
            
            conversations.documents.sumOf { doc ->
                val unreadCount = doc.data?.get("unreadCount") as? Map<*, *>
                (unreadCount?.get(userId) as? Number)?.toInt() ?: 0
            }
        } catch (e: Exception) {
            Timber.e(e, "💬 CHAT: Error getting total unread count")
            0
        }
    }
    
    // ==========================================
    // PRIVATE HELPERS
    // ==========================================
    
    @Suppress("UNCHECKED_CAST")
    private fun parseParticipantDetails(data: Any?): Map<String, ParticipantInfo> {
        val map = data as? Map<String, Map<String, Any>> ?: return emptyMap()
        return map.mapValues { (_, value) ->
            ParticipantInfo(
                name = value["name"] as? String ?: "",
                profileImage = value["profileImage"] as? String,
                role = value["role"] as? String ?: ""
            )
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun parseUnreadCount(data: Any?): Map<String, Int> {
        val map = data as? Map<String, Any> ?: return emptyMap()
        return map.mapValues { (_, value) -> (value as? Number)?.toInt() ?: 0 }
    }
}
