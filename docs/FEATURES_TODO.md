# DutyPe - Features To Implement

**Last Updated:** December 27, 2025  
**Total TODO Features:** 32 (3 completed since last update)

---

## FEATURE GAP ANALYSIS (December 27, 2025)

### ✅ IMPLEMENTED FEATURES (14 Total)
| # | Feature | Status |
|---|---------|--------|
| 1 | Two-way Rating System | ✅ Done |
| 2 | Developer Mode Detection | ✅ Done |
| 3 | Age Validation (18-70) | ✅ Done |
| 4 | Device Fingerprint Storage | ✅ Done |
| 5 | Hide Applied Jobs from Worker Screens | ✅ Done |
| 6 | Work History Timeline UI | ✅ Done |
| 7 | All Authentication (Google, OTP, Guest) | ✅ Done |
| 8 | Worker/Employer Profile Systems | ✅ Done |
| 9 | Job Posting (4-step wizard) | ✅ Done |
| 10 | Job Discovery & Location Matching | ✅ Done |
| 11 | Application Management | ✅ Done |
| 12 | Push Notifications (FCM) | ✅ Done |
| 13 | Notification Settings | ✅ Done |
| 14 | Employer My Ratings Screen | ✅ Done |

### ❌ MISSING FEATURES (32 Total)

#### HIGH PRIORITY - Version 1.0 Core
| # | Feature | Complexity |
|---|---------|------------|
| 1 | Real-time Chat/Messaging | High |
| 2 | Trust Score Algorithm | Medium |

#### ANTI-FRAUD Features (Version 1.0.1)
| # | Feature | Complexity |
|---|---------|------------|
| 3 | Structured Job Titles | Low |
| 4 | Location Consistency Check | Medium |
| 5 | Pay Rate Guardrails | Low |
| 6 | Community Reporting | Medium |

#### OPERATIONS Features (Version 1.1)
| # | Feature | Complexity |
|---|---------|------------|
| 7 | Job Check-in (GPS) | Medium |
| 8 | Job Check-out | Medium |
| 9 | "Promise" Token | Low |
| 10 | Strike System | Medium |
| 11 | Reliability Score | Low |
| 12 | Voice Chat | High |
| 13 | Location Sharing in Chat | Medium |

#### TRUST & SAFETY Features (Version 1.2)
| # | Feature | Complexity |
|---|---------|------------|
| 14 | Aadhaar OCR & Face Match | High |
| 15 | "Government Verified" Badge | Low |
| 16 | Employer Business Verification | Medium |
| 17 | AI Scam Detection | High |
| 18 | AI Job Rewriting | Medium |
| 19 | Risk Classification | Medium |

#### ACCESSIBILITY Features (Version 1.3)
| # | Feature | Complexity |
|---|---------|------------|
| 20 | Audio Job Descriptions | Medium |
| 21 | Map-First Interface | High |
| 22 | Landmark Navigation | Low |

#### FINTECH Features (Version 2.0)
| # | Feature | Complexity |
|---|---------|------------|
| 23 | Payment Status Tracking | Medium |
| 24 | Non-payment Reporting | Medium |
| 25 | Escrow/Trust Pay | High |
| 26 | In-app Wallet | High |
| 27 | Urgent Hiring Fee | Medium |
| 28 | Contact Unlock | Low |
| 29 | AI Support Bot | High |

#### ADDITIONAL Features
| # | Feature | Complexity |
|---|---------|------------|
| 30 | AI Feedback Writing | Medium |
| 31 | Skill Verification & Badges | High |
| 32 | Repeat Hiring & Favorites | Low |

---

## VERSION 1.0 - CORE FEATURES (Incomplete)

> These features have UI but need backend implementation

### 1. Real-time Chat/Messaging
- **Description:** Firebase-based real-time messaging between workers and employers
- **Current State:** UI mockup only (`ChatDetailsScreen.kt`)
- **Implementation:**
  - Create `messages` Firestore collection
  - Add real-time listeners for message updates
  - Implement message sending/receiving
  - Add push notifications for new messages

### 2. Two-way Rating System ✅ IMPLEMENTED
- **Description:** Worker rates employer after job, employer rates worker
- **Status:** ✅ Completed on December 24, 2025
- **Files Created:**
  - `models/RatingModels.kt` - Rating data models
  - `services/RatingService.kt` - Rating submission & retrieval
  - `components/JobRatingBottomSheet.kt` - Rating UI
  - `components/UserRatingDisplay.kt` - Rating display components
  - Updated `firestore.rules` - Rating collection rules
  - Updated `di/AppModule.kt` - RatingService provider

