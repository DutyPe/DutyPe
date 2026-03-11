# REFER & EARN SCREEN ISSUES - FIX REQUIRED

## Issues Identified

### Issue 1: Worker Screen Missing Referral Code Card
**Problem**: Worker Refer & Earn screen doesn't show the referral code with Copy and Share buttons like the Employer screen does.

**Location**: `app/src/main/java/com/example/dutype/worker/screens/WorkerReferEarnScreen.kt`

**What's Missing**:
- Referral Code Card with the code displayed
- Copy Code button
- Share button

**What Employer Has** (Line 218-240):
```kotlin
// Referral Code Card
item {
    AnimatedVisibility(visible = isVisible, enter = fadeIn(tween(400)) + slideInVertically(tween(400))) {
        EmployerReferralCodeCard(
            referralCode = uiState.stats?.referralCode ?: "",
            onCopyClick = {
                clipboardManager.setText(AnnotatedString(uiState.stats?.referralCode ?: ""))
                showCopySuccess = true
            },
            onShareClick = {
                val code = uiState.stats?.referralCode ?: ""
                val shareText = """
🎁 Join DutyPe for hiring!

Use my referral code: $code

📲 Download DutyPe: $playStoreUrl

Find reliable workers for your business and earn ₹25 bonus!
                """.trimIndent()
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, "Share Referral Code"))
            }
        )
    }
}
```

**What Worker Needs**: Same card but with worker-specific share text

---

### Issue 2: Stats Not Updating
**Problem**: Recent referrals show correctly, but these values are NOT updating:
- Next Milestone progress
- Your Stats (Total Referrals, Total Earned)
- Current Tier

**Root Cause**: The `ReferralViewModel` is loading data from Firestore, but the values might not be calculated correctly or the UI is not reacting to state changes.

**Files to Check**:
1. `app/src/main/java/com/example/dutype/viewmodels/ReferralViewModel.kt` - Check `loadReferralData()` method
2. `app/src/main/java/com/example/dutype/services/ReferralService.kt` - Check how stats are calculated
3. Firestore `referral_stats` collection - Check if data is being saved correctly

**Expected Behavior**:
- When a referral is successful, `totalReferrals` and `successfulReferrals` should increment
- `totalEarnings` should increase by ₹25
- `currentTier` should update based on `successfulReferrals`
- `nextMilestone` should update based on current progress

---

## Fix Plan

### Fix 1: Add Referral Code Card to Worker Screen

**Step 1**: Add the referral code card item in the LazyColumn (after TierBadgeCard, before StatsGrid)

**Location**: `WorkerReferEarnScreen.kt` around line 250

```kotlin
// Add this item after TierBadgeCard and before StatsGrid
item {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400))
    ) {
        WorkerReferralCodeCard(
            referralCode = uiState.stats?.referralCode ?: "",
            onCopyClick = {
                clipboardManager.setText(AnnotatedString(uiState.stats?.referralCode ?: ""))
                showCopySuccess = true
            },
            onShareClick = {
                val code = uiState.stats?.referralCode ?: ""
                val shareText = """
🎁 Join DutyPe and find work!

Use my referral code: $code

📲 Download DutyPe: $playStoreUrl

Find jobs near you and earn ₹25 bonus!
                """.trimIndent()
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, "Share Referral Code"))
            }
        )
    }
}
```

**Step 2**: Add the WorkerReferralCodeCard composable function

**Location**: Add at the end of the file, before the last closing brace

```kotlin
@Composable
private fun WorkerReferralCodeCard(
    referralCode: String,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Referral Code",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            SelectionContainer {
                Text(
                    text = referralCode,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        letterSpacing = 2.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF374151)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(IconSizes.Standard)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Code")
                }
                
                Button(
                    onClick = onShareClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2937)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(IconSizes.Standard)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}
```

---

### Fix 2: Debug Stats Not Updating

**Step 1**: Check ReferralViewModel

Need to verify that `loadReferralData()` is correctly fetching and calculating stats from Firestore.

**Step 2**: Check ReferralService

Need to verify that when a referral is successful, the stats are being updated in Firestore.

**Step 3**: Add Logging

Add Timber logs to track:
- When stats are loaded
- What values are being loaded
- When stats are updated
- What values are being updated

**Step 4**: Check Firestore Structure

Verify that the `referral_stats` collection has the correct structure:
```json
{
  "userId": "user123",
  "referralCode": "ABC123",
  "totalReferrals": 5,
  "successfulReferrals": 3,
  "totalEarnings": 75.0,
  "availableBalance": 75.0,
  "currentTier": "BRONZE",
  "nextMilestone": 5,
  "referrals": [
    {
      "referredUserId": "user456",
      "referredUserName": "John Doe",
      "status": "COMPLETED",
      "reward": 25.0,
      "timestamp": 1234567890
    }
  ]
}
```

---

## Testing Checklist

### Worker Screen
- [ ] Referral code is displayed
- [ ] Copy button works and shows "Code copied!" message
- [ ] Share button opens share dialog with correct text
- [ ] Stats show correct values
- [ ] Next Milestone progress bar updates
- [ ] Current Tier updates when milestones are reached

### Employer Screen
- [ ] Referral code is displayed
- [ ] Copy button works
- [ ] Share button works
- [ ] Stats show correct values
- [ ] Next Milestone progress bar updates
- [ ] Current Tier updates

### Both Screens
- [ ] Recent referrals list shows correct data
- [ ] Total Referrals count is correct
- [ ] Successful Referrals count is correct
- [ ] Total Earned amount is correct
- [ ] Available Balance is correct
- [ ] Withdraw button appears when balance >= ₹50

---

## Status

- [ ] Fix 1: Add Referral Code Card to Worker Screen
- [ ] Fix 2: Debug and fix stats not updating
- [ ] Test both screens thoroughly
- [ ] Verify Firestore data structure
- [ ] Deploy fixes

---

**Priority**: HIGH
**Impact**: User Experience - Users cannot share referral codes from Worker screen
**Estimated Time**: 2-3 hours
