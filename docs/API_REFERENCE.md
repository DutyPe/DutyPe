# DutyPe API Reference Documentation

## Overview

This document provides a complete reference for all services, repositories, ViewModels, and data models in the DutyPe application. Use this as a guide when integrating with existing code or extending functionality.

---

## Services

### FirestoreService

**Location:** `app/src/main/java/com/example/dutype/services/FirestoreService.kt`

**Description:** Core service for all Firestore database operations.

#### Methods

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

#### Usage Example
```kotlin
@Inject
lateinit var firestoreService: FirestoreService

// Create a job
val jobData = mapOf(
    "title" to "Cook",
    "payAmount" to "500",
    "location" to "Mumbai",
    "employerId" to currentUserId
)
val result = firestoreService.createJob(jobData)
result.fold(
    onSuccess = { jobId -> /* handle success */ },
    onFailure = { error -> /* handle error */ }
)
```

---

### JobApplicationService

**Location:** `app/src/main/java/com/example/dutype/services/JobApplicationService.kt`

**Description:** Manages job application workflow including submission, status updates, and notifications.

#### Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `submitApplication` | `application: JobApplication` | `Result<JobApplication>` | Submits new application |
| `getWorkerApplications` | `workerId: String` | `Flow<Result<List<JobApplication>>>` | Gets worker's applications |
| `getJobApplications` | `jobId: String` | `Flow<Result<List<JobApplication>>>` | Gets applications for a job |
| `getEmployerApplications` | `employerId: String` | `Flow<Result<List<JobApplication>>>` | Gets all employer's applications |
| `updateApplicationStatus` | `applicationId: String, status: ApplicationStatus, notes: String?` | `Result<Unit>` | Updates application status |
| `withdrawApplication` | `applicationId: String, workerId: String` | `Result<JobApplication>` | Withdraws application |
| `hasWorkerAppliedToJob` | `workerId: String, jobId: String` | `Result<Boolean>` | Checks if already applied |
| `getApplicationStats` | `userId: String` | `Result<Map<String, Int>>` | Gets application statistics |

#### Application Status Flow
```
PENDING → UNDER_REVIEW → ACCEPTED
                      → REJECTED
        → WITHDRAWN (from PENDING or UNDER_REVIEW)
```

#### Usage Example
```kotlin
@Inject
lateinit var jobApplicationService: JobApplicationService

// Submit application
val application = JobApplication(
    jobId = "job123",
    workerId = currentUserId,
    jobTitle = "Cook",
    companyName = "ABC Restaurant",
    // ... other fields
)
jobApplicationService.submitApplication(application).fold(
    onSuccess = { submitted -> /* navigate to success */ },
    onFailure = { error -> /* show error */ }
)

// Get worker's applications
jobApplicationService.getWorkerApplications(workerId).collect { result ->
    result.fold(
        onSuccess = { applications -> /* update UI */ },
        onFailure = { error -> /* handle error */ }
    )
}
```

---

### NotificationService

**Location:** `app/src/main/java/com/example/dutype/services/NotificationService.kt`

**Description:** Handles push notifications, local notifications, and notification storage.

#### Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `sendJobPostedNotification` | `jobTitle: String, employerId: String` | `Unit` | Notifies job posted |
| `sendApplicationReceivedNotification` | `jobTitle: String, applicantName: String, employerId: String` | `Unit` | Notifies new application |
| `sendApplicationStatusNotification` | `jobTitle: String, status: String, workerId: String` | `Unit` | Notifies status change |
| `sendJobPausedNotification` | `jobTitle: String, isPaused: Boolean, employerId: String` | `Unit` | Notifies job pause/resume |
| `getNotifications` | `userId: String` | `Flow<List<NotificationData>>` | Gets user notifications |
| `markAsRead` | `notificationId: String` | `Result<Unit>` | Marks notification read |
| `markAllAsRead` | `userId: String` | `Result<Unit>` | Marks all as read |

#### Notification Channels
```kotlin
object NotificationChannels {
    const val JOB_ALERTS = "job_alerts"           // High priority
    const val APPLICATION_UPDATES = "app_updates" // High priority
    const val MESSAGES = "messages"               // Default priority
    const val PROMOTIONS = "promotions"           // Low priority
}
```

---

### ProfileCompletionService

**Location:** `app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt`

**Description:** Manages user profile data, completion tracking, and profile image uploads.

#### Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `isProfileComplete` | `userId: String, role: String` | `Boolean` | Checks if profile complete |
| `saveUserInfo` | `email: String, name: String, role: String` | `Result<Unit>` | Saves basic user info |
| `saveWorkerProfileData` | `profileData: Map<String, Any>` | `Result<Unit>` | Saves worker profile |
| `saveEmployerProfileData` | `profileData: Map<String, Any>` | `Result<Unit>` | Saves employer profile |
| `getWorkerProfileData` | `userId: String` | `Result<Map<String, Any>>` | Gets worker profile |
| `getEmployerProfileData` | `userId: String` | `Result<Map<String, Any>>` | Gets employer profile |
| `uploadProfileImage` | `imageUri: Uri, userId: String, userRole: String` | `Result<String>` | Uploads profile image |
| `updateUserRole` | `role: String` | `Result<Unit>` | Updates user role |
| `checkPhoneExistsWithDifferentRole` | `phone: String, currentRole: String` | `Result<String?>` | Checks dual role |
| `savePhoneRole` | `phone: String, role: String` | `Result<Unit>` | Saves phone-role mapping |

