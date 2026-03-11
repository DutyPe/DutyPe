# Dual Role Architecture - Industry Best Practices

## Research Summary: How Major Companies Handle Dual Roles

### 1. **Uber** (Driver + Passenger)
**Architecture Pattern:**
- **Single Account, Multiple Roles**: One user can be both driver and passenger
- **Role Switching**: Users can switch between driver and passenger modes in the app
- **Separate Data Collections**: 
  - `users` collection stores core user data
  - `drivers` collection stores driver-specific data (vehicle, license, etc.)
  - `passengers` collection stores passenger-specific data (payment methods, preferences)
- **Active Role Tracking**: `activeRole` field tracks which mode user is currently in
- **Role-Specific Permissions**: Different permissions and features based on active role

**Database Structure:**
```
users/{userId}
  - id
  - phone
  - name
  - email
  - roles: ["DRIVER", "PASSENGER"]
  - activeRole: "DRIVER"
  - profileImageUrl
  
drivers/{userId}
  - vehicleInfo
  - licenseNumber
  - rating
  - completedTrips
  
passengers/{userId}
  - paymentMethods
  - savedAddresses
  - tripHistory
```

### 2. **Airbnb** (Host + Guest)
**Architecture Pattern:**
- **Single Account, Dual Capability**: One account can both host and book
- **No Explicit Role Switching**: Users access host or guest features as needed
- **Unified Profile**: Single profile with both host and guest information
- **Separate Collections for Listings and Bookings**

**Database Structure:**
```
users/{userId}
  - id
  - name
  - email
  - isHost: true/false
  - isGuest: true/false
  - hostProfile: {...}
  - guestProfile: {...}
  
listings/{listingId}
  - hostId
  - property details
  
bookings/{bookingId}
  - guestId
  - listingId
  - dates
```

### 3. **Fiverr** (Buyer + Seller)
**Architecture Pattern:**
- **Single Account, Dual Mode**: One account can buy and sell services
- **Automatic Role Detection**: System automatically shows buyer or seller interface based on context
- **Unified Notifications**: Single notification system for both roles
- **Separate Dashboards**: Different dashboards for buying and selling

**Database Structure:**
```
users/{userId}
  - id
  - username
  - email
  - roles: ["BUYER", "SELLER"]
  - sellerProfile: {...}
  - buyerProfile: {...}
  
gigs/{gigId}
  - sellerId
  - service details
  
orders/{orderId}
  - buyerId
  - sellerId
  - gigId
```

---

## DutyPe Implementation Strategy

### Current Architecture (CORRECT ✅)
We're following the **Uber/Fiverr pattern** - Single account with multiple roles:

```kotlin
// User Model
data class User(
    val id: String,
    val phone: String,
    val fullName: String,
    val roles: List<String> = listOf("WORKER"), // ["WORKER", "EMPLOYER"]
    val activeRole: UserRole = UserRole.WORKER,
    
    // Worker-specific fields
    val skills: String? = null,
    val experience: String? = null,
    
    // Employer-specific fields
    val companyName: String? = null,
    val industry: String? = null,
    val companySize: String? = null,
    
    // Shared fields
    val profileImageUrl: String? = null,
    val address: String = "",
    val profileCompleted: Boolean = false
)
```

### Database Structure

#### Single `users` Collection (Recommended ✅)
```
users/{userId}
  - id: "user123"
  - phone: "+919876543210"
  - fullName: "John Doe"
  - email: "john@example.com"
  - roles: ["WORKER", "EMPLOYER"]
  - activeRole: "WORKER"
  - profileCompleted: true
  
  // Worker-specific fields
  - skills: "Plumber, Electrician"
  - experience: "5 years"
  - dateOfBirth: "1990-01-01"
  - gender: "Male"
  
  // Employer-specific fields
  - companyName: "ABC Company"
  - industry: "Construction"
  - companySize: "10-50"
  - businessAddress: "123 Main St"
  - gstNumber: "GST123456"
  
  // Shared fields
  - profileImageUrl: "https://..."
  - address: "123 Main St"
  - latitude: 28.6139
  - longitude: 77.2090
  - fcmToken: "fcm_token_here"
  - createdAt: 1234567890
  - isActive: true
```

**Advantages:**
- ✅ Single source of truth
- ✅ Easy to query user data
- ✅ Simpler authentication
- ✅ Easier to implement role switching
- ✅ Follows Uber/Fiverr pattern

**Disadvantages:**
- ⚠️ Document can get large with many fields
- ⚠️ Need to handle null fields carefully

---

## Implementation Checklist

### ✅ Completed
1. User model supports multiple roles
2. DualRoleManager component for role switching
3. Role-specific navigation (WorkerMainScreen, EmployerMainScreen)
4. ProfileSetupStateManager tracks completion per role

