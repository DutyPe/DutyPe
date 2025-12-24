# DutyPe Features Documentation

## Overview

DutyPe is a comprehensive job marketplace platform with distinct features for Workers and Employers. This document details all features, their implementation, and user flows.

---

## Authentication Features

### 1. Google Sign-In

**Description:** One-tap Google authentication using Android Credential Manager API.

**Implementation:** `auth/GoogleSignInManager.kt`

**Flow:**
1. User taps "Continue with Google"
2. System shows Google account picker
3. On selection, Firebase authenticates user
4. Profile data (email, name) pre-filled for setup

**Key Code:**
```kotlin
class GoogleSignInManager(private val context: Context) {
    suspend fun signIn(): Flow<Result<AuthResult>>
    suspend fun signOut(): Flow<Result<Unit>>
}
```

**Benefits:**
- No password to remember
- Quick onboarding
- Trusted authentication

---

### 2. Phone OTP Authentication

**Description:** Phone number verification using Firebase Phone Auth with OTP.

**Implementation:** `viewmodels/OtpViewModel.kt`

**Flow:**
1. User enters phone number
2. System sends OTP via SMS
3. User enters 6-digit OTP
4. Firebase verifies and authenticates
5. Phone number pre-filled for profile setup

**Features:**
- Auto-read OTP (with permission)
- Resend OTP option
- Rate limiting protection

---

### 3. Dual Role Prevention

**Description:** Prevents users from having both Worker and Employer accounts with same phone.

**Implementation:** `phone_roles` Firestore collection

**Flow:**
1. Before OTP send, check `phone_roles/{phoneNumber}`
2. If exists with different role, show error
3. If same role or new, proceed with authentication

**Error Message:** "This phone number is already registered as [Role]. Please use a different number."

---

## Worker Features

### 1. Job Discovery

**Screen:** `worker/screens/WorkerHomeScreen.kt`

**Features:**
- Location-based job listings
- Category filter chips (Cook, Driver, Cleaner, etc.)
- Search functionality
- Pull-to-refresh
- Distance display from user location

**Job Card Information:**
- Job title and category icon
- Company name
- Pay amount and type (Daily/Hourly/Monthly)
- Location with distance
- Posted time
- Save/Share actions

---

### 2. Advanced Job Search

**Screen:** `worker/screens/AllJobsScreen.kt`

**Filters Available:**
| Filter | Options |
|--------|---------|
| Category | Cook, Driver, Cleaner, Helper, Delivery, Security, etc. |
| Pay Type | Daily, Hourly, Monthly, Per Task |
| Distance | 5km, 10km, 25km, 50km, Any |
| Sort By | Newest, Nearest, Highest Pay |

**Search Features:**
- Text search by job title/company
- Real-time filtering
- Result count display
- Clear all filters option

---

### 3. Job Details View

**Screen:** `worker/screens/JobDescriptionScreen.kt`

**Information Displayed:**
- Full job description
- Requirements and qualifications
- Work schedule/shift timing
- Exact location with map link
- Employer contact information
- Application deadline
- Number of vacancies

**Actions:**
- Apply Now button
- Save job for later
- Share via WhatsApp
- Call employer directly

---

### 4. Smart Job Application

**Screen:** `worker/screens/SmartJobApplicationScreen.kt`

**Features:**
- Multi-step application form
- Auto-fill from profile data
- Cover letter input
- Skills selection
- Experience details
- Document upload option

**Auto-Fill Fields:**
- Full name
- Email
- Phone number
- Address
- Skills from profile

**Application Status Tracking:**
- PENDING → UNDER_REVIEW → ACCEPTED/REJECTED
- Visual timeline in application card
- Push notifications on status change

---

### 5. My Jobs Management

**Screen:** `worker/screens/myJobs/MyJobsScreen.kt`

**Tabs:**

**Applied Jobs Tab:**
- List of all applications
- Status filter chips (All, Pending, Under Review, Accepted, Rejected)
- Search within applications
- Withdraw application option
- View job details

**Saved Jobs Tab:**
- Bookmarked jobs for later
- Quick apply from saved
- Remove from saved
- Check if job still active

---

### 6. Application Timeline

**Component:** `worker/components/JobApplicationCard.kt`

