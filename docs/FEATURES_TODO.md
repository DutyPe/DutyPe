# DutyPe - Features To Implement

**Last Updated:** December 29, 2025  
**Total TODO Features:** 18 (based on user requirements analysis)

---

## 🚨 CRITICAL MISSING FEATURES (User Requirements)

### 1. Language Selector (Telugu/English) ❌ HIGH PRIORITY
- **Description:** App must restart in chosen language
- **Current State:** Not implemented - English only
- **Implementation:**
  ```kotlin
  // utils/LocaleHelper.kt
  object LocaleHelper {
      fun setLocale(context: Context, language: String): Context {
          val locale = Locale(language) // "te" for Telugu, "en" for English
          Locale.setDefault(locale)
          val config = Configuration(context.resources.configuration)
          config.setLocale(locale)
          return context.createConfigurationContext(config)
      }
  }
  ```
- **Files to Create:**
  - `utils/LocaleHelper.kt`
  - `screens/LanguageSelectionScreen.kt`
  - Update `strings.xml` with Telugu translations
- **Complexity:** Medium (3-4 hours)

### 2. WhatsApp Apply (One-Click) ❌ HIGH PRIORITY
- **Description:** One-click redirection to Owner's WhatsApp with pre-filled message
- **Current State:** Not implemented
- **Implementation:**
  ```kotlin
  fun openWhatsAppWithMessage(context: Context, phone: String, jobTitle: String) {
      val message = "Hi! I'm interested in the $jobTitle position posted on DutyPe. I would like to apply."
      val url = "https://wa.me/$phone?text=${URLEncoder.encode(message, "UTF-8")}"
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
      context.startActivity(intent)
  }
  ```
- **Files to Modify:**
  - `JobDescriptionScreen.kt` - Add WhatsApp button
  - `WorkerJobCard.kt` - Add quick WhatsApp action
- **Complexity:** Low (1-2 hours)

### 3. Admin Panel ❌ MEDIUM PRIORITY
- **Description:** Simple panel to delete spam/fake jobs
- **Current State:** Not implemented
- **Options:**
  1. Firebase Console (manual)
  2. Simple Admin Screen in app
  3. Web-based admin panel
- **Recommended:** Start with Firebase Console + Cloud Functions for auto-moderation
- **Complexity:** High (1-2 days)

### 4. Audio-First Interface (TextToSpeech) ❌ MEDIUM PRIORITY
- **Description:** Speaker icon next to Job Titles for semi-literate users
- **Current State:** Not implemented
- **Implementation:**
  ```kotlin
  // utils/TextToSpeechHelper.kt
  class TextToSpeechHelper(context: Context) {
      private val tts = TextToSpeech(context) { status ->
          if (status == TextToSpeech.SUCCESS) {
              tts.language = Locale("te", "IN") // Telugu
          }
      }
      
      fun speak(text: String) {
          tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
      }
  }
  ```
- **Files to Create:**
  - `utils/TextToSpeechHelper.kt`
- **Files to Modify:**
  - `WorkerJobCard.kt` - Add speaker icon
  - `JobDescriptionScreen.kt` - Add TTS button
- **Complexity:** Medium (2-3 hours)

### 5. Geo-Fencing UI (3KM Radius Filter) ❌ MEDIUM PRIORITY
- **Description:** "Show jobs within 3KM of my live location" toggle
- **Current State:** Distance calculated but no filter UI
- **Implementation:**
  - Add radius slider/dropdown in WorkerHomeScreen
  - Filter jobs by distance in ViewModel
- **Files to Modify:**
  - `WorkerHomeScreen.kt` - Add filter UI
  - `FirestoreJobViewModel.kt` - Add distance filter logic
- **Complexity:** Low (2 hours)

### 6. Job Poster Generator (PDF) ❌ LOW PRIORITY
- **Description:** Owners can generate a "Wanted" poster PDF to print for shop window
- **Current State:** Not implemented
- **Implementation:**
  ```kotlin
  // Use Android's PdfDocument API
  fun generateJobPoster(job: JobListing): File {
      val document = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
      val page = document.startPage(pageInfo)
      // Draw job details on canvas
      document.finishPage(page)
      // Save to file
  }
  ```
- **Complexity:** Medium (4-5 hours)

### 7. QR Code in Referral System ❌ LOW PRIORITY
- **Description:** Generate shareable image with QR code for referrals
- **Current State:** Referral system exists but no QR code
- **Implementation:**
  - Use ZXing library for QR generation
  - Combine with referral card image
