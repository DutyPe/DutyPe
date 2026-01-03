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
    private val auth: FirebaseAuth,
    private val functions: com.google.firebase.functions.FirebaseFunctions,
    private val deviceFingerprintService: DeviceFingerprintService,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
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
    
    // ============================================
    // REFERRAL SYSTEM METHODS (Delegates to ReferralService for scalability)
    // ============================================
    
    companion object {
        private const val COLLECTION_REFERRAL_CODES = "referral_codes"
        private const val COLLECTION_REFERRAL_STATS = "referral_stats"
        private const val COLLECTION_REFERRALS = "referrals"
    }
    
    /**
     * Validate a referral code - O(1) lookup via referral_codes collection
     * Returns the referrer's userId and role if valid
     */
    suspend fun validateReferralCode(code: String): Result<Pair<String, String>?> {
        if (code.isBlank()) return Result.success(null)
        
        val trimmedCode = code.trim().uppercase()
        if (!com.example.dutype.models.isValidReferralCode(trimmedCode)) {
            return Result.failure(Exception("Invalid code format. Use WRK or EMP followed by 6 characters."))
        }
        
        return try {
            // O(1) lookup by document ID in referral_codes collection
            val codeDoc = firestore.collection(COLLECTION_REFERRAL_CODES)
                .document(trimmedCode)
                .get()
                .await()
            
            if (!codeDoc.exists()) {
                // Fallback: Check referral_stats for backward compatibility
                val querySnapshot = firestore.collection(COLLECTION_REFERRAL_STATS)
                    .whereEqualTo("referralCode", trimmedCode)
                    .limit(1)
                    .get()
                    .await()
                
                if (querySnapshot.isEmpty) {
                    return Result.failure(Exception("Referral code not found"))
                }
                
                val doc = querySnapshot.documents.first()
                val referrerUserId = doc.getString("userId") ?: ""
                val referrerRole = doc.getString("userRole") ?: ""
                
                if (referrerUserId.isNotBlank()) {
                    return Result.success(Pair(referrerUserId, referrerRole))
                } else {
                    return Result.failure(Exception("Invalid referral data"))
                }
            }
            
            // Check if code is active
            val isActive = codeDoc.getBoolean("isActive") ?: true
            if (!isActive) {
                return Result.failure(Exception("This referral code is no longer active"))
            }
            
            val referrerUserId = codeDoc.getString("userId") ?: ""
            val referrerRole = codeDoc.getString("userRole") ?: ""
            
            if (referrerUserId.isNotBlank()) {
                Result.success(Pair(referrerUserId, referrerRole))
            } else {
                Result.failure(Exception("Invalid referral data"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error validating referral code")
            Result.failure(e)
        }
    }
    
    /**
     * Apply a referral code for a new user
     * NOW USES CLOUD FUNCTION for fraud detection and atomic operations
     */
    suspend fun applyReferralCode(
        referralCode: String,
        newUserId: String,
        newUserRole: String,
        newUserName: String,
        newUserPhone: String
    ): Result<Unit> {
        if (referralCode.isBlank()) return Result.success(Unit)
        
        val trimmedCode = referralCode.trim().uppercase()
        
        return try {
            // Validate the code first (client-side for quick feedback)
            val validationResult = validateReferralCode(trimmedCode)
            if (validationResult.isFailure) {
                return Result.failure(validationResult.exceptionOrNull() ?: Exception("Invalid code"))
            }
            
            val referrerInfo = validationResult.getOrNull()
                ?: return Result.failure(Exception("Referral code not found"))
            
            val (referrerUserId, _) = referrerInfo
            
            // Don't allow self-referral
            if (referrerUserId == newUserId) {
                return Result.failure(Exception("Cannot use your own referral code"))
            }
            
            // Get device fingerprint for fraud detection
            val deviceFingerprint = deviceFingerprintService.getDeviceFingerprint(context)
            
            // Call Cloud Function for server-side processing with fraud detection
            val data = hashMapOf(
                "referralCode" to trimmedCode,
                "userRole" to newUserRole.uppercase(),
                "userName" to newUserName,
                "userPhone" to newUserPhone,
                "deviceFingerprint" to deviceFingerprint
            )
            
            Timber.d("🎁 REFERRAL: Applying code $trimmedCode via Cloud Function")
            
            val result = functions
                .getHttpsCallable("applyReferralCode")
                .call(data)
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val response = result.data as? Map<String, Any?> ?: emptyMap()
            
            val success = response["success"] as? Boolean ?: false
            
            if (success) {
                Timber.d("🎁 REFERRAL: ✅ Code applied successfully via Cloud Function")
                Result.success(Unit)
            } else {
                val error = response["error"] as? String ?: "Failed to apply referral code"
                Timber.w("🎁 REFERRAL: ❌ Code application failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error applying referral code via Cloud Function")
            Result.failure(e)
        }
    }
    
    /**
     * Complete a referral when referred user completes their profile
     * NOTE: This is now handled AUTOMATICALLY by Cloud Function 'onReferredUserProfileComplete'
     * when the user's profileCompleted field changes to true.
     * This method is kept for backward compatibility but does nothing.
     */
    suspend fun completeReferral(referredUserId: String): Result<Unit> {
        // The Cloud Function 'onReferredUserProfileComplete' automatically handles this
        // when the user document's profileCompleted field changes from false to true.
        // No client-side action needed - the trigger fires automatically.
        Timber.d("🎁 REFERRAL: completeReferral called for $referredUserId - handled by Cloud Function")
        return Result.success(Unit)
    }
    
    /**
     * Update referred user's stats (signup bonus for using a referral code)
     */
    private suspend fun updateReferredUserStats(referredUserId: String, rewardAmount: Double) {
        try {
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(referredUserId)
                .get()
                .await()
            
            if (!statsDoc.exists()) {
                Timber.w("No referral stats found for referred user $referredUserId")
                return
            }
            
            val currentEarnings = (statsDoc.getDouble("totalEarnings") ?: 0.0)
            val currentBalance = (statsDoc.getDouble("availableBalance") ?: 0.0)
            
            val updates = mapOf(
                "totalEarnings" to (currentEarnings + rewardAmount),
                "availableBalance" to (currentBalance + rewardAmount),
                "signupBonusReceived" to true,
                "signupBonusAmount" to rewardAmount,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(referredUserId)
                .update(updates)
                .await()
            
            Timber.d("🎁 Updated referred user $referredUserId stats: +₹$rewardAmount signup bonus")
        } catch (e: Exception) {
            Timber.e(e, "Error updating referred user stats")
        }
    }
    
    /**
     * Get or create referral stats for a user
     */
    private suspend fun getOrCreateReferralStats(userId: String, userRole: String): Map<String, Any>? {
        return try {
            val doc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()
            
            if (doc.exists()) {
                doc.data
            } else {
                // Create new stats with unique referral code
                val prefix = if (userRole.uppercase() == "EMPLOYER") "EMP" else "WRK"
                val referralCode = "$prefix${userId.take(6).uppercase()}"
                
                val newStats = mapOf(
                    "userId" to userId,
                    "userRole" to userRole,
                    "referralCode" to referralCode,
                    "totalReferrals" to 0,
                    "successfulReferrals" to 0,
                    "pendingReferrals" to 0,
                    "totalEarnings" to 0.0,
                    "pendingEarnings" to 0.0,
                    "withdrawnAmount" to 0.0,
                    "availableBalance" to 0.0,
                    "canWithdraw" to false,
                    "nextMilestone" to 5,
                    "freeJobPostings" to 0,
                    "lastUpdated" to System.currentTimeMillis()
                )
                
                firestore.collection(COLLECTION_REFERRAL_STATS)
                    .document(userId)
                    .set(newStats)
                    .await()
                
                Timber.d("Created referral stats for user $userId with code $referralCode")
                newStats
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting/creating referral stats")
            null
        }
    }
    
    /**
     * Update referrer's pending count
     */
    private suspend fun updateReferrerPendingCount(referrerUserId: String, delta: Int) {
        try {
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(referrerUserId)
                .get()
                .await()
            
            if (!statsDoc.exists()) return
            
            val currentTotal = (statsDoc.getLong("totalReferrals") ?: 0).toInt()
            val currentPending = (statsDoc.getLong("pendingReferrals") ?: 0).toInt()
            
            firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(referrerUserId)
                .update(
                    mapOf(
                        "totalReferrals" to (currentTotal + delta),
                        "pendingReferrals" to (currentPending + delta),
                        "lastUpdated" to System.currentTimeMillis()
                    )
                )
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error updating pending count")
        }
    }
    
    /**
     * Update referrer's stats after successful referral
     */
    private suspend fun updateReferrerStats(referrerUserId: String, rewardAmount: Double) {
        try {
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(referrerUserId)
                .get()
                .await()
            
            if (!statsDoc.exists()) return
            
            val currentSuccessful = (statsDoc.getLong("successfulReferrals") ?: 0).toInt()
            val currentPending = (statsDoc.getLong("pendingReferrals") ?: 0).toInt()
            val currentEarnings = (statsDoc.getDouble("totalEarnings") ?: 0.0)
            val currentBalance = (statsDoc.getDouble("availableBalance") ?: 0.0)
            val userRole = statsDoc.getString("userRole") ?: ""
            
            val newSuccessfulCount = currentSuccessful + 1
            val newPendingCount = maxOf(0, currentPending - 1)
            
            // Check for milestone bonus
            var bonusAmount = 0.0
            val milestones = mapOf(5 to 50.0, 10 to 100.0, 15 to 150.0)
            if (milestones.containsKey(newSuccessfulCount)) {
                bonusAmount = milestones[newSuccessfulCount] ?: 0.0
            }
            
            val newTotalEarnings = currentEarnings + rewardAmount + bonusAmount
            val newAvailableBalance = currentBalance + rewardAmount + bonusAmount
            
            // Can withdraw at 5, 10, 15 referrals, and anytime after 15
            val canWithdraw = newSuccessfulCount >= 15 || listOf(5, 10, 15).contains(newSuccessfulCount)
            
            val nextMilestone = when {
                newSuccessfulCount < 5 -> 5
                newSuccessfulCount < 10 -> 10
                newSuccessfulCount < 15 -> 15
                else -> newSuccessfulCount + 1
            }
            
            // Calculate employer free postings
            var freePostings = (statsDoc.getLong("freeJobPostings") ?: 0).toInt()
            var freePostingsExpiry = statsDoc.getLong("freeJobPostingsExpiry")
            
            if (userRole == "EMPLOYER") {
                when (newSuccessfulCount) {
                    5 -> {
                        freePostings = 5
                        freePostingsExpiry = System.currentTimeMillis() + (15 * 24 * 60 * 60 * 1000L)
                    }
                    10 -> {
                        freePostings = 10
                        freePostingsExpiry = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
                    }
                }
            }
            
            val updates = mutableMapOf<String, Any>(
                "successfulReferrals" to newSuccessfulCount,
                "pendingReferrals" to newPendingCount,
                "totalEarnings" to newTotalEarnings,
                "availableBalance" to newAvailableBalance,
                "canWithdraw" to canWithdraw,
                "nextMilestone" to nextMilestone,
                "freeJobPostings" to freePostings,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            if (freePostingsExpiry != null) {
                updates["freeJobPostingsExpiry"] = freePostingsExpiry
            }
            
            firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(referrerUserId)
                .update(updates)
                .await()
            
            if (bonusAmount > 0) {
                Timber.d("Milestone bonus earned: ₹$bonusAmount for $newSuccessfulCount referrals")
            }
            
            Timber.d("Updated referrer stats: successful=$newSuccessfulCount, balance=₹$newAvailableBalance")
        } catch (e: Exception) {
            Timber.e(e, "Error updating referrer stats")
        }
    }
    
    /**
     * Get referral stats for current user
     */
    suspend fun getReferralStats(): Result<Map<String, Any>?> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            val doc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()
            
            if (doc.exists()) {
                Result.success(doc.data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting referral stats")
            Result.failure(e)
        }
    }
    
    /**
     * Get referral history for current user
     */
    suspend fun getReferralHistory(limit: Int = 20): Result<List<Map<String, Any>>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        
        return try {
            val querySnapshot = firestore.collection(COLLECTION_REFERRALS)
                .whereEqualTo("referrerUserId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val referrals = querySnapshot.documents.mapNotNull { it.data }
            Result.success(referrals)
        } catch (e: Exception) {
            Timber.e(e, "Error getting referral history")
            Result.failure(e)
        }
    }
    
    /**
     * Create referral stats for a new user
     * This generates their unique referral code that they can share
     * 
     * NOTE: The Cloud Function 'onUserProfileComplete' also creates this automatically
     * when profileCompleted changes to true. This method serves as a fallback
     * and for backward compatibility.
     */
    suspend fun createReferralStats(userId: String, userRole: String, userName: String = ""): Result<Unit> {
        return try {
            // Check if stats already exist (Cloud Function may have already created them)
            val existingDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()
            
            if (existingDoc.exists()) {
                val existingCode = existingDoc.getString("referralCode")
                if (!existingCode.isNullOrBlank()) {
                    Timber.d("🎁 Referral stats already exist for user $userId with code $existingCode (likely created by Cloud Function)")
                    return Result.success(Unit)
                }
            }
            
            // Generate unique referral code (fallback if Cloud Function didn't create it)
            val referralCode = com.example.dutype.models.generateReferralCode(userId, userRole)
            
            // Use batch write for atomicity
            val batch = firestore.batch()
            
            // 1. Create referral_codes document (code as document ID for O(1) lookup)
            val codeDoc = firestore.collection(COLLECTION_REFERRAL_CODES).document(referralCode)
            val codeLookup = mapOf(
                "code" to referralCode,
                "userId" to userId,
                "userRole" to userRole.uppercase(),
                "userName" to userName,
                "isActive" to true,
                "createdAt" to System.currentTimeMillis(),
                "totalUsed" to 0
            )
            batch.set(codeDoc, codeLookup)
            
            // 2. Create referral_stats document (userId as document ID)
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS).document(userId)
            val newStats = mapOf(
                "userId" to userId,
                "userRole" to userRole.uppercase(),
                "referralCode" to referralCode,
                "totalReferrals" to 0,
                "successfulReferrals" to 0,
                "pendingReferrals" to 0,
                "expiredReferrals" to 0,
                "rejectedReferrals" to 0,
                "totalEarnings" to 0.0,
                "pendingEarnings" to 0.0,
                "withdrawnAmount" to 0.0,
                "availableBalance" to 0.0,
                "canWithdraw" to false,
                "nextMilestone" to 5,
                "currentTier" to "BRONZE",
                "freeJobPostings" to 0,
                "lastUpdated" to System.currentTimeMillis(),
                "isBlocked" to false
            )
            batch.set(statsDoc, newStats, com.google.firebase.firestore.SetOptions.merge())
            
            // Commit batch
            batch.commit().await()
            
            Timber.d("🎁 Created referral code $referralCode for user $userId (client-side fallback)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error creating referral stats")
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
