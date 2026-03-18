# Phase 2 Geohash Filtering - Quick Testing Guide

## 1. Build & Deploy

### Option A: Android Studio
```
1. Menu → Build → Clean Project
2. Menu → Build → Rebuild Project
3. Wait for build to complete (should show no errors)
4. Menu → Run → Run 'app' (or press Shift+F10)
5. Select your device/emulator
```

### Option B: Terminal
```bash
cd c:\Users\vamsi\StudioProjects\DutyPe
.\gradlew.bat clean build
.\gradlew.bat installDebug
```

## 2. Enable Location Services

### On Physical Device:
1. Settings → Location → Turn ON
2. Location mode → High Accuracy (uses GPS + WiFi)
3. Grant app permission when prompted

### On Emulator:
1. Extended Controls (⋯) → Location
2. Enter coordinates manually or use preset cities
3. Send sample location

## 3. Test Scenarios

### Test 1: Fresh App Start
**Expected**: 
- App loads location from device
- 3 nearby jobs shown on home screen
- Each job displays distance ✅

**Steps**:
1. Open app (allow location permission)
2. Go to Home screen (WorkerHomeViewModel)
3. Check logs for: `"✅ Geohash range query calculated"`
4. Visual: Jobs should show "X km away"

**Debug Logs to Look For**:
```
📂 ========== FIRESTORE QUERY START ==========
📂 getAllJobsSummary called:
📂   - limit: 3
📂   - userLocation: (19.0760, 72.8777)
📂 ✅ Valid user location detected: (19.0760, 72.8777)
📂 ✅ Geohash range query calculated:
📂   - Start: ttg9p
📂   - End: ttg9p~
📂   - Covering 50km radius
📂 ✅ Geohash filter applied
```

### Test 2: All Jobs Screen
**Expected**:
- Click "All Jobs"
- Jobs load with distances
- Scroll infinite
- All jobs showing reasonable distances (<50-100 km)

**Steps**:
1. Go to All Jobs screen (AllJobsViewModel)
2. Check if jobs load
3. Look for distance values (should be "X km away")
4. Enable Timber logging to see geohash queries
5. Scroll down 5-10 jobs

**Success Indicators**:
- ✓ Less than 50ms delay between scroll and new jobs
- ✓ All jobs show reasonable distances
- ✓ No "100+ km away" jobs in first page

### Test 3: Search by Category
**Expected**:
- Select category (e.g., "Delivery")
- Jobs in that category within 50km appear
- Category filter + geohash filter both applied

**Steps**:
1. Go to Categories screen (CategoriesViewModel)
2. Tap on "Delivery" or other category
3. Jobs load
4. Verify distance formatting

**Debug Log**:
```
📂 Display name: 'Delivery'
📂 Firestore query: 'DELIVERY'
📂 ✅ Category filter APPLIED: category == 'DELIVERY'
📂 ✅ Geohash filter applied
```

### Test 4: Location Change
**Expected**:
- Move to different city
- Refresh jobs
- Different jobs appear (now nearby to new location)

**Steps**:
1. (On emulator) Change location to different city
   - Extended Controls → Location → Select different preset
   - OR manually enter: New York (40.7128, -74.0060)
2. Go back to Home or All Jobs
3. Pull-to-refresh (trigger loadMore)
4. Check if jobs changed

**Debug**:
```
📂 userLocation: (40.7128, -74.0060)  // Different coordinates
📂 Start: [new geohash]               // Different geohash
📂 End: [new geohash]~
```

## 4. Performance Monitoring

### Measure Query Times:
1. Enable Timber logging (check logs in Android Studio)
2. Open Developer Tools during app usage
3. Look for timing info:

```
📂 Query completed in 240ms           // Firestore query time
📦 Firestore returned: 45 documents   // Jobs count
📦 After filtering: 42 jobs           // Active jobs count
```

### Performance Targets:
- Firestore query: <1s (mostly network)
- Client-side filtering: <5ms
- Distance enrichment: <1ms
- Total load: <2s

### Monitor in Android Studio:
1. Window → Show Tool Window → Logcat
2. Filter: `"Timber"` or `"📂"` or `"📦"`
3. Watch realtime queries:
   ```
   adb logcat | grep "FIRESTORE\|📂\|📦"
   ```

## 5. Verify Geohash Storage

### In Firebase Console:
1. Project → Firestore Database → Data
2. Collection: `jobs`
3. Open any document
4. Look for field: `geoHash`
5. Value should be like: `"ttg9p"` or `"u89wj"`

### Expected geoHash Values:
```
Mumbai (19.07°N, 72.87°E) → "ttg9p"
Delhi (28.63°N, 77.20°E) → "ts4xr"
Bangalore (12.97°N, 77.59°E) → "tdn3m"
NYC (40.71°N, -74.00°W) → "dr5r"
London (51.50°N, -0.12°W) → "u10h"
```

If you don't see `geoHash` field:
- New jobs might not have it (check createdAt timestamp)
- Run job creation with new job form
- Verify `normalizeGeoFields()` is called in `createJob()`

