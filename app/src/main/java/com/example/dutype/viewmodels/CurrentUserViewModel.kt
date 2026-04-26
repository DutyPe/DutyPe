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
                val normalizedPhone = auth.currentUser?.phoneNumber
                    ?.let(com.example.dutype.utils.PhoneNumberUtils::normalize)
                    .orEmpty()
                val phoneRoleData = if (normalizedPhone.isNotBlank()) {
                    firestore.collection(FirestoreCollections.PHONE_ROLES).document(normalizedPhone).get().await().data.orEmpty()
                } else {
                    emptyMap()
                }
                val role = (phoneRoleData["roles"] as? List<*>)?.firstOrNull()?.toString()?.uppercase()
                val profileCollection = if (role == UserRole.EMPLOYER.name) {
                    FirestoreCollections.EMPLOYER_PROFILES
                } else {
                    FirestoreCollections.WORKER_PROFILES
                }
                val profileDoc = firestore.collection(profileCollection).document(uid).get().await()
                if (!profileDoc.exists() && phoneRoleData.isEmpty()) return@launch
                val data = profileDoc.data.orEmpty().toMutableMap().apply {
                    putIfAbsent("role", role ?: get("role") ?: UserRole.WORKER.name)
                    (phoneRoleData["phoneNumber"] as? String)?.let { put("phone", it) }
                    (phoneRoleData["name"] as? String)?.let { put("fullName", it) }
                }
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
