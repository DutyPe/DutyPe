package com.example.partimes.smart.models

import com.example.partimes.worker.models.ApplicationStatus
import com.example.partimes.worker.models.JobCardModel

/**
 * Smart features models for intelligent job matching and recommendations
 */

/**
 * Job recommendation types
 */
enum class RecommendationType {
    SKILL_MATCH,
    LOCATION_BASED,
    RECENTLY_VIEWED,
    SIMILAR_JOBS,
    TRENDING,
    SALARY_MATCH,
    EXPERIENCE_MATCH,
    COMPANY_MATCH
}

/**
 * Job recommendation model
 */
data class JobRecommendation(
    val id: String,
    val jobId: String,
    val userId: String,
    val type: RecommendationType,
    val score: Double, // 0.0 to 1.0
    val reason: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false,
    val isApplied: Boolean = false,
    val isSaved: Boolean = false
)

/**
 * Enhanced filter options
 */
data class JobFilters(
    val searchQuery: String = "",
    val location: String = "",
    val salaryRange: SalaryRange? = null,
    val experienceLevel: ExperienceLevel? = null,
    val employmentType: EmploymentType? = null,
    val workType: WorkType? = null,
    val companySize: CompanySize? = null,
    val industry: String = "",
    val skills: List<String> = emptyList(),
    val postedWithin: PostedWithin? = null,
    val isRemote: Boolean? = null,
    val hasBenefits: Boolean? = null,
    val isVerified: Boolean? = null
)

/**
 * Salary range for filtering
 */
data class SalaryRange(
    val min: Double,
    val max: Double,
    val currency: String = "USD",
    val period: String = "annually" // annually, monthly, hourly
)

/**
 * Experience levels
 */
enum class ExperienceLevel {
    ENTRY_LEVEL,
    MID_LEVEL,
    SENIOR_LEVEL,
    EXECUTIVE_LEVEL
}

/**
 * Employment types
 */
enum class EmploymentType {
    FULL_TIME,
    PART_TIME,
    CONTRACT,
    FREELANCE,
    INTERNSHIP,
    TEMPORARY
}

/**
 * Work types
 */
enum class WorkType {
    REMOTE,
    ON_SITE,
    HYBRID,
    FLEXIBLE
}

/**
 * Company sizes
 */
enum class CompanySize {
    STARTUP, // 1-10
    SMALL, // 11-50
    MEDIUM, // 51-200
    LARGE, // 201-1000
    ENTERPRISE // 1000+
}

/**
 * Posted within timeframes
 */
enum class PostedWithin {
    TODAY,
    LAST_3_DAYS,
    LAST_WEEK,
    LAST_2_WEEKS,
    LAST_MONTH
}

/**
 * Recently viewed job
 */
data class RecentlyViewedJob(
    val id: String,
    val jobId: String,
    val userId: String,
    val viewedAt: Long = System.currentTimeMillis(),
    val viewDuration: Long = 0, // in milliseconds
    val isApplied: Boolean = false,
    val isSaved: Boolean = false
)


/**
 * Search suggestion
 */
data class SearchSuggestion(
    val text: String,
    val type: SuggestionType,
    val popularity: Int = 0,
    val category: String = ""
)

/**
 * Search suggestion types
 */
enum class SuggestionType {
    JOB_TITLE,
    COMPANY,
    SKILL,
    LOCATION,
    INDUSTRY
}

/**
 * Job matching score
 */
data class JobMatchScore(
    val jobId: String,
    val userId: String,
    val overallScore: Double, // 0.0 to 1.0
    val skillMatch: Double = 0.0,
    val experienceMatch: Double = 0.0,
    val locationMatch: Double = 0.0,
    val salaryMatch: Double = 0.0,
    val companyMatch: Double = 0.0,
    val calculatedAt: Long = System.currentTimeMillis()
)

/**
 * User preferences for recommendations
 */
