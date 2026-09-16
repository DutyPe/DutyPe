package com.example.dutype

import com.example.dutype.models.shouldDisplayPush
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {
    @Test
    fun `private messages are shown only to the matching signed in account`() {
        assertTrue(shouldDisplayPush("alice", "alice", false))
        assertFalse(shouldDisplayPush(null, "alice", false))
        assertFalse(shouldDisplayPush("bob", "alice", false))
        assertFalse(shouldDisplayPush("", "alice", false))
        assertFalse(shouldDisplayPush("alice", null, false))
        assertFalse(shouldDisplayPush("alice", "", false))
    }

    @Test
    fun `public topic messages work for guests but cannot bypass recipient checks`() {
        assertTrue(shouldDisplayPush(null, null, true))
        assertTrue(shouldDisplayPush("alice", null, true))
        assertFalse(shouldDisplayPush("bob", "alice", true))
        assertFalse(shouldDisplayPush(null, "alice", true))
    }
}