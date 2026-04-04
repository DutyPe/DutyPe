# Empty State & Navigation Best Practices Guide

**Last Updated**: January 2026  
**Version**: 2.3.0  
**Audience**: DutyPe Frontend Engineers  

---

## Overview

This guide establishes standardized patterns for:
- **Empty State UI**: When a list/feed has no items
- **Search Results UI**: When no results match the query
- **Navigation from Empty States**: Best practices for driving user action
- **Consistency**: Unified UX across all screens

---

## Quick Reference

| Scenario | Component | Best Practice |
|----------|-----------|---|
| No items in list | `EmptyListState` | Show icon, title, CTA to browse/create |
| No search results | `EmptySearchState` | Show query, suggest clearing search |
| No jobs near location | `EmptyLocationState` | Suggest nearby cities, change location button |
| No content created yet | `EmptyActionState` | Call user to create (post job, add profile) |
| No saved items | `EmptySavedItemsState` | Show benefits, navigate to browse |

---

## Usage Examples

### 1. Basic Empty List

**Before (Anti-pattern)**:
```kotlin
when {
    data.isEmpty() -> {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Work, contentDescription = null)
                Text("No jobs")
                Button(onClick = { navController.navigate(Routes.WORKER_HOME_TAB) }) {
                    Text("Browse")
                }
            }
        }
    }
}
```

**After (Pattern)**:
```kotlin
when {
    data.isEmpty() -> {
        EmptyListState(
            icon = Icons.Default.Work,
            title = "No applications yet",
            subtitle = "Start exploring and apply to jobs",
            actionButton = EmptyStateAction(
                label = "Browse Jobs",
                onClick = { onNavigateHome() }  // Callback, not direct nav
            )
        )
    }
}
```

---

### 2. Search Results Empty State

**Before**:
```kotlin
if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
    Text("No results for '$searchQuery'")
}
```

**After**:
```kotlin
if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
    EmptySearchState(
        searchQuery = searchQuery,
        onClearSearch = { clearSearch() }
    )
}
```

---

### 3. Location-Based Empty State (AllJobsScreen Pattern)

**Use Case**: When user searches for jobs but none are near their current location.

```kotlin
if (jobs.isEmpty() && !searchQuery.isNotEmpty()) {
    EmptyLocationState(
        categoryFilter = selectedCategory,
        suggestedCities = listOf("Delhi", "Bangalore", "Mumbai"),
        onChangeLocation = { navigateToManualLocation() },
        onCitySuggestionClick = { city -> searchInCity(city) }
    )
}
```

**Why it's good**:
- Context-aware: Shows relevant action (change location, not "go home")
- Smart suggestions: Suggests nearby cities
- Clear CTA: "Change Location" button
- Humor: Keeps users engaged when blank

---

### 4. Action-Based Empty State (Employer Empty Posted Jobs)

**Use Case**: Employer hasn't posted any jobs yet.

```kotlin
if (postedJobs.isEmpty()) {
    EmptyActionState(
        icon = Icons.Default.Work,
        title = "No jobs posted yet",
        subtitle = "Start building your team",
        actionLabel = "Post Your First Job",
        onAction = { navigateToPostJob() }
    )
}
```

---

### 5. Saved Items Empty State

**Use Case**: Worker has no saved jobs.

```kotlin
if (savedJobs.isEmpty()) {
    EmptySavedItemsState(
        itemType = "jobs",
        onBrowse = { navigateToHome() }
    )
}
```

---

## Navigation Best Practices

### ❌ ANTI-PATTERNS (Don't Do This)

```kotlin
// 1. Direct navigation from component (tightly coupled)
EmptyListState(
    onAction = {
        navController.navigate(Routes.HOME)
    }
)

// 2. Creating callback hells (too many params)
EmptyListState(
    onAction = { callback1() },
    onSecondaryAction = { callback2() },
    onTertiaryAction = { callback3() }
)

// 3. No error handling
navController.navigate(Routes.SOME_ROUTE)  // What if it fails?

// 4. Wrong navigation target
// From MyJobs → navigating to CommonNavGraph route instead of local tab
navController.navigate(Routes.WORKER_HOME)  // Wrong! Use WORKER_HOME_TAB

// 5. Routing around the navigation hierarchy
// If you're on a child screen, use local navController
// Don't always jump back to root
```

---

### ✅ CORRECT PATTERNS

