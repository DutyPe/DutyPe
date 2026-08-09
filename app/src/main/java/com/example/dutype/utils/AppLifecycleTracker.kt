package com.example.dutype.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import timber.log.Timber

object AppLifecycleTracker {
    private var startedActivityCount = 0

    fun isAppInForeground(): Boolean {
        return startedActivityCount > 0
    }

    val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            Timber.d("AppLifecycleTracker: onActivityCreated: %s", activity::class.java.simpleName)
        }
        
        override fun onActivityStarted(activity: Activity) {
            startedActivityCount++
            Timber.d("AppLifecycleTracker: onActivityStarted: %s, count = %d", activity::class.java.simpleName, startedActivityCount)
        }
        
        override fun onActivityResumed(activity: Activity) {}
        
        override fun onActivityPaused(activity: Activity) {}
        
        override fun onActivityStopped(activity: Activity) {
            if (startedActivityCount > 0) {
                startedActivityCount--
            }
            Timber.d("AppLifecycleTracker: onActivityStopped: %s, count = %d", activity::class.java.simpleName, startedActivityCount)
        }
        
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        
        override fun onActivityDestroyed(activity: Activity) {
            Timber.d("AppLifecycleTracker: onActivityDestroyed: %s", activity::class.java.simpleName)
        }
    }
}
