package com.example.dutype

import com.example.dutype.services.WithdrawalRequestJournal
import com.example.dutype.services.withdrawalRequestFingerprint
import org.junit.Assert.*
import org.junit.Test

class WithdrawalRequestTest {
    @Test
    fun `uncertain requests reuse their persisted ID after recreation`() {
        val persisted = mutableMapOf<String, String>()
        fun journal() = WithdrawalRequestJournal(persisted::get) { key, value ->
            if (value == null) persisted.remove(key) else persisted[key] = value
            true
        }
        val first = journal().begin("fingerprint")
        assertEquals(first, journal().begin("fingerprint"))
        assertTrue(journal().complete("fingerprint"))
        assertNotEquals(first, journal().begin("fingerprint"))
    }

    @Test
    fun `failed persistence prevents a request from being issued`() {
        val journal = WithdrawalRequestJournal({ null }, { _, _ -> false })
        assertThrows(IllegalStateException::class.java) { journal.begin("fingerprint") }
    }

    @Test
    fun `account amount and destination are part of the request identity`() {
        val details = linkedMapOf<String, Any?>("amount" to 100.0, "upiId" to "test@invalid")
        val fingerprint = withdrawalRequestFingerprint("alice", details)
        assertEquals(fingerprint, withdrawalRequestFingerprint("alice", details))
        assertNotEquals(fingerprint, withdrawalRequestFingerprint("bob", details))
        assertNotEquals(fingerprint, withdrawalRequestFingerprint("alice", details + ("amount" to 200.0)))
        assertNotEquals(fingerprint, withdrawalRequestFingerprint("alice", details + ("upiId" to "other@invalid")))
        assertTrue(fingerprint.matches(Regex("[0-9a-f]{64}")))
    }
}