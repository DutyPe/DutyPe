package com.example.dutype.employer.ai

import android.content.Context
import com.example.dutype.utils.LocaleHelper
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** One AI top pick for a job (server: functions/src/ai-hiring.ts aiShortlist). */
data class AiPick(
    val applicationId: String,
    val workerId: String,
    val name: String,
    val photoUrl: String,
    val distanceKm: Double?,
    val facts: List<String>,
    val reason: String
)

/** [aiLocked]: no DutyPe AI (plan / trial) — picks come from the fixed score only. */
data class AiShortlist(val picks: List<AiPick>, val considered: Int, val byAi: Boolean, val aiLocked: Boolean = false)

/** An action DutyPe AI proposes; runs only after the employer confirms. */
data class AiAction(val type: String, val args: Map<String, Any?>, val summary: String)

/** One DutyPe AI reply. [locked] = "upgrade" / "limit" when AI is not available right now. */
data class DutyPeAiTurn(val reply: String, val action: AiAction?, val locked: String?, val stats: AiStats? = null)

/** The employer's live numbers, sent back with every DutyPe AI answer. */
data class AiStats(val appliedToday: Int, val waiting: Int, val openJobs: Int, val hired: Int)

/** A job draft in the posting form's exact values (server: cleanDraft). */
data class AiJobDraft(
    val title: String = "",
    val category: String = "",
    val employmentType: String = "FULL_TIME",
    val payAmount: Long = 0,
    val payType: String = "MONTHLY",
    val vacancies: Int = 1,
    val shift: String = "ANY",
    val gender: String = "Both",
    val experience: String = "No Experience Required",
    val education: String = "No qualification required",
    val perks: List<String> = emptyList(),
    val description: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title, "category" to category, "employmentType" to employmentType, "payAmount" to payAmount,
        "payType" to payType, "vacancies" to vacancies, "shift" to shift, "gender" to gender,
        "experience" to experience, "education" to education, "perks" to perks, "description" to description
    )

    companion object {
        fun from(m: Map<*, *>?): AiJobDraft = AiJobDraft(
            title = m?.get("title") as? String ?: "",
            category = m?.get("category") as? String ?: "",
            employmentType = m?.get("employmentType") as? String ?: "FULL_TIME",
            payAmount = (m?.get("payAmount") as? Number)?.toLong() ?: 0,
            payType = m?.get("payType") as? String ?: "MONTHLY",
            vacancies = (m?.get("vacancies") as? Number)?.toInt() ?: 1,
            shift = m?.get("shift") as? String ?: "ANY",
            gender = m?.get("gender") as? String ?: "Both",
            experience = m?.get("experience") as? String ?: "No Experience Required",
            education = m?.get("education") as? String ?: "No qualification required",
            perks = (m?.get("perks") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            description = m?.get("description") as? String ?: ""
        )
    }
}

/** One turn of the talk-to-post conversation. */
data class AiAssistantTurn(val draft: AiJobDraft, val ready: Boolean, val question: String, val summary: String, val locked: String? = null)

/** Employer AI: top picks, nearby supply and talk-to-post. Every call is server-side and capped. */
@Singleton
class AiHiringService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val functions: FirebaseFunctions
) {
    private val lang: String get() = LocaleHelper.getLanguage(context)

    @Suppress("UNCHECKED_CAST")
    private suspend fun call(name: String, payload: Map<String, Any?>): Map<String, Any?> =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<String, Any?> ?: emptyMap()

    suspend fun shortlist(jobId: String): Result<AiShortlist> = runCatching {
        val data = call("aiShortlist", mapOf("jobId" to jobId, "lang" to lang))
        AiShortlist(
            picks = (data["picks"] as? List<*>).orEmpty().mapNotNull { raw ->
                val p = raw as? Map<*, *> ?: return@mapNotNull null
                AiPick(
                    applicationId = p["applicationId"] as? String ?: return@mapNotNull null,
                    workerId = p["workerId"] as? String ?: "",
                    name = p["name"] as? String ?: "Worker",
                    photoUrl = p["photoUrl"] as? String ?: "",
                    distanceKm = (p["distanceKm"] as? Number)?.toDouble(),
                    facts = (p["facts"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                    reason = p["reason"] as? String ?: ""
                )
            },
            considered = (data["considered"] as? Number)?.toInt() ?: 0,
            byAi = data["byAi"] == true,
            aiLocked = data["aiLocked"] == true
        )
    }

    /** About how many available workers with this skill are within 5 and 10 km. */
    suspend fun nearbyWorkers(lat: Double, lng: Double, category: String): Result<Pair<Int, Int>> = runCatching {
        val data = call("nearbyWorkerCount", mapOf("lat" to lat, "lng" to lng, "category" to category))
        ((data["within5km"] as? Number)?.toInt() ?: 0) to ((data["within10km"] as? Number)?.toInt() ?: 0)
    }

    suspend fun assistant(transcript: String, draft: AiJobDraft): Result<AiAssistantTurn> = runCatching {
        val data = call("aiJobAssistant", mapOf("transcript" to transcript, "draft" to draft.toMap(), "lang" to lang))
        AiAssistantTurn(
            draft = AiJobDraft.from(data["draft"] as? Map<*, *>),
            ready = data["ready"] == true,
            question = data["question"] as? String ?: "",
            summary = data["summary"] as? String ?: "",
            locked = data["locked"] as? String
        )
    }

    /** One message to DutyPe AI; [history] = the last few turns as (fromAi, text). */
    suspend fun dutypeAi(message: String, history: List<Pair<Boolean, String>>): Result<DutyPeAiTurn> = runCatching {
        val data = call(
            "dutypeAi",
            mapOf(
                "message" to message,
                "lang" to lang,
                "history" to history.takeLast(6).map { (fromAi, text) -> mapOf("role" to if (fromAi) "ai" else "user", "text" to text) }
            )
        )
        @Suppress("UNCHECKED_CAST")
        val action = (data["action"] as? Map<String, Any?>)?.let { a ->
            AiAction(a["type"] as? String ?: "", (a["args"] as? Map<String, Any?>).orEmpty(), a["summary"] as? String ?: "")
        }
        val stats = (data["stats"] as? Map<*, *>)?.let { s ->
            fun n(key: String) = (s[key] as? Number)?.toInt() ?: 0
            AiStats(n("appliedToday"), n("waiting"), n("openJobs"), n("hired"))
        }
        DutyPeAiTurn(data["reply"] as? String ?: "", action, data["locked"] as? String, stats)
    }
}
