package com.example.dutype.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.firestore.FirestoreSchema.PhoneRoles
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

    private val initialCachedDestination: String by lazy {
        val cached = StartDestinationCache.read(context)
        // Profile-setup routes need a signed-in uid.
        // Home routes (WORKER_HOME / EMPLOYER_HOME) support guest exploration.
        val isAuthGated = cached == Routes.PROFILE_SETUP ||
            cached == Routes.EMPLOYER_PROFILE_SETUP
        if (isAuthGated && FirebaseAuth.getInstance().currentUser == null) {
            Routes.SELECT_ROLE
        } else {
            cached ?: Routes.ONBOARDING
        }
    }

    private val _startupState = MutableStateFlow<StartupState>(
        StartupState.Resolved(initialCachedDestination)
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

                // 2. Check Role & Auth (Guest Mode removed)
                val currentUser = FirebaseAuth.getInstance().currentUser
                val dataStoreRole = profileSetupStateManager.getUserRole()

                if (currentUser == null) {
                    Timber.d("🚀 AppStartupViewModel -> SELECT_ROLE (User not authenticated)")
                    updateDestination(Routes.SELECT_ROLE)
                    return@launch
                }

                // Accounts whose login token predates the role claim (set by the server, e.g. after the
                // data migration) refresh it once in the background, so role-checked server calls work.
                viewModelScope.launch {
                    runCatching {
                        if (currentUser.getIdToken(false).await().claims["role"] == null) currentUser.getIdToken(true).await()
                    }.onFailure { Timber.w(it, "role claim refresh failed") }
                }

                // 3. Fast Path: Check DataStore
                if (dataStoreRole != null && profileSetupStateManager.isProfileComplete(dataStoreRole)) {
                    val dest = if (dataStoreRole == UserRole.WORKER) Routes.WORKER_HOME else Routes.EMPLOYER_HOME
                    Timber.d("🚀 AppStartupViewModel -> Fast Path: $dest")
                    updateDestination(dest)
                    return@launch
                }

                // 4. Slow Path: the role is a custom claim on the ID token (no read); phoneRoles
                // is read only for accounts whose token predates the claim.
                val claimRole = try {
                    withTimeoutOrNull(2000L) { currentUser.getIdToken(false).await().claims["role"] as? String }
                } catch (e: Exception) {
                    Timber.w(e, "🚀 AppStartupViewModel - token read failed")
                    null
                }
                val firestoreRoleStr = claimRole ?: try {
                    val phone = currentUser.phoneNumber?.let(PhoneNumberUtils::normalize).orEmpty()
                    if (phone.isBlank()) null else withTimeoutOrNull(2000L) {
                        firestore.collection(PhoneRoles.COLLECTION).document(phone).get().await().getString(PhoneRoles.ROLE)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "🚀 AppStartupViewModel - Error reading phoneRoles doc")
                    null
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
                    val localProfileComplete = profileSetupStateManager.isProfileComplete(userRole)
                    var completionUnknown = false
                    val firestoreProfileComplete = if (!localProfileComplete) {
                        val check = runCatching {
                            profileCompletionService.isProfileComplete(currentUser.uid, userRole.name)
                        }.getOrNull()
                        // Failure (offline, nothing cached) = unknown, NOT "incomplete".
                        completionUnknown = check == null || check.isFailure
                        check?.getOrNull() ?: false
                    } else true

                    // Offline and unknown: keep the last resolved home destination instead of
                    // sending a completed user back to profile setup.
                    val cachedHome = initialCachedDestination
                    if (completionUnknown && (cachedHome == Routes.WORKER_HOME || cachedHome == Routes.EMPLOYER_HOME)) {
                        Timber.w("🚀 AppStartupViewModel - completion unknown (offline); keeping cached $cachedHome")
                        updateDestination(cachedHome)
                        return@launch
                    }

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