**Visual Timeline Steps:**
1. Applied ✓
2. Pending Review (current/completed)
3. Under Review (current/completed)
4. Decision (Accepted ✓ / Rejected ✗)

**Features:**
- Animated progress indicator
- Color-coded status
- Time since application
- Withdraw button for pending applications

---

### 7. Worker Profile

**Screen:** `worker/screens/profile/WorkerProfile.kt`

**Profile Sections:**
- Profile photo upload
- Personal information
- Contact details
- Skills and experience
- Work preferences

**Settings Menu:**
- Notification settings
- Application history
- Help & Support
- About Us
- Privacy Policy
- Terms & Conditions
- Logout

---

### 8. Mandatory Profile Setup

**Screen:** `worker/screens/MandatoryWorkerProfileSetupScreen.kt`

**3-Step Process:**

**Step 1 - Personal Information:**
- Full name (required)
- Email (required for Google auth)
- Phone number (required)

**Step 2 - Additional Details:**
- Address (required)
- Date of birth (required)
- Gender (required)

**Step 3 - Professional Information:**
- Skills (required)
- Experience level (required)

**Validation:**
- Real-time field validation
- Error messages on Next click
- Progress indicator
- Cannot skip mandatory fields

---

## Employer Features

### 1. Employer Dashboard

**Screen:** `employer/screens/homeScreen/EmployerHomeScreen.kt`

**Dashboard Cards:**
- Active Jobs count
- Total Applications received
- Jobs posted today
- Paused jobs count

**Quick Actions:**
- Post New Job
- View All Applications
- Manage Posted Jobs

**Recent Jobs Section:**
- Last 10 posted jobs
- Quick status view
- Direct navigation to job management

---

### 2. Job Posting Wizard

**Screen:** `employer/screens/postjob/PostJobScreen.kt`

**4-Step Process:**

**Step 1 - Basic Information:**
- Job title
- Category selection
- Job description

**Step 2 - Compensation:**
- Pay amount
- Pay type (Daily/Hourly/Monthly/Per Task)
- Number of vacancies

**Step 3 - Location & Schedule:**
- Job location (auto-detect or manual)
- Shift timing
- Urgency level

**Step 4 - Review & Post:**
- Preview all details
- Edit any section
- Confirm and post

**Post-Submission:**
- Success notification
- Job appears in dashboard
- Push notification sent

---

### 3. Application Management

**Screen:** `employer/screens/applications/EmployerApplicationManagementScreen.kt`

**Features:**
- View all applications for a job
- Filter by status
- Search by applicant name
- Sort by date/status

**Application Card Shows:**
- Applicant name and photo
- Contact information
- Skills preview
- Cover letter preview
- Applied date
- Current status

**Actions:**
- View full application details
- Accept application
- Reject application
- Contact applicant (call/email)

---

### 4. Application Detail View

**Screen:** `employer/screens/applications/ApplicationDetailScreen.kt`

**Sections:**
- Applicant profile card
- Contact information with action buttons
- Work experience
- Skills list
- Cover letter (full)
- Application timeline

**Action Buttons:**
- Accept (green)
- Reject (red)
- Call applicant
- Email applicant

---

### 5. Posted Jobs Management

**Screen:** `employer/screens/postedJobs/MyJobsScreen.kt`

**Job Card Actions:**
- Edit job details
- View applications
- Pause/Resume job
- Share job
- Delete job

**Job Status Indicators:**
- Active (green)
- Paused (yellow)
- Expired (red)
- Filled (gray)

---

### 6. Edit Job

**Screen:** `employer/screens/editjob/EditJobScreen.kt`

**Editable Fields:**
- Job title
- Description
- Pay amount/type
- Location
- Vacancies
- Shift timing

**Restrictions:**
- Cannot edit after 48 hours
- Warning message displayed
- Some fields may be locked

---

### 7. Job History

**Screen:** `employer/screens/history/EmployerHistoryScreen.kt`

**Tabs:**
- All Jobs
- Active
- Expired
- Paused

**Statistics:**
- Total jobs posted
- Active jobs count
- Expired jobs count
- Total applications received

---

### 8. Employer Profile

**Screen:** `employer/screens/profilescreen/EmployerProfileScreen.kt`

**Company Information:**
- Company logo upload
- Company name
- Contact email
- Contact phone
- Business address

