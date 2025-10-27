package com.example.dutype.profile.services

import com.example.dutype.profile.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling resume upload and parsing
 * Extracts information from resumes to auto-fill profile
 */
@Singleton
class ResumeUploadService @Inject constructor() {
    
    /**
     * Upload resume file
     */
    suspend fun uploadResume(
        userId: String,
        fileBytes: ByteArray,
        fileName: String,
        onProgress: (Float) -> Unit = {}
    ): Result<ResumeUploadResult> {
        return try {
            // Simulate upload progress
            for (progress in 0..100 step 10) {
                onProgress(progress / 100f)
                kotlinx.coroutines.delay(100)
            }
            
            // Validate file type
            if (!isValidResumeFile(fileName)) {
                return Result.failure(Exception("Invalid file type. Please upload PDF or DOC file."))
            }
            
            // Validate file size (max 10MB)
            if (fileBytes.size > 10 * 1024 * 1024) {
                return Result.failure(Exception("File size too large. Maximum size is 10MB."))
            }
            
            // Simulate resume parsing
            val parsedData = parseResumeContent(fileName, fileBytes)
            
            val result = ResumeUploadResult(
                resumeId = UUID.randomUUID().toString(),
                fileName = fileName,
                fileSize = fileBytes.size.toLong(),
                uploadUrl = "https://example.com/resumes/${UUID.randomUUID()}",
                parsedData = parsedData,
                uploadedAt = System.currentTimeMillis()
            )
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    
    /**
     * Validate resume file type
     */
    private fun isValidResumeFile(fileName: String): Boolean {
        val allowedExtensions = listOf("pdf", "doc", "docx", "txt")
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return allowedExtensions.contains(extension)
    }
    
    /**
     * Get file size in human readable format
     */
    fun getFileSizeString(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes bytes"
        }
    }
    
    /**
     * Extract skills from text content
     */
    fun extractSkillsFromText(text: String): List<String> {
        // Common local work skills to look for
        val commonSkills = listOf(
            "Cooking", "Cleaning", "Delivery", "Driving", "Gardening", "Painting", "Plumbing",
            "Electrical Work", "Carpentry", "Sewing", "Babysitting", "Elderly Care", "Housekeeping",
            "Maintenance", "Repair", "Assembly", "Packaging", "Cashier", "Customer Service",
            "Communication", "Leadership", "Teamwork", "Problem Solving",
            "Time Management", "Multi-tasking", "Organization"
        )
        
        val foundSkills = mutableListOf<String>()
        val lowerText = text.lowercase()
        
        commonSkills.forEach { skill ->
            if (lowerText.contains(skill.lowercase())) {
                foundSkills.add(skill)
            }
        }
        
        return foundSkills.distinct()
    }
    
    /**
     * Parse resume content to extract information
     */
    private fun parseResumeContent(fileName: String, fileBytes: ByteArray): ParsedResumeData {
        // Simulate resume parsing - in real implementation, use a PDF/DOC parser
        return ParsedResumeData(
            personalInfo = PersonalInfo(
                fullName = "John Doe",
                email = "dutypein@gmail.com",
                phone = "+1-555-0123"
            ),
            skills = listOf(
                Skill(
                    name = "Cooking",
                    category = SkillCategory.TECHNICAL,
                    level = SkillLevel.ADVANCED,
                    yearsOfExperience = 5
                ),
                Skill(
                    name = "Cleaning",
                    category = SkillCategory.TECHNICAL,
                    level = SkillLevel.INTERMEDIATE,
                    yearsOfExperience = 3
                ),
                Skill(
                    name = "Customer Service",
                    category = SkillCategory.TECHNICAL,
                    level = SkillLevel.ADVANCED,
                    yearsOfExperience = 4
                )
            ),
            workExperience = listOf(
                WorkExperience(
                    company = "Local Restaurant",
                    position = "Head Cook",
                    startDate = "2020-01",
                    endDate = null,
                    isCurrent = true,
                    location = "New York, NY",
                    description = "Led kitchen operations and trained new staff members.",
                    achievements = listOf(
                        "Improved kitchen efficiency by 40%",
                        "Led team of 5 kitchen staff",
                        "Implemented new menu items"
                    ),
                    skills = listOf("Cooking", "Food Safety", "Kitchen Management", "Team Leadership")
                )
            ),
            education = listOf(
                Education(
                    institution = "Local Community College",
                    degree = "Diploma",
                    fieldOfStudy = "Culinary Arts",
                    level = EducationLevel.DIPLOMA,
                    startDate = "2014-09",
                    endDate = "2016-05",
                    gpa = "3.8",
                    location = "New York, NY",
                    description = "Focused on cooking techniques and food service management"
                )
            ),
            summary = "Experienced cook with 5+ years in food service, specializing in restaurant operations and team leadership.",
            languages = listOf("English", "Spanish"),
            certifications = listOf("Food Safety Certified", "Culinary Arts Diploma"),
            projects = listOf("Menu Development", "Kitchen Efficiency Project", "Staff Training Program")
        )
    }

    /**
     * Get resume templates
     */
    fun getResumeTemplates(): Flow<List<ResumeTemplate>> = flow {
        val templates = listOf(
            ResumeTemplate(
                id = "1",
                name = "Professional",
                description = "Clean and professional design suitable for corporate roles",
                previewUrl = "https://example.com/templates/professional.jpg",
                isPremium = false
            ),
            ResumeTemplate(
                id = "2",
                name = "Creative",
                description = "Modern design with creative elements for design and marketing roles",
                previewUrl = "https://example.com/templates/creative.jpg",
                isPremium = true
            ),
            ResumeTemplate(
                id = "3",
                name = "Service Worker",
                description = "Structured layout perfect for service and manual work positions",
                previewUrl = "https://example.com/templates/service.jpg",
                isPremium = false
            ),
            ResumeTemplate(
                id = "4",
                name = "Executive",
                description = "Elegant design for senior and executive level positions",
                previewUrl = "https://example.com/templates/executive.jpg",
                isPremium = true
            )
        )
        emit(templates)
    }.flowOn(Dispatchers.IO)
}

/**
 * Resume upload result
 */
data class ResumeUploadResult(
    val resumeId: String,
    val fileName: String,
    val fileSize: Long,
    val uploadUrl: String,
    val parsedData: ParsedResumeData,
    val uploadedAt: Long
)

/**
 * Resume template
 */
data class ResumeTemplate(
    val id: String,
    val name: String,
    val description: String,
    val previewUrl: String,
    val isPremium: Boolean = false
)
