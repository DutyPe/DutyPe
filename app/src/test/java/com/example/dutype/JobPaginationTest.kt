package com.example.dutype

import com.example.dutype.services.firestore.fillVisibleJobPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class JobPaginationTest {
    @Test
    fun `filtered pages are skipped without hiding older visible jobs`() = runBlocking {
        val documents = (1..12).toList()
        val requested = mutableListOf<Int>()
        val jobs = fillVisibleJobPage(
            limit = 3,
            fetch = { count, cursor: Int? ->
                requested.add(count)
                documents.dropWhile { it <= (cursor ?: 0) }.take(count)
            },
            visible = { it >= 8 }
        )
        assertEquals(listOf(8, 9, 10), jobs)
        assertEquals(listOf(3, 3, 3, 1), requested)
    }

    @Test
    fun `short visible page is returned only after raw query exhaustion`() = runBlocking {
        val jobs = fillVisibleJobPage(
            limit = 3,
            fetch = { count, cursor: Int? -> (1..5).filter { it > (cursor ?: 0) }.take(count) },
            visible = { it == 4 }
        )
        assertEquals(listOf(4), jobs)
    }

    @Test
    fun `empty collection returns an empty page`() = runBlocking {
        assertEquals(emptyList<Int>(), fillVisibleJobPage<Int>(3, { _, _ -> emptyList() }, { true }))
    }
}