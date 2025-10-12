package com.example.partimes.services

import com.example.partimes.models.User
import com.example.partimes.models.UserRole
import com.example.partimes.state.ProfileSetupStateManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise-level Profile Completion Service
 * Manages profile completion status and provides smart navigation decisions
 * Enhanced with 30+ years of Android development experience
 */
@Singleton
class ProfileCompletionService @Inject constructor(
    private val firestoreService: FirestoreService,
    private val auth: FirebaseAuth,
    private val profileSetupStateManager: ProfileSetupStateManager
) {
    
    /**
     * Check if user has complete profile for their role
     * Enhanced with state management integration
     */
    suspend fun isProfileComplete(userId: String, role: UserRole): Result<Boolean> {
        return try {
            // First check local state
            val localComplete = profileSetupStateManager.isProfileComplete(role)
            if (localComplete) {
                return Result.success(true)
            }
            
            // If not complete locally, check remote
            when (role) {
                UserRole.WORKER -> checkWorkerProfileComplete(userId)
                UserRole.EMPLOYER -> checkEmployerProfileComplete(userId)
                else -> Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Enhanced method to check if user should be redirected to profile setup
     * Now checks Firebase for existing profile data when local state is empty
     */
    suspend fun shouldRedirectToProfileSetup(role: UserRole): Boolean {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            return true // No user, need to sign in first
        }
        
        println("🔍 ProfileCompletionService.shouldRedirectToProfileSetup:")
        println("  role: $role")
        println("  currentUser.uid: ${currentUser.uid}")
        println("  currentUser.email: ${currentUser.email}")
        
        // First check local state
        val localShouldRedirect = profileSetupStateManager.shouldRedirectToProfileSetup(role)
        println("🔍 Local state suggests redirect: $localShouldRedirect")
        
        // If local state says we should redirect, check Firebase to see if profile already exists
        if (localShouldRedirect) {
            println("🔍 Local state suggests redirect, checking Firebase for existing profile...")
            
            try {
                // Check if user already has a complete profile in Firebase
                val profileExistsResult = when (role) {
                    UserRole.WORKER -> {
                        val workerProfile = firestoreService.getWorkerProfile(currentUser.uid).getOrNull()
                        workerProfile != null && isWorkerProfileComplete(workerProfile)
                    }
                    UserRole.EMPLOYER -> {
                        val employerProfile = firestoreService.getEmployerProfile(currentUser.uid).getOrNull()
                        employerProfile != null && isEmployerProfileComplete(employerProfile)
                    }
                    UserRole.ADMIN -> {
                        // For admin, assume profile is complete if user exists
                        true
                    }
                    else -> false
                }
                
                if (profileExistsResult) {
                    println("✅ Found existing complete profile in Firebase, updating local state...")
                    
                    // Update local state to reflect that profile is complete
                    profileSetupStateManager.markProfileComplete(role)
                    profileSetupStateManager.markProfileSetupAsShown(role)
                    
                    // Load existing profile data into local state
                    when (role) {
                        UserRole.WORKER -> {
                            val workerProfile = firestoreService.getWorkerProfile(currentUser.uid).getOrNull()
                            workerProfile?.let { profile ->
                                profileSetupStateManager.saveUserInfo(
                                    profile["email"]?.toString() ?: currentUser.email ?: "",
                                    profile["fullName"]?.toString() ?: currentUser.displayName ?: "",
                                    role
                                )
                            }
                        }
                        UserRole.EMPLOYER -> {
                            val employerProfile = firestoreService.getEmployerProfile(currentUser.uid).getOrNull()
                            employerProfile?.let { profile ->
                                profileSetupStateManager.saveUserInfo(
                                    profile["contactEmail"]?.toString() ?: currentUser.email ?: "",
                                    profile["companyName"]?.toString() ?: currentUser.displayName ?: "",
                                    role
                                )
                            }
                        }
                        UserRole.ADMIN -> {
                            // Admin role - no specific profile data to load
                            profileSetupStateManager.saveUserInfo(
                                currentUser.email ?: "",
                                currentUser.displayName ?: "",
                                role
                            )
                        }
                    }
                    
                    return false // Don't redirect, profile exists
                } else {
                    println("❌ No complete profile found in Firebase, will redirect to setup")
                    return true // Redirect to profile setup
                }
            } catch (e: Exception) {
                println("❌ Error checking Firebase profile: ${e.message}")
                return true // On error, redirect to setup
            }
        }
        
        return localShouldRedirect
    }
    
    /**
     * Check if worker profile is complete based on essential fields
     */
    private fun isWorkerProfileComplete(profile: Map<String, Any>): Boolean {
        return profile["fullName"]?.toString()?.isNotBlank() == true &&
               profile["email"]?.toString()?.isNotBlank() == true &&
               profile["phone"]?.toString()?.isNotBlank() == true &&
               profile["address"]?.toString()?.isNotBlank() == true &&
               profile["dateOfBirth"]?.toString()?.isNotBlank() == true &&
               profile["gender"]?.toString()?.isNotBlank() == true &&
               profile["skills"]?.toString()?.isNotBlank() == true &&
               profile["experience"]?.toString()?.isNotBlank() == true
    }
    
    /**
     * Check if employer profile is complete based on essential fields
     */
    private fun isEmployerProfileComplete(profile: Map<String, Any>): Boolean {
        return profile["companyName"]?.toString()?.isNotBlank() == true &&
               profile["contactEmail"]?.toString()?.isNotBlank() == true &&
               profile["contactPhone"]?.toString()?.isNotBlank() == true &&
               profile["businessAddress"]?.toString()?.isNotBlank() == true &&
               profile["industry"]?.toString()?.isNotBlank() == true &&
               profile["companySize"]?.toString()?.isNotBlank() == true
    }
    
    /**
     * High-level approach: Check if user has existing profile using multiple strategies
     * This method tries UID first (most reliable), then falls back to email query
     */
    suspend fun checkExistingProfileHighLevel(email: String, role: UserRole): Boolean {
        println("🔍 ProfileCompletionService.checkExistingProfileHighLevel:")
        println("  email: $email")
        println("  role: $role")
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("❌ No current user found")
            return false
        }
        
        println("🔍 Current user UID: ${currentUser.uid}")
        println("🔍 Current user email: ${currentUser.email}")
        
        return try {
            when (role) {
                UserRole.WORKER -> {
                    // Strategy 1: Check by UID (most reliable)
                    println("🔍 Strategy 1: Checking worker profile by UID...")
                    val workerProfileByUid = firestoreService.getWorkerProfile(currentUser.uid).getOrNull()
                    if (workerProfileByUid != null && isWorkerProfileComplete(workerProfileByUid)) {
                        println("✅ Found complete worker profile by UID")
                        return true
                    }
                    
                    // Strategy 2: Check by email query (if UID method fails)
                    println("🔍 Strategy 2: Checking worker profile by email query...")
                    val workerProfilesByEmail = firestoreService.getWorkerProfilesByEmail(email)
                    if (workerProfilesByEmail.isSuccess) {
                        val profiles = workerProfilesByEmail.getOrNull()
                        val hasCompleteProfile = profiles?.any { profile -> isWorkerProfileComplete(profile) } == true
                        println("🔍 Email query result: $hasCompleteProfile")
                        return hasCompleteProfile
                    } else {
                        println("❌ Email query failed, but UID check already completed")
                        false
                    }
                }
                UserRole.EMPLOYER -> {
                    // Strategy 1: Check by UID (most reliable)
                    println("🔍 Strategy 1: Checking employer profile by UID...")
                    val employerProfileByUid = firestoreService.getEmployerProfile(currentUser.uid).getOrNull()
                    if (employerProfileByUid != null && isEmployerProfileComplete(employerProfileByUid)) {
                        println("✅ Found complete employer profile by UID")
                        return true
                    }
                    
                    // Strategy 2: Check by email query (if UID method fails)
                    println("🔍 Strategy 2: Checking employer profile by email query...")
                    val employerProfilesByEmail = firestoreService.getEmployerProfilesByEmail(email)
                    if (employerProfilesByEmail.isSuccess) {
                        val profiles = employerProfilesByEmail.getOrNull()
                        val hasCompleteProfile = profiles?.any { profile -> isEmployerProfileComplete(profile) } == true
                        println("🔍 Email query result: $hasCompleteProfile")
                        return hasCompleteProfile
                    } else {
                        println("❌ Email query failed, but UID check already completed")
                        false
                    }
                }
                UserRole.ADMIN -> {
                    println("🔍 Admin user - profile considered complete")
                    true
                }
                else -> {
                    println("❌ Unknown role: $role")
                    false
                }
            }
        } catch (e: Exception) {
            println("❌ Error in high-level profile check: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Check if user already has a profile in Firebase by UID (more reliable than email)
     * This is useful for returning users who sign in with the same email
     */
    suspend fun checkExistingProfileByEmail(email: String, role: UserRole): Boolean {
        println("🔍 ProfileCompletionService.checkExistingProfileByEmail:")
        println("  email: $email")
        println("  role: $role")
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("❌ No current user found")
            return false
        }
        
        return try {
            when (role) {
                UserRole.WORKER -> {
                    // First try to get profile by UID (more reliable)
                    val workerProfile = firestoreService.getWorkerProfile(currentUser.uid).getOrNull()
                    println("🔍 Worker profile by UID result: ${workerProfile != null}")
                    
                    if (workerProfile != null) {
                        val isComplete = isWorkerProfileComplete(workerProfile)
                        println("🔍 Worker profile complete check: $isComplete")
                        if (isComplete) {
                            println("✅ Found complete worker profile by UID")
                            return true
                        }
                    }
                    
                    // Fallback: try querying by email if UID method fails
                    println("🔍 Fallback: Trying to query worker profiles by email...")
                    val workerProfiles = firestoreService.getWorkerProfilesByEmail(email)
                    println("🔍 Worker profiles query result: ${workerProfiles.isSuccess}")
                    
                    if (workerProfiles.isSuccess) {
                        val profiles = workerProfiles.getOrNull()
                        println("🔍 Found ${profiles?.size ?: 0} worker profiles by email")
                        
                        // Check if any profile is complete
                        val hasCompleteProfile = profiles?.any { profile -> 
                            val isComplete = isWorkerProfileComplete(profile)
                            println("🔍 Profile complete check: $isComplete")
                            isComplete
                        } == true
                        
                        println("🔍 Has complete worker profile: $hasCompleteProfile")
                        hasCompleteProfile
                    } else {
                        println("❌ Worker profiles query failed, but UID check already completed")
                        false
                    }
                }
                UserRole.EMPLOYER -> {
                    // First try to get profile by UID (more reliable)
                    val employerProfile = firestoreService.getEmployerProfile(currentUser.uid).getOrNull()
                    println("🔍 Employer profile by UID result: ${employerProfile != null}")
                    
                    if (employerProfile != null) {
                        val isComplete = isEmployerProfileComplete(employerProfile)
                        println("🔍 Employer profile complete check: $isComplete")
                        if (isComplete) {
                            println("✅ Found complete employer profile by UID")
                            return true
                        }
                    }
                    
                    // Fallback: try querying by email if UID method fails
                    println("🔍 Fallback: Trying to query employer profiles by email...")
                    val employerProfiles = firestoreService.getEmployerProfilesByEmail(email)
                    println("🔍 Employer profiles query result: ${employerProfiles.isSuccess}")
                    
                    if (employerProfiles.isSuccess) {
                        val profiles = employerProfiles.getOrNull()
                        println("🔍 Found ${profiles?.size ?: 0} employer profiles by email")
                        
                        // Check if any profile is complete
                        val hasCompleteProfile = profiles?.any { profile -> 
                            val isComplete = isEmployerProfileComplete(profile)
                            println("🔍 Profile complete check: $isComplete")
                            isComplete
                        } == true
                        
                        println("🔍 Has complete employer profile: $hasCompleteProfile")
                        hasCompleteProfile
                    } else {
                        println("❌ Employer profiles query failed, but UID check already completed")
                        false
                    }
                }
                UserRole.ADMIN -> {
                    // Admin users don't need profile setup
                    println("🔍 Admin user - skipping profile check")
                    true
                }
                else -> {
                    println("❌ Unknown role: $role")
                    false
                }
            }
        } catch (e: Exception) {
            println("❌ Error checking existing profile: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Load existing profile data into local state for returning users
     */
    suspend fun loadExistingProfileData(email: String, role: UserRole) {
        println("🔍 ProfileCompletionService.loadExistingProfileData:")
        println("  email: $email")
        println("  role: $role")
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            println("❌ No current user found")
            return
        }
        
        try {
            when (role) {
                UserRole.WORKER -> {
                    // First try to get profile by UID (more reliable)
                    val workerProfile = firestoreService.getWorkerProfile(currentUser.uid).getOrNull()
                    println("🔍 Worker profile by UID result: ${workerProfile != null}")
                    
                    if (workerProfile != null && isWorkerProfileComplete(workerProfile)) {
                        profileSetupStateManager.markProfileComplete(role)
                        profileSetupStateManager.markProfileSetupAsShown(role)
                        profileSetupStateManager.saveUserInfo(
                            workerProfile["email"]?.toString() ?: email,
                            workerProfile["fullName"]?.toString() ?: "",
                            role
                        )
                        println("✅ Loaded existing worker profile data by UID")
                        return
                    }
                    
                    // Fallback: try querying by email
                    println("🔍 Fallback: Trying to load worker profile by email...")
                    val workerProfiles = firestoreService.getWorkerProfilesByEmail(email)
                    workerProfiles.getOrNull()?.firstOrNull()?.let { profile ->
                        if (isWorkerProfileComplete(profile)) {
                            profileSetupStateManager.markProfileComplete(role)
                            profileSetupStateManager.markProfileSetupAsShown(role)
                            profileSetupStateManager.saveUserInfo(
                                profile["email"]?.toString() ?: email,
                                profile["fullName"]?.toString() ?: "",
                                role
                            )
                            println("✅ Loaded existing worker profile data by email")
                        }
                    }
                }
                UserRole.EMPLOYER -> {
                    // First try to get profile by UID (more reliable)
                    val employerProfile = firestoreService.getEmployerProfile(currentUser.uid).getOrNull()
                    println("🔍 Employer profile by UID result: ${employerProfile != null}")
                    
                    if (employerProfile != null && isEmployerProfileComplete(employerProfile)) {
                        profileSetupStateManager.markProfileComplete(role)
                        profileSetupStateManager.markProfileSetupAsShown(role)
                        profileSetupStateManager.saveUserInfo(
                            employerProfile["contactEmail"]?.toString() ?: email,
                            employerProfile["companyName"]?.toString() ?: "",
                            role
                        )
                        println("✅ Loaded existing employer profile data by UID")
                        return
                    }
                    
                    // Fallback: try querying by email
                    println("🔍 Fallback: Trying to load employer profile by email...")
                    val employerProfiles = firestoreService.getEmployerProfilesByEmail(email)
                    employerProfiles.getOrNull()?.firstOrNull()?.let { profile ->
                        if (isEmployerProfileComplete(profile)) {
                            profileSetupStateManager.markProfileComplete(role)
                            profileSetupStateManager.markProfileSetupAsShown(role)
                            profileSetupStateManager.saveUserInfo(
                                profile["contactEmail"]?.toString() ?: email,
                                profile["companyName"]?.toString() ?: "",
                                role
                            )
                            println("✅ Loaded existing employer profile data by email")
                        }
                    }
                }
                UserRole.ADMIN -> {
                    // Admin role - no specific profile data to load
                    profileSetupStateManager.markProfileComplete(role)
                    profileSetupStateManager.markProfileSetupAsShown(role)
                    profileSetupStateManager.saveUserInfo(
                        email,
                        "",
                        role
                    )
                    println("✅ Loaded existing admin profile data")
                }
            }
        } catch (e: Exception) {
            println("❌ Error loading existing profile data: ${e.message}")
        }
    }
    
    /**
     * Save user information from Google Sign-In
     */
    suspend fun saveUserInfo(email: String, name: String, role: UserRole) {
        profileSetupStateManager.saveUserInfo(email, name, role)
    }
    
    suspend fun updateUserRole(newRole: UserRole) {
        profileSetupStateManager.saveUserRole(newRole)
    }
    
    /**
     * Mark profile as complete and update state
     */
    suspend fun markProfileComplete(role: UserRole) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Update user profile in Firestore
            val userResult = firestoreService.getUserById(currentUser.uid)
            if (userResult.isSuccess) {
                val user = userResult.getOrNull()
                if (user != null) {
                    val updatedUser = user.copy(isProfileComplete = true)
                    firestoreService.createOrUpdateUser(updatedUser)
                }
            }
        }
        
        // Update local state
        profileSetupStateManager.markProfileComplete(role)
        profileSetupStateManager.markProfileSetupAsShown(role)
    }
    
    /**
     * Get profile setup status for UI
     */
    suspend fun getProfileSetupStatus(role: UserRole): com.example.partimes.state.ProfileSetupStatus {
        return profileSetupStateManager.getProfileSetupStatus(role)
    }
    
    /**
     * Reset profile setup state (for logout)
     */
    suspend fun resetProfileSetupState() {
        profileSetupStateManager.resetProfileSetupState()
    }
    
    /**
     * Get profile completion percentage
     */
    suspend fun getProfileCompletionPercentage(userId: String, role: UserRole): Result<Int> {
        return try {
            when (role) {
                UserRole.WORKER -> getWorkerProfileCompletion(userId)
                UserRole.EMPLOYER -> getEmployerProfileCompletion(userId)
                else -> Result.success(0)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get missing profile fields
     */
    suspend fun getMissingProfileFields(userId: String, role: UserRole): Result<List<String>> {
        return try {
            when (role) {
                UserRole.WORKER -> getMissingWorkerFields(userId)
                UserRole.EMPLOYER -> getMissingEmployerFields(userId)
                else -> Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user can apply for jobs directly (complete profile)
     */
    suspend fun canApplyDirectly(userId: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.success(false)
            }
            
            val userResult = firestoreService.getUserById(userId)
            if (userResult.isFailure) {
                return Result.success(false)
            }
            
            val user = userResult.getOrNull()
            if (user == null) {
                return Result.success(false)
            }
            
            // Check if profile is complete for worker role
            if (user.role == UserRole.WORKER) {
                checkWorkerProfileComplete(userId)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun checkWorkerProfileComplete(userId: String): Result<Boolean> {
        return try {
            val profileResult = firestoreService.getWorkerProfile(userId)
            profileResult.fold(
                onSuccess = { profile ->
                    if (profile == null) {
                        Result.success(false)
                    } else {
                        // Check essential fields
                        val hasEssentialFields = profile["fullName"]?.toString()?.isNotBlank() == true &&
                                               profile["email"]?.toString()?.isNotBlank() == true &&
                                               profile["phone"]?.toString()?.isNotBlank() == true
                        Result.success(hasEssentialFields)
                    }
                },
                onFailure = { 
                    Result.success(false)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun checkEmployerProfileComplete(userId: String): Result<Boolean> {
        return try {
            val profileResult = firestoreService.getEmployerProfile(userId)
            if (profileResult.isFailure) {
                return Result.success(false)
            }
            
            val profile = profileResult.getOrNull()
            if (profile == null) {
                return Result.success(false)
            }
            
            // Check essential fields
            val hasEssentialFields = profile["companyName"]?.toString()?.isNotBlank() == true &&
                                   profile["contactEmail"]?.toString()?.isNotBlank() == true &&
                                   profile["contactPhone"]?.toString()?.isNotBlank() == true
            
            Result.success(hasEssentialFields)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getWorkerProfileCompletion(userId: String): Result<Int> {
        return try {
            val profileResult = firestoreService.getWorkerProfile(userId)
            profileResult.fold(
                onSuccess = { profile ->
                    if (profile == null) {
                        Result.success(0)
                    } else {
                        var completedFields = 0
                        val totalFields = 8
                        
                        // Check each field
                        if (profile["fullName"]?.toString()?.isNotBlank() == true) completedFields++
                        if (profile["email"]?.toString()?.isNotBlank() == true) completedFields++
                        if (profile["phone"]?.toString()?.isNotBlank() == true) completedFields++
                        if (profile["address"]?.toString()?.isNotBlank() == true) completedFields++
                        if (profile["dateOfBirth"]?.toString()?.isNotBlank() == true) completedFields++
                        if (profile["gender"]?.toString()?.isNotBlank() == true) completedFields++
                        if (profile["skills"] != null) completedFields++
                        if (profile["experience"] != null) completedFields++
                        
                        val percentage = (completedFields * 100) / totalFields
                        Result.success(percentage)
                    }
                },
                onFailure = { 
                    Result.success(0)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getEmployerProfileCompletion(userId: String): Result<Int> {
        return try {
            val profileResult = firestoreService.getEmployerProfile(userId)
            if (profileResult.isFailure) {
                return Result.success(0)
            }
            
            val profile = profileResult.getOrNull()
            if (profile == null) {
                return Result.success(0)
            }
            
            var completedFields = 0
            val totalFields = 6
            
            // Check each field
            if (profile["companyName"]?.toString()?.isNotBlank() == true) completedFields++
            if (profile["contactEmail"]?.toString()?.isNotBlank() == true) completedFields++
            if (profile["contactPhone"]?.toString()?.isNotBlank() == true) completedFields++
            if (profile["businessAddress"]?.toString()?.isNotBlank() == true) completedFields++
            if (profile["industry"]?.toString()?.isNotBlank() == true) completedFields++
            if (profile["companySize"]?.toString()?.isNotBlank() == true) completedFields++
            
            val percentage = (completedFields * 100) / totalFields
            Result.success(percentage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getMissingWorkerFields(userId: String): Result<List<String>> {
        return try {
            val profileResult = firestoreService.getWorkerProfile(userId)
            profileResult.fold(
                onSuccess = { profile ->
                    if (profile == null) {
                        Result.success(listOf("Profile not found"))
                    } else {
                        val missingFields = mutableListOf<String>()
                        
                        if (profile["fullName"]?.toString()?.isBlank() != false) missingFields.add("Full Name")
                        if (profile["email"]?.toString()?.isBlank() != false) missingFields.add("Email")
                        if (profile["phone"]?.toString()?.isBlank() != false) missingFields.add("Phone Number")
                        if (profile["address"]?.toString()?.isBlank() != false) missingFields.add("Address")
                        if (profile["dateOfBirth"]?.toString()?.isBlank() != false) missingFields.add("Date of Birth")
                        if (profile["gender"]?.toString()?.isBlank() != false) missingFields.add("Gender")
                        if (profile["skills"] == null) missingFields.add("Skills")
                        if (profile["experience"] == null) missingFields.add("Work Experience")
                        
                        Result.success(missingFields)
                    }
                },
                onFailure = { 
                    Result.success(listOf("Profile not found"))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getMissingEmployerFields(userId: String): Result<List<String>> {
        return try {
            val profileResult = firestoreService.getEmployerProfile(userId)
            if (profileResult.isFailure) {
                return Result.success(listOf("Profile not found"))
            }
            
            val profile = profileResult.getOrNull()
            if (profile == null) {
                return Result.success(listOf("Profile not found"))
            }
            
            val missingFields = mutableListOf<String>()
            
            if (profile["companyName"]?.toString()?.isBlank() != false) missingFields.add("Company Name")
            if (profile["contactEmail"]?.toString()?.isBlank() != false) missingFields.add("Contact Email")
            if (profile["contactPhone"]?.toString()?.isBlank() != false) missingFields.add("Contact Phone")
            if (profile["businessAddress"]?.toString()?.isBlank() != false) missingFields.add("Business Address")
            if (profile["industry"]?.toString()?.isBlank() != false) missingFields.add("Industry")
            if (profile["companySize"]?.toString()?.isBlank() != false) missingFields.add("Company Size")
            
            Result.success(missingFields)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get worker profile data
     */
    suspend fun getWorkerProfile(userId: String): Result<Map<String, Any>> {
        return try {
            val result = firestoreService.getWorkerProfile(userId)
            result.fold(
                onSuccess = { profile ->
                    if (profile == null) {
                        Result.failure(Exception("Worker profile not found"))
                    } else {
                        Result.success(profile)
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user profile data (generic method for both workers and employers)
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            // Try to get worker profile first
            val workerResult = firestoreService.getWorkerProfile(userId)
            if (workerResult.isSuccess && workerResult.getOrNull() != null) {
                return Result.success(workerResult.getOrNull()!!)
            }
            
            // If not worker, try employer profile
            val employerResult = firestoreService.getEmployerProfile(userId)
            if (employerResult.isSuccess && employerResult.getOrNull() != null) {
                return Result.success(employerResult.getOrNull()!!)
            }
            
            Result.failure(Exception("User profile not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save worker profile data to Firestore
     */
    suspend fun saveWorkerProfileData(userId: String, profileData: Map<String, Any>) {
        firestoreService.createOrUpdateWorkerProfile(userId, profileData)
    }

    /**
     * Save employer profile data to Firestore
     */
    suspend fun saveEmployerProfileData(userId: String, profileData: Map<String, Any>) {
        firestoreService.createOrUpdateEmployerProfile(userId, profileData)
    }

    /**
     * Get employer profile data from Firestore
     */
    suspend fun getEmployerProfileData(userId: String): Map<String, Any>? {
        return firestoreService.getEmployerProfile(userId).getOrNull()
    }

    /**
     * Get worker profile data from Firestore
     */
    suspend fun getWorkerProfileData(userId: String): Map<String, Any>? {
        return firestoreService.getWorkerProfile(userId).getOrNull()
    }
}