### 3. Trust Score Algorithm
- **Description:** Behavior-based trust score calculation
- **Current State:** Only profile completion % exists
- **Implementation:**
  ```
  Trust Score = 
    25% × Punctuality +
    20% × Job Completion Rate +
    20% × Rating Average +
    15% × Payment History +
    10% × Dispute-Free +
    10% × Consistency
  ```
  - Track job completions, no-shows, ratings
  - Display trust score on profiles
  - Use for job matching priority

### 3.5 Developer Mode Detection ✅ IMPLEMENTED
- **Description:** Detect Developer Mode on device and show security warning
- **Status:** ✅ Completed on December 24, 2025
- **Why:** Developer Mode allows fake GPS locations via third-party apps, enabling fraud
- **Implementation:**
  - `DeveloperModeChecker` utility to detect developer options, USB debugging, mock locations
  - `DeveloperModeWarningSheet` non-dismissible bottom sheet with security warning
  - Integrated into `MainActivity.kt` - checks on app start and resume
  - User must exit app if Developer Mode is enabled
- **Files:**
  - `components/DeveloperModeWarningSheet.kt` - Detection utility + warning UI
  - `MainActivity.kt` - Integration with lifecycle

### 3.6 Age Validation (18-70 years) ✅ IMPLEMENTED
- **Description:** Profile setup screens validate date of birth to ensure users are between 18-70 years old
- **Status:** ✅ Completed on December 25, 2025
- **Why:** Legal compliance and appropriate user age range for job marketplace
- **Implementation:**
  - `ValidationUtils.kt` - Added `isValidDateOfBirth()`, `getAgeFromDateOfBirth()`, `getDateOfBirthError()`
  - Updated `MandatoryWorkerProfileSetupScreen.kt` and `MandatoryEmployerProfileSetupScreen.kt`
  - Shows appropriate error messages for underage (<18) or overage (>70) users

### 3.7 Device Fingerprint Storage ✅ IMPLEMENTED
- **Description:** Store device fingerprint in phone_roles collection for fraud prevention
- **Status:** ✅ Completed on December 25, 2025
- **Why:** Track devices to prevent banned users from creating new accounts
- **Implementation:**
  - `DeviceFingerprint` object in `ProfileCompletionService.kt`
  - Stores: `deviceFingerprint`, `deviceModel`, `androidId`, `joinedAt`, `updatedAt`
  - Data stored in `phone_roles` collection alongside phone number and role

### 3.8 Hide Applied Jobs from Worker Screens ✅ IMPLEMENTED
- **Description:** Jobs that worker has already applied to are hidden from home and all jobs screens
- **Status:** ✅ Completed on December 25, 2025
- **Why:** Cleaner UX - applied jobs only appear in MyJobsScreen's applied jobs tab
- **Implementation:**
  - Updated `WorkerHomeScreen.kt` - filters out applied jobs from job list
  - Updated `AllJobsScreen.kt` - filters out applied jobs from job list
  - Applied jobs still visible in MyJobsScreen for tracking

---

## VERSION 1.0.1 - ANTI-FRAUD (Next Priority)

### 4. Structured Job Titles
- **Description:** Remove free-text job titles, use predefined list only
- **Why:** Eliminates "Earn ₹50,000/day working from home" scams instantly
- **Implementation:** Modify `PostJobScreen.kt` to use dropdown only

### 5. Location Consistency Check
- **Description:** Compare employer GPS with job location (flag if >50km)
- **Why:** Prevents remote scam centers from posting local jobs
- **Implementation:** Add validation in job posting flow

### 6. Pay Rate Guardrails
- **Description:** Min/Max validation on salary fields
- **Why:** Error if someone tries to offer ₹10,000 for 1 hour of cleaning
- **Implementation:** Add validation rules based on job category

### 7. Community Reporting
- **Description:** "Report Fake Job" button, auto-hide after 3 reports
- **Why:** Crowdsourced fraud detection
- **Implementation:** Add report button, create `job_reports` collection

---

## VERSION 1.1 - OPERATIONS & RELIABILITY

### 8. Job Check-in (GPS)
- **Description:** Worker checks in when arriving at job location
- **Why:** GPS proof prevents fake check-ins, creates legal record
- **Implementation:** 
  - Add check-in button on accepted job card
  - Capture GPS + timestamp
  - Verify within 100m of job location

### 9. Job Check-out
- **Description:** Worker checks out with duration calculation
- **Why:** Auto-calculate hours worked, trigger payment flow
- **Implementation:**
  - Add check-out button
  - Calculate duration: check-out - check-in
  - Send notification to employer

