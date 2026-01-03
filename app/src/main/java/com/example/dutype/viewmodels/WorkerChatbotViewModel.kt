package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.repositories.AIBackendRepository
import com.example.dutype.services.ai.ChatResponse
import com.example.dutype.services.ai.JobSafetyResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Worker Chatbot ViewModel
 * 
 * Handles worker AI assistant interactions:
 * - Job safety checks
 * - Application status explanations
 * - Employer warnings
 * - Platform guidance
 * 
 * Supports Hinglish (Hindi + English) for Indian workers
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

data class WorkerChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = true,
    val currentJobSafety: JobSafetyResponse? = null
)

@HiltViewModel
class WorkerChatbotViewModel @Inject constructor(
    private val aiRepository: AIBackendRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "WorkerChatbot"
        
        // Quick action suggestions
        val QUICK_ACTIONS = listOf(
            "Is this job safe?",
            "Why is my application pending?",
            "How to spot fake jobs?",
            "Report a scam",
            "Contact support"
        )

        // Welcome message
        private val WELCOME_MESSAGE = """
            👋 Hi! I'm your DutyPe assistant.
            I can help you with:
            • Check if a job is safe
            • Explain application status
            • Warn about suspicious employers
            • Report scams
            Aap Hindi mein bhi baat kar sakte ho! 🇮🇳
        """.trimIndent()
    }
    private val _uiState = MutableStateFlow(WorkerChatUiState())
    val uiState: StateFlow<WorkerChatUiState> = _uiState.asStateFlow()
    
    private var workerId: String = ""
    private var currentJobContext: Map<String, Any>? = null
    private var currentEmployerContext: Map<String, Any>? = null
    
    init {
        // Add welcome message
        addBotMessage(WELCOME_MESSAGE)
        checkConnection()
    }
    
    /**
     * Initialize with worker ID
     */
    fun initialize(workerId: String) {
        this.workerId = workerId
    }
    
    /**
     * Set current job context for safety checks
     */
    fun setJobContext(job: Map<String, Any>, employer: Map<String, Any>? = null) {
        currentJobContext = job
        currentEmployerContext = employer
    }
    
    /**
     * Send a message to the chatbot
     **/
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        // Add user message
        addUserMessage(message)
        
        // Show loading
        val loadingMessage = ChatMessage(
            content = "...",
            isFromUser = false,
            isLoading = true
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + loadingMessage,
            isLoading = true
        )
        
        viewModelScope.launch {
            try {
                // Build context
                val context = mutableMapOf<String, Any>()
                currentJobContext?.let { context["current_job"] = it }
                currentEmployerContext?.let { context["current_employer"] = it }
                
                val result = aiRepository.workerChat(
                    message = message,
                    workerId = workerId,
                    context = context.takeIf { it.isNotEmpty() }
                )
                
                // Remove loading message
                val messagesWithoutLoading = _uiState.value.messages.filter { !it.isLoading }
                
                result.fold(
                    onSuccess = { response ->
                        _uiState.value = _uiState.value.copy(
                            messages = messagesWithoutLoading + ChatMessage(
                                content = response.reply,
                                isFromUser = false
                            ),
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { error ->
                        Timber.e("$TAG: Chat error: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            messages = messagesWithoutLoading + ChatMessage(
                                content = "Sorry, I couldn't process that. Please try again.",
                                isFromUser = false,
                                isError = true
                            ),
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Chat exception")
                val messagesWithoutLoading = _uiState.value.messages.filter { !it.isLoading }
                _uiState.value = _uiState.value.copy(
                    messages = messagesWithoutLoading + ChatMessage(
                        content = "Connection error. Please check your internet.",
                        isFromUser = false,
                        isError = true
                    ),
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Quick job safety check
     */
    fun checkJobSafety() {
        val job = currentJobContext ?: run {
            addBotMessage("Please open a job first to check its safety.")
            return
        }
        
        addUserMessage("Is this job safe?")
        
        val loadingMessage = ChatMessage(
            content = "Analyzing job safety...",
            isFromUser = false,
            isLoading = true
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + loadingMessage,
            isLoading = true
        )
        
        viewModelScope.launch {
            try {
                val result = aiRepository.checkJobSafety(job, currentEmployerContext)
                
                val messagesWithoutLoading = _uiState.value.messages.filter { !it.isLoading }
                
                result.fold(
                    onSuccess = { response ->
                        val safetyEmoji = when (response.riskLevel) {
                            "LOW" -> "✅"
                            "MEDIUM" -> "⚠️"
                            "HIGH" -> "🚨"
                            "CRITICAL" -> "⛔"
                            else -> "❓"
                        }
                        
                        val message = buildString {
                            append("$safetyEmoji Job Safety: ${response.riskLevel}\n\n")
                            append(response.explanation)
                            
                            if (response.warnings.isNotEmpty()) {
                                append("\n\n⚠️ Warnings:\n")
                                response.warnings.forEach { append("• $it\n") }
                            }
                            
                            if (response.redFlags.isNotEmpty()) {
                                append("\n🚩 Red Flags:\n")
                                response.redFlags.forEach { append("• $it\n") }
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            messages = messagesWithoutLoading + ChatMessage(
                                content = message,
                                isFromUser = false
                            ),
                            isLoading = false,
                            currentJobSafety = response
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            messages = messagesWithoutLoading + ChatMessage(
                                content = "Couldn't check job safety. Try again later.",
                                isFromUser = false,
                                isError = true
                            ),
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Job safety check error")
            }
        }
    }
    
    /**
     * Handle quick action tap
     */
    fun onQuickAction(action: String) {
        when (action) {
            "Is this job safe?" -> checkJobSafety()
            "Report a scam" -> sendMessage("I want to report a scam")
            else -> sendMessage(action)
        }
    }
    
    /**
     * Check connection to AI backend
     */
    private fun checkConnection() {
        viewModelScope.launch {
            val result = aiRepository.checkHealth()
            _uiState.value = _uiState.value.copy(
                isConnected = result.isSuccess
            )
        }
    }
    
    /**
     * Clear chat history
     */
    fun clearChat() {
        _uiState.value = WorkerChatUiState()
        addBotMessage(WELCOME_MESSAGE)
    }
    
    private fun addUserMessage(content: String) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + ChatMessage(
                content = content,
                isFromUser = true
            )
        )
    }
    
    private fun addBotMessage(content: String) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + ChatMessage(
                content = content,
                isFromUser = false
            )
        )
    }
}
