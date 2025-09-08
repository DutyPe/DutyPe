package com.example.partimes.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.partimes.jobseeker.models.JobCardModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing saved jobs with persistent storage
 * Uses SharedPreferences for simple local storage
 */
class SavedJobsRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("saved_jobs_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    private val _savedJobs = MutableStateFlow<List<JobCardModel>>(emptyList())
    val savedJobs: StateFlow<List<JobCardModel>> = _savedJobs.asStateFlow()

    private val _savedJobIds = MutableStateFlow<Set<String>>(emptySet())
    val savedJobIds: StateFlow<Set<String>> = _savedJobIds.asStateFlow()

    init {
        loadSavedJobs()
    }

    /**
     * Load saved jobs from SharedPreferences
     */
    private fun loadSavedJobs() {
        try {
            val savedJobsJson = sharedPreferences.getString(SAVED_JOBS_KEY, null)
            if (savedJobsJson != null) {
                val type = object : TypeToken<List<JobCardModel>>() {}.type
                val jobs: List<JobCardModel> = gson.fromJson(savedJobsJson, type)
                _savedJobs.value = jobs
                _savedJobIds.value = jobs.map { job: JobCardModel -> job.jobId }.toSet()
            }
        } catch (_: Exception) {
            _savedJobs.value = emptyList()
            _savedJobIds.value = emptySet()
        }
    }

    /**
     * Save a job to persistent storage
     */
    suspend fun saveJob(job: JobCardModel): Boolean {
        return try {
            val currentJobs: MutableList<JobCardModel> = _savedJobs.value.toMutableList()
            if (currentJobs.any { savedJob: JobCardModel -> savedJob.jobId == job.jobId }) {
                false
            } else {
                currentJobs.add(job)
                _savedJobs.value = currentJobs
                _savedJobIds.value = currentJobs.map { job: JobCardModel -> job.jobId }.toSet()
                persistSavedJobs(currentJobs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Remove a job from saved jobs
     */
    suspend fun unsaveJob(jobId: String): Boolean {
        return try {
            val currentJobs: MutableList<JobCardModel> = _savedJobs.value.toMutableList()
            val removedJob = currentJobs.removeAll { job: JobCardModel -> job.jobId == jobId }
            if (removedJob) {
                _savedJobs.value = currentJobs
                _savedJobIds.value = currentJobs.map { job: JobCardModel -> job.jobId }.toSet()
                persistSavedJobs(currentJobs)
            }
            removedJob
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if a job is saved
     */
    fun isJobSaved(jobId: String): Boolean {
        return _savedJobIds.value.contains(jobId)
    }

    /**
     * Get saved jobs by category
     */
    fun getSavedJobsByCategory(category: String): List<JobCardModel> {
        return _savedJobs.value.filter { job: JobCardModel ->
            job.tags.any { tag -> tag.text.lowercase().contains(category.lowercase()) }
        }
    }

    /**
     * Search saved jobs
     */
    fun searchSavedJobs(query: String): List<JobCardModel> {
        return _savedJobs.value.filter { job: JobCardModel ->
            job.title.contains(query, ignoreCase = true) ||
            job.employerName.contains(query, ignoreCase = true) ||
            job.location.getDisplayText().contains(query, ignoreCase = true) ||
            job.tags.any { tag -> tag.text.contains(query, ignoreCase = true) }
        }
    }

    /**
     * Clear all saved jobs
     */
    suspend fun clearAllSavedJobs() {
        _savedJobs.value = emptyList()
        _savedJobIds.value = emptySet()
        sharedPreferences.edit().remove(SAVED_JOBS_KEY).apply()
    }

    /**
     * Persist saved jobs to SharedPreferences
     */
    private fun persistSavedJobs(jobs: List<JobCardModel>) {
        try {
            val json = gson.toJson(jobs)
            sharedPreferences.edit().putString(SAVED_JOBS_KEY, json).apply()
        } catch (e: Exception) {
            // Handle error
        }
    }

    companion object {
        private const val SAVED_JOBS_KEY = "saved_jobs"
    }
}
