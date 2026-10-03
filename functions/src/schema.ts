/**
 * DutyPe Firestore schema — server mirror of
 * app/src/main/java/com/example/dutype/firestore/FirestoreSchema.kt. Change both together.
 *
 * Also imported by the web admin (web/lib/firebase/schema.ts re-exports this file's shape).
 */

export const PhoneRoles = {
  COLLECTION: "phoneRoles",
  UID: "uid",
  ROLE: "role",
} as const;

export const UserTokens = {
  COLLECTION: "user_tokens",
  FCM_TOKEN: "fcmToken",
  LANGUAGE: "language",
  UPDATED_AT: "updatedAt",
} as const;

export const WorkerProfiles = {
  COLLECTION: "worker_profiles",
  NAME: "name",
  PHONE: "phone",
  /** Optional; filled from Truecaller when the user shares it. */
  EMAIL: "email",
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
  /** Online switch: only online workers get urgent job offers. */
  AVAILABLE: "available",
  /** (server) the urgent need this worker accepted and has not finished — one at a time. */
  ACTIVE_URGENT_ID: "activeUrgentId",
  BLOCKED: "blocked",
  CREATED_AT: "createdAt",
  UPDATED_AT: "updatedAt",
} as const;

export const WorkerCards = {
  COLLECTION: "worker_cards",
  NAME: "name",
  PHOTO_URL: "photoUrl",
  SKILLS: "skills",
  EXPERIENCE_YEARS: "experienceYears",
  AREA: "area",
  /** Home point rounded to ~1 km (privacy). */
  LAT: "lat",
  LNG: "lng",
  CELL: "cell",
  DISTRICT_ID: "districtId",
  STATE_ID: "stateId",
  CURRENT_CELL: "currentCell",
  CURRENT_DISTRICT_ID: "currentDistrictId",
  /** "{SKILL}_{cell}" / "ANY_{cell}" for the home and current cells — employer matching. */
  SKILL_CELLS: "skillCells",
  /** "{SKILL}_{districtId}" / "ANY_{districtId}" for the home and current districts. */
  SKILL_DISTRICTS: "skillDistricts",
  AVAILABLE: "available",
  RATING: "rating",
  RATING_COUNT: "ratingCount",
  JOBS_COMPLETED: "jobsCompleted",
  /** (server) urgent jobs accepted but not turned up for — shown to employers. */
  NO_SHOWS: "noShows",
  LAST_ACTIVE_AT: "lastActiveAt",
} as const;

export const EmployerProfiles = {
  COLLECTION: "employer_profiles",
  EMPLOYER_TYPE: "employerType",
  OWNER_NAME: "ownerName",
  BUSINESS_NAME: "businessName",
  BUSINESS_TYPE: "businessType",
  GSTIN: "gstin",
  PHONE: "phone",
  /** Optional; filled from Truecaller when the user shares it. */
  EMAIL: "email",
  PHOTO_URL: "photoUrl",
  ADDRESS: "address",
  AREA: "area",
  LAT: "lat",
  LNG: "lng",
  GEOHASH: "geohash",
  SUBSCRIPTION: "subscription",
  FREE_URGENT_POSTS_USED: "freeUrgentPostsUsed",
  /** Free posts (normal or urgent) earned by referring; usable until [REFERRAL_FREE_POSTS_UNTIL]. */
  REFERRAL_FREE_POSTS: "referralFreePosts",
  REFERRAL_FREE_POSTS_UNTIL: "referralFreePostsUntil",
  /** (server) DutyPe AI actions used from the free trial (employers without an AI plan). */
  AI_TRIAL_USED: "aiTrialUsed",
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
    /** The plan includes DutyPe AI. */
    AI: "ai",
    /** DutyPe AI actions allowed per day on this plan. */
    AI_PER_DAY: "aiPerDay",
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
} as const;

/** Public face of an employer (no phone / GSTIN / billing), synced from employer_profiles. */
export const EmployerCards = {
  COLLECTION: "employer_cards",
  NAME: "name",
  PHOTO_URL: "photoUrl",
  AREA: "area",
  VERIFIED: "verified",
  RATING: "rating",
  RATING_COUNT: "ratingCount",
} as const;

