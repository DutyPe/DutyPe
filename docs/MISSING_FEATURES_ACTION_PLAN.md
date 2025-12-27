# DutyPe - Missing Features Action Plan

**Last Updated:** December 28, 2025  
**Total Missing Features:** 29  
**Estimated Timeline:** 17 weeks

---

## 📊 EXECUTIVE SUMMARY

| Category | Implemented | Missing | % Complete |
|----------|-------------|---------|------------|
| Authentication | 3/3 | 0 | 100% ✅ |
| Job Management | 3/3 | 0 | 100% ✅ |
| Ratings & Trust | 2/2 | 0 | 100% ✅ |
| Notifications | 2/2 | 0 | 100% ✅ |
| Security | 3/3 | 0 | 100% ✅ |
| Chat/Messaging | 0/1 | 1 | 0% ❌ |
| Payments/Fintech | 1/7 | 6 | 14% 🟡 |
| Verification | 0/6 | 6 | 0% ❌ |
| AI Features | 0/6 | 6 | 0% ❌ |
| Operations | 0/7 | 7 | 0% ❌ |
| Accessibility | 3/3 | 0 | 100% ✅ |
| **TOTAL** | **18/46** | **28** | **39%** |

---

## 🚨 PHASE 0: CRITICAL (Week 1)

### Feature 1: Real-time Chat/Messaging
- **Status:** ❌ UI Only
- **Complexity:** High
- **Files to Create:**
  - `services/ChatService.kt`
  - `models/ChatModels.kt`
  - `viewmodels/ChatViewModel.kt`
- **Firestore Collections:** `conversations`, `messages`
- **Steps:**
  1. Create Message and Conversation data classes
  2. Create ChatService with Firestore operations
  3. Add real-time listeners
  4. Update ChatDetailsScreen to use real data
  5. Add push notifications for new messages

### Feature 2: Trust Score Algorithm
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Formula:**
  - 25% Punctuality + 20% Job Completion + 20% Rating Average
  - 15% Payment History + 10% Dispute-Free + 10% Consistency
- **Files to Create:**
  - `services/TrustScoreService.kt`
  - `components/TrustScoreDisplay.kt`

---

## 🛡️ PHASE 1: ANTI-FRAUD (Weeks 2-3)

### Feature 3: Structured Job Titles ✅ COMPLETED
- **Status:** ✅ Implemented December 27, 2025
- **Complexity:** Low (2 hours)
- **File:** `PostJobScreen.kt`
- **What Was Done:**
  - Replaced free-text job title input with dropdown selection
  - Added 25 predefined job titles with icons
  - Auto-selects matching category when job title is chosen
  - Added anti-fraud info banner explaining the restriction
  - Prevents scam postings like "Earn ₹50,000/day from home"

### Feature 4: Location Consistency Check ✅ COMPLETED
- **Status:** ✅ Implemented December 27, 2025
- **Complexity:** Medium (4 hours)
- **File:** `PostJobScreen.kt`
- **What Was Done:**
  - Gets employer's current GPS location before job submission
  - Calculates distance between employer location and job location
  - Shows warning dialog if distance > 30km
  - Allows employer to proceed with warning acknowledgment
  - Logs all location checks for fraud analysis
  - Prevents remote scam centers from posting fake local jobs

### Feature 5: Pay Rate Guardrails
- **Status:** ❌ Not Started
- **Complexity:** Low (2 hours)
- **Logic:** Min/max validation per job category
- **File:** `PostJobScreen.kt`

### Feature 6: Community Reporting
- **Status:** ❌ Not Started
- **Complexity:** Medium (1 day)
- **Files to Create:**
  - `services/ReportService.kt`
  - `components/ReportJobButton.kt`
- **Firestore:** `job_reports` collection
- **Logic:** Auto-hide job after 3 reports

---

## ⚙️ PHASE 2: OPERATIONS (Weeks 4-5)

### Feature 7: Job Check-in (GPS)
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Logic:** Worker checks in when within 100m of job location
- **Files to Create:**
  - `services/CheckInService.kt`
  - `components/CheckInButton.kt`

### Feature 8: Job Check-out
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Logic:** Calculate duration, trigger payment flow
- **Files:** Same as check-in