#### Profile Completion Fields

**Worker Profile:**
- fullName (required)
- email (required for Google auth)
- phone (required)
- address (required)
- dateOfBirth (required)
- gender (required)
- skills (required)
- experience (required)

**Employer Profile:**
- companyName (required)
- contactEmail (required)
- contactPhone (required)
- businessAddress (required)
- industry (optional)
- companySize (optional)

---

### FCMTokenManager

**Location:** `app/src/main/java/com/example/dutype/services/FCMTokenManager.kt`

**Description:** Manages Firebase Cloud Messaging tokens for push notifications.

#### Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `getToken` | - | `String?` | Gets current FCM token |
| `saveToken` | `userId: String` | `Result<Unit>` | Saves token to Firestore |
| `deleteToken` | `userId: String` | `Result<Unit>` | Removes token on logout |
| `refreshToken` | - | `Result<String>` | Forces token refresh |

---

## Repositories

### FirestoreJobRepository

**Location:** `app/src/main/java/com/example/dutype/repositories/FirestoreJobRepository.kt`

**Description:** Repository for job-related data operations with caching and error handling.

#### Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `getAllJobs` | `limit: Long` | `Flow<Result<List<JobListing>>>` | Gets all active jobs |
| `getJobById` | `jobId: String` | `Flow<Result<JobListing>>` | Gets single job |
| `getJobsByEmployer` | `employerId: String` | `Flow<Result<List<JobListing>>>` | Gets employer's jobs |
| `getJobsByCategory` | `category: String` | `Flow<Result<List<JobListing>>>` | Gets jobs by category |
| `getJobsNearLocation` | `lat: Double, lng: Double, radiusKm: Double` | `Flow<Result<List<JobListing>>>` | Gets nearby jobs |
| `createJob` | `jobData: Map<String, Any>` | `Flow<Result<String>>` | Creates new job |
| `updateJob` | `jobId: String, updates: Map<String, Any>` | `Flow<Result<Unit>>` | Updates job |
| `deleteJob` | `jobId: String` | `Flow<Result<Unit>>` | Deletes job |
| `searchJobs` | `query: String` | `Flow<Result<List<JobListing>>>` | Searches jobs |

---

### FirestoreSavedJobRepository

**Location:** `app/src/main/java/com/example/dutype/repositories/FirestoreSavedJobRepository.kt`

**Description:** Repository for saved/bookmarked jobs functionality.

#### Methods

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `getSavedJobs` | `workerId: String` | `Flow<Result<List<JobListing>>>` | Gets saved jobs |
| `saveJob` | `workerId: String, jobId: String` | `Result<Unit>` | Saves a job |
| `unsaveJob` | `workerId: String, jobId: String` | `Result<Unit>` | Removes saved job |
| `isJobSaved` | `workerId: String, jobId: String` | `Result<Boolean>` | Checks if saved |

---

## ViewModels

### FirestoreJobViewModel

**Location:** `app/src/main/java/com/example/dutype/viewmodels/FirestoreJobViewModel.kt`

**Description:** ViewModel for worker's job discovery and listing.

#### State
```kotlin
data class FirestoreJobUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val selectedCategory: String? = null,
    val searchQuery: String = ""
)
```

#### Methods
| Method | Description |
|--------|-------------|
| `loadJobs()` | Loads all active jobs |
| `refreshJobs()` | Refreshes job list |
| `filterByCategory(category: String?)` | Filters by category |
| `searchJobs(query: String)` | Searches jobs |
| `clearError()` | Clears error state |

---

### FirestoreEmployerJobViewModel

**Location:** `app/src/main/java/com/example/dutype/viewmodels/FirestoreEmployerJobViewModel.kt`

**Description:** ViewModel for employer's job management.

#### State
```kotlin
data class FirestoreEmployerJobUiState(
    val myJobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val isCreatingJob: Boolean = false,
    val isUpdatingJob: Boolean = false,
    val isDeletingJob: Boolean = false
)
```

#### Methods
| Method | Parameters | Description |
|--------|------------|-------------|
| `loadMyJobs()` | - | Loads employer's jobs |
| `refreshMyJobs()` | - | Refreshes job list |
| `createJob(jobData, callback)` | `Map<String, Any>, (Boolean, String?) -> Unit` | Creates new job |
| `updateJob(jobId, updates, callback)` | `String, Map<String, Any>, (Boolean, String?) -> Unit` | Updates job |
| `deleteJob(jobId, callback)` | `String, (Boolean, String?) -> Unit` | Deletes job |
| `toggleJobStatus(jobId)` | `String` | Pauses/resumes job |
| `getJobById(jobId, callback)` | `String, (JobListing?) -> Unit` | Gets single job |

