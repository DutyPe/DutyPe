package com.example.dutype.firestore

/**
 * DutyPe Firestore schema — the single source of truth for collection and field names.
 *
 * Rules for every collection:
 *  - The document id is never repeated as a field.
 *  - One name per concept, stored in exactly one place (no mirrors, no aliases).
 *  - Anything that can be derived (from the doc id, another doc, or a count query) is not stored.
 *  - Nothing grows without bound inside one document (lists that grow become subcollections).
 *  - Geo documents carry `lat`, `lng` (numbers) + `geohash` (string, precision 9) and a short
 *    `area` for display.
 *  - Pay is whole rupees; wallet and payment money is whole paise.
 *  - Timestamps are Firestore Timestamps.
 *  - Fields marked "server" are written only by Cloud Functions (rules forbid client writes).
 *  - Enum-like values come from [Values]; never type them as string literals.
 *
 * There is intentionally NO `users/{uid}` collection: role lives in phoneRoles, identity in the
 * role profile, push settings in user_tokens, referral linkage in referral_stats / referrals.
 *
 * Mirrored 1:1 in functions/src/schema.ts (Cloud Functions + web admin) — change both together.
 */
object FirestoreSchema {

    // ───────────────────────────────── Identity ─────────────────────────────────

    /** phoneRoles/{+91XXXXXXXXXX} — O(1) "does this number exist, with which role" (pre-OTP and at app start). */
    object PhoneRoles {
        const val COLLECTION = "phoneRoles"
        const val UID = "uid"
        const val ROLE = "role"               // Values.Role
    }

    /** user_tokens/{uid} — push delivery (read only by Cloud Functions). */
    object UserTokens {
        const val COLLECTION = "user_tokens"
        const val FCM_TOKEN = "fcmToken"
        const val LANGUAGE = "language"       // en | te | hi — localizes push text
        const val UPDATED_AT = "updatedAt"    // stale-token cleanup
    }

    // ───────────────────────────────── Workers ──────────────────────────────────

    /** worker_profiles/{uid} — full private profile, read on tap. Public numbers live on worker_cards. */
    object WorkerProfiles {
        const val COLLECTION = "worker_profiles"
        const val NAME = "name"
        const val PHONE = "phone"
        const val EMAIL = "email"                 // optional, from Truecaller (server-written)
        const val PHOTO_URL = "photoUrl"
        const val GENDER = "gender"
        const val DATE_OF_BIRTH = "dateOfBirth"
        const val EDUCATION = "education"
        const val EXPERIENCE_YEARS = "experienceYears"
        const val SKILLS = "skills"                   // category keys, e.g. ["DRIVER","HELPER"]
        const val BIO = "bio"
        const val ADDRESS = "address"
        const val AREA = "area"
        const val LAT = "lat"
        const val LNG = "lng"
        const val GEOHASH = "geohash"
        const val AVAILABLE = "available"             // instant-help availability toggle
        const val ACTIVE_URGENT_ID = "activeUrgentId" // (server) accepted urgent need not yet finished (one at a time)
        const val BLOCKED = "blocked"                 // (server)
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    /**
     * worker_cards/{uid} — public card employers browse nearby. Server-written from worker_profiles
     * (home) and resolvePlace (where the worker is now). The ONLY home of rating / jobs-completed
     * numbers. Never contains the phone number or the exact home location.
     */
    object WorkerCards {
        const val COLLECTION = "worker_cards"
        const val NAME = "name"
        const val PHOTO_URL = "photoUrl"
        const val SKILLS = "skills"                   // skills[0] is the primary skill
        const val EXPERIENCE_YEARS = "experienceYears"
        const val AREA = "area"
        const val LAT = "lat"                         // home, rounded to ~1 km (privacy)
        const val LNG = "lng"                         // home, rounded to ~1 km (privacy)
        const val CELL = "cell"                       // home geohash5
        const val DISTRICT_ID = "districtId"          // home district (LGD)
        const val STATE_ID = "stateId"
        const val CURRENT_CELL = "currentCell"        // where the app last saw the worker (geohash5)
        const val CURRENT_DISTRICT_ID = "currentDistrictId"
        /** "{SKILL}_{cell}" for home + current cell, plus "ANY_{cell}" — employer matching keys. */
        const val SKILL_CELLS = "skillCells"
        /** "{SKILL}_{districtId}" for home + current district, plus "ANY_{districtId}". */
        const val SKILL_DISTRICTS = "skillDistricts"
        const val AVAILABLE = "available"
        const val RATING = "rating"                   // (server)
        const val RATING_COUNT = "ratingCount"        // (server)
        const val JOBS_COMPLETED = "jobsCompleted"    // (server)
        const val NO_SHOWS = "noShows"               // (server) urgent jobs accepted but not turned up for
        const val LAST_ACTIVE_AT = "lastActiveAt"     // (server) ranking signal
    }

