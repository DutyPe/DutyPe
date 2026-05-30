package com.example.dutype

import com.example.dutype.utils.JobSearchMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobSearchMatcherTest {
    @Test
    fun `matches jobs by title and location together`() {
        val tokens = JobSearchMatcher.queryTokens("sales jobs tirupati")

        val match = JobSearchMatcher.evaluate(
            query = "sales jobs tirupati",
            tokens = tokens,
            fields = JobSearchMatcher.Fields(
                title = "Sales Executive",
                companyName = "Sri Balaji Stores",
                companyCity = "Tirupati",
                addressText = "Renigunta Road, Tirupati"
            )
        )

        assertTrue(match.matches)
    }

    @Test
    fun `does not match title-only jobs when location token is missing`() {
        val tokens = JobSearchMatcher.queryTokens("sales jobs tirupati")

        val match = JobSearchMatcher.evaluate(
            query = "sales jobs tirupati",
            tokens = tokens,
            fields = JobSearchMatcher.Fields(
                title = "Sales Executive",
                companyName = "City Mart",
                companyCity = "Chennai",
                addressText = "T Nagar, Chennai"
            )
        )

        assertFalse(match.matches)
    }

    @Test
    fun `matches jobs by company name`() {
        val tokens = JobSearchMatcher.queryTokens("balaji stores")

        val match = JobSearchMatcher.evaluate(
            query = "balaji stores",
            tokens = tokens,
            fields = JobSearchMatcher.Fields(
                title = "Cashier",
                companyName = "Sri Balaji Stores",
                companyCity = "Srikalahasti"
            )
        )

        assertTrue(match.matches)
    }

    @Test
    fun `matches jobs by address text`() {
        val tokens = JobSearchMatcher.queryTokens("srikalahasti")

        val match = JobSearchMatcher.evaluate(
            query = "srikalahasti",
            tokens = tokens,
            fields = JobSearchMatcher.Fields(
                title = "Helper",
                companyName = "Local Hardware",
                addressText = "Bazaar Street, Srikalahasti, Andhra Pradesh"
            )
        )

        assertTrue(match.matches)
    }

    @Test
    fun `adds compact variants for multi word locations`() {
        val variants = JobSearchMatcher.locationQueryVariants(
            query = "Sri Kalahasti",
            tokens = JobSearchMatcher.queryTokens("Sri Kalahasti")
        )

        assertTrue(variants.contains("Sri Kalahasti"))
        assertTrue(variants.contains("Srikalahasti"))
    }

    @Test
    fun `ignores generic job words in useful searches`() {
        val tokens = JobSearchMatcher.queryTokens("delivery jobs nearby")

        assertTrue(tokens.contains("delivery"))
        assertFalse(tokens.contains("jobs"))
        assertFalse(tokens.contains("nearby"))
    }
}