/** The employer's number for a job; employer-only. Workers get it from applyToJob(viaCall). */
export const JobContacts = {
  COLLECTION: "job_contacts",
  EMPLOYER_ID: "employerId",
  CONTACT_NUMBER: "contactNumber",
} as const;

export const Jobs = {
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
  /** geohash precision 5 (~4.9 km): the feed's km bands query `cell in [...]`. */
  CELL: "cell",
  /** Set by the server from lat/lng (LGD codes + names). */
  DISTRICT_ID: "districtId",
  DISTRICT: "district",
  STATE_ID: "stateId",
  STATE: "state",
  /** (server) search words of title, company, area, district, category — `array-contains` search. */
  KEYWORDS: "keywords",
  STATUS: "status",
  APPLICATION_COUNT: "applicationCount",
  CREATED_AT: "createdAt",
  EXPIRES_AT: "expiresAt",
} as const;

export const JobDetails = {
  COLLECTION: "job_details",
  EMPLOYER_ID: "employerId",
  DESCRIPTION: "description",
  ADDRESS_TEXT: "addressText",
  GENDER: "gender",
  EXPERIENCE_REQUIRED: "experienceRequired",
  EDUCATION_REQUIRED: "educationRequired",
  BENEFITS: "benefits",
} as const;

export const Applications = {
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
} as const;

export const SavedJobs = {
  COLLECTION: "saved_jobs",
  USER_ID: "userId",
  JOB_ID: "jobId",
  CREATED_AT: "createdAt",
} as const;

export const Notifications = {
  COLLECTION: "notifications",
  RECIPIENT_ID: "recipientId",
  TITLE: "title",
  BODY: "body",
  TYPE: "type",
  DATA: "data",
  READ: "read",
  CREATED_AT: "createdAt",
  EXPIRE_AT: "expireAt",
} as const;

export const Ratings = {
  COLLECTION: "ratings",
  RATER_ID: "raterId",
  TARGET_ID: "targetId",
  STARS: "stars",
  REVIEW: "review",
  TAGS: "tags",
  CREATED_AT: "createdAt",
} as const;

export const InstantRequests = {
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
  /** geohash precision 5 (~4.9 km): workers' nearby list queries `cell in [...]`. */
  CELL: "cell",
  RADIUS_KM: "radiusKm",
  CONTACT_NUMBER: "contactNumber",
  STATUS: "status",
  SELECTED_WORKER_IDS: "selectedWorkerIds",
  RESPONSE_COUNT: "responseCount",
  /** (server) offers have been pushed to workers up to this many km (5 → 10 → 15 → 20). */
  DISPATCH_RADIUS_KM: "dispatchRadiusKm",
  /** (server) when the next, wider wave of offers goes out; removed when filled or at 20 km. */
  NEXT_WAVE_AT: "nextWaveAt",
  CREATED_AT: "createdAt",
  EXPIRES_AT: "expiresAt",
  Responses: {
    COLLECTION: "responses",
    WORKER_ID: "workerId",
    STATUS: "status",
    WORKER_NAME: "workerName",
    /** Why the employer removed the worker (shown to the worker). */
    REASON: "reason",
    CREATED_AT: "createdAt",
  },
} as const;

export const ReferralCodes = {
  COLLECTION: "referral_codes",
  UID: "uid",
  ROLE: "role",
  ACTIVE: "active",
} as const;

export const Referrals = {
  COLLECTION: "referrals",
  REFERRER_UID: "referrerUid",
  CODE: "code",
  STATUS: "status",
  FRAUD_SCORE: "fraudScore",
  CREATED_AT: "createdAt",
  COMPLETED_AT: "completedAt",
  EXPIRES_AT: "expiresAt",
} as const;