### Feature 9: Promise Token
- **Status:** ❌ Not Started
- **Complexity:** Low
- **UI:** Digital job card/ticket when worker accepts
- **File:** `components/JobTicketCard.kt`

### Feature 10: Strike System
- **Status:** ❌ Not Started (Infrastructure exists)
- **Complexity:** Medium
- **Logic:** 3 no-shows = 7-day suspension
- **Files to Create:**
  - `services/StrikeService.kt`
- **Firestore:** Add `strikes` field to user profile

### Feature 11: Reliability Score
- **Status:** ❌ Not Started
- **Complexity:** Low
- **Logic:** "Did they show up?" Yes/No after job
- **Display:** Reliability % on profile

### Feature 12: Voice Chat
- **Status:** ❌ Not Started
- **Complexity:** High
- **Dependencies:** Add audio libraries to build.gradle
- **Files to Create:**
  - `utils/AudioRecorder.kt`
  - `components/VoiceMessagePlayer.kt`

### Feature 13: Location Sharing in Chat
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **File:** `ChatDetailsScreen.kt`
- **Add:** "Send Location" button

---

## 🔐 PHASE 3: TRUST & SAFETY (Weeks 6-9)

### Feature 14: Aadhaar OCR & Face Match
- **Status:** ❌ Not Started
- **Complexity:** High
- **Dependencies:** Add ML Kit to build.gradle
- **Files to Create:**
  - `services/VerificationService.kt`
  - `screens/AadhaarVerificationScreen.kt`

### Feature 15: Government Verified Badge
- **Status:** ❌ Not Started
- **Complexity:** Low
- **UI:** Green tick on verified profiles
- **File:** `components/VerifiedBadge.kt`

### Feature 16: Employer Business Verification
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Logic:** Require photo after 3rd job posting
- **Files:** `PostJobScreen.kt`, `VerificationService.kt`

### Feature 17: AI Scam Detection
- **Status:** ❌ Not Started
- **Complexity:** High
- **Dependencies:** Add Azure OpenAI SDK
- **Files to Create:**
  - `services/AIService.kt`
  - `utils/ScamDetector.kt`

### Feature 18: AI Job Rewriting
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Logic:** Auto-improve unclear job descriptions
- **File:** `AIService.kt`

### Feature 19: Risk Classification
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Logic:** SAFE/MODIFY/HIGH_RISK for jobs
- **File:** `AIService.kt`

---

## 🎯 PHASE 4: ACCESSIBILITY (Weeks 10-11)

### Feature 20: Audio Job Descriptions
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Logic:** Employer records 30s audio clip
- **Files:** `PostJobScreen.kt`, `AudioRecorder.kt`

### Feature 21: Map-First Interface ✅ COMPLETED
- **Status:** ✅ Implemented December 28, 2025
- **Complexity:** High
- **What Was Done:**
  - Created AzureMapView.kt - WebView-based Azure Maps component
  - Created AzureMapHtml.kt - HTML/JS for Azure Maps SDK
  - Updated JobMapScreen.kt to use Azure Maps instead of Google Maps
  - Uses Azure Maps JavaScript SDK via WebView
  - Full marker support with job pins (green for available, red for urgent)
  - User location tracking with blue dot marker
  - Popup info windows on marker click
  - Zoom controls and "My Location" button
  - Animated job card when marker is selected
  - Uses AZURE_MAPS_KEY from local.properties

### Feature 22: Landmark Navigation ✅ COMPLETED
- **Status:** ✅ Implemented December 28, 2025
- **Complexity:** Low
- **What Was Done:**
  - Added `landmark` field to JobListing model
  - Added landmark input field to EnhancedLocationSection in PostJobScreen
  - Landmark is saved to Firestore with job data
  - Landmark is displayed in JobDescriptionScreen (green card with 🏛️ icon)
  - Landmark is shown in JobMapScreen job cards
  - Helps workers find locations by recognizable landmarks

---

## 💰 PHASE 5: FINTECH (Weeks 12-17)

### Feature 23: Payment Status Tracking
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Logic:** Paid/Unpaid/Partial status
- **Firestore:** Add `paymentStatus` to jobs

### Feature 24: Non-payment Reporting
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Logic:** Worker reports non-payment with evidence
- **Files to Create:**
  - `services/PaymentReportService.kt`
  - `screens/ReportNonPaymentScreen.kt`