data class RecommendationPreferences(
    val userId: String,
    val preferredJobTypes: List<EmploymentType> = emptyList(),
    val preferredWorkTypes: List<WorkType> = emptyList(),
    val preferredCompanySizes: List<CompanySize> = emptyList(),
    val preferredIndustries: List<String> = emptyList(),
    val preferredLocations: List<String> = emptyList(),
    val salaryExpectation: SalaryRange? = null,
    val experienceLevel: ExperienceLevel? = null,
    val isRemotePreferred: Boolean = false,
    val notificationFrequency: NotificationFrequency = NotificationFrequency.DAILY,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Notification frequency for recommendations
 */
enum class NotificationFrequency {
    IMMEDIATE,
    DAILY,
    WEEKLY,
    MONTHLY,
    NEVER
}

/**
 * Job analytics
 */
data class JobAnalytics(
    val jobId: String,
    val viewCount: Int = 0,
    val applicationCount: Int = 0,
    val saveCount: Int = 0,
    val shareCount: Int = 0,
    val averageViewDuration: Long = 0,
    val lastViewed: Long? = null,
    val trendingScore: Double = 0.0
)

/**
 * User behavior tracking
 */
data class UserBehavior(
    val userId: String,
    var jobViews: List<JobView> = emptyList(),
    val applications: List<JobApplication> = emptyList(),
    val savedJobs: List<String> = emptyList(),
    val searchHistory: List<SearchQuery> = emptyList(),
    var lastActive: Long = System.currentTimeMillis()
)

/**
 * Job view tracking
 */
data class JobView(
    val jobId: String,
    val viewedAt: Long = System.currentTimeMillis(),
    val duration: Long = 0,
    val source: ViewSource = ViewSource.SEARCH
)

/**
 * View sources
 */
enum class ViewSource {
    SEARCH,
    RECOMMENDATIONS,
    SAVED_JOBS,
    NOTIFICATIONS,
    SHARED_LINK,
    DIRECT
}

/**
 * Job application tracking
 */
data class JobApplication(
    val jobId: String,
    val appliedAt: Long = System.currentTimeMillis(),
    val status: ApplicationStatus = ApplicationStatus.SUBMITTED,
    val source: ViewSource = ViewSource.SEARCH
)

/**
 * Search query tracking
 */
data class SearchQuery(
    val query: String,
    val filters: JobFilters,
    val resultCount: Int,
    val searchedAt: Long = System.currentTimeMillis()
)

/**
 * Extension functions for ExperienceLevel
 */
fun ExperienceLevel.getDisplayName(): String {
    return when (this) {
        ExperienceLevel.ENTRY_LEVEL -> "Entry Level (0-2 years)"
        ExperienceLevel.MID_LEVEL -> "Mid Level (3-5 years)"
        ExperienceLevel.SENIOR_LEVEL -> "Senior Level (6+ years)"
        ExperienceLevel.EXECUTIVE_LEVEL -> "Executive Level (10+ years)"
    }
}

fun ExperienceLevel.getMinYears(): Int {
    return when (this) {
        ExperienceLevel.ENTRY_LEVEL -> 0
        ExperienceLevel.MID_LEVEL -> 3
        ExperienceLevel.SENIOR_LEVEL -> 6
        ExperienceLevel.EXECUTIVE_LEVEL -> 10
    }
}

/**
 * Extension functions for CompanySize
 */
fun CompanySize.getDisplayName(): String {
    return when (this) {
        CompanySize.STARTUP -> "Startup (1-10 employees)"
        CompanySize.SMALL -> "Small (11-50 employees)"
        CompanySize.MEDIUM -> "Medium (51-200 employees)"
        CompanySize.LARGE -> "Large (201-1000 employees)"
        CompanySize.ENTERPRISE -> "Enterprise (1000+ employees)"
    }
}

fun CompanySize.getMinEmployees(): Int {
    return when (this) {
        CompanySize.STARTUP -> 1
        CompanySize.SMALL -> 11
        CompanySize.MEDIUM -> 51
        CompanySize.LARGE -> 201
        CompanySize.ENTERPRISE -> 1000
    }
}

/**
 * Extension functions for PostedWithin
 */
fun PostedWithin.getDisplayName(): String {
    return when (this) {
        PostedWithin.TODAY -> "Today"
        PostedWithin.LAST_3_DAYS -> "Last 3 days"
        PostedWithin.LAST_WEEK -> "Last week"
        PostedWithin.LAST_2_WEEKS -> "Last 2 weeks"
        PostedWithin.LAST_MONTH -> "Last month"
    }
}

fun PostedWithin.getDaysAgo(): Int {
    return when (this) {
        PostedWithin.TODAY -> 0
        PostedWithin.LAST_3_DAYS -> 3
        PostedWithin.LAST_WEEK -> 7
        PostedWithin.LAST_2_WEEKS -> 14
        PostedWithin.LAST_MONTH -> 30
    }
}

/**
 * Extension functions for RecommendationType
 */
fun RecommendationType.getDisplayName(): String {
    return when (this) {
        RecommendationType.SKILL_MATCH -> "Matches your skills"
        RecommendationType.LOCATION_BASED -> "Near your location"
        RecommendationType.RECENTLY_VIEWED -> "Similar to viewed jobs"
        RecommendationType.SIMILAR_JOBS -> "Similar jobs"
        RecommendationType.TRENDING -> "Trending in your area"
        RecommendationType.SALARY_MATCH -> "Matches your salary expectations"
        RecommendationType.EXPERIENCE_MATCH -> "Matches your experience level"
        RecommendationType.COMPANY_MATCH -> "From preferred companies"
    }
}

fun RecommendationType.getIcon(): String {
    return when (this) {
        RecommendationType.SKILL_MATCH -> "🎯"
        RecommendationType.LOCATION_BASED -> "📍"
        RecommendationType.RECENTLY_VIEWED -> "👁️"
        RecommendationType.SIMILAR_JOBS -> "🔄"
        RecommendationType.TRENDING -> "📈"
        RecommendationType.SALARY_MATCH -> "💰"
        RecommendationType.EXPERIENCE_MATCH -> "⭐"
        RecommendationType.COMPANY_MATCH -> "🏢"
    }
}

/**
 * Saved search model
 */
data class SavedSearch(
    val id: String,
    val userId: String,
    val name: String,
    val query: String,
    val filters: JobFilters,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notificationEnabled: Boolean = true
)

/**
 * Saved search result
 */
data class SavedSearchResult(
    val search: SavedSearch?,
    val results: List<JobCardModel>,
    val newJobsCount: Int,
    val error: String?
)

/**
 * Search notification
 */
data class SearchNotification(
    val id: String,
    val searchId: String,
    val userId: String,
    val message: String,
    val newJobsCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

/**
 * Smart search with filters result
 */
data class SmartSearchWithFiltersResult(
    val searchResult: List<JobCardModel>,
    val filteredJobs: List<JobCardModel>,
    val relatedSearches: List<SearchSuggestion>,
    val totalResults: Int
)
