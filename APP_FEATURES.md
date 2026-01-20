# DutyPe - Complete App Documentation & Feature Audit

**Last Updated:** December 30, 2025  
**App Version:** 22.0  
**Package:** com.dutype.app  
**Platform:** Android (Kotlin + Jetpack Compose)

---

## 🎯 ABOUT DUTYPE

### What is DutyPe?
DutyPe is **India's first hyperlocal job marketplace** specifically designed for blue-collar workers. We connect local workers with local employers for jobs within walking distance - from home helpers to delivery executives, cooks to security guards.

### Our Motto
> **"Connecting Local Jobs with Local Workers"**

### Our Vision
To become India's most trusted hyperlocal job marketplace for blue-collar workers, eliminating fraud and creating genuine employment opportunities within walking distance.

### Our Mission
- Make job discovery as simple as opening an app
- Eliminate fake jobs and scams from the platform
- Empower workers with fair wages and ratings
- Help employers find reliable local workers instantly

### Why DutyPe Will Win
- **Existing Apps:** Are "Search Engines" for jobs
- **DutyPe:** Is a "Community Connection" platform with ZERO FRAUD tolerance

### Target Market
**450+ Million Blue-Collar Workers in India** including:
- Domestic helpers, maids, cooks
- Drivers, delivery executives
- Security guards, watchmen
- Shop helpers, sales assistants
- Waiters, restaurant staff
- Construction workers, painters
- Electricians, plumbers, carpenters

---

## 🏛️ THREE CORE PILLARS

### 1. HYPER-LOCAL
- 100% location-dependent job matching
- Jobs sorted by GPS distance (not just city)
- "400 meters away" matters more than addresses
- Landmark-based navigation for workers
- "Near the big temple" is valid location context
- **Map Radar** - Uber-style pulsing dots showing jobs nearby

### 2. BLUE-COLLAR FIRST
- Interfaces designed for low-literacy users
- Visual, voice-first, simple design
- No resumes required
- Profile = Photo + Name + Phone + 3 Skills
- 1-tap apply with auto-filled profile
- Telugu/English language support (planned)
- **Audio Job Descriptions** - TTS for semi-literate users

### 3. ZERO FRAUD
- Aggressive filtering of fake jobs and scams
- Structured job titles (dropdown only, no free text)
- **No-Data-Entry Firewall** - Auto-block WFH/Online scams
- Location consistency check (30km flag)
- Pay rate guardrails (min/max per category)
- Community reporting (3 reports = auto-hide)
- Video job descriptions (planned)
- **Aadhaar Face Match** - Live selfie vs card photo
- "Genuinity" is our product

---

## 🎯 THE "GENUINE JOBS" GOLDMINE STRATEGY

### Target These First (High Trust, Low Scam)

> **Do NOT compete with "Data Entry" or "Office Assistant" jobs initially—that's where scams are.**
> **Start with jobs that CANNOT be faked because they require physical presence.**

#### A. The "Visible" Jobs (High Trust)

| Category | Examples | Why It's Safe |
|----------|----------|---------------|
| **Retail Helpers** | Saree folding boy, Kirana shop helper | You can see the shop. It exists. |
| **Food & Beverage** | Tea master, Fast food cutter, Weekend waiter | High turnover. They need people TODAY. |
| **Domestic Help** | Maid for 2 hours, Elderly care night shift | Community vouching (neighbors verify neighbors) |

#### B. The "Micro-Gig" (Gap No One is Filling)

| Category | Examples | Pay Model |
|----------|---------|-----------|
| **Event Staff** | 5 boys to serve food at wedding tonight | ₹500 instant |
| **Loading/Unloading** | 2 people to shift furniture to 2nd floor | Per task |
| **Construction** | 3 helpers for mixing cement for 1 day | Daily wage |

---

## 💪 DUTYPE "POWER FEATURES" (To Be Superior)

### 🔥 Feature 1: "No-Data-Entry" Firewall 🚫 ❌ NOT IMPLEMENTED
**Smart Keyword Ban - Auto-Block WFH Scams**

| Banned Keywords | Action |
|-----------------|--------|
| Online, SMS, Investment, Part-time WFH | AUTO-BLOCK |
| Laptop required, PDF conversion | AUTO-BLOCK |
| Work from home, Data entry | AUTO-BLOCK |
| Typing job, Form filling | AUTO-BLOCK |

**Logic:**
```
IF job_title OR job_description CONTAINS banned_keywords:
    → Block post immediately
    → Show warning: "DutyPe is for PHYSICAL location jobs only."
```

**Impact:** Eliminates 90% of scam jobs instantly. No "Earn ₹50k from home" garbage.

---

### 🔥 Feature 2: Video Job Description 🎥 ❌ NOT IMPLEMENTED
**The Killer Feature for Trust**

| Problem | DutyPe Fix |
|---------|------------|
| Text is easy to fake. "Data Entry Job ₹30k" is a lie. | Employer MUST record a 15-second video to post a job |

**Example Script:**
> "Hi, I am Suresh from Laxmi Bakery in Ameerpet. I need a helper who knows how to bake puffs. Salary ₹12,000. Come meet me."

**Impact:** A scammer sitting in a dark room CANNOT do this. A real shop owner CAN. **Trust +100%**

---

### 🔥 Feature 3: Hyper-Local Radar Map 🗺️ ❌ NOT IMPLEMENTED
**Dark-Mode Map with Pulsating Job Dots**

| Current State | DutyPe Fix |
|---------------|------------|
| Static map pins | Uber-style pulsing animated dots |
| No visual urgency | Pulsing = "This job is HOT right now" |
| No walking distance filter | "Show jobs within 500m walking distance" |
| Light mode only | Dark-mode map for better visibility |

**Visual Design:**
```
┌─────────────────────────────────────────────────────────────┐
│                    🗺️ HYPER-LOCAL RADAR                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│     [Dark Mode Map Background]                               │
│                                                              │
│           ◉ (pulsing red)     ← Urgent job                  │
│                    🧑 YOU                                    │
│        ◉ (pulsing orange)                                   │
│                         ◉ (pulsing green)                   │
│     ◉ (pulsing orange)                                      │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  Filter: [500m ▼] [1km] [2km] [5km] [All]           │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  📍 5 jobs within 500m walking distance                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Pulsing Dot Colors:**
| Color | Urgency | Animation |
|-------|---------|-----------|
| 🔴 Red | IMMEDIATE (Today) | Fast pulse (0.5s) |
| 🟠 Orange | URGENT (This Week) | Medium pulse (1s) |
| 🟢 Green | NORMAL | Slow pulse (2s) |
| 🔵 Blue | FILLED (for reference) | No pulse |

**Use Case - Maid Example:**
```
A maid opens the app and sees:
- 3 jobs in her apartment complex (within 200m)
- 2 more jobs in the next building (within 500m)

She can take ALL 5 jobs in one day without spending on transport!
Gap Filled: Saves petrol money. Workers can cluster jobs geographically.
```

**Implementation:**
```kotlin
// Pulsing animation for map markers
@Composable
fun PulsingJobMarker(
    job: JobListing,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (job.urgency) {
                    "IMMEDIATE" -> 500
                    "URGENT" -> 1000
                    else -> 2000
                }
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val color = when (job.urgency) {
        "IMMEDIATE" -> Color.Red
        "URGENT" -> Color(0xFFFF9800)
        else -> Color.Green
    }
    
    Box(
        modifier = Modifier
            .scale(scale)
            .size(24.dp)
            .background(color.copy(alpha = 0.7f), CircleShape)
            .clickable { onClick() }
    )
}

