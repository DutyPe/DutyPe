package com.example.dutype.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dutype.worker.models.PersonalInfo
import com.example.dutype.models.WorkExperience
import com.example.dutype.worker.models.Document
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed persistence for the worker application form.
 *
 * Uses the JSON / IO helpers in [DataStoreExt] to avoid 6× repeated
 * try / withContext(IO) / Gson boilerplate.
 */
@Singleton
class ApplicationFormDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "application_form_data"
        )

        private val KEY_PERSONAL_INFO = stringPreferencesKey("personal_info")
        private val KEY_EXPERIENCE = stringPreferencesKey("experience")
        private val KEY_SKILLS = stringPreferencesKey("skills")
        private val KEY_COVER_LETTER = stringPreferencesKey("cover_letter")
        private val KEY_DOCUMENTS = stringPreferencesKey("documents")
        private val KEY_FORM_COMPLETED = booleanPreferencesKey("form_completed")

        private const val TAG = "ApplicationFormDataStore"
    }

    private val store get() = context.dataStore
    private val emptyPersonalInfo = PersonalInfo("", "", "", "", "", "")

    // ---- Personal info ---------------------------------------------------
    suspend fun savePersonalInfo(personalInfo: PersonalInfo) =
        store.saveJson(TAG, KEY_PERSONAL_INFO, personalInfo)

    suspend fun getPersonalInfo(): PersonalInfo =
        store.getJson(TAG, KEY_PERSONAL_INFO, emptyPersonalInfo)

    // ---- Work experience -------------------------------------------------
    suspend fun saveExperience(experience: List<WorkExperience>) =
        store.saveJson(TAG, KEY_EXPERIENCE, experience)

    suspend fun getExperience(): List<WorkExperience> =
        store.getJson(TAG, KEY_EXPERIENCE, emptyList())

    // ---- Skills ----------------------------------------------------------
    suspend fun saveSkills(skills: List<String>) =
        store.saveJson(TAG, KEY_SKILLS, skills)

    suspend fun getSkills(): List<String> =
        store.getJson(TAG, KEY_SKILLS, emptyList())

    // ---- Cover letter (plain string) -------------------------------------
    suspend fun saveCoverLetter(coverLetter: String) =
        store.editIo<Unit>(TAG) { it[KEY_COVER_LETTER] = coverLetter }

    suspend fun getCoverLetter(): String =
        store.readIo(TAG, "") { it[KEY_COVER_LETTER] ?: "" }

    // ---- Documents -------------------------------------------------------
    suspend fun saveDocuments(documents: List<Document>) =
        store.saveJson(TAG, KEY_DOCUMENTS, documents)

    suspend fun getDocuments(): List<Document> =
        store.getJson(TAG, KEY_DOCUMENTS, emptyList())

    // ---- Form completion flag --------------------------------------------
    suspend fun setFormCompleted(completed: Boolean) =
        store.editIo<Unit>(TAG) { it[KEY_FORM_COMPLETED] = completed }

    suspend fun isFormCompleted(): Boolean =
        store.readIo(TAG, false) { it[KEY_FORM_COMPLETED] ?: false }

    // ---- Bulk operations -------------------------------------------------
    suspend fun clearAllData() = store.editIo<Unit>(TAG) { it.clear() }

    suspend fun hasFormData(): Boolean = try {
        getPersonalInfo().fullName.isNotBlank() ||
            getExperience().isNotEmpty() ||
            getSkills().isNotEmpty() ||
            getCoverLetter().isNotBlank() ||
            getDocuments().isNotEmpty()
    } catch (e: Exception) {
        false
    }
}