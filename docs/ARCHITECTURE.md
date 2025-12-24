# DutyPe Architecture Documentation

## Overview

DutyPe is a hyper-local job marketplace Android application built with modern Android development practices. The app connects blue-collar workers with local employers in India, focusing on part-time, flexible work opportunities.

## Technology Stack

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

## Project Structure

```
app/src/main/java/com/example/dutype/
├── ads/                    # Advertisement management
│   ├── AdsManager.kt
│   └── InterstitialAdManager.kt
├── auth/                   # Authentication
│   ├── AuthManager.kt
│   ├── GoogleSignInManager.kt
│   └── EnhancedLoginScreen.kt
├── common/                 # Shared screens
│   └── chat/
│       ├── SelectRoleScreen.kt
│       ├── help/           # Help & Support screens
│       └── info/           # FAQ, Privacy, Terms
├── components/             # Reusable UI components
│   ├── CommonHeader.kt
│   ├── DutyPeSplashScreen.kt
│   ├── FeedbackBottomSheet.kt
│   ├── ProfessionalLogoutDialog.kt
│   ├── ProfileCompletionProgress.kt
│   ├── ReusableBottomBar.kt
│   ├── ScrollAwareLazyColumn.kt
│   └── ShimmerComponents.kt
├── data/                   # Data persistence
│   └── ApplicationFormDataStore.kt
├── di/                     # Dependency Injection
│   └── AppModule.kt
├── employer/               # Employer module
│   ├── components/
│   ├── helpers/
│   ├── models/
│   ├── screens/
│   └── viewmodels/
├── location/               # Location services
│   ├── LocationPreferences.kt
│   ├── LocationUtils.kt
│   └── PlacesLocationManager.kt
├── models/                 # Data models
│   ├── User.kt
│   ├── JobListing.kt
│   ├── JobApplicationModels.kt
│   ├── LocationData.kt
│   └── NotificationModels.kt
├── navigation/             # Navigation
│   ├── Routes.kt
│   ├── MainNavGraph.kt
│   └── employer/
│   └── workerNavGraph/
├── notifications/          # Notification system
│   ├── components/
│   ├── manager/
│   ├── models/
│   └── services/
├── onboarding/             # Onboarding flow
│   └── OnboardingScreen.kt
├── repositories/           # Data repositories
│   ├── AuthRepository.kt
│   ├── FirestoreJobRepository.kt
│   └── FirestoreSavedJobRepository.kt
├── services/               # Business logic services
│   ├── FirestoreService.kt
│   ├── JobApplicationService.kt
│   ├── NotificationService.kt
│   ├── ProfileCompletionService.kt
│   ├── DutyPeFirebaseMessagingService.kt
│   └── FCMTokenManager.kt
├── state/                  # State management
│   ├── ApplicationStateManager.kt
│   ├── ProfileSetupStateManager.kt
│   └── SavedJobsStateManager.kt
├── ui/                     # UI theme
│   ├── components/
│   └── theme/
├── utils/                  # Utilities
│   ├── ComposableUtils.kt
│   ├── CrashReportingHelper.kt
│   ├── FirestoreUtils.kt
│   ├── LocationService.kt
│   ├── NotificationPermissionManager.kt
│   └── ValidationUtils.kt
├── viewmodels/             # Shared ViewModels
│   ├── FirestoreJobViewModel.kt
│   ├── FirestoreEmployerJobViewModel.kt
│   ├── JobApplicationViewModel.kt
│   ├── ProfileCompletionViewModel.kt
│   └── SavedJobsViewModel.kt
├── worker/                 # Worker module
│   ├── components/
│   ├── helpers/
│   ├── models/
│   ├── screens/
│   └── viewmodels/
├── MainActivity.kt
└── DutyPeApplication.kt
```

## Architecture Layers

### 1. Presentation Layer (UI)

The UI layer uses Jetpack Compose with Material 3 design system.

**Key Components:**
- **Screens**: Composable functions representing full screens
- **Components**: Reusable UI elements (cards, buttons, dialogs)
- **Theme**: Colors, typography, shapes defined in `ui/theme/`

