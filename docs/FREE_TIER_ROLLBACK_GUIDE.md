# DutyPe 100% Free Campaign & Rollback Guide

> **Context**: To accelerate marketplace liquidity and maximize job postings, applicant matching, and home service bookings, DutyPe is set to **100% Free** across all core services until the platform surpasses **50+ employer hires per day**.
> 
> - **Job Postings**: Unlimited free postings for all employers (no ₹49 subscriptions, no 48h expiration cuts, no credit limits).
> - **Applicant Unlocks**: Free and instant viewing/calling of applicant contact numbers.
> - **Instant / Urgent Needs**: Unlimited free emergency worker requests.
> - **Home Services**: Free service booking for customers (zero platform fee / booking fee) and zero commission / zero credit deductions from service partners.
> - **Service Notifications**: Nearest partner dispatch strictly restricted to within 5–10 km of the booking location.

---

## 1. Summary of Changes Made

### Cloud Functions (`functions/src/`)

#### A. `functions/src/services.ts`
1. **Service Notification Radius (Nearest 5–10 km)**:
   - Changed `SERVICE_WAVES_KM` from `[3, 6, 10, 15, 25]` to `[5, 10] as const`.
   - In `partnersInRing()`:
     - Replaced the hardcoded Khammam district center check (`distFromCenter > 25.0`) with customer-relative distance calculation (`distanceKm(lat, lng, pLat, pLng)`).
     - Enforced `MAX_SERVICE_RADIUS_KM = 10.0`. Partners farther than 10 km from the booking address are never notified.
   - In `acceptServiceBooking()`:
     - Enforced `distKm <= 10.0` for accepting doorstep bookings.
2. **Free Service Booking & Zero Partner Commission**:
   - In `creditsNeededFor()`: Returns `() => 0` so partners need zero prepaid wallet balance/credits to accept jobs.
   - In `createServiceBooking()`: Forced `isFreePlatformFee = true` and `effectiveBookingFee = 0`. Status transitions directly to `SEARCHING` without placing the customer into `PAYMENT_PENDING`.
   - In `updateServiceBooking()` (`case "complete"`): Set `take = 0`, ensuring partners keep 100% of their earnings and their wallet is not debited.

#### B. `functions/src/jobs.ts`
1. **Unlimited Free Job Posting**:
   - In `chargeForPost()`: Always returns `"free"` and increments `FREE_JOB_POSTS_USED` for analytics without ever throwing `resource-exhausted` or demanding subscription plans.
2. **7-Day Expiration for Free Jobs**:
   - In `postJob()`: Set `[Jobs.EXPIRES_AT] = Timestamp.fromMillis(nowMs + JOB_LIFETIME_MS)` (full 7-day validity instead of 48-hour expiration).

#### C. `functions/src/instant.ts`
1. **Unlimited Urgent / Instant Requests**:
   - In `postInstantRequest()`: Set `paid = "free"` without failing after 3 posts.

#### D. `functions/src/applications.ts`
1. **Free Applicant Contact Unlocking**:
   - In `revealApplicantContact()`: Removed subscription/credit checks so employers can view and call candidates for free.

---

### Android Application (`app/src/main/java/com/example/dutype/`)

#### A. `app/src/main/java/com/example/dutype/employer/screens/EmployerHomeStitchComponents.kt`
- **Employer Countdown & ₹49 Button**:
  - Transformed `EmployerTrialCountdownCard`:
    - Removed the 48-hour ticking countdown timer ("HRS : MIN : SEC").
    - Removed the "Upgrade (₹49)" button.
    - Removed the "TRIAL EXPIRED" locked state.
    - Replaced with the **"🎉 100% FREE FOR ALL EMPLOYERS"** banner and a full-width **"Post Unlimited Jobs Free"** button.

#### B. `app/src/main/java/com/example/dutype/employer/screens/PostJobScreen.kt`
- Removed `showNoCreditsDialog = true` check on job submission.
- Updated top banner to:
  - Title: `"100% Free Job Posting"`
  - Subtitle: `"Post unlimited jobs on DutyPe completely free of charge!"`

