package com.example.partimes.data

import android.content.Context
import android.content.SharedPreferences
import com.example.partimes.worker.models.PersonalInfo
import com.example.partimes.worker.models.WorkExperience
import com.example.partimes.worker.models.Document
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data store for application form data using SharedPreferences
 * Provides persistent storage for user's application form data
 */
@Singleton
class ApplicationFormDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("application_form_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_PERSONAL_INFO = "personal_info"
        private const val KEY_EXPERIENCE = "experience"
        private const val KEY_SKILLS = "skills"
        private const val KEY_COVER_LETTER = "cover_letter"
        private const val KEY_DOCUMENTS = "documents"
        private const val KEY_FORM_COMPLETED = "form_completed"
    }
    
    /**
     * Save personal information
     */
    fun savePersonalInfo(personalInfo: PersonalInfo) {
        val json = gson.toJson(personalInfo)
        prefs.edit().putString(KEY_PERSONAL_INFO, json).apply()
    }
    
    /**
     * Get personal information
     */
    fun getPersonalInfo(): PersonalInfo {
        val json = prefs.getString(KEY_PERSONAL_INFO, null)
        return if (json != null) {
            try {
                gson.fromJson(json, PersonalInfo::class.java)
            } catch (e: Exception) {
                PersonalInfo("", "", "", "", "", "")
            }
        } else {
            PersonalInfo("", "", "", "", "", "")
        }
    }
    
    /**
     * Save work experience list
     */
    fun saveExperience(experience: List<WorkExperience>) {
        val json = gson.toJson(experience)
        prefs.edit().putString(KEY_EXPERIENCE, json).apply()
    }
    
    /**
     * Get work experience list
     */
    fun getExperience(): List<WorkExperience> {
        val json = prefs.getString(KEY_EXPERIENCE, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<WorkExperience>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Save skills list
     */
    fun saveSkills(skills: List<String>) {
        val json = gson.toJson(skills)
        prefs.edit().putString(KEY_SKILLS, json).apply()
    }
    
    /**
     * Get skills list
     */
    fun getSkills(): List<String> {
        val json = prefs.getString(KEY_SKILLS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Save cover letter
     */
    fun saveCoverLetter(coverLetter: String) {
        prefs.edit().putString(KEY_COVER_LETTER, coverLetter).apply()
    }
    
    /**
     * Get cover letter
     */
    fun getCoverLetter(): String {
        return prefs.getString(KEY_COVER_LETTER, "") ?: ""
    }
    
    /**
     * Save documents list
     */
    fun saveDocuments(documents: List<Document>) {
        val json = gson.toJson(documents)
        prefs.edit().putString(KEY_DOCUMENTS, json).apply()
    }
    
    /**
     * Get documents list
     */
    fun getDocuments(): List<Document> {
        val json = prefs.getString(KEY_DOCUMENTS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<Document>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Mark form as completed
     */
    fun setFormCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_FORM_COMPLETED, completed).apply()
    }
    
    /**
     * Check if form is completed
     */
    fun isFormCompleted(): Boolean {
        return prefs.getBoolean(KEY_FORM_COMPLETED, false)
    }
    
    /**
     * Clear all form data
     */
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Get form completion percentage
     */
    fun getFormCompletionPercentage(): Int {
        val personalInfo = getPersonalInfo()
        val experience = getExperience()
        val skills = getSkills()
        val coverLetter = getCoverLetter()
        val documents = getDocuments()
        
        var completedFields = 0
        var totalFields = 0
        
        // Personal Info (6 fields)
        totalFields += 6
        if (personalInfo.fullName.isNotBlank()) completedFields++
        if (personalInfo.email.isNotBlank()) completedFields++
        if (personalInfo.phone.isNotBlank()) completedFields++
        if (personalInfo.address.isNotBlank()) completedFields++
        if (personalInfo.dateOfBirth.isNotBlank()) completedFields++
        if (personalInfo.gender.isNotBlank()) completedFields++
        
        // Experience (1 field - at least one experience)
        totalFields += 1
        if (experience.isNotEmpty()) completedFields++
        
        // Skills (1 field - at least one skill)
        totalFields += 1
        if (skills.isNotEmpty()) completedFields++
        
        // Cover Letter (1 field)
        totalFields += 1
        if (coverLetter.isNotBlank()) completedFields++
        
        // Documents (1 field - at least one document)
        totalFields += 1
        if (documents.isNotEmpty()) completedFields++
        
        return if (totalFields > 0) (completedFields * 100) / totalFields else 0
    }
}
