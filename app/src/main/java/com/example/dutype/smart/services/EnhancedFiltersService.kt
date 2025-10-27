package com.example.dutype.smart.services

import com.example.dutype.smart.models.*
import com.example.dutype.worker.models.JobCardModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for advanced job filtering
 * Provides sophisticated filtering capabilities with multiple criteria
 */
@Singleton
class EnhancedFiltersService @Inject constructor() {
    
    /**
     * Apply filters to job list
     */
    fun filterJobs(jobs: List<JobCardModel>, filters: JobFilters): Flow<List<JobCardModel>> = flow {
        var filteredJobs = jobs
        
        // Apply search query filter
        if (filters.searchQuery.isNotEmpty()) {
            filteredJobs = filteredJobs.filter { job ->
                job.title.contains(filters.searchQuery, ignoreCase = true) ||
                job.employerName.contains(filters.searchQuery, ignoreCase = true) ||
                job.description.contains(filters.searchQuery, ignoreCase = true) ||
                job.tags.any { tag -> tag.text.contains(filters.searchQuery, ignoreCase = true) }
            }
        }
        
        // Apply location filter
        if (filters.location.isNotEmpty()) {
            filteredJobs = filteredJobs.filter { job ->
                job.location.getDisplayText().contains(filters.location, ignoreCase = true)
            }
        }
        
        // Apply salary range filter
        if (filters.salaryRange != null) {
            filteredJobs = filteredJobs.filter { job ->
                val jobSalary = extractJobSalary(job)
                jobSalary != null && isSalaryInRange(jobSalary, filters.salaryRange)
            }
        }
        
        // Apply experience level filter
        if (filters.experienceLevel != null) {
            filteredJobs = filteredJobs.filter { job ->
                val jobExperience = extractJobExperience(job)
                isExperienceMatch(jobExperience, filters.experienceLevel!!)
            }
        }
        
        // Apply employment type filter
        if (filters.employmentType != null) {
            filteredJobs = filteredJobs.filter { job ->
                val jobEmploymentType = extractJobEmploymentType(job)
                jobEmploymentType == filters.employmentType
            }
        }
        
        // Apply work type filter
        if (filters.workType != null) {
            filteredJobs = filteredJobs.filter { job ->
                val jobWorkType = extractJobWorkType(job)
                jobWorkType == filters.workType
            }
        }
        
        // Apply company size filter
        if (filters.companySize != null) {
            filteredJobs = filteredJobs.filter { job ->
                val jobCompanySize = extractJobCompanySize(job)
                jobCompanySize == filters.companySize
            }
        }
        
        // Apply industry filter
        if (filters.industry.isNotEmpty()) {
            filteredJobs = filteredJobs.filter { job ->
                job.tags.any { tag -> 
                    tag.text.contains(filters.industry, ignoreCase = true) 
                }
            }
        }
        
        // Apply skills filter
        if (filters.skills.isNotEmpty()) {
            filteredJobs = filteredJobs.filter { job ->
                val jobSkills = extractJobSkills(job)
                filters.skills.any { skill -> 
                    jobSkills.contains(skill.lowercase()) 
                }
            }
        }
        
        // Apply posted within filter
        if (filters.postedWithin != null) {
            val cutoffTime = System.currentTimeMillis() - (filters.postedWithin!!.getDaysAgo() * 24 * 60 * 60 * 1000L)
            filteredJobs = filteredJobs.filter { job ->
                job.postedAt >= cutoffTime
            }
        }
        
        // Apply remote filter
        if (filters.isRemote != null) {
            filteredJobs = filteredJobs.filter { job ->
                val isRemote = extractJobIsRemote(job)
                isRemote == filters.isRemote
            }
        }
        
        // Apply benefits filter
        if (filters.hasBenefits != null) {
            filteredJobs = filteredJobs.filter { job ->
                val hasBenefits = extractJobHasBenefits(job)
                hasBenefits == filters.hasBenefits
            }
        }
        
        // Apply verification filter
        if (filters.isVerified != null) {
            filteredJobs = filteredJobs.filter { job ->
                // In a real app, this would check if the employer is verified
                filters.isVerified == true // For demo, assume all jobs are verified
            }
        }
        
        emit(filteredJobs)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get filter suggestions based on available jobs
     */
    fun getFilterSuggestions(jobs: List<JobCardModel>): Flow<FilterSuggestions> = flow {
        val locations = jobs.map { it.location.getDisplayText() }.distinct().sorted()
        val companies = jobs.map { it.employerName }.distinct().sorted()
        val industries = jobs.flatMap { it.tags.map { tag -> tag.text } }.distinct().sorted()
        val skills = jobs.flatMap { extractJobSkills(it) }.distinct().sorted()
        
        val salaryRanges = generateSalaryRanges()
        val experienceLevels = ExperienceLevel.values().toList()
        val employmentTypes = EmploymentType.values().toList()
        val workTypes = WorkType.values().toList()
        val companySizes = CompanySize.values().toList()
        val postedWithinOptions = PostedWithin.values().toList()
        
        val suggestions = FilterSuggestions(
            locations = locations,
            companies = companies,
            industries = industries,
            skills = skills,
            salaryRanges = salaryRanges,
            experienceLevels = experienceLevels,
            employmentTypes = employmentTypes,
            workTypes = workTypes,
            companySizes = companySizes,
            postedWithinOptions = postedWithinOptions
        )
        
        emit(suggestions)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get popular filters
     */
    fun getPopularFilters(): Flow<List<PopularFilter>> = flow {
        val popularFilters = listOf(
            PopularFilter(
                name = "Remote Jobs",
                filter = JobFilters(isRemote = true),
                icon = "🏠",
                count = 0 // Will be calculated based on available jobs
            ),
            PopularFilter(
                name = "High Salary",
                filter = JobFilters(salaryRange = SalaryRange(80000.0, 200000.0)),
                icon = "💰",
                count = 0
            ),
            PopularFilter(
                name = "Senior Level",
                filter = JobFilters(experienceLevel = ExperienceLevel.SENIOR_LEVEL),
                icon = "⭐",
                count = 0
            ),
            PopularFilter(
                name = "Startup Jobs",
                filter = JobFilters(companySize = CompanySize.STARTUP),
                icon = "🚀",
                count = 0
            ),
            PopularFilter(
                name = "Full Time",
                filter = JobFilters(employmentType = EmploymentType.FULL_TIME),
                icon = "💼",
                count = 0
            ),
            PopularFilter(
                name = "Food Service Jobs",
                filter = JobFilters(industry = "Food Service"),
                icon = "🍳",
                count = 0
            )
        )
        
        emit(popularFilters)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Save filter as preset
     */
    suspend fun saveFilterPreset(userId: String, name: String, filters: JobFilters): Result<FilterPreset> {
        return try {
            val preset = FilterPreset(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                name = name,
                filters = filters,
                createdAt = System.currentTimeMillis()
            )
            
            // In a real app, save to database
            Result.success(preset)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get saved filter presets
     */
    fun getFilterPresets(userId: String): Flow<List<FilterPreset>> = flow {
        // In a real app, fetch from database
        val presets = listOf(
            FilterPreset(
                id = "1",
                userId = userId,
                name = "My Food Service Jobs",
                filters = JobFilters(
                    industry = "Food Service",
                    employmentType = EmploymentType.FULL_TIME,
                    experienceLevel = ExperienceLevel.MID_LEVEL
                ),
                createdAt = System.currentTimeMillis()
            ),
            FilterPreset(
                id = "2",
                userId = userId,
                name = "Remote Opportunities",
                filters = JobFilters(
                    isRemote = true,
                    workType = WorkType.REMOTE
                ),
                createdAt = System.currentTimeMillis()
            )
        )
        
        emit(presets)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Extract job salary from job data
     */
    private fun extractJobSalary(job: JobCardModel): SalaryRange? {
        // In a real app, this would parse salary from job description
        // For demo, return mock salary based on job title
        return when {
            job.title.contains("Senior", ignoreCase = true) -> SalaryRange(80000.0, 120000.0)
            job.title.contains("Lead", ignoreCase = true) -> SalaryRange(100000.0, 150000.0)
            job.title.contains("Manager", ignoreCase = true) -> SalaryRange(90000.0, 130000.0)
            job.title.contains("Junior", ignoreCase = true) -> SalaryRange(40000.0, 60000.0)
            else -> SalaryRange(50000.0, 80000.0)
        }
    }
    
    /**
     * Extract job experience level
     */
    private fun extractJobExperience(job: JobCardModel): ExperienceLevel {
        val jobText = "${job.title} ${job.description}".lowercase()
        
        return when {
            jobText.contains("senior") || jobText.contains("lead") -> ExperienceLevel.SENIOR_LEVEL
            jobText.contains("mid") || jobText.contains("intermediate") -> ExperienceLevel.MID_LEVEL
            jobText.contains("junior") || jobText.contains("entry") -> ExperienceLevel.ENTRY_LEVEL
            jobText.contains("executive") || jobText.contains("director") -> ExperienceLevel.EXECUTIVE_LEVEL
            else -> ExperienceLevel.MID_LEVEL
        }
    }
    
    /**
     * Extract job employment type
     */
    private fun extractJobEmploymentType(job: JobCardModel): EmploymentType {
        val jobText = "${job.title} ${job.description}".lowercase()
        
        return when {
            jobText.contains("part-time") || jobText.contains("part time") -> EmploymentType.PART_TIME
            jobText.contains("contract") -> EmploymentType.CONTRACT
            jobText.contains("freelance") -> EmploymentType.FREELANCE
            jobText.contains("intern") -> EmploymentType.INTERNSHIP
            jobText.contains("temporary") || jobText.contains("temp") -> EmploymentType.TEMPORARY
            else -> EmploymentType.FULL_TIME
        }
    }
    
    /**
     * Extract job work type
     */
    private fun extractJobWorkType(job: JobCardModel): WorkType {
        val jobText = "${job.title} ${job.description}".lowercase()
        
        return when {
            jobText.contains("remote") -> WorkType.REMOTE
            jobText.contains("hybrid") -> WorkType.HYBRID
            jobText.contains("flexible") -> WorkType.FLEXIBLE
            else -> WorkType.ON_SITE
        }
    }
    
    /**
     * Extract job company size
     */
    private fun extractJobCompanySize(job: JobCardModel): CompanySize {
        // In a real app, this would be stored in job data
        // For demo, return mock company size
        return when (job.employerName.lowercase()) {
            "local restaurant chain", "mega cleaning co", "enterprise services" -> CompanySize.ENTERPRISE
            "midcorp", "medium corp" -> CompanySize.LARGE
            "local restaurant", "small cleaning co" -> CompanySize.MEDIUM
            "startup", "new business" -> CompanySize.STARTUP
            else -> CompanySize.MEDIUM
        }
    }
    
    /**
     * Extract job skills
     */
    private fun extractJobSkills(job: JobCardModel): List<String> {
        val commonSkills = listOf(
            "cooking", "cleaning", "delivery", "driving", "gardening", "painting", "plumbing",
            "electrical", "carpentry", "sewing", "babysitting", "elderly care", "housekeeping",
            "maintenance", "repair", "assembly", "packaging", "cashier", "customer service",
            "communication", "leadership", "teamwork", "problem solving"
        )
        
        val jobText = "${job.title} ${job.description}".lowercase()
        return commonSkills.filter { skill -> jobText.contains(skill) }
    }
    
    /**
     * Extract if job is remote
     */
    private fun extractJobIsRemote(job: JobCardModel): Boolean {
        val jobText = "${job.title} ${job.description}".lowercase()
        return jobText.contains("remote") || jobText.contains("work from home")
    }
    
    /**
     * Extract if job has benefits
     */
    private fun extractJobHasBenefits(job: JobCardModel): Boolean {
        val jobText = "${job.title} ${job.description}".lowercase()
        return jobText.contains("benefits") || jobText.contains("health insurance") || 
               jobText.contains("401k") || jobText.contains("vacation")
    }
    
    /**
     * Check if salary is in range
     */
    private fun isSalaryInRange(jobSalary: SalaryRange, filterRange: SalaryRange): Boolean {
        return jobSalary.max >= filterRange.min && jobSalary.min <= filterRange.max
    }
    
    /**
     * Check if experience matches
     */
    private fun isExperienceMatch(jobExperience: ExperienceLevel, filterExperience: ExperienceLevel): Boolean {
        return jobExperience == filterExperience
    }
    
    /**
     * Generate salary ranges
     */
    private fun generateSalaryRanges(): List<SalaryRange> {
        return listOf(
            SalaryRange(30000.0, 50000.0),
            SalaryRange(50000.0, 70000.0),
            SalaryRange(70000.0, 90000.0),
            SalaryRange(90000.0, 120000.0),
            SalaryRange(120000.0, 150000.0),
            SalaryRange(150000.0, 200000.0)
        )
    }
}

/**
 * Filter suggestions data class
 */
data class FilterSuggestions(
    val locations: List<String> = emptyList(),
    val companies: List<String> = emptyList(),
    val industries: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val salaryRanges: List<SalaryRange> = emptyList(),
    val experienceLevels: List<ExperienceLevel> = emptyList(),
    val employmentTypes: List<EmploymentType> = emptyList(),
    val workTypes: List<WorkType> = emptyList(),
    val companySizes: List<CompanySize> = emptyList(),
    val postedWithinOptions: List<PostedWithin> = emptyList()
)

/**
 * Popular filter data class
 */
data class PopularFilter(
    val name: String,
    val filter: JobFilters,
    val icon: String,
    val count: Int
)

/**
 * Filter preset data class
 */
data class FilterPreset(
    val id: String,
    val userId: String,
    val name: String,
    val filters: JobFilters,
    val createdAt: Long
)