- **Complexity:** Low (2 hours)

### 8. Aadhaar OCR & Face Match ❌ LOW PRIORITY (Phase 3)
- **Description:** Government ID verification for "Green Tick" badge
- **Current State:** Not implemented
- **Dependencies:**
  ```kotlin
  implementation("com.google.mlkit:text-recognition:16.0.0")
  implementation("com.google.mlkit:face-detection:16.1.5")
  ```
- **Complexity:** High (1-2 weeks)

---

## ✅ RECENTLY COMPLETED FEATURES

| Feature | Date | Notes |
|---------|------|-------|
| Digital Visiting Card | Dec 28, 2025 | Shareable image card |
| Map View (Google Maps) | Dec 28, 2025 | Job pins on map |
| Razorpay Integration | Dec 28, 2025 | Subscription payments |
| Featured Listings | Dec 28, 2025 | Pro/Enterprise plans |
| Direct Call Button | Pre-existing | ACTION_DIAL intent |
| Save/Favorite Jobs | Pre-existing | SavedJobsViewModel |
| Category Filtering | Pre-existing | FilterChip UI |
| Salary Type Filter | Pre-existing | PayType filtering |
| Role Selection | Pre-existing | "I want a Job" / "I want to Hire" |
| Referral System | Pre-existing | EmployerReferEarnScreen |

---

## 📋 IMPLEMENTATION PRIORITY ORDER

### Week 1: Quick Wins
1. ⚡ **WhatsApp Apply** (1-2 hours) - Huge UX improvement
2. ⚡ **Geo-Fencing UI** (2 hours) - 3KM radius filter
3. ⚡ **QR Code Referral** (2 hours) - Viral growth

### Week 2: Core Features
4. 🔧 **Language Selector** (3-4 hours) - Telugu/English
5. 🔧 **Audio-First Interface** (2-3 hours) - TTS for jobs

### Week 3: Operations
6. 🔧 **Real-time Chat** (1-2 days) - Backend implementation
7. 🔧 **Admin Panel** (1-2 days) - Spam management

### Month 2: Advanced
8. 📄 **Job Poster Generator** (4-5 hours) - PDF for shops
9. 🔐 **Aadhaar Verification** (1-2 weeks) - OCR + Face Match

### Month 3: Trust Shield Features
10. 🛡️ **Standby Bench** (16 hours) - Auto-replace no-shows with standby workers
11. ~~🔐 **Work Start Verification (QR/Code)** (12 hours) - QR or code to verify work started~~ ✅ DONE
12. 💳 **SafePay (Escrow)** (16 hours) - Employer deposits before job
13. 🆘 **SOS Panic Widget** (12 hours) - Lock screen emergency button

---

## 🔧 DEPENDENCIES TO ADD

```kotlin
// build.gradle.kts

// QR Code Generation
implementation("com.google.zxing:core:3.5.2")

// PDF Generation (already in Android SDK)
// No additional dependency needed

// Aadhaar Verification (Future)
implementation("com.google.mlkit:text-recognition:16.0.0")
implementation("com.google.mlkit:face-detection:16.1.5")
```

---

## 📊 FEATURE COMPLETION MATRIX

| User Requirement | Status | Priority |
|-----------------|--------|----------|
| OTP Login | ✅ | - |
| Language Selector | ❌ | HIGH |
| Role Selection | ✅ | - |
| Digital Visiting Card | ✅ | - |
| Geo-Fencing (3KM) | ⚠️ | MEDIUM |
| Map View | ✅ | - |
| Category Chips | ✅ | - |
| Salary Filter | ✅ | - |
| Direct Call Button | ✅ | - |
| WhatsApp Apply | ❌ | HIGH |
| Save/Favorite | ✅ | - |
| Referral System | ✅ | - |
| QR Code Referral | ❌ | LOW |
| Push Notifications | ✅ | - |
| Admin Panel | ❌ | MEDIUM |
| Audio-First (TTS) | ❌ | MEDIUM |
| Job Poster PDF | ❌ | LOW |
| Featured Listings | ✅ | - |
| Aadhaar Verification | ❌ | LOW |
| No-Data-Entry Firewall | ✅ | - |
| Pay Rate Guardrails | ✅ | - |

---

**Total Features: 21**  
**Implemented: 14 (67%)**  
**Remaining: 7 (33%)**  
**Estimated Timeline: 3-4 weeks for core features**