#### C. `app/src/main/java/com/example/dutype/homeservices/CustomerServicesScreens.kt`
- Replaced platform fee / low wallet warnings with:
  - `"🎁 100% Free Booking • Pay technician after service only"`
  - Price line in breakdown: `"Platform Fee (100% Free 🎉): ₹0"`.

---

## 2. How to Roll Back / Re-enable Monetization (50+ Hires/Day)

When DutyPe reaches the target milestone of 50+ employer hires per day, follow these exact steps to re-enable monetization:

### Step 1: Re-enable Job Posting Monetization in `functions/src/jobs.ts`
Restore the original `chargeForPost()` logic:
```typescript
function chargeForPost(
  tx: admin.firestore.Transaction, employer: Employer, quota: admin.firestore.DocumentSnapshot, nowMs: number,
  config?: JobPostingConfig,
): Charge {
  const S = EmployerProfiles.Subscription;
  const sub = obj(employer.data[EmployerProfiles.SUBSCRIPTION]);
  const expiresAt = sub[S.EXPIRES_AT] instanceof Timestamp ? (sub[S.EXPIRES_AT] as admin.firestore.Timestamp).toMillis() : 0;
  const active = sub[S.STATUS] === "ACTIVE" && (expiresAt === 0 || expiresAt > nowMs);
  const normalCredits = Number(obj(sub[S.CREDITS])[S.CREDITS_NORMAL] || 0);
  if (active && normalCredits > 0) {
    tx.update(employer.ref, `${EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_NORMAL}`, FieldValue.increment(-1));
    return "credit";
  }
  if (referralPostsLeft(employer.data, nowMs) > 0) {
    useReferralPost(tx, employer.ref);
    return "referral";
  }
  const grantedFree = Number(employer.data["grantedFreeJobPosts"] || 0);
  if (grantedFree > 0) {
    tx.update(employer.ref, {
      grantedFreeJobPosts: FieldValue.increment(-1),
      [EmployerProfiles.FREE_JOB_POSTS_USED]: FieldValue.increment(1),
    });
    return "free";
  }
  const freeUsed = Number(employer.data[EmployerProfiles.FREE_JOB_POSTS_USED] || 0);
  if (freeUsed < 1) {
    tx.update(employer.ref, {
      [EmployerProfiles.FREE_JOB_POSTS_USED]: FieldValue.increment(1),
    });
    return "free";
  }
  fail("resource-exhausted", "Your free trial has ended. Please subscribe to an Employer Plan (from ₹49) to post jobs & contact workers.");
}
```

### Step 2: Re-enable Service Platform Fee in `functions/src/services.ts`
1. In `createServiceBooking`:
```typescript
const isFreePlatformFee = !hasCustomPrice && (hasVipFreeBooking || firstBooking || (who.planMember && config.planMembersFeeFree) || config.firstBookingFeeFree);
const effectiveBookingFee = isFreePlatformFee ? 0 : (q.bookingFee > 0 ? q.bookingFee : (config.bookingFee || 19));
```
2. In `updateServiceBooking` (`case "complete"`):
```typescript
const take = takePaise(price, extras, fee, discount, feeOf(d, config.partnerFee), Number(d[BK.COMMISSION_PCT] || 0));
const balance = Number(p.get(SP.CREDITS_PAISE) || 0) - take;
```
3. In `creditsNeededFor`:
```typescript
async function creditsNeededFor(b: admin.firestore.DocumentData): Promise<(jobsCompleted: number) => number> {
  const config = await loadConfig();
  return (jobs) => requiredCredits(b, partnerFeeOf(config, jobs, b));
}
```

### Step 3: Re-deploy Cloud Functions
```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = 'C:\Users\vamsi\AppData\Roaming\firebase\dutypein_gmail_com_application_default_credentials.json'
npm --prefix functions run build
firebase deploy --only "functions:createServiceBooking,functions:updateServiceBooking,functions:acceptServiceBooking,functions:postJob,functions:postInstantRequest,functions:revealApplicantContact"
```
