# Referral System - Complete Flow Diagram

## 🎯 COMPLETE REFERRAL FLOW

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    NEW USER REGISTRATION WITH REFERRAL                   │
└─────────────────────────────────────────────────────────────────────────┘

STEP 1: USER OPENS APP
┌──────────────────┐
│  User clicks     │
│  "Register"      │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  LoginBottomSheet / EnhancedLoginScreen                                  │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  📱 Enter Phone Number: [9876543210]                               │  │
│  │                                                                     │  │
│  │  🎁 Have a referral code?                                          │  │
│  │     [abcd1234] [Verify]                                            │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
         │
         │ User clicks "Verify"
         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  REFERRAL CODE VALIDATION (Client-Side)                                 │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  ReferralService.validateReferralCode("abcd1234")                  │  │
│  │                                                                     │  │
│  │  1. Check Firestore: referral_codes/abcd1234                      │  │
│  │  2. Verify code exists and is active                              │  │
│  │  3. Get referrer name: "John Doe"                                 │  │
│  │  4. Show: ✓ Valid code from John Doe                             │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
         │
         │ User clicks "Continue"
         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  SAVE REFERRAL CODE TO VIEWMODEL                                        │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  profileCompletionViewModel.saveReferralCode("abcd1234")          │  │
│  │                                                                     │  │
│  │  Stored in: DataStore (local storage)                             │  │
│  │  Key: REFERRAL_CODE                                                │  │
│  │  Value: "abcd1234"                                                 │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
         │
         │ Send OTP
         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  OTP VERIFICATION                                                        │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  📱 Enter OTP: [1] [2] [3] [4] [5] [6]                            │  │
│  │                                                                     │  │
│  │  Firebase Auth verifies OTP                                        │  │
│  │  Creates user: userId = "xyz789"                                   │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
         │
         │ OTP Verified ✓
         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  🎁 CRITICAL FIX: APPLY REFERRAL CODE (NEW!)                            │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  LaunchedEffect(otpState.otpVerified) {                            │  │
│  │    if (otpState.otpVerified && existingUserData == null) {         │  │
│  │      // New user - apply referral code                             │  │
│  │      val savedCode = profileCompletionViewModel.getReferralCode()  │  │
│  │                                                                     │  │
│  │      if (savedCode != null) {                                      │  │
│  │        profileCompletionViewModel.applyReferralCode(               │  │
│  │          referralCode = "abcd1234",                                │  │
│  │          newUserId = "xyz789",                                     │  │
│  │          newUserRole = "WORKER",                                   │  │
│  │          newUserName = "New User",                                 │  │
│  │          newUserPhone = "+919876543210"                            │  │
│  │        )                                                            │  │
│  │      }                                                              │  │
│  │    }                                                                │  │
│  │  }                                                                  │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
         │
         │ Call Cloud Function
         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  CLOUD FUNCTION: applyReferralCode                                      │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  STEP 1: VALIDATION                                                │  │