// Distance filter options
enum class DistanceFilter(val meters: Int, val label: String) {
    WALKING_500M(500, "500m"),
    WALKING_1KM(1000, "1km"),
    CYCLING_2KM(2000, "2km"),
    NEARBY_5KM(5000, "5km"),
    ALL(Int.MAX_VALUE, "All")
}
```

**Features:**
- Dark-mode map for better dot visibility
- Pulsing animation based on urgency
- Distance filter (500m, 1km, 2km, 5km, All)
- Job count badge ("5 jobs within 500m")
- Tap dot to see job preview
- Cluster nearby jobs for cleaner view

**Impact:** 
- Workers can plan routes efficiently
- Saves transport costs
- Maids/helpers can take multiple jobs in same area
- Makes job discovery feel ALIVE and URGENT

---

### 🔥 Feature 4: Market Rate Suggestions 💰 ❌ NOT IMPLEMENTED
**App Suggests Local Market Rate When Posting**

| Category | Suggested Rate | Range |
|----------|----------------|-------|
| Cook (Full-time) | ₹12,000/month | ₹8,000 - ₹18,000 |
| Maid (Part-time) | ₹3,000/month | ₹2,000 - ₹5,000 |
| Driver (Personal) | ₹15,000/month | ₹12,000 - ₹20,000 |
| Helper (Shop) | ₹8,000/month | ₹6,000 - ₹12,000 |
| Security (Night) | ₹10,000/month | ₹8,000 - ₹15,000 |

**Flow:**
1. Employer selects category (e.g., Cook)
2. App shows: "💡 Suggested rate for Cook in your area: ₹12,000/month"
3. Employer can adjust within range
4. If too low: Warning "This is below market rate. You may not get applicants."
5. If too high: Warning "This is above market rate. Are you sure?"

**Impact:** Fair wages for workers. No exploitation. No unrealistic expectations.

---

### 🔥 Feature 5: "The Rate Card" (Anti-Looting) 📋 ❌ NOT IMPLEMENTED
**Standard DutyPe Rate Card for Services**

| Service | Standard Rate | Parts Extra |
|---------|---------------|-------------|
| Fan Capacitor Change | ₹150 | + Parts |
| AC Service (Split) | ₹500 | - |
| AC Service (Window) | ₹400 | - |
| Geyser Repair | ₹300 | + Parts |
| Washing Machine Service | ₹400 | + Parts |
| RO Service | ₹350 | + Parts |
| Electrician (per hour) | ₹200 | + Parts |
| Plumber (per hour) | ₹250 | + Parts |
| Carpenter (per hour) | ₹300 | + Parts |

**Display:** Before user calls worker, show:
> "📋 DutyPe Standard Rate Card for AC Service: ₹500"
> "This protects you from overcharging."

**Impact:** Massive trust. Users feel "protected" by DutyPe's standard rates.

---

### 🔥 Feature 6: Aadhaar Face Match 🆔 ❌ NOT IMPLEMENTED
**100% Real Humans. Zero Bots.**

| Problem | DutyPe Fix |
|---------|------------|
| People upload fake Aadhaar cards | Live selfie + Face match verification |

**Flow:**
1. User uploads Aadhaar Card photo
2. App forces **Live Selfie** (with blink detection)
3. AI API (HyperVerge/DigiLocker) matches:
   - Face on Aadhaar Card vs Live Selfie
   - Name extraction via OCR
4. If match > 85%: ✅ "Aadhaar Verified" badge
5. If mismatch: ❌ Reject verification

**Tech Options:**
- HyperVerge API (paid)
- DigiLocker API (government)
- Manual verification initially

**Impact:** 100% Real Humans. Zero fake profiles. Zero bots.

---

### 🔥 Feature 7: Background Safe-Check 🛡️ ❌ NOT IMPLEMENTED
**For Home-Entry Jobs (Maid, Electrician, Plumber)**

| What User Sees | Data Shown |
|----------------|------------|
| Worker's Aadhaar-verified face | ✅ Photo from verification |
| Jobs completed in THIS colony | "12 jobs done in Ameerpet" |
| Rating from neighbors | "4.8★ from 8 reviews in your area" |
| Verification status | "Aadhaar Verified ✓" |

**Display Before Hiring:**
```
┌─────────────────────────────────────┐
│  🛡️ BACKGROUND SAFE-CHECK          │
├─────────────────────────────────────┤
│  [PHOTO]  Ramesh Kumar              │
│           ✅ Aadhaar Verified       │
│           📍 12 jobs in Ameerpet    │
│           ⭐ 4.8 (8 reviews nearby) │
│           🏠 Trusted for home entry │
└─────────────────────────────────────┘
```

**Impact:** Users feel SAFE letting workers into their homes.

---

### 🔥 Feature 8: SOS Panic Button 🆘 ❌ NOT IMPLEMENTED
**Emergency Button During Active Jobs**

| Feature | Description |
|---------|-------------|
| Floating SOS button | Appears when job is "In Progress" |
| One-tap emergency | Sends location to emergency contacts |
| Auto-call option | Calls 100 (Police) or saved contact |
| Job tracking | Time-based tracking while job is active |

**Flow:**
1. Worker accepts job → Job status = "In Progress"
2. Floating 🆘 button appears on both Worker & Employer app
3. Tap SOS → Options: "Call Police", "Call Emergency Contact", "Share Location"
4. Location shared with timestamp
5. Job auto-ends after X hours (safety timeout)

**Impact:** Safety for workers AND employers. Trust +200%.

---

### 🔥 Feature 9: "Postpaid" Model for Employers 💰 ✅ IMPLEMENTED
**Pay-Per-Contact Model**

| Problem | DutyPe Fix |
|---------|------------|
| Scammers pay small fees to post fake bulk jobs | Posting is FREE. Employers pay ₹29-49 to "Unlock Candidate's Phone Number" |

**Current Implementation:**
- First 3 contacts FREE
- ₹29 per additional contact (Basic plan)
- Unlimited contacts (Pro/Enterprise)

**Impact:** Scammers won't pay ₹50 per person. Genuine shop owners will happily pay.

---

### 🔥 Feature 10: Worker-Centric Rating ⭐ ✅ IMPLEMENTED
**Workers Rate Employers Too**

| Problem | DutyPe Fix |
|---------|------------|
| Apps rate workers, but nobody rates employers. Workers get abused or not paid. | After job completion, worker rates employer on 3 things |

**Rating Categories:**
1. Did they pay on time? 💰
2. Was the behavior good? 🤝
3. Was the job location real? 📍

**Result:** If a shop owner abuses a worker, their "Trust Score" drops. No one will join them next time.

**Current Implementation:** `JobRatingBottomSheet.kt`, `RatingService.kt`

---

### 🔥 Feature 11: "Local Reference" (Community Vouch) 🏘️ ❌ NOT IMPLEMENTED
**Digitizing "Do You Know Him?"**

| Concept | Implementation |
|---------|----------------|
| When worker signs up, ask: "Who knows you in this colony?" | If they add a reference (local Pan Shop owner / contact on app), give "Verified Local" badge |

**Why:** In India, we hire based on "Do you know him?" Digitizing this "Reference" is powerful.

---

### 🔥 Feature 12: Selfie with Shop Board 📸 ❌ NOT IMPLEMENTED
**Shop Owner Verification**

| Rule | Status |
|------|--------|
| Shop owner must upload selfie standing in front of shop board | ❌ NOT IMPLEMENTED |

---

### 🔥 Feature 13: "No-Consultancy" Filter 🚫 ❌ NOT IMPLEMENTED
**Algorithm Rule to Block Fake Recruiters**

| Rule | Action |
|------|--------|
| If one user posts jobs in 5+ different categories (Driver, Nurse, Java Dev, Data Entry) | AUTO-BLOCK |

**Why:** Real shop owners only hire for THEIR shop. Consultancies post across categories.

---

## 👥 USER ROLES & USE CASES

### For Workers (Job Seekers)

| Use Case | Description | Status |
|----------|-------------|--------|
| Find Nearby Jobs | See jobs sorted by distance from current location | ✅ |
| Map Radar View | Uber-style pulsing dots on map | ❌ |
| Quick Apply | 1-tap apply with auto-filled profile | ✅ |
| Track Applications | Visual timeline: Pending → Under Review → Accepted | ✅ |
| Build Reputation | Earn ratings from employers | ✅ |
| Digital Identity | Shareable visiting card for WhatsApp | ✅ |
| Save Jobs | Bookmark interesting jobs for later | ✅ |
| Get Notified | Push notifications for new jobs & updates | ✅ |
| Work History | LinkedIn-style timeline of completed jobs | ✅ |
| Rate Employers | Rate employer after job completion | ✅ |
| Voice Job Descriptions | Listen to job details (TTS) | ❌ |
| WhatsApp Apply | One-click apply via WhatsApp | ❌ |
| SOS Panic Button | Emergency button during active jobs | ❌ |
| Aadhaar Verification | Face match for "Verified" badge | ❌ |
| Background Safe-Check | Show verification to employers | ❌ |

### For Employers (Job Posters)

| Use Case | Description | Status |
|----------|-------------|--------|
| Post Jobs Quickly | 4-step wizard with GPS auto-fill | ✅ |
| Market Rate Suggestion | App suggests local market rate | ❌ |
| Find Local Workers | Location-based matching | ✅ |
| Manage Applications | View, accept, reject applicants | ✅ |
| View Worker Profiles | See skills, experience, ratings | ✅ |
| Background Safe-Check | See worker's verification status | ❌ |
| Rate Workers | Build community trust | ✅ |
| Analytics Dashboard | Track job performance | ✅ |
| Trust Badges | Verified/Trusted/Business tiers | ✅ |
| Subscription Plans | Free/Basic/Pro/Enterprise | ✅ |
| Video Job Description | 15-second video for trust | ❌ |
| Job Poster PDF | Print poster for shop window | ❌ |
| Rate Card Display | Show standard rates to workers | ❌ |
| SOS Panic Button | Emergency button during active jobs | ❌ |


---

## 📱 COMPLETE FEATURE LIST

### ✅ IMPLEMENTED FEATURES (52+ Total)

#### Authentication & Onboarding (8 Features)

| # | Feature | Status | File(s) | Description |
|---|---------|--------|---------|-------------|
| 1 | Phone OTP Login | ✅ Done | `OtpViewModel.kt` | Firebase Auth with SMS verification |
| 2 | Google Sign-In | ✅ Done | `GoogleSignInManager.kt` | Credential Manager API |
| 3 | Guest Mode | ✅ Done | `EnhancedLoginScreen.kt` | Browse without account |
| 4 | Role Selection | ✅ Done | `SelectRoleScreen.kt` | "I want a Job" / "I want to Hire" |
| 5 | Animated Onboarding | ✅ Done | `OnboardingScreen.kt` | Lottie animations |
| 6 | Dual-role Prevention | ✅ Done | `phone_roles` collection | Same phone can't be both roles |
| 7 | Age Validation | ✅ Done | `ValidationUtils.kt` | Users must be 18-70 years |
| 8 | Device Fingerprint | ✅ Done | `DeviceFingerprintService.kt` | Fraud prevention |

#### Worker Features (20 Features)

| # | Feature | Status | File(s) | Description |
|---|---------|--------|---------|-------------|
| 1 | Worker Profile System | ✅ Done | `WorkerProfile.kt` | Complete profile management |
| 2 | Profile Completion % | ✅ Done | `ProfileCompletionService.kt` | Gamified progress bar |
| 3 | Mandatory Profile Setup | ✅ Done | `MandatoryWorkerProfileSetupScreen.kt` | Required fields wizard |

**Worker Profile Completion Weights:**
| Field | Weight | Required |
|-------|--------|----------|
| Full Name | 10% | ✅ Yes |
| Email | 10% | ✅ Yes |
| Phone | 10% | ✅ Yes |
| Address | 20% | ✅ Yes |
| Date of Birth | 10% | ✅ Yes |
| Gender | 5% | ✅ Yes |
| Skills | 15% | ✅ Yes |
| Experience | 15% | ✅ Yes |
| Profile Picture | 5% | ❌ Optional |

> **Note:** Profile picture is optional (5% weight). Users can apply for jobs with 95% completion (without selfie). Minimum 80% required to apply for jobs.

| 4 | Job Discovery | ✅ Done | `WorkerHomeScreen.kt` | Browse all jobs |
| 5 | Location-based Matching | ✅ Done | `FirestoreJobViewModel.kt` | Haversine formula |
| 6 | Job Filtering | ✅ Done | `AllJobsScreen.kt` | Category, pay type, distance |
| 7 | Category Chips | ✅ Done | `WorkerHomeScreen.kt` | Driver, Helper, Cook, etc. |
| 8 | Salary Filter | ✅ Done | `AllJobsScreen.kt` | Daily/Hourly/Monthly |
| 9 | Save/Bookmark Jobs | ✅ Done | `SavedJobsList.kt` | Shortlist jobs |
| 10 | Job Details View | ✅ Done | `JobDescriptionScreen.kt` | Full job information |
| 11 | Direct Call Button | ✅ Done | `JobDescriptionScreen.kt` | ACTION_DIAL intent |
| 12 | 1-Tap Application | ✅ Done | `JobDescriptionScreen.kt` | Auto-filled apply |
| 13 | Application Tracking | ✅ Done | `JobApplicationCard.kt` | Visual timeline |
| 14 | Work History | ✅ Done | `WorkerHistoryScreen.kt` | LinkedIn-style timeline |
| 15 | Digital Visiting Card | ✅ Done | `DigitalVisitingCardScreen.kt` | Shareable image card |
| 16 | Map View | ✅ Done | `JobMapScreen.kt` | Google Maps with job pins |
| 17 | Uber-style Map Routes | ✅ Done | `GoogleMapView.kt` | Actual road navigation routes |
| 18 | Smart Auto-Zoom | ✅ Done | `GoogleMapView.kt` | Zoom based on nearest job distance |
| 19 | Notifications | ✅ Done | `WorkerNotificationScreen.kt` | Push & in-app |
| 20 | Notification Settings | ✅ Done | `WorkerNotificationSettingsScreen.kt` | Per-channel control |

#### Employer Features (20 Features)

| # | Feature | Status | File(s) | Description |
|---|---------|--------|---------|-------------|
| 1 | Employer Profile | ✅ Done | `EmployerProfileScreen.kt` | Company profile |
| 2 | Company Details | ✅ Done | `EmployerCompanyDetailsScreen.kt` | Business information |
| 3 | Mandatory Profile Setup | ✅ Done | `MandatoryEmployerProfileSetupScreen.kt` | Required fields |

**Employer Profile Completion Weights:**
| Field | Weight | Required |
|-------|--------|----------|
| Company Name | 15% | ✅ Yes |
| Industry | 15% | ✅ Yes |
| Contact Phone | 15% | ✅ Yes |
| Business Address | 20% | ✅ Yes |
| Gender | 10% | ✅ Yes |
| Date of Birth | 10% | ✅ Yes |
| Contact Email | 5% | ❌ Optional |
| Company Size | 5% | ❌ Optional |
| Profile Picture | 5% | ❌ Optional |

> **Note:** Profile picture is optional (5% weight). Employers can post jobs with 85% completion (without optional fields). Minimum 80% required to post jobs.

| 4 | 4-Step Job Posting | ✅ Done | `PostJobScreen.kt` | Wizard-style posting |
| 5 | GPS Auto-fill Location | ✅ Done | `LocationService.kt` | Auto-detect location |
| 6 | Location Search | ✅ Done | `PostJobScreen.kt` | Geocoder-based search with suggestions |
| 7 | Structured Job Titles | ✅ Done | `PostJobScreen.kt` | Dropdown only (anti-fraud) |
| 8 | Location Consistency Check | ✅ Done | `PostJobScreen.kt` | 30km flag |
| 9 | Payment Amount Validation | ✅ Done | `PostJobScreen.kt` | Max ₹50,000 for hyper-local jobs |
| 10 | Vacancy Validation | ✅ Done | `PostJobFormComponents.kt` | Max 50 vacancies |
| 11 | Application Management | ✅ Done | `EmployerApplicationManagementScreen.kt` | View all applicants |
| 12 | View Worker Profiles | ✅ Done | `ProfessionalWorkerProfileViewScreen.kt` | Skills, experience, ratings |
| 13 | Accept/Reject Applications | ✅ Done | `ApplicationDetailScreen.kt` | With notifications |
| 14 | Edit Posted Jobs | ✅ Done | `EditJobScreen.kt` | Modify job details |
| 15 | Job History | ✅ Done | `EmployerHistoryScreen.kt` | Past jobs |
| 16 | Analytics Dashboard | ✅ Done | `AnalyticsScreen.kt` | Job performance |
| 17 | Trust Badges | ✅ Done | `TrustBadgesScreen.kt` | 3-tier system |
| 18 | Referral System | ✅ Done | `EmployerReferEarnScreen.kt` | Refer & earn |
| 19 | Work Start Verification | ✅ Done | `EmployerVerifyWorkScreen.kt` | QR/Code verification |
| 20 | Notifications | ✅ Done | `EmployerNotificationScreen.kt` | Push & in-app |

#### Trust & Verification (8 Features)

| # | Feature | Status | File(s) | Description |
|---|---------|--------|---------|-------------|
| 1 | Two-way Rating System | ✅ Done | `JobRatingBottomSheet.kt` | Workers & employers rate each other |
| 2 | Employer Trust Badges | ✅ Done | `TrustBadge.kt` | VERIFIED → TRUSTED → BUSINESS |
| 3 | GST Business Verification | ✅ Done | `EmployerTrustModels.kt` | Business tier unlock |
| 4 | Developer Mode Detection | ✅ Done | `DeveloperModeWarningSheet.kt` | Fake GPS prevention |
| 5 | Hide Applied Jobs | ✅ Done | `WorkerHomeScreen.kt` | Cleaner UX |
| 6 | Rating Categories | ✅ Done | `RatingModels.kt` | Punctuality, Quality, Payment, Behavior |
| 7 | Work Start QR Verification | ✅ Done | `WorkStartQRScreen.kt` | Worker shows QR for employer to scan |
| 8 | Job Expiry System | ✅ Done | `JobListing.kt`, `FirestoreService.kt` | Jobs expire after 15 days |

#### Monetization (1 Feature)

| # | Feature | Status | File(s) | Description |
|---|---------|--------|---------|-------------|
| 1 | AdMob Integration | ✅ Done | `AdManager.kt` | Banner & Interstitial ads for revenue |

**Note:** Subscription/Razorpay features have been removed. App now uses AdMob ads for monetization.

#### Policy & Compliance (6 Features)

| # | Feature | Status | File(s) | Description |
|---|---------|--------|---------|-------------|
| 1 | Privacy Policy | ✅ Done | `PrivacyPolicyScreen.kt` | GDPR compliant |
| 2 | Terms & Conditions | ✅ Done | `TermsAndConditionsScreen.kt` | Legal terms |
| 3 | Job Expiry Policy | ✅ Done | `TermsAndConditionsScreen.kt` | 15-day job expiry policy documented |
| 4 | Cancellation & Refund | ✅ Done | `CancellationRefundScreen.kt` | 7-day refund window |
| 5 | Contact Us | ✅ Done | `ContactUsScreen.kt` | Support contact |
| 6 | Security & Legal | ✅ Done | `SecurityLegalScreen.kt` | Combined legal info |

#### Help & Support (7 Features)

| # | Feature | Status | File(s) | Description |
|---|---------|--------|---------|-------------|
| 1 | Help Center | ✅ Done | `HelpMainScreen.kt` | Main help hub |
| 2 | FAQ | ✅ Done | `FaqScreen.kt` | Common questions |
| 3 | Chat Support | ✅ Done | `ChatSupportScreen.kt` | In-app chat |
| 4 | Call Support | ✅ Done | `CallSupportScreen.kt` | Phone support |
| 5 | Report Problem | ✅ Done | `ReportProblemScreen.kt` | Issue reporting |
| 6 | Tutorial | ✅ Done | `TutorialScreen.kt` | App guide |
| 7 | Feedback | ✅ Done | `FeedbackBottomSheet.kt` | User feedback |

#### UI/UX Features (10 Features)

| # | Feature | Status | File(s) | Description |
|---|---------|--------|---------|-------------|
| 1 | Shimmer Loading | ✅ Done | `ShimmerComponents.kt` | Skeleton loading |
| 2 | Pull-to-Refresh | ✅ Done | Multiple screens | Refresh data |
| 3 | Splash Screen | ✅ Done | `DutyPeSplashScreen.kt` | Animated splash with DutyPe text logo |
| 4 | Bottom Navigation | ✅ Done | `ReusableBottomBar.kt` | Tab navigation |
| 5 | Common Header | ✅ Done | `CommonHeader.kt` | Consistent headers |
| 6 | Scroll-aware UI | ✅ Done | `ScrollAwareLazyColumn.kt` | Hide/show on scroll |
| 7 | Lottie Animations | ✅ Done | Multiple screens | Engaging animations |
| 8 | Responsive Layout | ✅ Done | `ResponsiveLayout.kt` | Adaptive UI |
| 9 | Location Caching | ✅ Done | `WorkerHomeScreen.kt` | Stale-while-revalidate pattern |
| 10 | Role-specific Notifications | ✅ Done | `EmployerNotificationViewModel.kt` | Filter by user role |


---

### ❌ NOT IMPLEMENTED FEATURES (45+ Total)

#### 🔴 CRITICAL - Power Features (Anti-Fraud & Safety) - 13 Features

| # | Feature | Priority | Complexity | Description | Impact |
|---|---------|----------|------------|-------------|--------|
| 1 | **No-Data-Entry Firewall** | 🔴 HIGH | Low | Auto-block WFH/Online scam keywords | Eliminates 90% scams |
| 2 | **Video Job Description** | 🔴 HIGH | High | 15-second video required to post | Scammers can't fake. Trust +100% |
| 3 | **Map Radar (Uber-style)** | 🔴 HIGH | Medium | Pulsing animated dots on map | Makes discovery feel ALIVE |
| 4 | **Market Rate Suggestions** | 🔴 HIGH | Medium | App suggests local market rate | Fair wages, no exploitation |
| 5 | **The Rate Card** | 🔴 HIGH | Low | Standard rates for services | Protects users from overcharging |
| 6 | **Aadhaar Face Match** | 🔴 HIGH | High | Live selfie + blink detection + AI match | 100% real humans, zero bots |
| 7 | **Background Safe-Check** | 🔴 HIGH | Medium | Show verification for home-entry jobs | Users feel SAFE |
| 8 | **SOS Panic Button** | 🔴 HIGH | Medium | Floating emergency button during jobs | Safety for workers & employers |
| 9 | **Selfie with Shop Board** | 🟡 MEDIUM | Medium | Employer selfie in front of shop | Verifies shop exists |
| 10 | **No-Consultancy Filter** | 🟡 MEDIUM | Medium | Auto-block if 5+ categories posted | Block fake recruiters |
| 11 | **Local Reference (Community Vouch)** | 🟡 MEDIUM | Medium | "Who knows you in this colony?" | Digitizes Indian reference system |
| 12 | **Pay Rate Guardrails** | 🟡 MEDIUM | Low | Min/max validation per category | Blocks "₹50,000/day" scams |
| 13 | **Community Reporting** | 🟡 MEDIUM | Medium | 3 reports = auto-hide job | Crowd-sourced moderation |

#### 🔴 CRITICAL - User Requirements - 8 Features

| # | Feature | Priority | Complexity | Description |
|---|---------|----------|------------|-------------|
| 14 | **Language Selector (Telugu/English)** | 🔴 HIGH | Medium | App restart in chosen language |
| 15 | **WhatsApp Apply** | 🔴 HIGH | Low | One-click with pre-filled message |
| 16 | **Admin Panel** | 🔴 HIGH | High | Delete spam/fake jobs |
| 17 | **Audio-First Interface (TTS)** | 🔴 HIGH | Medium | Speaker icon for job titles |
| 18 | **Geo-Fencing UI (3KM filter)** | 🟡 MEDIUM | Low | Radius filter toggle |
| 19 | **QR Code in Referral** | 🟢 LOW | Low | Shareable QR code image |
| 20 | **Job Poster Generator (PDF)** | 🟢 LOW | Medium | Print poster for shop window |
| 21 | **Government Verified Badge** | 🟢 LOW | Low | Green tick for Aadhaar-verified |

#### 🟡 OPERATIONS Features - 9 Features

| # | Feature | Priority | Complexity | Description |
|---|---------|----------|------------|-------------|
| 22 | **Real-time Chat** | 🔴 HIGH | High | Firebase messaging backend |
| 23 | **Trust Score Algorithm** | 🟡 MEDIUM | Medium | Behavior-based scoring (0-100) |
| 24 | **Job Check-in (GPS)** | 🟡 MEDIUM | Medium | Worker checks in within 100m |
| 25 | **Job Check-out** | 🟡 MEDIUM | Medium | Duration calculation, trigger payment |
| 26 | **Promise Token** | 🟢 LOW | Low | Digital job card when accepted |
| 27 | **Strike System** | 🟡 MEDIUM | Medium | 3 no-shows = 7-day suspension |
| 28 | **Reliability Score** | 🟢 LOW | Low | "Did they show up?" percentage |
| 29 | **Voice Chat** | 🟢 LOW | High | Audio messages in chat |
| 30 | **Location Sharing in Chat** | 🟢 LOW | Medium | Send location button |

#### 🟢 FINTECH Features - 6 Features

| # | Feature | Priority | Complexity | Description |
|---|---------|----------|------------|-------------|
| 31 | **Payment Status Tracking** | 🟡 MEDIUM | Medium | Paid/Unpaid/Partial status |
| 32 | **Non-payment Reporting** | 🟡 MEDIUM | Medium | Worker reports non-payment |
| 33 | **Escrow/Trust Pay** | 🟢 LOW | High | Employer deposits before shift |
| 34 | **In-app Wallet** | 🟢 LOW | High | Worker wallet with withdrawals |
| 35 | **Urgent Hiring Fee** | 🟢 LOW | Medium | ₹999 to blast to 500 workers |
| 36 | **AI Support Bot** | 🟢 LOW | High | Context-aware help |

#### 🟢 AI Features - 4 Features

| # | Feature | Priority | Complexity | Description |
|---|---------|----------|------------|-------------|
| 37 | **AI Scam Detection** | 🟡 MEDIUM | High | Auto-flag suspicious jobs |
| 38 | **AI Job Rewriting** | 🟢 LOW | Medium | Improve job descriptions |
| 39 | **AI Feedback Writing** | 🟢 LOW | Medium | Help write reviews |
| 40 | **Risk Classification** | 🟢 LOW | Medium | Score job risk level |

#### 🟢 Additional Features - 5 Features

| # | Feature | Priority | Complexity | Description |
|---|---------|----------|------------|-------------|
| 41 | **Skill Verification & Badges** | 🟢 LOW | Medium | Verify worker skills |
| 42 | **Repeat Hiring & Favorites** | 🟢 LOW | Low | Employer favorites workers |
| 43 | **Job Expiry Auto-close** | 🟢 LOW | Low | Auto-close after X days |
| 44 | **Bulk Job Posting** | 🟢 LOW | Medium | Post multiple jobs at once |
| 45 | **Worker Availability Status** | 🟢 LOW | Low | "Available Now" toggle |

---

## 🛡️ ZERO FAKE POLICY - Operational Strategy

### How to Ensure 100% Genuine Jobs from Day 1

#### Rule 1: No-Data-Entry Firewall ❌ NOT IMPLEMENTED
```
BANNED KEYWORDS:
- Online, SMS, Investment, Part-time WFH
- Laptop required, PDF conversion
- Work from home, Data entry
- Typing job, Form filling
- Earn from home, No experience needed

