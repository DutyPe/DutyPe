package com.example.dutype.utils

object JobSearchMatcher {
    private val stopWords = setOf(
        "job",
        "jobs",
        "work",
        "works",
        "vacancy",
        "vacancies",
        "opening",
        "openings",
        "hiring",
        "hire",
        "near",
        "nearby",
        "me",
        "in",
        "at",
        "for"
    )

    data class Fields(
        val title: String = "",
        val companyName: String = "",
        val companyCity: String = "",
        val addressText: String = "",
        val jobType: String = "",
        val category: String = "",
        val categoryLabel: String = ""
    )

    data class Result(
        val matches: Boolean,
        val score: Int
    )

    fun queryTokens(query: String): List<String> = JobCategoryResolver.searchQueryTokens(query)
        .map { it.trim().lowercase() }
        .filter { it.length >= 2 && it !in stopWords }
        .distinct()

    fun locationQueryVariants(query: String, tokens: List<String>): Set<String> {
        val values = linkedSetOf<String>()

        fun addVariants(value: String) {
            val raw = value.trim()
            if (raw.length < 2 || raw.lowercase() in stopWords) return
            val words = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
            val titleCased = words.joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { char -> char.titlecase() }
            }
            val compact = words.joinToString("")
            val compactTitleCased = compact.lowercase().replaceFirstChar { char -> char.titlecase() }

            values += raw
            values += titleCased
            values += compactTitleCased
            values += raw.lowercase()
            values += raw.uppercase()
            values += compact.lowercase()
            values += compact.uppercase()
        }

        tokens.take(5).forEach(::addVariants)
        if (query.split(Regex("\\s+")).size <= 2) {
            addVariants(query)
        }

        return values.take(10).toSet()
    }

    fun evaluate(query: String, tokens: List<String>, fields: Fields): Result {
        val normalizedQuery = query.trim().lowercase()
        val meaningfulTokens = if (tokens.isNotEmpty()) tokens else queryTokens(query)
        if (normalizedQuery.length < 2 || meaningfulTokens.isEmpty()) return Result(matches = false, score = 0)

        val title = fields.title.lowercase()
        val companyName = fields.companyName.lowercase()
        val companyCity = fields.companyCity.lowercase()
        val addressText = fields.addressText.lowercase()
        val jobType = fields.jobType.lowercase()
        val category = fields.category.lowercase()
        val categoryLabel = fields.categoryLabel.lowercase()
        val searchableText = listOf(title, companyName, companyCity, addressText, jobType, category, categoryLabel)
            .joinToString(" ")

        val allTokensMatch = meaningfulTokens.all { token -> searchableText.contains(token) }
        if (!allTokensMatch) return Result(matches = false, score = 0)

        val score = when {
            title.startsWith(normalizedQuery) -> 140
            title.contains(normalizedQuery) -> 130
            meaningfulTokens.all { title.contains(it) } -> 120
            companyName.contains(normalizedQuery) -> 115
            meaningfulTokens.all { companyName.contains(it) } -> 110
            companyCity.contains(normalizedQuery) -> 105
            addressText.contains(normalizedQuery) -> 100
            meaningfulTokens.any { category.contains(it) || categoryLabel.contains(it) } -> 90
            meaningfulTokens.any { jobType.contains(it) } -> 80
            else -> 60 + meaningfulTokens.count { searchableText.contains(it) }
        }

        return Result(matches = true, score = score)
    }
}