### Feature 25: Escrow/Trust Pay
- **Status:** ❌ Not Started
- **Complexity:** High
- **Dependencies:** Add Razorpay/Stripe SDK
- **Files to Create:**
  - `services/PaymentService.kt`
  - `services/EscrowService.kt`

### Feature 26: In-app Wallet
- **Status:** ❌ Not Started
- **Complexity:** High
- **Files to Create:**
  - `screens/WalletScreen.kt`
  - `services/WalletService.kt`

### Feature 27: Urgent Hiring Fee
- **Status:** ❌ Not Started
- **Complexity:** Medium
- **Logic:** ₹999 to blast notification to 500 workers
- **File:** `PostJobScreen.kt`, `PaymentService.kt`

### Feature 28: Contact Unlock ✅ COMPLETED
- **Status:** ✅ Implemented December 28, 2025
- **Complexity:** Low
- **What Was Done:**
  - First 3 applicants: Contact info visible for free
  - 4th+ applicants: Contact info locked with "Unlock Contact" button
  - Added `unlockedContacts` and `freeContactsRemaining` to ViewModel state
  - Added `isContactUnlocked()`, `unlockContact()`, `processContactUnlockPayment()` methods
  - Created ContactUnlockDialog with payment UI (₹29 per unlock)
  - Created FreeContactsBanner showing remaining free unlocks
  - Locked contacts show yellow lock icon, unlocked show green phone icon
  - Ready for Razorpay/Stripe integration in production

### Feature 29: AI Support Bot
- **Status:** ❌ Not Started
- **Complexity:** High
- **Files to Create:**
  - `screens/SupportBotScreen.kt`
  - `services/SupportBotService.kt`

---

## 📦 ADDITIONAL FEATURES

### Feature 30: AI Feedback Writing
- **Status:** ❌ Not Started
- **Logic:** Generate feedback from ratings

### Feature 31: Skill Verification & Badges
- **Status:** ❌ Not Started
- **Logic:** Skill tests, video demos, badges

### Feature 32: Repeat Hiring & Favorites
- **Status:** ❌ Not Started
- **Logic:** "Hire again" button, favorite workers list

---

## 🔧 DEPENDENCIES TO ADD

### build.gradle.kts additions needed:

```kotlin
// Chat & Voice
implementation("com.google.android.exoplayer:exoplayer:2.19.1")

// Maps
implementation("com.google.android.gms:play-services-maps:18.2.0")

// ML Kit (Verification)
implementation("com.google.mlkit:text-recognition:16.0.0")
implementation("com.google.mlkit:face-detection:16.1.5")

// Azure OpenAI (AI Features)
implementation("com.azure:azure-ai-openai:1.0.0-beta.5")

// Payments
implementation("com.razorpay:checkout:1.6.33")
```

---

## 📅 IMPLEMENTATION TIMELINE

| Week | Phase | Features |
|------|-------|----------|
| 1 | Phase 0 | Chat, Trust Score |
| 2-3 | Phase 1 | Anti-Fraud (4 features) |
| 4-5 | Phase 2 | Operations (7 features) |
| 6-9 | Phase 3 | Trust & Safety (6 features) |
| 10-11 | Phase 4 | Accessibility (3 features) |
| 12-17 | Phase 5 | Fintech (7 features) |

---

## ✅ QUICK WINS (Can do in 1 day)

1. **Structured Job Titles** - ✅ DONE
2. **Pay Rate Guardrails** - 2 hours
3. **Landmark Navigation** - ✅ DONE
4. **Government Verified Badge** - 2 hours
5. **Promise Token UI** - 4 hours
6. **Reliability Score** - 4 hours

---

## 🎯 RECOMMENDED START ORDER

1. ⚡ **Real-time Chat** (Most requested feature)
2. ⚡ **Trust Score Algorithm** (Core trust feature)
3. 🛡️ **Structured Job Titles** (Quick anti-fraud win)
4. 🛡️ **Community Reporting** (User safety)
5. ⚙️ **Check-in/Check-out** (Operations core)
6. ⚙️ **Strike System** (Reliability enforcement)

---

**Document Created:** December 27, 2025  
**Next Review:** After Phase 0 completion
