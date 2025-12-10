package com.example.dutype.services

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import timber.log.Timber
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
        
        // Basic Information (20%)
        if (fullName.isNotBlank()) completion += 4
        if (email.isNotBlank()) completion += 4
        if (phoneNumber.isNotBlank()) completion += 4
        if (address.isNotBlank()) completion += 4
        if (dateOfBirth.isNotBlank()) completion += 4
        
        // Personal Details (15%)
        if (gender.isNotBlank()) completion += 15
        
        // Skills & Experience (30%)
        if (skills.isNotBlank()) completion += 15
        if (experience.isNotBlank()) completion += 15
        
        // Profile Picture (35%)
        if (profileImageUrl != null && profileImageUrl.isNotBlank()) completion += 35
        
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
            
            Timber.d("🔍 ProfileCompletionService.calculateWorkerProfileCompletion for userId: $userId")
            Timber.d("🔍 Firebase userData keys: ${userData.keys}")
            Timber.d("🔍 Worker profile data exists: ${workerProfileData != null}")
            if (workerProfileData != null) {
                Timber.d("🔍 Worker profile keys: ${workerProfileData.keys}")
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
            
            Timber.d("🔍 Merged data keys: ${mergedData.keys}")
            Timber.d("🔍 Phone field (phone): ${mergedData["phone"]}")
            Timber.d("🔍 Phone field (phoneNumber): ${mergedData["phoneNumber"]}")
            Timber.d("🔍 Address: ${mergedData["address"]}")
            Timber.d("🔍 Skills: ${mergedData["skills"]}")
            Timber.d("🔍 Experience: ${mergedData["experience"]}")
            
            var completion = 0
            
            // Basic Information (20%)
            if (mergedData["fullName"] != null && mergedData["fullName"].toString().isNotBlank()) completion += 4
            if (mergedData["email"] != null && mergedData["email"].toString().isNotBlank()) completion += 4
            // Check both "phone" and "phoneNumber" fields for compatibility
            val phoneValue = mergedData["phone"] ?: mergedData["phoneNumber"]
            if (phoneValue != null && phoneValue.toString().isNotBlank()) completion += 4
            if (mergedData["address"] != null && mergedData["address"].toString().isNotBlank()) completion += 4
            if (mergedData["dateOfBirth"] != null && mergedData["dateOfBirth"].toString().isNotBlank()) completion += 4
            
            // Personal Details (15%)
            if (mergedData["gender"] != null && mergedData["gender"].toString().isNotBlank()) completion += 15
            
            // Skills & Experience (30%)
            if (mergedData["skills"] != null && mergedData["skills"].toString().isNotBlank()) completion += 15
            if (mergedData["experience"] != null && mergedData["experience"].toString().isNotBlank()) completion += 15
            
            // Profile Picture (35%)
            if (mergedData["profileImageUrl"] != null && mergedData["profileImageUrl"].toString().isNotBlank()) completion += 35
            
            val finalCompletion = completion.coerceAtMost(100)
            Timber.d("🔍 ProfileCompletionService - Final completion percentage: $finalCompletion%")
            finalCompletion
            } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService - Error calculating completion: ${e.message}")
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
        
        // Company Information (25%)
        if (companyName.isNotBlank()) completion += 8
        if (industry.isNotBlank()) completion += 8
        if (companySize.isNotBlank()) completion += 9
        
        // Contact Details (20%)
        if (contactEmail.isNotBlank()) completion += 10
        if (contactPhone.isNotBlank()) completion += 10
        
        // Business Details (20%)
        if (businessAddress.isNotBlank()) completion += 10
        if (description.isNotBlank()) completion += 10
        
        // Website & Profile Picture (35%)
        if (website.isNotBlank()) completion += 15
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
            
            // Company Information (25%)
            if (userData["companyName"] != null && userData["companyName"].toString().isNotBlank()) completion += 8
            if (userData["industry"] != null && userData["industry"].toString().isNotBlank()) completion += 8
            if (userData["companySize"] != null && userData["companySize"].toString().isNotBlank()) completion += 9
            
            // Contact Details (20%)
            if (userData["contactEmail"] != null && userData["contactEmail"].toString().isNotBlank()) completion += 10
            if (userData["contactPhone"] != null && userData["contactPhone"].toString().isNotBlank()) completion += 10
            
            // Business Details (20%)
            if (userData["businessAddress"] != null && userData["businessAddress"].toString().isNotBlank()) completion += 10
            if (userData["description"] != null && userData["description"].toString().isNotBlank()) completion += 10
            
            // Website & Profile Picture (35%)
            if (userData["website"] != null && userData["website"].toString().isNotBlank()) completion += 15
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
            
            // Also update worker_profiles collection if it's a worker
            if (userRole == "worker") {
                try {
                    firestore.collection("worker_profiles").document(userId)
                        .update("profileImageUrl", downloadUrl.toString())
                        .await()
                } catch (e: Exception) {
                    // If worker_profiles document doesn't exist, create it
                    firestore.collection("worker_profiles").document(userId)
                        .set(mapOf("profileImageUrl" to downloadUrl.toString()), com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }
            }
            
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
     * Get user profile data - merges data from users and worker_profiles collections
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any?>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data?.toMutableMap() ?: mutableMapOf()
            
            // Also try to get data from worker_profiles collection
            val workerProfileDoc = firestore.collection("worker_profiles").document(userId).get().await()
            val workerProfileData = workerProfileDoc.data
            
            // Merge data - worker profile data fills in missing fields
            workerProfileData?.let { profileData ->
                profileData.forEach { (key, value) ->
                    if (!userData.containsKey(key) || userData[key] == null || userData[key].toString().isBlank()) {
                        userData[key] = value
                    }
                }
            }
            
            if (userData.isEmpty()) {
                return Result.failure(Exception("User not found"))
            }
            
            Timber.d("🔍 ProfileCompletionService.getUserProfile - Merged profile data for $userId: ${userData.keys}")
            Result.success(userData)
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService.getUserProfile - Error: ${e.message}")
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
            
            Timber.d("🔍 ProfileCompletionService.saveWorkerProfileData - Saved profile data: ${profileData.keys}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService.saveWorkerProfileData - Error: ${e.message}")
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
            
            Timber.d("🔍 ProfileCompletionService.getWorkerProfileData - Merged keys: ${mergedData.keys}")
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
            
            if (query.isEmpty) {
                return Result.success(false)
            }
            
            // Check if user has completed profile (not just if document exists)
            val userDoc = query.documents.first()
            val isProfileComplete = userDoc.getBoolean("isProfileComplete") ?: false
            Timber.d("🔍 checkExistingProfileByEmail: User exists with email: $email, isProfileComplete: $isProfileComplete")
            
            Result.success(isProfileComplete)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check existing profile by current authenticated user's UID
     * Returns true only if the user has a COMPLETED profile (isProfileComplete=true)
     * Returns false for new users (document exists but isProfileComplete=false)
     */
    suspend fun checkExistingProfileByCurrentUser(): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Timber.d("🔍 checkExistingProfileByCurrentUser: User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }
            
            Timber.d("🔍 checkExistingProfileByCurrentUser: Checking user document for UID: ${currentUser.uid}")
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            
            if (!userDoc.exists()) {
                Timber.d("🔍 checkExistingProfileByCurrentUser: User document does not exist - new user")
                return Result.success(false)
            }
            
            // Check if profile is actually complete, not just if document exists
            val isProfileComplete = userDoc.getBoolean("isProfileComplete") ?: false
            val userData = userDoc.data
            Timber.d("🔍 checkExistingProfileByCurrentUser: User found in database with email: ${userData?.get("email")}, isProfileComplete: $isProfileComplete")
            
            Result.success(isProfileComplete)
        } catch (e: Exception) {
            Timber.d("🔍 checkExistingProfileByCurrentUser: Error: ${e.message}")
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
        
        if (completionPercentage >= 100) {
            return missingFields  // Profile is complete
        }
        
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return emptyList()
            
            if (userRole == "WORKER") {
                // Check for missing fields specifically
                if (userData["fullName"] == null || userData["fullName"].toString().isBlank()) 
                    missingFields.add("Full Name")
                if (userData["email"] == null || userData["email"].toString().isBlank()) 
                    missingFields.add("Email")
                val phoneValue = userData["phone"] ?: userData["phoneNumber"]
                if (phoneValue == null || phoneValue.toString().isBlank()) 
                    missingFields.add("Phone Number")
                if (userData["address"] == null || userData["address"].toString().isBlank()) 
                    missingFields.add("Address")
                if (userData["dateOfBirth"] == null || userData["dateOfBirth"].toString().isBlank()) 
                    missingFields.add("Date of Birth")
                if (userData["gender"] == null || userData["gender"].toString().isBlank()) 
                    missingFields.add("Gender")
                if (userData["skills"] == null || userData["skills"].toString().isBlank()) 
                    missingFields.add("Skills")
                if (userData["experience"] == null || userData["experience"].toString().isBlank()) 
                    missingFields.add("Experience")
                if (userData["profileImageUrl"] == null || userData["profileImageUrl"].toString().isBlank()) 
                    missingFields.add("Profile Picture")
            } else {
                // Check for missing employer fields
                if (userData["companyName"] == null || userData["companyName"].toString().isBlank()) 
                    missingFields.add("Company Name")
                if (userData["industry"] == null || userData["industry"].toString().isBlank()) 
                    missingFields.add("Industry")
                if (userData["companySize"] == null || userData["companySize"].toString().isBlank()) 
                    missingFields.add("Company Size")
                if (userData["contactEmail"] == null || userData["contactEmail"].toString().isBlank()) 
                    missingFields.add("Contact Email")
                if (userData["contactPhone"] == null || userData["contactPhone"].toString().isBlank()) 
                    missingFields.add("Contact Phone")
                if (userData["businessAddress"] == null || userData["businessAddress"].toString().isBlank()) 
                    missingFields.add("Business Address")
                if (userData["description"] == null || userData["description"].toString().isBlank()) 
                    missingFields.add("Business Description")
                if (userData["website"] == null || userData["website"].toString().isBlank()) 
                    missingFields.add("Website")
                if (userData["profileImageUrl"] == null || userData["profileImageUrl"].toString().isBlank()) 
                    missingFields.add("Profile Picture")
            }
            
            missingFields
        } catch (e: Exception) {
            emptyList()
        }
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
