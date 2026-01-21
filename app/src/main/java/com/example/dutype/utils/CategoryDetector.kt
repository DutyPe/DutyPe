package com.example.dutype.utils

/**
 * CategoryDetector - Automatically detects job category from title and description
 * Uses keyword matching to classify jobs into predefined categories
 */
object CategoryDetector {
    
    private val categoryKeywords = mapOf(
        "Cook" to listOf("cook", "chef", "kitchen", "food", "restaurant", "hotel", "catering", "culinary"),
        "Delivery" to listOf("delivery", "driver", "courier", "logistics", "transport", "bike", "vehicle"),
        "Housekeeping" to listOf("housekeeping", "cleaning", "maid", "janitor", "sweeper", "cleaner", "domestic"),
        "Security" to listOf("security", "guard", "watchman", "bouncer", "safety"),
        "Sales" to listOf("sales", "marketing", "retail", "shop", "store", "counter"),
        "Waiter" to listOf("waiter", "server", "steward", "bartender", "service"),
        "Helper" to listOf("helper", "assistant", "support", "aide"),
        "Packer" to listOf("packer", "packaging", "warehouse", "loading", "unloading"),
        "Electrician" to listOf("electrician", "electrical", "wiring", "repair"),
        "Plumber" to listOf("plumber", "plumbing", "pipe", "water"),
        "Carpenter" to listOf("carpenter", "wood", "furniture", "carpentry"),
        "Painter" to listOf("painter", "painting", "wall", "decoration"),
        "Mechanic" to listOf("mechanic", "auto", "vehicle", "repair", "garage"),
        "Tailor" to listOf("tailor", "stitching", "sewing", "garment", "alteration"),
        "Beautician" to listOf("beautician", "beauty", "salon", "makeup", "hair", "spa"),
        "Receptionist" to listOf("receptionist", "front desk", "reception", "office"),
        "Teacher" to listOf("teacher", "tutor", "education", "teaching", "trainer", "instructor"),
        "Nurse" to listOf("nurse", "nursing", "medical", "healthcare", "patient care", "caregiver"),
        "Babysitter" to listOf("babysitter", "nanny", "childcare", "kids", "children"),
        "Gardener" to listOf("gardener", "gardening", "landscaping", "plants", "lawn"),
        "Driver" to listOf("driver", "driving", "cab", "taxi", "chauffeur"),
        "Construction" to listOf("construction", "labor", "mason", "building", "site", "contractor")
    )
    
    /**
     * Detect category from job title and description
     * Returns the most relevant category or "Other" if no match found
     */
    fun detectCategory(title: String, description: String): String {
        val text = "$title $description".lowercase()
        
        // Find all matching categories with their match count
        val matches = categoryKeywords.mapNotNull { (category, keywords) ->
            val matchCount = keywords.count { keyword -> text.contains(keyword) }
            if (matchCount > 0) category to matchCount else null
        }
        
        // Return category with highest match count, or "Other" if no matches
        return matches.maxByOrNull { it.second }?.first ?: "Other"
    }
    
    /**
     * Get all available categories
     */
    fun getAllCategories(): List<String> {
        return categoryKeywords.keys.toList() + "Other"
    }
    
    /**
     * Check if a category is valid
     */
    fun isValidCategory(category: String): Boolean {
        return category in categoryKeywords.keys || category == "Other"
    }
}
