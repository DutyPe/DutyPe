package com.example.dutype

import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.getDisplayName
import com.example.dutype.models.statusUpdateFields
import com.example.dutype.models.canBeAccepted
import com.example.dutype.models.occupiesVacancy
import com.example.dutype.models.canEmployerTransitionTo
import com.example.dutype.services.observeApplicationUpdates
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import com.google.firebase.firestore.util.CustomClassMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ApplicationWorkflowTest {
    @Test
    fun `employer transitions cannot rewind started work or reopen terminal applications`() {
        assertFalse(ApplicationStatus.IN_PROGRESS.canEmployerTransitionTo(ApplicationStatus.PENDING))
        assertFalse(ApplicationStatus.COMPLETED.canEmployerTransitionTo(ApplicationStatus.ACCEPTED))
        assertFalse(ApplicationStatus.WITHDRAWN.canEmployerTransitionTo(ApplicationStatus.ACCEPTED))
        assertEquals(true, ApplicationStatus.ACCEPTED.canEmployerTransitionTo(ApplicationStatus.REJECTED))
        assertEquals(true, ApplicationStatus.IN_PROGRESS.canEmployerTransitionTo(ApplicationStatus.COMPLETED))
    }

    @Test
    fun `application listener delivers subsequent updates and detaches on cancellation`() = runBlocking {
        withTimeout(5000L) {
            val subscribed = CompletableDeferred<(Result<List<JobApplication>>) -> Unit>()
            var removals = 0
            val flow = observeApplicationUpdates { publish ->
                subscribed.complete(publish)
                val remove: () -> Unit = { removals += 1 }
                remove
            }
            val collected = async { flow.take(2).toList() }
            val publish = subscribed.await()
            publish(Result.success(listOf(JobApplication(status = ApplicationStatus.PENDING))))
            publish(Result.success(listOf(JobApplication(status = ApplicationStatus.IN_PROGRESS))))
            assertEquals(
                listOf(ApplicationStatus.PENDING, ApplicationStatus.IN_PROGRESS),
                collected.await().map { it.getOrThrow().single().status }
            )
            assertEquals(1, removals)
        }
    }

    @Test
    fun `application listener detaches after an authorization failure`() = runBlocking {
        withTimeout(5000L) {
            var removals = 0
            val results = observeApplicationUpdates { publish ->
                publish(Result.failure(IllegalStateException("Account changed")))
                val remove: () -> Unit = { removals += 1 }
                remove
            }.toList()
            assertEquals("Account changed", results.single().exceptionOrNull()?.message)
            assertEquals(1, removals)
        }
    }

    @Test
    fun `only pending and reviewed applications can consume a new vacancy`() {
        assertEquals(
            setOf(ApplicationStatus.PENDING, ApplicationStatus.UNDER_REVIEW),
            ApplicationStatus.entries.filter { it.canBeAccepted() }.toSet()
        )
        assertEquals(
            setOf(ApplicationStatus.ACCEPTED, ApplicationStatus.IN_PROGRESS, ApplicationStatus.COMPLETED),
            ApplicationStatus.entries.filter { it.occupiesVacancy() }.toSet()
        )
    }

    @Test
    fun `every persisted application status deserializes without falling back`() {
        for (status in ApplicationStatus.entries) {
            val application = CustomClassMapper.convertToCustomClass(
                mapOf("status" to status.name), JobApplication::class.java, null
            )
            assertEquals(status, application.status)
        }
    }

    @Test
    fun `started work retains its persisted status and display name`() {
        val application = CustomClassMapper.convertToCustomClass(
            mapOf("status" to "IN_PROGRESS"), JobApplication::class.java, null
        )
        assertEquals(ApplicationStatus.IN_PROGRESS, application.status)
        assertEquals("In Progress", application.status.getDisplayName())
    }

    @Test
    fun `status updates contain only workflow fields and preserve verification`() {
        val stored = mapOf(
            "status" to "ACCEPTED",
            "verification" to mapOf("verificationCode" to "DTP-AAAAAA"),
            "verificationStatus" to "PENDING",
            "workStartedAt" to 123L,
            "workerId" to "worker",
            "employerId" to "employer"
        )
        val application = CustomClassMapper.convertToCustomClass(
            stored, JobApplication::class.java, null
        ).copy(status = ApplicationStatus.COMPLETED, updatedAt = 456L)
        val changes = application.statusUpdateFields()
        assertEquals(setOf("status", "statusHistory", "updatedAt"), changes.keys)
        assertFalse(changes.containsKey("workerId"))
        assertFalse(changes.containsKey("employerId"))
        val updated = stored + changes
        assertEquals("COMPLETED", updated["status"])
        assertEquals(stored["verification"], updated["verification"])
        assertEquals(stored["verificationStatus"], updated["verificationStatus"])
        assertEquals(stored["workStartedAt"], updated["workStartedAt"])
    }
}