---

### JobApplicationViewModel

**Location:** `app/src/main/java/com/example/dutype/viewmodels/JobApplicationViewModel.kt`

**Description:** ViewModel for worker's job applications.

#### State
```kotlin
data class JobApplicationUiState(
    val applications: List<JobApplication> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val submissionSuccess: Boolean = false
)
```

#### Methods
| Method | Parameters | Description |
|--------|------------|-------------|
| `loadMyApplications()` | - | Loads worker's applications |
| `loadApplicationsByStatus(status)` | `ApplicationStatus` | Filters by status |
| `submitApplication(application)` | `JobApplication` | Submits application |
| `withdrawApplication(applicationId, callback)` | `String, (Boolean, String?) -> Unit` | Withdraws application |
| `hasAppliedToJob(jobId, callback)` | `String, (Boolean) -> Unit` | Checks if applied |
| `refreshApplications()` | - | Refreshes list |

---

### ProfileCompletionViewModel

**Location:** `app/src/main/java/com/example/dutype/viewmodels/ProfileCompletionViewModel.kt`

**Description:** ViewModel for profile setup and management.

#### Methods
| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `isProfileComplete(role)` | `UserRole` | `Boolean` | Checks completion |
| `saveUserInfo(email, name, role)` | `String, String, UserRole` | `Result<Unit>` | Saves user info |
| `saveWorkerProfileData(data)` | `Map<String, Any>` | `Result<Unit>` | Saves worker profile |
| `saveEmployerProfileData(data)` | `Map<String, Any>` | `Result<Unit>` | Saves employer profile |
| `getWorkerProfileData(userId)` | `String` | `Result<Map<String, Any>>` | Gets worker profile |
| `getEmployerProfileData(userId)` | `String` | `Result<Map<String, Any>>` | Gets employer profile |
| `uploadProfileImage(uri, userId, role)` | `Uri, String, String` | `Result<String>` | Uploads image |
| `updateUserRole(role)` | `UserRole` | `Unit` | Updates role |
| `getUserEmail()` | - | `String?` | Gets saved email |
| `getUserName()` | - | `String?` | Gets saved name |
| `getAuthMethod()` | - | `String?` | Gets auth method |
| `markProfileComplete(role)` | `UserRole` | `Unit` | Marks complete |
| `resetProfileSetupState()` | - | `Unit` | Resets state |

---

### SavedJobsViewModel

**Location:** `app/src/main/java/com/example/dutype/viewmodels/SavedJobsViewModel.kt`

**Description:** ViewModel for saved jobs functionality.

#### State
```kotlin
data class SavedJobsUiState(
    val savedJobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null
)
```

#### Methods
| Method | Parameters | Description |
|--------|------------|-------------|
| `loadSavedJobs()` | - | Loads saved jobs |
| `saveJob(jobId)` | `String` | Saves a job |
| `unsaveJob(jobId)` | `String` | Removes saved job |
| `isJobSaved(jobId)` | `String` | Returns `Boolean` |
| `toggleSaveJob(jobId)` | `String` | Toggles save state |

---

## Data Models

### User

**Location:** `app/src/main/java/com/example/dutype/models/User.kt`

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

enum class UserRole {
    WORKER,
    EMPLOYER
}
```

---

### JobListing

**Location:** `app/src/main/java/com/example/dutype/models/JobListing.kt`

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
    val expiresAt: Long = 0,
    val imageUrl: String? = null
)
```

---

### JobApplication

**Location:** `app/src/main/java/com/example/dutype/models/JobApplicationModels.kt`

```kotlin
data class JobApplication(
    val applicationId: String = UUID.randomUUID().toString(),
    val jobId: String = "",
    val workerId: String = "",
    val employerId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val jobLocation: String = "",
    val jobType: String = "",
    val payInfo: String = "",
    val workerName: String = "",
    val workerEmail: String = "",
    val workerPhone: String? = null,
    val workerLocation: String? = null,
    val workerProfileImageUrl: String? = null,
    val coverLetter: String = "",
    val skills: List<String> = emptyList(),
    val skillsText: String? = null,
    val experience: String = "",
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val appliedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewNotes: String? = null,
    val isFilled: Boolean = false,
    val isActive: Boolean = true
)

enum class ApplicationStatus {
    PENDING,
    UNDER_REVIEW,
    ACCEPTED,
    REJECTED,
    WITHDRAWN
}
```

---

### LocationData

**Location:** `app/src/main/java/com/example/dutype/models/LocationData.kt`

```kotlin
data class LocationData(
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postalCode: String? = null
)
```

---

### NotificationModels

**Location:** `app/src/main/java/com/example/dutype/models/NotificationModels.kt`

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
    val createdAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null
)

enum class NotificationType {
    JOB_APPLICATION,
    APPLICATION_STATUS,
    NEW_JOB,
    MESSAGE,
    SYSTEM_UPDATE,
    PROMOTION
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
```

---

## Firestore Collections Schema

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

## Error Codes

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
