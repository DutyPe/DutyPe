package com.example.dutype

import com.example.dutype.di.AIModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AIConfigurationTest {
    @Test
    fun `release URLs require HTTPS and reject missing or credential-bearing configuration`() {
        for (url in listOf("", "not a URL", "http://10.0.2.2:8000/", "https://user:secret@example.invalid/", "https://example.invalid/?token=value")) {
            assertNull(AIModule.configuredUrl(url, false))
        }
        assertEquals("https://example.invalid/api/", AIModule.configuredUrl("https://example.invalid/api", false).toString())
    }

    @Test
    fun `debug configuration can target the local emulator`() {
        assertNotNull(AIModule.configuredUrl("http://10.0.2.2:8000/", true))
    }
}