package com.example.partimes.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class UserViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    val phoneNumber: StateFlow<String> = repository.currentUser
        .map { it.phoneNumber }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
}
