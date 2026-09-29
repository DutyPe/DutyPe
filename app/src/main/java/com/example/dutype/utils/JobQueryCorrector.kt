package com.example.dutype.utils

/**
 * Fixes misspelled job words in a search query, e.g. "diver" -> "driver",
 * "plumer" -> "plumber", "electricain" -> "electrician".
 *
 * Only words close to a known job / category word are touched; anything else
 * (locations, company names) is left exactly as typed.
 */
object JobQueryCorrector {

    private val vocabulary: Set<String> by lazy { JobCategoryResolver.searchVocabulary() }

    /** Returns the corrected query, or null when nothing needed fixing. */
    fun correct(query: String): String? {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null

        var changed = false
        val fixed = words.map { word ->
            val suggestion = correctWord(word.lowercase())
            if (suggestion != null) {
                changed = true
                suggestion
            } else {
                word
            }
        }
        return if (changed) fixed.joinToString(" ") else null
    }

    private fun correctWord(word: String): String? {
        // Short words, numbers and words still being typed (a prefix of a real word) stay as-is.
        if (word.length < 4 || word.any { it.isDigit() }) return null
        if (word in vocabulary || vocabulary.any { it.startsWith(word) }) return null

        val maxDistance = if (word.length <= 5) 1 else 2
        var best: String? = null
        var bestDistance = Int.MAX_VALUE
        for (candidate in vocabulary) {
            if (kotlin.math.abs(candidate.length - word.length) > maxDistance) continue
            val distance = editDistance(word, candidate, maxDistance)
            val better = distance < bestDistance ||
                // Tie: prefer the candidate that starts with the same letter ("diver" -> "driver").
                (distance == bestDistance && best != null && candidate[0] == word[0] && best[0] != word[0])
            if (better) {
                best = candidate
                bestDistance = distance
            }
        }
        return best?.takeIf { bestDistance in 1..maxDistance }
    }

    /**
     * Optimal-string-alignment distance (insert / delete / substitute / swap two adjacent
     * letters), stopping early once every value in a row exceeds [limit].
     */
    private fun editDistance(a: String, b: String, limit: Int): Int {
        val rows = a.length + 1
        val cols = b.length + 1
        val d = Array(rows) { IntArray(cols) }
        for (i in 0 until rows) d[i][0] = i
        for (j in 0 until cols) d[0][j] = j
        for (i in 1 until rows) {
            var rowMin = Int.MAX_VALUE
            for (j in 1 until cols) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var value = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    value = minOf(value, d[i - 2][j - 2] + 1)
                }
                d[i][j] = value
                rowMin = minOf(rowMin, value)
            }
            if (rowMin > limit) return limit + 1
        }
        return d[a.length][b.length]
    }
}