    // ──────────────────────────────── Employers ─────────────────────────────────

    /**
     * employer_profiles/{uid}.
     * INDIVIDUAL: ownerName, phone, address/area — businessName/businessType/gstin absent.
     * COMPANY:    + businessName (required), businessType (required), gstin (optional).
     */
    object EmployerProfiles {
        const val COLLECTION = "employer_profiles"
        const val EMPLOYER_TYPE = "employerType"      // Values.EmployerType
        const val OWNER_NAME = "ownerName"
        const val BUSINESS_NAME = "businessName"      // COMPANY only
        const val BUSINESS_TYPE = "businessType"      // COMPANY only: shop | hotel | pg | office | warehouse | factory | other
        const val GSTIN = "gstin"                     // COMPANY only, optional
        const val PHONE = "phone"
        const val EMAIL = "email"                 // optional, from Truecaller (server-written)
        const val PHOTO_URL = "photoUrl"
        const val ADDRESS = "address"
        const val AREA = "area"
        const val LAT = "lat"
        const val LNG = "lng"
        const val GEOHASH = "geohash"
        const val SUBSCRIPTION = "subscription"       // map, keys in [Subscription] (server)
        const val FREE_URGENT_POSTS_USED = "freeUrgentPostsUsed"  // (server) first 3 urgent posts are free
        const val AI_TRIAL_USED = "aiTrialUsed"     // (server) free DutyPe AI actions used
        const val VERIFIED = "verified"               // (server)
        const val RATING = "rating"                   // (server)
        const val RATING_COUNT = "ratingCount"        // (server)
        const val TOTAL_HIRES = "totalHires"          // (server)
        const val BLOCKED = "blocked"                 // (server)
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"

        /** Keys inside the `subscription` map (server-written after payment verification or a campaign grant). */
        object Subscription {
            const val PLAN_ID = "planId"              // "" = none, "UNLIMITED_CAMPAIGN" = referral/launch grant
            const val STATUS = "status"               // NONE | ACTIVE | EXPIRED
            const val START_AT = "startAt"
            const val EXPIRES_AT = "expiresAt"
            const val CREDITS = "credits"             // map { normal, instant }
            const val CREDITS_NORMAL = "normal"
            const val CREDITS_INSTANT = "instant"
            const val AI = "ai"                         // the plan includes DutyPe AI
            const val AI_PER_DAY = "aiPerDay"           // DutyPe AI actions per day
        }

        /**
         * employer_profiles/{uid}/unlocks/{workerId} — workers whose phone this employer unlocked.
         * A subcollection, not an ever-growing array on the profile (Firestore's 1 MB doc limit).
         */
        object Unlocks {
            const val COLLECTION = "unlocks"
            const val JOB_ID = "jobId"                // job the contact was unlocked for
            const val CREATED_AT = "createdAt"
        }