export const Wallets = {
  COLLECTION: "referral_stats",
  REFERRAL_CODE: "referralCode",
  BALANCE_PAISE: "balancePaise",
  LIFETIME_EARNED_PAISE: "lifetimeEarnedPaise",
  WITHDRAWN_PAISE: "withdrawnPaise",
  SUCCESSFUL_REFERRALS: "successfulReferrals",
  AWARDED_MILESTONES: "awardedMilestones",
  BLOCKED: "blocked",
  UPDATED_AT: "updatedAt",
} as const;

export const WalletLedger = {
  COLLECTION: "wallet_ledger",
  UID: "uid",
  TYPE: "type",
  AMOUNT_PAISE: "amountPaise",
  REF_ID: "refId",
  BALANCE_AFTER_PAISE: "balanceAfterPaise",
  CREATED_AT: "createdAt",
} as const;

export const Withdrawals = {
  COLLECTION: "withdrawal_requests",
  UID: "uid",
  AMOUNT_PAISE: "amountPaise",
  UPI_ID: "upiId",
  STATUS: "status",
  TXN_REF: "txnRef",
  FAILURE_REASON: "failureReason",
  CREATED_AT: "createdAt",
  PROCESSED_AT: "processedAt",
} as const;

export const WithdrawalDaily = {
  COLLECTION: "withdrawal_daily",
  AMOUNT_PAISE: "amountPaise",
  REQUEST_COUNT: "requestCount",
  EXPIRE_AT: "expireAt",
} as const;

export const SubscriptionPayments = {
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
} as const;

export const PaymentQrCodes = {
  COLLECTION: "active_qr_codes",
  IMAGE_URL: "imageUrl",
  LABEL: "label",
  ACTIVE: "active",
  CREATED_AT: "createdAt",
} as const;

export const AppConfig = {
  COLLECTION: "app_config",
  DOC_REFERRAL: "referral",
  DOC_APP_UPDATE: "app_update",
  DOC_DYNAMIC_FEATURES: "dynamic_features",
  DOC_SUBSCRIPTION_PLANS: "subscription_plans",
  /** DutyPe Services settings: fees, commission, UPI for partner top-ups, price overrides. */
  DOC_SERVICES: "services",
} as const;

/**
 * service_bookings/{id} — DutyPe Services (Urban Company style home services). Created and changed
 * only by Cloud Functions (services.ts); the customer and the assigned partner can read it.
 */
export const ServiceBookings = {
  COLLECTION: "service_bookings",
  CUSTOMER_ID: "customerId",
  CUSTOMER_NAME: "customerName",
  CUSTOMER_PHONE: "customerPhone",
  CATEGORY: "category",
  SERVICE_ID: "serviceId",
  SERVICE_NAME: "serviceName",
  /** Rupees, fixed catalog price at booking time. */
  PRICE: "price",
  /** Rupees, DutyPe booking fee the customer pays (collected by the partner, debited from credits). */
  BOOKING_FEE: "bookingFee",
  COMMISSION_PCT: "commissionPct",
  INSPECTION: "inspection",
  ADDRESS_TEXT: "addressText",
  AREA: "area",
  LAT: "lat",
  LNG: "lng",
  NOTE: "note",
  /** "now" or "scheduled". */
  WHEN: "when",
  SCHEDULED_AT: "scheduledAt",
  STATUS: "status",
  PARTNER_ID: "partnerId",
  PARTNER_NAME: "partnerName",
  PARTNER_PHONE: "partnerPhone",
  PARTNER_PHOTO_URL: "partnerPhotoUrl",
  PARTNER_RATING: "partnerRating",
  /** Partners who cancelled this booking; never offered it again. */
  EXCLUDED_PARTNER_IDS: "excludedPartnerIds",
  DISPATCH_RADIUS_KM: "dispatchRadiusKm",
  NEXT_WAVE_AT: "nextWaveAt",
  EXPIRES_AT: "expiresAt",
  /** Rupees off for the customer (first-booking offer or coupon); capped at DutyPe's take. */
  DISCOUNT: "discount",
  DISCOUNT_LABEL: "discountLabel",
  COUPON_CODE: "couponCode",
  /** Rupees charged to the partner who accepted (flat fee; first job free). */
  PARTNER_FEE: "partnerFee",
  /** Rupees added at completion for extra work / parts the customer approved. */
  EXTRAS: "extras",
  EXTRAS_NOTE: "extrasNote",
  /** Rupees the customer pays the partner: price + bookingFee + extras. */
  TOTAL: "total",
  /** Paise DutyPe took from the partner's credits (fee + commission). */
  PLATFORM_TAKE_PAISE: "platformTakePaise",
  RATING: "rating",
  REVIEW: "review",
  CANCELLED_BY: "cancelledBy",
  CANCEL_REASON: "cancelReason",
  CREATED_AT: "createdAt",
  ASSIGNED_AT: "assignedAt",
  STARTED_AT: "startedAt",
  COMPLETED_AT: "completedAt",
  UPDATED_AT: "updatedAt",
} as const;

