# DutyPe Development Guide

## Overview

This guide provides step-by-step instructions for common development tasks in the DutyPe project. Follow these patterns to ensure consistent, error-free development.

---

## Quick Start Checklist

Before making any changes:
- [ ] Sync Gradle: `File → Sync Project with Gradle Files`
- [ ] Build successfully: `Ctrl+F9`
- [ ] Run app to confirm working state: `Shift+F10`

After each change:
- [ ] Build immediately to catch errors
- [ ] Fix errors before next change
- [ ] Test affected functionality
- [ ] Commit working code

---

## Adding New Screens

### Step 1: Add Route Constant

**File:** `app/src/main/java/com/example/dutype/navigation/Routes.kt`

```kotlin
object Routes {
    // Existing routes...
    
    // Add your new route
    const val MY_NEW_SCREEN = "my_new_screen"
    
    // For parameterized routes
    const val MY_DETAIL_SCREEN = "my_detail/{itemId}"
    fun myDetailRoute(itemId: String) = "my_detail/$itemId"
}
```

### Step 2: Create Screen Composable

**File:** Create in appropriate module folder

**Worker screens:** `app/src/main/java/com/example/dutype/worker/screens/`
**Employer screens:** `app/src/main/java/com/example/dutype/employer/screens/`
**Common screens:** `app/src/main/java/com/example/dutype/common/`

```kotlin
package com.example.dutype.worker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNewScreen(
    navController: NavController,
    viewModel: MyNewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My New Screen") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Screen content here
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Content loaded")
            }
        }
    }
}
```

### Step 3: Add to NavGraph

**Worker screens:** `app/src/main/java/com/example/dutype/navigation/workerNavGraph/WorkerNavGraph.kt`
**Employer screens:** `app/src/main/java/com/example/dutype/navigation/employer/EmployerMainScreen.kt`
**Common screens:** `app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt`

```kotlin
// Inside NavHost or navigation graph
composable(Routes.MY_NEW_SCREEN) {
    MyNewScreen(navController = navController)
}

// For parameterized routes
composable(
    route = Routes.MY_DETAIL_SCREEN,
    arguments = listOf(navArgument("itemId") { type = NavType.StringType })
) { backStackEntry ->
    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
    MyDetailScreen(
        navController = navController,
        itemId = itemId
    )
}
```

### Step 4: Navigate to Screen

```kotlin
// Simple navigation
navController.navigate(Routes.MY_NEW_SCREEN)

// With parameters
navController.navigate(Routes.myDetailRoute("item123"))

// With options
navController.navigate(Routes.MY_NEW_SCREEN) {
    popUpTo(Routes.WORKER_HOME) { inclusive = false }
    launchSingleTop = true
}
```

---

## Adding New ViewModels

### Step 1: Create ViewModel Class

**File:** `app/src/main/java/com/example/dutype/viewmodels/MyNewViewModel.kt`

```kotlin
package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.services.FirestoreService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// Define UI State
data class MyNewUiState(
    val items: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MyNewViewModel @Inject constructor(
    private val firestoreService: FirestoreService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MyNewUiState())
    val uiState: StateFlow<MyNewUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Fetch data
                val result = firestoreService.getAllJobs(50)
                result.fold(
                    onSuccess = { data ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            items = data.map { it["title"] as? String ?: "" }
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load data")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Exception loading data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(hasError = false, error = null)
    }
}
```

### Step 2: Verify Dependencies in AppModule

**File:** `app/src/main/java/com/example/dutype/di/AppModule.kt`

