package com.dutype.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P1-1: Generates a Baseline Profile that covers cold-start through the
 * first usable frame of [com.dutype.app] for both worker and employer roles.
 *
 * Execution:
 * ```
 * ./gradlew :app:generateBaselineProfile
 * ```
 *
 * Output: `app/src/<variant>/generated/baselineProfiles/baseline-prof.txt`,
 * which the `androidx.baselineprofile` plugin auto-bundles into the release AAB.
 *
 * Coverage scope:
 * - App process bring-up
 * - Splash screen + first composition of `MainNavGraph`
 * - First scroll of the home screen list (helps Compose pre-compile its
 *   recycling code paths)
 *
 * The journey is intentionally minimal — Baseline Profiles benefit from
 * focusing on the hot path rather than exercising the whole app.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()

            // Wait for first usable frame. Adjust selector to a stable
            // resource id once the home screen exposes one.
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), 5_000)

            // Scroll the home list a little to capture list rendering paths.
            runCatching {
                val scrollable = device.findObject(By.scrollable(true))
                scrollable?.fling(androidx.test.uiautomator.Direction.DOWN)
                device.waitForIdle()
            }
        }
    }

    private companion object {
        const val PACKAGE_NAME = "com.dutype.app"
    }
}
