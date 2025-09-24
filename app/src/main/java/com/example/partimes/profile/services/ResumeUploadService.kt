package com.example.partimes.profile.services

import com.example.partimes.profile.models.*
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
        // Common technical skills to look for
        val commonSkills = listOf(
            "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Go", "Rust",
            "React", "Angular", "Vue", "Node.js", "Spring Boot", "Django", "Flask",
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Elasticsearch",
            "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Git", "Jenkins",
            "Communication", "Leadership", "Teamwork", "Problem Solving",
            "Project Management", "Agile", "Scrum"
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
                email = "john.doe@example.com",
                phone = "+1-555-0123"
            ),
            skills = listOf(
                Skill(
                    name = "Java",
                    category = SkillCategory.TECHNICAL,
                    level = SkillLevel.ADVANCED,
                    yearsOfExperience = 5
                ),
                Skill(
                    name = "Kotlin",
                    category = SkillCategory.TECHNICAL,
                    level = SkillLevel.INTERMEDIATE,
                    yearsOfExperience = 3
                ),
                Skill(
                    name = "Android Development",
                    category = SkillCategory.TECHNICAL,
                    level = SkillLevel.ADVANCED,
                    yearsOfExperience = 4
                )
            ),
            workExperience = listOf(
                WorkExperience(
                    company = "TechCorp Inc.",
                    position = "Senior Software Engineer",
                    startDate = "2020-01",
                    endDate = null,
                    isCurrent = true,
                    location = "New York, NY",
                    description = "Led development of mobile applications and mentored junior developers.",
                    achievements = listOf(
                        "Improved app performance by 40%",
                        "Led team of 5 developers",
                        "Implemented CI/CD pipeline"
                    ),
                    skills = listOf("Java", "Kotlin", "Android", "Git")
                )
            ),
            education = listOf(
                Education(
                    institution = "University of Technology",
                    degree = "Bachelor of Science",
                    fieldOfStudy = "Computer Science",
                    level = EducationLevel.BACHELORS,
                    startDate = "2014-09",
                    endDate = "2018-05",
                    gpa = "3.8",
                    location = "New York, NY",
                    description = "Focused on software engineering and mobile development"
                )
            ),
            summary = "Experienced software engineer with 5+ years in mobile development, specializing in Android applications and team leadership.",
            languages = listOf("English", "Spanish"),
            certifications = listOf("AWS Certified Developer", "Google Android Developer"),
            projects = listOf("E-commerce Mobile App", "Social Media Platform", "IoT Dashboard")
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
                name = "Technical",
                description = "Structured layout perfect for technical and engineering positions",
                previewUrl = "https://example.com/templates/technical.jpg",
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
