package com.example.dutype.profile

import androidx.annotation.Keep
import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.firestore.FirestoreSchema.WorkerProfiles
import com.example.dutype.models.EmployerSubscription
import com.example.dutype.utils.epochMillis

/** `worker_profiles/{uid}` — exactly the schema fields. */
@Keep
data class WorkerProfile(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val education: String = "",
    val experienceYears: Int = 0,
    /** Category keys, e.g. ["DRIVER", "HELPER"]; skills[0] is the primary skill. */
    val skills: List<String> = emptyList(),
    val bio: String = "",
    val address: String = "",
    val area: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val available: Boolean = false,
    val createdAt: Long = 0L
) {
    /** Name + phone + at least one skill = can apply (the server uses the same rule for referrals). */
    val isComplete: Boolean get() = name.isNotBlank() && phone.isNotBlank() && skills.isNotEmpty()

    /** Weighted progress shown in the profile header. */
    val completionPercent: Int get() {
        var score = 0
        if (name.isNotBlank()) score += 25
        if (phone.isNotBlank()) score += 20
        if (skills.isNotEmpty()) score += 30
        if (address.isNotBlank()) score += 10
        if (photoUrl.isNotBlank()) score += 5
        if (gender.isNotBlank()) score += 5
        if (dateOfBirth.isNotBlank()) score += 5
        return score.coerceAtMost(100)
    }

    val missingFields: List<String> get() = buildList {
        if (name.isBlank()) add("Name")
        if (skills.isEmpty()) add("Skills")
        if (address.isBlank()) add("Address")
        if (photoUrl.isBlank()) add("Photo")
    }

    companion object {
        fun from(uid: String, d: Map<String, Any?>): WorkerProfile = WorkerProfile(
            uid = uid,
            name = d[WorkerProfiles.NAME] as? String ?: "",
            phone = d[WorkerProfiles.PHONE] as? String ?: "",
            photoUrl = d[WorkerProfiles.PHOTO_URL] as? String ?: "",
            gender = d[WorkerProfiles.GENDER] as? String ?: "",
            dateOfBirth = d[WorkerProfiles.DATE_OF_BIRTH] as? String ?: "",
            education = d[WorkerProfiles.EDUCATION] as? String ?: "",
            experienceYears = (d[WorkerProfiles.EXPERIENCE_YEARS] as? Number)?.toInt() ?: 0,
            skills = (d[WorkerProfiles.SKILLS] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            bio = d[WorkerProfiles.BIO] as? String ?: "",
            address = d[WorkerProfiles.ADDRESS] as? String ?: "",
            area = d[WorkerProfiles.AREA] as? String ?: "",
            lat = (d[WorkerProfiles.LAT] as? Number)?.toDouble() ?: 0.0,
            lng = (d[WorkerProfiles.LNG] as? Number)?.toDouble() ?: 0.0,
            available = d[WorkerProfiles.AVAILABLE] as? Boolean ?: false,
            createdAt = d[WorkerProfiles.CREATED_AT].epochMillis()
        )
    }
}

/**
 * `employer_profiles/{uid}`. INDIVIDUAL: ownerName, address. COMPANY: + businessName, businessType
 * (required) and gstin (optional).
 */
@Keep
data class EmployerProfile(
    val uid: String = "",
    val employerType: String = Values.EmployerType.INDIVIDUAL,
    val ownerName: String = "",
    val businessName: String = "",
    val businessType: String = "",
    val gstin: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val address: String = "",
    val area: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val subscription: EmployerSubscription = EmployerSubscription(),
    val freeUrgentPostsUsed: Int = 0,
    /** Free posts (normal or urgent) earned by referring friends, usable until [referralFreePostsUntil]. */
    val referralFreePosts: Int = 0,
    val referralFreePostsUntil: Long = 0L,
    /** Free DutyPe AI actions used (10 free for employers without an AI plan). */
    val aiTrialUsed: Int = 0,
    val verified: Boolean = false,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val totalHires: Int = 0,
    val createdAt: Long = 0L
) {
    val isCompany: Boolean get() = employerType == Values.EmployerType.COMPANY

    /** Referral free posts that can be used right now. */
    val referralPostsLeft: Int get() =
        if (referralFreePostsUntil > System.currentTimeMillis()) referralFreePosts.coerceAtLeast(0) else 0

    /** Name shown on job cards: business name for companies, the owner's name otherwise. */
    val displayName: String get() = if (isCompany && businessName.isNotBlank()) businessName else ownerName

    val isComplete: Boolean get() = ownerName.isNotBlank() && phone.isNotBlank() &&
        (!isCompany || (businessName.isNotBlank() && businessType.isNotBlank()))

    val completionPercent: Int get() {
        var score = 0
        if (ownerName.isNotBlank()) score += 30
        if (phone.isNotBlank()) score += 25
        if (!isCompany || businessName.isNotBlank()) score += 20
        if (address.isNotBlank()) score += 20
        if (photoUrl.isNotBlank()) score += 5
        return score.coerceAtMost(100)
    }

    val missingFields: List<String> get() = buildList {
        if (ownerName.isBlank()) add("Your name")
        if (isCompany && businessName.isBlank()) add("Business name")
        if (isCompany && businessType.isBlank()) add("Business type")
        if (address.isBlank()) add("Address")
    }

    companion object {
        fun from(uid: String, d: Map<String, Any?>): EmployerProfile = EmployerProfile(
            uid = uid,
            employerType = d[EmployerProfiles.EMPLOYER_TYPE] as? String ?: Values.EmployerType.INDIVIDUAL,
            ownerName = d[EmployerProfiles.OWNER_NAME] as? String ?: "",
            businessName = d[EmployerProfiles.BUSINESS_NAME] as? String ?: "",
            businessType = d[EmployerProfiles.BUSINESS_TYPE] as? String ?: "",
            gstin = d[EmployerProfiles.GSTIN] as? String ?: "",
            phone = d[EmployerProfiles.PHONE] as? String ?: "",
            photoUrl = d[EmployerProfiles.PHOTO_URL] as? String ?: "",
            address = d[EmployerProfiles.ADDRESS] as? String ?: "",
            area = d[EmployerProfiles.AREA] as? String ?: "",
            lat = (d[EmployerProfiles.LAT] as? Number)?.toDouble() ?: 0.0,
            lng = (d[EmployerProfiles.LNG] as? Number)?.toDouble() ?: 0.0,
            subscription = EmployerSubscription.fromMap(
                (d[EmployerProfiles.SUBSCRIPTION] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }
            ),
            freeUrgentPostsUsed = (d[EmployerProfiles.FREE_URGENT_POSTS_USED] as? Number)?.toInt() ?: 0,
            referralFreePosts = (d[EmployerProfiles.REFERRAL_FREE_POSTS] as? Number)?.toInt() ?: 0,
            referralFreePostsUntil = d[EmployerProfiles.REFERRAL_FREE_POSTS_UNTIL].epochMillis(),
            aiTrialUsed = (d[EmployerProfiles.AI_TRIAL_USED] as? Number)?.toInt() ?: 0,
            verified = d[EmployerProfiles.VERIFIED] as? Boolean ?: false,
            rating = (d[EmployerProfiles.RATING] as? Number)?.toDouble() ?: 0.0,
            ratingCount = (d[EmployerProfiles.RATING_COUNT] as? Number)?.toInt() ?: 0,
            totalHires = (d[EmployerProfiles.TOTAL_HIRES] as? Number)?.toInt() ?: 0,
            createdAt = d[EmployerProfiles.CREATED_AT].epochMillis()
        )
    }
}

/** Worker experience choices shown in setup, stored as whole years. */
enum class ExperienceBucket(val label: String, val years: Int) {
    FRESHER("Fresher", 0),
    ONE_TO_TWO("1-2 years", 1),
    THREE_TO_FIVE("3-5 years", 3),
    FIVE_PLUS("5+ years", 5);

    companion object {
        fun forYears(years: Int): ExperienceBucket = entries.lastOrNull { years >= it.years } ?: FRESHER
    }
}