│  │  ─────────────────────────────────────────────────────────────────  │  │
│  │  ✓ Check referral code exists                                      │  │
│  │  ✓ Check code is active                                            │  │
│  │  ✓ Check user hasn't used a code before                           │  │
│  │  ✓ Check not self-referral                                         │  │
│  │  ✓ Get device fingerprint                                          │  │
│  │  ✓ Get IP address                                                  │  │
│  │                                                                     │  │
│  │  STEP 2: FRAUD DETECTION                                           │  │
│  │  ─────────────────────────────────────────────────────────────────  │  │
│  │  ✓ Check velocity (too many referrals in short time)              │  │
│  │  ✓ Check same device used multiple times                          │  │
│  │  ✓ Check same IP used multiple times                              │  │
│  │  ✓ Calculate fraud score (0-100)                                   │  │
│  │                                                                     │  │
│  │  STEP 3: CREATE REFERRAL RECORD                                    │  │
│  │  ─────────────────────────────────────────────────────────────────  │  │
│  │  referrals/{referralId}:                                           │  │
│  │    referrerUserId: "abc123"                                        │  │
│  │    referredUserId: "xyz789"                                        │  │
│  │    referredUserPhone: "+919876543210"  ← PHONE STORED HERE        │  │
│  │    referralCode: "abcd1234"                                        │  │
│  │    status: "PENDING"                                               │  │
│  │    createdAt: 2026-03-10T10:00:00Z                                 │  │
│  │    expiresAt: 2026-03-17T10:00:00Z (7 days)                        │  │
│  │    deviceFingerprint: "device123"                                  │  │
│  │    ipAddress: "103.x.x.x"                                          │  │
│  │                                                                     │  │
│  │  STEP 4: UPDATE USER STATS                                         │  │
│  │  ─────────────────────────────────────────────────────────────────  │  │
│  │  users/xyz789:                                                     │  │
│  │    phone: "+919876543210"                                          │  │
│  │    referralStats:                                                  │  │
│  │      referredByCode: "abcd1234"  ← CODE STORED (IMMUTABLE)        │  │
│  │      referredByUserId: "abc123"                                    │  │
│  │      referredAt: 2026-03-10T10:00:00Z                              │  │
│  │                                                                     │  │
│  │  users/abc123:                                                     │  │
│  │    referralStats:                                                  │  │
│  │      totalReferrals: 5 → 6                                         │  │
│  │      pendingReferrals: 2 → 3                                       │  │
│  │                                                                     │  │
│  │  STEP 5: LOG EVENT                                                 │  │
│  │  ─────────────────────────────────────────────────────────────────  │  │
│  │  referral_events/{eventId}:                                        │  │
│  │    eventType: "REFERRAL_CREATED"                                   │  │
│  │    referralId: "ref123"                                            │  │
│  │    referrerUserId: "abc123"                                        │  │
│  │    referredUserId: "xyz789"                                        │  │
│  │    timestamp: 2026-03-10T10:00:00Z                                 │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
         │
         │ Return success
         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  SHOW SUCCESS MESSAGE                                                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  Toast.makeText(                                                   │  │
│  │    "✓ Referral code applied! You earned ₹25 bonus",               │  │
│  │    Toast.LENGTH_LONG                                               │  │
│  │  ).show()                                                           │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
         │
         │ After 7 days...
         ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  CLOUD FUNCTION: completeReferral (Firestore Trigger)                   │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  Triggered when: referral status changes to COMPLETED              │  │
│  │                                                                     │  │
│  │  STEP 1: CALCULATE REWARDS                                         │  │
│  │  ─────────────────────────────────────────────────────────────────  │  │
│  │  Base Reward: ₹50 (referrer)                                       │  │
│  │  Tier Bonus: ₹10 (if Silver tier)                                  │  │
│  │  Milestone Bonus: ₹100 (if 10th referral)                          │  │
│  │  Total: ₹160                                                        │  │
│  │                                                                     │  │
│  │  New User Reward: ₹25                                              │  │
│  │                                                                     │  │
│  │  STEP 2: UPDATE BALANCES                                           │  │
│  │  ─────────────────────────────────────────────────────────────────  │  │
│  │  users/abc123:                                                     │  │
│  │    referralStats:                                                  │  │
│  │      availableBalance: ₹500 → ₹660                                 │  │
│  │      totalEarnings: ₹1000 → ₹1160                                  │  │
│  │      successfulReferrals: 9 → 10                                   │  │
│  │      pendingReferrals: 3 → 2                                       │  │
│  │      currentTier: "BRONZE" → "SILVER"                              │  │
│  │                                                                     │  │
│  │  users/xyz789:                                                     │  │
│  │    referralStats:                                                  │  │
│  │      availableBalance: ₹0 → ₹25                                    │  │
│  │      totalEarnings: ₹0 → ₹25                                       │  │
│  │                                                                     │  │
│  │  STEP 3: SEND NOTIFICATIONS                                        │  │
│  │  ─────────────────────────────────────────────────────────────────  │  │
│  │  notifications/{notifId1}:                                         │  │
│  │    recipientId: "abc123"                                           │  │
│  │    title: "🎁 Referral Reward!"                                    │  │
│  │    message: "You earned ₹160! New User joined using your code"    │  │
│  │    type: "REFERRAL_REWARD"                                         │  │
│  │                                                                     │  │
│  │  notifications/{notifId2}:                                         │  │
│  │    recipientId: "xyz789"                                           │  │
│  │    title: "🎁 Welcome Bonus!"                                      │  │
│  │    message: "You earned ₹25 for joining with a referral code!"    │  │
│  │    type: "SIGNUP_BONUS"                                            │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                         FINAL DATABASE STATE                             │
└─────────────────────────────────────────────────────────────────────────┘

