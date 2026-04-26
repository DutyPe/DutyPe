package com.example.dutype.services

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
    private fun buildBasicUserFallback(userId: String): MutableMap<String, Any?> {
        val currentUser = auth.currentUser
        val fallback = mutableMapOf<String, Any?>(
            "userId" to userId,
            "fullName" to (currentUser?.displayName ?: ""),
            "phone" to (currentUser?.phoneNumber ?: "")
        )
        fallback.remove("fullName", "")
        fallback.remove("phone", "")
        return fallback
    }

    private fun profileCollectionForRole(role: String): String =
        if (role.uppercase() == "EMPLOYER") COLLECTION_EMPLOYER_PROFILES else COLLECTION_WORKER_PROFILES

    private suspend fun currentPhoneRoleData(userId: String): Map<String, Any> {
        val currentUser = auth.currentUser ?: return emptyMap()
        val phone = currentUser.phoneNumber?.takeIf { it.isNotBlank() }?.let(PhoneNumberUtils::normalize)
            ?: return emptyMap()
        if (currentUser.uid != userId) return emptyMap()
        return firestore.collection(COLLECTION_PHONE_ROLES).document(phone).get().await().data.orEmpty()
    }

    private fun extractSkills(rawSkills: Any?): List<String> {
        return when (rawSkills) {
            is List<*> -> rawSkills.mapNotNull { it?.toString()?.trim()?.lowercase() }
            is String -> rawSkills.split(",").map { it.trim().lowercase() }
            else -> emptyList()
        }.filter { it.isNotBlank() }
            .distinct()
    }

    private fun readWorkerSkills(workerData: Map<String, Any>): List<String> =
        extractSkills(workerData["skills"])

    private fun extractValidLocation(profileData: Map<String, Any>, existingUser: Map<String, Any>): Map<String, Any>? {
        val candidate = (profileData["location"] as? Map<*, *>) ?: (existingUser["location"] as? Map<*, *>)
        val lat = (candidate?.get("lat") as? Number)?.toDouble()
        val lng = (candidate?.get("lng") as? Number)?.toDouble()
        return if (lat != null && lng != null && GeoUtils.hasValidCoordinates(lat, lng)) {
            mapOf("lat" to lat, "lng" to lng)
        } else {
            null
        }
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
        * Calculate profile completion percentage for workers from worker_profiles.
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
            val workerData = try {
                firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(userId).get().await().data.orEmpty()
            } catch (e: Exception) {
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Timber.w("worker_profiles read denied for userId=%s; reporting empty profile", userId)
                    emptyMap<String, Any>()
                } else {
                    throw e
                }
            }
            
            Timber.d("🔍 ProfileCompletionService.calculateWorkerProfileCompletion for userId: $userId")
            Timber.d("🔍 Worker profile exists: ${workerData.isNotEmpty()}")
            
            var completion = 0
            
            if (workerData["fullName"] != null && workerData["fullName"].toString().isNotBlank()) completion += 30
            val phoneValue = workerData["phone"]
            if (phoneValue != null && phoneValue.toString().isNotBlank()) completion += 30

            // Strict worker profile fields
            val skills = readWorkerSkills(workerData)
            if (skills.isNotEmpty()) completion += 35
            
            // Optional display field
            if (workerData["profileImageUrl"] != null && workerData["profileImageUrl"].toString().isNotBlank()) completion += 5

            // SECURITY FIX: Do NOT inflate completion to 80% when worker_profiles is missing/unreadable.
            // The previous fallback let unverified users bypass the apply-gate by triggering a
            // PERMISSION_DENIED on worker_profiles. The apply-gate must reflect actual stored data.
            if (workerData.isEmpty()) {
                Timber.w("⚠️ ProfileCompletionService - worker_profiles empty/unreadable for $userId; reporting stored profile score")
            }

            val finalCompletion = completion.coerceAtMost(100)
            Timber.d("🔍 ProfileCompletionService - Final completion percentage: $finalCompletion%")
            finalCompletion
            } catch (e: Exception) {
            // SECURITY FIX: All failure paths return 0% so the apply-gate / post-gate refuses
            // to let unverified users in. Auth-only fallback is intentionally removed — it
            // overstated completion when worker_profiles couldn't be read.
            Timber.e(e, "❌ ProfileCompletionService - Error calculating completion (returning 0): ${e.message}")
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
     * - Contact Email: 5% (optional)
     * - Company Size: 5% (optional)
     * - Profile Picture: 5% (optional - won't block job posting)
     * 
     * Without optional fields: max 85% (above 80% threshold for posting jobs)
     */
    suspend fun calculateEmployerProfileCompletion(userId: String): Int {
        return try {
            val employerData = firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(userId).get().await().data.orEmpty()
            
            SecureLogger.d("ProfileCompletionService", "Calculating employer profile completion for user",
                "userId" to userId)
            
            var completion = 0
            
            if (employerData["fullName"] != null && employerData["fullName"].toString().isNotBlank()) completion += 30
            if (employerData["phone"] != null && employerData["phone"].toString().isNotBlank()) completion += 30

            // Strict employer profile fields
            if (employerData["companyName"] != null && employerData["companyName"].toString().isNotBlank()) completion += 35

            // Optional display field
            if (employerData["profileImageUrl"] != null && employerData["profileImageUrl"].toString().isNotBlank()) completion += 5
            
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
    * Profile image URL is stored on the role-specific profile document.
     */
    suspend fun uploadProfileImage(imageUri: Uri, userId: String, userRole: String): Result<String> {
        return try {
            // SECURITY FIX: Don't log sensitive data
            Timber.d("📸 PROFILE IMAGE: Starting upload for user role: $userRole")
            Timber.d("📸 PROFILE IMAGE: Image URI provided")
            
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
                    
                    Timber.d("📸 PROFILE IMAGE: Updating profile document...")
                    val imageData = mapOf(
                        "profileImageUrl" to downloadUrl,
                        "updatedAt" to Timestamp.now()
                    )
                    firestore.collection(profileCollectionForRole(userRole)).document(userId)
                        .set(imageData, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                    Timber.d("📸 PROFILE IMAGE: Profile document updated")
                    
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
            val workerProfileExists = firestore
                .collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES)
                .document(userId)
                .get()
                .await()
                .exists()
            if (!workerProfileExists) return Result.success(false)

            val completion = calculateWorkerProfileCompletion(userId)
            
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
     * Get user profile data from role-specific profile collections.
     */
    suspend fun getUserProfile(userId: String): Result<Map<String, Any?>> {
        return try {
            Timber.d("🔍 ProfileCompletionService.getUserProfile - Fetching profile")
            val workerData = firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(userId).get().await().data.orEmpty()
            val employerData = firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(userId).get().await().data.orEmpty()
            val userData = when {
                workerData.isNotEmpty() -> getWorkerProfileData(userId).getOrNull().orEmpty().toMutableMap()
                employerData.isNotEmpty() -> getEmployerProfileData(userId).getOrNull().orEmpty().toMutableMap()
                else -> mutableMapOf()
            }

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
     * Check if user has complete profile for their role.
     * Batch-p #1: 'complete' means good-enough to participate (>= 80%),
     * matching `canApplyDirectly`. Workers / employers who have filled
     * all the meaningful fields shouldn't see "Complete profile" CTAs
     * on the refer-and-earn screen just because the percentage sits at
     * 95% (e.g. one optional skill missing).
     */
    suspend fun isProfileComplete(userId: String, role: String): Result<Boolean> {
        return try {
            val completion = if (role == "WORKER") {
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
        * Save user info for profile setup.
     */
    suspend fun saveUserInfo(email: String, name: String, role: String): Result<Unit> {
        return try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            val normalizedPhone = currentUser.phoneNumber?.let { PhoneNumberUtils.normalize(it) }.orEmpty()
            if (normalizedPhone.isBlank()) {
                return Result.failure(IllegalArgumentException("Phone number is required"))
            }
            val now = Timestamp.now()
            val normalizedRole = role.uppercase()
            val batch = firestore.batch()
            batch.set(
                firestore.collection(COLLECTION_PHONE_ROLES).document(normalizedPhone),
                mapOf(
                    "phoneNumber" to normalizedPhone,
                    "role" to normalizedRole,
                    "name" to name.trim(),
                    "uid" to currentUser.uid,
                    "updatedAt" to now
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            batch.set(
                firestore.collection(profileCollectionForRole(normalizedRole)).document(currentUser.uid),
                mapOf(
                    "userId" to currentUser.uid,
                    "phone" to normalizedPhone,
                    "fullName" to name.trim(),
                    "role" to normalizedRole,
                    "updatedAt" to now
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            batch.commit().await()

            Timber.d("✅ ProfileCompletionService - Ensured identity/profile docs for role=${role.uppercase()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService - Error saving user info: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
        * Update user role.
     * Sets the user's single role.
     *
        * Accounts are single-role for life; call sites that previously appended
        * a second role are no longer valid.
     */
    suspend fun updateUserRole(newRole: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            val role = runCatching {
                com.example.dutype.models.UserRole.valueOf(newRole.uppercase())
            }.getOrDefault(com.example.dutype.models.UserRole.WORKER)

            val normalizedPhone = currentUser.phoneNumber?.let { PhoneNumberUtils.normalize(it) }.orEmpty()
            if (normalizedPhone.isBlank()) {
                return Result.failure(IllegalArgumentException("Phone number is required"))
            }
            val now = Timestamp.now()
            val batch = firestore.batch()
            batch.set(
                firestore.collection(COLLECTION_PHONE_ROLES).document(normalizedPhone),
                mapOf(
                    "phoneNumber" to normalizedPhone,
                    "role" to role.name,
                    "name" to currentUser.displayName.orEmpty(),
                    "uid" to currentUser.uid,
                    "updatedAt" to now
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            batch.set(
                firestore.collection(profileCollectionForRole(role.name)).document(currentUser.uid),
                mapOf("role" to role.name, "updatedAt" to now),
                com.google.firebase.firestore.SetOptions.merge()
            )
            batch.commit().await()

            Timber.d("✅ ProfileCompletionService - Set single role: ${role.name}")
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
            val workerRef = firestore.collection(COLLECTION_WORKER_PROFILES).document(currentUser.uid)
            val existingWorker = workerRef.get().await().data.orEmpty()

            val fullName = (profileData["fullName"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingWorker["fullName"] as? String)?.trim()
                ?: currentUser.displayName?.trim().orEmpty()
            val phone = (profileData["phone"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingWorker["phone"] as? String)?.trim()
                ?: currentUser.phoneNumber?.let(PhoneNumberUtils::normalize).orEmpty()
            val profileImageUrl = (profileData["profileImageUrl"] as? String)?.trim()
            val skills = extractSkills(profileData["skills"])

            if (fullName.isBlank()) {
                return Result.failure(IllegalArgumentException("Full name is required"))
            }
            if (phone.isBlank()) {
                return Result.failure(IllegalArgumentException("Phone number is required"))
            }
            if (skills.isEmpty()) {
                return Result.failure(IllegalArgumentException("Select at least one skill"))
            }

            val validLocation = extractValidLocation(profileData, existingWorker)
            val dateOfBirth = (profileData["dateOfBirth"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingWorker["dateOfBirth"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            val gender = (profileData["gender"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingWorker["gender"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            val experience = (profileData["experience"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingWorker["experience"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            val educationQualification = (profileData["educationQualification"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingWorker["educationQualification"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            val bio = (profileData["bio"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingWorker["bio"] as? String)?.trim()?.takeIf { it.isNotBlank() }

            val email = (profileData["email"] as? String)?.trim()?.takeIf { it.isNotBlank() }

            val workerProfile = mutableMapOf<String, Any>(
                "userId" to currentUser.uid,
                "fullName" to fullName,
                "phone" to PhoneNumberUtils.normalize(phone),
                "role" to "WORKER",
                "updatedAt" to now,
                "skills" to skills,
                "isAvailable" to ((existingWorker["isAvailable"] as? Boolean) ?: true)
                // Notes:
                //  - `userId` removed: redundant with doc ID (no readers use the body field).
                //  - `lastActiveAt` removed: profile freshness is tracked through `updatedAt`.
                //  - `jobTypes` was a legacy duplicate of `skills`; readers already fall back via skills.
                //  - rating / totalRatings / totalJobs are CF-only aggregates (never client-written).
            )
            if (!email.isNullOrBlank()) {
                workerProfile["email"] = email
            }
            if (!profileImageUrl.isNullOrBlank()) {
                workerProfile["profileImageUrl"] = profileImageUrl
            }
            if (validLocation != null) {
                workerProfile["location"] = validLocation
                workerProfile["geohash"] = GeoUtils.encodeGeohash(
                    (validLocation["lat"] as Number).toDouble(),
                    (validLocation["lng"] as Number).toDouble()
                )
            }
            if (!dateOfBirth.isNullOrBlank()) {
                workerProfile["dateOfBirth"] = dateOfBirth
            }
            if (!gender.isNullOrBlank()) {
                workerProfile["gender"] = gender
            }
            if (!experience.isNullOrBlank()) {
                workerProfile["experience"] = experience
            }
            if (!educationQualification.isNullOrBlank()) {
                workerProfile["educationQualification"] = educationQualification.take(120)
            }
            if (!bio.isNullOrBlank()) {
                workerProfile["bio"] = bio.take(300)
            }

            val batch = firestore.batch()
            batch.set(
                firestore.collection(COLLECTION_PHONE_ROLES).document(PhoneNumberUtils.normalize(phone)),
                mapOf(
                    "phoneNumber" to PhoneNumberUtils.normalize(phone),
                    "role" to "WORKER",
                    "name" to fullName,
                    "uid" to currentUser.uid,
                    "updatedAt" to now
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            batch.set(workerRef, workerProfile, com.google.firebase.firestore.SetOptions.merge())
            batch.commit().await()
            
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
            val employerRef = firestore.collection(COLLECTION_EMPLOYER_PROFILES).document(currentUser.uid)
            val existingEmployer = employerRef.get().await().data.orEmpty()

            val companyName = (profileData["companyName"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingEmployer["companyName"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (existingEmployer["fullName"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: currentUser.displayName?.trim()?.takeIf { it.isNotBlank() }
                .orEmpty()

            val fullName = (profileData["fullName"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingEmployer["fullName"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: companyName
            val phone = (profileData["phone"] as? String)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: (existingEmployer["phone"] as? String)?.trim()
                ?: currentUser.phoneNumber?.let(PhoneNumberUtils::normalize).orEmpty()
            val profileImageUrl = (profileData["profileImageUrl"] as? String)?.trim()

            if (fullName.isBlank()) {
                return Result.failure(IllegalArgumentException("Full name is required"))
            }
            if (phone.isBlank()) {
                return Result.failure(IllegalArgumentException("Phone number is required"))
            }
            if (companyName.isBlank()) {
                return Result.failure(IllegalArgumentException("Company name is required"))
            }

            val email = (profileData["email"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (profileData["contactEmail"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (existingEmployer["email"] as? String)?.trim()?.takeIf { it.isNotBlank() }

            val employerProfile = mutableMapOf<String, Any>(
                "userId" to currentUser.uid,
                "fullName" to fullName,
                "phone" to PhoneNumberUtils.normalize(phone),
                "role" to "EMPLOYER",
                "updatedAt" to now,
                "companyName" to companyName
                // Notes:
                //  - `userId` removed: redundant with doc ID (no readers use the body field).
                //  - `lastActiveAt` removed: profile freshness is tracked through `updatedAt`.
                //  - isVerified / rating / totalRatings / totalHires are CF-only aggregates.
            )
            if (!email.isNullOrBlank()) {
                employerProfile["email"] = email
            }
            if (!profileImageUrl.isNullOrBlank()) {
                employerProfile["profileImageUrl"] = profileImageUrl
            }
            // Optional business fields. Each is persisted only when the form
            // explicitly supplied it (or it already exists), so the rule's
            // hasOnly() whitelist is never broken with empty strings.
            val gstNumber = (profileData["gstNumber"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (existingEmployer["gstNumber"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            if (!gstNumber.isNullOrBlank()) {
                employerProfile["gstNumber"] = gstNumber
            }
            val industry = (profileData["industry"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (existingEmployer["industry"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            if (!industry.isNullOrBlank()) {
                employerProfile["industry"] = industry
            }
            val companySize = (profileData["companySize"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (existingEmployer["companySize"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            if (!companySize.isNullOrBlank()) {
                employerProfile["companySize"] = companySize
            }
            val businessAddress = (profileData["businessAddress"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (existingEmployer["businessAddress"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            if (!businessAddress.isNullOrBlank()) {
                employerProfile["businessAddress"] = businessAddress
            }
            val businessLocation = (profileData["businessLocation"] as? Map<*, *>)
                ?: (existingEmployer["businessLocation"] as? Map<*, *>)
            val businessLat = (businessLocation?.get("lat") as? Number)?.toDouble()
            val businessLng = (businessLocation?.get("lng") as? Number)?.toDouble()
            if (businessLat != null && businessLng != null && com.example.dutype.utils.GeoUtils.hasValidCoordinates(businessLat, businessLng)) {
                employerProfile["businessLocation"] = mapOf("lat" to businessLat, "lng" to businessLng)
            }

            val batch = firestore.batch()
            batch.set(
                firestore.collection(COLLECTION_PHONE_ROLES).document(PhoneNumberUtils.normalize(phone)),
                mapOf(
                    "phoneNumber" to PhoneNumberUtils.normalize(phone),
                    "role" to "EMPLOYER",
                    "name" to fullName,
                    "uid" to currentUser.uid,
                    "updatedAt" to now
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            batch.set(employerRef, employerProfile, com.google.firebase.firestore.SetOptions.merge())
            batch.commit().await()
            
            Timber.d("🔍 ProfileCompletionService.saveEmployerProfileData - Saved profile data: ${profileData.keys}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ ProfileCompletionService.saveEmployerProfileData - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get employer profile data from employer_profiles plus phoneRoles identity.
     */
    suspend fun getEmployerProfileData(userId: String): Result<Map<String, Any?>> {
        return try {
            val employerData = firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(userId).get().await().data.orEmpty()
            val phoneRoleData = currentPhoneRoleData(userId)
            val merged = employerData.toMutableMap()
            merged["userId"] = userId
            (phoneRoleData["phoneNumber"] as? String)?.takeIf { it.isNotBlank() }?.let { merged["phone"] = merged["phone"] ?: it }
            (phoneRoleData["name"] as? String)?.takeIf { it.isNotBlank() }?.let { merged["fullName"] = merged["fullName"] ?: it }
            merged["role"] = merged["role"] ?: "EMPLOYER"
            
            // SECURITY FIX: Don't log sensitive data
            Timber.d("🔍 ProfileCompletionService.getEmployerProfileData - keys: ${merged.keys}")
            Timber.d("🔍 ProfileCompletionService.getEmployerProfileData - Has profileImage: ${merged["profileImageUrl"] != null}")
            Result.success(merged)
        } catch (e: Exception) {
            Timber.e(e, "Error getting employer profile data")
            Result.failure(e)
        }
    }
    
    /**
     * Get worker profile data from worker_profiles plus phoneRoles identity.
     */
    suspend fun getWorkerProfileData(userId: String): Result<Map<String, Any?>> {
        return try {
            val workerData = firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(userId).get().await().data.orEmpty()
            val phoneRoleData = currentPhoneRoleData(userId)
            val merged = workerData.toMutableMap()
            merged["userId"] = userId
            (phoneRoleData["phoneNumber"] as? String)?.takeIf { it.isNotBlank() }?.let { merged["phone"] = merged["phone"] ?: it }
            (phoneRoleData["name"] as? String)?.takeIf { it.isNotBlank() }?.let { merged["fullName"] = merged["fullName"] ?: it }
            merged["role"] = merged["role"] ?: "WORKER"
            val skills = readWorkerSkills(workerData)
            if (skills.isNotEmpty()) {
                merged["skills"] = skills
            }
            
            SecureLogger.logCollectionSize("ProfileCompletionService", "Worker profile keys", merged.keys.size)
            Result.success(merged)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bug #19 fix: employer-side worker profile read.
     *
     * Firestore rules block direct employer reads of `worker_profiles`. Use
     * the `getWorkerProfileForEmployer` callable, which authorises by
     * checking that the caller has an application from this worker
     * (optionally scoped to a specific jobId for the tightest check).
     */
    suspend fun getWorkerProfileForEmployer(
        workerId: String,
        jobId: String? = null,
    ): Result<Map<String, Any?>> {
        return try {
            val payload = mutableMapOf<String, Any>("workerId" to workerId)
            if (!jobId.isNullOrBlank()) payload["jobId"] = jobId
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("Employer not authenticated"))
            val token = currentUser.getIdToken(true).await().token
            if (token.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Employer auth token unavailable"))
            }
            // Batch-k fix: `getWorkerProfileForEmployer` is deployed via
            // `onCallSecured` which pins the function to `asia-south1`. The
            // default `FirebaseFunctions.getInstance()` targets us-central1
            // and returns NOT_FOUND, which surfaced on the employer-side
            // worker profile view as "Failed to load worker profile".
            // Explicitly pin the client to the deployment region.
            val res = com.google.firebase.functions.FirebaseFunctions
                .getInstance("asia-south1")
                .getHttpsCallable("getWorkerProfileForEmployer")
                .call(payload)
                .await()
            @Suppress("UNCHECKED_CAST")
            val data = res.data as? Map<String, Any?> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val profile = (data["profile"] as? Map<String, Any?>) ?: emptyMap()
            Result.success(profile)
        } catch (e: Exception) {
            Timber.w(e, "getWorkerProfileForEmployer failed for workerId=$workerId")
            Result.failure(e)
        }
    }
    /**
     * Check existing profile high level
     */
    suspend fun checkExistingProfileHighLevel(userId: String): Result<Boolean> {
        return try {
            val hasWorker = firestore.collection(COLLECTION_WORKER_PROFILES).document(userId).get().await().exists()
            val hasEmployer = firestore.collection(COLLECTION_EMPLOYER_PROFILES).document(userId).get().await().exists()
            Result.success(hasWorker || hasEmployer)
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
            
            Timber.d("🔍 checkExistingProfileByCurrentUser: Checking profile document for UID: ${currentUser.uid}")
            val phoneRoleData = currentPhoneRoleData(currentUser.uid)
            if (phoneRoleData.isEmpty()) {
                Timber.d("🔍 checkExistingProfileByCurrentUser: phoneRoles document does not exist - new user")
                return Result.success(false)
            }

            val resolvedRole = (phoneRoleData["role"] as? String)
            val roleUpper = resolvedRole?.uppercase()
            val hasRequiredCore = !((phoneRoleData["phoneNumber"] as? String).isNullOrBlank()) &&
                !((phoneRoleData["name"] as? String).isNullOrBlank())
            val hasRoleData = when (roleUpper) {
                "WORKER" -> firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(currentUser.uid).get().await().exists()
                "EMPLOYER" -> firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(currentUser.uid).get().await().exists()
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
            getUserProfile(userId)
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
            
            getUserProfile(currentUser.uid)
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
            if (userRole == "WORKER") {
                val workerData = firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(userId).get().await().data.orEmpty()
                // Check for missing fields specifically
                if (workerData["fullName"] == null || workerData["fullName"].toString().isBlank())
                    missingFields.add("Full Name")
                val phoneValue = workerData["phone"]
                if (phoneValue == null || phoneValue.toString().isBlank()) 
                    missingFields.add("Phone Number")
                if (readWorkerSkills(workerData).isEmpty())
                    missingFields.add("Skills")
                if (workerData["profileImageUrl"] == null || workerData["profileImageUrl"].toString().isBlank())
                    missingFields.add("Profile Picture")
            } else {
                // Check for missing employer fields (target schema only)
                val employerData = firestore.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(userId).get().await().data.orEmpty()
                if (employerData["fullName"] == null || employerData["fullName"].toString().isBlank())
                    missingFields.add("Full Name")
                if (employerData["companyName"] == null || employerData["companyName"].toString().isBlank()) 
                    missingFields.add("Company Name")
                if (employerData["phone"] == null || employerData["phone"].toString().isBlank())
                    missingFields.add("Phone Number")
                if (employerData["profileImageUrl"] == null || employerData["profileImageUrl"].toString().isBlank())
                    missingFields.add("Profile Picture")
            }
            
            missingFields
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ============================================
    // REFERRAL SYSTEM METHODS (Delegates to ReferralService for scalability)
    // ============================================
    
    companion object {
        private const val COLLECTION_PHONE_ROLES = "phoneRoles"
        private const val COLLECTION_WORKER_PROFILES = "worker_profiles"
        private const val COLLECTION_EMPLOYER_PROFILES = "employer_profiles"
        private const val COLLECTION_REFERRALS = "referrals"
        private const val COLLECTION_REFERRAL_STATS = com.example.dutype.firestore.FirestoreCollections.REFERRAL_STATS
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
            // Check canonical referral_stats/{userId} document
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()
            
            if (statsDoc.exists()) {
                val referredByCode = statsDoc.getString("referredByCode")
                if (!referredByCode.isNullOrBlank()) {
                    Timber.d("🎁 REFERRAL: User $userId already used code: $referredByCode")
                    return true
                }
            }
            
            // Double-check referrals collection
            val referrals = firestore.collection(com.example.dutype.firestore.FirestoreCollections.REFERRALS)
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
            Timber.w("🎁 REFERRAL: Unable to check referral usage, defaulting to false: ${e.message}")
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
        Timber.d("REFERRAL: Strict schema mode - updateReferredUserStats skipped for $referredUserId")
    }
    
    /**
     * Get or create referral stats for a user
     */
    private suspend fun getOrCreateReferralStats(userId: String, userRole: String): Map<String, Any>? {
        return try {
            val statsDoc = firestore.collection(COLLECTION_REFERRAL_STATS)
                .document(userId)
                .get()
                .await()
            
            if (statsDoc.exists()) {
                @Suppress("UNCHECKED_CAST")
                return statsDoc.data as? Map<String, Any>
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
        Timber.d("REFERRAL: Strict schema mode - updateReferrerPendingCount skipped for $referrerUserId")
    }
    
    /**
     * Update referrer's stats after successful referral
     */
    private suspend fun updateReferrerStats(referrerUserId: String, rewardAmount: Double) {
        Timber.d("REFERRAL: Strict schema mode - updateReferrerStats skipped for $referrerUserId")
    }

    private fun com.example.dutype.models.ReferralStats.toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "userRole" to userRole,
        "referralCode" to referralCode,
        "totalReferrals" to totalReferrals,
        "successfulReferrals" to successfulReferrals,
        "totalEarnings" to totalEarnings,
        "availableBalance" to availableBalance,
        "canWithdraw" to canWithdraw,
        "currentTier" to currentTier.name
    )

    private fun com.example.dutype.models.Referral.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "referrerId" to referrerUserId,
        "referredUserId" to referredUserId,
        "referralCode" to referralCode,
        "status" to status.name,
        "rewardAmount" to rewardAmount,
        "bonusAmount" to bonusAmount,
        "referredUserReward" to referredUserReward,
        "createdAt" to createdAt,
        "completedAt" to completedAt,
        "deviceFingerprint" to deviceFingerprint,
        "referredUserName" to referredUserName,
        "referredUserRole" to referredUserRole
    )
    
    /**
     * Get referral stats for current user
     */
    suspend fun getReferralStats(): Result<Map<String, Any>?> {
        return try {
            referralService.getCurrentUserReferralStats().fold(
                onSuccess = { stats ->
                    @Suppress("UNCHECKED_CAST")
                    Result.success(stats.toMap() as Map<String, Any>)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting referral stats")
            Result.failure(e)
        }
    }
    
    /**
     * Get referral history for current user
     * SENIOR FIX: Use canonical field name 'referrerId' (not legacy 'referrerUserId')
     * Aligns with Firestore schema and Cloud Function referral data model
     */
    suspend fun getReferralHistory(limit: Int = 20): Result<List<Map<String, Any>>> {
        return try {
            val history = referralService.getReferralHistory(limit)
            @Suppress("UNCHECKED_CAST")
            Result.success(history.map { it.toMap() as Map<String, Any> })
        } catch (e: Exception) {
            Timber.e(e, "Error getting referral history")
            Result.failure(e)
        }
    }
    
    /**
     * Create referral stats for a new user
     * This generates their unique referral code that they can share
     */
    suspend fun createReferralStats(userId: String, userRole: String, userName: String = ""): Result<Unit> {
        Timber.d("REFERRAL: Strict schema mode - createReferralStats skipped for $userId")
        return Result.success(Unit)
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

