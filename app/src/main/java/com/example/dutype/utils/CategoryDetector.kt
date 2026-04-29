package com.example.dutype.utils

/**
 * CategoryDetector - Automatically detects job category from title and description
 * Uses keyword matching to classify jobs into predefined categories
 */
object CategoryDetector {

    /**
     * Detect category from job title and description
     * Returns the most relevant category or "Other" if no match found
     */
    fun detectCategory(title: String, description: String): String {
        return JobCategoryResolver.inferCategory(title, description)?.displayName ?: "Other"
    }
    
    /**
     * Get all available categories
     */
    fun getAllCategories(): List<String> {
        return com.example.dutype.employer.models.JobCategory.entries.map { it.displayName }
    }
    
    /**
     * Check if a category is valid
     */
    fun isValidCategory(category: String): Boolean {
        return JobCategoryResolver.enumNameForDisplay(category) != null || category.equals("Other", ignoreCase = true)
    }
}
