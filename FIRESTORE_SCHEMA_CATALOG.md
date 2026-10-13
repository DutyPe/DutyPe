# DutyPe Complete Firestore Collections & Fields Catalog

> **Single Source of Truth**: This catalog documents the complete database schema implemented across the DutyPe Android Kotlin app (`app/`), Firebase Cloud Functions (`functions/`), and Next.js Web Admin panel (`web/`).
>
> **Project ID**: `dutype-860ac`

---

## 🧭 Architectural Principles & Invariants

1. **No Root `users/{uid}` Collection**:
   - Identity and user state are intentionally split into single-responsibility collections:
     - `phoneRoles`: O(1) phone-to-role lookup before authentication.
     - `user_tokens`: Device FCM tokens & UI localization settings.
     - `worker_profiles` / `employer_profiles`: Private role identity and settings.
     - `worker_cards` / `employer_cards`: Public, sanitized profile cards for search & matching.
     - `referrals` / `referral_stats`: Referral linkage, milestones, balances, and payouts.
2. **1 Phone Number = 1 Role Forever**:
   - A user cannot switch between Worker and Employer on the same phone number.
3. **Server-Only Mutations for Sensitive Data**:
   - Client applications (`firestore.rules`) cannot modify wallets, subscriptions, ratings, job applications, referral rewards, or counters. These are strictly written by Cloud Functions via the Firebase Admin SDK.

---

## 🏷️ Status Legend
- 🟢 **Live Now**: Actively queried, rendered, or mutated in production code right now.
- 🟡 **Useful for Future**: Designed and hooked for planned features (e.g., automated AI campaigns, Truecaller fast auth, scheduled shifts, partner subscriptions).
- 🔴 **Unnecessary / Redundant**: Field or collection that is unused, duplicated, or can be safely deprecated without impacting app behavior.

---

