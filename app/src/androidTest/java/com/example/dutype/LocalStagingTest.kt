package com.example.dutype

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import com.dutype.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalStagingTest {
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()
    private val functions get() = FirebaseFunctions.getInstance()

    @Before
    fun verifyLocalTarget() {
        assumeTrue(BuildConfig.LOCAL_STAGING)
        assertEquals("com.dutype.app.staging", InstrumentationRegistry.getInstrumentation().targetContext.packageName)
        assertEquals("demo-dutype-android-fixes", FirebaseApp.getInstance().options.projectId)
        assertEquals("10.0.2.2:8185", db.firestoreSettings.host)
        assertFalse(db.firestoreSettings.isSslEnabled)
        auth.signOut()
    }

    @After
    fun signOutLocalUser() {
        if (BuildConfig.LOCAL_STAGING) auth.signOut()
    }

    @Test
    fun guestLaunchRemainsInTheIsolatedStagingApp() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("com.dutype.app.staging", activity.packageName)
                assertFalse(activity.isFinishing)
                assertEquals("10.0.2.2:8185", db.firestoreSettings.host)
            }
        }
    }

    @Test
    fun publicProfilesAreReadableButPrivateProfilesAreNot() = runBlocking {
        withTimeout(30000) {
            val public = db.collection("public_profiles").document("device-worker").get(Source.SERVER).await()
            assertTrue(public.exists())
            assertEquals("Staging Worker", public.getString("fullName"))
            assertFalse(public.contains("phone"))
            val denied = try {
                db.collection("users").document("device-worker").get(Source.SERVER).await()
                false
            } catch (error: com.google.firebase.firestore.FirebaseFirestoreException) {
                error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            }
            assertTrue(denied)
        }
    }

    @Test
    fun authenticatedCallsSupportContactsAndRetrySafeWithdrawal() = runBlocking {
        withTimeout(60000) {
            auth.signInWithEmailAndPassword("device-worker@example.invalid", "local-emulator-only-password").await()
            assertEquals("device-worker", auth.currentUser?.uid)
            assertTrue(db.collection("users").document("device-worker").get(Source.SERVER).await().exists())
            val contact = functions.getHttpsCallable("getApplicationContact")
                .call(mapOf("applicationId" to "device-completed-work")).await().data as Map<*, *>
            assertEquals("device-employer", (contact["profile"] as Map<*, *>)["id"])
            assertNull((contact["profile"] as Map<*, *>)["fcmToken"])
            val request = mapOf("userId" to "device-worker", "requestId" to "device-withdrawal-request-0001",
                "amount" to 100, "paymentMethod" to "UPI", "upiId" to "staging@invalid")
            val first = functions.getHttpsCallable("requestWithdrawal").call(request).await().data as Map<*, *>
            val second = functions.getHttpsCallable("requestWithdrawal").call(request).await().data as Map<*, *>
            assertEquals(true, first["success"])
            assertEquals(first["withdrawalId"], second["withdrawalId"])
            val profile = db.collection("users").document("device-worker").get(Source.SERVER).await()
            assertEquals(400.0, (profile.get("referralStats.availableBalance") as Number).toDouble(), 0.001)
            auth.signOut()
            val denied = try {
                functions.getHttpsCallable("getApplicationContact").call(mapOf("applicationId" to "device-completed-work")).await()
                false
            } catch (error: com.google.firebase.functions.FirebaseFunctionsException) {
                error.code == com.google.firebase.functions.FirebaseFunctionsException.Code.UNAUTHENTICATED
            }
            assertTrue(denied)
        }
    }

    @Test
    fun serverRatingIsIdempotentAndOldDirectWritesAreDenied() = runBlocking {
        withTimeout(60000) {
            auth.signInWithEmailAndPassword("device-worker@example.invalid", "local-emulator-only-password").await()
            val request = mapOf("userId" to "device-worker", "applicationId" to "device-completed-work", "rating" to 4,
                "review" to "Local staging review", "tags" to listOf("Reliable"))
            val first = functions.getHttpsCallable("submitRating").call(request).await().data as Map<*, *>
            val second = functions.getHttpsCallable("submitRating").call(request).await().data as Map<*, *>
            assertEquals(first["ratingId"], second["ratingId"])
            val denied = try {
                db.collection("notifications").document("device-forged").set(mapOf("recipientId" to "device-employer", "title" to "Old client write")).await()
                false
            } catch (error: com.google.firebase.firestore.FirebaseFirestoreException) {
                error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
            }
            assertTrue(denied)
        }
    }
}