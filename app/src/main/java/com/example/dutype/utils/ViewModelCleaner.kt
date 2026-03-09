package com.example.dutype.utils

import com.example.dutype.models.UserRole
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ViewModelCleaner - Clears role-specific ViewModels during role switch
 * 
 * Architecture:
 * - Worker-specific ViewModels: AllJobsViewModel, CategoriesViewModel, WorkerHomeViewModel, etc.
 * - Employer-specific ViewModels: EmployerApplicationViewModel, AIJobPostingViewModel, etc.
 * - Shared ViewModels: ProfileViewModel, RoleManagementViewModel, ReferralViewModel (preserved)
 * 
 * Note: In Jetpack Compose with Hilt, ViewModels are automatically cleared when their
 * associated composables are removed from composition. This utility provides explicit
 * cleanup hooks for additional state management if needed.
 */
@Singleton
class ViewModelCleaner @Inject constructor() {
    
    /**
     * Clear ViewModels specific to a role
     * 
     * In Compose + Hilt architecture, ViewModels are scoped to their composables.
     * When we navigate away from a role's screens, those ViewModels are automatically
     * cleared by the framework. This method provides logging and explicit cleanup hooks.
     */
    fun clearRoleSpecificViewModels(role: UserRole) {
        when (role) {
            UserRole.WORKER -> clearWorkerViewModels()
            UserRole.EMPLOYER -> clearEmployerViewModels()
            else -> Timber.w("⚠️ VIEWMODEL_CLEANER: Unknown role $role")
        }
    }
    
    /**
     * Clear Worker-specific ViewModels
     * 
     * Worker ViewModels:
     * - AllJobsViewModel (job listings)
     * - CategoriesViewModel (job categories)
     * - WorkerHomeViewModel (home screen state)
     * - JobApplicationViewModel (application state)
     * - WorkerNotificationViewModel (worker notifications)
     */
    private fun clearWorkerViewModels() {
        Timber.d("🧹 VIEWMODEL_CLEANER: Clearing Worker ViewModels")
        
        // In Compose + Hilt, ViewModels are automatically cleared when screens are removed
        // from composition. We just log for debugging purposes.
        
        Timber.d("  ✓ AllJobsViewModel will be cleared")
        Timber.d("  ✓ CategoriesViewModel will be cleared")
        Timber.d("  ✓ WorkerHomeViewModel will be cleared")
        Timber.d("  ✓ JobApplicationViewModel will be cleared")
        Timber.d("  ✓ WorkerNotificationViewModel will be cleared")
        
        // Additional cleanup can be added here if needed
        // For example, canceling ongoing coroutines, clearing listeners, etc.
    }
    
    /**
     * Clear Employer-specific ViewModels
     * 
     * Employer ViewModels:
     * - EmployerApplicationViewModel (application management)
     * - AIJobPostingViewModel (job posting)
     * - EmployerHomeViewModel (home screen state)
     * - EmployerNotificationViewModel (employer notifications)
     */
    private fun clearEmployerViewModels() {
        Timber.d("🧹 VIEWMODEL_CLEANER: Clearing Employer ViewModels")
        
        // In Compose + Hilt, ViewModels are automatically cleared when screens are removed
        // from composition. We just log for debugging purposes.
        
        Timber.d("  ✓ EmployerApplicationViewModel will be cleared")
        Timber.d("  ✓ AIJobPostingViewModel will be cleared")
        Timber.d("  ✓ EmployerHomeViewModel will be cleared")
        Timber.d("  ✓ EmployerNotificationViewModel will be cleared")
        
        // Additional cleanup can be added here if needed
    }
    
    /**
     * Shared ViewModels that should NOT be cleared:
     * - ProfileViewModel (user profile data)
     * - RoleManagementViewModel (role switching logic)
     * - ReferralViewModel (referral system)
     * - AuthViewModel (authentication state)
     * - ProfileCompletionViewModel (profile completion state)
     * 
     * These ViewModels contain data that persists across role switches.
     */
}
