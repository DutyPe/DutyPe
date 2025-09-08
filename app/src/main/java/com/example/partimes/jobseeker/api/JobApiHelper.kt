package com.example.partimes.jobseeker.api

import android.util.Log
import com.example.partimes.apis.RetrofitClient
import com.example.partimes.models.JobListing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun postJobToBackend(job: JobListing, onSuccess: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.postJob(job).execute()
            if (response.isSuccessful) {
                withContext(Dispatchers.Main) { onSuccess() }
            } else {
                Log.e("PostJob", "Failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("PostJob", "Error posting job", e)
        }
    }
}

fun getJobById(jobId: String, onResult: (JobListing?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.getJobById(jobId).execute()
            val job = if (response.isSuccessful) response.body() else null
            withContext(Dispatchers.Main) { onResult(job) }
        } catch (e: Exception) {
            Log.e("GetJob", "Error fetching job", e)
            withContext(Dispatchers.Main) { onResult(null) }
        }
    }
}
suspend fun fetchAllJobs(): List<JobListing> {
    return try {
        RetrofitClient.apiService.getAllJobs()
    } catch (e: Exception) {
        Log.e("FetchJobs", "Error fetching jobs", e)
        emptyList()
    }
}