referrals/ref123:
  referrerUserId: "abc123"
  referredUserId: "xyz789"
  referredUserPhone: "+919876543210"  ← PERMANENTLY LINKED
  referralCode: "abcd1234"
  status: "COMPLETED"
  createdAt: 2026-03-10T10:00:00Z
  completedAt: 2026-03-17T10:00:00Z

users/xyz789:
  phone: "+919876543210"  ← PERMANENTLY LINKED
  referralStats:
    referredByCode: "abcd1234"  ← IMMUTABLE, CANNOT CHANGE
    availableBalance: ₹25

users/abc123:
  referralStats:
    totalReferrals: 10
    successfulReferrals: 10
    availableBalance: ₹660
    currentTier: "SILVER"
```

---

## 🔒 SECURITY LAYERS

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PREVENTING DUPLICATE REFERRAL CODES                   │
└─────────────────────────────────────────────────────────────────────────┘

LAYER 1: UI PREVENTION
┌──────────────────────────────────────────────────────────────────────────┐
│  Profile Setup Screens                                                   │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  showReferralSection = false  (hardcoded)                          │  │
│  │  hasAlreadyUsedReferral = true  (hardcoded)                        │  │
│  │                                                                     │  │
│  │  Result: Referral input NOT shown after registration               │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘

LAYER 2: CLIENT VALIDATION
┌──────────────────────────────────────────────────────────────────────────┐
│  ProfileCompletionService.applyReferralCode()                            │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  // Check if user already used a code                              │  │
│  │  val existingReferrals = firestore.collection("referrals")         │  │
│  │    .whereEqualTo("referredUserId", userId)                         │  │
│  │    .limit(1)                                                        │  │
│  │    .get()                                                           │  │
│  │                                                                     │  │
│  │  if (!existingReferrals.isEmpty) {                                 │  │
│  │    return Result.failure("Already used a referral code")           │  │
│  │  }                                                                  │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘

LAYER 3: CLOUD FUNCTION VALIDATION
┌──────────────────────────────────────────────────────────────────────────┐
│  Cloud Function: applyReferralCode                                       │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  // Server-side check (cannot be bypassed)                         │  │
│  │  const existingReferrals = await db.collection("referrals")        │  │
│  │    .where("referredUserId", "==", userId)                          │  │
│  │    .limit(1)                                                        │  │
│  │    .get();                                                          │  │
│  │                                                                     │  │
│  │  if (!existingReferrals.empty) {                                   │  │
│  │    return { success: false, error: "Already used a code" };        │  │
│  │  }                                                                  │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘

LAYER 4: FIRESTORE RULES
┌──────────────────────────────────────────────────────────────────────────┐
│  firestore.rules                                                         │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │  match /users/{userId} {                                           │  │
│  │    allow update: if request.auth != null &&                        │  │
│  │      request.auth.uid == userId &&                                 │  │
│  │      // Referral code is IMMUTABLE                                 │  │
│  │      request.resource.data.referralCode ==                         │  │
│  │        resource.data.referralCode;                                 │  │
│  │  }                                                                  │  │
│  │                                                                     │  │
│  │  match /referrals/{referralId} {                                   │  │
│  │    allow create: if isAdmin();  // Only Cloud Functions            │  │
│  │    allow update: if false;      // Immutable                       │  │
│  │  }                                                                  │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘

RESULT: User can ONLY use referral code ONCE during initial registration
```

---

## 📊 DATA FLOW SUMMARY

```
USER INPUT                    STORAGE LOCATIONS
──────────                    ─────────────────

Phone: 9876543210    ──────►  users/{userId}/phone: "+919876543210"
                     ──────►  referrals/{refId}/referredUserPhone: "+919876543210"

Referral: abcd1234   ──────►  users/{userId}/referralStats/referredByCode: "abcd1234"
                     ──────►  referrals/{refId}/referralCode: "abcd1234"

User ID: xyz789      ──────►  users/xyz789
                     ──────►  referrals/{refId}/referredUserId: "xyz789"

IMMUTABILITY: Once stored, these values CANNOT be changed by users
SECURITY: Only Cloud Functions can write to these fields
PERMANENCE: Data persists forever (audit trail)
```

---

*Visual flow diagram - March 10, 2026*
*Shows complete referral system from registration to reward distribution*
