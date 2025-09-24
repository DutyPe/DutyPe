package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.auth.AuthManager
import com.example.partimes.models.User
import com.example.partimes.network.ApiClient
import com.example.partimes.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Check if user is already logged in
        _isLoggedIn.value = authRepository.isLoggedIn()
        _currentUser.value = authRepository.getCurrentUser()
    }
    
    fun verifyPhone(firebaseToken: String, phoneNumber: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = authRepository.verifyPhone(firebaseToken, phoneNumber)
                result.fold(
                    onSuccess = { data ->
                        _isLoggedIn.value = true
                        _currentUser.value = authRepository.getCurrentUser()
                        _isLoading.value = false
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        _isLoading.value = false
                        _errorMessage.value = exception.message
                        callback(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = e.message
                callback(false, e.message)
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                _isLoggedIn.value = false
                _currentUser.value = null
                _errorMessage.value = null
            } catch (e: Exception) {
                // Even if logout fails, clear local state
                _isLoggedIn.value = false
                _currentUser.value = null
            }
        }
    }
    
    fun updateProfile(user: User, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = authRepository.updateProfile(user)
                result.fold(
                    onSuccess = { updatedUser ->
                        _currentUser.value = updatedUser
                        _isLoading.value = false
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        _isLoading.value = false
                        _errorMessage.value = exception.message
                        callback(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = e.message
                callback(false, e.message)
            }
        }
    }
    
    fun getProfile(callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = authRepository.getProfile()
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        _isLoading.value = false
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        _isLoading.value = false
                        _errorMessage.value = exception.message
                        callback(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = e.message
                callback(false, e.message)
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