IF job contains banned keywords:
    → Block immediately
    → Show: "DutyPe is for PHYSICAL location jobs only."
```

#### Rule 2: Selfie Verification ❌ NOT IMPLEMENTED
```
When shop owner registers:
→ Must upload selfie standing in front of shop board
→ AI verifies shop name matches registration
→ Grants "Verified Shop" badge
```

#### Rule 3: No-Consultancy Filter ❌ NOT IMPLEMENTED
```
Algorithm Rule:
IF user posts jobs in 5+ different categories
   (e.g., Driver, Nurse, Java Dev, Data Entry)
THEN AUTO-BLOCK account

Why: Real shop owners only hire for THEIR shop
```

#### Rule 4: Video Job Description ❌ NOT IMPLEMENTED
```
To post a job:
→ Employer records 15-second video
→ Shows face + shop + explains job
→ Scammers in dark rooms CANNOT do this
```

#### Rule 5: Location Consistency ✅ IMPLEMENTED
```
When posting job:
→ Compare employer's GPS with job location
→ If > 30km apart, show warning flag
→ "This employer is posting from far away"
```

#### Rule 6: Structured Job Titles ✅ IMPLEMENTED
```
Job titles are DROPDOWN ONLY:
→ No free text like "Data Entry ₹50k"
→ Only: Cook, Driver, Helper, Maid, etc.
→ Prevents scam job titles
```

#### Rule 7: Aadhaar Face Match ❌ NOT IMPLEMENTED
```
For "Verified" badge:
→ Upload Aadhaar card
→ Take live selfie (blink detection)
→ AI matches face on card vs selfie
→ If match > 85%: Verified badge
```

#### Rule 8: Rate Card Display ❌ NOT IMPLEMENTED
```
Before calling worker:
→ Show DutyPe Standard Rate Card
→ "AC Service: ₹500"
→ Protects users from overcharging
```


---

## 📂 JOB CATEGORIES SUPPORTED

| Category | Icon | Examples | Trust Level | Suggested Rate |
|----------|------|----------|-------------|----------------|
| Cook | 👨‍🍳 | Home cook, Restaurant chef, Tea master | HIGH | ₹8,000-18,000/month |
| Maid/Cleaner | 🧹 | House cleaning, Office cleaning | HIGH | ₹2,000-5,000/month |
| Driver | 🚗 | Personal driver, Delivery driver | HIGH | ₹12,000-20,000/month |
| Helper | 🤝 | Shop helper, Kirana helper, Saree folding | HIGH | ₹6,000-12,000/month |
| Security | 🛡️ | Security guard, Watchman | HIGH | ₹8,000-15,000/month |
| Gardener | 🌱 | Garden maintenance | HIGH | ₹5,000-10,000/month |
| Caretaker | 👥 | Elderly care, Child care | HIGH | ₹10,000-20,000/month |
| Delivery | 📦 | Food delivery, Package delivery | HIGH | ₹300-500/day |
| Waiter/Server | 🍽️ | Restaurant staff, Event staff | HIGH | ₹400-600/day |
| Electrician | ⚡ | Electrical work | MEDIUM | ₹200-400/hour |
| Plumber | 🔧 | Plumbing work | MEDIUM | ₹250-450/hour |
| Painter | 🎨 | House painting | MEDIUM | ₹400-600/day |
| Carpenter | 🪚 | Woodwork | MEDIUM | ₹300-500/hour |
| Receptionist | 💼 | Front desk | MEDIUM | ₹10,000-18,000/month |
| Cashier | 💵 | Billing, Cash handling | MEDIUM | ₹8,000-15,000/month |
| Packer | 📦 | Warehouse packing | HIGH | ₹300-500/day |
| Construction | 🏗️ | Cement mixing, Loading | HIGH | ₹400-600/day |
| Other | 📋 | Miscellaneous | LOW | Varies |

### Micro-Gig Categories (High Demand)

| Category | Example | Pay Model | Suggested Rate |
|----------|---------|-----------|----------------|
| Event Staff | "5 boys to serve food at wedding tonight" | Per event | ₹500-800/event |
| Loading/Unloading | "2 people to shift furniture" | Per task | ₹200-400/task |
| Construction Helper | "3 helpers for cement mixing" | Daily wage | ₹400-600/day |

### Service Rate Card (Anti-Looting)

| Service | Standard Rate | Parts Extra |
|---------|---------------|-------------|
| Fan Capacitor Change | ₹150 | + Parts |
| AC Service (Split) | ₹500 | - |
| AC Service (Window) | ₹400 | - |
| Geyser Repair | ₹300 | + Parts |
| Washing Machine Service | ₹400 | + Parts |
| RO Service | ₹350 | + Parts |
| Electrician (per hour) | ₹200 | + Parts |
| Plumber (per hour) | ₹250 | + Parts |
| Carpenter (per hour) | ₹300 | + Parts |
| Painter (per day) | ₹500 | + Materials |

---

## 💰 SUBSCRIPTION PLANS

| Plan | Price | Job Posts | Contact Views | Features |
|------|-------|-----------|---------------|----------|
| **Free** | ₹0 | 1/month | 3 free | Basic features |
| **Basic** | ₹199/month | 5/month | ₹29 each | Priority support |
| **Pro** | ₹499/month | Unlimited | Unlimited | Featured listings, Analytics, Verified badge |
| **Enterprise** | ₹999/month | Unlimited | Unlimited | All Pro + Dedicated support, Custom branding |

### Postpaid Model (Anti-Scam)
- **Posting is FREE** for everyone
- **Pay only to unlock** candidate phone numbers
- Scammers won't pay ₹29-50 per contact
- Genuine employers happily pay for real workers

---

## 🔧 TECHNOLOGY STACK

| Layer | Technology | Version |
|-------|------------|---------|
| Language | Kotlin | 2.0.21 |
| UI Framework | Jetpack Compose | BOM 2024.09.00 |
| Architecture | MVVM + Repository | - |
| DI Framework | Hilt | 2.52 |
| Backend | Firebase | Multiple |
| Database | Cloud Firestore | - |
| Auth | Firebase Auth | - |
| Push Notifications | FCM | - |
| Crash Reporting | Crashlytics | - |
| Image Loading | Coil | 2.4.0 |
| Navigation | Navigation Compose | 2.8.9 |
| Maps | Google Maps SDK | 18.2.0 |
| Payments | Razorpay | 1.6.40 |
| Animations | Lottie | - |
| Logging | Timber | - |

### Dependencies Needed for Future Features

```kotlin
// Voice Chat (Phase 2)
implementation("com.google.android.exoplayer:exoplayer:2.19.1")

