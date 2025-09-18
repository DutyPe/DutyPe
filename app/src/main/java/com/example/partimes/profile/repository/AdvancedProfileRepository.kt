package com.example.partimes.profile.repository

import com.example.partimes.profile.models.*
import com.example.partimes.profile.services.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing advanced user profiles
 * Coordinates between different profile services
 */
@Singleton
class AdvancedProfileRepository @Inject constructor(
    private val skillsManagementService: SkillsManagementService,
    private val resumeUploadService: ResumeUploadService,
    private val verificationService: VerificationService,
    private val profileCompletionService: ProfileCompletionService
) {
    
    // In-memory storage for profiles (in production, use Room database)
    private val userProfiles = mutableMapOf<String, AdvancedProfile>()
    
    /**
     * Get user profile
     */
    fun getUserProfile(userId: String): Flow<AdvancedProfile?> = flow {
        val profile = userProfiles[userId] ?: createDefaultProfile(userId)
        emit(profile)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(profile: AdvancedProfile): Result<AdvancedProfile> {
        return try {
            userProfiles[profile.userId] = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update personal information
     */
    suspend fun updatePersonalInfo(userId: String, personalInfo: PersonalInfo): Result<AdvancedProfile> {
        return try {
            val profile = userProfiles[userId] ?: createDefaultProfile(userId)
            val updatedProfile = profile.copy(
                personalInfo = personalInfo,
                lastUpdated = System.currentTimeMillis()
            )
            userProfiles[userId] = updatedProfile
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add skill to profile
     */
    suspend fun addSkill(userId: String, skill: Skill): Result<AdvancedProfile> {
        return try {
            val profile = userProfiles[userId] ?: createDefaultProfile(userId)
            val updatedSkills = profile.skills.toMutableList()
            updatedSkills.add(skill)
            
            val updatedProfile = profile.copy(
                skills = updatedSkills,
                lastUpdated = System.currentTimeMillis()
            )
            userProfiles[userId] = updatedProfile
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove skill from profile
     */
    suspend fun removeSkill(userId: String, skillId: String): Result<AdvancedProfile> {
        return try {
            val profile = userProfiles[userId] ?: return Result.failure(Exception("Profile not found"))
            val updatedSkills = profile.skills.filter { it.id != skillId }
            
            val updatedProfile = profile.copy(
                skills = updatedSkills,
                lastUpdated = System.currentTimeMillis()
            )
            userProfiles[userId] = updatedProfile
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add education to profile
     */
    suspend fun addEducation(userId: String, education: Education): Result<AdvancedProfile> {
        return try {
            val profile = userProfiles[userId] ?: createDefaultProfile(userId)
            val updatedEducation = profile.education.toMutableList()
            updatedEducation.add(education)
            
            val updatedProfile = profile.copy(
                education = updatedEducation,
                lastUpdated = System.currentTimeMillis()
            )
            userProfiles[userId] = updatedProfile
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add work experience to profile
     */
    suspend fun addWorkExperience(userId: String, experience: WorkExperience): Result<AdvancedProfile> {
        return try {
            val profile = userProfiles[userId] ?: createDefaultProfile(userId)
            val updatedExperience = profile.workExperience.toMutableList()
            updatedExperience.add(experience)
            
            val updatedProfile = profile.copy(
                workExperience = updatedExperience,
                lastUpdated = System.currentTimeMillis()
            )
            userProfiles[userId] = updatedProfile
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update work preferences
     */
    suspend fun updateWorkPreferences(userId: String, preferences: WorkPreferences): Result<AdvancedProfile> {
        return try {
            val profile = userProfiles[userId] ?: createDefaultProfile(userId)
            val updatedProfile = profile.copy(
                workPreferences = preferences,
                lastUpdated = System.currentTimeMillis()
            )
            userProfiles[userId] = updatedProfile
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload resume
     */
    suspend fun uploadResume(
        userId: String,
        fileBytes: ByteArray,
        fileName: String,
        onProgress: (Float) -> Unit = {}
    ): Result<AdvancedProfile> {
        return try {
            val uploadResult = resumeUploadService.uploadResume(userId, fileBytes, fileName, onProgress)
            
            uploadResult.fold(
                onSuccess = { result ->
                    val profile = userProfiles[userId] ?: createDefaultProfile(userId)
                    
                    // Auto-fill profile with parsed data
                    val updatedProfile = profile.copy(
                        resumeUrl = result.uploadUrl,
                        personalInfo = result.parsedData.personalInfo ?: profile.personalInfo,
                        skills = result.parsedData.skills,
                        workExperience = result.parsedData.workExperience,
                        education = result.parsedData.education,
                        bio = result.parsedData.summary,
                        lastUpdated = System.currentTimeMillis()
                    )
                    
                    userProfiles[userId] = updatedProfile
                    Result.success(updatedProfile)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update profile picture
     */
    suspend fun updateProfilePicture(userId: String, imageUrl: String): Result<AdvancedProfile> {
        return try {
            val profile = userProfiles[userId] ?: createDefaultProfile(userId)
            val updatedProfile = profile.copy(
                profilePictureUrl = imageUrl,
                lastUpdated = System.currentTimeMillis()
            )
            userProfiles[userId] = updatedProfile
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update bio
     */
    suspend fun updateBio(userId: String, bio: String): Result<AdvancedProfile> {
        return try {
            val profile = userProfiles[userId] ?: createDefaultProfile(userId)
            val updatedProfile = profile.copy(
                bio = bio,
                lastUpdated = System.currentTimeMillis()
            )
            userProfiles[userId] = updatedProfile
            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get profile completion status
     */
    fun getProfileCompletion(userId: String): Flow<ProfileCompletion> = flow {
        val profile = userProfiles[userId] ?: createDefaultProfile(userId)
        val completion = profileCompletionService.calculateProfileCompletion(profile)
        emit(completion)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get profile recommendations
     */
    fun getProfileRecommendations(userId: String): Flow<List<ProfileRecommendation>> = flow {
        val profile = userProfiles[userId] ?: createDefaultProfile(userId)
        profileCompletionService.getProfileRecommendations(profile).collect { recommendations ->
            emit(recommendations)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get profile insights
     */
    fun getProfileInsights(userId: String): Flow<ProfileInsights> = flow {
        val profile = userProfiles[userId] ?: createDefaultProfile(userId)
        val insights = profileCompletionService.getProfileInsights(profile)
        emit(insights)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get user skills
     */
    fun getUserSkills(userId: String): Flow<List<Skill>> = flow {
        val profile = userProfiles[userId] ?: createDefaultProfile(userId)
        emit(profile.skills)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get skill recommendations
     */
    fun getSkillRecommendations(userId: String): Flow<List<Skill>> = flow {
        skillsManagementService.getSkillRecommendations(userId).collect { recommendations ->
            emit(recommendations)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get verification status
     */
    fun getVerificationStatus(userId: String): Flow<List<Verification>> = flow {
        verificationService.getUserVerifications(userId).collect { verifications ->
            emit(verifications)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Start email verification
     */
    suspend fun startEmailVerification(userId: String, email: String): Result<VerificationCode> {
        return verificationService.startEmailVerification(userId, email)
    }
    
    /**
     * Start phone verification
     */
    suspend fun startPhoneVerification(userId: String, phone: String): Result<VerificationCode> {
        return verificationService.startPhoneVerification(userId, phone)
    }
    
    /**
     * Verify email
     */
    suspend fun verifyEmail(userId: String, code: String): Result<Verification> {
        return verificationService.verifyEmail(userId, code)
    }
    
    /**
     * Verify phone
     */
    suspend fun verifyPhone(userId: String, code: String): Result<Verification> {
        return verificationService.verifyPhone(userId, code)
    }
    
    /**
     * Create default profile for new user
     */
    private fun createDefaultProfile(userId: String): AdvancedProfile {
        return AdvancedProfile(
            userId = userId,
            personalInfo = PersonalInfo(),
            skills = emptyList(),
            education = emptyList(),
            workExperience = emptyList(),
            workPreferences = WorkPreferences(),
            verifications = emptyList(),
            portfolio = emptyList(),
            socialLinks = SocialLinks(),
            resumeUrl = null,
            profilePictureUrl = null,
            coverLetter = "",
            bio = "",
            profileCompletionPercentage = 0,
            lastUpdated = System.currentTimeMillis(),
            isPublic = true,
            isSearchable = true
        )
    }
}