        /** employer_profiles/{uid}/work_locations/{id} — saved hiring addresses (prefill job location). */
        object WorkLocations {
            const val COLLECTION = "work_locations"
            const val LABEL = "label"
            const val ADDRESS = "address"
            const val LAT = "lat"
            const val LNG = "lng"
        }
    }

    // ─────────────────────────────────── Jobs ───────────────────────────────────

    /** jobmetadata/{jobId} — the job CARD. Lists, map and search read only this (< 1 KB). */
    object Jobs {
        const val COLLECTION = "jobmetadata"
        const val EMPLOYER_ID = "employerId"
        const val TITLE = "title"
        const val CATEGORY = "category"               // required category key, e.g. DRIVER
        const val EMPLOYMENT_TYPE = "employmentType"  // Values.EmploymentType
        const val COMPANY_NAME = "companyName"        // businessName (COMPANY) or ownerName (INDIVIDUAL) at post time
        const val PHOTO_URL = "photoUrl"
        const val PAY_AMOUNT = "payAmount"            // rupees
        const val PAY_TYPE = "payType"                // Values.PayType
        const val VACANCIES = "vacancies"
        const val URGENCY = "urgency"                 // Values.Urgency
        const val SHIFT = "shift"                     // Values.Shift
        const val AREA = "area"
        const val LAT = "lat"
        const val LNG = "lng"
        const val GEOHASH = "geohash"                 // precision 9 (~5 m)
        /** Geohash precision 5 (~4.9 x 4.9 km). The feed's km bands are `cell in [...]` queries. */
        const val CELL = "cell"
        const val DISTRICT_ID = "districtId"          // (server) LGD district code, from lat/lng
        const val DISTRICT = "district"               // (server) district name, e.g. "Khammam"
        const val STATE_ID = "stateId"                // (server) LGD state code
        const val STATE = "state"                     // (server) state name, e.g. "Telangana"
        /** (server) lower-case words of title, company, area, district and category: search by `array-contains`. */
        const val KEYWORDS = "keywords"
        const val STATUS = "status"                   // Values.JobStatus (server)
        const val APPLICATION_COUNT = "applicationCount"  // (server)
        const val CREATED_AT = "createdAt"
        const val EXPIRES_AT = "expiresAt"
    }

    /**
     * employer_cards/{uid} — (server) the public face of an employer (no phone, GSTIN or billing),
     * synced from employer_profiles. Used wherever someone else's employer name/photo is shown.
     */
    object EmployerCards {
        const val COLLECTION = "employer_cards"
        const val NAME = "name"                       // business name for companies, else owner name
        const val PHOTO_URL = "photoUrl"
        const val AREA = "area"
        const val VERIFIED = "verified"
        const val RATING = "rating"
        const val RATING_COUNT = "ratingCount"
    }

    /** job_details/{jobId} — ONLY what the card doesn't have. Read when a job is opened. */
    object JobDetails {
        const val COLLECTION = "job_details"
        const val EMPLOYER_ID = "employerId"          // security rules check ownership without an extra read
        const val DESCRIPTION = "description"
        const val ADDRESS_TEXT = "addressText"
        const val GENDER = "gender"
        const val EXPERIENCE_REQUIRED = "experienceRequired"
        const val EDUCATION_REQUIRED = "educationRequired"
        const val BENEFITS = "benefits"
    }

    /**
     * job_contacts/{jobId} — the employer's number for a job. Only the employer can read it; a
     * worker gets it from `applyToJob(viaCall)`, which also records the call as an application.
     */
    object JobContacts {
        const val COLLECTION = "job_contacts"
        const val EMPLOYER_ID = "employerId"
        const val CONTACT_NUMBER = "contactNumber"
    }