/** coupon_uses/{uid}_{CODE} — one use of a coupon per customer (deleted if the booking is cancelled). */
export const CouponUses = {
  COLLECTION: "coupon_uses",
  BOOKING_ID: "bookingId",
  CREATED_AT: "createdAt",
} as const;

/** service_booking_secrets/{bookingId} — the start code only the customer can read. */
export const ServiceBookingSecrets = {
  COLLECTION: "service_booking_secrets",
  CUSTOMER_ID: "customerId",
  START_OTP: "startOtp",
} as const;

/** service_partners/{uid} — verified service partners (workers). Server-written; the partner reads own. */
export const ServicePartners = {
  COLLECTION: "service_partners",
  /** PENDING | APPROVED | REJECTED | SUSPENDED */
  STATUS: "status",
  NAME: "name",
  PHONE: "phone",
  PHOTO_URL: "photoUrl",
  CATEGORIES: "categories",
  EXPERIENCE_YEARS: "experienceYears",
  AREA: "area",
  NOTE: "note",
  /** How the partner can show skill for SKILLED categories (ITI, past shop, photos...). */
  SKILL_PROOF: "skillProof",
  /** Requested categories that need a skill check before approval. */
  SKILLED_CATEGORIES: "skilledCategories",
  /** When the partner accepted the DutyPe partner code of conduct. */
  GUIDELINES_ACCEPTED_AT: "guidelinesAcceptedAt",
  ONLINE: "online",
  LAT: "lat",
  LNG: "lng",
  LAST_SEEN_AT: "lastSeenAt",
  /** Paise of prepaid credits (fees and commission are taken from here; can go below 0 after extras). */
  CREDITS_PAISE: "creditsPaise",
  ACTIVE_BOOKING_ID: "activeBookingId",
  RATING_SUM: "ratingSum",
  RATING_COUNT: "ratingCount",
  JOBS_COMPLETED: "jobsCompleted",
  CANCELLATIONS: "cancellations",
  REJECTION_REASON: "rejectionReason",
  APPLIED_AT: "appliedAt",
  APPROVED_AT: "approvedAt",
  UPDATED_AT: "updatedAt",
} as const;

/** partner_topups/{id} — a partner paid DutyPe by UPI to add credits; an admin verifies the UTR. */
export const PartnerTopups = {
  COLLECTION: "partner_topups",
  PARTNER_ID: "partnerId",
  PARTNER_NAME: "partnerName",
  AMOUNT_PAISE: "amountPaise",
  UTR_NUMBER: "utrNumber",
  /** PENDING | VERIFIED | REJECTED */
  STATUS: "status",
  REJECTION_REASON: "rejectionReason",
  CREATED_AT: "createdAt",
  VERIFIED_AT: "verifiedAt",
} as const;

/** service_partners/{uid}/ledger/{id} — every credit change (top-up, job, adjustment). */
export const PartnerLedger = {
  SUBCOLLECTION: "ledger",
  AMOUNT_PAISE: "amountPaise",
  BALANCE_PAISE: "balancePaise",
  KIND: "kind",
  BOOKING_ID: "bookingId",
  TOPUP_ID: "topupId",
  NOTE: "note",
  CREATED_AT: "createdAt",
} as const;

