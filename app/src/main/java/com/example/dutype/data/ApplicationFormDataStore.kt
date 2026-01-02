package com.example.dutype.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dutype.worker.models.PersonalInfo
import com.example.dutype.models.WorkExperience
import com.example.dutype.worker.models.Document
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data store for application form data using DataStore (migrated from SharedPreferences)
 * 
 * REFACTORED:
 * - Migrated from SharedPreferences to DataStore for type-safety and coroutine support
 * - All operations are now suspend functions running on IO dispatcher
 * - Removed duplicate getFormCompletionPercentage() - use ProfileCompletionService instead
 * - Added proper error handling with Timber logging
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class ApplicationFormDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "application_form_data"
        )
        
        // DataStore keys
        private val KEY_PERSONAL_INFO = stringPreferencesKey("personal_info")
        private val KEY_EXPERIENCE = stringPreferencesKey("experience")
        private val KEY_SKILLS = stringPreferencesKey("skills")
        private val KEY_COVER_LETTER = stringPreferencesKey("cover_letter")
        private val KEY_DOCUMENTS = stringPreferencesKey("documents")
        private val KEY_FORM_COMPLETED = booleanPreferencesKey("form_completed")
    }
    
    private val gson = Gson()
    
    // ==========================================
    // PERSONAL INFO OPERATIONS
    // ==========================================
    
    /**
     * Save personal information
     * Runs on IO dispatcher for thread safety
     */
    suspend fun savePersonalInfo(personalInfo: PersonalInfo) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(personalInfo)
            context.dataStore.edit { preferences ->
                preferences[KEY_PERSONAL_INFO] = json
            }
            Timber.d("📝 ApplicationFormDataStore: Personal info saved")
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to save personal info")
        }
    }
    
    /**
     * Get personal information
     * Returns default empty PersonalInfo if not found or on error
     */
    suspend fun getPersonalInfo(): PersonalInfo = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { preferences ->
                val json = preferences[KEY_PERSONAL_INFO]
                if (json != null) {
                    try {
                        gson.fromJson(json, PersonalInfo::class.java)
                    } catch (e: Exception) {
                        Timber.e(e, "❌ ApplicationFormDataStore: Failed to parse personal info")
                        createEmptyPersonalInfo()
                    }
                } else {
                    createEmptyPersonalInfo()
                }
            }.first()
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to get personal info")
            createEmptyPersonalInfo()
        }
    }
    
    private fun createEmptyPersonalInfo() = PersonalInfo("", "", "", "", "", "")
    
    // ==========================================
    // WORK EXPERIENCE OPERATIONS
    // ==========================================
    
    /**
     * Save work experience list
     */
    suspend fun saveExperience(experience: List<WorkExperience>) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(experience)
            context.dataStore.edit { preferences ->
                preferences[KEY_EXPERIENCE] = json
            }
            Timber.d("📝 ApplicationFormDataStore: Experience saved (${experience.size} items)")
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to save experience")
        }
    }
    
    /**
     * Get work experience list
     */
    suspend fun getExperience(): List<WorkExperience> = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { preferences ->
                val json = preferences[KEY_EXPERIENCE]
                if (json != null) {
                    try {
                        val type = object : TypeToken<List<WorkExperience>>() {}.type
                        gson.fromJson<List<WorkExperience>>(json, type) ?: emptyList()
                    } catch (e: Exception) {
                        Timber.e(e, "❌ ApplicationFormDataStore: Failed to parse experience")
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }.first()
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to get experience")
            emptyList()
        }
    }
    
    // ==========================================
    // SKILLS OPERATIONS
    // ==========================================
    
    /**
     * Save skills list
     */
    suspend fun saveSkills(skills: List<String>) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(skills)
            context.dataStore.edit { preferences ->
                preferences[KEY_SKILLS] = json
            }
            Timber.d("📝 ApplicationFormDataStore: Skills saved (${skills.size} items)")
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to save skills")
        }
    }
    
    /**
     * Get skills list
     */
    suspend fun getSkills(): List<String> = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { preferences ->
                val json = preferences[KEY_SKILLS]
                if (json != null) {
                    try {
                        val type = object : TypeToken<List<String>>() {}.type
                        gson.fromJson<List<String>>(json, type) ?: emptyList()
                    } catch (e: Exception) {
                        Timber.e(e, "❌ ApplicationFormDataStore: Failed to parse skills")
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }.first()
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to get skills")
            emptyList()
        }
    }
    
    // ==========================================
    // COVER LETTER OPERATIONS
    // ==========================================
    
    /**
     * Save cover letter
     */
    suspend fun saveCoverLetter(coverLetter: String) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_COVER_LETTER] = coverLetter
            }
            Timber.d("📝 ApplicationFormDataStore: Cover letter saved")
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to save cover letter")
        }
    }
    
    /**
     * Get cover letter
     */
    suspend fun getCoverLetter(): String = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { preferences ->
                preferences[KEY_COVER_LETTER] ?: ""
            }.first()
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to get cover letter")
            ""
        }
    }
    
    // ==========================================
    // DOCUMENTS OPERATIONS
    // ==========================================
    
    /**
     * Save documents list
     */
    suspend fun saveDocuments(documents: List<Document>) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(documents)
            context.dataStore.edit { preferences ->
                preferences[KEY_DOCUMENTS] = json
            }
            Timber.d("📝 ApplicationFormDataStore: Documents saved (${documents.size} items)")
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to save documents")
        }
    }
    
    /**
     * Get documents list
     */
    suspend fun getDocuments(): List<Document> = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { preferences ->
                val json = preferences[KEY_DOCUMENTS]
                if (json != null) {
                    try {
                        val type = object : TypeToken<List<Document>>() {}.type
                        gson.fromJson<List<Document>>(json, type) ?: emptyList()
                    } catch (e: Exception) {
                        Timber.e(e, "❌ ApplicationFormDataStore: Failed to parse documents")
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }.first()
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to get documents")
            emptyList()
        }
    }
    
    // ==========================================
    // FORM COMPLETION STATUS
    // ==========================================
    
    /**
     * Mark form as completed
     */
    suspend fun setFormCompleted(completed: Boolean) = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { preferences ->
                preferences[KEY_FORM_COMPLETED] = completed
            }
            Timber.d("📝 ApplicationFormDataStore: Form completed status set to $completed")
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to set form completed status")
        }
    }
    
    /**
     * Check if form is completed
     */
    suspend fun isFormCompleted(): Boolean = withContext(Dispatchers.IO) {
        try {
            context.dataStore.data.map { preferences ->
                preferences[KEY_FORM_COMPLETED] ?: false
            }.first()
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to get form completed status")
            false
        }
    }
    
    // ==========================================
    // UTILITY OPERATIONS
    // ==========================================
    
    /**
     * Clear all form data
     * Use when user logs out or wants to reset the form
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            Timber.d("📝 ApplicationFormDataStore: All data cleared")
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to clear data")
        }
    }
    
    /**
     * Check if form has any data saved
     * Useful for showing "Continue Application" vs "Start New Application"
     */
    suspend fun hasFormData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val personalInfo = getPersonalInfo()
            val experience = getExperience()
            val skills = getSkills()
            val coverLetter = getCoverLetter()
            val documents = getDocuments()
            
            personalInfo.fullName.isNotBlank() ||
                experience.isNotEmpty() ||
                skills.isNotEmpty() ||
                coverLetter.isNotBlank() ||
                documents.isNotEmpty()
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFormDataStore: Failed to check form data")
            false
        }
    }
    
    // ==========================================
    // NOTE: getFormCompletionPercentage() REMOVED
    // ==========================================
    // Profile completion percentage should be calculated by ProfileCompletionService
    // which fetches data from Firestore (source of truth) and provides consistent
    // calculation across the app. Using local data for completion percentage
    // can lead to inconsistencies.
    //
    // Use: ProfileCompletionService.calculateWorkerProfileCompletion(userId)
    // ==========================================
}
