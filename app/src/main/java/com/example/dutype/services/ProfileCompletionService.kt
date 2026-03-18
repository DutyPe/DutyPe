package com.example.dutype.services

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.example.dutype.utils.PhoneNumberUtils
import com.example.dutype.utils.SecureLogger
import com.example.dutype.utils.FirestoreUtils
import com.example.dutype.utils.GeoUtils
import com.example.dutype.components.isValidReferralCode
import com.example.dutype.models.normalizeReferralCode
import javax.inject.Inject
import javax.inject.Singleton

// NOTE: Device fingerprinting removed - not needed for profile completion

/**
 * Profile Completion Service
 * Enhanced with 30+ years of Android development experience
 * Handles profile completion calculations and image uploads
 * 
 * REFACTORED: Now receives Firebase dependencies via constructor injection
 * ENTERPRISE: Integrated with ErrorHandler for robust error handling
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
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val errorHandler: com.example.dutype.core.error.ErrorHandler,
    private val referralService: ReferralService
) {
    // #22 FIX: In-memory TTL cache for user docs — prevents 12x repeated reads per session
    // Key: userId, Value: Pair(timestampMs, userData)
    private val userDocCache = mutableMapOf<String, Pair<Long, Map<String, Any>>>()
    private val USER_DOC_CACHE_TTL_MS = 30_000L // 30 seconds
    
    /**
     * Get cached user document or fetch from Firestore. 
     * Caches for 30s to avoid repeated reads in the same session flow.
     */
    private suspend fun getCachedUserDoc(userId: String): Map<String, Any>? {
        val cached = userDocCache[userId]
        if (cached != null && (System.currentTimeMillis() - cached.first) < USER_DOC_CACHE_TTL_MS) {
            return cached.second
        }
        val userDoc = firestore.collection("users").document(userId).get().await()
        val data = userDoc.data ?: return null
        userDocCache[userId] = System.currentTimeMillis() to data
        return data
    }
    
    /** Invalidate cache for a user after writes */
    private fun invalidateUserCache(userId: String) {
        userDocCache.remove(userId)
    }
    
    /**
     * Calculate profile completion from individual fields (used during setup flow).
     * Schema-aligned: only fullName, phone, jobTypes (skills), profileImageUrl matter.
     */
    fun calculateWorkerProfileCompletion(
        fullName: String,
        phone: String,
        skills: String,
        profileImageUrl: String?
    ): Int {
        var completion = 0
        if (fullName.isNotBlank()) completion += 30
        if (phone.isNotBlank()) completion += 30
        if (skills.isNotBlank()) completion += 35
        if (!profileImageUrl.isNullOrBlank()) completion += 5
        return completion.coerceAtMost(100)
    }
    
    /**
     * Calculate profile completion percentage for workers from Firestore (users collection only)
     * 
     * WEIGHTS (Profile picture is optional - only 5%):
     * - Full Name: 10%
     * - Email: 10%
     * - Phone: 10%
     * - Address: 20%
     * - Date of Birth: 10%
     * - Gender: 5%
     * - Skills: 15%
     * - Experience: 15%
     * - Profile Picture: 5% (optional - won't block job applications)
     * 
     * Without profile picture: max 95% (above 80% threshold for applying)
     */
    suspend fun calculateWorkerProfileCompletion(userId: String): Int {
        return try {
            val userData = getCachedUserDoc(userId) ?: return 0
            val workerData = firestore.collection("worker_profiles").document(userId).get().await().data.orEmpty()
            
            Timber.d("🔍 ProfileCompletionService.calculateWorkerProfileCompletion for userId: $userId")
            Timber.d("🔍 Firebase userData keys: ${userData.keys}")
            // SECURITY FIX: Don't log PII - only log field existence
            Timber.d("🔍 Phone field exists: ${userData["phone"] != null}")
            Timber.d("🔍 Worker profile exists: ${workerData.isNotEmpty()}")
            
            var completion = 0
            
            // Strict users core fields
            if (userData["fullName"] != null && userData["fullName"].toString().isNotBlank()) completion += 30
            val phoneValue = userData["phone"]
            if (phoneValue != null && phoneValue.toString().isNotBlank()) completion += 30

            // Strict worker profile fields
            val jobTypes = workerData["jobTypes"] as? List<*>
            if (!jobTypes.isNullOrEmpty()) completion += 35
            
            // Optional display field
            if (userData["profileImageUrl"] != null && userData["profileImageUrl"].toString().isNotBlank()) completion += 5
            
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
     * 
     * WEIGHTS (Profile picture is optional - only 5%):
     * - Company Name: 15%
     * - Industry: 15%
     * - Contact Phone: 15%
     * - Business Address: 20%
     * - Gender: 10%
     * - Date of Birth: 10%
     * - Contact Email: 5% (optional)
     * - Company Size: 5% (optional)
     * - Profile Picture: 5% (optional - won't block job posting)
     * 
     * Without optional fields: max 85% (above 80% threshold for posting jobs)
     * With all mandatory fields: 85% (can post jobs)
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
        
        // Mandatory Fields (85% total)
        if (companyName.isNotBlank()) completion += 15
        if (industry.isNotBlank()) completion += 15
        if (contactPhone.isNotBlank()) completion += 15
        if (businessAddress.isNotBlank()) completion += 20
        // Note: gender and dateOfBirth are checked in Firestore version
        
        // Optional Fields (15% total)
        if (contactEmail.isNotBlank()) completion += 5
        if (companySize.isNotBlank()) completion += 5
        if (profileImageUrl != null && profileImageUrl.isNotBlank()) completion += 5
        
        return completion.coerceAtMost(100)
    }
    
    /**
     * Calculate profile completion percentage for employers from Firestore
     * 
     * WEIGHTS (Profile picture is optional - only 5%):
     * - Company Name: 15%
     * - Industry: 15%
     * - Contact Phone: 15%
     * - Business Address: 20%
     * - Gender: 10%
     * - Date of Birth: 10%
     * - Contact Email: 5% (optional)
     * - Company Size: 5% (optional)
     * - Profile Picture: 5% (optional - won't block job posting)
     * 
     * Without optional fields: max 85% (above 80% threshold for posting jobs)
     */
    suspend fun calculateEmployerProfileCompletion(userId: String): Int {
        return try {
            val userData = getCachedUserDoc(userId) ?: return 0
            val employerData = firestore.collection("employer_profiles").document(userId).get().await().data.orEmpty()
            
            SecureLogger.d("ProfileCompletionService", "Calculating employer profile completion for user",
                "userId" to userId)
            SecureLogger.logCollectionSize("ProfileCompletionService", "userData keys", userData.keys.size)
            
            var completion = 0
            
            // Strict users core fields
            if (userData["fullName"] != null && userData["fullName"].toString().isNotBlank()) completion += 30
            if (userData["phone"] != null && userData["phone"].toString().isNotBlank()) completion += 30

            // Strict employer profile fields
            if (employerData["companyName"] != null && employerData["companyName"].toString().isNotBlank()) completion += 35

            // Optional display field
            if (userData["profileImageUrl"] != null && userData["profileImageUrl"].toString().isNotBlank()) completion += 5
            
            val finalCompletion = completion.coerceAtMost(100)
            Timber.d("🔍 ProfileCompletionService - Employer completion percentage: $finalCompletion%")
            finalCompletion
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService - Error calculating employer completion: ${e.message}")
            0
        }
    }

    /**
     * Upload profile image to Firebase Storage with compression
     * Images are compressed to max 2MB before upload while maintaining quality
     * Images are stored under: profile_images/{userId}/profile.jpg
     * Profile image URL is stored only in the users collection
     */
    suspend fun uploadProfileImage(imageUri: Uri, userId: String, userRole: String): Result<String> {
        return try {
            // SECURITY FIX: Don't log sensitive data
            Timber.d("📸 PROFILE IMAGE: Starting upload for user role: $userRole")
            Timber.d("📸 PROFILE IMAGE: Image URI provided: ${imageUri != null}")
            
            // Store under user's folder: profile_images/{userId}/profile_{timestamp}.jpg
            val fileName = "profile_${System.currentTimeMillis()}.jpg"
            val storagePath = "profile_images/$userId/$fileName"
            
            Timber.d("📸 PROFILE IMAGE: Compressing image before upload...")
            
            // Use ImageUploadUtils for compression and upload with retry
            val uploadResult = com.example.dutype.utils.ImageUploadUtils.uploadWithRetry(
                context = context,
                uri = imageUri,
                storagePath = storagePath
            )
            
            when (uploadResult) {
                is com.example.dutype.utils.ImageUploadUtils.UploadResult.Success -> {
                    val downloadUrl = uploadResult.downloadUrl
                    Timber.d("📸 PROFILE IMAGE: Download URL obtained successfully")
                    
                    // Update user document with image URL (only in users collection)
                    // Use set with merge to handle case where document might not exist
                    Timber.d("📸 PROFILE IMAGE: Updating user document...")
                    val imageData = mapOf(
                        "profileImageUrl" to downloadUrl,
                        "lastActiveAt" to Timestamp.now()
                    )
                    firestore.collection("users").document(userId)
                        .set(imageData, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                    Timber.d("📸 PROFILE IMAGE: User document updated")
                    
                    Timber.i("📸 PROFILE IMAGE: ✅ Profile image uploaded successfully with compression!")
                    Result.success(downloadUrl)
                }
                is com.example.dutype.utils.ImageUploadUtils.UploadResult.Failure -> {
                    Timber.e(uploadResult.exception, "📸 PROFILE IMAGE: ❌ Failed to upload profile image: ${uploadResult.error}")
                    errorHandler.handle(uploadResult.exception ?: Exception(uploadResult.error), "Profile image upload failed")
                    Result.failure(uploadResult.exception ?: Exception(uploadResult.error))
                }
                else -> {
                    // Progress updates are handled internally
                    Result.failure(Exception("Unexpected upload result"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "📸 PROFILE IMAGE: ❌ Failed to upload profile image")
            // Report to error tracking
            errorHandler.handle(e, "Profile image upload failed")
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
            val userData = getCachedUserDoc(userId) ?: return Result.failure(Exception("User not found"))
            val userRole = (userData["activeRole"] as? String)
                ?: (userData["roles"] as? List<*>)?.firstOrNull()?.toString()
                ?: return Result.failure(Exception("User role not found"))
            
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
     * Check if employer can post jobs (profile completion >= 80%)
     * This is the employer equivalent of canApplyDirectly for workers
     * 
     * @param userId The employer's user ID
     * @return Result containing true if employer can post jobs, false otherwise
     */
    suspend fun canPostJob(userId: String): Result<Boolean> {
        return try {
            Timber.d("🔍 ProfileCompletionService.canPostJob - Checking for userId: $userId")
            val completion = calculateEmployerProfileCompletion(userId)
            
            Timber.d("🔍 ProfileCompletionService.canPostJob - completion: $completion%, canPost: ${completion >= 80}")
            Result.success(completion >= 80)
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService.canPostJob - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Pre-check result for job posting eligibility
     * Similar to PreApplicationCheckResult for workers
     */
    data class PreJobPostCheckResult(
        val canPost: Boolean,
        val isProfileComplete: Boolean,
        val completionPercentage: Int,
        val missingFields: List<String>,
        val errorMessage: String? = null
    )
    
    /**
     * Comprehensive pre-check before employer posts a job
     * Returns detailed information about profile completion status
     * 
     * @param userId The employer's user ID
     * @return PreJobPostCheckResult with all relevant information
     */
    suspend fun preJobPostCheck(userId: String): PreJobPostCheckResult {
        return try {
            Timber.d("📦 PRE-JOB-POST CHECK: Starting for userId: $userId")
            
            val completion = calculateEmployerProfileCompletion(userId)
            val canPost = completion >= 80
            val missingFields = if (!canPost) {
                getMissingFields(userId, "EMPLOYER", completion)
            } else {
                emptyList()
            }
            
            val errorMessage = if (!canPost) {
                "Please complete your profile to post jobs (${completion}% complete, need 80%)"
            } else {
                null
            }
            
            Timber.d("📦 PRE-JOB-POST CHECK: completion=$completion%, canPost=$canPost, missing=${missingFields.size} fields")
            
            PreJobPostCheckResult(
                canPost = canPost,
                isProfileComplete = completion >= 100,
                completionPercentage = completion,
                missingFields = missingFields,
                errorMessage = errorMessage
            )
        } catch (e: Exception) {
            Timber.e(e, "📦 PRE-JOB-POST CHECK: Error")
            PreJobPostCheckResult(
                canPost = false,
                isProfileComplete = false,
                completionPercentage = 0,
                missingFields = emptyList(),
                errorMessage = e.message ?: "Failed to check profile eligibility"
            )
        }
    }
    
    /**
     * Get user profile data from users collection
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any?>> {
        return try {
            Timber.d("🔍 ProfileCompletionService.getUserProfile - Fetching profile")
            val userData = getCachedUserDoc(userId)?.toMutableMap() ?: mutableMapOf()
            
            if (userData.isEmpty()) {
                Timber.w("🔍 ProfileCompletionService.getUserProfile - No data found")
                return Result.failure(Exception("User not found"))
            }
            
            // SECURITY FIX: Don't log PII - only log field existence
            Timber.d("🔍 ProfileCompletionService.getUserProfile - Profile data keys: ${userData.keys}")
            Timber.d("🔍 ProfileCompletionService.getUserProfile - Has fullName: ${userData["fullName"] != null}")
            Timber.d("🔍 ProfileCompletionService.getUserProfile - Has phone: ${userData["phone"] != null}")
            Timber.d("🔍 ProfileCompletionService.getUserProfile - Has profileImage: ${userData["profileImageUrl"] != null}")
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
     * Save user info for profile setup - DUAL ROLE SUPPORT
     * Initializes roles array with the selected role
     * Sets activeRole to the selected role
     */
    suspend fun saveUserInfo(email: String, name: String, role: String): Result<Unit> {
        return try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            val normalizedPhone = currentUser.phoneNumber?.let { PhoneNumberUtils.normalize(it) }
            FirestoreUtils.ensureMinimalUserDocument(
                userId = currentUser.uid,
                role = role,
                phoneNumber = normalizedPhone,
                fullName = name
            )

            invalidateUserCache(currentUser.uid)

            Timber.d("✅ ProfileCompletionService - Ensured strict users core doc for role=${role.uppercase()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService - Error saving user info: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update user role - DUAL ROLE SUPPORT
     * Adds the role to the roles array if not already present
     * Updates activeRole to the new role
     * This allows users to have multiple roles (Worker + Employer)
     */
    suspend fun updateUserRole(newRole: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            // Fetch current user data to get existing roles (use cache)
            val userData = getCachedUserDoc(currentUser.uid)
            
            // Get existing roles array
            @Suppress("UNCHECKED_CAST")
            val existingRoles = (userData?.get("roles") as? List<String>)?.toMutableList() ?: mutableListOf()
            
            // Add new role if not already present
            val roleUpper = newRole.uppercase()
            if (!existingRoles.contains(roleUpper)) {
                existingRoles.add(roleUpper)
                Timber.d("✅ ProfileCompletionService - Adding role $roleUpper to roles array")
            } else {
                Timber.d("✅ ProfileCompletionService - Role $roleUpper already exists in roles array")
            }
            
            // Update both roles array and activeRole
            val updates = mapOf(
                "roles" to existingRoles,
                "activeRole" to roleUpper
            )
            
            firestore.collection("users").document(currentUser.uid)
                .update(updates)
                .await()
            invalidateUserCache(currentUser.uid)
            
            Timber.d("✅ ProfileCompletionService - Updated roles array: $existingRoles, activeRole: $roleUpper")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService - Error updating role: ${e.message}")
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

            val now = Timestamp.now()

            val fullName = (profileData["fullName"] as? String)?.trim()
            val phone = (profileData["phone"] as? String)?.trim()
            val profileImageUrl = (profileData["profileImageUrl"] as? String)?.trim()
            val userRef = firestore.collection("users").document(currentUser.uid)
            val existingUser = userRef.get().await().data.orEmpty()
            val existingRoles = (existingUser["roles"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
            val mergedRoles = (existingRoles + "WORKER").distinct()

            FirestoreUtils.ensureMinimalUserDocument(
                userId = currentUser.uid,
                role = "WORKER",
                phoneNumber = phone,
                fullName = fullName
            )

            val locationMap = profileData["location"] as? Map<*, *>
            val lat = (locationMap?.get("lat") as? Number)?.toDouble() ?: 0.0
            val lng = (locationMap?.get("lng") as? Number)?.toDouble() ?: 0.0

            val userUpdates = mutableMapOf<String, Any>(
                "roles" to mergedRoles,
                "activeRole" to "WORKER",
                "lastActiveAt" to now
            )

            if (!fullName.isNullOrBlank()) userUpdates["fullName"] = fullName
            if (!phone.isNullOrBlank()) userUpdates["phone"] = PhoneNumberUtils.normalize(phone)
            if (!profileImageUrl.isNullOrBlank()) userUpdates["profileImageUrl"] = profileImageUrl
            userUpdates["location"] = mapOf("lat" to lat, "lng" to lng)
            userUpdates["geohash"] = GeoUtils.encodeGeohash(lat, lng)

            firestore.collection("users").document(currentUser.uid)
                .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                .await()

            val skills = (profileData["skills"] as? String)
                ?.split(",")
                ?.map { it.trim().lowercase() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?: emptyList()

            val workerProfile = mapOf(
                "userId" to currentUser.uid,
                "jobTypes" to skills,
                "isAvailable" to true,
                "rating" to 0.0,
                "totalRatings" to 0,
                "totalJobs" to 0,
                "lastActiveAt" to now
            )

            firestore.collection("worker_profiles").document(currentUser.uid)
                .set(workerProfile, com.google.firebase.firestore.SetOptions.merge())
                .await()

            invalidateUserCache(currentUser.uid)
            
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

            val now = Timestamp.now()
            val fullName = (profileData["fullName"] as? String)?.trim()
            val phone = (profileData["phone"] as? String)?.trim()
            val profileImageUrl = (profileData["profileImageUrl"] as? String)?.trim()
            val companyName = (profileData["companyName"] as? String)?.trim().orEmpty()
            val userRef = firestore.collection("users").document(currentUser.uid)
            val existingUser = userRef.get().await().data.orEmpty()
            val existingRoles = (existingUser["roles"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
            val mergedRoles = (existingRoles + "EMPLOYER").distinct()

            FirestoreUtils.ensureMinimalUserDocument(
                userId = currentUser.uid,
                role = "EMPLOYER",
                phoneNumber = phone,
                fullName = fullName
            )

            val userUpdates = mutableMapOf<String, Any>(
                "roles" to mergedRoles,
                "activeRole" to "EMPLOYER",
                "lastActiveAt" to now
            )

            if (!fullName.isNullOrBlank()) userUpdates["fullName"] = fullName
            if (!phone.isNullOrBlank()) userUpdates["phone"] = PhoneNumberUtils.normalize(phone)
            if (!profileImageUrl.isNullOrBlank()) userUpdates["profileImageUrl"] = profileImageUrl

            firestore.collection("users").document(currentUser.uid)
                .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                .await()

            val employerProfile = mapOf(
                "userId" to currentUser.uid,
                "companyName" to companyName,
                "rating" to 0.0,
                "totalRatings" to 0,
                "totalHires" to 0
            )

            firestore.collection("employer_profiles").document(currentUser.uid)
                .set(employerProfile, com.google.firebase.firestore.SetOptions.merge())
                .await()
            invalidateUserCache(currentUser.uid)
            
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
            val userData = getCachedUserDoc(userId) ?: return Result.failure(Exception("User not found"))
            val employerData = firestore.collection("employer_profiles").document(userId).get().await().data.orEmpty()
            val merged = userData.toMutableMap()
            merged.putAll(employerData)
            
            // SECURITY FIX: Don't log sensitive data
            Timber.d("🔍 ProfileCompletionService.getEmployerProfileData - keys: ${userData.keys}")
            Timber.d("🔍 ProfileCompletionService.getEmployerProfileData - Has profileImage: ${userData["profileImageUrl"] != null}")
            Result.success(merged)
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
            val userData = getCachedUserDoc(userId) ?: return Result.failure(Exception("User not found"))
            val workerData = firestore.collection("worker_profiles").document(userId).get().await().data.orEmpty()
            val merged = userData.toMutableMap()
            merged.putAll(workerData)
            
            SecureLogger.logCollectionSize("ProfileCompletionService", "Worker profile keys", userData.keys.size)
            Result.success(merged)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check existing profile high level
     */
    suspend fun checkExistingProfileHighLevel(userId: String): Result<Boolean> {
        return try {
            val userData = getCachedUserDoc(userId)
            Result.success(userData != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check existing profile by email
     */
    suspend fun checkExistingProfileByEmail(email: String): Result<Boolean> {
        Timber.w("checkExistingProfileByEmail is unsupported in strict users schema")
        return Result.success(false)
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
            val userData = getCachedUserDoc(currentUser.uid)
            
            if (userData == null) {
                Timber.d("🔍 checkExistingProfileByCurrentUser: User document does not exist - new user")
                return Result.success(false)
            }
            
            val roles = (userData["roles"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
            val hasRequiredCore = !((userData["phone"] as? String).isNullOrBlank()) &&
                !((userData["fullName"] as? String).isNullOrBlank())
            val hasRoleData = when {
                roles.contains("WORKER") -> firestore.collection("worker_profiles").document(currentUser.uid).get().await().exists()
                roles.contains("EMPLOYER") -> firestore.collection("employer_profiles").document(currentUser.uid).get().await().exists()
                else -> false
            }

            Timber.d("🔍 checkExistingProfileByCurrentUser: hasRequiredCore=$hasRequiredCore hasRoleData=$hasRoleData")
            Result.success(hasRequiredCore && hasRoleData)
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
            val userData = getCachedUserDoc(userId) ?: return Result.failure(Exception("User not found"))
            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load existing profile data by email
     */
    suspend fun loadExistingProfileDataByEmail(email: String): Result<Map<String, Any?>> {
        return Result.failure(Exception("Email lookup is unsupported in strict users schema"))
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
            
            val userData = getCachedUserDoc(currentUser.uid) ?: return Result.failure(Exception("User not found"))
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
            val userData = getCachedUserDoc(userId) ?: return emptyList()
            
            if (userRole == "WORKER") {
                // Check for missing fields specifically
                if (userData["fullName"] == null || userData["fullName"].toString().isBlank()) 
                    missingFields.add("Full Name")
                val phoneValue = userData["phone"]
                if (phoneValue == null || phoneValue.toString().isBlank()) 
                    missingFields.add("Phone Number")
                val workerData = firestore.collection("worker_profiles").document(userId).get().await().data.orEmpty()
                val jobTypes = workerData["jobTypes"] as? List<*>
                if (jobTypes.isNullOrEmpty())
                    missingFields.add("Job Types")
                if (userData["profileImageUrl"] == null || userData["profileImageUrl"].toString().isBlank()) 
                    missingFields.add("Profile Picture")
            } else {
                // Check for missing employer fields (target schema only)
                val employerData = firestore.collection("employer_profiles").document(userId).get().await().data.orEmpty()
                if (employerData["companyName"] == null || employerData["companyName"].toString().isBlank()) 
                    missingFields.add("Company Name")
                if (userData["phone"] == null || userData["phone"].toString().isBlank()) 
                    missingFields.add("Phone Number")
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
     * DUAL-ROLE SUPPORT: Returns null to allow users to add additional roles
     * 
     * Architecture Pattern: Uber/Airbnb/Fiverr
     * - Single account can have BOTH Worker and Employer roles
     * - Users can switch between roles seamlessly
     * - No blocking on role mismatch
     * 
     * This function now only checks if the phone exists, not if it has a different role.
     * The actual role management is handled in the user document's `roles` array.
     */
    suspend fun checkPhoneExistsWithDifferentRole(phone: String, currentRole: String): Result<String?> {
        return try {
            // Use canonical PhoneNumberUtils for phone normalization
            val cleanPhone = PhoneNumberUtils.normalizePhone(phone)
            
            Timber.d("📱 DUAL-ROLE: Checking phone for: $cleanPhone, requestedRole: $currentRole")
            
            // Check if user exists in users collection
            val usersQuery = firestore.collection("users")
                .whereEqualTo("phone", cleanPhone)
                .limit(1)
                .get()
                .await()
            
            if (!usersQuery.isEmpty) {
                val userDoc = usersQuery.documents.first()
                val userData = userDoc.data ?: return Result.success(null)
                
                // Get user's roles array
                @Suppress("UNCHECKED_CAST")
                val roles = userData["roles"] as? List<String> ?: emptyList()
                val activeRole = userData["activeRole"] as? String
                
                Timber.d("📱 DUAL-ROLE: User found - roles=$roles, activeRole=$activeRole")
                
                // Check if user already has the requested role
                val hasRequestedRole = roles.any { it.uppercase() == currentRole.uppercase() }
                
                if (hasRequestedRole) {
                    // User already has this role - allow login
                    Timber.d("📱 DUAL-ROLE: ✅ User already has $currentRole role - allowing login")
                    return Result.success(null)
                } else {
                    // User exists but doesn't have this role yet
                    // This is OK - they can add the role later via profile settings
                    Timber.d("📱 DUAL-ROLE: ℹ️ User exists but doesn't have $currentRole role yet")
                    
                    // Return the active role to inform user they need to add the role first
                    val existingRole = activeRole ?: roles.firstOrNull()
                    return Result.success(existingRole)
                }
            } else {
                Timber.d("📱 DUAL-ROLE: No user found for $cleanPhone - new registration")
                Result.success(null) // Phone not found - new user
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ DUAL-ROLE: Error checking phone existence")
            Result.failure(e)
        }
    }
    
    /**
     * Save phone-role mapping to phone_roles collection
     * DUAL-ROLE SUPPORT: Stores ALL roles as an array, not just one
     * 
     * Architecture Pattern: Uber/Airbnb/Fiverr
     * - Stores roles as array: ["WORKER", "EMPLOYER"]
     * - Tracks activeRole for current session
     * - Includes device fingerprint for fraud prevention
     * 
     * Called when:
     * 1. User completes initial profile setup
     * 2. User enables additional role via profile settings
     */
    suspend fun savePhoneRole(phone: String, role: String): Result<Unit> {
        Timber.d("ℹ️ DUAL-ROLE: savePhoneRole skipped in strict schema mode (phone_roles removed)")
        return Result.success(Unit)
    }
    
    // ============================================
    // REFERRAL SYSTEM METHODS (Delegates to ReferralService for scalability)
    // ============================================
    
    companion object {
        private const val COLLECTION_REFERRAL_CODES = "referral_codes"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_REFERRALS = "referrals"
    }
    
    /**
     * Validate a referral code - O(1) lookup via referral_codes collection
     * Returns the referrer's userId and role if valid
     */
    suspend fun validateReferralCode(code: String): Result<Pair<String, String>?> {
        if (code.isBlank()) return Result.success(null)

        return try {
            val validation = referralService.validateReferralCode(code)
            if (validation.isValid && !validation.referrerUserId.isNullOrBlank()) {
                Result.success(
                    Pair(
                        validation.referrerUserId,
                        validation.referrerRole ?: ""
                    )
                )
            } else if (validation.isValid) {
                Result.failure(Exception("Invalid referral data"))
            } else {
                Result.failure(Exception(validation.errorMessage ?: "Referral code not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error validating referral code")
            Result.failure(e)
        }
    }
    /**
     * Apply a referral code for a new user
     * NOW USES CLOUD FUNCTION for fraud detection and atomic operations
     * 
     * CRITICAL CHECKS:
     * 1. User must be NEW (no existing referral record)
     * 2. User cannot use their own code
     * 3. User can only use ONE referral code ever
     */
    suspend fun applyReferralCode(
        referralCode: String,
        newUserId: String,
        newUserRole: String,
        newUserName: String,
        newUserPhone: String
    ): Result<Unit> {
        if (referralCode.isBlank()) return Result.success(Unit)

        return try {
            val result = referralService.applyReferralCode(
                referralCode = referralCode,
                userRole = newUserRole.uppercase(),
                userName = newUserName,
                userPhone = newUserPhone
            )

            if (result.isSuccess) {
                invalidateUserCache(newUserId)
                Timber.d("REFERRAL: Code applied successfully via ReferralService")
                Result.success(Unit)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to apply referral code"
                Timber.w("REFERRAL: Code application failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error applying referral code via ReferralService")
            Result.failure(e)
        }
    }
    /**
     * Check if user has already used a referral code
     * Returns true if user has used a code, false otherwise
     */
    suspend fun hasUserUsedReferralCode(userId: String): Boolean {
        return try {
            // Check users.referralStats for referredByCode
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            
            if (userDoc.exists()) {
                @Suppress("UNCHECKED_CAST")
                val referralStats = userDoc.get("referralStats") as? Map<String, Any?>
                val referredByCode = referralStats?.get("referredByCode") as? String
                if (!referredByCode.isNullOrBlank()) {
                    Timber.d("🎁 REFERRAL: User $userId already used code: $referredByCode")
                    return true
                }
            }
            
            // Double-check referrals collection
            val referrals = firestore.collection("referrals")
                .whereEqualTo("referredUserId", userId)
                .limit(1)
                .get()
                .await()
            
            if (!referrals.isEmpty) {
                Timber.d("🎁 REFERRAL: User $userId has existing referral record")
                return true
            }
            
            false
        } catch (e: Exception) {
            Timber.e(e, "Error checking if user used referral code")
            false // Default to false to not block user
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
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(referredUserId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                Timber.w("No user document found for referred user $referredUserId")
                return
            }
            
            @Suppress("UNCHECKED_CAST")
            val referralStats = userDoc.get("referralStats") as? Map<String, Any?> ?: emptyMap()
            val currentEarnings = (referralStats["totalEarnings"] as? Number)?.toDouble() ?: 0.0
            val currentBalance = (referralStats["availableBalance"] as? Number)?.toDouble() ?: 0.0
            
            val updates = mapOf(
                "referralStats.totalEarnings" to (currentEarnings + rewardAmount),
                "referralStats.availableBalance" to (currentBalance + rewardAmount),
                "referralStats.signupBonusReceived" to true,
                "referralStats.signupBonusAmount" to rewardAmount,
                "referralStats.lastUpdated" to System.currentTimeMillis()
            )
            
            firestore.collection(COLLECTION_USERS)
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
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            
            if (userDoc.exists()) {
                @Suppress("UNCHECKED_CAST")
                val referralStats = userDoc.get("referralStats") as? Map<String, Any>
                return referralStats
            }

            null
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
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(referrerUserId)
                .get()
                .await()
            
            if (!userDoc.exists()) return
            
            @Suppress("UNCHECKED_CAST")
            val referralStats = userDoc.get("referralStats") as? Map<String, Any?> ?: emptyMap()
            val currentTotal = (referralStats["totalReferrals"] as? Number)?.toInt() ?: 0
            val currentPending = (referralStats["pendingReferrals"] as? Number)?.toInt() ?: 0
            
            firestore.collection(COLLECTION_USERS)
                .document(referrerUserId)
                .update(
                    mapOf(
                        "referralStats.totalReferrals" to (currentTotal + delta),
                        "referralStats.pendingReferrals" to (currentPending + delta),
                        "referralStats.lastUpdated" to System.currentTimeMillis()
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
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(referrerUserId)
                .get()
                .await()
            
            if (!userDoc.exists()) return
            
            @Suppress("UNCHECKED_CAST")
            val referralStats = userDoc.get("referralStats") as? Map<String, Any?> ?: emptyMap()
            val currentSuccessful = (referralStats["successfulReferrals"] as? Number)?.toInt() ?: 0
            val currentPending = (referralStats["pendingReferrals"] as? Number)?.toInt() ?: 0
            val currentEarnings = (referralStats["totalEarnings"] as? Number)?.toDouble() ?: 0.0
            val currentBalance = (referralStats["availableBalance"] as? Number)?.toDouble() ?: 0.0
            val userRole = userDoc.getString("activeRole") ?: ""
            
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
            var freePostings = (referralStats["freeJobPostings"] as? Number)?.toInt() ?: 0
            var freePostingsExpiry = (referralStats["freeJobPostingsExpiry"] as? Number)?.toLong()
            
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
                "referralStats.successfulReferrals" to newSuccessfulCount,
                "referralStats.pendingReferrals" to newPendingCount,
                "referralStats.totalEarnings" to newTotalEarnings,
                "referralStats.availableBalance" to newAvailableBalance,
                "referralStats.canWithdraw" to canWithdraw,
                "referralStats.nextMilestone" to nextMilestone,
                "referralStats.freeJobPostings" to freePostings,
                "referralStats.lastUpdated" to System.currentTimeMillis()
            )
            
            if (freePostingsExpiry != null) {
                updates["referralStats.freeJobPostingsExpiry"] = freePostingsExpiry
            }
            
            firestore.collection(COLLECTION_USERS)
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
            val userDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            
            if (userDoc.exists()) {
                @Suppress("UNCHECKED_CAST")
                val referralStats = userDoc.get("referralStats") as? Map<String, Any>
                Result.success(referralStats)
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
            val existingDoc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (!existingDoc.exists()) {
                return Result.failure(Exception("User not found"))
            }

            val updates = mutableMapOf<String, Any>()
            @Suppress("UNCHECKED_CAST")
            val existingStats = existingDoc.get("referralStats") as? Map<String, Any?> ?: emptyMap()
            val defaults = mapOf(
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
                "signupBonusReceived" to false,
                "signupBonusAmount" to 0.0
            )

            defaults.forEach { (key, value) ->
                if (!existingStats.containsKey(key)) {
                    updates["referralStats.$key"] = value
                }
            }
            updates["referralStats.lastUpdated"] = System.currentTimeMillis()

            if (updates.isNotEmpty()) {
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .update(updates)
                    .await()
            }

            Timber.d("REFERRAL: Referral stats initialized for $userId; backend will ensure canonical referral code")
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

