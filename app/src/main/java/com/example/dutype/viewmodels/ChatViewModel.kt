package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import com.example.dutype.services.ChatService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ChatViewModel - Provides ChatService for screens that need it
 * 
 * This replaces the deprecated ChatServiceProvider pattern.
 * Screens should use this ViewModel to access ChatService.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    val chatService: ChatService
) : ViewModel()