// Aadhaar Face Match (Phase 3)
implementation("com.google.mlkit:text-recognition:16.0.0")
implementation("com.google.mlkit:face-detection:16.1.5")
// OR HyperVerge SDK for production

// AI Features (Phase 3)
implementation("com.azure:azure-ai-openai:1.0.0-beta.5")

// QR Code Generation
implementation("com.google.zxing:core:3.5.2")

// PDF Generation (built-in Android)
// android.graphics.pdf.PdfDocument
```

---

## 📁 PROJECT STRUCTURE

```
app/src/main/java/com/example/dutype/
├── ads/                    # Ad management (AdMob)
├── auth/                   # Authentication (Google, OTP)
├── common/                 # Shared screens
│   └── chat/
│       ├── chat/           # Chat screens
│       ├── help/           # Help & support screens
│       └── info/           # Policy screens
├── components/             # Reusable UI components
├── data/                   # Data stores
├── di/                     # Dependency Injection (Hilt)
├── employer/               # Employer module
│   ├── components/
│   ├── helpers/
│   ├── models/
│   ├── screens/
│   └── viewmodels/
├── location/               # Location services
├── models/                 # Data models
├── navigation/             # Navigation graphs
├── notifications/          # Notification system
├── onboarding/             # Onboarding screens
├── repositories/           # Data repositories
├── services/               # Business logic services
├── state/                  # State managers
├── ui/                     # Theme & components
├── utils/                  # Utilities
├── viewmodels/             # Shared ViewModels
├── worker/                 # Worker module
│   ├── components/
│   ├── helpers/
│   ├── models/
│   ├── screens/
│   └── viewmodels/
├── MainActivity.kt
└── DutyPeApplication.kt
```

**Total Files:** 108+ Kotlin files

---

## 🗄️ FIRESTORE COLLECTIONS

| Collection | Purpose | Key Fields |
|------------|---------|------------|
| `users` | User accounts | id, email, phone, name, role |
| `worker_profiles` | Worker data | skills, experience, ratings, aadhaarVerified |
| `employer_profiles` | Employer data | companyName, gstNumber, trustTier, shopSelfie |
| `jobs` | Job listings | title, payAmount, location, lat/lng, videoUrl |
| `job_applications` | Applications | jobId, workerId, status |
| `saved_jobs` | Bookmarked jobs | userId, jobId |
| `notifications` | User notifications | title, message, type |
| `phone_roles` | Phone-to-role mapping | phoneNumber, role |
| `fcm_tokens` | Push tokens | userId, token |
| `ratings` | User ratings | raterId, ratedId, score, categories |
| `rating_summaries` | Rating aggregates | averageRating, totalRatings |
| `subscriptions` | User subscriptions | planType, status |
| `payment_transactions` | Payment history | amount, status |
| `job_reports` | Reported jobs | jobId, reporterId, reason |
| `local_references` | Community vouches | workerId, referenceId |
| `aadhaar_verifications` | Aadhaar data | userId, aadhaarNumber, faceMatchScore |
| `rate_cards` | Standard rates | category, city, minRate, maxRate |
| `sos_alerts` | Emergency alerts | userId, jobId, location, timestamp |

---

## 📊 APP STATISTICS

| Metric | Value |
|--------|-------|
| Total Kotlin Files | 110+ |
| Total Screens | 48+ |
| Implemented Features | 60+ |
| Missing Features | 40+ |
| Architecture | 100% MVVM |
| UI Framework | 100% Jetpack Compose |
| Job Categories | 17+ |


---

## 🎬 DEEP DIVE SCENARIOS

### Scenario 1: Retail Helper (Saree Shop)
**User Story:** Shop owner needs someone to fold sarees and assist customers

| Step | Action | DutyPe Feature |
|------|--------|----------------|
| 1 | Owner posts job with 15-sec video | Video Job Description ❌ |
| 2 | Shows shop board in video | Selfie with Shop Board ❌ |
| 3 | Worker sees job 400m away | Map Radar ❌ |
| 4 | Worker applies with 1-tap | ✅ IMPLEMENTED |
| 5 | Owner unlocks contact (₹29) | Postpaid Model ✅ |
| 6 | Worker rates owner after job | Worker-Centric Rating ✅ |

---

### Scenario 2: Event Staff (Wedding)
**User Story:** Caterer needs 5 boys to serve food at wedding tonight

| Step | Action | DutyPe Feature |
|------|--------|----------------|
| 1 | Urgent job posted (₹500/person) | Micro-Gig Category ✅ |
| 2 | Blast to 500 workers nearby | Urgent Hiring Fee ❌ |
| 3 | Workers see pulsing dots on map | Map Radar ❌ |
| 4 | 5 workers apply within 30 mins | 1-Tap Apply ✅ |
| 5 | Caterer accepts all 5 | Application Management ✅ |
| 6 | Workers check-in at venue | Job Check-in (GPS) ❌ |
| 7 | Payment released after event | Escrow/Trust Pay ❌ |

---

### Scenario 3: Domestic Help (Maid)
**User Story:** Family needs part-time maid for 2 hours daily

| Step | Action | DutyPe Feature |
|------|--------|----------------|
| 1 | Family posts job with location | GPS Auto-fill ✅ |
| 2 | App suggests ₹3,000/month rate | Market Rate Suggestions ❌ |
| 3 | Maid sees job in her colony | Location-based Matching ✅ |
| 4 | Family sees maid's verification | Background Safe-Check ❌ |
| 5 | Family sees "12 jobs in Ameerpet" | Local Job History ❌ |
| 6 | Maid has "Verified Local" badge | Local Reference ❌ |
| 7 | Both rate each other after 1 week | Two-way Rating ✅ |

---

### Scenario 4: Elderly Care Assistant 👴 ❌ NOT IMPLEMENTED
**User Story:** Family needs caretaker for elderly parent with medicine schedule

| Feature | Description | Status |
|---------|-------------|--------|
| **Medicine Timeline** | Photo proof of medicine given at scheduled times | ❌ NOT IMPLEMENTED |
| **Geofence Alert** | 50m radius alert if caretaker leaves premises | ❌ NOT IMPLEMENTED |
| **Mood Check-In** | 5-second audio recording from patient daily | ❌ NOT IMPLEMENTED |
| **Emergency SOS** | Panic button for medical emergencies | ❌ NOT IMPLEMENTED |
| **Daily Report** | Auto-generated summary sent to family | ❌ NOT IMPLEMENTED |

**Flow:**
```
1. Family posts "Elderly Care" job with special requirements
2. Caretaker accepts and starts job
3. App shows medicine schedule with reminders
4. Caretaker takes photo when giving medicine → Sent to family
5. If caretaker leaves 50m radius → Family gets alert
6. Daily: App prompts 5-sec audio from patient ("How are you feeling?")
7. Family receives daily report with all check-ins
```

**Impact:** Peace of mind for families with elderly parents. Trust +500%.

---

### Scenario 5: Temp Driver 🚗 ❌ NOT IMPLEMENTED
**User Story:** Family needs driver for outstation trip or daily commute

| Feature | Description | Status |
|---------|-------------|--------|
| **Behavioral Telematics** | Speed/braking monitoring during trips | ❌ NOT IMPLEMENTED |
| **License OCR & E-Challan Check** | Verify license validity + check pending challans | ❌ NOT IMPLEMENTED |
| **Video Testimonial Portfolio** | Past employers record 15-sec video reviews | ❌ NOT IMPLEMENTED |
| **Trip Tracking** | Real-time location sharing with family | ❌ NOT IMPLEMENTED |
| **Fuel Log** | Track fuel expenses with photo receipts | ❌ NOT IMPLEMENTED |

**Flow:**
```
1. Driver uploads driving license
2. App OCR extracts license number
3. API checks for pending e-challans (Parivahan API)
4. If clean record → "Safe Driver" badge
5. During trip: App monitors speed/harsh braking
6. Family can track trip in real-time
7. Past employers can record video testimonials
```

**Impact:** Safe drivers for families. No drunk/rash drivers. Trust +300%.

---

### Scenario 6: Cash-Handling Helper 💰 ❌ NOT IMPLEMENTED
**User Story:** Shop needs helper who handles cash register

| Feature | Description | Status |
|---------|-------------|--------|
| **Digital Collateral (Trust Bond)** | ₹500 locked in wallet as security deposit | ❌ NOT IMPLEMENTED |
| **Aadhaar-Linked Legal Consent** | Digital agreement with Aadhaar e-sign | ❌ NOT IMPLEMENTED |
| **Cash Handling Badge** | Special verification for cash jobs | ❌ NOT IMPLEMENTED |
| **Employer Insurance** | Optional insurance against theft | ❌ NOT IMPLEMENTED |
| **Daily Cash Report** | Worker logs cash handled daily | ❌ NOT IMPLEMENTED |

**Flow:**
```
1. Worker applies for "Cashier" job
2. App requires ₹500 Trust Bond (locked in wallet)
3. Worker signs Aadhaar-linked legal consent
4. If theft occurs → Trust Bond forfeited + legal action
5. If job completes successfully → Trust Bond returned + bonus
6. Worker earns "Trusted Cashier" badge
```

**Impact:** Employers feel safe hiring for cash-handling roles. Workers build trust.

---

## 🛡️ DUTYPE TRUST SHIELD (Infrastructure)

### 1. SafePay (Escrow System) 💳 ❌ NOT IMPLEMENTED
**Employer deposits before job starts, released on completion**

| Step | Action | Who |
|------|--------|-----|
| 1 | Employer posts job with salary ₹500/day | Employer |
| 2 | Employer deposits ₹500 to DutyPe Escrow | Employer |
| 3 | Worker sees "💰 Payment Secured" badge on job | Worker |
| 4 | Worker completes job | Worker |
| 5 | Employer confirms completion | Employer |
| 6 | ₹500 released to worker's wallet | System |
| 7 | If dispute → DutyPe mediates | Support |

**Benefits:**
- Workers guaranteed payment (no "come tomorrow" excuses)
- Employers can't cheat workers
- DutyPe earns 2% transaction fee
- Builds massive trust in platform

**Implementation:**
```kotlin
data class EscrowTransaction(
    val transactionId: String,
    val jobId: String,
    val employerId: String,
    val workerId: String,
    val amount: Double,
    val status: EscrowStatus, // DEPOSITED, RELEASED, DISPUTED, REFUNDED
    val depositedAt: Timestamp,
    val releasedAt: Timestamp?
)
```

---

### 2. SOS Panic Widget 🆘 ❌ NOT IMPLEMENTED
**Red button on lock screen, sends live location + audio**

| Feature | Description |
|---------|-------------|
| **Lock Screen Widget** | Red SOS button accessible without unlocking phone |
| **One-Tap Activation** | Press and hold for 3 seconds to activate |
| **Live Location** | Sends GPS coordinates to emergency contacts |
| **Audio Recording** | Records 30-second audio clip automatically |
| **Auto-Call** | Option to auto-call 100 (Police) or saved contact |
| **Job Context** | Includes current job details in alert |

**Flow:**
```
1. Worker starts job → SOS widget appears on lock screen
2. Emergency situation → Worker presses SOS for 3 seconds
3. App sends:
   - Live GPS location
   - 30-second audio recording
   - Current job details (employer name, location)
   - Worker's Aadhaar-verified photo