**Settings Menu:**
- Manage Addresses
- Notification Settings
- Job Posting History
- Help & Support
- About Us
- Privacy Policy
- Terms & Conditions
- Security
- Send Feedback
- Logout

---

## Common Features

### 1. Onboarding

**Screen:** `onboarding/OnboardingScreen.kt`

**3 Pages:**
1. Find Jobs Near You - Location-based discovery
2. Apply Easily - Simple application process
3. Get Hired Fast - Quick employer response

**Features:**
- Swipe navigation
- Skip option
- Progress dots
- Lottie animations

---

### 2. Role Selection

**Screen:** `common/chat/SelectRoleScreen.kt`

**Options:**
- "I'm looking for work" → Worker flow
- "I'm hiring" → Employer flow

**Features:**
- Animated illustrations
- Clear role descriptions
- Single selection required

---

### 3. Splash Screen

**Component:** `components/DutyPeSplashScreen.kt`

**Features:**
- DutyPe logo display
- 2-second duration (new users)
- 1.5-second duration (returning users)
- Black background with centered logo

---

### 4. Notification System

**Components:**
- `services/NotificationService.kt`
- `notifications/manager/InAppNotificationManager.kt`
- `notifications/components/InAppNotificationBanner.kt`

**Notification Types:**
| Type | Description |
|------|-------------|
| JOB_APPLICATION | New application received |
| APPLICATION_STATUS | Status update |
| NEW_JOB | New job matching preferences |
| MESSAGE | New message |
| SYSTEM_UPDATE | App updates |

**Channels:**
- Job Alerts (High priority)
- Application Updates (High priority)
- Messages (Default priority)
- Promotions (Low priority)

---

### 5. Location Services

**Components:**
- `utils/LocationService.kt`
- `location/LocationPreferences.kt`
- `location/PlacesLocationManager.kt`

**Features:**
- Auto-detect current location
- Manual location selection
- Google Places autocomplete
- Distance calculation (Haversine formula)
- Location persistence

---

### 6. Feedback System

**Component:** `components/FeedbackBottomSheet.kt`

**Features:**
- 5-star rating
- Category selection (General, Bug, Feature Request, etc.)
- Text feedback input
- Anonymous submission option
- Success animation

---

### 7. Help & Support

**Screen:** `common/chat/help/HelpMainScreen.kt`

**Options:**
- FAQs
- Chat Support
- WhatsApp Support
- Report a Problem
- How to Use the App (Tutorial)

---

### 8. Information Screens

**FAQ Screen:** `common/chat/info/FaqScreen.kt`
- Expandable question/answer cards
- Categorized by topic
- Search functionality

**Privacy Policy:** `common/chat/info/PrivacyPolicyScreen.kt`
- Numbered sections
- Clear formatting
- Contact information

**Terms & Conditions:** `common/chat/info/TermsAndConditionsScreen.kt`
- Legal terms
- User responsibilities
- Platform usage rules

---

### 9. Logout Flow

**Component:** `components/ProfessionalLogoutDialog.kt`

**Process:**
1. Show confirmation bottom sheet
2. Sign out from Google (if applicable)
3. Sign out from Firebase
4. Clear local auth data
5. Reset profile setup state
6. Navigate to login screen

---

## UI Components Library

### Shimmer Loading States

**Component:** `components/ShimmerComponents.kt`

**Available Shimmers:**
- `ProfileShimmer` - Profile screen loading
- `ApplicationDetailShimmer` - Application detail loading
- `ApplicationListItemShimmer` - List item loading
- `NotificationShimmer` - Notification screen loading
- `ContentCardShimmer` - Generic card loading

### Bottom Navigation

**Component:** `components/ReusableBottomBar.kt`

**Worker Tabs:**
- Jobs (Home icon)
- My Jobs (History icon)
- Profile (Person icon)

**Employer Tabs:**
- Home (Home icon)
- Post (Add icon)
- Profile (Person icon)

### Common Header

**Component:** `components/CommonHeader.kt`

**Features:**
- Back button  
- Title text
- Optional action buttons
- Consistent styling

---

## Feature Flags & Configuration

**Currently Disabled:**
- Ads (AdsManager disabled for testing)
- Some promotional notifications

**Configurable:**
- Notification preferences per user
- Location radius for job search
- Profile completion requirements
