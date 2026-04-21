package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Read-only viewmodel for the currently authenticated user.
 *
 * Replaces the dual-role `RoleManagementViewModel`. It exposes the user's
 * single role and basic identity fields. There is no API for changing the
 * role at runtime — accounts are single-role for life.
 */
@HiltViewModel
class CurrentUserViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                val doc = firestore.collection(FirestoreCollections.USERS).document(uid).get().await()
                if (!doc.exists()) return@launch
                val data = doc.data ?: return@launch
                _currentUser.value = User.fromFirestoreMap(uid, data)
                Timber.d("CurrentUserViewModel - loaded uid=%s role=%s", uid, _currentUser.value?.role)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "CurrentUserViewModel - load failed")
            }
        }
    }
}