4. Alert sent to:
   - Worker's emergency contacts
   - DutyPe Safety Team
   - Optionally: Local police (100)
5. DutyPe Safety Team follows up within 5 minutes
```

**Implementation:**
```kotlin
data class SOSAlert(
    val alertId: String,
    val workerId: String,
    val jobId: String?,
    val location: GeoPoint,
    val audioUrl: String?,
    val timestamp: Timestamp,
    val status: SOSStatus, // TRIGGERED, ACKNOWLEDGED, RESOLVED
    val emergencyContacts: List<String>,
    val employerDetails: EmployerInfo?
)
```

**Impact:** Workers feel SAFE. Parents feel safe sending daughters to work. Trust +1000%.

---

### 3. Double-Blind Reviews ⭐ ❌ NOT IMPLEMENTED
**Both ratings hidden until both parties submit**

| Problem | Solution |
|---------|----------|
| Workers afraid to give honest ratings (fear of retaliation) | Ratings hidden until both submit |
| Employers give low ratings to avoid paying | Can't see worker's rating first |
| Biased ratings based on other's rating | True independent feedback |

**Flow:**
```
1. Job completes
2. Both worker and employer get rating prompt
3. Worker submits rating → Stored but HIDDEN
4. Employer submits rating → Stored but HIDDEN
5. Once BOTH submit → Both ratings revealed simultaneously
6. If one party doesn't rate within 7 days → Other rating published
```

**Benefits:**
- Honest feedback from both sides
- No retaliation fear
- True trust scores
- Better community health

**Implementation:**
```kotlin
data class BlindRating(
    val ratingId: String,
    val jobId: String,
    val workerRating: Rating?, // Hidden until both submit
    val employerRating: Rating?, // Hidden until both submit
    val workerSubmittedAt: Timestamp?,
    val employerSubmittedAt: Timestamp?,
    val revealedAt: Timestamp?, // When both ratings become visible
    val status: BlindRatingStatus // PENDING_BOTH, PENDING_WORKER, PENDING_EMPLOYER, REVEALED
)
```

---

### 4. Trust Score Algorithm 📊 ❌ NOT IMPLEMENTED
**Behavior-based scoring (0-100)**

| Factor | Weight | Description |
|--------|--------|-------------|
| **Ratings Average** | 30% | Average of all ratings received |
| **Completion Rate** | 25% | Jobs completed vs accepted |
| **Response Time** | 15% | How fast they respond to messages |
| **Verification Level** | 15% | Aadhaar, Phone, Local Reference |
| **Tenure** | 10% | How long on platform |
| **Reports Against** | 5% | Negative reports (deduction) |

**Score Tiers:**
| Score | Badge | Benefits |
|-------|-------|----------|
| 90-100 | 🏆 Elite | Featured in search, priority support |
| 75-89 | ⭐ Trusted | Verified badge, higher visibility |
| 50-74 | ✅ Good | Standard features |
| 25-49 | ⚠️ New | Limited features, needs verification |
| 0-24 | 🚫 Restricted | Account review, limited access |

---

### 5. Community Shield 🛡️ ❌ NOT IMPLEMENTED
**Crowd-sourced moderation**

| Feature | Description |
|---------|-------------|
| **Report Button** | Report suspicious jobs/users |
| **3-Strike Rule** | 3 reports = auto-hide for review |
| **Community Moderators** | Trusted users can flag content |
| **Reward System** | Earn points for accurate reports |
| **Appeal Process** | Wrongly flagged users can appeal |

**Report Categories:**
- Fake job / Scam
- Inappropriate content
- Wrong location
- Unrealistic salary
- Harassment
- Non-payment
- Other

---

### 6. Standby Bench (No-Show Killer) 🪑 ❌ NOT IMPLEMENTED
**Auto-replace no-show workers with standby workers**

| Problem | Solution |
|---------|----------|
| Caterer needs 10 waiters, fears only 6 will come | Book 10 "Active" + 3 "Standby" workers |
| No-shows ruin events and business | Auto-fire no-shows, auto-hire standby |
| Employers have no backup plan | Built-in backup system |

**Scenario:**
```
Caterer needs 10 waiters for wedding at 6 PM.
He fears only 6 will actually show up.

