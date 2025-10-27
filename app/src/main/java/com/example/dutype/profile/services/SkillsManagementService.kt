package com.example.dutype.profile.services

import com.example.dutype.profile.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing user skills
 * Handles skill CRUD operations, categorization, and recommendations
 */
@Singleton
class SkillsManagementService @Inject constructor() {
    
    // In-memory storage for skills (in production, use Room database)
    private val userSkills = mutableMapOf<String, MutableList<Skill>>()
    private val skillRecommendations = mutableListOf<Skill>()
    
    private val _skills = MutableStateFlow<List<Skill>>(emptyList())
    val skills: StateFlow<List<Skill>> = _skills.asStateFlow()
    
    init {
        initializeSkillRecommendations()
    }
    
    /**
     * Get all skills for a user
     */
    fun getUserSkills(userId: String): Flow<List<Skill>> = flow {
        val skills = userSkills[userId] ?: emptyList()
        emit(skills)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Add a new skill
     */
    suspend fun addSkill(userId: String, skill: Skill): Result<Skill> {
        return try {
            val skillList = userSkills.getOrPut(userId) { mutableListOf() }
            
            // Check if skill already exists
            if (skillList.any { it.name.equals(skill.name, ignoreCase = true) }) {
                return Result.failure(Exception("Skill already exists"))
            }
            
            val newSkill = skill.copy(
                id = UUID.randomUUID().toString(),
                addedAt = System.currentTimeMillis()
            )
            
            skillList.add(newSkill)
            _skills.value = skillList.toList()
            
            Result.success(newSkill)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing skill
     */
    suspend fun updateSkill(userId: String, skill: Skill): Result<Skill> {
        return try {
            val skillList = userSkills[userId] ?: return Result.failure(Exception("User not found"))
            val index = skillList.indexOfFirst { it.id == skill.id }
            
            if (index == -1) {
                return Result.failure(Exception("Skill not found"))
            }
            
            skillList[index] = skill
            _skills.value = skillList.toList()
            
            Result.success(skill)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove a skill
     */
    suspend fun removeSkill(userId: String, skillId: String): Result<Unit> {
        return try {
            val skillList = userSkills[userId] ?: return Result.failure(Exception("User not found"))
            val removed = skillList.removeAll { it.id == skillId }
            
            if (removed) {
                _skills.value = skillList.toList()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Skill not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get skills by category
     */
    fun getSkillsByCategory(userId: String, category: SkillCategory): Flow<List<Skill>> = flow {
        val skills = userSkills[userId]?.filter { it.category == category } ?: emptyList()
        emit(skills)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get skills by level
     */
    fun getSkillsByLevel(userId: String, level: SkillLevel): Flow<List<Skill>> = flow {
        val skills = userSkills[userId]?.filter { it.level == level } ?: emptyList()
        emit(skills)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search skills
     */
    fun searchSkills(userId: String, query: String): Flow<List<Skill>> = flow {
        val skills = userSkills[userId]?.filter { 
            it.name.contains(query, ignoreCase = true) 
        } ?: emptyList()
        emit(skills)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get skill recommendations based on existing skills
     */
    fun getSkillRecommendations(userId: String): Flow<List<Skill>> = flow {
        val userSkillNames = userSkills[userId]?.map { it.name.lowercase() } ?: emptyList()
        val recommendations = skillRecommendations.filter { 
            !userSkillNames.contains(it.name.lowercase()) 
        }
        emit(recommendations)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get skills statistics
     */
    fun getSkillsStatistics(userId: String): SkillsStatistics {
        val skills = userSkills[userId] ?: emptyList()
        
        val byCategory = skills.groupingBy { it.category }.eachCount()
        val byLevel = skills.groupingBy { it.level }.eachCount()
        val verifiedCount = skills.count { it.isVerified }
        val totalExperience = skills.sumOf { it.yearsOfExperience }
        
        return SkillsStatistics(
            totalSkills = skills.size,
            verifiedSkills = verifiedCount,
            totalExperience = totalExperience,
            byCategory = byCategory,
            byLevel = byLevel,
            averageExperience = if (skills.isNotEmpty()) totalExperience / skills.size else 0
        )
    }
    
    /**
     * Verify a skill
     */
    suspend fun verifySkill(userId: String, skillId: String, verifiedBy: String): Result<Unit> {
        return try {
            val skillList = userSkills[userId] ?: return Result.failure(Exception("User not found"))
            val index = skillList.indexOfFirst { it.id == skillId }
            
            if (index == -1) {
                return Result.failure(Exception("Skill not found"))
            }
            
            skillList[index] = skillList[index].copy(
                isVerified = true,
                verifiedBy = verifiedBy,
                verifiedAt = System.currentTimeMillis()
            )
            
            _skills.value = skillList.toList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get popular skills in the platform
     */
    fun getPopularSkills(): Flow<List<Skill>> = flow {
        // In a real app, this would come from analytics
        val popularSkills = listOf(
            Skill(
                name = "JavaScript",
                category = SkillCategory.TECHNICAL,
                level = SkillLevel.INTERMEDIATE,
                yearsOfExperience = 0
            ),
            Skill(
                name = "Python",
                category = SkillCategory.TECHNICAL,
                level = SkillLevel.INTERMEDIATE,
                yearsOfExperience = 0
            ),
            Skill(
                name = "Communication",
                category = SkillCategory.SOFT_SKILLS,
                level = SkillLevel.INTERMEDIATE,
                yearsOfExperience = 0
            ),
            Skill(
                name = "Project Management",
                category = SkillCategory.SOFT_SKILLS,
                level = SkillLevel.INTERMEDIATE,
                yearsOfExperience = 0
            ),
            Skill(
                name = "React",
                category = SkillCategory.FRAMEWORKS,
                level = SkillLevel.INTERMEDIATE,
                yearsOfExperience = 0
            )
        )
        emit(popularSkills)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Initialize skill recommendations
     */
    private fun initializeSkillRecommendations() {
        skillRecommendations.addAll(
            listOf(
                // Technical Skills
                Skill(name = "Cooking", category = SkillCategory.TECHNICAL, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Cleaning", category = SkillCategory.TECHNICAL, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Driving", category = SkillCategory.TECHNICAL, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Gardening", category = SkillCategory.TECHNICAL, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Painting", category = SkillCategory.TECHNICAL, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Plumbing", category = SkillCategory.TECHNICAL, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Electrical Work", category = SkillCategory.TECHNICAL, level = SkillLevel.INTERMEDIATE),
                
                // Service Skills
                Skill(name = "Customer Service", category = SkillCategory.FRAMEWORKS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Food Safety", category = SkillCategory.FRAMEWORKS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Menu Planning", category = SkillCategory.FRAMEWORKS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Kitchen Management", category = SkillCategory.FRAMEWORKS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Inventory Management", category = SkillCategory.FRAMEWORKS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Quality Control", category = SkillCategory.FRAMEWORKS, level = SkillLevel.INTERMEDIATE),
                
                // Specialized Skills
                Skill(name = "Babysitting", category = SkillCategory.DATABASES, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Elderly Care", category = SkillCategory.DATABASES, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Housekeeping", category = SkillCategory.DATABASES, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Maintenance", category = SkillCategory.DATABASES, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Repair Work", category = SkillCategory.DATABASES, level = SkillLevel.INTERMEDIATE),
                
                // Tools & Equipment
                Skill(name = "Cash Register", category = SkillCategory.TOOLS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Kitchen Equipment", category = SkillCategory.TOOLS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Cleaning Supplies", category = SkillCategory.TOOLS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Delivery Vehicle", category = SkillCategory.TOOLS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Garden Tools", category = SkillCategory.TOOLS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Painting Tools", category = SkillCategory.TOOLS, level = SkillLevel.INTERMEDIATE),
                
                // Soft Skills
                Skill(name = "Leadership", category = SkillCategory.SOFT_SKILLS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Teamwork", category = SkillCategory.SOFT_SKILLS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Problem Solving", category = SkillCategory.SOFT_SKILLS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Time Management", category = SkillCategory.SOFT_SKILLS, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Critical Thinking", category = SkillCategory.SOFT_SKILLS, level = SkillLevel.INTERMEDIATE),
                
                // Languages
                Skill(name = "English", category = SkillCategory.LANGUAGES, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Spanish", category = SkillCategory.LANGUAGES, level = SkillLevel.INTERMEDIATE),
                Skill(name = "French", category = SkillCategory.LANGUAGES, level = SkillLevel.INTERMEDIATE),
                Skill(name = "German", category = SkillCategory.LANGUAGES, level = SkillLevel.INTERMEDIATE),
                Skill(name = "Mandarin", category = SkillCategory.LANGUAGES, level = SkillLevel.INTERMEDIATE)
            )
        )
    }
}

/**
 * Skills statistics data class
 */
data class SkillsStatistics(
    val totalSkills: Int = 0,
    val verifiedSkills: Int = 0,
    val totalExperience: Int = 0,
    val averageExperience: Int = 0,
    val byCategory: Map<SkillCategory, Int> = emptyMap(),
    val byLevel: Map<SkillLevel, Int> = emptyMap()
)