```kotlin
// 1. Use callbacks (decouple component from navigation)
EmptyListState(
    actionButton = EmptyStateAction(
        label = "Browse Jobs",
        onClick = onNavigateHome  // Simple callback
    )
)

// 2. Parent screen handles navigation
@Composable
fun MyJobsScreen(navController: NavHostController) {
    when {
        applications.isEmpty() -> {
            EmptyListState(
                actionButton = EmptyStateAction(
                    label = "Browse Jobs",
                    onClick = {
                        // Navigate within worker nav graph
                        navController.navigate(Routes.WORKER_HOME_TAB) {
                            popUpTo(Routes.WORKER_HOME_TAB) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            )
        }
    }
}

// 3. Context-aware navigation (AllJobsScreen pattern)
EmptyLocationState(
    onChangeLocation = {
        // If no location found AND we need location selection
        navController.navigate(Routes.MANUAL_LOCATION_ROUTE)
    }
)

// 4. Callback with error handling
EmptyListState(
    actionButton = EmptyStateAction(
        label = "Browse",
        onClick = {
            runCatching {
                navController.navigate(Routes.WORKER_HOME_TAB) {
                    popUpTo(Routes.WORKER_HOME_TAB) { inclusive = false }
                }
            }.onFailure { error ->
                Timber.e(error, "Navigation failed")
                // Show error snackbar or fallback
            }
        }
    )
)
```

---

## Navigation Targets Reference

### For Worker Tab Navigation (From Any Tab)

**Use**: `Routes.WORKER_HOME_TAB = "home"`  
**NOT**: `Routes.WORKER_HOME` (different route)  
**Context**: MyJobsScreen, SavedJobsList, WorkerProfileScreen

```kotlin
// ✅ CORRECT
navController.navigate(Routes.WORKER_HOME_TAB) {
    popUpTo(Routes.WORKER_HOME_TAB) { inclusive = false }
    launchSingleTop = true
}

// ❌ WRONG
navController.navigate(Routes.WORKER_HOME)  // Doesn't exist in WorkerNavGraph
```

**Worker Bottom Tabs**:
- `Home` → `WorkerBottomRoutes.HOME` or `Routes.WORKER_HOME_TAB = "home"`
- `My Jobs` → `WorkerBottomRoutes.MY_JOBS` or `Routes.WORKER_MY_JOBS = "myjobs"`
- `Profile` → `WorkerBottomRoutes.PROFILE` or `Routes.WORKER_PROFILE = "profile"`

### For Special Cases

**Location Selection**: `Routes.MANUAL_LOCATION_ROUTE`  
**Use When**: User needs to change their current location  
**Example**: AllJobsScreen empty state "Change Location" button

```kotlin
navController.navigate(Routes.MANUAL_LOCATION_ROUTE)
```

---

## Component Selection Decision Tree

```
Empty State Needed?
    ├─ User has no items yet AND no search active?
    │  └─ Use EmptyListState (or EmptySavedItemsState if saved items)
    │
    ├─ User searched but no results?
    │  └─ Use EmptySearchState
    │
    ├─ Location-based (no nearby items)?
    │  └─ Use EmptyLocationState with suggested cities
    │
    ├─ User needs to CREATE something to get started?
    │  └─ Use EmptyActionState (e.g., "Post Job")
    │
    └─ Custom empty state needed?
       └─ Compose from EmptyStateIcon + EmptyStateText + EmptyStateButtons
```

---

## Migration Checklist

### MyJobsScreen (Applied Jobs Tab)
- [ ] Replace `EmptyAppliedJobsState` with `EmptyListState`
- [ ] Verify navigation to `WORKER_HOME_TAB` in callback
- [ ] Replace `EmptySearchResults` with `EmptySearchState`
- [ ] Test empty → populated → empty transitions

### SavedJobsList (Saved Jobs Tab)
- [ ] Replace `EmptySavedJobsState` with `EmptySavedItemsState`
- [ ] Replace `EmptySearchResultsForSavedJobs` with `EmptySearchState`
- [ ] Update callbacks to use `WORKER_HOME_TAB`
- [ ] Test pagination and dynamic loading

### AllJobsScreen
- [ ] Keep `EmptyLocationState` logic as-is (already correct pattern)
- [ ] Consider extracting to use `EmptyLocationState` component
- [ ] Add suggested cities chips
- [ ] Test "Change Location" navigation

### EarningsDashboardScreen
- [ ] Extract local empty state to use `EmptyActionState`
- [ ] Add meaningful CTA (e.g., "Browse Jobs")

### EmployerAddressManagementScreen
- [ ] Extract local empty state to use `EmptyListState`
- [ ] Add "Add Address" action button

### PostedJobsScreen
- [ ] Keep `EmptyActionState` pattern (already good)
- [ ] Consider updating to use `EmptyActionState` component directly

---

## Testing Empty States

### Manual Testing Checklist

1. **List Empty State**
   - [ ] Displays when list is empty
   - [ ] Shows correct icon/title/subtitle
   - [ ] CTA button navigates correctly
   - [ ] Back button/gesture returns to previous screen

