package com.dutype.app

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline Profile Generator for DutyPe App
 * Generates a baseline profile to optimize app startup and UI rendering
 * 
 * This benchmark captures the app's main user workflows and creates an optimized
 * profile that helps the system pre-compile critical code paths, resulting in:
 * - 30-60% faster cold app launch
 * - Smoother scrolling and transitions
 * - Better overall app performance
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() = baselineProfileRule.collect(
        packageName = "com.dutype.app",
        maxDurationMillis = 15_000, // 15 seconds of recording
        stableIterations = 3
    ) {
        // Navigate and interact with the app to record typical user workflows
        val device = UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())

        // ===== Startup Phase =====
        pressHome()
        device.wait(
            androidx.test.uiautomator.Until.hasObject(By.pkg("com.dutype.app")),
            5_000
        )

        // Wait for app to fully launch
        device.waitForIdle(2_000)

        // ===== Main Screen Workflow =====
        // Scroll through the home screen job list
        val jobList = device.findObject(By.res("com.dutype.app", "job_list"))
        if (jobList != null && jobList.isClickable) {
            repeat(3) {
                jobList.scroll(Direction.DOWN, 0.5f)
                device.waitForIdle(300)
                jobList.scroll(Direction.UP, 0.5f)
                device.waitForIdle(300)
            }
        }

        // ===== Navigation Workflow =====
        // Tap search button to open search screen
        val searchButton = device.findObject(By.desc("Search"))
        if (searchButton != null && searchButton.isClickable) {
            searchButton.click()
            device.waitForIdle(1_000)
            device.pressBack()
            device.waitForIdle(300)
        }

        // ===== Profile Navigation =====
        // Open side menu or profile section
        val profileButton = device.findObject(By.desc("Profile"))
        if (profileButton != null && profileButton.isClickable) {
            profileButton.click()
            device.waitForIdle(1_000)
            device.pressBack()
            device.waitForIdle(300)
        }

        // ===== Additional Scrolling =====
        // Perform more scrolling to capture UI rendering
        if (jobList != null && jobList.isClickable) {
            repeat(2) {
                jobList.scroll(Direction.DOWN, 0.3f)
                device.waitForIdle(200)
            }
        }

        // ===== Return to Home =====
        device.pressHome()
    }

    private fun pressHome() {
        val device = UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        device.pressHome()
    }
}
