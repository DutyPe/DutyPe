package com.example.dutype.utils

/**
 * Smart Search Processor - Intelligent query parsing for job search
 * 
 * Handles natural language queries like:
 * - "delivery jobs in Khammam"
 * - "bike driving jobs"
 * - "cook needed"
 * - "waiter job near me"
 * 
 * Extracts keywords and synonyms for better matching
 */
object SmartSearchProcessor {
    
    // Job type synonyms and variations
    private val jobSynonyms = mapOf(
        "delivery" to listOf("delivery", "deliver", "courier", "parcel", "package", "food delivery", "zomato", "swiggy", "dunzo"),
        "driver" to listOf("driver", "driving", "drive", "bike", "car", "vehicle", "rider", "biker"),
        "cook" to listOf("cook", "cooking", "chef", "kitchen", "food", "restaurant"),
        "waiter" to listOf("waiter", "server", "serving", "restaurant", "hotel", "cafe"),
        "cleaner" to listOf("cleaner", "cleaning", "housekeeping", "maid", "sweeper", "janitor"),
        "security" to listOf("security", "guard", "watchman", "safety"),
        "helper" to listOf("helper", "assistant", "support", "aide"),
        "packer" to listOf("packer", "packing", "packaging", "warehouse"),
        "sales" to listOf("sales", "selling", "salesman", "marketing"),
        "cashier" to listOf("cashier", "billing", "counter", "cash")
    )
    
    // Common stop words to remove
    private val stopWords = setOf(
        "job", "jobs", "work", "needed", "required", "wanted", "looking", "for",
        "in", "at", "near", "me", "the", "a", "an", "and", "or", "to", "from"
    )
    
    /**
     * Process natural language query into search keywords
     * Returns list of keywords to search for
     */
    fun processQuery(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        
        val keywords = mutableSetOf<String>()
        val words = query.lowercase().split(Regex("\\s+"))
        
        // Extract meaningful words (remove stop words)
        val meaningfulWords = words.filter { word ->
            word.length > 2 && !stopWords.contains(word)
        }
        
        // Add original meaningful words
        keywords.addAll(meaningfulWords)
        
        // Add synonyms for known job types
        meaningfulWords.forEach { word ->
            jobSynonyms.forEach { (key, synonyms) ->
                if (synonyms.any { it.contains(word) || word.contains(it) }) {
                    keywords.add(key)
                    keywords.addAll(synonyms.take(3)) // Add top 3 synonyms
                }
            }
        }
        
        return keywords.toList()
    }
    
    /**
     * Check if job matches search query using intelligent matching
     */
    fun matchesQuery(
        query: String,
        jobTitle: String,
        jobCategory: String,
        jobLocation: String,
        companyName: String,
        requirements: List<String> = emptyList()
    ): Boolean {
        if (query.isBlank()) return true
        
        val keywords = processQuery(query)
        if (keywords.isEmpty()) return true
        
        // Combine all searchable text
        val searchableText = buildString {
            append(jobTitle.lowercase())
            append(" ")
            append(jobCategory.lowercase())
            append(" ")
            append(jobLocation.lowercase())
            append(" ")
            append(companyName.lowercase())
            append(" ")
            append(requirements.joinToString(" ").lowercase())
        }
        
        // Match if ANY keyword is found (OR logic for better results)
        return keywords.any { keyword ->
            searchableText.contains(keyword, ignoreCase = true)
        }
    }
    
    /**
     * Calculate relevance score for ranking results
     * Higher score = more relevant
     */
    fun calculateRelevanceScore(
        query: String,
        jobTitle: String,
        jobCategory: String,
        jobLocation: String
    ): Int {
        if (query.isBlank()) return 0
        
        val keywords = processQuery(query)
        var score = 0
        
        keywords.forEach { keyword ->
            // Title match = highest priority
            if (jobTitle.contains(keyword, ignoreCase = true)) {
                score += 10
            }
            // Category match = high priority
            if (jobCategory.contains(keyword, ignoreCase = true)) {
                score += 7
            }
            // Location match = medium priority
            if (jobLocation.contains(keyword, ignoreCase = true)) {
                score += 5
            }
        }
        
        return score
    }
}