Solution:
- Books 10 "Active" workers
- Books 3 "Standby" workers (backup)
- Standby workers are on alert, ready to step in
```

**Flow:**
```
1. Employer posts job for 10 workers + 3 standby
2. 10 Active workers accept → Status: "CONFIRMED"
3. 3 Standby workers accept → Status: "STANDBY"
4. Job day: Active workers must turn ON "On the Way" GPS toggle by 5 PM
5. If Active worker doesn't toggle by deadline:
   → Auto-Fire: Status changes to "NO_SHOW"
   → Auto-Hire: First Standby worker promoted to "ACTIVE"
   → Notification sent to both parties
6. Standby worker gets: "You're now ACTIVE! Head to venue now."
7. No-show worker gets: "You missed the deadline. Strike +1"
```

**Implementation:**
```kotlin
data class JobBooking(
    val bookingId: String,
    val jobId: String,
    val workerId: String,
    val status: BookingStatus, // CONFIRMED, STANDBY, ON_THE_WAY, NO_SHOW, COMPLETED
    val isStandby: Boolean,
    val standbyPriority: Int, // 1 = first backup, 2 = second backup
    val onTheWayDeadline: Timestamp, // e.g., 5 PM
    val onTheWayToggledAt: Timestamp?,
    val autoPromotedAt: Timestamp? // When standby was promoted to active
)

