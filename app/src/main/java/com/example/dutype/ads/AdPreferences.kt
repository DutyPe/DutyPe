package com.example.dutype.ads

import android.content.Context
import android.content.SharedPreferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdPreferences - Tracks ad-related state like contact unlocks
 * 
 * Employer gets 3 contact unlocks per rewarded ad watched
 */
@Singleton
class AdPreferences @Inject constructor() {
    
    companion object {
        private const val PREFS_NAME = "ad_preferences"
        private const val KEY_CONTACT_UNLOCKS_REMAINING = "contact_unlocks_remaining"
        private const val KEY_TOTAL_ADS_WATCHED = "total_ads_watched"
        private const val KEY_UNLOCKED_CONTACTS = "unlocked_contacts" // Comma-separated applicationIds
        private const val KEY_WORKER_JOB_VIEWS = "worker_job_views" // Comma-separated jobIds viewed today
        private const val KEY_LAST_VIEW_DATE = "last_view_date"
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // ==================== EMPLOYER CONTACT UNLOCKS ====================
    
    /**
     * Get remaining contact unlocks
     */
    fun getContactUnlocksRemaining(context: Context): Int {
        return getPrefs(context).getInt(KEY_CONTACT_UNLOCKS_REMAINING, 0)
    }
    
    /**
     * Add contact unlocks after watching rewarded ad
     */
    fun addContactUnlocks(context: Context, count: Int = AdManager.CONTACTS_PER_AD) {
        val current = getContactUnlocksRemaining(context)
        getPrefs(context).edit()
            .putInt(KEY_CONTACT_UNLOCKS_REMAINING, current + count)
            .apply()
        Timber.d("📺 Added $count contact unlocks. Total: ${current + count}")
    }
    
    /**
     * Use one contact unlock
     * @return true if unlock was successful, false if no unlocks remaining
     */
    fun useContactUnlock(context: Context, applicationId: String): Boolean {
        val remaining = getContactUnlocksRemaining(context)
        if (remaining <= 0) {
            Timber.d("📺 No contact unlocks remaining")
            return false
        }
        
        // Decrement unlocks
        getPrefs(context).edit()
            .putInt(KEY_CONTACT_UNLOCKS_REMAINING, remaining - 1)
            .apply()
        
        // Track unlocked contact
        markContactUnlocked(context, applicationId)
        
        Timber.d("📺 Used 1 contact unlock for $applicationId. Remaining: ${remaining - 1}")
        return true
    }
    
    /**
     * Check if a specific contact is already unlocked
     */
    fun isContactUnlocked(context: Context, applicationId: String): Boolean {
        val unlocked = getPrefs(context).getString(KEY_UNLOCKED_CONTACTS, "") ?: ""
        return unlocked.split(",").contains(applicationId)
    }
    
    /**
     * Mark a contact as unlocked
     */
    private fun markContactUnlocked(context: Context, applicationId: String) {
        val unlocked = getPrefs(context).getString(KEY_UNLOCKED_CONTACTS, "") ?: ""
        val unlockedSet = unlocked.split(",").filter { it.isNotBlank() }.toMutableSet()
        unlockedSet.add(applicationId)
        getPrefs(context).edit()
            .putString(KEY_UNLOCKED_CONTACTS, unlockedSet.joinToString(","))
            .apply()
    }
    
    /**
     * Increment total ads watched counter
     */
    fun incrementAdsWatched(context: Context) {
        val current = getPrefs(context).getInt(KEY_TOTAL_ADS_WATCHED, 0)
        getPrefs(context).edit()
            .putInt(KEY_TOTAL_ADS_WATCHED, current + 1)
            .apply()
    }
    
    // ==================== WORKER JOB VIEWS ====================
    
    /**
     * Check if worker has already viewed this job today (no ad needed)
     */
    fun hasViewedJobToday(context: Context, jobId: String): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastDate = getPrefs(context).getString(KEY_LAST_VIEW_DATE, "")
        
        // Reset views if it's a new day
        if (lastDate != today) {
            getPrefs(context).edit()
                .putString(KEY_WORKER_JOB_VIEWS, "")
                .putString(KEY_LAST_VIEW_DATE, today)
                .apply()
            return false
        }
        
        val viewedJobs = getPrefs(context).getString(KEY_WORKER_JOB_VIEWS, "") ?: ""
        return viewedJobs.split(",").contains(jobId)
    }
    
    /**
     * Mark job as viewed today
     */
    fun markJobViewed(context: Context, jobId: String) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastDate = getPrefs(context).getString(KEY_LAST_VIEW_DATE, "")
        
        // Reset if new day
        val currentViews = if (lastDate != today) {
            getPrefs(context).edit().putString(KEY_LAST_VIEW_DATE, today).apply()
            ""
        } else {
            getPrefs(context).getString(KEY_WORKER_JOB_VIEWS, "") ?: ""
        }
        
        val viewedSet = currentViews.split(",").filter { it.isNotBlank() }.toMutableSet()
        viewedSet.add(jobId)
        
        getPrefs(context).edit()
            .putString(KEY_WORKER_JOB_VIEWS, viewedSet.joinToString(","))
            .apply()
        
        Timber.d("📺 Marked job $jobId as viewed")
    }
    
    /**
     * Get count of jobs viewed today
     */
    fun getJobsViewedTodayCount(context: Context): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val lastDate = getPrefs(context).getString(KEY_LAST_VIEW_DATE, "")
        
        if (lastDate != today) return 0
        
        val viewedJobs = getPrefs(context).getString(KEY_WORKER_JOB_VIEWS, "") ?: ""
        return viewedJobs.split(",").filter { it.isNotBlank() }.size
    }
}
