package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.repositories.AIBackendRepository
import com.example.dutype.services.ai.ScoreBreakdownItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Employer Chatbot ViewModel
 * 
 * Handles employer AI assistant interactions:
 * - Score explanations
 * - Penalty reasons
 * - Improvement tips
 * - Platform rules
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */

data class EmployerChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = true,
    val scoreBreakdown: List<ScoreBreakdownItem>? = null
)

@HiltViewModel
class EmployerChatbotViewModel @Inject constructor(
    private val aiRepository: AIBackendRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "EmployerChatbot"
        
        // Quick action suggestions
        val QUICK_ACTIONS = listOf(
            "Why is my score low?",
            "How to improve my rating?",
            "Why was my job blocked?",
            "Posting guidelines",
            "Contact support"
        )
        
        // Welcome message
        private val WELCOME_MESSAGE = """
            👋 Hello! I'm your DutyPe business assistant.
            
            I can help you with:
            • Explain your reliability score
            • Understand penalties
            • Tips to improve your rating
            • Platform posting rules
            
            How can I help you today?
        """.trimIndent()
    }
    
    private val _uiState = MutableStateFlow(EmployerChatUiState())
    val uiState: StateFlow<EmployerChatUiState> = _uiState.asStateFlow()
    
    private var employerId: String = ""
    private var currentScoreContext: Map<String, Any>? = null
    private var currentMetricsContext: Map<String, Any>? = null
    private var currentPenaltyContext: Map<String, Any>? = null
    
    init {
        addBotMessage(WELCOME_MESSAGE)
        checkConnection()
    }
    
    /**
     * Initialize with employer ID
     */
    fun initialize(employerId: String) {
        this.employerId = employerId
    }
    
    /**
     * Set current score context for explanations
     */
    fun setScoreContext(score: Map<String, Any>, metrics: Map<String, Any>) {
        currentScoreContext = score
        currentMetricsContext = metrics
    }
    
    /**
     * Set penalty context for explanations
     */
    fun setPenaltyContext(penalty: Map<String, Any>) {
        currentPenaltyContext = penalty
    }
    
    /**
     * Send a message to the chatbot
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        addUserMessage(message)
        
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
                val context = mutableMapOf<String, Any>()
                currentScoreContext?.let { context["current_score"] = it }
                currentMetricsContext?.let { context["current_metrics"] = it }
                currentPenaltyContext?.let { context["current_penalty"] = it }
                
                val result = aiRepository.employerChat(
                    message = message,
                    employerId = employerId,
                    context = context.takeIf { it.isNotEmpty() }
                )
                
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
     * Get detailed score explanation
     */
    fun explainScore() {
        val score = currentScoreContext ?: run {
            addBotMessage("Score data not available. Please refresh your profile.")
            return
        }
        val metrics = currentMetricsContext ?: mapOf()
        
        addUserMessage("Why is my score like this?")
        
        val loadingMessage = ChatMessage(
            content = "Analyzing your score...",
            isFromUser = false,
            isLoading = true
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + loadingMessage,
            isLoading = true
        )
        
        viewModelScope.launch {
            try {
                val result = aiRepository.explainEmployerScore(score, metrics)
                
                val messagesWithoutLoading = _uiState.value.messages.filter { !it.isLoading }
                
                result.fold(
                    onSuccess = { response ->
                        val message = buildString {
                            append("📊 Score Breakdown\n\n")
                            append(response.explanation)
                            
                            if (response.breakdown.isNotEmpty()) {
                                append("\n\n📈 Factors:\n")
                                response.breakdown.forEach { item ->
                                    val emoji = when (item.impact) {
                                        "POSITIVE" -> "✅"
                                        "NEGATIVE" -> "❌"
                                        else -> "➖"
                                    }
                                    append("$emoji ${item.factor}: ${item.description}\n")
                                }
                            }
                            
                            if (response.improvementAreas.isNotEmpty()) {
                                append("\n💡 Areas to Improve:\n")
                                response.improvementAreas.forEach { append("• $it\n") }
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            messages = messagesWithoutLoading + ChatMessage(
                                content = message,
                                isFromUser = false
                            ),
                            isLoading = false,
                            scoreBreakdown = response.breakdown
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            messages = messagesWithoutLoading + ChatMessage(
                                content = "Couldn't analyze score. Try again later.",
                                isFromUser = false,
                                isError = true
                            ),
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Score explanation error")
            }
        }
    }
    
    /**
     * Get improvement tips
     */
    fun getImprovementTips() {
        val score = currentScoreContext ?: mapOf("reliability_score" to 50)
        val metrics = currentMetricsContext ?: mapOf()
        
        addUserMessage("How can I improve my rating?")
        
        val loadingMessage = ChatMessage(
            content = "Generating personalized tips...",
            isFromUser = false,
            isLoading = true
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + loadingMessage,
            isLoading = true
        )
        
        viewModelScope.launch {
            try {
                val result = aiRepository.getImprovementTips(score, metrics)
                
                val messagesWithoutLoading = _uiState.value.messages.filter { !it.isLoading }
                
                result.fold(
                    onSuccess = { response ->
                        val message = buildString {
                            append("💡 Improvement Tips\n\n")
                            append(response.tips)
                            
                            if (response.weakAreas.isNotEmpty()) {
                                append("\n\n⚠️ Areas to Focus:\n")
                                response.weakAreas.forEach { append("• $it\n") }
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            messages = messagesWithoutLoading + ChatMessage(
                                content = message,
                                isFromUser = false
                            ),
                            isLoading = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            messages = messagesWithoutLoading + ChatMessage(
                                content = "Couldn't generate tips. Try again later.",
                                isFromUser = false,
                                isError = true
                            ),
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Improvement tips error")
            }
        }
    }
    
    /**
     * Handle quick action tap
     */
    fun onQuickAction(action: String) {
        when (action) {
            "Why is my score low?" -> explainScore()
            "How to improve my rating?" -> getImprovementTips()
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
        _uiState.value = EmployerChatUiState()
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
