package com.example.dutype.services

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profile Completion Service
 * Enhanced with 30+ years of Android development experience
 * Handles profile completion calculations and image uploads
 */
@Singleton
class ProfileCompletionService @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Calculate profile completion percentage for workers from individual fields
     */
    fun calculateWorkerProfileCompletion(
        fullName: String,
        email: String,
        phoneNumber: String,
        address: String,
        dateOfBirth: String,
        gender: String,
        skills: String,
        experience: String,
        profileImageUrl: String?
    ): Int {
        var completion = 0
        
        // Basic Information (25%)
        if (fullName.isNotBlank()) completion += 5
        if (email.isNotBlank()) completion += 5
        if (phoneNumber.isNotBlank()) completion += 5
        if (address.isNotBlank()) completion += 5
        if (dateOfBirth.isNotBlank()) completion += 5
        
        // Contact Details (20%)
        if (phoneNumber.isNotBlank()) completion += 10
        if (address.isNotBlank()) completion += 10
        
        // Personal Details (20%)
        if (dateOfBirth.isNotBlank()) completion += 10
        if (gender.isNotBlank()) completion += 10
        
        // Skills & Experience (20%)
        if (skills.isNotBlank()) completion += 10
        if (experience.isNotBlank()) completion += 10
        
        // Profile Picture (15%)
        if (profileImageUrl != null && profileImageUrl.isNotBlank()) completion += 15
        
        return completion.coerceAtMost(100)
    }
    
    /**
     * Calculate profile completion percentage for workers from Firestore
     */
    suspend fun calculateWorkerProfileCompletion(userId: String): Int {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return 0
            
            // Also try to get data from worker_profiles collection
            val workerProfileDoc = firestore.collection("worker_profiles").document(userId).get().await()
            val workerProfileData = workerProfileDoc.data
            
            println("🔍 ProfileCompletionService.calculateWorkerProfileCompletion for userId: $userId")
            println("🔍 Firebase userData keys: ${userData.keys}")
            println("🔍 Worker profile data exists: ${workerProfileData != null}")
            if (workerProfileData != null) {
                println("🔍 Worker profile keys: ${workerProfileData.keys}")
            }
            
            // Merge data - user data takes precedence, fallback to worker profile data
            val mergedData = userData.toMutableMap()
            workerProfileData?.let { profileData ->
                profileData.forEach { (key, value) ->
                    if (!mergedData.containsKey(key) || mergedData[key] == null) {
                        mergedData[key] = value
                    }
                }
            }
            
            println("🔍 Merged data keys: ${mergedData.keys}")
            println("🔍 Phone field (phone): ${mergedData["phone"]}")
            println("🔍 Phone field (phoneNumber): ${mergedData["phoneNumber"]}")
            println("🔍 Address: ${mergedData["address"]}")
            println("🔍 Skills: ${mergedData["skills"]}")
            println("🔍 Experience: ${mergedData["experience"]}")
            
            var completion = 0
            
            // Basic Information (25%) - Use merged data
            if (mergedData["fullName"] != null && mergedData["fullName"].toString().isNotBlank()) completion += 5
            if (mergedData["email"] != null && mergedData["email"].toString().isNotBlank()) completion += 5
            // Check both "phone" and "phoneNumber" fields for compatibility
            val phoneValue = mergedData["phone"] ?: mergedData["phoneNumber"]
            if (phoneValue != null && phoneValue.toString().isNotBlank()) completion += 5
            if (mergedData["address"] != null && mergedData["address"].toString().isNotBlank()) completion += 5
            if (mergedData["dateOfBirth"] != null && mergedData["dateOfBirth"].toString().isNotBlank()) completion += 5
            
            // Contact Details (20%)
            if (phoneValue != null && phoneValue.toString().isNotBlank()) completion += 10
            if (mergedData["address"] != null && mergedData["address"].toString().isNotBlank()) completion += 10
            
            // Personal Details (20%)
            if (mergedData["dateOfBirth"] != null && mergedData["dateOfBirth"].toString().isNotBlank()) completion += 10
            if (mergedData["gender"] != null && mergedData["gender"].toString().isNotBlank()) completion += 10
            
            // Skills & Experience (20%)
            if (mergedData["skills"] != null && mergedData["skills"].toString().isNotBlank()) completion += 10
            if (mergedData["experience"] != null && mergedData["experience"].toString().isNotBlank()) completion += 10
            
            // Profile Picture (15%)
            if (mergedData["profileImageUrl"] != null && mergedData["profileImageUrl"].toString().isNotBlank()) completion += 15
            
            val finalCompletion = completion.coerceAtMost(100)
            println("🔍 ProfileCompletionService - Final completion percentage: $finalCompletion%")
            finalCompletion
        } catch (e: Exception) {
            println("❌ ProfileCompletionService - Error calculating completion: ${e.message}")
            0
        }
    }

    /**
     * Calculate profile completion percentage for employers from individual fields
     */
    fun calculateEmployerProfileCompletion(
        companyName: String,
        contactEmail: String,
        contactPhone: String,
        businessAddress: String,
        industry: String,
        companySize: String,
        website: String,
        description: String,
        profileImageUrl: String?
    ): Int {
        var completion = 0
        
        // Company Information (30%)
        if (companyName.isNotBlank()) completion += 10
        if (industry.isNotBlank()) completion += 10
        if (companySize.isNotBlank()) completion += 10
        
        // Contact Details (25%)
        if (contactEmail.isNotBlank()) completion += 10
        if (contactPhone.isNotBlank()) completion += 10
        if (website.isNotBlank()) completion += 5
        
        // Business Details (25%)
        if (businessAddress.isNotBlank()) completion += 15
        if (description.isNotBlank()) completion += 10
        
        // Profile Picture (20%)
        if (profileImageUrl != null && profileImageUrl.isNotBlank()) completion += 20
        
        return completion.coerceAtMost(100)
    }
    
    /**
     * Calculate profile completion percentage for employers from Firestore
     */
    suspend fun calculateEmployerProfileCompletion(userId: String): Int {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return 0
            
            var completion = 0
            
            // Company Information (30%)
            if (userData["companyName"] != null && userData["companyName"].toString().isNotBlank()) completion += 10
            if (userData["industry"] != null && userData["industry"].toString().isNotBlank()) completion += 10
            if (userData["companySize"] != null && userData["companySize"].toString().isNotBlank()) completion += 10
            
            // Contact Details (25%)
            if (userData["contactEmail"] != null && userData["contactEmail"].toString().isNotBlank()) completion += 10
            if (userData["contactPhone"] != null && userData["contactPhone"].toString().isNotBlank()) completion += 10
            if (userData["website"] != null && userData["website"].toString().isNotBlank()) completion += 5
            
            // Business Details (25%)
            if (userData["businessAddress"] != null && userData["businessAddress"].toString().isNotBlank()) completion += 15
            if (userData["description"] != null && userData["description"].toString().isNotBlank()) completion += 10
            
            // Profile Picture (20%)
            if (userData["profileImageUrl"] != null && userData["profileImageUrl"].toString().isNotBlank()) completion += 20
            
            completion.coerceAtMost(100)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Upload profile image to Firebase Storage
     */
    suspend fun uploadProfileImage(imageUri: Uri, userId: String, userRole: String): Result<String> {
        return try {
            val fileName = "${userRole}_${userId}_${System.currentTimeMillis()}.jpg"
            val storageRef = storage.reference.child("profile_images/$fileName")
            
            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            
            // Update user document with image URL
            firestore.collection("users").document(userId)
                .update("profileImageUrl", downloadUrl.toString())
                .await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get profile completion status
     */
    suspend fun getProfileCompletionStatus(userId: String, userRole: String): ProfileCompletionStatus {
        val completionPercentage = if (userRole == "WORKER") {
            calculateWorkerProfileCompletion(userId)
        } else {
            calculateEmployerProfileCompletion(userId)
        }
        
        return ProfileCompletionStatus(
            completionPercentage = completionPercentage,
            isCompleted = completionPercentage >= 100,
            missingFields = getMissingFields(userId, userRole, completionPercentage)
        )
    }
    
    /**
     * Check if user can apply directly (profile completion >= 80%)
     */
    suspend fun canApplyDirectly(userId: String): Result<Boolean> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return Result.failure(Exception("User not found"))
            val userRole = userData["role"] as? String ?: return Result.failure(Exception("User role not found"))
            
            val completion = if (userRole == "WORKER") {
                calculateWorkerProfileCompletion(userId)
                    } else {
                calculateEmployerProfileCompletion(userId)
            }
            
            Result.success(completion >= 80)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user profile data
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any?>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return Result.failure(Exception("User not found"))
            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user has complete profile for their role
     */
    suspend fun isProfileComplete(userId: String, role: String): Result<Boolean> {
        return try {
            val completion = if (role == "WORKER") {
                calculateWorkerProfileCompletion(userId)
                    } else {
                calculateEmployerProfileCompletion(userId)
            }
            Result.success(completion >= 100)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Save user info for profile setup
     */
    suspend fun saveUserInfo(email: String, name: String, role: String): Result<Unit> {
        return try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val userData = mapOf(
                "email" to email,
                "name" to name,
                "role" to role,
                "createdAt" to System.currentTimeMillis()
            )
            
            firestore.collection("users").document(currentUser.uid)
                .set(userData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user role
     */
    suspend fun updateUserRole(newRole: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            firestore.collection("users").document(currentUser.uid)
                .update("role", newRole)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if profile setup should be shown
     */
    suspend fun shouldRedirectToProfileSetup(role: String): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val completion = if (role == "WORKER") {
                calculateWorkerProfileCompletion(currentUser.uid)
            } else {
                calculateEmployerProfileCompletion(currentUser.uid)
            }
            
            Result.success(completion < 100)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Save worker profile data
     */
    suspend fun saveWorkerProfileData(profileData: Map<String, Any>): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            firestore.collection("users").document(currentUser.uid)
                .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            println("🔍 ProfileCompletionService.saveWorkerProfileData - Saved profile data: ${profileData.keys}")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ ProfileCompletionService.saveWorkerProfileData - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Save employer profile data
     */
    suspend fun saveEmployerProfileData(profileData: Map<String, Any>): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            firestore.collection("users").document(currentUser.uid)
                .update(profileData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get employer profile data
     */
    suspend fun getEmployerProfileData(userId: String): Result<Map<String, Any?>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return Result.failure(Exception("User not found"))
            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get worker profile data
     */
    suspend fun getWorkerProfileData(userId: String): Result<Map<String, Any?>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return Result.failure(Exception("User not found"))
            
            // Also try to get data from worker_profiles collection
            val workerProfileDoc = firestore.collection("worker_profiles").document(userId).get().await()
            val workerProfileData = workerProfileDoc.data
            
            // Merge data - user data takes precedence, fallback to worker profile data
            val mergedData = userData.toMutableMap()
            workerProfileData?.let { profileData ->
                profileData.forEach { (key, value) ->
                    if (!mergedData.containsKey(key) || mergedData[key] == null) {
                        mergedData[key] = value
                    }
                }
            }
            
            println("🔍 ProfileCompletionService.getWorkerProfileData - Merged keys: ${mergedData.keys}")
            Result.success(mergedData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check existing profile high level
     */
    suspend fun checkExistingProfileHighLevel(userId: String): Result<Boolean> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            Result.success(userDoc.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check existing profile by email
     */
    suspend fun checkExistingProfileByEmail(email: String): Result<Boolean> {
        return try {
            val query = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            
            Result.success(!query.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check existing profile by current authenticated user's UID
     */
    suspend fun checkExistingProfileByCurrentUser(): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                println("🔍 checkExistingProfileByCurrentUser: User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }
            
            println("🔍 checkExistingProfileByCurrentUser: Checking user document for UID: ${currentUser.uid}")
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val exists = userDoc.exists()
            println("🔍 checkExistingProfileByCurrentUser: Document exists: $exists")
            
            if (exists) {
                val userData = userDoc.data
                println("🔍 checkExistingProfileByCurrentUser: User data keys: ${userData?.keys}")
                
                // Check if this is more than just basic auth data (checking for profile completion indicators)
                val hasEssentialData = userData?.containsKey("phoneNumber") == true || 
                                     userData?.containsKey("address") == true ||
                                     userData?.containsKey("dateOfBirth") == true ||
                                     userData?.containsKey("profileCompleted") == true
                
                println("🔍 checkExistingProfileByCurrentUser: Has essential profile data: $hasEssentialData")
                
                // If user document exists, consider them as existing user
                // They should go to home screen if they have some profile data, or continue setup if not
                Result.success(exists)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            println("🔍 checkExistingProfileByCurrentUser: Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Load existing profile data by userId
     */
    suspend fun loadExistingProfileData(userId: String): Result<Map<String, Any?>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return Result.failure(Exception("User not found"))
            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load existing profile data by email
     */
    suspend fun loadExistingProfileDataByEmail(email: String): Result<Map<String, Any?>> {
        return try {
            val query = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            
            if (query.isEmpty) {
                return Result.failure(Exception("User not found"))
            }
            
            val userDoc = query.documents.first()
            val userData = userDoc.data ?: return Result.failure(Exception("User data not found"))
            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load existing profile data by current authenticated user's UID
     */
    suspend fun loadExistingProfileDataByCurrentUser(): Result<Map<String, Any?>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userData = userDoc.data ?: return Result.failure(Exception("User not found"))
            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get profile completion percentage
     */
    suspend fun getProfileCompletionPercentage(userId: String, userRole: String): Result<Int> {
        return try {
            val completion = if (userRole == "WORKER") {
                calculateWorkerProfileCompletion(userId)
            } else {
                calculateEmployerProfileCompletion(userId)
            }
            Result.success(completion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get missing profile fields
     */
    suspend fun getMissingProfileFields(userId: String, userRole: String): Result<List<String>> {
        return try {
            val completion = if (userRole == "WORKER") {
                calculateWorkerProfileCompletion(userId)
            } else {
                calculateEmployerProfileCompletion(userId)
            }
            
            val missingFields = getMissingFields(userId, userRole, completion)
            Result.success(missingFields)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get missing fields for profile completion
     */
    private suspend fun getMissingFields(userId: String, userRole: String, completionPercentage: Int): List<String> {
        val missingFields = mutableListOf<String>()
        
        if (completionPercentage < 100) {
            if (userRole == "WORKER") {
                if (completionPercentage < 25) missingFields.add("Basic Information")
                if (completionPercentage < 45) missingFields.add("Contact Details")
                if (completionPercentage < 65) missingFields.add("Personal Details")
                if (completionPercentage < 85) missingFields.add("Skills & Experience")
                if (completionPercentage < 100) missingFields.add("Profile Picture")
            } else {
                if (completionPercentage < 30) missingFields.add("Company Information")
                if (completionPercentage < 55) missingFields.add("Contact Details")
                if (completionPercentage < 80) missingFields.add("Business Details")
                if (completionPercentage < 100) missingFields.add("Profile Picture")
            }
        }
        
        return missingFields
    }
}

/**
 * Data class for profile completion status
 */
data class ProfileCompletionStatus(
    val completionPercentage: Int,
    val isCompleted: Boolean,
    val missingFields: List<String>
)