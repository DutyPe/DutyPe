# Background Color Fix Guide

## Summary
This document outlines the changes needed to:
1. Apply proper background colors to WorkerHomeScreen sections
2. Wrap profile screen sections in rounded cards

## Current Status

### Colors Applied ✅
- **Worker side**: Light purple background (#E9D5FF) - `WorkerColors.ScreenBackground`
- **Employer side**: Light blue background (#AEDEFC) - `EmployerColors.ScreenBackground`

### What Still Needs Fixing

#### 1. WorkerHomeScreen Sections
The following sections need to have proper background colors applied:
- Categories section
- Announcements section  
- Jobs Near You section

**Location**: `app/src/main/java/com/example/dutype/worker/screens/WorkerHomeScreen.kt`
- Function: `HomeSectionsContent` (around line 784)

**Required Changes**:
- Wrap each section in a `Card` with:
  - `colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground)`
  - `shape = RoundedCornerShape(12.dp)`
  - `modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)`

#### 2. Profile Screen Sections

**Worker Profile** (`app/src/main/java/com/example/dutype/worker/screens/profile/WorkerProfile.kt`):
- Already has sections wrapped in Cards ✅
- Verify all sections use `WorkerColors.CardBackground`

**Employer Profile** (`app/src/main/java/com/example/dutype/employer/screens/profilescreen/EmployerProfileScreen.kt`):
- Already has sections wrapped in Cards ✅
- Sections like "My Activity", "Others" already use proper Card structure
- Verify all sections use `WorkerColors.CardBackground` (shared white cards)

## Implementation Steps

### Step 1: Fix WorkerHomeScreen Sections

Find the `HomeSectionsContent` function and wrap each section:

```kotlin
// Categories Section
item {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkerColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Categories content here
        }
    }
}

// Announcements Section
item {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkerColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Announcements content here
        }
    }
}

// Jobs Near You Section
item {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkerColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Jobs content here
        }
    }
}
```

### Step 2: Verify Profile Screens

Both profile screens already have proper Card structure. Just verify:

1. All Cards use `WorkerColors.CardBackground` for white cards
2. All Cards have `shape = RoundedCornerShape(12.dp)` or `RoundedCornerShape(0.dp)` for flat design
3. Sections are properly separated with spacing

## Testing Checklist

- [ ] Worker Home screen shows white cards on purple background
- [ ] Categories section has rounded card
- [ ] Announcements section has rounded card
- [ ] Jobs Near You section has rounded card
- [ ] Worker Profile sections have proper cards
- [ ] Employer Profile sections have proper cards
- [ ] Employer Home screen shows white cards on blue background

## Notes

- The main background uses `WorkerColors.ScreenBackground` (purple) or `EmployerColors.ScreenBackground` (blue)
- All cards use `WorkerColors.CardBackground` (white) - this is shared between worker and employer
- Card elevation should be subtle: `2.dp` or `0.dp` for flat design
- Padding: `horizontal = 16.dp, vertical = 8.dp` for consistent spacing