export const Announcements = {
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
} as const;

export const LocationDemand = {
  COLLECTION: "location_demand",
  UID: "uid",
  AREA: "area",
  LAT: "lat",
  LNG: "lng",
  CATEGORY: "category",
  CREATED_AT: "createdAt",
} as const;

/** otp_codes/{+91…} — WhatsApp login codes (server only; hashed; TTL on expireAt). */
export const OtpCodes = {
  COLLECTION: "otp_codes",
  HASH: "hash",
  EXPIRES_AT: "expiresAt",
  ATTEMPTS: "attempts",
  /** "whatsapp" | "sms": where the current code went. */
  CHANNEL: "channel",
  /** The code, encrypted, so a resend within the window sends the same digits. */
  SEALED: "sealed",
  /** When this code was first made (a code lives at most 30 minutes). */
  ISSUED_AT: "issuedAt",
  LAST_SENT_AT: "lastSentAt",
  HOUR_START: "hourStart",
  HOUR_COUNT: "hourCount",
  DAY_KEY: "dayKey",
  DAY_COUNT: "dayCount",
  EXPIRE_AT: "expireAt",
} as const;

/** otp_ip/{hash(ip)_hour} — codes requested from one internet address in one hour (anti-abuse). */
export const OtpIp = {
  COLLECTION: "otp_ip",
  COUNT: "count",
  EXPIRE_AT: "expireAt",
} as const;

/** otp_daily/{YYYY-MM-DD} — WhatsApp codes sent per day across all users (spend cap). */
export const OtpDaily = {
  COLLECTION: "otp_daily",
  COUNT: "count",
  /** Codes sent by our SMS gateway that day (separate cap). */
  SMS_COUNT: "smsCount",
  EXPIRE_AT: "expireAt",
} as const;

/** truecaller_profiles/{uid} — what the user shared through Truecaller (server only). */
export const TruecallerProfiles = {
  COLLECTION: "truecaller_profiles",
  PHONE: "phone",
  NAME: "name",
  EMAIL: "email",
  UPDATED_AT: "updatedAt",
} as const;

export const Idempotency = {
  COLLECTION: "idempotency",
  RESULT: "result",
  EXPIRE_AT: "expireAt",
} as const;

export const Values = {
  Role: { WORKER: "WORKER", EMPLOYER: "EMPLOYER" },
  EmployerType: { INDIVIDUAL: "INDIVIDUAL", COMPANY: "COMPANY" },
  EmploymentType: { FULL_TIME: "FULL_TIME", PART_TIME: "PART_TIME", DAILY: "DAILY" },
  PayType: { DAILY: "DAILY", WEEKLY: "WEEKLY", MONTHLY: "MONTHLY", HOURLY: "HOURLY", NEGOTIABLE: "NEGOTIABLE" },
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
} as const;

/** Highest pay a job or urgent request may offer, in rupees, whatever the pay type. */
export const MAX_PAY_RUPEES = 50_000;

export const CATEGORY_KEYS = [
  "COOK", "MAID", "DRIVER", "HELPER", "SECURITY", "GARDENER", "CARETAKER", "DELIVERY", "WAITER",
  "ELECTRICIAN", "PLUMBER", "PAINTER", "CARPENTER", "RECEPTIONIST", "CASHIER", "PACKER", "SALES",
  "TELECALLER", "TEACHER", "OFFICE_STAFF", "CUSTOMER_SUPPORT", "FIELD_EXECUTIVE", "MARKETING", "FINANCE",
  "HEALTHCARE", "BEAUTICIAN", "TAILOR", "MECHANIC", "DATA_ENTRY", "LEGAL",
  // Student-friendly and hyper-local work (home tuition, events, in-store promoters, pharmacies, sites).
  "TUTOR", "EVENT_STAFF", "PROMOTER", "PHARMACY", "CONSTRUCTION", "OTHER",
] as const;