// Auto-fire logic (Cloud Function)
fun checkNoShows(jobId: String) {
    val deadline = job.onTheWayDeadline
    val activeWorkers = getActiveWorkers(jobId)
    
    activeWorkers.forEach { worker ->
        if (worker.onTheWayToggledAt == null && now > deadline) {
            // Auto-fire
            worker.status = "NO_SHOW"
            worker.strikes += 1
            
            // Auto-hire standby
            val standby = getFirstStandby(jobId)
            if (standby != null) {
                standby.status = "CONFIRMED"
                standby.isStandby = false
                sendNotification(standby, "You're now ACTIVE!")
            }
        }
    }
}
```

**Benefits:**
- 100% attendance guarantee
- Employers have peace of mind
- Standby workers get opportunity
- No-show workers get penalized (strike system)
- Events/businesses never ruined by no-shows

**Impact:** Solves the #1 problem in blue-collar hiring. Trust +500%.

---

### 7. Work Start Verification (QR/Code) 🔐 ✅ IMPLEMENTED
**QR code or unique code verification when starting work**

| Problem | Solution |
|---------|----------|
| Worker claims they started work but didn't | QR/Code verification required |
| Employer can't verify worker actually arrived | Worker shows QR, employer scans |
| Disputes about work start time | Timestamped verification |

**How It Works:**

**Option A: QR Code Scan**
```
1. Employer accepts worker's application
2. On job day, worker opens app → Shows QR Code
3. Employer scans QR code with their app
4. Both apps show "✅ Work Started" with timestamp
5. Job status changes to "IN_PROGRESS"
```

**Option B: Unique Verification Code**
```
1. Employer accepts worker's application
2. On job day, worker opens app → Shows 6-character code (e.g., "DTP-7X9K")
3. Employer enters code in their app
4. Both apps show "✅ Work Started" with timestamp
5. Job status changes to "IN_PROGRESS"
```

**Implementation Files:**
- `WorkVerificationModels.kt` - Data models for verification
- `WorkVerificationService.kt` - Firebase service for verification
- `WorkStartQRScreen.kt` - Worker's QR code display screen
- `EmployerVerifyWorkScreen.kt` - Employer's QR scanner/code entry screen

**Flow:**
```
┌─────────────────────────────────────────────────────────────┐
│                    WORK START VERIFICATION                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  WORKER APP                        EMPLOYER APP              │
│  ──────────                        ────────────              │
│                                                              │
│  ┌─────────────────┐              ┌─────────────────┐       │
│  │   📱 QR CODE    │              │  📷 SCAN QR     │       │
│  │                 │     OR       │                 │       │
│  │  [QR IMAGE]     │  ────────►   │  [CAMERA VIEW]  │       │
│  │                 │              │                 │       │
│  │  Code: DTP-7X9K │              │  Enter Code:    │       │
│  └─────────────────┘              │  [_ _ _ - _ _ _]│       │
│                                   └─────────────────┘       │
│                                                              │
│  After verification:                                         │
│  ┌─────────────────────────────────────────────────┐        │
│  │  ✅ WORK STARTED                                 │        │
│  │  Time: 9:00 AM, Dec 29, 2025                    │        │
│  │  Worker: Ramesh Kumar                            │        │
│  │  Job: Waiter at Wedding                          │        │
│  │  Location: Verified ✓                            │        │
│  └─────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

**Implementation:**
```kotlin
data class WorkVerification(
    val verificationId: String,
    val jobId: String,
    val workerId: String,
    val employerId: String,
    val verificationCode: String, // e.g., "DTP-7X9K"
    val qrCodeData: String, // Encoded job + worker info
    val status: VerificationStatus, // PENDING, VERIFIED, EXPIRED
    val generatedAt: Timestamp,
    val verifiedAt: Timestamp?,
    val verifiedLocation: GeoPoint?, // GPS at verification time
    val expiresAt: Timestamp // Code expires after 2 hours
)

// Generate unique verification code
fun generateVerificationCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // No confusing chars (0,O,1,I)
    val prefix = "DTP"
    val code = (1..4).map { chars.random() }.joinToString("")
    return "$prefix-$code" // e.g., "DTP-7X9K"
}

// QR Code contains
data class QRCodePayload(
    val jobId: String,
    val workerId: String,
    val verificationCode: String,
    val timestamp: Long,
    val signature: String // HMAC signature for security
)
```