### 🔧 Fixes Needed
1. **Profile Setup Screens** - Ensure roles array is updated when creating profile
2. **Role Switching** - Ensure activeRole is updated in Firestore
3. **Profile Completion Check** - Check profileCompleted flag correctly per role
4. **Data Persistence** - Ensure role-specific data is saved and loaded correctly

---

## Code Implementation

### 1. When User Creates Employer Profile (After Switching from Worker)

```kotlin
// In MandatoryEmployerProfileSetupScreen.kt
fun handleCompletion() {
    scope.launch {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Save employer profile data
            val employerProfileData = mapOf(
                "companyName" to companyName,
                "industry" to industry,
                "companySize" to companySize,
                "businessAddress" to businessAddress,
                "gstNumber" to gstNumber,
                "profileCompleted" to true,
                "role" to "EMPLOYER"
            )
            
            profileCompletionViewModel.saveEmployerProfileData(employerProfileData)
            
            // CRITICAL: Update roles array and activeRole
            val userRef = firestore.collection("users").document(currentUser.uid)
            val userDoc = userRef.get().await()
            val currentRoles = userDoc.get("roles") as? List<String> ?: listOf()
            
            if (!currentRoles.contains("EMPLOYER")) {
                val updatedRoles = currentRoles.toMutableList().apply {
                    add("EMPLOYER")
                }
                
                userRef.update(mapOf(
                    "roles" to updatedRoles,
                    "activeRole" to "EMPLOYER"
                )).await()
            }
            
            // Mark profile complete for employer role
            profileCompletionViewModel.markProfileComplete(UserRole.EMPLOYER)
        }
    }
}
```

### 2. When User Switches Roles

```kotlin
// In RoleSwitchManager.kt
suspend fun switchRole(newRole: UserRole) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
        // Update activeRole in Firestore
        firestore.collection("users")
            .document(currentUser.uid)
            .update("activeRole", newRole.name)
            .await()
        
        // Clear caches
        roleCacheManager.clearRoleSpecificCache(oldRole)
        
        // Navigate to new role home
        navigateToRoleHome(navController, newRole)
    }
}
```

### 3. Profile Completion Check

```kotlin
// Check if profile is complete for current role
suspend fun isProfileComplete(userId: String, role: UserRole): Boolean {
    val userDoc = firestore.collection("users").document(userId).get().await()
    
    return when (role) {
        UserRole.WORKER -> {
            // Check worker-specific required fields
            userDoc.getString("fullName") != null &&
            userDoc.getString("phone") != null &&
            userDoc.getString("skills") != null &&
            userDoc.getBoolean("profileCompleted") == true
        }
        UserRole.EMPLOYER -> {
            // Check employer-specific required fields
            userDoc.getString("companyName") != null &&
            userDoc.getString("phone") != null &&
            userDoc.getString("industry") != null &&
            userDoc.getBoolean("profileCompleted") == true
        }
        else -> false
    }
}
```

---

## Best Practices

### 1. **Data Consistency**
- Always update `roles` array when enabling a new role
- Always update `activeRole` when switching roles
- Use Firestore transactions for critical updates

### 2. **Profile Completion**
- Track completion separately for each role
- Use `profileCompleted` flag as master flag (true if ANY role is complete)
- Check role-specific required fields before allowing access

### 3. **Role Switching**
- Clear role-specific caches when switching
- Refresh user data from Firestore after switch
- Update FCM token with new role for notifications

### 4. **Security**
- Validate role permissions in Firestore rules
- Ensure users can only access data for their enabled roles
- Prevent unauthorized role switching

---

## Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      // Users can read their own data
      allow read: if request.auth.uid == userId;
      
      // Users can update their own data
      allow update: if request.auth.uid == userId
        // Prevent changing roles array directly (must use Cloud Function)
        && (!request.resource.data.diff(resource.data).affectedKeys().hasAny(['roles']))
        // Allow changing activeRole only to roles they have
        && (request.resource.data.activeRole in resource.data.roles);
      
      // Allow creation during signup
      allow create: if request.auth.uid == userId;
    }
  }
}
```

---

## Summary

**DutyPe follows the industry-standard dual-role pattern used by Uber, Fiverr, and Airbnb:**

1. ✅ Single account with multiple roles
2. ✅ `roles` array tracks enabled roles
3. ✅ `activeRole` tracks current active role
4. ✅ Role-specific data stored in same document
5. ✅ Role switching updates `activeRole` only
6. ✅ Profile completion tracked per role

**Key Fix:** Ensure `roles` array is updated when user completes profile setup for a new role.
