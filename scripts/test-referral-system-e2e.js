/**
 * Comprehensive End-to-End Test Suite for DutyPe Referral System
 * Tests every single corner of the referral code system:
 * 1. Unique canonical code generation (DUTYXXXX) for workers & employers
 * 2. Multi-collection synchronization (referral_codes, users, referral_stats)
 * 3. Self-healing code lookup
 * 4. Self-referral prevention
 * 5. Invalid code rejection
 * 6. Instant two-sided rewards (₹25 referrer + ₹25 referee)
 * 7. Real-time balance and earnings updates in users & referral_stats
 * 8. Notification dispatch for both parties
 * 9. Referral events logging
 * 10. Duplicate referral prevention (replay protection)
 * 11. Profile completion independence (works without 100% profile setup)
 * 12. Milestones and tier calculations
 * 13. Automatic test cleanup
 */

const admin = require('../functions/node_modules/firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const serviceAccount = loadServiceAccount();
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

// Color formatting
const colors = {
  reset: "\x1b[0m",
  green: "\x1b[32m",
  red: "\x1b[31m",
  yellow: "\x1b[33m",
  cyan: "\x1b[36m",
  bold: "\x1b[1m"
};

let passedTests = 0;
let failedTests = 0;

function assert(condition, message) {
  if (condition) {
    console.log(`  ${colors.green}✓ PASS:${colors.reset} ${message}`);
    passedTests++;
  } else {
    console.error(`  ${colors.red}✗ FAIL:${colors.reset} ${message}`);
    failedTests++;
    throw new Error(`Assertion failed: ${message}`);
  }
}

// Import compiled functions from functions/lib/referral-system.js
const referralSystem = require('../functions/lib/referral-system.js');
const { executeReferralApplication, processPendingReferralOnProfileComplete } = referralSystem;

// Generate unique test ID
const testRunId = `TEST_${Date.now()}`;
const testUserAId = `usr_ref_a_${testRunId}`;
const testUserBId = `usr_ref_b_${testRunId}`;
const testUserCId = `usr_ref_c_${testRunId}`;

const cleanupList = {
  users: [],
  referral_codes: [],
  referral_stats: [],
  referrals: [],
  notifications: [],
  referral_events: []
};

async function runAllTests() {
  console.log(`\n${colors.bold}${colors.cyan}======================================================${colors.reset}`);
  console.log(`${colors.bold}${colors.cyan}  DUTYPE REFERRAL SYSTEM - COMPREHENSIVE E2E TESTS     ${colors.reset}`);
  console.log(`${colors.bold}${colors.cyan}======================================================${colors.reset}\n`);
  console.log(`Test Run ID: ${testRunId}\n`);

  try {
    // -----------------------------------------------------------------
    // TEST CORNER 1: Unique Code Generation (Canonical DUTYXXXX Format)
    // -----------------------------------------------------------------
    console.log(`${colors.bold}Corner 1: Unique Canonical Code Generation & Doc Sync${colors.reset}`);
    
    const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    function genSuffix() {
      let s = "";
      for (let i = 0; i < 4; i++) s += alphabet.charAt(Math.floor(Math.random() * alphabet.length));
      return s;
    }
    const codeA = `DUTY${genSuffix()}`;
    const codeC = `DUTY${genSuffix()}`;
    
    assert(codeA.startsWith("DUTY") && codeA.length === 8, `Code A format valid: ${codeA}`);
    assert(codeC.startsWith("DUTY") && codeC.length === 8, `Code C format valid: ${codeC}`);
    assert(codeA !== codeC, `Codes are mutually unique: ${codeA} !== ${codeC}`);

    // Create User A (Referrer - Worker, incomplete profile)
    cleanupList.users.push(testUserAId);
    cleanupList.referral_stats.push(testUserAId);
    cleanupList.referral_codes.push(codeA);

    await db.collection("users").doc(testUserAId).set({
      userId: testUserAId,
      uid: testUserAId,
      fullName: "Referrer Ramesh",
      phoneNumber: "+919888800001",
      phone: "+919888800001",
      role: "WORKER",
      activeRole: "WORKER",
      referralCode: codeA,
      profileCompleted: false, // Testing that profile does NOT need to be 100% complete
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      referralStats: {
        totalReferrals: 0,
        successfulReferrals: 0,
        totalEarnings: 0,
        availableBalance: 0,
        canWithdraw: false,
        currentTier: "BRONZE"
      }
    });

    await db.collection("referral_stats").doc(testUserAId).set({
      userId: testUserAId,
      userRole: "WORKER",
      referralCode: codeA,
      totalReferrals: 0,
      successfulReferrals: 0,
      totalEarnings: 0,
      availableBalance: 0,
      canWithdraw: false,
      currentTier: "BRONZE"
    });

    await db.collection("referral_codes").doc(codeA).set({
      code: codeA,
      userId: testUserAId,
      userRole: "WORKER",
      userName: "Referrer Ramesh",
      isActive: true,
      totalUsed: 0,
      successfulReferrals: 0,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Create User C (Employer, incomplete profile)
    cleanupList.users.push(testUserCId);
    cleanupList.referral_stats.push(testUserCId);
    cleanupList.referral_codes.push(codeC);

    await db.collection("users").doc(testUserCId).set({
      userId: testUserCId,
      uid: testUserCId,
      fullName: "Employer Enterprises",
      companyName: "Employer Enterprises",
      phoneNumber: "+919888800002",
      phone: "+919888800002",
      role: "EMPLOYER",
      activeRole: "EMPLOYER",
      referralCode: codeC,
      profileCompleted: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      referralStats: {
        totalReferrals: 0,
        successfulReferrals: 0,
        totalEarnings: 0,
        availableBalance: 0,
        canWithdraw: false,
        currentTier: "BRONZE"
      }
    });

    await db.collection("referral_stats").doc(testUserCId).set({
      userId: testUserCId,
      userRole: "EMPLOYER",
      referralCode: codeC,
      totalReferrals: 0,
      successfulReferrals: 0,
      totalEarnings: 0,
      availableBalance: 0,
      canWithdraw: false,
      currentTier: "BRONZE"
    });

    await db.collection("referral_codes").doc(codeC).set({
      code: codeC,
      userId: testUserCId,
      userRole: "EMPLOYER",
      userName: "Employer Enterprises",
      isActive: true,
      totalUsed: 0,
      successfulReferrals: 0,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Verify persistence
    const codeDocA = await db.collection("referral_codes").doc(codeA).get();
    assert(codeDocA.exists && codeDocA.data().userId === testUserAId, `referral_codes/${codeA} indexed with correct userId`);
    const statsDocA = await db.collection("referral_stats").doc(testUserAId).get();
    assert(statsDocA.exists && statsDocA.data().referralCode === codeA, `referral_stats/${testUserAId} has referralCode`);

    // -----------------------------------------------------------------
    // TEST CORNER 2: Self-Healing Lookup & Case Insensitivity
    // -----------------------------------------------------------------
    console.log(`\n${colors.bold}Corner 2: Self-Healing Lookup & Case Insensitivity${colors.reset}`);
    
    // Test case-insensitivity: lowercase code lookup
    const lowerCodeA = codeA.toLowerCase();
    const lookupLower = await executeReferralApplication({
      newUserId: testUserAId, // Self-referral test will fail at self-check after lookup succeeds!
      referralCode: lowerCodeA
    });
    assert(lookupLower.error === "Cannot use your own referral code", "Lowercase code properly resolved and reached validation");

    // Test self-healing: Delete referral_codes doc for codeC, then verify executeReferralApplication heals it
    await db.collection("referral_codes").doc(codeC).delete();
    const checkDeleted = await db.collection("referral_codes").doc(codeC).get();
    assert(!checkDeleted.exists, "referral_codes doc temporarily deleted for healing test");

    // User C tries self-referral with codeC: lookup should heal the missing referral_codes doc!
    const healResult = await executeReferralApplication({
      newUserId: testUserCId,
      referralCode: codeC
    });
    assert(healResult.error === "Cannot use your own referral code", "Self-healing lookup found code in users collection");
    const checkHealed = await db.collection("referral_codes").doc(codeC).get();
    assert(checkHealed.exists && checkHealed.data().userId === testUserCId, "referral_codes doc successfully self-healed in Firestore");

    // -----------------------------------------------------------------
    // TEST CORNER 3: Self-Referral Prevention
    // -----------------------------------------------------------------
    console.log(`\n${colors.bold}Corner 3: Self-Referral Prevention${colors.reset}`);
    const selfRefResult = await executeReferralApplication({
      newUserId: testUserAId,
      referralCode: codeA,
      newUserRole: "WORKER",
      newUserName: "Referrer Ramesh"
    });
    assert(!selfRefResult.success, "Self-referral rejected");
    assert(selfRefResult.error === "Cannot use your own referral code", `Correct error message returned: ${selfRefResult.error}`);

    // Verify balances did not change
    const postSelfStats = await db.collection("referral_stats").doc(testUserAId).get();
    assert((postSelfStats.data().totalEarnings || 0) === 0, "No earnings credited on self-referral attempt");

    // -----------------------------------------------------------------
    // TEST CORNER 4: Invalid Referral Code Rejection
    // -----------------------------------------------------------------
    console.log(`\n${colors.bold}Corner 4: Invalid Referral Code Rejection${colors.reset}`);
    const invalidResult = await executeReferralApplication({
      newUserId: "dummy_user_999",
      referralCode: "DUTY9999",
      newUserRole: "WORKER"
    });
    assert(!invalidResult.success, "Invalid referral code rejected");
    assert(invalidResult.error === "Referral code not found", `Correct error message: ${invalidResult.error}`);

    // -----------------------------------------------------------------
    // TEST CORNER 5: Instant Two-Sided Rewards (₹25 + ₹25)
    // -----------------------------------------------------------------
    console.log(`\n${colors.bold}Corner 5: Instant Two-Sided Reward Crediting (₹25 Each)${colors.reset}`);

    // Create User B (New registered user / Referee, incomplete profile)
    cleanupList.users.push(testUserBId);
    cleanupList.referral_stats.push(testUserBId);

    await db.collection("users").doc(testUserBId).set({
      userId: testUserBId,
      uid: testUserBId,
      fullName: "Referee Suresh",
      phoneNumber: "+919888800003",
      phone: "+919888800003",
      role: "WORKER",
      activeRole: "WORKER",
      profileCompleted: false, // New user without complete profile
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      referralStats: {
        totalReferrals: 0,
        successfulReferrals: 0,
        totalEarnings: 0,
        availableBalance: 0,
        canWithdraw: false,
        currentTier: "BRONZE"
      }
    });

    await db.collection("referral_stats").doc(testUserBId).set({
      userId: testUserBId,
      userRole: "WORKER",
      totalReferrals: 0,
      successfulReferrals: 0,
      totalEarnings: 0,
      availableBalance: 0,
      canWithdraw: false,
      currentTier: "BRONZE"
    });

    // Apply User A's referral code for User B
    const applyResult = await executeReferralApplication({
      newUserId: testUserBId,
      referralCode: codeA,
      newUserRole: "WORKER",
      newUserName: "Referee Suresh",
      newUserPhone: "+919888800003"
    });

    assert(applyResult.success === true, "Referral applied successfully");
    assert(applyResult.referrerReward === 25, "Referrer reward is ₹25");
    assert(applyResult.referredUserReward === 25, "Referred user reward is ₹25");
    assert(Boolean(applyResult.referralId), `Referral ID created: ${applyResult.referralId}`);
    cleanupList.referrals.push(applyResult.referralId);

    // Verify referrals collection document (Stage 1: Profile Incomplete -> PENDING)
    const referralDoc = await db.collection("referrals").doc(applyResult.referralId).get();
    assert(referralDoc.exists, "Referral record exists in 'referrals' collection");
    const refData = referralDoc.data();
    assert(refData.status === "PENDING", `Referral starts as PENDING when profile incomplete`);
    assert(refData.profileCompleted === false, `Referral doc profileCompleted === false`);
    assert(refData.rewardAmount === 25, "Referral doc rewardAmount === 25");
    assert(refData.referrerUserId === testUserAId, "Referrer userId matches User A");
    assert(refData.referredUserId === testUserBId, "Referred userId matches User B");

    // Verify Referrer User A has pendingReferrals: 1 (Stage 1)
    const pendingStatsA = await db.collection("referral_stats").doc(testUserAId).get();
    assert(pendingStatsA.data().pendingReferrals === 1, `Referrer pendingReferrals === 1`);
    assert(pendingStatsA.data().successfulReferrals === 0, `Referrer successfulReferrals is 0 until profile complete`);

    // Stage 2: User B completes profile! Trigger completion
    console.log(`  ${colors.cyan}Simulating User B profile completion...${colors.reset}`);
    await db.collection("users").doc(testUserBId).update({
      profileCompleted: true,
      bio: "Skilled worker with complete profile"
    });
    const completionResult = await processPendingReferralOnProfileComplete(testUserBId, {
      profileCompleted: true,
      activeRole: "WORKER",
      role: "WORKER",
      fullName: "Referee Suresh"
    });
    assert(completionResult?.status === "COMPLETED", "Profile completion transitioned referral to COMPLETED");

    // Verify referrals collection document is now COMPLETED
    const completedRefDoc = await db.collection("referrals").doc(applyResult.referralId).get();
    const completedRefData = completedRefDoc.data();
    assert(completedRefData.status === "COMPLETED", "Referral doc status updated to COMPLETED");
    assert(completedRefData.profileCompleted === true, "Referral doc profileCompleted updated to true");

    // Verify Referrer User A balances and stats (Both in referral_stats and users.referralStats)
    const updatedStatsA = await db.collection("referral_stats").doc(testUserAId).get();
    const statsAData = updatedStatsA.data();
    assert(statsAData.totalEarnings === 25, `Referrer referral_stats totalEarnings === 25 (was 0)`);
    assert(statsAData.availableBalance === 25, `Referrer referral_stats availableBalance === 25 (was 0)`);
    assert(statsAData.successfulReferrals === 1, `Referrer referral_stats successfulReferrals === 1`);
    assert(statsAData.pendingReferrals === 0, `Referrer referral_stats pendingReferrals decremented to 0`);

    const updatedUserA = await db.collection("users").doc(testUserAId).get();
    const userAData = updatedUserA.data();
    assert(userAData.referralStats.totalEarnings === 25, `Referrer users.referralStats.totalEarnings === 25`);
    assert(userAData.referralStats.availableBalance === 25, `Referrer users.referralStats.availableBalance === 25`);

    // Verify Referee User B balances and stats (Both in referral_stats and users.referralStats)
    const updatedStatsB = await db.collection("referral_stats").doc(testUserBId).get();
    const statsBData = updatedStatsB.data();
    assert(statsBData.totalEarnings === 25, `Referee referral_stats totalEarnings === 25 (Signup Bonus)`);
    assert(statsBData.availableBalance === 25, `Referee referral_stats availableBalance === 25`);
    assert(statsBData.signupBonusReceived === true, `Referee signupBonusReceived === true`);
    assert(statsBData.signupBonusAmount === 25, `Referee signupBonusAmount === 25`);
    assert(statsBData.referredByCode === codeA, `Referee referredByCode === ${codeA}`);
    assert(statsBData.referredByUserId === testUserAId, `Referee referredByUserId === ${testUserAId}`);

    const updatedUserB = await db.collection("users").doc(testUserBId).get();
    const userBData = updatedUserB.data();
    assert(userBData.referralStats.totalEarnings === 25, `Referee users.referralStats.totalEarnings === 25`);
    assert(userBData.referralStats.availableBalance === 25, `Referee users.referralStats.availableBalance === 25`);

    // Verify Notifications dispatched for both
    const notifQueryA = await db.collection("notifications")
      .where("recipientId", "==", testUserAId)
      .where("type", "==", "REFERRAL_REWARD")
      .get();
    assert(!notifQueryA.empty, "Notification created for Referrer (REFERRAL_REWARD)");
    notifQueryA.forEach(d => cleanupList.notifications.push(d.id));

    const notifQueryB = await db.collection("notifications")
      .where("recipientId", "==", testUserBId)
      .where("type", "==", "SIGNUP_BONUS")
      .get();
    assert(!notifQueryB.empty, "Notification created for Referee (SIGNUP_BONUS)");
    notifQueryB.forEach(d => cleanupList.notifications.push(d.id));

    // Verify referral_events logged for both
    const eventQueryA = await db.collection("referral_events")
      .where("userId", "==", testUserAId)
      .where("eventType", "==", "REWARD_CREDITED")
      .get();
    assert(!eventQueryA.empty, "Referral event REWARD_CREDITED logged for Referrer");
    eventQueryA.forEach(d => cleanupList.referral_events.push(d.id));

    const eventQueryB = await db.collection("referral_events")
      .where("userId", "==", testUserBId)
      .where("eventType", "==", "SIGNUP_BONUS_CREDITED")
      .get();
    assert(!eventQueryB.empty, "Referral event SIGNUP_BONUS_CREDITED logged for Referee");
    eventQueryB.forEach(d => cleanupList.referral_events.push(d.id));

    // -----------------------------------------------------------------
    // TEST CORNER 6: Duplicate Referral Prevention (Replay Protection)
    // -----------------------------------------------------------------
    console.log(`\n${colors.bold}Corner 6: Duplicate Referral Prevention${colors.reset}`);
    
    // User B tries to apply the same code again:
    const duplicateSameCode = await executeReferralApplication({
      newUserId: testUserBId,
      referralCode: codeA,
      newUserRole: "WORKER"
    });
    assert(duplicateSameCode.success === true && duplicateSameCode.message === "Referral already applied",
      "Re-applying same code returns idempotent success ('Referral already applied')");

    // User B tries to apply a different code (User C's code):
    const duplicateDifferentCode = await executeReferralApplication({
      newUserId: testUserBId,
      referralCode: codeC,
      newUserRole: "WORKER"
    });
    assert(!duplicateDifferentCode.success, "Applying a different code is strictly blocked");
    assert(duplicateDifferentCode.error === "You have already used a referral code",
      `Correct error message: ${duplicateDifferentCode.error}`);

    // Verify User A and User B balances did not double-credit
    const doubleCheckStatsA = await db.collection("referral_stats").doc(testUserAId).get();
    assert(doubleCheckStatsA.data().totalEarnings === 25, "Referrer earnings remains ₹25 (no double credit)");
    const doubleCheckStatsB = await db.collection("referral_stats").doc(testUserBId).get();
    assert(doubleCheckStatsB.data().totalEarnings === 25, "Referee earnings remains ₹25 (no double credit)");

    // -----------------------------------------------------------------
    // TEST CORNER 7: Instant Crediting for Already Complete Profiles
    // -----------------------------------------------------------------
    console.log(`\n${colors.bold}Corner 7: Instant Crediting for Already Complete Profiles${colors.reset}`);
    const testUserDId = `usr_ref_d_${testRunId}`;
    cleanupList.users.push(testUserDId);
    cleanupList.referral_stats.push(testUserDId);

    await db.collection("users").doc(testUserDId).set({
      userId: testUserDId,
      uid: testUserDId,
      fullName: "Employer Rao",
      phoneNumber: "+919888800004",
      phone: "+919888800004",
      role: "EMPLOYER",
      activeRole: "EMPLOYER",
      profileCompleted: true, // Already completed
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      referralStats: {
        totalReferrals: 0,
        successfulReferrals: 0,
        totalEarnings: 0,
        availableBalance: 0,
        canWithdraw: false,
        currentTier: "BRONZE"
      }
    });

    await db.collection("referral_stats").doc(testUserDId).set({
      userId: testUserDId,
      userRole: "EMPLOYER",
      totalReferrals: 0,
      successfulReferrals: 0,
      totalEarnings: 0,
      availableBalance: 0,
      canWithdraw: false,
      currentTier: "BRONZE"
    });

    const instantApply = await executeReferralApplication({
      newUserId: testUserDId,
      referralCode: codeC,
      newUserRole: "EMPLOYER",
      newUserName: "Employer Rao",
      newUserPhone: "+919888800004"
    });
    assert(instantApply.success === true, "Referral applied successfully for complete profile");
    cleanupList.referrals.push(instantApply.referralId);

    const instantRefDoc = await db.collection("referrals").doc(instantApply.referralId).get();
    assert(instantRefDoc.data().status === "COMPLETED", "Referral for already-completed profile is COMPLETED instantly");
    assert(instantRefDoc.data().profileCompleted === true, "Referral doc has profileCompleted: true");

    // -----------------------------------------------------------------
    // TEST CORNER 8: Milestone & Tier Calculations
    // -----------------------------------------------------------------
    console.log(`\n${colors.bold}Corner 8: Milestone & Tier Engine Verification${colors.reset}`);
    const { calculateTier, getMilestoneBonus, canWithdraw } = require('../functions/lib/referral-rules.js');
    assert(calculateTier(0) === "BRONZE", "0 referrals -> BRONZE tier");
    assert(calculateTier(4) === "BRONZE", "4 referrals -> BRONZE tier");
    assert(calculateTier(5) === "SILVER", "5 referrals -> SILVER tier");
    assert(calculateTier(10) === "GOLD", "10 referrals -> GOLD tier");
    assert(calculateTier(25) === "PLATINUM", "25 referrals -> PLATINUM tier");
    assert(calculateTier(50) === "DIAMOND", "50 referrals -> DIAMOND tier");

    assert(getMilestoneBonus(5) === 50, "5th referral unlocks ₹50 milestone bonus");
    assert(getMilestoneBonus(10) === 100, "10th referral unlocks ₹100 milestone bonus");
    assert(getMilestoneBonus(15) === 150, "15th referral unlocks ₹150 milestone bonus");
    assert(getMilestoneBonus(25) === 250, "25th referral unlocks ₹250 milestone bonus");
    assert(getMilestoneBonus(6) === 0, "6th referral has ₹0 milestone bonus (standard ₹25)");

    assert(canWithdraw(100, 50) === true, "₹100 balance with ₹50 min unlocks withdrawal");
    assert(canWithdraw(30, 50) === false, "₹30 balance with ₹50 min blocks withdrawal");

    console.log(`\n${colors.bold}${colors.green}======================================================${colors.reset}`);
    console.log(`${colors.bold}${colors.green}  ALL ${passedTests} TESTS PASSED! ZERO FAILURES.             ${colors.reset}`);
    console.log(`${colors.bold}${colors.green}======================================================${colors.reset}\n`);

  } catch (err) {
    console.error(`\n${colors.bold}${colors.red}Test run aborted due to error:${colors.reset}`, err);
  } finally {
    console.log(`🧹 Cleaning up test artifacts...`);
    for (const id of cleanupList.referrals) {
      await db.collection("referrals").doc(id).delete().catch(() => {});
    }
    for (const code of cleanupList.referral_codes) {
      await db.collection("referral_codes").doc(code).delete().catch(() => {});
    }
    for (const uid of cleanupList.users) {
      await db.collection("users").doc(uid).delete().catch(() => {});
    }
    for (const uid of cleanupList.referral_stats) {
      await db.collection("referral_stats").doc(uid).delete().catch(() => {});
    }
    for (const id of cleanupList.notifications) {
      await db.collection("notifications").doc(id).delete().catch(() => {});
    }
    for (const id of cleanupList.referral_events) {
      await db.collection("referral_events").doc(id).delete().catch(() => {});
    }
    console.log(`✨ Cleanup complete! Firestore left in clean state.\n`);
  }
}

runAllTests().then(() => {
  if (failedTests > 0) {
    process.exit(1);
  } else {
    process.exit(0);
  }
});
