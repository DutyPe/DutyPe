package com.example.dutype.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.JobPerk
import com.example.dutype.employer.models.JobUrgency
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
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
 * JobDraftDataStore - Auto-save job posting drafts to DataStore
 * 
 * P2 FIX: Prevents user frustration by auto-saving job posting drafts.
 * Drafts are saved on field changes (debounced) and restored on screen load.
 * 
 * Features:
 * - Auto-save on field changes
 * - Restore draft on screen load
 * - Clear draft on successful post
 * - 24-hour TTL for drafts
 * 
 * @author DutyPe Engineering Team
 * @since 2.3.0
 */
@Singleton
class JobDraftDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.jobDraftDataStore: DataStore<Preferences> by preferencesDataStore(
            name = "job_draft_data"
        )
        
        // Draft TTL: 24 hours
        private const val DRAFT_TTL_MS = 24 * 60 * 60 * 1000L
        
        // DataStore keys
        private val KEY_TITLE = stringPreferencesKey("draft_title")
        private val KEY_DESCRIPTION = stringPreferencesKey("draft_description")
        private val KEY_PAY_AMOUNT = stringPreferencesKey("draft_pay_amount")
        private val KEY_PAY_TYPE = stringPreferencesKey("draft_pay_type")
        private val KEY_LOCATION = stringPreferencesKey("draft_location")
        private val KEY_CATEGORY = stringPreferencesKey("draft_category")
        private val KEY_CUSTOM_CATEGORY = stringPreferencesKey("draft_custom_category")
        private val KEY_VACANCIES = stringPreferencesKey("draft_vacancies")
        private val KEY_CONTACT_NUMBER = stringPreferencesKey("draft_contact_number")
        private val KEY_SHIFT_TIMING = stringPreferencesKey("draft_shift_timing")
        private val KEY_URGENCY = stringPreferencesKey("draft_urgency")
        private val KEY_PERKS = stringPreferencesKey("draft_perks")
        private val KEY_WORK_TYPE = stringPreferencesKey("draft_work_type")
        private val KEY_EXPERIENCE_LEVEL = stringPreferencesKey("draft_experience_level")
        private val KEY_AGE_RANGE = stringPreferencesKey("draft_age_range")
        private val KEY_GENDER = stringPreferencesKey("draft_gender")
        private val KEY_LANDMARK = stringPreferencesKey("draft_landmark")
        private val KEY_REQUIREMENTS = stringPreferencesKey("draft_requirements")
        private val KEY_BENEFITS = stringPreferencesKey("draft_benefits")
        private val KEY_TIMESTAMP = longPreferencesKey("draft_timestamp")
        private val KEY_EMPLOYER_ID = stringPreferencesKey("draft_employer_id")
    }
    
    private val gson = Gson()

    
    /**
     * Job draft data class
     */
    data class JobDraft(
        val title: String = "",
        val description: String = "",
        val payAmount: String = "",
        val payType: PayType = PayType.HOURLY,
        val location: String = "",
        val category: JobCategory = JobCategory.COOK,
        val customCategory: String = "",
        val vacancies: String = "",
        val contactNumber: String = "",
        val shiftTiming: ShiftTiming = ShiftTiming.FLEXIBLE,
        val urgency: JobUrgency = JobUrgency.FLEXIBLE,
        val perks: Set<JobPerk> = emptySet(),
        val workType: String = "Part-time",
        val experienceLevel: String = "No Experience Required",
        val ageRange: String = "18-35",
        val gender: String = "Any",
        val landmark: String = "",
        val requirements: String = "",
        val benefits: String = "",
        val timestamp: Long = 0L,
        val employerId: String = ""
    ) {
        fun isValid(): Boolean = System.currentTimeMillis() - timestamp < DRAFT_TTL_MS
        fun hasContent(): Boolean = title.isNotBlank() || description.isNotBlank() || 
            payAmount.isNotBlank() || location.isNotBlank()
    }
    
    /**
     * Save job draft
     */
    suspend fun saveDraft(draft: JobDraft) = withContext(Dispatchers.IO) {
        try {
            context.jobDraftDataStore.edit { prefs ->
                prefs[KEY_TITLE] = draft.title
                prefs[KEY_DESCRIPTION] = draft.description
                prefs[KEY_PAY_AMOUNT] = draft.payAmount
                prefs[KEY_PAY_TYPE] = draft.payType.name
                prefs[KEY_LOCATION] = draft.location
                prefs[KEY_CATEGORY] = draft.category.name
                prefs[KEY_CUSTOM_CATEGORY] = draft.customCategory
                prefs[KEY_VACANCIES] = draft.vacancies
                prefs[KEY_CONTACT_NUMBER] = draft.contactNumber
                prefs[KEY_SHIFT_TIMING] = draft.shiftTiming.name
                prefs[KEY_URGENCY] = draft.urgency.name
                prefs[KEY_PERKS] = gson.toJson(draft.perks.map { it.name })
                prefs[KEY_WORK_TYPE] = draft.workType
                prefs[KEY_EXPERIENCE_LEVEL] = draft.experienceLevel
                prefs[KEY_AGE_RANGE] = draft.ageRange
                prefs[KEY_GENDER] = draft.gender
                prefs[KEY_LANDMARK] = draft.landmark
                prefs[KEY_REQUIREMENTS] = draft.requirements
                prefs[KEY_BENEFITS] = draft.benefits
                prefs[KEY_TIMESTAMP] = System.currentTimeMillis()
                prefs[KEY_EMPLOYER_ID] = draft.employerId
            }
            Timber.d("📝 JOB_DRAFT: Draft saved successfully")
        } catch (e: Exception) {
            Timber.e(e, "📝 JOB_DRAFT: Failed to save draft")
        }
    }

    
    /**
     * Get saved draft for employer
     */
    suspend fun getDraft(employerId: String): JobDraft? = withContext(Dispatchers.IO) {
        try {
            context.jobDraftDataStore.data.map { prefs ->
                val savedEmployerId = prefs[KEY_EMPLOYER_ID] ?: ""
                val timestamp = prefs[KEY_TIMESTAMP] ?: 0L
                
                // Check if draft belongs to this employer and is not expired
                if (savedEmployerId != employerId) {
                    Timber.d("📝 JOB_DRAFT: No draft for employer $employerId")
                    return@map null
                }
                
                if (System.currentTimeMillis() - timestamp > DRAFT_TTL_MS) {
                    Timber.d("📝 JOB_DRAFT: Draft expired, clearing...")
                    return@map null
                }
                
                val perksJson = prefs[KEY_PERKS] ?: "[]"
                val perkNames: List<String> = try {
                    val type = object : TypeToken<List<String>>() {}.type
                    gson.fromJson(perksJson, type) ?: emptyList()
                } catch (e: Exception) { emptyList() }
                
                val perks = perkNames.mapNotNull { name ->
                    try { JobPerk.valueOf(name) } catch (e: Exception) { null }
                }.toSet()
                
                JobDraft(
                    title = prefs[KEY_TITLE] ?: "",
                    description = prefs[KEY_DESCRIPTION] ?: "",
                    payAmount = prefs[KEY_PAY_AMOUNT] ?: "",
                    payType = try { PayType.valueOf(prefs[KEY_PAY_TYPE] ?: "HOURLY") } 
                        catch (e: Exception) { PayType.HOURLY },
                    location = prefs[KEY_LOCATION] ?: "",
                    category = try { JobCategory.valueOf(prefs[KEY_CATEGORY] ?: "COOK") } 
                        catch (e: Exception) { JobCategory.COOK },
                    customCategory = prefs[KEY_CUSTOM_CATEGORY] ?: "",
                    vacancies = prefs[KEY_VACANCIES] ?: "",
                    contactNumber = prefs[KEY_CONTACT_NUMBER] ?: "",
                    shiftTiming = try { ShiftTiming.valueOf(prefs[KEY_SHIFT_TIMING] ?: "FLEXIBLE") } 
                        catch (e: Exception) { ShiftTiming.FLEXIBLE },
                    urgency = try { JobUrgency.valueOf(prefs[KEY_URGENCY] ?: "FLEXIBLE") } 
                        catch (e: Exception) { JobUrgency.FLEXIBLE },
                    perks = perks,
                    workType = prefs[KEY_WORK_TYPE] ?: "Part-time",
                    experienceLevel = prefs[KEY_EXPERIENCE_LEVEL] ?: "No Experience Required",
                    ageRange = prefs[KEY_AGE_RANGE] ?: "18-35",
                    gender = prefs[KEY_GENDER] ?: "Any",
                    landmark = prefs[KEY_LANDMARK] ?: "",
                    requirements = prefs[KEY_REQUIREMENTS] ?: "",
                    benefits = prefs[KEY_BENEFITS] ?: "",
                    timestamp = timestamp,
                    employerId = savedEmployerId
                )
            }.first()?.also {
                if (it.hasContent()) {
                    Timber.d("📝 JOB_DRAFT: Draft restored for employer $employerId")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "📝 JOB_DRAFT: Failed to get draft")
            null
        }
    }

    
    /**
     * Clear draft after successful post
     */
    suspend fun clearDraft() = withContext(Dispatchers.IO) {
        try {
            context.jobDraftDataStore.edit { prefs ->
                prefs.clear()
            }
            Timber.d("📝 JOB_DRAFT: Draft cleared")
        } catch (e: Exception) {
            Timber.e(e, "📝 JOB_DRAFT: Failed to clear draft")
        }
    }
    
    /**
     * Check if a draft exists for employer
     */
    suspend fun hasDraft(employerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val draft = getDraft(employerId)
            draft != null && draft.hasContent()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Update single field in draft (for debounced auto-save)
     */
    suspend fun updateField(field: String, value: String) = withContext(Dispatchers.IO) {
        try {
            context.jobDraftDataStore.edit { prefs ->
                when (field) {
                    "title" -> prefs[KEY_TITLE] = value
                    "description" -> prefs[KEY_DESCRIPTION] = value
                    "payAmount" -> prefs[KEY_PAY_AMOUNT] = value
                    "location" -> prefs[KEY_LOCATION] = value
                    "vacancies" -> prefs[KEY_VACANCIES] = value
                    "contactNumber" -> prefs[KEY_CONTACT_NUMBER] = value
                    "landmark" -> prefs[KEY_LANDMARK] = value
                    "requirements" -> prefs[KEY_REQUIREMENTS] = value
                    "benefits" -> prefs[KEY_BENEFITS] = value
                }
                prefs[KEY_TIMESTAMP] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Timber.e(e, "📝 JOB_DRAFT: Failed to update field $field")
        }
    }
}