    /**
     * applications/{jobId_workerId}. Job details are NOT copied here: the worker's My Jobs reads the
     * live job cards (30 per query), which also gives the current open/filled/expired status.
     * The worker snapshot lets the employer's applicant list render without reading each worker.
     */
    object Applications {
        const val COLLECTION = "applications"
        const val JOB_ID = "jobId"
        const val WORKER_ID = "workerId"
        const val EMPLOYER_ID = "employerId"
        const val STATUS = "status"                   // Values.ApplicationStatus
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val HIRED_AT = "hiredAt"                // auto-complete runs from this
        const val COMPLETED_AT = "completedAt"
        const val CALL_COUNT = "callCount"
        const val LAST_CALLED_AT = "lastCalledAt"
        // Worker snapshot at apply time
        const val WORKER_NAME = "workerName"
        const val WORKER_PHOTO = "workerPhoto"
        const val WORKER_SKILL = "workerSkill"
    }

    /** saved_jobs/{uid_jobId} */
    object SavedJobs {
        const val COLLECTION = "saved_jobs"
        const val USER_ID = "userId"
        const val JOB_ID = "jobId"
        const val CREATED_AT = "createdAt"
    }

    /** notifications/{id} — TTL on expireAt. One number = one role, so no role field. */
    object Notifications {
        const val COLLECTION = "notifications"
        const val RECIPIENT_ID = "recipientId"
        const val TITLE = "title"
        const val BODY = "body"
        const val TYPE = "type"
        const val DATA = "data"
        const val READ = "read"
        const val CREATED_AT = "createdAt"
        const val EXPIRE_AT = "expireAt"
    }

    /** ratings/{applicationId}_{raterRole} — jobId and roles are derivable from the id. */
    object Ratings {
        const val COLLECTION = "ratings"
        const val RATER_ID = "raterId"
        const val TARGET_ID = "targetId"
        const val STARS = "stars"
        const val REVIEW = "review"
        const val TAGS = "tags"
        const val CREATED_AT = "createdAt"
    }

    /** instant_requests/{id} (+ /responses/{workerId}) — urgent same-day needs. */
    object InstantRequests {
        const val COLLECTION = "instant_requests"
        const val EMPLOYER_ID = "employerId"
        const val TITLE = "title"
        const val CATEGORY = "category"
        const val WORKERS_NEEDED = "workersNeeded"
        const val PAY_PER_PERSON = "payPerPerson"
        const val DURATION_TEXT = "durationText"
        const val SCHEDULED_AT = "scheduledAt"
        const val AREA = "area"
        const val ADDRESS_TEXT = "addressText"
        const val LAT = "lat"
        const val LNG = "lng"
        const val GEOHASH = "geohash"
        const val CELL = "cell"                     // geohash5 (~4.9 km): the worker list queries `cell in [...]`
        const val RADIUS_KM = "radiusKm"
        const val CONTACT_NUMBER = "contactNumber"
        const val STATUS = "status"
        const val SELECTED_WORKER_IDS = "selectedWorkerIds"
        const val RESPONSE_COUNT = "responseCount"   // (server)
        const val DISPATCH_RADIUS_KM = "dispatchRadiusKm" // (server) offers pushed up to this many km (5/10/15/20)
        const val NEXT_WAVE_AT = "nextWaveAt"         // (server) when the next wider wave goes out
        const val CREATED_AT = "createdAt"
        const val EXPIRES_AT = "expiresAt"

        object Responses {
            const val COLLECTION = "responses"      // id = workerId
            const val WORKER_ID = "workerId"        // same as the id; lets a worker list their responses (collection group)
            const val STATUS = "status"
            const val WORKER_NAME = "workerName"
            const val CREATED_AT = "createdAt"
        }
    }

    // ──────────────────────────── Referrals & money ─────────────────────────────

    /** referral_codes/{CODE} — code -> owner lookup when someone types a code. */
    object ReferralCodes {
        const val COLLECTION = "referral_codes"
        const val UID = "uid"
        const val ROLE = "role"                     // employer rewards differ
        const val ACTIVE = "active"
    }

