# DutyPe Database Optimization Analysis - Part 4: Optimization Recommendations

---

## 3️⃣ Unnecessary Fields to Remove

### ❌ Deprecated Collections (Remove Entirely)

#### 1. `fcm_tokens` Collection
**Status:** Fully migrated to `users.fcmToken`  
**Action:** Delete collection  
**Impact:** -10K documents, saves ~500KB storage  
**Migration:** Complete ✅

```javascript
// Before (deprecated)
db.collection("fcm_tokens").doc(userId).get()

// After (current)
db.collection("users").doc(userId).get() // Read users.fcmToken field
```

#### 2. `saved_jobs` Collection
**Status:** Fully migrated to `users.savedJobs[]`  
**Action:** Delete collection  
**Impact:** -5K documents, saves ~250KB storage  
**Migration:** Complete ✅

```javascript
// Before (deprecated)
db.collection("saved_jobs").where("workerId", "==", userId).get()

// After (current)
db.collection("users").doc(userId).get() // Read users.savedJobs array
```

#### 3. `rating_summaries` Collection
**Status:** Fully migrated to `users.ratingSummary`  
**Action:** Delete collection  
**Impact:** -10K documents, saves ~500KB storage  
**Migration:** Complete ✅

```javascript
// Before (deprecated)
db.collection("rating_summaries").doc(userId).get()

// After (current)
db.collection("users").doc(userId).get() // Read users.ratingSummary object
```

**Total Savings:** -25K documents, ~1.25MB storage

---

### ⚠️ Redundant Fields (Consider Removing)

#### Users Collection

| Field | Issue | Recommendation |
|-------|-------|----------------|
| None identified | All fields actively used | No changes |

#### Jobs Collection

| Field | Issue | Recommendation |
|-------|-------|----------------|
| `companyName` | Duplicates `users.companyName` | ⚠️ Keep for denormalization (performance) |

**Analysis:** The denormalization is intentional for performance. Fetching company name from users collection for every job card would require 50+ additional reads per page.

**Recommendation:** Keep current design.

#### Applications Collection

| Field | Issue | Recommendation |
|-------|-------|----------------|
| `jobTitle` | Duplicates `jobs.title` | ✅ Keep - Denormalized for performance |
| `jobLocation` | Duplicates `jobs.location` | ✅ Keep - Denormalized for performance |
| `companyName` | Duplicates `jobs.companyName` | ✅ Keep - Denormalized for performance |
| `workerName` | Duplicates `users.fullName` | ✅ Keep - Denormalized for performance |

**Analysis:** These denormalized fields prevent 4 additional reads per application card. Essential for list views.

---

### 🔄 Fields That Can Be Derived (Consider Removing)

#### Jobs Collection

| Field | Can Be Derived From | Recommendation |
|-------|---------------------|----------------|
| `isFilled` | `vacancies == 0` OR `applicationCount >= vacancies` | ⚠️ Keep - Explicit status is clearer |

**Analysis:** While `isFilled` can be derived, having an explicit field:
- Improves query performance (indexed field)
- Prevents race conditions
- Makes business logic clearer

**Recommendation:** Keep current design.

#### Applications Collection

| Field | Can Be Derived From | Recommendation |
|-------|---------------------|----------------|
| `updatedAt` | Last status change timestamp | ✅ Keep - Useful for sorting |

---

### 📊 Summary of Removals

| Category | Items | Storage Saved | Read Reduction |
|----------|-------|---------------|----------------|
| Deprecated Collections | 3 | ~1.25MB | -25K reads/day |
| Redundant Fields | 0 | 0 | 0 |
| Derivable Fields | 0 | 0 | 0 |

**Total Impact:** Minimal storage savings, but cleaner architecture.