**State Management:**
```kotlin
// ViewModel exposes StateFlow
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Composable collects state
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

### 2. ViewModel Layer

ViewModels manage UI state and business logic orchestration.

**Pattern Used:**
```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: ExampleRepository,
    private val service: ExampleService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getData().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            data = data
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            }
        }
    }
}
```

### 3. Repository Layer

Repositories abstract data sources and provide clean APIs.

**Pattern Used:**
```kotlin
class ExampleRepository @Inject constructor(
    private val firestoreService: FirestoreService
) {
    fun getData(): Flow<Result<List<Data>>> = flow {
        try {
            val result = firestoreService.getCollection("data")
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
```

### 4. Service Layer

Services contain business logic and external API interactions.

**Key Services:**
- `FirestoreService`: CRUD operations for Firestore
- `JobApplicationService`: Application workflow management
- `NotificationService`: Push and local notifications
- `ProfileCompletionService`: Profile management

### 5. Data Layer

**Firebase Firestore Collections:**
```
├── users/                  # User accounts
├── worker_profiles/        # Worker profile data
├── employer_profiles/      # Employer profile data
├── jobs/                   # Job listings
├── job_applications/       # Job applications
├── saved_jobs/             # Saved jobs by workers
├── notifications/          # User notifications
├── phone_roles/            # Phone-to-role mapping
├── fcm_tokens/             # FCM tokens for push
└── app_feedback/           # User feedback
```

## Dependency Injection

Hilt is used for dependency injection throughout the app.

**AppModule.kt provides:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFirestoreService(): FirestoreService = FirestoreService()
    
    @Provides
    @Singleton
    fun provideJobApplicationService(
        firestoreService: FirestoreService
    ): JobApplicationService = JobApplicationService(firestoreService)
    
    // ... other providers
}
```

## Navigation Architecture

Navigation uses Jetpack Navigation Compose with nested graphs.

**Structure:**
```
MainNavGraph
├── Splash Screen
├── Onboarding
├── Role Selection
├── Login (Worker/Employer)
├── Profile Setup
├── Worker Home (nested graph)
│   ├── Home Tab
│   ├── My Jobs Tab
│   └── Profile Tab
└── Employer Home (nested graph)
    ├── Dashboard Tab
    ├── Post Job Tab
    └── Profile Tab
```

**Route Definition:**
```kotlin
object Routes {
    const val SPLASH = "splash"
    const val WORKER_HOME = "worker_home"
    const val EMPLOYER_HOME = "employer_home"
    
    // Parameterized routes
    const val JOB_DETAIL = "job_detail/{jobId}"
    fun jobDetailRoute(jobId: String) = "job_detail/$jobId"
}
```

## Data Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Screen    │────▶│  ViewModel  │────▶│ Repository  │
│ (Composable)│     │             │     │             │
└─────────────┘     └─────────────┘     └─────────────┘
       ▲                   │                   │
       │                   │                   ▼
       │            ┌──────┴──────┐     ┌─────────────┐
       │            │  StateFlow  │     │   Service   │
       │            └─────────────┘     │             │
       │                                └─────────────┘
       │                                       │
       └───────────────────────────────────────┘
                    UI State Updates
```

## Authentication Flow

```
┌─────────────┐
│ App Launch  │
└──────┬──────┘
       ▼
┌─────────────┐     Yes    ┌─────────────┐
│ User Logged │───────────▶│  Check Role │
│    In?      │            └──────┬──────┘
└──────┬──────┘                   │
       │ No                       ▼
       ▼                   ┌─────────────┐
┌─────────────┐            │ Navigate to │
│ Role Select │            │  Role Home  │
└──────┬──────┘            └─────────────┘
       ▼
┌─────────────┐
│   Login     │
│ Google/OTP  │
└──────┬──────┘
       ▼
┌─────────────┐     No     ┌─────────────┐
│  Profile    │───────────▶│   Profile   │
│ Complete?   │            │    Setup    │
└──────┬──────┘            └──────┬──────┘
       │ Yes                      │
       ▼                          ▼
┌─────────────┐            ┌─────────────┐
│  Role Home  │◀───────────│  Role Home  │
└─────────────┘            └─────────────┘
```

## Error Handling Strategy

**Layered Error Handling:**
1. **Service Layer**: Catches exceptions, returns Result<T>
2. **Repository Layer**: Transforms errors, adds context
3. **ViewModel Layer**: Updates UI state with error messages
4. **UI Layer**: Displays error states, retry options

```kotlin
// Service returns Result
suspend fun getData(): Result<Data> = try {
    Result.success(fetchData())
} catch (e: Exception) {
    Timber.e(e, "Failed to fetch data")
    Result.failure(e)
}

// ViewModel handles Result
result.fold(
    onSuccess = { /* update success state */ },
    onFailure = { /* update error state */ }
)
```

## Performance Considerations

1. **Lazy Loading**: LazyColumn/LazyRow for lists
2. **Image Caching**: Coil handles image caching
3. **State Hoisting**: Minimize recompositions
4. **Background Processing**: Coroutines with appropriate dispatchers
5. **Pagination**: Firestore query limits for large datasets

## Security

1. **Firebase Security Rules**: Role-based access control
2. **App Check**: Firebase App Check for API protection
3. **Data Validation**: Client and server-side validation
4. **Secure Storage**: Sensitive data in encrypted preferences