### 10. "Promise" Token
- **Description:** Digital job card when worker accepts
- **Why:** Psychology: "I have a ticket, I must go"
- **Implementation:** Generate visual job card/ticket UI

### 11. Strike System
- **Description:** 3 no-shows = 7-day suspension
- **Why:** Enforces seriousness, reduces attrition
- **Implementation:**
  - Track no-shows in user profile
  - Auto-suspend after 3 strikes
  - Show strike count to user

### 12. Reliability Score
- **Description:** "Did they show up?" Yes/No rating after job
- **Why:** Build visible reliability percentage
- **Implementation:**
  - Add simple Yes/No question after job
  - Calculate and display reliability %

### 13. Voice Chat
- **Description:** In-app audio messages (like WhatsApp voice notes)
- **Why:** Essential for explaining precise directions
- **Implementation:** Audio recording + playback in chat

### 14. Location Sharing in Chat
- **Description:** "Send Current Location" button in chat
- **Why:** Help workers find exact job location
- **Implementation:** Add location share button in `ChatDetailsScreen.kt`

---

## VERSION 1.2 - TRUST & SAFETY

### 15. Aadhaar OCR & Face Match
- **Description:** Scan Aadhaar card → Extract Name → Compare with Selfie
- **Why:** Government-level identity verification
- **Implementation:**
  - ML Kit for OCR
  - Face detection for comparison
  - Store verification status

### 16. "Government Verified" Badge
- **Description:** Green tick for Aadhaar-verified profiles
- **Why:** Visual trust indicator
- **Implementation:** Add badge to profile UI

### 17. Employer Business Verification
- **Description:** Employers posting >3 jobs must upload shop/office photo
- **Why:** Proves they are a real business entity
- **Implementation:**
  - Track job count per employer
  - Require photo after 3rd job
  - Manual review queue

### 18. AI Scam Detection
- **Description:** Azure OpenAI to scan jobs before publishing
- **Why:** Prevents 99% of obvious scams
- **Implementation:**
  ```
  Employer submits job
    ↓
  Azure OpenAI analyzes:
    - Scam patterns: "advance fee", "registration", "deposit"
    - Wage realism: ₹50/hr vs ₹5000/day
    - Safety: No "bring documents" requests
    ↓
  Classification: SAFE / MODIFY / HIGH_RISK
  ```
- **Cost:** ₹0.10 per job analysis

### 19. AI Job Rewriting
- **Description:** Auto-improve unclear job descriptions
- **Why:** 3x more applications for clear jobs
- **Implementation:**
  ```
  Input: "need boy urgent, cook, flexible hours"
  Output: "Cook needed, flexible hours, competitive pay, immediate start"
  ```

### 20. Risk Classification
- **Description:** Safe/Modify/Block classification for jobs
- **Why:** Transparent feedback to employers
- **Implementation:**
  - 🟢 SAFE → Auto-publish
  - 🟡 MODIFY → Suggest edits
  - 🔴 HIGH_RISK → Block with explanation

---

## VERSION 1.3 - ACCESSIBILITY


### 22. Audio Job Descriptions
- **Description:** Employer can record 30s audio clip
- **Why:** "Need someone to unload a truck at 5 PM" is clearer spoken
- **Implementation:**
  - Audio recording in job posting
  - Playback button on job card

### 23. Map-First Interface
- **Description:** Full-screen map showing job pins
- **Why:** Workers recognize landmarks better than street names
- **Implementation:** Google Maps SDK with job markers

### 24. Landmark Navigation
- **Description:** "Near the big temple" style directions
- **Why:** Non-IT workers navigate by landmarks
- **Implementation:** Allow employers to add landmark descriptions

---

## VERSION 2.0 - FINTECH & MONETIZATION

### 25. Payment Status Tracking
- **Description:** Paid/Unpaid/Partial status for completed jobs
- **Why:** Workers know payment status instantly
- **Implementation:**
  - Add payment status field to job
  - UI to show payment state

### 26. Non-payment Reporting
- **Description:** Worker reports non-payment with evidence
- **Why:** Protect workers from payment fraud
- **Implementation:**
  - "Payment Issue" button
  - Collect evidence (GPS, timestamps, chat)
  - Auto-restrict non-paying employers

### 27. Escrow/Trust Pay
- **Description:** Employer deposits money before shift starts
- **Why:** Solves "Worker fear of not getting paid"
- **Implementation:**
  - Payment gateway integration
  - Hold funds until job completion
  - Auto-release on confirmation

