package com.example.dutype.services

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.example.dutype.utils.PhoneUtils
import javax.inject.Inject
import javax.inject.Singleton

// NOTE: DeviceFingerprint object REMOVED - Use DeviceFingerprintService instead
// This eliminates duplicate device fingerprint logic across the codebase

/**
 * Profile Completion Service
 * Enhanced with 30+ years of Android development experience
 * Handles profile completion calculations and image uploads
 * 
 * REFACTORED: Now receives Firebase dependencies via constructor injection
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class ProfileCompletionService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    
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
     * Calculate profile completion percentage for workers from Firestore (users collection only)
     */
    suspend fun calculateWorkerProfileCompletion(userId: String): Int {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return 0
            
            Timber.d("🔍 ProfileCompletionService.calculateWorkerProfileCompletion for userId: $userId")
            Timber.d("🔍 Firebase userData keys: ${userData.keys}")
            Timber.d("🔍 Phone field (phone): ${userData["phone"]}")
            Timber.d("🔍 Phone field (phoneNumber): ${userData["phoneNumber"]}")
            Timber.d("🔍 Address: ${userData["address"]}")
            Timber.d("🔍 Skills: ${userData["skills"]}")
            Timber.d("🔍 Experience: ${userData["experience"]}")
            
            var completion = 0
            
            // Basic Information (20%)
            if (userData["fullName"] != null && userData["fullName"].toString().isNotBlank()) completion += 4
            if (userData["email"] != null && userData["email"].toString().isNotBlank()) completion += 4
            // Check both "phone" and "phoneNumber" fields for compatibility
            val phoneValue = userData["phone"] ?: userData["phoneNumber"]
            if (phoneValue != null && phoneValue.toString().isNotBlank()) completion += 4
            if (userData["address"] != null && userData["address"].toString().isNotBlank()) completion += 4
            if (userData["dateOfBirth"] != null && userData["dateOfBirth"].toString().isNotBlank()) completion += 4
            
            // Personal Details (15%)
            if (userData["gender"] != null && userData["gender"].toString().isNotBlank()) completion += 15
            
            // Skills & Experience (30%)
            if (userData["skills"] != null && userData["skills"].toString().isNotBlank()) completion += 15
            if (userData["experience"] != null && userData["experience"].toString().isNotBlank()) completion += 15
            
            // Profile Picture (35%)
            if (userData["profileImageUrl"] != null && userData["profileImageUrl"].toString().isNotBlank()) completion += 35
            
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
     * Images are stored under: profile_images/{userId}/profile.jpg
     * Profile image URL is stored only in the users collection
     */
    suspend fun uploadProfileImage(imageUri: Uri, userId: String, userRole: String): Result<String> {
        return try {
            Timber.d("📸 PROFILE IMAGE DEBUG: uploadProfileImage() called")
            Timber.d("📸   - userId: $userId")
            Timber.d("📸   - userRole: $userRole")
            Timber.d("📸   - imageUri: $imageUri")
            
            // Store under user's folder: profile_images/{userId}/profile_{timestamp}.jpg
            val fileName = "profile_${System.currentTimeMillis()}.jpg"
            val storagePath = "profile_images/$userId/$fileName"
            val storageRef = storage.reference.child(storagePath)
            
            Timber.d("📸 PROFILE IMAGE DEBUG: Uploading to path: $storagePath")
            
            val uploadTask = storageRef.putFile(imageUri).await()
            Timber.d("📸 PROFILE IMAGE DEBUG: Upload task completed, getting download URL...")
            
            val downloadUrl = storageRef.downloadUrl.await()
            Timber.d("📸 PROFILE IMAGE DEBUG: Download URL: $downloadUrl")
            
            // Update user document with image URL (only in users collection)
            Timber.d("📸 PROFILE IMAGE DEBUG: Updating users collection...")
            firestore.collection("users").document(userId)
                .update("profileImageUrl", downloadUrl.toString())
                .await()
            Timber.d("📸 PROFILE IMAGE DEBUG: ✅ users collection updated")
            
            Timber.i("📸 PROFILE IMAGE DEBUG: ✅ Profile image uploaded successfully!")
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Timber.e(e, "📸 PROFILE IMAGE DEBUG: ❌ Failed to upload profile image")
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
            Timber.d("🔍 ProfileCompletionService.canApplyDirectly - Checking for userId: $userId")
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return Result.failure(Exception("User not found"))
            val userRole = userData["role"] as? String ?: return Result.failure(Exception("User role not found"))
            
            Timber.d("🔍 ProfileCompletionService.canApplyDirectly - userRole: $userRole")
            
            val completion = if (userRole == "WORKER") {
                calculateWorkerProfileCompletion(userId)
                    } else {
                calculateEmployerProfileCompletion(userId)
            }
            
            Timber.d("🔍 ProfileCompletionService.canApplyDirectly - completion: $completion%, canApply: ${completion >= 80}")
            Result.success(completion >= 80)
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService.canApplyDirectly - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get user profile data from users collection
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any?>> {
        return try {
            Timber.d("🔍 ProfileCompletionService.getUserProfile - Fetching profile for userId: $userId")
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data?.toMutableMap() ?: mutableMapOf()
            
            if (userData.isEmpty()) {
                Timber.w("🔍 ProfileCompletionService.getUserProfile - No data found for userId: $userId")
                return Result.failure(Exception("User not found"))
            }
            
            Timber.d("🔍 ProfileCompletionService.getUserProfile - Profile data keys: ${userData.keys}")
            Timber.d("🔍 ProfileCompletionService.getUserProfile - fullName: ${userData["fullName"]}")
            Timber.d("🔍 ProfileCompletionService.getUserProfile - name: ${userData["name"]}")
            Timber.d("🔍 ProfileCompletionService.getUserProfile - email: ${userData["email"]}")
            Timber.d("🔍 ProfileCompletionService.getUserProfile - phone: ${userData["phone"]}")
            Timber.d("🔍 ProfileCompletionService.getUserProfile - profileImageUrl: ${userData["profileImageUrl"]}")
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
            
            // Use set with merge to create document if it doesn't exist
            firestore.collection("users").document(currentUser.uid)
                .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            Timber.d("🔍 ProfileCompletionService.saveEmployerProfileData - Saved profile data: ${profileData.keys}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService.saveEmployerProfileData - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get employer profile data from users collection
     */
    suspend fun getEmployerProfileData(userId: String): Result<Map<String, Any?>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return Result.failure(Exception("User not found"))
            
            Timber.d("🔍 ProfileCompletionService.getEmployerProfileData - keys: ${userData.keys}")
            Timber.d("🔍 ProfileCompletionService.getEmployerProfileData - profileImageUrl: ${userData["profileImageUrl"]}")
            Result.success(userData)
        } catch (e: Exception) {
            Timber.e(e, "Error getting employer profile data")
            Result.failure(e)
        }
    }
    
    /**
     * Get worker profile data from users collection
     */
    suspend fun getWorkerProfileData(userId: String): Result<Map<String, Any?>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: return Result.failure(Exception("User not found"))
            
            Timber.d("🔍 ProfileCompletionService.getWorkerProfileData - keys: ${userData.keys}")
            Result.success(userData)
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
    
    /**
     * Check if phone number exists with a different role
     * Uses the phone_roles collection which has PUBLIC READ access
     * Returns the existing role if found, null otherwise
     */
    suspend fun checkPhoneExistsWithDifferentRole(phone: String, currentRole: String): Result<String?> {
        return try {
            // Use canonical PhoneUtils for phone normalization
            val cleanPhone = PhoneUtils.normalizePhone(phone)
            
            Timber.d("Checking phone_roles for phone: $cleanPhone, currentRole: $currentRole")
            
            // Check in phone_roles collection (PUBLIC READ - no auth required)
            val phoneRoleDoc = firestore.collection("phone_roles")
                .document(cleanPhone)
                .get()
                .await()
            
            if (phoneRoleDoc.exists()) {
                val existingRole = phoneRoleDoc.getString("role")
                Timber.d("Found phone_roles entry: role=$existingRole")
                
                if (existingRole != null && existingRole.uppercase() != currentRole.uppercase()) {
                    Timber.d("Role mismatch! Existing: $existingRole, Current: $currentRole")
                    Result.success(existingRole) // Phone exists with different role
                } else {
                    Timber.d("Same role or no role found")
                    Result.success(null)
                }
            } else {
                Timber.d("No phone_roles entry found for $cleanPhone")
                Result.success(null) // Phone not found
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking phone existence in phone_roles")
            Result.failure(e)
        }
    }
    
    /**
     * Save phone-role mapping to phone_roles collection
     * Called when user completes profile setup
     * Includes device fingerprint for fraud prevention and joined date
     * 
     * NOTE: Device fingerprint is now handled by DeviceFingerprintService
     * which should be called separately during registration flow
     */
    suspend fun savePhoneRole(phone: String, role: String, deviceFingerprintService: DeviceFingerprintService? = null, context: Context? = null): Result<Unit> {
        return try {
            val cleanPhone = PhoneUtils.normalizePhone(phone)
            val currentTime = System.currentTimeMillis()
            
            // Build the data map
            val phoneRoleData = mutableMapOf<String, Any>(
                "role" to role.uppercase(),
                "updatedAt" to currentTime
            )
            
            // Check if this is a new entry (first time registration)
            val existingDoc = firestore.collection("phone_roles")
                .document(cleanPhone)
                .get()
                .await()
            
            if (!existingDoc.exists()) {
                // First time registration - add joinedAt
                phoneRoleData["joinedAt"] = currentTime
                Timber.d("📱 New user registration - adding joinedAt timestamp")
            }
            
            // Add device fingerprint if service and context are available
            // Uses canonical DeviceFingerprintService instead of duplicate DeviceFingerprint object
            if (deviceFingerprintService != null && context != null) {
                val deviceInfo = deviceFingerprintService.getDeviceInfo(context)
                phoneRoleData["deviceFingerprint"] = deviceInfo["deviceFingerprint"] as String
                phoneRoleData["deviceModel"] = deviceInfo["deviceModel"] as String
                phoneRoleData["androidId"] = deviceInfo["androidId"] as String
                Timber.d("📱 Device fingerprint added: ${deviceInfo["deviceFingerprint"]}")
            }
            
            firestore.collection("phone_roles")
                .document(cleanPhone)
                .set(phoneRoleData, com.google.firebase.firestore.SetOptions.merge())
                .await()
            
            Timber.d("Saved phone_roles entry: $cleanPhone -> $role with device info")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving phone role")
            Result.failure(e)
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
