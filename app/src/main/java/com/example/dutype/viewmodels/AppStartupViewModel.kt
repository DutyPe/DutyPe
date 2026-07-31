package com.example.dutype.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.navigation.StartDestinationCache
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ProfileSetupStateManager
import com.example.dutype.utils.PhoneNumberUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

sealed interface StartupState {
    object Loading : StartupState
    data class Resolved(val startDestination: String) : StartupState
}

@HiltViewModel
class AppStartupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileCompletionService: ProfileCompletionService,
    private val profileSetupStateManager: ProfileSetupStateManager,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val initialCachedDestination: String? by lazy {
        val cached = StartDestinationCache.read(context)
        // Profile-setup routes need a signed-in uid just as much as the home routes do.
        // Omitting them let a logged-out cold start open mandatory setup with no user.
        val isAuthGated = cached == Routes.WORKER_HOME ||
            cached == Routes.EMPLOYER_HOME ||
            cached == Routes.PROFILE_SETUP ||
            cached == Routes.EMPLOYER_PROFILE_SETUP
        if (isAuthGated && FirebaseAuth.getInstance().currentUser == null) {
            null
        } else {
            cached
        }
    }

    private val _startupState = MutableStateFlow<StartupState>(
        if (initialCachedDestination != null) StartupState.Resolved(initialCachedDestination!!)
        else StartupState.Loading
    )
    val startupState: StateFlow<StartupState> = _startupState.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            try {
                Timber.d("🚀 AppStartupViewModel - Starting resolution...")

                // 1. Check Onboarding
                val hasCompletedOnboarding = profileSetupStateManager.hasOnboardingBeenCompleted()
                if (!hasCompletedOnboarding) {
                    Timber.d("🚀 AppStartupViewModel -> ONBOARDING")
                    updateDestination(Routes.ONBOARDING)
                    return@launch
                }

                // 2. Check FirebaseAuth
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Timber.d("🚀 AppStartupViewModel -> SELECT_ROLE")
                    updateDestination(Routes.SELECT_ROLE)
                    return@launch
                }

                // 3. Fast Path: Check DataStore
                val dataStoreRole = profileSetupStateManager.getUserRole()
                if (dataStoreRole != null && profileSetupStateManager.isProfileComplete(dataStoreRole)) {
                    val dest = if (dataStoreRole == UserRole.WORKER) Routes.WORKER_HOME else Routes.EMPLOYER_HOME
                    Timber.d("🚀 AppStartupViewModel -> Fast Path: $dest")
                    updateDestination(dest)
                    return@launch
                }

                // 4. Slow Path: Check Firestore asynchronously with timeout
                val normalizedPhone: String = currentUser.phoneNumber
                    ?.let(PhoneNumberUtils::normalize)
                    .orEmpty()

                val phoneRoleDoc = try {
                    withTimeoutOrNull(2000L) {
                        if (normalizedPhone.isNotBlank()) {
                            firestore.collection(FirestoreCollections.PHONE_ROLES)
                                .document(normalizedPhone)
                                .get()
                                .await()
                        } else null
                    }
                } catch (e: Exception) {
                    Timber.e(e, "🚀 AppStartupViewModel - Error reading phoneRoles doc")
                    null
                }
                val firestoreRoleStr = phoneRoleDoc?.getString("role")

                val profileDocExists: Boolean = try {
                    val profileCollection = if (firestoreRoleStr?.uppercase() == "EMPLOYER") {
                        FirestoreCollections.EMPLOYER_PROFILES
                    } else {
                        FirestoreCollections.WORKER_PROFILES
                    }
                    withTimeoutOrNull(2000L) {
                        firestore.collection(profileCollection)
                            .document(currentUser.uid)
                            .get()
                            .await()
                            .exists()
                    } ?: true
                } catch (e: Exception) {
                    Timber.e(e, "🚀 AppStartupViewModel - Error reading profile doc")
                    true
                }

                var userRole: UserRole? = null
                if (firestoreRoleStr != null) {
                    userRole = runCatching { UserRole.valueOf(firestoreRoleStr.uppercase()) }.getOrNull()
                    if (userRole != null) {
                        runCatching { profileSetupStateManager.saveUserRole(userRole) }
                    }
                }
                if (userRole == null) {
                    userRole = profileSetupStateManager.getUserRole()
                }

                val targetDestination = if (userRole != null) {
                    val localProfileComplete = profileDocExists && profileSetupStateManager.isProfileComplete(userRole)
                    val firestoreProfileComplete = if (!localProfileComplete) {
                        runCatching {
                            profileCompletionService.isProfileComplete(currentUser.uid, userRole.name).getOrDefault(false)
                        }.getOrDefault(false)
                    } else true

                    val isProfileComplete = localProfileComplete || firestoreProfileComplete

                    if (isProfileComplete && !localProfileComplete) {
                        runCatching {
                            profileSetupStateManager.markProfileComplete(userRole)
                            profileSetupStateManager.markProfileSetupAsShown(userRole)
                        }
                    }

                    if (isProfileComplete && userRole == UserRole.WORKER) Routes.WORKER_HOME
                    else if (isProfileComplete && userRole == UserRole.EMPLOYER) Routes.EMPLOYER_HOME
                    else if (userRole == UserRole.WORKER) Routes.PROFILE_SETUP
                    else if (userRole == UserRole.EMPLOYER) Routes.EMPLOYER_PROFILE_SETUP
                    else Routes.SELECT_ROLE
                } else {
                    Routes.SELECT_ROLE
                }

                Timber.d("🚀 AppStartupViewModel -> Resolved: $targetDestination")
                updateDestination(targetDestination)

            } catch (e: Exception) {
                Timber.e(e, "🚀 AppStartupViewModel - Error resolving start destination, fallback")
                updateDestination(Routes.SELECT_ROLE)
            }
        }
    }

    private fun updateDestination(destination: String) {
        runCatching { StartDestinationCache.save(context, destination) }
        _startupState.value = StartupState.Resolved(destination)
    }
}