**Security Features:**
- Code expires after 2 hours
- GPS location captured at verification
- HMAC signature prevents tampering
- One-time use (can't reuse code)
- Both parties get notification

**Benefits:**
- Proof of work start
- No disputes about arrival time
- GPS verification of location
- Professional work tracking
- Builds trust between parties

**Impact:** Eliminates "he said, she said" disputes. Trust +300%.

---

## 📅 PRODUCT ROADMAP

### Phase 1: Foundation ✅ COMPLETE (Current)
- Authentication (Google, OTP)
- Worker & Employer Profiles
- Job Posting & Discovery
- Location-based Matching
- Application Management
- Push Notifications
- Two-way Rating System
- Razorpay Payments
- Subscription Plans
- Trust Badges

### Phase 2: Anti-Fraud & Safety (Month 2) 🔴 PRIORITY
- [ ] No-Data-Entry Firewall (keyword ban)
- [ ] Map Radar (Uber-style pulsing dots)
- [ ] Market Rate Suggestions
- [ ] The Rate Card (standard rates)
- [ ] WhatsApp Apply
- [ ] Language Selector (Telugu/English)
- [ ] Audio-First Interface (TTS)
- [ ] Pay Rate Guardrails
- [ ] Community Reporting
- [ ] No-Consultancy Filter

### Phase 3: Trust & Verification (Month 3)
- [ ] Video Job Description
- [ ] Selfie with Shop Board
- [ ] Aadhaar Face Match (HyperVerge/DigiLocker)
- [ ] Background Safe-Check
- [ ] SOS Panic Button
- [ ] Local Reference (Community Vouch)
- [ ] Real-time Chat
- [ ] Admin Panel

### Phase 4: Operations (Month 4)
- [ ] Job Check-in/Check-out
- [ ] Promise Token
- [ ] Strike System
- [ ] Reliability Score
- [ ] Trust Score Algorithm

### Phase 5: Fintech (Month 5+)
- [ ] Escrow/Trust Pay
- [ ] In-app Wallet
- [ ] Payment Status Tracking
- [ ] AI Support Bot
- [ ] AI Scam Detection

---

## 🔐 SECURITY FEATURES

| Feature | Status | Description |
|---------|--------|-------------|
| Firebase Auth | ✅ | Secure authentication |
| Phone OTP | ✅ | SMS verification |
| Developer Mode Detection | ✅ | Fake GPS prevention |
| Device Fingerprinting | ✅ | Fraud prevention |
| Dual-role Prevention | ✅ | Same phone can't be both roles |
| Age Validation | ✅ | 18-70 years only |
| Location Consistency | ✅ | 30km flag for suspicious postings |
| Structured Inputs | ✅ | Prevent scam job titles |
| No-Data-Entry Firewall | ❌ | Auto-block WFH/Online scams |
| Video Verification | ❌ | 15-second video for trust |
| Selfie with Shop | ❌ | Verify shop exists |
| No-Consultancy Filter | ❌ | Block bulk fake posters |
| Aadhaar Face Match | ❌ | Live selfie vs card photo |
| SOS Panic Button | ❌ | Emergency during active jobs |
| Background Safe-Check | ❌ | Verification for home-entry jobs |

---

## 📈 COMPLETION SUMMARY

| Category | Implemented | Total | % |
|----------|-------------|-------|---|
| Authentication | 8 | 8 | 100% ✅ |
| Worker Features | 18 | 25 | 72% 🟡 |
| Employer Features | 16 | 22 | 73% 🟡 |
| Power Features (Anti-Fraud) | 2 | 13 | 15% ❌ |
| Trust & Verification | 6 | 14 | 43% 🟡 |
| Payments | 5 | 11 | 45% 🟡 |
| Policy & Compliance | 5 | 5 | 100% ✅ |
| Help & Support | 7 | 7 | 100% ✅ |
| UI/UX | 8 | 10 | 80% 🟡 |
| Operations | 0 | 9 | 0% ❌ |
| AI Features | 0 | 4 | 0% ❌ |
| Deep Dive Scenarios | 0 | 15 | 0% ❌ |
| Trust Shield Infrastructure | 0 | 7 | 0% ❌ |
| **TOTAL** | **75** | **150** | **50%** |

---

## 🎯 IMMEDIATE PRIORITIES

### This Week (Quick Wins - 15 hours)
| # | Feature | Time | Impact |
|---|---------|------|--------|
| 1 | **No-Data-Entry Firewall** | 2 hrs | CRITICAL - Block 90% scams |
| 2 | **WhatsApp Apply** | 1-2 hrs | HIGH - Primary communication |
| 3 | **Pay Rate Guardrails** | 2 hrs | HIGH - Block scam rates |
| 4 | **Market Rate Suggestions** | 3 hrs | HIGH - Fair wages |
| 5 | **The Rate Card** | 2 hrs | HIGH - Anti-looting |
| 6 | **Geo-Fencing UI (3KM)** | 2 hrs | MEDIUM - Better UX |

### Next Week (Core Features - 20 hours)
| # | Feature | Time | Impact |
|---|---------|------|--------|
| 7 | **Map Radar (Uber-style)** | 4 hrs | HIGH - Visual engagement |
| 8 | **Language Selector** | 4 hrs | HIGH - Telugu users |
| 9 | **Audio-First (TTS)** | 3 hrs | HIGH - Semi-literate users |
| 10 | **Community Reporting** | 4 hrs | HIGH - Crowd moderation |
| 11 | **No-Consultancy Filter** | 3 hrs | HIGH - Block fake recruiters |

### Month 2 (Power Features - 60+ hours)
| # | Feature | Time | Impact |
|---|---------|------|--------|
| 12 | **Video Job Description** | 8 hrs | CRITICAL - Trust +100% |
| 13 | **Selfie with Shop Board** | 6 hrs | HIGH - Verify shops |
| 14 | **Aadhaar Face Match** | 16 hrs | CRITICAL - 100% real humans |
| 15 | **Background Safe-Check** | 8 hrs | HIGH - Safety for home jobs |
| 16 | **SOS Panic Button** | 8 hrs | CRITICAL - Emergency safety |
| 17 | **Real-time Chat** | 16 hrs | HIGH - Communication |
| 18 | **Admin Panel** | 12 hrs | HIGH - Moderation |

---

## 📞 CONTACT & SUPPORT

| Channel | Details |
|---------|---------|
| Email | dutypein@gmail.com |
| Phone | +91-9121706236 |
| Hours | Mon-Sat, 9 AM - 6 PM IST |
| Address | 2-80-6, Surya Thanda Village, Enkoor, Khammam, Telangana 507168 |
| Play Store | https://play.google.com/store/apps/details?id=com.dutype.app |

---

## 🏆 WHY DUTYPE WILL WIN

### For Workers
✅ Find jobs near your home (not 20km away)  
✅ No resume needed (just photo + skills)  
✅ 1-tap apply (auto-filled profile)  
✅ Build your reputation (ratings)  
✅ Get paid fairly (market rate suggestions)  
✅ Digital identity (visiting card)  
✅ Rate bad employers (protect community)  
✅ SOS button for emergencies  
✅ Aadhaar verification for trust  

### For Employers
✅ Find workers instantly (location-based)  
✅ Verified profiles (Aadhaar + ratings)  
✅ Simple job posting (4-step wizard)  
✅ Video verification (trust +100%)  
✅ Background safe-check (for home jobs)    
✅ Manage applications (accept/reject)  
✅ Rate workers (build trust)  
✅ Analytics dashboard (track performance)  
✅ Standard rate card (fair pricing)  

### For India
✅ 450M+ blue-collar workers served  
✅ Zero fraud tolerance (No-Data-Entry Firewall)  
✅ Hyperlocal focus (Map Radar)  
✅ Voice-first design (TTS)  
✅ Regional language support (Telugu)  
✅ Community-based trust (Local Reference)  
✅ Safety features (SOS, Background Check)  
✅ Fair wages (Rate Card, Market Rates)  
✅ "Genuinity" is the product  

---

## 📝 CHANGELOG

### December 29, 2025 (Latest)
- Added Deep Dive Scenarios section:
  - Scenario 4: Elderly Care Assistant (Medicine Timeline, Geofence Alert, Mood Check-In)
  - Scenario 5: Temp Driver (Behavioral Telematics, License OCR, Video Testimonials)
  - Scenario 6: Cash-Handling Helper (Digital Collateral, Aadhaar Legal Consent)
- Added DutyPe Trust Shield Infrastructure:
  - SafePay (Escrow System)
  - SOS Panic Widget (Lock screen emergency button)
  - Double-Blind Reviews
  - Trust Score Algorithm
  - Community Shield
- Updated feature count to 148 total (75 implemented, 73 not implemented)
- Added No-Data-Entry Firewall (keyword ban)
- Added Map Radar (Uber-style pulsing dots)
- Added Market Rate Suggestions
- Added The Rate Card (standard rates)
- Added Aadhaar Face Match (live selfie + blink)
- Added Background Safe-Check
- Added SOS Panic Button
- Comprehensive feature audit completed

### December 28, 2025
- Razorpay payment integration
- Subscription plans (4 tiers)
- Employer Trust Badges (3-tier)
- Google Maps integration
- Policy screens (Razorpay KYC)
- Digital Visiting Card

### December 27, 2025
- Structured Job Titles (anti-fraud)
- Location Consistency Check (30km)

### December 25, 2025
- Age Validation (18-70)
- Device Fingerprint Storage
- Hide Applied Jobs

### December 24, 2025
- Two-way Rating System
- Developer Mode Detection

---

**Document Version:** 5.0  
**Last Updated:** December 29, 2025  
**Maintained by:** KGPV Tech Solutions  
**App Package:** com.dutype.app

---

> *"Connecting Local Jobs with Local Workers"*
> 
> **DutyPe - India's Most Trusted Hyperlocal Job Marketplace**
> 
> **Zero Fraud. 100% Genuine. Community-Powered.**