    /** referrals/{refereeUid} — one referral per user, enforced by the id. */
    object Referrals {
        const val COLLECTION = "referrals"
        const val REFERRER_UID = "referrerUid"
        const val CODE = "code"
        const val STATUS = "status"                 // Values.ReferralStatus
        const val FRAUD_SCORE = "fraudScore"
        const val CREATED_AT = "createdAt"
        const val COMPLETED_AT = "completedAt"
        const val EXPIRES_AT = "expiresAt"
    }

    /** referral_stats/{uid} — the wallet (server-only). Pending count = count query on referrals. */
    object Wallets {
        const val COLLECTION = "referral_stats"
        const val REFERRAL_CODE = "referralCode"    // the user's own code (uid -> code)
        const val BALANCE_PAISE = "balancePaise"
        const val LIFETIME_EARNED_PAISE = "lifetimeEarnedPaise"
        const val WITHDRAWN_PAISE = "withdrawnPaise"
        const val SUCCESSFUL_REFERRALS = "successfulReferrals"  // milestone maths inside transactions
        const val AWARDED_MILESTONES = "awardedMilestones"
        const val BLOCKED = "blocked"
        const val UPDATED_AT = "updatedAt"
    }

    /** wallet_ledger/{eventId} — append-only money entries; id = the event, so no double credit. */
    object WalletLedger {
        const val COLLECTION = "wallet_ledger"
        const val UID = "uid"
        const val TYPE = "type"                     // Values.LedgerType (welcome bonus id = welcome_{campaign}_{uid})
        const val AMOUNT_PAISE = "amountPaise"      // + credit / - debit
        const val REF_ID = "refId"
        const val BALANCE_AFTER_PAISE = "balanceAfterPaise"
        const val CREATED_AT = "createdAt"
    }

    object Withdrawals {
        const val COLLECTION = "withdrawal_requests"
        const val UID = "uid"
        const val AMOUNT_PAISE = "amountPaise"
        const val UPI_ID = "upiId"
        const val STATUS = "status"                 // Values.WithdrawalStatus
        const val TXN_REF = "txnRef"
        const val FAILURE_REASON = "failureReason"
        const val CREATED_AT = "createdAt"
        const val PROCESSED_AT = "processedAt"
    }

    /** withdrawal_daily/{uid}_{yyyy-mm-dd} — per-day withdrawal limit counter (server; TTL on expireAt). */
    object WithdrawalDaily {
        const val COLLECTION = "withdrawal_daily"
        const val AMOUNT_PAISE = "amountPaise"
        const val REQUEST_COUNT = "requestCount"
        const val EXPIRE_AT = "expireAt"
    }

    // ─────────────────────────────── Subscriptions ──────────────────────────────

    /** subscription_payment_requests/{id} — employer UPI payment proof, verified by admin. */
    object SubscriptionPayments {
        const val COLLECTION = "subscription_payment_requests"
        const val EMPLOYER_ID = "employerId"
        const val PLAN_ID = "planId"
        const val AMOUNT_PAISE = "amountPaise"
        const val UPI_ID_USED = "upiIdUsed"
        const val UTR_NUMBER = "utrNumber"
        const val SCREENSHOT_URL = "screenshotUrl"
        const val STATUS = "status"                 // PENDING | VERIFIED | REJECTED
        const val REJECTION_REASON = "rejectionReason"
        const val CREATED_AT = "createdAt"
        const val VERIFIED_AT = "verifiedAt"
    }

    /** active_qr_codes/{id} — UPI QR images shown on the subscription screen. */
    object PaymentQrCodes {
        const val COLLECTION = "active_qr_codes"
        const val IMAGE_URL = "imageUrl"
        const val LABEL = "label"
        const val ACTIVE = "active"
        const val CREATED_AT = "createdAt"
    }

    // ───────────────────────────────── Config ───────────────────────────────────

