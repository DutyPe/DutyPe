package com.example.dutype

import com.example.dutype.jobs.searchWords
import com.example.dutype.utils.JobCategoryResolver
import org.junit.Assert.assertEquals
import org.junit.Test

/** The app must split search text exactly like the server's keywordsOf (functions/src/lib/keywords.ts). */
class SearchWordsTest {
    @Test
    fun matchesServerSplitting() {
        assertEquals(listOf("night", "security", "guard"), searchWords("Night  Security-Guard"))
        assertEquals(listOf("వంట", "మనిషి"), searchWords("వంట మనిషి"))
        assertEquals(listOf("रसोइया"), searchWords("रसोइया!"))
        assertEquals(listOf("cd"), searchWords("a b cd"))
    }

    /** A trade typed in Telugu or Hindi searches the same category as the English word. */
    @Test
    fun localTradeWordsFindTheCategory() {
        assertEquals("COOK", JobCategoryResolver.inferCategory("వంట మనిషి")?.name)
        assertEquals("DRIVER", JobCategoryResolver.inferCategory("డ్రైవర్")?.name)
        assertEquals("SECURITY", JobCategoryResolver.inferCategory("चौकीदार")?.name)
        assertEquals("MAID", JobCategoryResolver.inferCategory("పనిమనిషి కావాలి")?.name)
        assertEquals("DRIVER", JobCategoryResolver.inferCategory("driver")?.name)
    }
}
