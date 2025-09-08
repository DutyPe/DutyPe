package com.example.partimes.jobseeker.repositories

import com.example.partimes.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserRepository {
    // Simulate user data
    private val _currentUser = MutableStateFlow(
        User(id = "user123", name = "John Doe", phoneNumber = "9876543210")
    )
    val currentUser: StateFlow<User> = _currentUser

    // You could later connect this to your auth backend
}