    /** app_config/{docId} — admin-editable config read by the app (public get). */
    object AppConfig {
        const val COLLECTION = "app_config"
        const val DOC_REFERRAL = "referral"
        const val DOC_APP_UPDATE = "app_update"
        const val DOC_DYNAMIC_FEATURES = "dynamic_features"
        const val DOC_SUBSCRIPTION_PLANS = "subscription_plans"
    }

    /** announcements/{id} — in-app banners managed from the admin panel. */
    object Announcements {
        const val COLLECTION = "announcements"
        const val TITLE = "title"
        const val MESSAGE = "message"
        const val TYPE = "type"
        const val PRIORITY = "priority"
        const val TARGET_ROLE = "targetRole"        // WORKER | EMPLOYER | ALL
        const val ACTION_ROUTE = "actionRoute"
        const val ACTIVE = "active"
        const val CREATED_AT = "createdAt"
        const val EXPIRES_AT = "expiresAt"
    }

    /** location_demand/{auto} — "tell me when DutyPe launches here" leads (create-only; admin reads). */
    object LocationDemand {
        const val COLLECTION = "location_demand"
        const val UID = "uid"                     // "" for guests
        const val AREA = "area"
        const val LAT = "lat"
        const val LNG = "lng"
        const val CATEGORY = "category"
        const val CREATED_AT = "createdAt"
    }

    /** idempotency/{uid_op_key} — callable replay protection (server only; TTL on expireAt). */
    object Idempotency {
        const val COLLECTION = "idempotency"
        const val RESULT = "result"
        const val EXPIRE_AT = "expireAt"
    }

    // ───────────────────────────────── Values ───────────────────────────────────

    /** Allowed values for enum-like fields. */
    object Values {
        object Role {
            const val WORKER = "WORKER"
            const val EMPLOYER = "EMPLOYER"
        }
        object EmployerType {
            const val INDIVIDUAL = "INDIVIDUAL"
            const val COMPANY = "COMPANY"
        }
        object EmploymentType {
            const val FULL_TIME = "FULL_TIME"
            const val PART_TIME = "PART_TIME"
            const val DAILY = "DAILY"
        }
        object PayType {
            const val DAILY = "DAILY"
            const val WEEKLY = "WEEKLY"
            const val MONTHLY = "MONTHLY"
            const val HOURLY = "HOURLY"
            const val NEGOTIABLE = "NEGOTIABLE"
        }
        object Urgency {
            const val NORMAL = "NORMAL"
            const val HIGH = "HIGH"
        }
        object Shift {
            const val DAY = "DAY"
            const val NIGHT = "NIGHT"
            const val ANY = "ANY"
        }
        object JobStatus {
            const val OPEN = "open"
            const val FILLED = "filled"
            const val CLOSED = "closed"
            const val EXPIRED = "expired"
        }
        object ApplicationStatus {
            const val APPLIED = "applied"
            const val HIRED = "hired"
            const val COMPLETED = "completed"
            const val REJECTED = "rejected"
            const val WITHDRAWN = "withdrawn"
        }
        object ReferralStatus {
            const val PENDING = "PENDING"
            const val COMPLETED = "COMPLETED"
            const val REJECTED = "REJECTED"
            const val EXPIRED = "EXPIRED"
        }
        object LedgerType {
            const val REFERRAL_REWARD = "REFERRAL_REWARD"
            const val SIGNUP_BONUS = "SIGNUP_BONUS"
            const val WELCOME_BONUS = "WELCOME_BONUS"
            const val MILESTONE = "MILESTONE"
            const val WITHDRAWAL = "WITHDRAWAL"
            const val REFUND = "REFUND"
            const val ADJUSTMENT = "ADJUSTMENT"
        }
        object WithdrawalStatus {
            const val PENDING = "PENDING"
            const val PROCESSING = "PROCESSING"
            const val COMPLETED = "COMPLETED"
            const val FAILED = "FAILED"
        }
    }
}