## Table of Contents
1. [User Identity & Roles](#1-user-identity--roles)
2. [Worker Profiles & Discovery](#2-worker-profiles--discovery)
3. [Employer Profiles & Locations](#3-employer-profiles--locations)
4. [Regular Job Vacancies & Applications](#4-regular-job-vacancies--applications)
5. [Urgent / Instant Hiring Needs](#5-urgent--instant-hiring-needs)
6. [Home & Commercial Services](#6-home--commercial-services)
7. [Referral & Earnings Engine](#7-referral--earnings-engine)
8. [Employer Subscriptions & Unlocks](#8-employer-subscriptions--unlocks)
9. [Notifications & Broadcasts](#9-notifications--broadcasts)
10. [WhatsApp Marketing & Automation](#10-whatsapp-marketing--automation)
11. [App Configuration & Dynamic Feeds](#11-app-configuration--dynamic-feeds)
12. [Field Operations & Lead Capture](#12-field-operations--lead-capture)
13. [Internal Security & Idempotency](#13-internal-security--idempotency)
14. [Detailed Audit: Unnecessary & Redundant Fields](#14-detailed-audit-unnecessary--redundant-fields)

---

## 1. User Identity & Roles

### 1.1 `phoneRoles`
- **Path**: `phoneRoles/{+91XXXXXXXXXX}`
- **Purpose**: Ultra-fast O(1) phone lookup prior to OTP verification and during app startup.
- **Fields**:
  - `uid` (`String`) — 🟢 **Live Now**: Firebase Auth UID.
  - `role` (`String`) — 🟢 **Live Now**: `"WORKER"` | `"EMPLOYER"`.

### 1.2 `user_tokens`
- **Path**: `user_tokens/{uid}`
- **Purpose**: Stores device push notification tokens and UI language.
- **Fields**:
  - `fcmToken` (`String`) — 🟢 **Live Now**: FCM registration token.
  - `language` (`String`) — 🟢 **Live Now**: `"en"` | `"te"` | `"hi"`. Used to send localized notifications.
  - `updatedAt` (`Timestamp`) — 🟢 **Live Now**: Last token refresh time.

### 1.3 `truecaller_profiles`
- **Path**: `truecaller_profiles/{phone}`
- **Purpose**: Cached Truecaller profile payload when logging in via Truecaller SDK.
- **Fields**:
  - `name` (`String`) — 🟡 **Useful for Future**: Pre-populates profile name.
  - `email` (`String`) — 🟡 **Useful for Future**: Pre-populates user email.
  - `verified` (`Boolean`) — 🟡 **Useful for Future**: Fast-track trust badge.
  - `updatedAt` (`Timestamp`) — 🟡 **Useful for Future**: Cache freshness timestamp.

---

## 2. Worker Profiles & Discovery

### 2.1 `worker_profiles`
- **Path**: `worker_profiles/{uid}`
- **Purpose**: Private worker profile record containing personal details, KYC, coordinates, and status.
- **Fields**:
  - `name` (`String`) — 🟢 **Live Now**: Full name of the worker.
  - `phone` (`String`) — 🟢 **Live Now**: Mobile number.
  - `email` (`String`) — 🟡 **Useful for Future**: Optional email address.
  - `photoUrl` (`String`) — 🟢 **Live Now**: Worker profile picture / selfie.
  - `gender` (`String`) — 🟢 **Live Now**: `"MALE"` | `"FEMALE"` | `"OTHER"`.
  - `dateOfBirth` (`String`) — 🟢 **Live Now**: Used for birthday greetings and age verification.
  - `education` (`String`) — 🟢 **Live Now**: Qualification level.
  - `experienceYears` (`Number`) — 🟢 **Live Now**: Total years of working experience.
  - `skills` (`List<String>`) — 🟢 **Live Now**: List of standardized job skills.
  - `bio` (`String`) — 🟢 **Live Now**: Short worker self-introduction.
  - `address` (`String`) — 🟢 **Live Now**: Full home address text.
  - `area` (`String`) — 🟢 **Live Now**: Short locality name for display.
  - `lat` (`Number`) — 🟢 **Live Now**: GPS latitude.
  - `lng` (`Number`) — 🟢 **Live Now**: GPS longitude.
  - `geohash` (`String`) — 🟢 **Live Now**: Geohash precision 9 for location search.
  - `available` (`Boolean`) — 🟢 **Live Now**: Online/Offline toggle for urgent job calls.
  - `activeUrgentId` (`String`) — 🟢 **Live Now**: ID of urgent job currently accepted (server-written).
  - `blocked` (`Boolean`) — 🟢 **Live Now**: Admin ban flag.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Profile registration timestamp.
  - `updatedAt` (`Timestamp`) — 🟢 **Live Now**: Last profile update timestamp.

### 2.2 `worker_cards`
- **Path**: `worker_cards/{uid}`
- **Purpose**: Public, anonymized profile cards readable by employers for searching and worker matching.
- **Fields**:
  - `name` (`String`) — 🟢 **Live Now**: Worker display name.
  - `photoUrl` (`String`) — 🟢 **Live Now**: Profile photo.
  - `skills` (`List<String>`) — 🟢 **Live Now**: Worker skill tags.
  - `experienceYears` (`Number`) — 🟢 **Live Now**: Experience years.
  - `area` (`String`) — 🟢 **Live Now**: Locality name (never full private address).
  - `lat` (`Number`) — 🟢 **Live Now**: Home point rounded to ~1 km for privacy.
  - `lng` (`Number`) — 🟢 **Live Now**: Home point rounded to ~1 km for privacy.
  - `cell` (`String`) — 🟢 **Live Now**: S2 / Geohash cell identifier for rapid spatial search.
  - `districtId` (`String`) — 🟢 **Live Now**: District identifier.
  - `stateId` (`String`) — 🟢 **Live Now**: State identifier.
  - `currentCell` (`String`) — 🟡 **Useful for Future**: Real-time roaming cell.
  - `currentDistrictId` (`String`) — 🟡 **Useful for Future**: Real-time roaming district.
  - `skillCells` (`List<String>`) — 🟢 **Live Now**: Array of `"{SKILL}_{cell}"` for compound index queries.
  - `skillDistricts` (`List<String>`) — 🟢 **Live Now**: Array of `"{SKILL}_{district}"`.
  - `available` (`Boolean`) — 🟢 **Live Now**: Active status.
  - `rating` (`Number`) — 🟢 **Live Now**: Average rating score (0.0 - 5.0).
  - `ratingCount` (`Number`) — 🟢 **Live Now**: Number of completed ratings received.
  - `jobsCompleted` (`Number`) — 🟢 **Live Now**: Completed job count.
  - `noShows` (`Number`) — 🟢 **Live Now**: Urgent jobs accepted but failed to appear.
  - `lastActiveAt` (`Timestamp`) — 🟢 **Live Now**: Last app active ping.

---

## 3. Employer Profiles & Locations

### 3.1 `employer_profiles`
- **Path**: `employer_profiles/{uid}`
- **Purpose**: Authoritative employer record for hiring, billing, and profile data.
- **Fields**:
  - `employerType` (`String`) — 🟢 **Live Now**: `"INDIVIDUAL"` | `"COMPANY"`.
  - `ownerName` (`String`) — 🟢 **Live Now**: Full name of employer/owner.
  - `businessName` (`String`) — 🟢 **Live Now**: Shop / Store / Business name (optional).
  - `businessType` (`String`) — 🔴 **Removed**: Removed from collection schemas, security rules, Cloud Functions, and app profile setups. Category is dynamically inferred from job titles.
  - `gstin` (`String`) — 🔴 **Removed**: Removed from collection schemas, security rules, Cloud Functions, and app screens. Hyper-local blue-collar hiring requires zero GST friction.
  - `phone` (`String`) — 🟢 **Live Now**: Verified contact phone number.
  - `email` (`String`) — 🟡 **Useful for Future**: Optional email for invoices and monthly summaries.
  - `photoUrl` (`String`) — 🟢 **Live Now**: Shop facade photo or employer selfie.
  - `address` (`String`) — 🟢 **Live Now**: Full address text.
  - `area` (`String`) — 🟢 **Live Now**: Locality name.
  - `lat` (`Number`) — 🟢 **Live Now**: GPS latitude.
  - `lng` (`Number`) — 🟢 **Live Now**: GPS longitude.
  - `geohash` (`String`) — 🟢 **Live Now**: Precision 9 geohash.
  - `subscription` (`Map`) — 🟢 **Live Now**: Active subscription details (server-written):
    - `planId` (`String`): Current active plan.
    - `status` (`String`): `"ACTIVE"` | `"EXPIRED"`.
    - `startAt` (`Timestamp`): Plan activation date.
    - `expiresAt` (`Timestamp`): Plan expiry date.
    - `credits` (`Map`): Available `creditsNormal` and `creditsInstant`.
    - `ai` (`Boolean`): Whether DutyPe AI voice assistant is enabled.
    - `aiPerDay` (`Number`): Max AI prompts allowed per day.
  - `freeUrgentPostsUsed` (`Number`) — 🟢 **Live Now**: Counter tracking free urgent posts used.
  - `aiTrialUsed` (`Number`) — 🟢 **Live Now**: Free AI trial requests consumed.
  - `referralFreePosts` (`Number`) — 🟢 **Live Now**: Free normal job posts earned via referral program.
  - `referralFreePostsUntil` (`Timestamp`) — 🟢 **Live Now**: Expiry of referral-earned free posts.
  - `verified` (`Boolean`) — 🟢 **Live Now**: Verified badge indicator.
  - `rating` (`Number`) — 🟢 **Live Now**: Employer rating average.
  - `ratingCount` (`Number`) — 🟢 **Live Now**: Total ratings received from workers.
  - `totalHires` (`Number`) — 🟢 **Live Now**: Total workers hired count.
  - `blocked` (`Boolean`) — 🟢 **Live Now**: Admin ban flag.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Account creation timestamp.
  - `updatedAt` (`Timestamp`) — 🟢 **Live Now**: Last update timestamp.

### 3.2 `employer_profiles/{uid}/work_locations` (Subcollection)
- **Path**: `employer_profiles/{uid}/work_locations/{locationId}`
- **Purpose**: Saved workplace locations ("My Addresses") used for quick autofill during job posting.
- **Fields**:
  - `label` (`String`) — 🟢 **Live Now**: Label e.g. `"Home"`, `"Main Branch"`, `"Warehouse"`.
  - `address` (`String`) — 🟢 **Live Now**: Street address text.
  - `area` (`String`) — 🟢 **Live Now**: Locality name.
  - `lat` (`Number`) — 🟢 **Live Now**: GPS latitude.
  - `lng` (`Number`) — 🟢 **Live Now**: GPS longitude.
  - `geohash` (`String`) — 🟢 **Live Now**: Geohash for matching.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Timestamp added.

### 3.3 `employer_profiles/{uid}/unlocks` (Subcollection)
- **Path**: `employer_profiles/{uid}/unlocks/{workerId}`
- **Purpose**: Tracks worker contact numbers unlocked by this employer using credits.
- **Fields**:
  - `workerId` (`String`) — 🟢 **Live Now**: Unlocked worker UID.
  - `unlockedAt` (`Timestamp`) — 🟢 **Live Now**: Unlock timestamp.

### 3.4 `employer_cards`
- **Path**: `employer_cards/{uid}`
- **Purpose**: Public employer summary card displayed on job details screens.
- **Fields**:
  - `businessName` (`String`) — 🟢 **Live Now**: Company or owner name.
  - `photoUrl` (`String`) — 🟢 **Live Now**: Logo / photo.
  - `area` (`String`) — 🟢 **Live Now**: Locality name.
  - `rating` (`Number`) — 🟢 **Live Now**: Average rating.
  - `ratingCount` (`Number`) — 🟢 **Live Now**: Total rating reviews count.
  - `totalHires` (`Number`) — 🟢 **Live Now**: Total hires count.
  - `verified` (`Boolean`) — 🟢 **Live Now**: Verified badge indicator.

---

## 4. Regular Job Vacancies & Applications

### 4.1 `jobmetadata` (Jobs)
- **Path**: `jobmetadata/{jobId}` (referred as `jobs` in schema)
- **Purpose**: Public job vacancy cards displayed in search, feeds, and maps.
- **Fields**:
  - `employerId` (`String`) — 🟢 **Live Now**: Employer UID.
  - `title` (`String`) — 🟢 **Live Now**: Job title.
  - `category` (`String`) — 🟢 **Live Now**: Auto-inferred category key (e.g. `"COOK"`, `"DRIVER"`).
  - `payType` (`String`) — 🟢 **Live Now**: `"HOURLY"` | `"DAILY"` | `"WEEKLY"` | `"MONTHLY"` | `"NEGOTIABLE"`.
  - `payAmount` (`Number`) — 🟢 **Live Now**: Pay in whole rupees.
  - `openings` (`Number`) — 🟢 **Live Now**: Number of vacancies to fill.
  - `applicationsCount` (`Number`) — 🟢 **Live Now**: Count of applicants (server-maintained).
  - `area` (`String`) — 🟢 **Live Now**: Work locality.
  - `lat` (`Number`) — 🟢 **Live Now**: Job latitude.
  - `lng` (`Number`) — 🟢 **Live Now**: Job longitude.
  - `geohash` (`String`) — 🟢 **Live Now**: Precision 9 geohash.
  - `status` (`String`) — 🟢 **Live Now**: `"open"` | `"filled"` | `"closed"` | `"expired"`.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Posting date.
  - `expiresAt` (`Timestamp`) — 🟢 **Live Now**: Expiration date (typically 30 days).

### 4.2 `job_details`
- **Path**: `job_details/{jobId}`
- **Purpose**: Extended job details retrieved only when viewing the full job screen.
- **Fields**:
  - `description` (`String`) — 🟢 **Live Now**: Job requirements and responsibilities.
  - `shift` (`String`) — 🟢 **Live Now**: `"DAY"` | `"NIGHT"` | `"ANY"`.
  - `timings` (`String`) — 🟢 **Live Now**: Working hours (e.g. "9 AM - 6 PM").
  - `genderPreference` (`String`) — 🟢 **Live Now**: `"ANY"` | `"MALE"` | `"FEMALE"`.
  - `startDate` (`String`) — 🔴 **Removed**: Removed from schema and job forms. Stated naturally in description when applicable.
  - `perks` / `benefits` (`List<String>`) — 🔴 **Removed**: Removed from schema, cloud functions, and job forms. Mentioned in description when applicable.

### 4.3 `job_contacts`
- **Path**: `job_contacts/{jobId}`
- **Purpose**: Direct employer phone number for a job listing (protected read).
- **Fields**:
  - `contactNumber` (`String`) — 🟢 **Live Now**: Phone number revealed upon unlocking.

### 4.4 `applications`
- **Path**: `applications/{applicationId}`
- **Purpose**: Job application submitted by a worker for a job vacancy.
- **Fields**:
  - `jobId` (`String`) — 🟢 **Live Now**: Target job ID.
  - `workerId` (`String`) — 🟢 **Live Now**: Worker UID.
  - `employerId` (`String`) — 🟢 **Live Now**: Employer UID.
  - `status` (`String`) — 🟢 **Live Now**: `"applied"` | `"hired"` | `"completed"` | `"rejected"` | `"withdrawn"`.
  - `workerName` (`String`) — 🟢 **Live Now**: Worker name snapshot.
  - `workerPhone` (`String`) — 🟢 **Live Now**: Worker phone snapshot.
  - `workerPhotoUrl` (`String`) — 🟢 **Live Now**: Worker photo.
  - `workerSkills` (`List<String>`) — 🟢 **Live Now**: Worker skills snapshot.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Timestamp of application.
  - `updatedAt` (`Timestamp`) — 🟢 **Live Now**: Timestamp of status change.

### 4.5 `saved_jobs`
- **Path**: `saved_jobs/{docId}` (Composite ID: `{workerId}_{jobId}`)
- **Purpose**: Bookmarked jobs for workers.
- **Fields**:
  - `workerId` (`String`) — 🟢 **Live Now**: Worker UID.
  - `jobId` (`String`) — 🟢 **Live Now**: Job ID.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Bookmark date.

---

## 5. Urgent / Instant Hiring Needs

### 5.1 `instant_requests`
- **Path**: `instant_requests/{requestId}`
- **Purpose**: Immediate hiring needs dispatched to nearby available workers within 15–30 minutes.
- **Fields**:
  - `employerId` (`String`) — 🟢 **Live Now**: Employer UID.
  - `employerName` (`String`) — 🟢 **Live Now**: Employer display name.
  - `employerPhone` (`String`) — 🟢 **Live Now**: Employer contact phone.
  - `category` (`String`) — 🟢 **Live Now**: Category of work required.
  - `workDetails` (`String`) — 🟢 **Live Now**: Brief task description.
  - `payAmount` (`Number`) — 🟢 **Live Now**: Custom wage in whole rupees.
  - `payType` (`String`) — 🟢 **Live Now**: `"TASK"` | `"HOURLY"` | `"DAILY"` | `"2-3 DAYS"` | `"WEEK"`.
  - `area` (`String`) — 🟢 **Live Now**: Work locality.
  - `address` (`String`) — 🟢 **Live Now**: Exact address.
  - `lat` (`Number`) — 🟢 **Live Now**: Latitude.
  - `lng` (`Number`) — 🟢 **Live Now**: Longitude.
  - `geohash` (`String`) — 🟢 **Live Now**: Precision 9 geohash.
  - `status` (`String`) — 🟢 **Live Now**: `"open"` | `"filled"` | `"completed"` | `"cancelled"` | `"expired"`.
  - `hiredWorkerId` (`String`) — 🟢 **Live Now**: Worker UID who accepted the urgent task.
  - `hiredWorkerName` (`String`) — 🟢 **Live Now**: Accepted worker name.
  - `hiredWorkerPhone` (`String`) — 🟢 **Live Now**: Accepted worker phone.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Request creation timestamp.
  - `expiresAt` (`Timestamp`) — 🟢 **Live Now**: Expiry timeout (usually 15 to 45 mins).

### 5.2 `instant_requests/{requestId}/responses` (Subcollection)
- **Path**: `instant_requests/{requestId}/responses/{workerId}`
- **Purpose**: Real-time worker responses to an urgent dispatch broadcast.
- **Fields**:
  - `workerId` (`String`) — 🟢 **Live Now**: Worker UID.
  - `status` (`String`) — 🟢 **Live Now**: `"applied"` | `"called"` | `"accepted"` | `"rejected"` | `"no_show"`.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Response timestamp.

---

## 6. Home & Commercial Services

### 6.1 `service_bookings`
- **Path**: `service_bookings/{bookingId}`
- **Purpose**: Fixed-price and inspection-based service bookings booked by customers and accepted by service partners.
- **Fields**:
  - `customerId` (`String`) — 🟢 **Live Now**: Customer UID.
  - `customerName` (`String`) — 🟢 **Live Now**: Customer name.
  - `customerPhone` (`String`) — 🟢 **Live Now**: Customer phone.
  - `category` (`String`) — 🟢 **Live Now**: Service category (e.g. `"PLUMBING"`, `"CLEANING"`).
  - `serviceId` (`String`) — 🟢 **Live Now**: Specific service ID.
  - `serviceName` (`String`) — 🟢 **Live Now**: Display title of the service.
  - `price` (`Number`) — 🟢 **Live Now**: Service base price in rupees.
  - `bookingFee` (`Number`) — 🟢 **Live Now**: Convenience / platform fee in rupees.
  - `commissionPct` (`Number`) — 🟢 **Live Now**: Platform commission rate.
  - `discount` (`Number`) — 🟢 **Live Now**: Promo code discount applied.
  - `couponCode` (`String`) — 🟢 **Live Now**: Coupon code used.
  - `total` (`Number`) — 🟢 **Live Now**: Final amount payable.
  - `addressText` (`String`) — 🟢 **Live Now**: Customer service address.
  - `area` (`String`) — 🟢 **Live Now**: Locality name.
  - `lat` (`Number`) — 🟢 **Live Now**: Latitude.
  - `lng` (`Number`) — 🟢 **Live Now**: Longitude.
  - `note` (`String`) — 🟢 **Live Now**: Special instructions.
  - `when` (`String`) — 🟢 **Live Now**: `"now"` | `"scheduled"`.
  - `scheduledAt` (`Timestamp`) — 🟢 **Live Now**: Scheduled appointment time (if scheduled).
  - `status` (`String`) — 🟢 **Live Now**: `"SEARCHING"` | `"ASSIGNED"` | `"ON_THE_WAY"` | `"STARTED"` | `"COMPLETED"` | `"CANCELLED"`.
  - `partnerId` (`String`) — 🟢 **Live Now**: Assigned partner UID.
  - `partnerName` (`String`) — 🟢 **Live Now**: Partner display name.
  - `partnerPhone` (`String`) — 🟢 **Live Now**: Partner contact phone.
  - `partnerRating` (`Number`) — 🟢 **Live Now**: Partner rating.
  - `partnerFee` (`Number`) — 🟢 **Live Now**: Net fee earned by the partner.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Booking creation timestamp.
  - `updatedAt` (`Timestamp`) — 🟢 **Live Now**: Last update timestamp.

### 6.2 `service_booking_secrets`
- **Path**: `service_booking_secrets/{bookingId}`
- **Purpose**: Confidential OTPs for service security; never readable by service partners.
- **Fields**:
  - `customerId` (`String`) — 🟢 **Live Now**: Customer UID.
  - `startOtp` (`String`) — 🟢 **Live Now**: 4-digit start OTP given by customer to partner to begin work.

### 6.3 `service_partners`
- **Path**: `service_partners/{uid}`
- **Purpose**: Approved service professionals profile and wallet state.
- **Fields**:
  - `name` (`String`) — 🟢 **Live Now**: Partner name.
  - `phone` (`String`) — 🟢 **Live Now**: Partner phone number.
  - `photoUrl` (`String`) — 🟢 **Live Now**: Profile photo.
  - `status` (`String`) — 🟢 **Live Now**: `"PENDING"` | `"APPROVED"` | `"REJECTED"` | `"SUSPENDED"`.
  - `categories` (`List<String>`) — 🟢 **Live Now**: Services the partner is certified to deliver.
  - `creditsPaise` (`Number`) — 🟢 **Live Now**: Partner prepaid platform balance in paise.
  - `online` (`Boolean`) — 🟢 **Live Now**: Availability toggle for job waves.
  - `activeBookingId` (`String`) — 🟢 **Live Now**: Current active booking ID being executed.
  - `lat` (`Number`) — 🟢 **Live Now**: Real-time / home latitude.
  - `lng` (`Number`) — 🟢 **Live Now**: Real-time / home longitude.
  - `ratingSum` (`Number`) — 🟢 **Live Now**: Cumulative ratings sum.
  - `ratingCount` (`Number`) — 🟢 **Live Now**: Total ratings count.
  - `jobsCompleted` (`Number`) — 🟢 **Live Now**: Finished jobs count.
  - `appliedAt` (`Timestamp`) — 🟢 **Live Now**: Application submission timestamp.
  - `updatedAt` (`Timestamp`) — 🟢 **Live Now**: Update timestamp.

### 6.4 `service_partners/{uid}/ledger` (Subcollection)
- **Path**: `service_partners/{uid}/ledger/{entryId}`
- **Purpose**: Immutable financial ledger of commission debits and topup credits for partners.
- **Fields**:
  - `type` (`String`) — 🟢 **Live Now**: `"COMMISSION_DEBIT"` | `"TOPUP_CREDIT"` | `"ADJUSTMENT"`.
  - `amountPaise` (`Number`) — 🟢 **Live Now**: Amount moved in paise.
  - `balanceAfter` (`Number`) — 🟢 **Live Now**: Remaining balance in paise.
  - `bookingId` (`String`) — 🟢 **Live Now**: Associated booking ID.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Timestamp.

### 6.5 `coupon_uses`
- **Path**: `coupon_uses/{uid}_{code}`
- **Purpose**: Enforces atomic, one-time coupon redemption per user.
- **Fields**:
  - `bookingId` (`String`) — 🟢 **Live Now**: Associated booking ID.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Timestamp applied.

### 6.6 `partner_topups`
- **Path**: `partner_topups/{topupId}`
- **Purpose**: UPI topup payment requests by partners to recharge their job-acceptance balance.
- **Fields**:
  - `partnerId` (`String`) — 🟢 **Live Now**: Partner UID.
  - `amountPaise` (`Number`) — 🟢 **Live Now**: Topup amount in paise.
  - `utrNumber` (`String`) — 🟢 **Live Now**: 12-digit bank UPI transaction reference number.
  - `status` (`String`) — 🟢 **Live Now**: `"PENDING"` | `"VERIFIED"` | `"REJECTED"`.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Request timestamp.

---

## 7. Referral & Earnings Engine

### 7.1 `referral_codes`
- **Path**: `referral_codes/{code}` (e.g. `DUTY8921`)
- **Purpose**: Global registry of referral codes mapping each unique code to its owner.
- **Fields**:
  - `ownerUid` (`String`) — 🟢 **Live Now**: Referrer's Auth UID.
  - `role` (`String`) — 🟢 **Live Now**: Referrer's role (`"WORKER"` | `"EMPLOYER"`).
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Code creation date.

### 7.2 `referrals`
- **Path**: `referrals/{refereeUid}`
- **Purpose**: Tracks who referred this specific user and whether payout milestones were met.
- **Fields**:
  - `referrerUid` (`String`) — 🟢 **Live Now**: Referrer UID.
  - `referrerRole` (`String`) — 🟢 **Live Now**: Referrer role.
  - `refereeRole` (`String`) — 🟢 **Live Now**: Referee role.
  - `referralCode` (`String`) — 🟢 **Live Now**: Code entered.
  - `status` (`String`) — 🟢 **Live Now**: `"PENDING"` | `"COMPLETED"` | `"REJECTED"`.
  - `rewardPaid` (`Boolean`) — 🟢 **Live Now**: Flag indicating bonus was credited.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Signup date.
  - `completedAt` (`Timestamp`) — 🟢 **Live Now**: Qualification date.

### 7.3 `referral_stats`
- **Path**: `referral_stats/{uid}`
- **Purpose**: Real-time referral earnings, lifetime balances, and payout tracking.
- **Fields**:
  - `referralCode` (`String`) — 🟢 **Live Now**: User's own referral code.
  - `totalReferred` (`Number`) — 🟢 **Live Now**: Total people invited.
  - `totalSuccessful` (`Number`) — 🟢 **Live Now**: Qualified successful referrals.
  - `totalEarned` (`Number`) — 🟢 **Live Now**: Lifetime earnings in rupees.
  - `balance` (`Number`) — 🟢 **Live Now**: Withdrawable balance in rupees.
  - `updatedAt` (`Timestamp`) — 🟢 **Live Now**: Last ledger update timestamp.

### 7.4 `wallet_ledger`
- **Path**: `wallet_ledger/{entryId}`
- **Purpose**: Immutable ledger tracking all wallet balance changes.
- **Fields**:
  - `userId` (`String`) — 🟢 **Live Now**: User UID.
  - `amount` (`Number`) — 🟢 **Live Now**: Transaction amount in rupees.
  - `type` (`String`) — 🟢 **Live Now**: `"REFERRAL_REWARD"` | `"SIGNUP_BONUS"` | `"WITHDRAWAL"`.
  - `description` (`String`) — 🟢 **Live Now**: Explanation shown in app wallet history.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Timestamp.

### 7.5 `withdrawal_requests`
- **Path**: `withdrawal_requests/{requestId}`
- **Purpose**: User payout requests submitted to UPI IDs.
- **Fields**:
  - `userId` (`String`) — 🟢 **Live Now**: Requester UID.
  - `amount` (`Number`) — 🟢 **Live Now**: Amount requested in rupees.
  - `upiId` (`String`) — 🟢 **Live Now**: Verified target UPI VPA address.
  - `status` (`String`) — 🟢 **Live Now**: `"PENDING"` | `"PROCESSING"` | `"COMPLETED"` | `"FAILED"`.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Request date.
  - `processedAt` (`Timestamp`) — 🟢 **Live Now**: Settlement date.

### 7.6 `withdrawal_daily`
- **Path**: `withdrawal_daily/{YYYYMMDD}`
- **Purpose**: Enforces daily platform cash payout limits to prevent fraud or wallet drain.
- **Fields**:
  - `totalAmount` (`Number`) — 🟢 **Live Now**: Total rupees withdrawn across all users today.
  - `requestCount` (`Number`) — 🟢 **Live Now**: Count of withdrawal requests today.

---

## 8. Employer Subscriptions & Unlocks

### 8.1 `subscription_payment_requests`
- **Path**: `subscription_payment_requests/{requestId}`
- **Purpose**: Employer subscription payment proofs uploaded via manual UPI QR code scan.
- **Fields**:
  - `employerId` (`String`) — 🟢 **Live Now**: Employer UID.
  - `planId` (`String`) — 🟢 **Live Now**: Target plan (e.g. `"basic_99"`, `"pro_ai_199"`).
  - `utrNumber` (`String`) — 🟢 **Live Now**: 12-digit bank UPI transaction reference number.
  - `screenshotUrl` (`String`) — 🟢 **Live Now**: Payment screenshot proof image.
  - `status` (`String`) — 🟢 **Live Now**: `"PENDING"` | `"VERIFIED"` | `"REJECTED"`.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Submission date.
  - `verifiedAt` (`Timestamp`) — 🟢 **Live Now**: Verification date.

### 8.2 `active_qr_codes`
- **Path**: `active_qr_codes/{qrId}`
- **Purpose**: Admin configurable UPI payment receiver VPA and QR asset links.
- **Fields**:
  - `upiId` (`String`) — 🟢 **Live Now**: Company receiver UPI VPA.
  - `qrImageUrl` (`String`) — 🟢 **Live Now**: Cloud Storage URL for dynamic QR graphic.
  - `isActive` (`Boolean`) — 🟢 **Live Now**: Active status flag.

---

## 9. Notifications & Broadcasts

### 9.1 `notifications`
- **Path**: `notifications/{notificationId}`
- **Purpose**: In-app notifications inbox for users.
- **Fields**:
  - `recipientId` (`String`) — 🟢 **Live Now**: Target user UID.
  - `title` (`String`) — 🟢 **Live Now**: Notification header text.
  - `message` (`String`) — 🟢 **Live Now**: Notification body text.
  - `type` (`String`) — 🟢 **Live Now**: `"JOB"`, `"PAYMENT"`, `"SYSTEM"`, `"REFERRAL"`.
  - `read` (`Boolean`) — 🟢 **Live Now**: Whether read by user.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Timestamp.

### 9.2 `broadcast_notifications`
- **Path**: `broadcast_notifications/{broadcastId}`
- **Purpose**: Web admin FCM broadcast audit history.
- **Fields**:
  - `title` (`String`) — 🟢 **Live Now**: Broadcast title.
  - `message` (`String`) — 🟢 **Live Now**: Broadcast text.
  - `targetRole` (`String`) — 🟢 **Live Now**: `"ALL"` | `"WORKER"` | `"EMPLOYER"`.
  - `sentCount` (`Number`) — 🟢 **Live Now**: Number of messages delivered.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Dispatch timestamp.

---

## 10. WhatsApp Marketing & Automation

### 10.1 `whatsapp_prefs`
- **Path**: `whatsapp_prefs/{uid}`
- **Purpose**: User opt-in and consent for WhatsApp utility and promotional alerts.
- **Fields**:
  - `promos` (`Boolean`) — 🟢 **Live Now**: Toggle switch state from app settings.
  - `phone` (`String`) — 🟢 **Live Now**: E.164 phone number.
  - `language` (`String`) — 🟢 **Live Now**: Preferred language (`"te"`, `"en"`, `"hi"`).
  - `roles` (`List<String>`) — 🟢 **Live Now**: Associated roles.
  - `consentAt` (`Timestamp`) — 🟢 **Live Now**: Opt-in timestamp.
  - `optedOutAt` (`Timestamp`) — 🟢 **Live Now**: Set when user disables or sends STOP on WhatsApp.
  - `source` (`String`) — 🟢 **Live Now**: `"app"` | `"whatsapp_stop"`.

### 10.2 `whatsapp_campaigns`
- **Path**: `whatsapp_campaigns/{campaignId}`
- **Purpose**: Record of bulk promotional WhatsApp template campaigns dispatched by Admin.
- **Fields**:
  - `template` (`String`) — 🟢 **Live Now**: Meta-approved template name.
  - `audience` (`String`) — 🟢 **Live Now**: `"ALL"` | `"WORKER"` | `"EMPLOYER"` | `"PARTNER"`.
  - `sent` (`Number`) — 🟢 **Live Now**: Total successfully delivered.
  - `failed` (`Number`) — 🟢 **Live Now**: Total failures.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Creation timestamp.

### 10.3 `whatsapp_delivery_logs`
- **Path**: `whatsapp_delivery_logs/{logId}`
- **Purpose**: Per-message webhook delivery confirmation logs from Meta Cloud API.
- **Fields**:
  - `phone` (`String`) — 🟢 **Live Now**: Target phone number.
  - `status` (`String`) — 🟢 **Live Now**: `"sent"` | `"delivered"` | `"read"` | `"failed"`.
  - `template` (`String`) — 🟢 **Live Now**: Template invoked.
  - `timestamp` (`Timestamp`) — 🟢 **Live Now**: Timestamp.

---

## 11. App Configuration & Dynamic Feeds

### 11.1 `app_config` (Collection of Singletons)
- **Path**: `app_config/{docId}`
- **Documents**:
  1. `app_config/services` — 🟢 **Live Now**: Catalog of home and commercial services, price lists, booking fees, and commission percentages.
  2. `app_config/referral` — 🟢 **Live Now**: Dynamic referral amounts (₹50, ₹100, etc.) and welcome bonuses.
  3. `app_config/app_update` — 🟢 **Live Now**: Force update min version and store URLs.
  4. `app_config/dynamic_features` — 🟢 **Live Now**: Feature flags (e.g. enable AI voice, fast hiring).
  5. `app_config/subscription_plans` — 🟢 **Live Now**: Dynamic pricing tiers for employer credits.
  6. `app_config/ai_quotas` — 🟢 **Live Now**: DutyPe AI rate limits and trial allowances.

### 11.2 `announcements`
- **Path**: `announcements/{announcementId}`
- **Purpose**: In-app banners and urgent notices shown at the top of the home feeds.
- **Fields**:
  - `title` (`String`) — 🟢 **Live Now**: Banner title.
  - `message` (`String`) — 🟢 **Live Now**: Banner copy.
  - `type` (`String`) — 🟢 **Live Now**: `"INFO"` | `"WARNING"` | `"PROMOTION"`.
  - `priority` (`String`) — 🟢 **Live Now**: Priority level.
  - `targetRole` (`String`) — 🟢 **Live Now**: `"worker"` | `"employer"` | `"all"`.
  - `active` (`Boolean`) — 🟢 **Live Now**: Active visibility flag.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Creation date.
  - `expiresAt` (`Timestamp`) — 🟢 **Live Now**: Expiry date.

### 11.3 `promo_showcases`
- **Path**: `promo_showcases/{showcaseId}`
- **Purpose**: Local brand, restaurant, and shop promotional showcase microsites and carousel slides with geo-fencing.
- **Fields**:
  - `id` (`String`) — 🟢 **Live Now**: Unique showcase slug (e.g. `paradise_biryani_khammam`).
  - `title` (`String`) — 🟢 **Live Now**: Business / Brand name.
  - `tagline` (`String`) — 🟢 **Live Now**: Catchphrase and specialty summary.
  - `categoryBadge` (`String`) — 🟢 **Live Now**: Category tag (e.g. `RESTAURANT`, `MALL`, `CLINIC`).
  - `rating` (`String`) — 🟢 **Live Now**: Star rating (e.g. `4.8`).
  - `heroImageUrl` (`String`) — 🟢 **Live Now**: High-res cover image URL.
  - `videoUrl` (`String`) — 🟢 **Live Now**: YouTube or MP4 video URL.
  - `aboutDescription` (`String`) — 🟢 **Live Now**: Detailed story / description.
  - `address` (`String`) — 🟢 **Live Now**: Physical shop address.
  - `mapLink` (`String`) — 🟢 **Live Now**: Google Maps navigation URL.
  - `district` (`String`) — 🟢 **Live Now**: Geo-targeting district (e.g. `"Khammam"`, `"Warangal"`, or `"ALL"`).
  - `scope` (`String`) — 🟢 **Live Now**: Geographic scope tier (`"DISTRICT"` | `"STATE"` | `"NATIONAL"`).
  - `active` (`Boolean`) — 🟢 **Live Now**: Live visibility switch.
  - `discountOfferTitle` (`String`) — 🟢 **Live Now**: Offer title (e.g. `"Flat 20% Off Food Bill"`).
  - `discountOfferCode` (`String`) — 🟢 **Live Now**: Promo code (e.g. `"PARADISE20"`).
  - `discountOfferTerms` (`String`) — 🟢 **Live Now**: Terms & conditions.
  - `highlights` (`Array<String>`) — 🟢 **Live Now**: Bulleted feature chips.
  - `galleryImages` (`Array<String>`) — 🟢 **Live Now**: Photo gallery array.
  - `actionButtons` (`Array<Map>`) — 🟢 **Live Now**: Contact buttons (`label`, `icon`, `actionUrl`, `isPrimary`).
  - `updatedAt` (`Number`) — 🟢 **Live Now**: Last edit epoch millis.

### 11.4 `location_demand`
- **Path**: `location_demand/{leadId}`
- **Purpose**: Records search requests in unlaunched areas to gauge expansion demand.
- **Fields**:
  - `uid` (`String`) — 🟢 **Live Now**: User UID (or empty if anonymous).
  - `area` (`String`) — 🟢 **Live Now**: Unserved area searched.
  - `lat` (`Number`) — 🟢 **Live Now**: Latitude.
  - `lng` (`Number`) — 🟢 **Live Now**: Longitude.
  - `category` (`String`) — 🟢 **Live Now**: Searched job or service category.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Search timestamp.

---

## 12. Field Operations & Lead Capture

### 12.1 `field_agents`
- **Path**: `field_agents/{agentId}`
- **Purpose**: Field ground agents registering workers and shops in physical markets.
- **Fields**:
  - `name` (`String`) — 🟢 **Live Now**: Agent name.
  - `phone` (`String`) — 🟢 **Live Now**: Agent phone.
  - `city` (`String`) — 🟢 **Live Now**: Assigned city.
  - `leadsCount` (`Number`) — 🟢 **Live Now**: Total on-ground leads captured.
  - `active` (`Boolean`) — 🟢 **Live Now**: Agent status.

### 12.2 `field_leads`
- **Path**: `field_leads/{leadId}`
- **Purpose**: Ground worker or shop leads submitted by field agents.
- **Fields**:
  - `agentId` (`String`) — 🟢 **Live Now**: Field agent ID.
  - `phone` (`String`) — 🟢 **Live Now**: Prospect phone.
  - `name` (`String`) — 🟢 **Live Now**: Prospect name.
  - `role` (`String`) — 🟢 **Live Now**: Prospect interested role (`"WORKER"` | `"EMPLOYER"`).
  - `area` (`String`) — 🟢 **Live Now**: Market / area name.
  - `registered` (`Boolean`) — 🟢 **Live Now**: Whether the lead completed app login.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Submission date.

---

## 13. Internal Security & Idempotency

### 13.1 `idempotency`
- **Path**: `idempotency/{key}`
- **Purpose**: Prevents duplicate executions for actions like double-posting, double payment processing, or rapid button clicks.
- **Fields**:
  - `result` (`Any`) — 🟢 **Live Now**: Cached response returned on replay.
  - `expireAt` (`Timestamp`) — 🟢 **Live Now**: TTL expiry.

### 13.2 `otp_codes`
- **Path**: `otp_codes/{phone}`
- **Purpose**: Secure server-side hashed storage for custom SMS & WhatsApp OTP auth.
- **Fields**:
  - `hash` (`String`) — 🟢 **Live Now**: Salted hash of the OTP code.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Generation timestamp.
  - `attempts` (`Number`) — 🟢 **Live Now**: Verification attempt count (max 3).

### 13.3 `otp_ip`
- **Path**: `otp_ip/{ipHash}`
- **Purpose**: Rate limiting OTP requests per client IP to prevent SMS exhaustion attacks.
- **Fields**:
  - `count` (`Number`) — 🟢 **Live Now**: Request count in current window.
  - `expireAt` (`Timestamp`) — 🟢 **Live Now**: Reset timestamp.

### 13.4 `otp_daily`
- **Path**: `otp_daily/{YYYYMMDD}`
- **Purpose**: Tracks platform-wide daily spend caps for OTPs and WhatsApp messages.
- **Fields**:
  - `smsCount` (`Number`) — 🟢 **Live Now**: Total SMS sent today.
  - `waCount` (`Number`) — 🟢 **Live Now**: Total WhatsApp OTPs sent today.
  - `promoCount` (`Number`) — 🟢 **Live Now**: Total WhatsApp promotional messages sent today.

### 13.5 `admins`
- **Path**: `admins/{uid}`
- **Purpose**: Authoritative document granting web admin portal privileges.
- **Fields**:
  - `role` (`String`) — 🟢 **Live Now**: `"SUPERADMIN"` | `"SUPPORT"` | `"CONTENT"`.
  - `grantedAt` (`Timestamp`) — 🟢 **Live Now**: Grant timestamp.

### 13.6 `ratings`
- **Path**: `ratings/{ratingId}` (Composite ID: `{reviewerId}_{targetId}_{contextId}`)
- **Purpose**: Bidirectional rating reviews submitted between workers and employers.
- **Fields**:
  - `reviewerId` (`String`) — 🟢 **Live Now**: Reviewer UID.
  - `targetId` (`String`) — 🟢 **Live Now**: Target UID.
  - `rating` (`Number`) — 🟢 **Live Now**: Star score (1 to 5).
  - `review` (`String`) — 🟢 **Live Now**: Optional review text.
  - `createdAt` (`Timestamp`) — 🟢 **Live Now**: Timestamp.

---

## 14. Detailed Audit: Unnecessary & Redundant Fields

The following fields and structures have been identified as unnecessary, redundant, or legacy. They can be safely trimmed or deprecated without breaking the application:

| Collection | Field / Structure | Status | Rationale & Recommendation |
| :--- | :--- | :---: | :--- |
| `employer_profiles` | `businessType` | 🔴 **Removed** | Removed from schema, rules, functions, and apps. Category is automatically inferred from job titles and set on listings directly. |
| `employer_profiles` | `gstin` | 🔴 **Removed** | Removed from schema, rules, functions, and profile setup screens. Hyper-local blue-collar hiring requires zero GST friction. |
| `job_details` | `startDate` | 🔴 **Removed** | Normal job vacancies are ongoing or immediate. Specific start dates can be written naturally by the employer in the job description (`description`). Removed from UI and schema. |
| `job_details` | `perks` / `benefits` | 🔴 **Removed** | Removed from schema, rules, functions, and job post payload. Employers mention perks directly in the job description. |
| `worker_cards` | `currentCell`, `currentDistrictId` | 🟡 **Future Only** | Real-time GPS roaming tracking for workers is currently inactive; search queries currently rely on the primary `cell` and `districtId`. Keep schema intact for future background geofencing. |
| `truecaller_profiles` | Entire Collection | 🟡 **Future Only** | Currently, Truecaller SDK is not the primary login route (custom WhatsApp/SMS OTP is the primary fast auth). Retain for future one-tap Truecaller sign-in. |
| `employer_profiles` | `email` | 🟡 **Future Only** | Client writes to `email` in `employer_profiles` are restricted by security rules (marked server-written). Stored in local app storage for convenience, but Firestore persistence can be promoted when invoicing is rolled out. |
| `worker_profiles` | `education` | 🟡 **Future Only** | Rarely used for blue-collar matching algorithms (cooks, drivers, maids). Can remain optional without enforcing input. |