Ensure all injected services are provided:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFirestoreService(): FirestoreService = FirestoreService()
    
    // Add new service providers here if needed
}
```

### Step 3: Use in Composable

```kotlin
@Composable
fun MyNewScreen(
    navController: NavController,
    viewModel: MyNewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Use uiState in your UI
}
```

---

## Adding New Services

### Step 1: Create Service Class

**File:** `app/src/main/java/com/example/dutype/services/MyNewService.kt`

```kotlin
package com.example.dutype.services

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyNewService @Inject constructor(
    private val firestoreService: FirestoreService
) {
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun doSomething(param: String): Result<String> {
        return try {
            // Business logic here
            val result = firestore.collection("my_collection")
                .document(param)
                .get()
                .await()
            
            Result.success(result.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to do something")
            Result.failure(e)
        }
    }
    
    suspend fun createItem(data: Map<String, Any>): Result<String> {
        return try {
            val docRef = firestore.collection("my_collection")
                .add(data)
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create item")
            Result.failure(e)
        }
    }
}
```

### Step 2: Add Provider to AppModule

**File:** `app/src/main/java/com/example/dutype/di/AppModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // Existing providers...
    
    @Provides
    @Singleton
    fun provideMyNewService(
        firestoreService: FirestoreService
    ): MyNewService = MyNewService(firestoreService)
}
```

### Step 3: Inject in ViewModel

```kotlin
@HiltViewModel
class SomeViewModel @Inject constructor(
    private val myNewService: MyNewService
) : ViewModel() {
    // Use myNewService
}
```

---

## Adding New Data Models

### Step 1: Create Model Class

**File:** `app/src/main/java/com/example/dutype/models/MyNewModel.kt`

```kotlin
package com.example.dutype.models

data class MyNewModel(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Firestore requires no-arg constructor for deserialization
    // Default values handle this
    
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
    
    companion object {
        fun fromMap(map: Map<String, Any>, id: String = ""): MyNewModel {
            return MyNewModel(
                id = id.ifEmpty { map["id"] as? String ?: "" },
                title = map["title"] as? String ?: "",
                description = map["description"] as? String ?: "",
                isActive = map["isActive"] as? Boolean ?: true,
                createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = map["updatedAt"] as? Long ?: System.currentTimeMillis()
            )
        }
    }
}
```

### Step 2: Add Firestore Rules (if new collection)

**File:** `firestore.rules`

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Existing rules...
    
    // Add rules for new collection
    match /my_collection/{docId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null && 
                       request.auth.uid == resource.data.userId;
      allow delete: if request.auth != null && 
                       request.auth.uid == resource.data.userId;
    }
  }
}
```

---

## Working with Navigation

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

// Navigate with single top
navController.navigate(Routes.SOME_SCREEN) {
    launchSingleTop = true
}

// Go back
navController.popBackStack()

// Go back to specific destination
navController.popBackStack(Routes.HOME, inclusive = false)
```

### Passing Data Between Screens

```kotlin
// Define route with parameters
const val DETAIL_SCREEN = "detail/{itemId}/{itemName}"
fun detailRoute(itemId: String, itemName: String) = "detail/$itemId/$itemName"

// In NavGraph
composable(
    route = Routes.DETAIL_SCREEN,
    arguments = listOf(
        navArgument("itemId") { type = NavType.StringType },
        navArgument("itemName") { type = NavType.StringType }
    )
) { backStackEntry ->
    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
    val itemName = backStackEntry.arguments?.getString("itemName") ?: ""
    DetailScreen(itemId = itemId, itemName = itemName)
}

// Navigate with parameters
navController.navigate(Routes.detailRoute("123", "My Item"))
```

---

## Common UI Patterns

### Loading State

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.hasError -> {
                ErrorContent(
                    message = uiState.error ?: "Something went wrong",
                    onRetry = { viewModel.loadData() }
                )
            }
            else -> {
                // Main content
                MainContent(data = uiState.data)
            }
        }
    }
}
```

### Pull to Refresh

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableList(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullRefreshState
    ) {
        content()
    }
}
```

### Bottom Sheet

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreenWithBottomSheet() {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    // Main content
    Button(onClick = { showBottomSheet = true }) {
        Text("Show Options")
    }
    
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bottom Sheet Content")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showBottomSheet = false }) {
                    Text("Close")
                }
            }
        }
    }
}
```

### Dialog