2. **Search Empty State**
   - [ ] Appears when search has no results
   - [ ] Shows the search query
   - [ ] Clear button clears search
   - [ ] Returns to list after clearing

3. **Location Empty State**
   - [ ] Shows when no items in current location
   - [ ] Suggested cities clickable
   - [ ] "Change Location" button works
   - [ ] City selection filters correctly

4. **Action Empty State**
   - [ ] Primary CTA button functions
   - [ ] Icon/text are visible
   - [ ] Styling is consistent

5. **Navigation**
   - [ ] Navigation doesn't crash when called multiple times
   - [ ] Back stack is correct (back button works)
   - [ ] DeepLinks to screens with empty states work

### Automated Testing Example

```kotlin
@Test
fun testEmptyListStateNavigation() {
    composeTestRule.setContent {
        EmptyListState(
            icon = Icons.Default.Work,
            title = "No items",
            subtitle = "Try something",
            actionButton = EmptyStateAction(
                label = "Action",
                onClick = { /* tracking */ }
            )
        )
    }
    
    // Verify UI elements exist
    composeTestRule.onNodeWithText("No items").assertExists()
    composeTestRule.onNodeWithText("Action").assertExists()
    
    // Click button and verify callback
    composeTestRule.onNodeWithText("Action").performClick()
    // Assert callback was called
}
```

---

## Troubleshooting

### Problem: Navigation fails silently
**Cause**: Wrong route name or navigation graph mismatch  
**Solution**: Always use `runCatching` and log errors

```kotlin
runCatching {
    navController.navigate(Routes.WORKER_HOME_TAB)
}.onFailure { error ->
    Timber.e(error, "Navigation failed to WORKER_HOME_TAB")
}
```

---

### Problem: Empty state appears during loading
**Cause**: Data loading state not checked properly  
**Solution**: Check `isLoading` flag before showing empty state

```kotlin
// ❌ Wrong
when {
    data.isEmpty() -> EmptyState()
}

// ✅ Correct
when {
    isLoading -> LoadingState()
    data.isEmpty() -> EmptyState()
    else -> ListContent()
}
```

---

### Problem: Empty state navigation takes too long
**Cause**: Navigation happening on wrong thread or blocked  
**Solution**: Ensure navigation happens in coroutine scope

```kotlin
// ✅ Good
EmptyListState(
    actionButton = EmptyStateAction(
        label = "Browse",
        onClick = {
            scope.launch {
                navController.navigate(Routes.WORKER_HOME_TAB)
            }
        }
    )
)
```

---

## Related Documentation

- [Navigation Architecture](../navigation/README.md)
- [Worker Screen Hierarchy](../worker/SCREENS.md)
- [UI Theme & Typography](../ui/THEME.md)
- [Loading States & Skeletons](./LoadingStates.md)

---

## Performance Considerations

### Recomposition
Empty state components are lightweight and won't cause excessive recomposition:
```kotlin
// ✅ No excessive recomposition
@Composable
fun MyJobsScreen() {
    val applications by viewModel.applications.collectAsStateWithLifecycle()
    
    when {
        applications.isEmpty() -> EmptyListState(...)  // Simple comparison
        else -> LazyColumn { ... }
    }
}
```

### Memory
Empty states use minimal memory:
- No lazy loading needed (small UI)
- Icons are vector drawables (small size)
- Text is immutable

### LazyColumn + Empty States
Use `ScrollAwareLazyColumn` for proper scroll handling:
```kotlin
ScrollAwareLazyColumn() {
    items(filteredJobs) { job ->
        JobCard(job)
    }
}

// Outside the LazyColumn for proper rendering:
if (filteredJobs.isEmpty()) {
    EmptyListState()
}
```

---

## FAQ

**Q: Should empty state components handle navigation?**  
A: No. Use callbacks. This keeps components reusable and testable.

**Q: What if I need a truly custom empty state?**  
A: Use `EmptyStateIcon`, `EmptyStateText`, `EmptyStateButtons` composables as building blocks.

**Q: Can I use these in employer flow?**  
A: Yes! The components are role-agnostic. Only the actions/callbacks differ.

**Q: What about error states vs empty states?**  
A: Use separate components. Error states have retry buttons; empty states have browse/create CTAs.

**Q: How do I test empty state navigation?**  
A: Mock the callback and verify it's called. Parent screen handles actual navigation.

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 2.3.0 | Jan 2026 | Initial centralized empty state components |
| 2.2.0 | Dec 2025 | Individual screen-level empty states |

---

**Questions?** Reach out to the DutyPe frontend team or open an issue in the project tracker.
