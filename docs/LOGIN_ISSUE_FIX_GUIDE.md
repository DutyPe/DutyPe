# Login Issue Fix Guide - "No Account Existed" Error

## Problem

User reports: "When I'm logging in with my old phone number which I logged in many times recently, it's saying 'no account existed please register now'"

## Root Cause

**Phone number format mismatch** between what's stored in Firestore and what's being queried during login.

Possible scenarios:
- Stored as: `+919876543210` but querying: `919876543210`
- Stored as: `919876543210` but querying: `+919876543210`
- Stored in `phoneNumber` field but querying `phone` field
- User exists in Firebase Auth but not in Firestore

## Solution Implemented

### 1. Created PhoneNumberUtils.kt

New utility class that normalizes phone numbers to consistent format:
- Always stores as: `+{countryCode}{number}`
- Example: `+919876543210`
- Handles all input formats automatically

**Location:** `app/src/main/java/com/example/dutype/utils/PhoneNumberUtils.kt`

### 2. Updated FirestoreUtils.kt

Enhanced phone lookup to try all possible variants:
- `+919876543210` (normalized with +)
- `919876543210` (without +)
- `9876543210` (without country code)

Searches both `phone` and `phoneNumber` fields for backward compatibility.

**Location:** `app/src/main/java/com/example/dutype/utils/FirestoreUtils.kt`

### 3. Added Comprehensive Logging

Now logs every step of phone lookup:
- Input phone number
- All variants tried
- Query results
- Found/not found status

Check logs with tag: `FirestoreUtils`

### 4. Created Admin Debug Tool

Script to check user existence in both Firestore and Firebase Auth.

**Location:** `scripts/check-user-by-phone.js`

**Usage:**
```bash
node scripts/check-user-by-phone.js +919876543210
```

## How to Test the Fix

### Step 1: Check User Data

Run the debug script with the user's phone number:

```bash
cd scripts
node check-user-by-phone.js +919876543210
```

This will show:
- ✅ If user exists in Firestore
- ✅ If user exists in Firebase Auth
- ⚠️ If there's a mismatch
- 📊 User data if found

### Step 2: Test Login Flow

1. Open the app
2. Go to Login screen
3. Enter the phone number
4. Check Logcat for detailed logs:
   ```
   🔍 === USER EXISTENCE CHECK START ===
   🔍 Input phone: +919876543210
   🔍 Variants to try: [+919876543210, 919876543210, 9876543210]
   🔍 Trying phone field with variant: +919876543210
   ✅ FOUND! Document ID: abc123
   ```

### Step 3: Verify Fix

Expected behavior:
- ✅ User found with any phone format
- ✅ Login proceeds to OTP screen
- ✅ After OTP, user goes to home screen

## Common Issues & Solutions

### Issue 1: User in Auth but not Firestore

**Symptom:** Debug script shows user in Auth but not Firestore

**Solution:** The app will now automatically recreate the Firestore document when user logs in.

**Manual Fix (if needed):**
```javascript
// In Firebase Console or using script
db.collection('users').doc('{uid}').set({
  phone: '+919876543210',
  role: 'WORKER',
  platform: 'android',
  createdAt: admin.firestore.FieldValue.serverTimestamp()
}, { merge: true });
```

### Issue 2: Phone Format Inconsistency

**Symptom:** User has both `phone` and `phoneNumber` fields with different formats

**Solution:** Run data migration script (coming soon) or manually update:

```javascript
// Update to normalized format
db.collection('users').doc('{uid}').update({
  phone: '+919876543210',
  phoneNumber: admin.firestore.FieldValue.delete()  // Remove legacy field
});
```

### Issue 3: Missing Firestore Index

**Symptom:** Queries timeout or fail

**Solution:**
1. Go to Firebase Console → Firestore → Indexes
2. Create composite index:
   - Collection: `users`
   - Field: `phone` (Ascending)
   - Query scope: Collection

## Monitoring

### Check Logs

Filter Logcat by tag:
```
adb logcat -s FirestoreUtils
```

Look for:
- `✅ FOUND!` - User lookup successful
- `❌ NOT FOUND` - User lookup failed
- `⚠️ User exists in Auth but not Firestore` - Mismatch detected

### Track Metrics

Monitor these in production:
- Login success rate (target: >95%)
- Phone lookup failures (target: <5%)
- Auth/Firestore mismatches (target: <10/day)

## Next Steps

### Immediate (Done ✅)
- [x] Create PhoneNumberUtils
- [x] Update FirestoreUtils
- [x] Add comprehensive logging
- [x] Create debug script

### Short-term (To Do)
- [ ] Test with user's actual phone number
- [ ] Verify Firestore indexes exist
- [ ] Create data migration script
- [ ] Update all phone number saves to use PhoneNumberUtils

### Long-term (Future)
- [ ] Implement Firebase PNV (instant verification)
- [ ] Add device fingerprinting
- [ ] Implement rate limiting
- [ ] Add phone number change flow

## Support

If issue persists:

1. **Collect Debug Info:**
   ```bash
   node scripts/check-user-by-phone.js <phone_number>
   ```

2. **Check Logs:**
   - Filter by `FirestoreUtils` tag
   - Look for error messages
   - Note which variants were tried

3. **Verify Data:**
   - Check Firebase Console → Firestore → users collection
   - Search by phone number
   - Verify `phone` field format

4. **Contact Support:**
   - Provide debug script output
   - Provide relevant logs
   - Provide user's phone number (last 4 digits only)

---

## Technical Details

### Phone Number Normalization

**Input Formats Supported:**
- `9876543210` → `+919876543210`
- `919876543210` → `+919876543210`
- `+919876543210` → `+919876543210`

**Lookup Variants:**
1. `+919876543210` (normalized)
2. `919876543210` (without +)
3. `9876543210` (without country code)

### Database Schema

**users/{uid}:**
```javascript
{
  phone: "+919876543210",        // Primary field (normalized)
  phoneNumber: "...",             // Legacy field (deprecated)
  role: "WORKER",
  roles: ["WORKER"],
  activeRole: "WORKER",
  profileCompleted: true,
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

### Firestore Rules

```javascript
match /users/{userId} {
  // Allow unauthenticated phone lookup
  allow get: if true;
  allow list: if request.query.limit <= 1;
}
```

---

*Last Updated: March 11, 2026*
*Version: 1.0*