### 28. In-app Wallet
- **Description:** Payment released to worker wallet on completion
- **Why:** Instant payment, no chasing employer
- **Implementation:**
  - Wallet balance UI
  - Withdrawal to bank account

### 29. Urgent Hiring Fee
- **Description:** Employer pays ₹49 to blast notification to 500 nearby workers
- **Why:** Monetization for urgent needs
- **Implementation:**
  - Premium job posting option
  - Push to nearby workers instantly

### 30. Contact Unlock
- **Description:** First 3 applicants free, pay to see 4th+ applicant details
- **Why:** Monetization without blocking core flow
- **Implementation:**
  - Track applicant count
  - Paywall after 3rd applicant

### 31. AI Support Bot
- **Description:** Context-aware help using user data
- **Why:** Reduces support tickets by 80%
- **Implementation:**
  ```
  User: "Why can't I apply for office jobs?"
  Bot: "Office jobs require trust score 70.
       You're at 52.
       Complete 2 more on-time jobs (+10 points)
       Add resume (+5 points)
       = 67 (almost there!)"
  ```

---

## ADDITIONAL FEATURES (Future)

### 32. Work History Timeline UI ✅ IMPLEMENTED
- **Description:** LinkedIn-style immutable work history timeline
- **Status:** ✅ Completed on December 25, 2025
- **Why:** More trustworthy than resume - shows verified work history
- **Implementation:**
  - Timeline view grouped by month (e.g., "DEC 2025")
  - Each job shows: status icon, job title, company, pay, job type
  - Completed jobs show star rating and feedback quote
  - Visual timeline with dashed connecting lines
  - Three tabs: Timeline (completed/accepted), Completed, All History
- **Files:**
  - `worker/screens/history/WorkerHistoryScreen.kt` - Complete rewrite with timeline UI
- **Example Display:**
  ```
  DEC 2025
  ✅ Shop Helper – ₹400/day – Daily
     ⭐⭐⭐⭐⭐ 5.0 | "Excellent worker"
  ```

### 33. AI Feedback Writing
- **Description:** Generate professional feedback from ratings
- **Why:** Prevents abusive language, normalizes ratings
- **Implementation:**
  ```
  Input: Punctuality ⭐⭐⭐⭐⭐, Quality ⭐⭐⭐⭐
  Output: "Worker arrived on time, delivered high-quality work."
  ```

### 34. Skill Verification & Badges
- **Description:** Skill tests, video demonstrations, micro-credentials
- **Why:** Prove skills beyond self-reporting
- **Implementation:** Skill test modules, badge system

### 35. Repeat Hiring & Favorites
- **Description:** "Hire again" button, preferred worker lists
- **Why:** Reduce friction for repeat employers
- **Implementation:**
  - Favorite workers list
  - Quick re-hire flow

### 36. Advanced Trust Score Algorithm
- **Description:** Full behavior-based trust calculation (moved from #3 for full implementation)
- **Note:** Basic trust score in #3, this is the advanced version with all factors

---

## IMPLEMENTATION PRIORITY

### Phase 0: Core Completion (Week 1) ⚡ HIGHEST PRIORITY
1. Real-time Chat/Messaging
2. Two-way Rating System
3. Trust Score Algorithm

### Phase 1: Safety (Weeks 2-3)
4. Community Reporting
5. Pay Rate Guardrails
6. Location Consistency Check

### Phase 2: Operations (Weeks 4-5)
7. Job Check-in/Check-out
8. Strike System
9. Reliability Score

### Phase 3: AI Integration (Weeks 6-9)
10. AI Scam Detection
11. AI Job Rewriting
12. AI Support Bot

### Phase 4: Fintech (Weeks 10-13)
13. Payment Status Tracking
14. Non-payment Reporting
15. Escrow/Trust Pay

### Phase 5: Accessibility (Weeks 14-17)
16. Map-First Interface

---

## AZURE OPENAI SETUP (For AI Features)

### Setup Steps
1. Create Azure account at portal.azure.com
2. Create "DutyPe" resource group
3. Create "Azure OpenAI Service" resource
4. Select "East US" region (cheapest)
5. Deploy gpt-4o-mini model
6. Note down:
   - AZURE_OPENAI_ENDPOINT
   - AZURE_OPENAI_API_KEY
   - DEPLOYMENT_NAME

### Cost Estimate
- gpt-4o-mini: $0.00015 per 1K input tokens
- ~200 tokens per job analysis = $0.00003 per job
- 1M jobs/month = $30/month

---

**Total Features: 36**  
**Estimated Timeline: 17 weeks**
