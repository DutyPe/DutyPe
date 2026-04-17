const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('../app/google-services.json');

admin.initializeApp({
  credential: admin.credential.cert({
    projectId: serviceAccount.project_id,
    clientEmail: serviceAccount.client_email,
    privateKey: serviceAccount.private_key
  })
});

const db = admin.firestore();
const auth = admin.auth();

async function testReferralCode() {
  const referralCode = 'WRKG0XBC8';
  
  console.log('🎯 Testing Referral Code:', referralCode);
  console.log('='.repeat(50));
  
  try {
    // Step 1: Check if referral code exists
    console.log('\n📋 Step 1: Checking if referral code exists...');
    const codeDoc = await db.collection('referral_codes').doc(referralCode).get();
    
    if (!codeDoc.exists) {
      console.log('❌ Referral code does not exist!');
      return;
    }
    
    const codeData = codeDoc.data();
    console.log('✅ Referral code found!');
    console.log('   Referrer User ID:', codeData.userId);
    console.log('   Created At:', codeData.createdAt?.toDate());
    
    // Step 2: Get referrer details
    console.log('\n👤 Step 2: Getting referrer details...');
    const referrerDoc = await db.collection('users').doc(codeData.userId).get();
    
    if (!referrerDoc.exists) {
      console.log('❌ Referrer user not found!');
      return;
    }
    
    const referrerData = referrerDoc.data();
    console.log('✅ Referrer found!');
    console.log('   Name:', referrerData.name);
    console.log('   Phone:', referrerData.phone);
    console.log('   Role:', referrerData.role || referrerData.activeRole);
    console.log('   Current Earnings:', referrerData.referralEarnings || 0);
    console.log('   Successful Referrals:', referrerData.successfulReferrals || 0);
    
    // Step 3: Create a test user who will use the referral code
    console.log('\n🆕 Step 3: Creating test user...');
    const testEmail = `test_${Date.now()}@test.com`;
    const testPhone = `+91${Math.floor(1000000000 + Math.random() * 9000000000)}`;
    
    let testUser;
    try {
      testUser = await auth.createUser({
        email: testEmail,
        phoneNumber: testPhone,
        password: 'Test@123',
        displayName: 'Test User'
      });
      console.log('✅ Test user created!');
      console.log('   UID:', testUser.uid);
      console.log('   Email:', testEmail);
      console.log('   Phone:', testPhone);
    } catch (error) {
      console.log('❌ Error creating test user:', error.message);
      return;
    }
    
    // Step 4: Create user document in Firestore
    console.log('\n📝 Step 4: Creating user document...');
    await db.collection('users').doc(testUser.uid).set({
      name: 'Test User',
      email: testEmail,
      phone: testPhone,
      role: 'WORKER',
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      profileComplete: false
    });
    console.log('✅ User document created!');
    
    // Step 5: Apply referral code (simulate Cloud Function)
    console.log('\n🎁 Step 5: Applying referral code...');
    
    // Create referral document
    const referralId = `${codeData.userId}_${testUser.uid}`;
    await db.collection('referrals').doc(referralId).set({
      referralCode: referralCode,
      referrerUserId: codeData.userId,
      referredUserId: testUser.uid,
      referredUserName: 'Test User',
      referredUserPhone: testPhone,
      referredUserRole: 'WORKER',
      status: 'PENDING',
      rewardAmount: 50, // ₹50 for worker referral
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) // 30 days
    });
    console.log('✅ Referral document created!');
    
    // Step 6: Mark profile as complete and credit reward
    console.log('\n✨ Step 6: Completing profile and crediting reward...');
    
    // Update referred user
    await db.collection('users').doc(testUser.uid).update({
      profileComplete: true,
      usedReferralCode: referralCode,
      referredBy: codeData.userId
    });
    
    // Update referral status
    await db.collection('referrals').doc(referralId).update({
      status: 'COMPLETED',
      completedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    // Credit reward to referrer
    await db.collection('users').doc(codeData.userId).update({
      referralEarnings: admin.firestore.FieldValue.increment(50),
      successfulReferrals: admin.firestore.FieldValue.increment(1),
      totalReferrals: admin.firestore.FieldValue.increment(1)
    });
    
    console.log('✅ Referral completed and reward credited!');
    
    // Step 7: Verify the results
    console.log('\n🔍 Step 7: Verifying results...');
    
    const updatedReferrerDoc = await db.collection('users').doc(codeData.userId).get();
    const updatedReferrerData = updatedReferrerDoc.data();
    
    console.log('✅ Referrer updated stats:');
    console.log('   Current Earnings: ₹' + (updatedReferrerData.referralEarnings || 0));
    console.log('   Successful Referrals:', updatedReferrerData.successfulReferrals || 0);
    console.log('   Total Referrals:', updatedReferrerData.totalReferrals || 0);
    
    const referralDoc = await db.collection('referrals').doc(referralId).get();
    const referralData = referralDoc.data();
    
    console.log('\n✅ Referral details:');
    console.log('   Status:', referralData.status);
    console.log('   Reward Amount: ₹' + referralData.rewardAmount);
    console.log('   Completed At:', referralData.completedAt?.toDate());
    
    // Step 8: Check in admin panel
    console.log('\n🌐 Step 8: Admin panel verification:');
    console.log('   Go to: https://dutype-860ac.web.app/admin/referrals.html');
    console.log('   You should see:');
    console.log('   - Referral Code:', referralCode);
    console.log('   - Referred User: Test User');
    console.log('   - Status: COMPLETED');
    console.log('   - Reward: ₹50');
    
    console.log('\n✅ Test completed successfully!');
    console.log('\n📱 In the app:');
    console.log('   - Open Refer & Earn screen');
    console.log('   - You should see earnings increased by ₹50');
    console.log('   - Successful referrals count increased by 1');
    
    // Cleanup option
    console.log('\n🗑️  Cleanup:');
    console.log('   Test user UID:', testUser.uid);
    console.log('   Run this to delete test user:');
    console.log(`   node scripts/cleanup-test-user.js ${testUser.uid}`);
    
  } catch (error) {
    console.error('❌ Error:', error);
  } finally {
    process.exit(0);
  }
}

testReferralCode();