## 6. Enable Debug Logging

### In Timber (already configured):
```kotlin
// In MainApplication or LaunchActivity
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
```

### Filter specific queries in Logcat:
1. Android Studio → Logcat
2. In search box, enter: `FIRESTORE` or `📂` or `getAllJobs`
3. Watch geohash queries in real-time

### Sample Full Log:
```
📂 ========== FIRESTORE QUERY START ==========
📂 getAllJobsSummary called:
📂   - limit: 50
📂   - lastDocumentId: null
📂   - category: DELIVERY
📂   - userLocation: (19.0760, 72.8777)
📂   - radiusKm: 50.0
📂 ✅ Valid user location detected: (19.0760, 72.8777) - Applying geohash filter
📂 ✅ Category filter APPLIED: category == 'DELIVERY'
📂 Required index: (category ASC, geoHash ASC, createdAt DESC)
📂 ✅ Geohash range query calculated:
📂   - Start: ttg9p
📂   - End: ttg9p~
📂   - Covering 50km radius
📂 ✅ Geohash filter applied - fetching only nearby jobs
📂 Ordering: createdAt DESC
📂 Pagination: FIRST PAGE (no cursor)
📂 Limit: 50 jobs
📂 Executing Firestore query...
📂 Query completed in 342ms
📂 Documents returned from Firestore: 47
📦 ========== CLIENT-SIDE FILTERING ==========
📦 Firestore returned: 47 documents
📦 After filtering (isActive=true, not filled, not expired): 42 jobs
📦 Filtered out: 5 jobs (non-active, filled, or expired)
📦 Sample job categories:
📦   - Urgent Delivery Needed: category='DELIVERY'
📦   - Help Moving Items: category='DELIVERY'
```

## 7. Troubleshooting

### Problem: No jobs displayed
**Possible Causes**:
1. Location permission not granted
2. No jobs in database within radius
3. All jobs are expired/filled
4. Geohash field missing from jobs

**Fix**:
```
1. Check location permission: Settings → Permissions
2. Create test job with current location
3. Check Firebase Console for jobs with valid coordinates
4. Verify geoHash field exists on documents
5. Check logs for error messages
```

### Problem: Jobs from 100+ km away still showing
**Indicates**: Geohash filter not working

**Debug**:
1. Check log for: `"✅ Geohash filter applied"`
2. If NOT present, userLocation might be invalid:
   ```
   📂 ⚠️ Invalid/missing user location - Fetching all jobs
   ```
3. Verify device location is enabled
4. Check location coordinates are valid:
   - Latitude: -90 to +90
   - Longitude: -180 to +180
   - Not (0, 0)

### Problem: Query timeout or very slow
**Possible Causes**:
1. Composite index not created in Firebase
2. Too many jobs in collection (100K+)
3. Network connectivity issue

**Fix**:
1. Create composite index in Firebase Console
2. Check index status = "READY"
3. Check internet connection
4. Try with smaller radiusKm value (25 instead of 50)

### Problem: Duplicate jobs in infinite scroll
**Should not happen, but if it does**:
- Indicates `lastDocumentId` pagination cursor broken
- Check DocumentSnapshot is being used correctly

**Verify**:
```kotlin
// Should see in logs:
📂 Pagination: startAfter document 'doc_id_12345'
// NOT:
📂 Pagination: startAfter document timestamp
```

## 8. Before Committing/Deploying

### Checklist:
- [ ] Build compiles without errors
- [ ] No runtime crashes when opening app
- [ ] Location permission works
- [ ] Jobs load with geohash queries
- [ ] Distance values display correctly
- [ ] Scroll loads more jobs
- [ ] Category filter works
- [ ] No 100+ km jobs in nearby listings
- [ ] Performance is acceptable (<2s initial load)

### Push to Production:
```bash
1. Verify all tests pass
2. Create Firestore indexes (if not auto-created)
3. Build release APK: gradlew bundleRelease
4. Upload to Firebase/Play Store
5. Monitor first 24 hours for crashes
```

## 9. Testing Edge Cases

### Test: Empty results
```
Use coordinates in ocean: (20.0, 20.0)
Expected: No jobs returned (or very few)
```

### Test: Multiple categories
```
Open "All Jobs", then filter by category
Expected: Category filter + geohash filter both applied
```

### Test: Rapid location changes
```
Change location 5 times quickly
Expected: No crashes, queries should cancel old requests
```

### Test: Network disconnection
```
Turn off WiFi/mobile while app loading
Expected: Graceful error, not crash
```

## 10. Performance Benchmarking

### Measure with Profiler:
1. Run → Profile 'app'
2. Wait for profiler to load
3. Open All Jobs screen
4. Check CPU / Memory usage
5. Profiler should show Firestore query time

### Look for:
- CPU: Query execution <500ms
- Memory: Increase <50MB for 50 jobs
- Network: Download <2MB

---

**Testing Status**: Ready ✅
**Next Step**: Create Firestore composite indexes
**Estimated Time**: 30 minutes testing