```kotlin
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

---

## Working with Firebase

### Firestore Operations

```kotlin
// Read document
suspend fun getDocument(collection: String, docId: String): Result<Map<String, Any>> {
    return try {
        val doc = firestore.collection(collection).document(docId).get().await()
        if (doc.exists()) {
            Result.success(doc.data ?: emptyMap())
        } else {
            Result.failure(Exception("Document not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Write document
suspend fun setDocument(collection: String, docId: String, data: Map<String, Any>): Result<Unit> {
    return try {
        firestore.collection(collection).document(docId).set(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Query documents
suspend fun queryDocuments(
    collection: String,
    field: String,
    value: Any,
    limit: Long = 50
): Result<List<Map<String, Any>>> {
    return try {
        val snapshot = firestore.collection(collection)
            .whereEqualTo(field, value)
            .limit(limit)
            .get()
            .await()
        
        val results = snapshot.documents.mapNotNull { doc ->
            doc.data?.plus("id" to doc.id)
        }
        Result.success(results)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Real-time Listeners

```kotlin
fun observeCollection(collection: String): Flow<List<Map<String, Any>>> = callbackFlow {
    val listener = firestore.collection(collection)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val data = snapshot?.documents?.mapNotNull { doc ->
                doc.data?.plus("id" to doc.id)
            } ?: emptyList()
            
            trySend(data)
        }
    
    awaitClose { listener.remove() }
}
```

---

## Testing Patterns

### ViewModel Testing

```kotlin
@Test
fun `loadData should update state with items`() = runTest {
    // Given
    val mockService = mockk<FirestoreService>()
    coEvery { mockService.getAllJobs(any()) } returns Result.success(listOf(
        mapOf("title" to "Job 1"),
        mapOf("title" to "Job 2")
    ))
    
    val viewModel = MyNewViewModel(mockService)
    
    // When
    viewModel.loadData()
    advanceUntilIdle()
    
    // Then
    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(2, state.items.size)
}
```

### Composable Testing

```kotlin
@Test
fun `screen shows loading indicator when loading`() {
    composeTestRule.setContent {
        MyNewScreen(
            uiState = MyNewUiState(isLoading = true),
            onAction = {}
        )
    }
    
    composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
}
```

---

## Troubleshooting

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

**Clean Build:**
```
./gradlew clean build
```

**Invalidate Caches:**
`File → Invalidate Caches → Invalidate and Restart`

**Sync Gradle:**
`File → Sync Project with Gradle Files`

### Debugging Tips

1. Use `Timber.d()` for debug logs
2. Check Logcat with filter: `package:com.example.dutype`
3. Use Android Studio debugger with breakpoints
4. Check Firebase Console for Firestore data
5. Use Layout Inspector for UI issues

---

## Code Style Guidelines

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
import com.example.dutype.services.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

// 3. Data classes / Enums
data class MyUiState(...)

// 4. Main class
@HiltViewModel
class MyViewModel @Inject constructor(...) : ViewModel() {
    // Properties
    // Init block
    // Public functions
    // Private functions 
}
```

### Compose Best Practices

1. Keep composables small and focused
2. Hoist state to parent composables
3. Use `remember` for expensive calculations
4. Use `LaunchedEffect` for side effects
5. Use `derivedStateOf` for computed values
6. Avoid passing ViewModels deep into composable tree

---

## Git Workflow

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

### If Something Breaks
```bash
# Undo all uncommitted changes
git checkout -- .

# Go back one commit
git reset --hard HEAD~1
```

### Commit Message Format
```
type: Short description

Types:
- feat: New feature
- fix: Bug fix
- refactor: Code refactoring
- docs: Documentation
- style: Formatting changes
- test: Adding tests
- chore: Maintenance
```

---

## Performance Tips

1. Use `LazyColumn` for lists, not `Column` with `forEach`
2. Use `key` parameter in `LazyColumn` items
3. Avoid unnecessary recompositions with `remember`
4. Use `Dispatchers.IO` for network/database operations
5. Implement pagination for large datasets
6. Use Coil's `AsyncImage` for image loading
7. Profile with Android Studio Profiler

---

## Security Checklist

- [ ] Never hardcode API keys or secrets
- [ ] Validate all user inputs
- [ ] Use Firebase Security Rules
- [ ] Enable Firebase App Check
- [ ] Use HTTPS for all network calls
- [ ] Sanitize data before displaying
- [ ] Handle authentication state properly
- [ ] Log out users on security events
