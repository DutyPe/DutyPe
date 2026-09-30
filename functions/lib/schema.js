"use strict";
/**
 * DutyPe Firestore schema — server mirror of
 * app/src/main/java/com/example/dutype/firestore/FirestoreSchema.kt. Change both together.
 *
 * Also imported by the web admin (web/lib/firebase/schema.ts re-exports this file's shape).
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.CATEGORY_KEYS = exports.Values = exports.Idempotency = exports.LocationDemand = exports.Feedback = exports.Announcements = exports.AppConfig = exports.PaymentQrCodes = exports.SubscriptionPayments = exports.WithdrawalDaily = exports.Withdrawals = exports.WalletLedger = exports.Wallets = exports.Referrals = exports.ReferralCodes = exports.InstantRequests = exports.JobReports = exports.Ratings = exports.Notifications = exports.SavedJobs = exports.Applications = exports.JobCells = exports.JobDetails = exports.Jobs = exports.EmployerProfiles = exports.WorkerCards = exports.WorkerProfiles = exports.UserTokens = exports.PhoneRoles = void 0;
exports.PhoneRoles = {
    COLLECTION: "phoneRoles",
    UID: "uid",
    ROLE: "role",
};
exports.UserTokens = {
    COLLECTION: "user_tokens",
    FCM_TOKEN: "fcmToken",
    LANGUAGE: "language",
    UPDATED_AT: "updatedAt",
};
exports.WorkerProfiles = {
    COLLECTION: "worker_profiles",
    NAME: "name",
    PHONE: "phone",
    PHOTO_URL: "photoUrl",
    GENDER: "gender",
    DATE_OF_BIRTH: "dateOfBirth",
    EDUCATION: "education",
    EXPERIENCE_YEARS: "experienceYears",
    SKILLS: "skills",
    BIO: "bio",
    ADDRESS: "address",
    AREA: "area",
    LAT: "lat",
    LNG: "lng",
    GEOHASH: "geohash",
    AVAILABLE: "available",
    BLOCKED: "blocked",
    CREATED_AT: "createdAt",
    UPDATED_AT: "updatedAt",
};
exports.WorkerCards = {
    COLLECTION: "worker_cards",
    NAME: "name",
    PHOTO_URL: "photoUrl",
    SKILLS: "skills",
    EXPERIENCE_YEARS: "experienceYears",
    AREA: "area",
    LAT: "lat",
    LNG: "lng",
    GEOHASH: "geohash",
    AVAILABLE: "available",
    RATING: "rating",
    RATING_COUNT: "ratingCount",
    JOBS_COMPLETED: "jobsCompleted",
    LAST_ACTIVE_AT: "lastActiveAt",
};
exports.EmployerProfiles = {
    COLLECTION: "employer_profiles",
    EMPLOYER_TYPE: "employerType",
    OWNER_NAME: "ownerName",
    BUSINESS_NAME: "businessName",
    BUSINESS_TYPE: "businessType",
    GSTIN: "gstin",
    PHONE: "phone",
    PHOTO_URL: "photoUrl",
    ADDRESS: "address",
    AREA: "area",
    LAT: "lat",
    LNG: "lng",
    GEOHASH: "geohash",
    SUBSCRIPTION: "subscription",
    FREE_URGENT_POSTS_USED: "freeUrgentPostsUsed",
    VERIFIED: "verified",
    RATING: "rating",
    RATING_COUNT: "ratingCount",
    TOTAL_HIRES: "totalHires",
    BLOCKED: "blocked",
    CREATED_AT: "createdAt",
    UPDATED_AT: "updatedAt",
    Subscription: {
        PLAN_ID: "planId",
        STATUS: "status",
        START_AT: "startAt",
        EXPIRES_AT: "expiresAt",
        CREDITS: "credits",
        CREDITS_NORMAL: "normal",
        CREDITS_INSTANT: "instant",
    },
    Unlocks: {
        COLLECTION: "unlocks",
        JOB_ID: "jobId",
        CREATED_AT: "createdAt",
    },
    WorkLocations: {
        COLLECTION: "work_locations",
        LABEL: "label",
        ADDRESS: "address",
        LAT: "lat",
        LNG: "lng",
    },
};
exports.Jobs = {
    COLLECTION: "jobmetadata",
    EMPLOYER_ID: "employerId",
    TITLE: "title",
    CATEGORY: "category",
    EMPLOYMENT_TYPE: "employmentType",
    COMPANY_NAME: "companyName",
    PHOTO_URL: "photoUrl",
    PAY_AMOUNT: "payAmount",
    PAY_TYPE: "payType",
    VACANCIES: "vacancies",
    URGENCY: "urgency",
    SHIFT: "shift",
    AREA: "area",
    LAT: "lat",
    LNG: "lng",
    GEOHASH: "geohash",
    STATUS: "status",
    APPLICATION_COUNT: "applicationCount",
    CREATED_AT: "createdAt",
    EXPIRES_AT: "expiresAt",
};
exports.JobDetails = {
    COLLECTION: "job_details",
    EMPLOYER_ID: "employerId",
    DESCRIPTION: "description",
    ADDRESS_TEXT: "addressText",
    CONTACT_NUMBER: "contactNumber",
    GENDER: "gender",
    EXPERIENCE_REQUIRED: "experienceRequired",
    EDUCATION_REQUIRED: "educationRequired",
    BENEFITS: "benefits",
};
exports.JobCells = {
    COLLECTION: "job_cells",
    CELLS: "cells",
    TOTAL: "_",
    URGENT: "URGENT",
    UPDATED_AT: "updatedAt",
    REGION_PRECISION: 4,
    CELL_PRECISION: 6,
};
exports.Applications = {
    COLLECTION: "applications",
    JOB_ID: "jobId",
    WORKER_ID: "workerId",
    EMPLOYER_ID: "employerId",
    STATUS: "status",
    CREATED_AT: "createdAt",
    UPDATED_AT: "updatedAt",
    HIRED_AT: "hiredAt",
    COMPLETED_AT: "completedAt",
    CALL_COUNT: "callCount",
    LAST_CALLED_AT: "lastCalledAt",
    WORKER_NAME: "workerName",
    WORKER_PHOTO: "workerPhoto",
    WORKER_SKILL: "workerSkill",
};
exports.SavedJobs = {
    COLLECTION: "saved_jobs",
    USER_ID: "userId",
    JOB_ID: "jobId",
    CREATED_AT: "createdAt",
};
exports.Notifications = {
    COLLECTION: "notifications",
    RECIPIENT_ID: "recipientId",
    TITLE: "title",
    BODY: "body",
    TYPE: "type",
    DATA: "data",
    READ: "read",
    CREATED_AT: "createdAt",
    EXPIRE_AT: "expireAt",
};
exports.Ratings = {
    COLLECTION: "ratings",
    RATER_ID: "raterId",
    TARGET_ID: "targetId",
    STARS: "stars",
    REVIEW: "review",
    TAGS: "tags",
    CREATED_AT: "createdAt",
};
exports.JobReports = {
    COLLECTION: "job_reports",
    JOB_ID: "jobId",
    REPORTER_ID: "reporterId",
    REASON: "reason",
    NOTE: "note",
    STATUS: "status",
    CREATED_AT: "createdAt",
};
exports.InstantRequests = {
    COLLECTION: "instant_requests",
    EMPLOYER_ID: "employerId",
    TITLE: "title",
    CATEGORY: "category",
    WORKERS_NEEDED: "workersNeeded",
    PAY_PER_PERSON: "payPerPerson",
    DURATION_TEXT: "durationText",
    SCHEDULED_AT: "scheduledAt",
    AREA: "area",
    ADDRESS_TEXT: "addressText",
    LAT: "lat",
    LNG: "lng",
    GEOHASH: "geohash",
    RADIUS_KM: "radiusKm",
    CONTACT_NUMBER: "contactNumber",
    STATUS: "status",
    SELECTED_WORKER_IDS: "selectedWorkerIds",
    RESPONSE_COUNT: "responseCount",
    CREATED_AT: "createdAt",
    EXPIRES_AT: "expiresAt",
    Responses: {
        COLLECTION: "responses",
        WORKER_ID: "workerId",
        STATUS: "status",
        WORKER_NAME: "workerName",
        CREATED_AT: "createdAt",
    },
};
exports.ReferralCodes = {
    COLLECTION: "referral_codes",
    UID: "uid",
    ROLE: "role",
    ACTIVE: "active",
};
exports.Referrals = {
    COLLECTION: "referrals",
    REFERRER_UID: "referrerUid",
    CODE: "code",
    STATUS: "status",
    FRAUD_SCORE: "fraudScore",
    CREATED_AT: "createdAt",
    COMPLETED_AT: "completedAt",
    EXPIRES_AT: "expiresAt",
};
exports.Wallets = {
    COLLECTION: "referral_stats",
    REFERRAL_CODE: "referralCode",
    BALANCE_PAISE: "balancePaise",
    LIFETIME_EARNED_PAISE: "lifetimeEarnedPaise",
    WITHDRAWN_PAISE: "withdrawnPaise",
    SUCCESSFUL_REFERRALS: "successfulReferrals",
    AWARDED_MILESTONES: "awardedMilestones",
    BLOCKED: "blocked",
    UPDATED_AT: "updatedAt",
};
exports.WalletLedger = {
    COLLECTION: "wallet_ledger",
    UID: "uid",
    TYPE: "type",
    AMOUNT_PAISE: "amountPaise",
    REF_ID: "refId",
    BALANCE_AFTER_PAISE: "balanceAfterPaise",
    CREATED_AT: "createdAt",
};
exports.Withdrawals = {
    COLLECTION: "withdrawal_requests",
    UID: "uid",
    AMOUNT_PAISE: "amountPaise",
    UPI_ID: "upiId",
    STATUS: "status",
    TXN_REF: "txnRef",
    FAILURE_REASON: "failureReason",
    CREATED_AT: "createdAt",
    PROCESSED_AT: "processedAt",
};
exports.WithdrawalDaily = {
    COLLECTION: "withdrawal_daily",
    AMOUNT_PAISE: "amountPaise",
    REQUEST_COUNT: "requestCount",
    EXPIRE_AT: "expireAt",
};
exports.SubscriptionPayments = {
    COLLECTION: "subscription_payment_requests",
    EMPLOYER_ID: "employerId",
    PLAN_ID: "planId",
    AMOUNT_PAISE: "amountPaise",
    UPI_ID_USED: "upiIdUsed",
    UTR_NUMBER: "utrNumber",
    SCREENSHOT_URL: "screenshotUrl",
    STATUS: "status",
    REJECTION_REASON: "rejectionReason",
    CREATED_AT: "createdAt",
    VERIFIED_AT: "verifiedAt",
};
exports.PaymentQrCodes = {
    COLLECTION: "active_qr_codes",
    IMAGE_URL: "imageUrl",
    LABEL: "label",
    ACTIVE: "active",
    CREATED_AT: "createdAt",
};
exports.AppConfig = {
    COLLECTION: "app_config",
    DOC_REFERRAL: "referral",
    DOC_APP_UPDATE: "app_update",
    DOC_DYNAMIC_FEATURES: "dynamic_features",
    DOC_SUBSCRIPTION_PLANS: "subscription_plans",
};
exports.Announcements = {
    COLLECTION: "announcements",
    TITLE: "title",
    MESSAGE: "message",
    TYPE: "type",
    PRIORITY: "priority",
    TARGET_ROLE: "targetRole",
    ACTION_ROUTE: "actionRoute",
    ACTIVE: "active",
    CREATED_AT: "createdAt",
    EXPIRES_AT: "expiresAt",
};
exports.Feedback = {
    COLLECTION: "feedback",
    UID: "uid",
    ROLE: "role",
    RATING: "rating",
    CATEGORY: "category",
    TEXT: "text",
    APP_VERSION: "appVersion",
    CREATED_AT: "createdAt",
};
exports.LocationDemand = {
    COLLECTION: "location_demand",
    UID: "uid",
    AREA: "area",
    LAT: "lat",
    LNG: "lng",
    CATEGORY: "category",
    CREATED_AT: "createdAt",
};
exports.Idempotency = {
    COLLECTION: "idempotency",
    RESULT: "result",
    EXPIRE_AT: "expireAt",
};
exports.Values = {
    Role: { WORKER: "WORKER", EMPLOYER: "EMPLOYER" },
    EmployerType: { INDIVIDUAL: "INDIVIDUAL", COMPANY: "COMPANY" },
    EmploymentType: { FULL_TIME: "FULL_TIME", PART_TIME: "PART_TIME", DAILY: "DAILY" },
    PayType: { DAILY: "DAILY", MONTHLY: "MONTHLY", HOURLY: "HOURLY", NEGOTIABLE: "NEGOTIABLE" },
    Urgency: { NORMAL: "NORMAL", HIGH: "HIGH" },
    Shift: { DAY: "DAY", NIGHT: "NIGHT", ANY: "ANY" },
    JobStatus: { OPEN: "open", FILLED: "filled", CLOSED: "closed", EXPIRED: "expired" },
    ApplicationStatus: {
        APPLIED: "applied", HIRED: "hired", COMPLETED: "completed", REJECTED: "rejected", WITHDRAWN: "withdrawn",
    },
    ReferralStatus: { PENDING: "PENDING", COMPLETED: "COMPLETED", REJECTED: "REJECTED", EXPIRED: "EXPIRED" },
    LedgerType: {
        REFERRAL_REWARD: "REFERRAL_REWARD",
        SIGNUP_BONUS: "SIGNUP_BONUS",
        WELCOME_BONUS: "WELCOME_BONUS",
        MILESTONE: "MILESTONE",
        WITHDRAWAL: "WITHDRAWAL",
        REFUND: "REFUND",
        ADJUSTMENT: "ADJUSTMENT",
    },
    WithdrawalStatus: { PENDING: "PENDING", PROCESSING: "PROCESSING", COMPLETED: "COMPLETED", FAILED: "FAILED" },
    InstantStatus: { OPEN: "open", FILLED: "filled", COMPLETED: "completed", CANCELLED: "cancelled", EXPIRED: "expired" },
    InstantResponseStatus: {
        APPLIED: "applied", CALLED: "called", ACCEPTED: "accepted", REJECTED: "rejected",
        COMPLETED: "completed", NO_SHOW: "no_show", CANCELLED: "cancelled",
    },
};
exports.CATEGORY_KEYS = [
    "COOK", "MAID", "DRIVER", "HELPER", "SECURITY", "GARDENER", "CARETAKER", "DELIVERY", "WAITER",
    "ELECTRICIAN", "PLUMBER", "PAINTER", "CARPENTER", "RECEPTIONIST", "CASHIER", "PACKER", "SALES",
    "TELECALLER", "TEACHER", "OFFICE_STAFF", "CUSTOMER_SUPPORT", "FIELD_EXECUTIVE", "MARKETING", "FINANCE",
    "HEALTHCARE", "BEAUTICIAN", "TAILOR", "MECHANIC", "DATA_ENTRY", "LEGAL", "OTHER",
];
//# sourceMappingURL=schema.js.map