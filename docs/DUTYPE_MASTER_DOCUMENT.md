# DutyPe - Master Documentation & Feature Roadmap

**Last Updated:** December 25, 2025  
**App Version:** 1.0.0  
**Platform:** Android (Kotlin + Jetpack Compose)

---

## TABLE OF CONTENTS

1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture](#architecture)
4. [Feature Status by Version](#feature-status-by-version)
5. [Detailed Feature List](#detailed-feature-list)
6. [API Reference Summary](#api-reference-summary)
7. [Development Guidelines](#development-guidelines)

---

## PROJECT OVERVIEW

### What is DutyPe?
DutyPe is a **hyperlocal job marketplace** connecting blue-collar workers with employers across India for local jobs (home, shops, restaurants, offices, factories, hospitals, warehouses, events).

### Core Pillars
1. **Hyper-Local:** 100% location-dependent - if it's not nearby, it doesn't exist
2. **Non-IT / Blue Collar:** Interfaces designed for low-literacy users - visual, voice-first, simple
3. **Zero Fraud:** Aggressive filtering of fake jobs, scams, and spam - "Genuinity" is the product

### Project Stats
- **108 Kotlin files**
- **100% MVVM + Jetpack Compose architecture**
- **Firebase backend** (Firestore, FCM, Auth, Crashlytics)
- **Location-based matching** (Haversine formula)
- **Dual-role system** (Worker & Employer in same app)

---

## TECHNOLOGY STACK

| Layer | Technology | Version |
|-------|------------|---------|
| Language | Kotlin | 2.0.21 |
| UI Framework | Jetpack Compose | BOM 2024.09.00 |
| Architecture | MVVM + Repository | - |
| DI Framework | Hilt | 2.52 |
| Backend | Firebase | Multiple |
| Image Loading | Coil | 2.4.0 |
| Navigation | Navigation Compose | 2.8.9 |
| Logging | Timber | - |

### Firebase Services Used
- **Firebase Auth** - Google Sign-In, Phone OTP
- **Cloud Firestore** - Database
- **Firebase Cloud Messaging (FCM)** - Push notifications
- **Firebase Crashlytics** - Crash reporting
- **Firebase Storage** - Profile images

---

## ARCHITECTURE

### Project Structure
```
app/src/main/java/com/example/dutype/
├── auth/                   # Authentication (Google, OTP)
├── common/                 # Shared screens (Role selection, Help)
├── components/             # Reusable UI components
├── di/                     # Dependency Injection (Hilt)
├── employer/               # Employer module (screens, viewmodels)
├── location/               # Location services
├── models/                 # Data models
├── navigation/             # Navigation graphs & routes
├── repositories/           # Data repositories
├── services/               # Business logic services
├── state/                  # State managers
├── ui/theme/               # Theme, colors, typography
├── utils/                  # Utilities
├── viewmodels/             # Shared ViewModels
├── worker/                 # Worker module (screens, viewmodels)
├── MainActivity.kt
└── DutyPeApplication.kt
```

### Data Flow
```
Screen (Composable) → ViewModel → Repository → Service → Firestore
         ↑                                                    |
         └──────────── StateFlow (UI State) ←─────────────────┘
```

### Firestore Collections
| Collection | Purpose |
|------------|---------|
| `users` | User accounts & profiles |
| `worker_profiles` | Worker-specific data |
| `employer_profiles` | Employer-specific data |
| `jobs` | Job listings |
| `job_applications` | Applications |
| `saved_jobs` | Bookmarked jobs |
| `notifications` | User notifications |
| `phone_roles` | Phone-to-role mapping |
| `fcm_tokens` | Push notification tokens |

---

## FEATURE STATUS BY VERSION

### ✅ CURRENT VERSION (v1.0) - IMPLEMENTED

#### Authentication & Onboarding
| Feature | Status | Files |
|---------|--------|-------|
| Google Sign-In | ✅ Done | `GoogleSignInManager.kt`, `EnhancedLoginScreen.kt` |
| Phone OTP Login | ✅ Done | `OtpViewModel.kt` |
| Dual-role System | ✅ Done | `SelectRoleScreen.kt` |
| Guest Mode | ✅ Done | `EnhancedLoginScreen.kt` |
| Animated Onboarding | ✅ Done | `OnboardingScreen.kt` |
| Dual-role Prevention | ✅ Done | `phone_roles` collection |

#### Worker Features
| Feature | Status | Files |
|---------|--------|-------|
| Worker Profile System | ✅ Done | `WorkerProfile.kt`, `ProfileCompletionService.kt` |
| Profile Completion % | ✅ Done | `ProfileCompletionProgress.kt` |
| Mandatory Profile Setup | ✅ Done | `MandatoryWorkerProfileSetupScreen.kt` |
| Job Discovery | ✅ Done | `WorkerHomeScreen.kt`, `AllJobsScreen.kt` |
| Location-based Matching | ✅ Done | `FirestoreJobViewModel.kt`, `LocationService.kt` |
| Job Filtering | ✅ Done | `AllJobsScreen.kt` |
| Save/Bookmark Jobs | ✅ Done | `SavedJobsList.kt`, `SavedJobsViewModel.kt` |
| Job Details View | ✅ Done | `JobDescriptionScreen.kt` |
| 1-Tap Application | ✅ Done | `SmartJobApplicationScreen.kt` |
| Application Status Tracking | ✅ Done | `JobApplicationCard.kt`, `MyJobsScreen.kt` |
| Application Timeline | ✅ Done | `JobApplicationCard.kt` |
| Work History | ✅ Done | `WorkerHistoryScreen.kt` (LinkedIn-style timeline) |
| Notifications | ✅ Done | `WorkerNotificationScreen.kt` |
| Notification Settings | ✅ Done | `WorkerNotificationSettingsScreen.kt` |

#### Employer Features
| Feature | Status | Files |
|---------|--------|-------|
| Employer Profile | ✅ Done | `EmployerProfileScreen.kt` |
| Company Details | ✅ Done | `EmployerCompanyDetailsScreen.kt` |
| Mandatory Profile Setup | ✅ Done | `MandatoryEmployerProfileSetupScreen.kt` |
| 4-Step Job Posting | ✅ Done | `PostJobScreen.kt` |
| Job Categories | ✅ Done | `PostJobFormComponents.kt` |
| GPS Auto-fill Location | ✅ Done | `LocationService.kt` |
| Application Management | ✅ Done | `EmployerApplicationManagementScreen.kt` |
| View Worker Profiles | ✅ Done | `ProfessionalWorkerProfileViewScreen.kt` |
| Accept/Reject Applications | ✅ Done | `ApplicationDetailScreen.kt` |
| Edit Posted Jobs | ✅ Done | `EditJobScreen.kt` |
| Job History | ✅ Done | `EmployerHistoryScreen.kt` |
| Analytics Dashboard | ✅ Done | `AnalyticsScreen.kt` |
| Notifications | ✅ Done | `EmployerNotificationScreen.kt` |
| Notification Settings | ✅ Done | `EmployerNotificationSettingsScreen.kt` |

#### Common Features
| Feature | Status | Files |
|---------|--------|-------|
| Push Notifications (FCM) | ✅ Done | `DutyPeFirebaseMessagingService.kt`, `FCMTokenManager.kt` |
| In-app Notifications | ✅ Done | `NotificationService.kt` |
| Chat/Messaging | ⚠️ UI Only | `ChatDetailsScreen.kt` (mockup only) |
| Location Services | ✅ Done | `LocationService.kt`, `LocationPreferences.kt` |
| Google Places API | ✅ Done | `PlacesLocationManager.kt` |
| Two-way Rating | ✅ Done | `JobRatingBottomSheet.kt`, `RatingService.kt` |
| Developer Mode Detection | ✅ Done | `DeveloperModeWarningSheet.kt`, `MainActivity.kt` |
| Age Validation (18-70) | ✅ Done | `ValidationUtils.kt`, Profile Setup Screens |
| Device Fingerprint Storage | ✅ Done | `ProfileCompletionService.kt`, `phone_roles` collection |
| Hide Applied Jobs | ✅ Done | `WorkerHomeScreen.kt`, `AllJobsScreen.kt` |
| Trust Score | ⚠️ Partial | `ProfileCompletionService.kt` (profile % only) |
| Help & Support | ✅ Done | `HelpMainScreen.kt` |
| FAQ | ✅ Done | `FaqScreen.kt` |
| Privacy Policy | ✅ Done | `PrivacyPolicyScreen.kt` |
| Terms & Conditions | ✅ Done | `TermsAndConditionsScreen.kt` |
| Feedback System | ✅ Done | `FeedbackBottomSheet.kt` |
| Shimmer Loading | ✅ Done | `ShimmerComponents.kt` |
| Pull-to-Refresh | ✅ Done | Multiple screens |

---

## DETAILED FEATURE LIST

### Implemented Features (39 Total)

#### 1. User Authentication & Role Selection ✅
- Phone OTP login via Firebase
- Google Sign-In with Credential Manager API
- Dual-role system (Worker OR Employer)
- Guest mode for browsing
- Dual-role prevention (same phone can't be both)

#### 2. Worker Profile System ✅
- Profile completion % (0-100%)
- Fields: name, skills, experience, phone, address, DOB, gender, photo
- Gamified progress bar
- Auto-sync to Firestore

#### 3. Employer Profile & Verification ✅
- Company name, location, contact info
- GPS auto-fill for location
- Trust score from behavior

#### 4. Job Posting Wizard (4-Step) ✅
- Step 1: Job details & category
- Step 2: Payment & location
- Step 3: Contact & urgency
- Step 4: Review & submit

#### 5. Job Discovery & Location Matching ✅
- Haversine formula for distance
- Filter by category, pay type, distance
- Sort by distance, newest, highest pay
- Real-time updates

#### 6. Worker Application System ✅
- 1-tap apply with auto-fill
- Application status tracking
- Visual timeline (Pending → Under Review → Accepted/Rejected)
- Withdraw application option

#### 7. Employer Application Management ✅
- View all applications
- Filter by status
- Accept/Reject with notifications
- View worker profiles

#### 8. Two-way Rating System ✅
- Worker rates employer after job completion
- Employer rates worker after job completion
- Rating categories: Overall, Punctuality, Quality, Communication, Professionalism, Payment
- Quick tags for fast feedback
- Rating summaries with star distribution
- Firestore collections: `ratings`, `rating_summaries`

#### 9. Developer Mode Detection ✅
- Detects if Developer Options are enabled on device
- Shows non-dismissible security warning bottom sheet
- Explains why Developer Mode is a security risk (fake GPS)
- Provides instructions to disable Developer Mode
- "Open Settings" button to navigate to Developer Options
- "Exit App" button forces app closure
- Re-checks on app resume (in case user disabled it)

#### 10. Age Validation (18-70 years) ✅
- Profile setup screens validate date of birth
- Users must be between 18-70 years old
- Shows appropriate error messages for invalid ages
- Prevents underage or overage users from completing profile

#### 11. Device Fingerprint Storage ✅
- Stores device fingerprint in `phone_roles` collection
- Captures: Android ID, device model, device fingerprint
- Records `joinedAt` (first registration) and `updatedAt` timestamps
- Enables fraud prevention by tracking devices

#### 12. Hide Applied Jobs from Worker Screens ✅
- Jobs worker has applied to are hidden from WorkerHomeScreen
- Jobs worker has applied to are hidden from AllJobsScreen
- Applied jobs only visible in MyJobsScreen's applied jobs tab
- Cleaner UX - no duplicate job cards

#### 13. Notification System ✅
- Push notifications (FCM)
- In-app notifications
- Notification settings per channel

#### 11. Chat/Messaging ⚠️ (UI Only)
- Chat UI mockup exists
- Real-time Firebase messaging NOT implemented
- Message history NOT persisted

---

## API REFERENCE SUMMARY

### Key Services

| Service | Purpose | Key Methods |
|---------|---------|-------------|
| `FirestoreService` | Database CRUD | `createJob()`, `getJob()`, `updateJob()` |
| `JobApplicationService` | Application workflow | `submitApplication()`, `updateStatus()` |
| `NotificationService` | Push & local notifications | `sendNotification()`, `getNotifications()` |
| `ProfileCompletionService` | Profile management | `saveProfile()`, `getCompletionPercent()` |
| `FCMTokenManager` | Push token management | `saveToken()`, `deleteToken()` |

### Key ViewModels

| ViewModel | Purpose |
|-----------|---------|
| `FirestoreJobViewModel` | Worker job discovery |
| `FirestoreEmployerJobViewModel` | Employer job management |
| `JobApplicationViewModel` | Application handling |
| `ProfileCompletionViewModel` | Profile setup |
| `SavedJobsViewModel` | Bookmarked jobs |

### Key Models

| Model | Fields |
|-------|--------|
| `User` | id, email, phone, name, role, profileImageUrl |
| `JobListing` | id, title, description, payAmount, location, employerId |
| `JobApplication` | applicationId, jobId, workerId, status, appliedAt |
| `NotificationData` | id, title, message, type, isRead |

---

## DEVELOPMENT GUIDELINES

### Adding New Screens
1. Add route to `Routes.kt`
2. Create screen composable in appropriate module
3. Add to NavGraph
4. Create ViewModel if needed

### Adding New Services
1. Create service class with `@Singleton`
2. Add provider to `AppModule.kt`
3. Inject in ViewModel

### Code Style
- Classes: PascalCase (`JobApplicationService`)
- Functions: camelCase (`loadJobs()`)
- Constants: SCREAMING_SNAKE (`MAX_RETRY_COUNT`)
- Composables: PascalCase (`JobCard()`)

### Error Handling
```kotlin
result.fold(
    onSuccess = { /* update success state */ },
    onFailure = { /* update error state */ }
)
```

---

## SUMMARY

| Category | Implemented | Partial/UI Only |
|----------|-------------|-----------------|
| Authentication | 6 | 0 |
| Worker Features | 15 | 0 |
| Employer Features | 14 | 0 |
| Common Features | 17 | 2 |
| **TOTAL** | **52** | **2** |

**Current Version: v1.0.0**

> ⚠️ Partial features (UI only, no backend): Chat/Messaging, Trust Score
> For upcoming features (35 total), see `docs/FEATURES_TODO.md`

---

## DOCUMENTS CONSOLIDATED

This master document consolidates content from:
- `ARCHITECTURE.md` - Technical architecture
- `FEATURES.md` - Feature documentation
- `API_REFERENCE.md` - API reference
- `DEVELOPMENT_GUIDE.md` - Development guidelines
- `APP_FEATURES.md` - Product roadmap
- `dutype_complete_analysis.md` - Complete feature analysis
- `dutype_ai_integration_plan.md` - AI integration plan
- `dutype_complete_md_analysis.md` - Project analysis

**Related Document:**
- `docs/FEATURES_TODO.md` - All 33 unimplemented features with implementation details

---

**End of Master Document**


---

## DETAILED API REFERENCE

### FirestoreService Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `createUser` | `userData: Map<String, Any>` | `Result<String>` | Creates new user document |
| `getUser` | `userId: String` | `Result<Map<String, Any>>` | Gets user by ID |
| `updateUser` | `userId: String, updates: Map<String, Any>` | `Result<Unit>` | Updates user document |
| `createJob` | `jobData: Map<String, Any>` | `Result<String>` | Creates new job listing |
| `getJob` | `jobId: String` | `Result<Map<String, Any>>` | Gets job by ID |
| `getAllJobs` | `limit: Long` | `Result<List<Map<String, Any>>>` | Gets all active jobs |
| `getJobsByEmployer` | `employerId: String` | `Result<List<Map<String, Any>>>` | Gets employer's jobs |
| `updateJob` | `jobId: String, updates: Map<String, Any>` | `Result<Unit>` | Updates job document |
| `deleteJob` | `jobId: String` | `Result<Unit>` | Deletes job document |

### JobApplicationService Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `submitApplication` | `application: JobApplication` | `Result<JobApplication>` | Submits new application |
| `getWorkerApplications` | `workerId: String` | `Flow<Result<List<JobApplication>>>` | Gets worker's applications |
| `getJobApplications` | `jobId: String` | `Flow<Result<List<JobApplication>>>` | Gets applications for a job |
| `getEmployerApplications` | `employerId: String` | `Flow<Result<List<JobApplication>>>` | Gets all employer's applications |
| `updateApplicationStatus` | `applicationId: String, status: ApplicationStatus, notes: String?` | `Result<Unit>` | Updates application status |
| `withdrawApplication` | `applicationId: String, workerId: String` | `Result<JobApplication>` | Withdraws application |
| `hasWorkerAppliedToJob` | `workerId: String, jobId: String` | `Result<Boolean>` | Checks if already applied |

### Application Status Flow
```
PENDING → UNDER_REVIEW → ACCEPTED
                      → REJECTED
        → WITHDRAWN (from PENDING or UNDER_REVIEW)
```

### NotificationService Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `sendJobPostedNotification` | `jobTitle: String, employerId: String` | `Unit` | Notifies job posted |
| `sendApplicationReceivedNotification` | `jobTitle: String, applicantName: String, employerId: String` | `Unit` | Notifies new application |
| `sendApplicationStatusNotification` | `jobTitle: String, status: String, workerId: String` | `Unit` | Notifies status change |
| `getNotifications` | `userId: String` | `Flow<List<NotificationData>>` | Gets user notifications |
| `markAsRead` | `notificationId: String` | `Result<Unit>` | Marks notification read |
| `markAllAsRead` | `userId: String` | `Result<Unit>` | Marks all as read |

### Notification Channels
```kotlin
object NotificationChannels {
    const val JOB_ALERTS = "job_alerts"           // High priority
    const val APPLICATION_UPDATES = "app_updates" // High priority
    const val MESSAGES = "messages"               // Default priority
    const val PROMOTIONS = "promotions"           // Low priority
}
```

### ProfileCompletionService Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `isProfileComplete` | `userId: String, role: String` | `Boolean` | Checks if profile complete |
| `saveUserInfo` | `email: String, name: String, role: String` | `Result<Unit>` | Saves basic user info |
| `saveWorkerProfileData` | `profileData: Map<String, Any>` | `Result<Unit>` | Saves worker profile |
| `saveEmployerProfileData` | `profileData: Map<String, Any>` | `Result<Unit>` | Saves employer profile |
| `getWorkerProfileData` | `userId: String` | `Result<Map<String, Any>>` | Gets worker profile |
| `getEmployerProfileData` | `userId: String` | `Result<Map<String, Any>>` | Gets employer profile |
| `uploadProfileImage` | `imageUri: Uri, userId: String, userRole: String` | `Result<String>` | Uploads profile image |
| `checkPhoneExistsWithDifferentRole` | `phone: String, currentRole: String` | `Result<String?>` | Checks dual role |

### Profile Completion Fields

**Worker Profile (Required):**
- fullName, email, phone, address, dateOfBirth, gender, skills, experience

**Employer Profile (Required):**
- companyName, contactEmail, contactPhone, businessAddress

**Employer Profile (Optional):**
- industry, companySize

### FirestoreJobRepository Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `getAllJobs` | `limit: Long` | `Flow<Result<List<JobListing>>>` | Gets all active jobs |
| `getJobById` | `jobId: String` | `Flow<Result<JobListing>>` | Gets single job |
| `getJobsByEmployer` | `employerId: String` | `Flow<Result<List<JobListing>>>` | Gets employer's jobs |
| `getJobsByCategory` | `category: String` | `Flow<Result<List<JobListing>>>` | Gets jobs by category |
| `getJobsNearLocation` | `lat: Double, lng: Double, radiusKm: Double` | `Flow<Result<List<JobListing>>>` | Gets nearby jobs |
| `createJob` | `jobData: Map<String, Any>` | `Flow<Result<String>>` | Creates new job |
| `updateJob` | `jobId: String, updates: Map<String, Any>` | `Flow<Result<Unit>>` | Updates job |
| `searchJobs` | `query: String` | `Flow<Result<List<JobListing>>>` | Searches jobs |

---

## DETAILED DATA MODELS

### User Model
```kotlin
data class User(
    val id: String = "",
    val email: String = "",
    val phone: String = "",
    val name: String = "",
    val role: UserRole = UserRole.WORKER,
    val profileImageUrl: String? = null,
    val profileCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class UserRole { WORKER, EMPLOYER }
```

### JobListing Model
```kotlin
data class JobListing(
    val id: String = "",
    val jobId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val payAmount: String = "",
    val payType: String = "DAILY",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val contactNumber: String = "",
    val companyName: String = "",
    val employerId: String = "",
    val employerName: String = "",
    val vacancies: Int = 1,
    val shiftTiming: String = "FLEXIBLE",
    val urgency: String = "FLEXIBLE",
    val isActive: Boolean = true,
    val isFilled: Boolean = false,
    val applicationCount: Long = 0,
    val postedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0
)
```

### JobApplication Model
```kotlin
data class JobApplication(
    val applicationId: String = UUID.randomUUID().toString(),
    val jobId: String = "",
    val workerId: String = "",
    val employerId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val jobLocation: String = "",
    val workerName: String = "",
    val workerEmail: String = "",
    val workerPhone: String? = null,
    val coverLetter: String = "",
    val skills: List<String> = emptyList(),
    val experience: String = "",
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val appliedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ApplicationStatus {
    PENDING, UNDER_REVIEW, ACCEPTED, REJECTED, WITHDRAWN
}
```

### NotificationData Model
```kotlin
data class NotificationData(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM_UPDATE,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val userId: String = "",
    val relatedJobId: String? = null,
    val relatedApplicationId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    JOB_APPLICATION, APPLICATION_STATUS, NEW_JOB, MESSAGE, SYSTEM_UPDATE, PROMOTION
}

enum class NotificationPriority { LOW, NORMAL, HIGH, URGENT }
```

---

## FIRESTORE COLLECTIONS SCHEMA

### users
```json
{
  "id": "string (document ID)",
  "email": "string",
  "phone": "string",
  "name": "string",
  "role": "WORKER | EMPLOYER",
  "profileCompleted": "boolean",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### jobs
```json
{
  "jobId": "string (document ID)",
  "title": "string",
  "description": "string",
  "category": "string",
  "payAmount": "string",
  "payType": "DAILY | HOURLY | MONTHLY | PER_TASK",
  "location": "string",
  "latitude": "number",
  "longitude": "number",
  "contactNumber": "string",
  "employerId": "string",
  "employerName": "string",
  "vacancies": "number",
  "isActive": "boolean",
  "isFilled": "boolean",
  "applicationCount": "number",
  "postedAt": "timestamp",
  "expiresAt": "timestamp"
}
```

### job_applications
```json
{
  "applicationId": "string (document ID)",
  "jobId": "string",
  "workerId": "string",
  "employerId": "string",
  "status": "PENDING | UNDER_REVIEW | ACCEPTED | REJECTED | WITHDRAWN",
  "coverLetter": "string",
  "skills": "array<string>",
  "appliedAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### phone_roles
```json
{
  "phoneNumber": "string (document ID, without +91)",
  "role": "WORKER | EMPLOYER"
}
```

---

## DETAILED DEVELOPMENT GUIDE

### Quick Start Checklist

**Before making changes:**
- [ ] Sync Gradle: `File → Sync Project with Gradle Files`
- [ ] Build successfully: `Ctrl+F9`
- [ ] Run app to confirm working state: `Shift+F10`

**After each change:**
- [ ] Build immediately to catch errors
- [ ] Fix errors before next change
- [ ] Test affected functionality
- [ ] Commit working code

### Adding New Screens (Step-by-Step)

**Step 1: Add Route**
```kotlin
// Routes.kt
object Routes {
    const val MY_NEW_SCREEN = "my_new_screen"
    const val MY_DETAIL_SCREEN = "my_detail/{itemId}"
    fun myDetailRoute(itemId: String) = "my_detail/$itemId"
}
```

**Step 2: Create Screen**
```kotlin
@Composable
fun MyNewScreen(
    navController: NavController,
    viewModel: MyNewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Screen content
}
```

**Step 3: Add to NavGraph**
```kotlin
composable(Routes.MY_NEW_SCREEN) {
    MyNewScreen(navController = navController)
}
```

**Step 4: Navigate**
```kotlin
navController.navigate(Routes.MY_NEW_SCREEN)
```

### Adding New ViewModels

```kotlin
@HiltViewModel
class MyNewViewModel @Inject constructor(
    private val firestoreService: FirestoreService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MyNewUiState())
    val uiState: StateFlow<MyNewUiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Load data...
        }
    }
}
```

### Navigation Patterns

```kotlin
// Basic navigation
navController.navigate(Routes.SOME_SCREEN)

// Navigate with pop behavior
navController.navigate(Routes.SOME_SCREEN) {
    popUpTo(Routes.HOME) { inclusive = false }
}

// Navigate and clear back stack
navController.navigate(Routes.LOGIN) {
    popUpTo(0) { inclusive = true }
}

// Go back
navController.popBackStack()
```

### Common UI Patterns

**Loading State:**
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    when {
        uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        uiState.hasError -> ErrorContent(message = uiState.error, onRetry = { viewModel.loadData() })
        else -> MainContent(data = uiState.data)
    }
}
```

**Pull to Refresh:**
```kotlin
PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh
) { content() }
```

### Firestore Operations

```kotlin
// Read document
val doc = firestore.collection(collection).document(docId).get().await()

// Write document
firestore.collection(collection).document(docId).set(data).await()

// Query documents
firestore.collection(collection)
    .whereEqualTo(field, value)
    .limit(limit)
    .get()
    .await()
```

### Real-time Listeners

```kotlin
fun observeCollection(collection: String): Flow<List<Map<String, Any>>> = callbackFlow {
    val listener = firestore.collection(collection)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val data = snapshot?.documents?.mapNotNull { it.data?.plus("id" to it.id) } ?: emptyList()
            trySend(data)
        }
    awaitClose { listener.remove() }
}
```

---

## TROUBLESHOOTING

### Common Errors and Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Unresolved reference` | Missing import | Press `Alt+Enter` to auto-import |
| `Cannot create instance of ViewModel` | Missing Hilt annotations | Add `@HiltViewModel` and `@Inject constructor` |
| `No destination with route X` | Route not in NavGraph | Add route to `Routes.kt` AND NavGraph |
| `Resource not found` | Missing drawable/string | Add resource to `res/` folder |
| `Null pointer exception` | Nullable field access | Use safe calls `?.` or provide defaults |
| `Type mismatch` | Wrong type from Firestore | Cast properly with `as? Type ?: default` |

### Build Issues

```bash
# Clean Build
./gradlew clean build

# Invalidate Caches
File → Invalidate Caches → Invalidate and Restart

# Sync Gradle
File → Sync Project with Gradle Files
```

### Debugging Tips

1. Use `Timber.d()` for debug logs
2. Check Logcat with filter: `package:com.example.dutype`
3. Use Android Studio debugger with breakpoints
4. Check Firebase Console for Firestore data
5. Use Layout Inspector for UI issues

---

## CODE STYLE GUIDELINES

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `JobApplicationService` |
| Functions | camelCase | `loadJobApplications()` |
| Variables | camelCase | `isLoading` |
| Constants | SCREAMING_SNAKE | `MAX_RETRY_COUNT` |
| Composables | PascalCase | `JobCard()` |
| State | camelCase with prefix | `_uiState`, `uiState` |

### File Organization

```kotlin
// 1. Package declaration
package com.example.dutype.viewmodels

// 2. Imports (grouped by type)
import androidx.lifecycle.*
import com.example.dutype.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// 3. Data classes / Enums
data class MyUiState(...)

// 4. Main class
@HiltViewModel
class MyViewModel @Inject constructor(...) : ViewModel() { }
```

### Compose Best Practices

1. Keep composables small and focused
2. Hoist state to parent composables
3. Use `remember` for expensive calculations
4. Use `LaunchedEffect` for side effects
5. Use `derivedStateOf` for computed values
6. Avoid passing ViewModels deep into composable tree

---

## GIT WORKFLOW

### Before Starting Work
```bash
git status
git pull origin main
```

### After Each Working Change
```bash
git add .
git commit -m "feat: Add job filter functionality"
```

### Commit Message Format
```
type: Short description

Types: feat, fix, refactor, docs, style, test, chore
```

---

## PERFORMANCE TIPS

1. Use `LazyColumn` for lists, not `Column` with `forEach`
2. Use `key` parameter in `LazyColumn` items
3. Avoid unnecessary recompositions with `remember`
4. Use `Dispatchers.IO` for network/database operations
5. Implement pagination for large datasets
6. Use Coil's `AsyncImage` for image loading
7. Profile with Android Studio Profiler

---

## SECURITY CHECKLIST

- [ ] Never hardcode API keys or secrets
- [ ] Validate all user inputs
- [ ] Use Firebase Security Rules
- [ ] Enable Firebase App Check
- [ ] Use HTTPS for all network calls
- [ ] Sanitize data before displaying
- [ ] Handle authentication state properly
- [ ] Log out users on security events

---

## ERROR CODES

| Code | Description | Resolution |
|------|-------------|------------|
| `AUTH_001` | User not authenticated | Redirect to login |
| `AUTH_002` | Phone already registered with different role | Use different phone |
| `PROFILE_001` | Profile incomplete | Redirect to profile setup |
| `JOB_001` | Job not found | Show error, refresh list |
| `JOB_002` | Cannot edit after 48 hours | Show restriction message |
| `APP_001` | Already applied to this job | Show existing application |
| `APP_002` | Cannot withdraw accepted application | Disable withdraw button |
| `LOCATION_001` | Location permission denied | Show permission dialog |
| `NETWORK_001` | No internet connection | Show offline message |

---

## TRUST SCORE ALGORITHM (Reference)

```
Trust Score = 
  25% × Punctuality +
  20% × Job Completion Rate +
  20% × Rating Average +
  15% × Payment History +
  10% × Dispute-Free +
  10% × Consistency

Example Worker (Ramesh):
  - Completed 25 jobs (98% completion) → 19.6%
  - Average rating: 4.8/5 → 19.2%
  - On-time 95% of time → 23.75%
  - No non-payment issues → 15%
  - No disputes → 10%
  - Works every day × 30 days → 10%
  ──────────────────
  Total Trust Score: 97.55 → 97/100 🟢
```

### Starting Trust Points (New Users)
- Phone verified: +5 points
- Profile 50% complete: +10 points
- LiveFace verified: +15 points
- **Total starting trust: 30 points**

---

## HAVERSINE DISTANCE FORMULA

Used for calculating distance between worker and job location:

```kotlin
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // Earth's radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + 
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
            sin(dLon / 2).pow(2)
    val c = 2 * asin(sqrt(a))
    return R * c // Distance in km
}
```

**Example:**
- Worker at (17.389, 78.456)
- Job at (17.392, 78.460)
- Distance = 0.45 km

---

**Document Version:** 2.0  
**Last Updated:** December 24, 2025  
**Total Pages:** Comprehensive Master Document
