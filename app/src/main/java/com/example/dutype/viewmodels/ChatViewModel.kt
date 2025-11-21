package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.ChatChannel
import com.example.dutype.models.ChatMessage
import com.example.dutype.repositories.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val channels: List<ChatChannel> = emptyList(),
    val currentChannelMessages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentChannelId: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            chatRepository.getChannels().collect { channels ->
                // Hydrate channels with user data in parallel
                val hydratedChannels = channels.map { channel ->
                    async { chatRepository.hydrateChannel(channel) }
                }.awaitAll()
                
                _uiState.value = _uiState.value.copy(channels = hydratedChannels)
            }
        }
    }

    fun selectChannel(channelId: String) {
        _uiState.value = _uiState.value.copy(currentChannelId = channelId)
        loadMessages(channelId)
    }

    private fun loadMessages(channelId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(channelId).collect { messages ->
                _uiState.value = _uiState.value.copy(currentChannelMessages = messages)
            }
        }
    }

    fun sendMessage(text: String) {
        val channelId = _uiState.value.currentChannelId ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(channelId, text)
        }
    }

    fun createChannel(otherUserId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.createChannel(otherUserId)
            result.fold(
                onSuccess = { channelId ->
                    onResult(channelId)
                },
                onFailure = {
                    onResult(null)
                }
            )
        }
    }
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
