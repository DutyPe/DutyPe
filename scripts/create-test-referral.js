// Direct Firestore test - creates a test referral for code WRKG0XBC8
const admin = require('firebase-admin');
const serviceAccount = require('../app/google-services.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert({
    projectId: serviceAccount.project_info.project_id,
    clientEmail: `firebase-adminsdk-${serviceAccount.project_info.project_number}@${serviceAccount.project_info.project_id}.iam.gserviceaccount.com`,
    privateKey: "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDGxqYxqxqxqxqx\n-----END PRIVATE KEY-----\n"
  }),
  databaseURL: `https://${serviceAccount.project_info.project_id}.firebaseio.com`
});

const db = admin.firestore();

async function testReferral() {
  const referralCode = 'WRKG0XBC8';
  
  console.log('🎯 Testing Referral System');
  console.log('='.repeat(60));
  console.log('Referral Code:', referralCode);
  console.log('');
  
  try {
    // Step 1: Check if code exists
    console.log('📋 Step 1: Checking referral code...');
    const codeDoc = await db.collection('referral_codes').doc(referralCode).get();
    
    if (!codeDoc.exists) {
      console.log('❌ ERROR: Referral code does not exist!');
      console.log('   The code should be created when user completes profile.');
      process.exit(1);
    }
    
    const codeData = codeDoc.data();
    console.log('✅ Code exists!');
    console.log('   Referrer ID:', codeData.userId);
    
    // Step 2: Get referrer current stats
    console.log('\n👤 Step 2: Getting referrer current stats...');
    const referrerDoc = await db.collection('users').doc(codeData.userId).get();
    
    if (!referrerDoc.exists) {
      console.log('❌ ERROR: Referrer user not found!');
      process.exit(1);
    }
    
    const referrerData = referrerDoc.data();
    console.log('✅ Referrer found!');
    console.log('   Name:', referrerData.name || referrerData.fullName);
    console.log('   Phone:', referrerData.phone);
    console.log('   Current Earnings: ₹' + (referrerData.referralEarnings || 0));
    console.log('   Successful Referrals:', referrerData.successfulReferrals || 0);
    
    // Step 3: Create test referral
    console.log('\n🎁 Step 3: Creating test referral...');
    const testUserId = 'TEST_USER_' + Date.now();
    const testPhone = '+919999999999';
    const referralId = `${codeData.userId}_${testUserId}`;
    
    await db.collection('referrals').doc(referralId).set({
      referralCode: referralCode,
      referrerUserId: codeData.userId,
      referredUserId: testUserId,
      referredUserName: 'Test User',
      referredUserPhone: testPhone,
      referredUserRole: 'WORKER',
      status: 'COMPLETED',
      rewardAmount: 50,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      completedAt: admin.firestore.FieldValue.serverTimestamp(),
      expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
    });
    console.log('✅ Test referral created!');
    console.log('   Referral ID:', referralId);
    
    // Step 4: Update referrer stats
    console.log('\n💰 Step 4: Updating referrer earnings...');
    await db.collection('users').doc(codeData.userId).update({
      referralEarnings: admin.firestore.FieldValue.increment(50),
      successfulReferrals: admin.firestore.FieldValue.increment(1),
      totalReferrals: admin.firestore.FieldValue.increment(1)
    });
    console.log('✅ Earnings updated! Added ₹50');
    
    // Step 5: Verify results
    console.log('\n🔍 Step 5: Verifying results...');
    const updatedReferrerDoc = await db.collection('users').doc(codeData.userId).get();
    const updatedData = updatedReferrerDoc.data();
    
    console.log('✅ Updated Stats:');
    console.log('   New Earnings: ₹' + (updatedData.referralEarnings || 0));
    console.log('   New Successful Referrals:', updatedData.successfulReferrals || 0);
    console.log('   Increase: +₹50, +1 referral');
    
    // Step 6: Check all referrals
    console.log('\n📊 Step 6: Checking all referrals for this code...');
    const referralsSnapshot = await db.collection('referrals')
      .where('referralCode', '==', referralCode)
      .get();
    
    console.log(`✅ Total referrals: ${referralsSnapshot.size}`);
    referralsSnapshot.forEach((doc, index) => {
      const ref = doc.data();
      console.log(`\n   Referral #${index + 1}:`);
      console.log('     User:', ref.referredUserName);
      console.log('     Phone:', ref.referredUserPhone);
      console.log('     Status:', ref.status);
      console.log('     Reward: ₹' + ref.rewardAmount);
    });
    
    console.log('\n✅ TEST COMPLETED SUCCESSFULLY!');
    console.log('\n📱 Check in app:');
    console.log('   - Open Refer & Earn screen');
    console.log('   - You should see earnings: ₹' + (updatedData.referralEarnings || 0));
    console.log('   - Successful referrals: ' + (updatedData.successfulReferrals || 0));
    
    console.log('\n🌐 Check in admin panel:');
    console.log('   - Go to: https://dutype-860ac.web.app/admin/referrals.html');
    console.log('   - Or: https://dutype-860ac.web.app/admin/test-referral.html');
    console.log('   - You should see the test referral');
    
    console.log('\n🗑️  To remove test data:');
    console.log('   - Delete referral document:', referralId);
    console.log('   - Subtract ₹50 from earnings');
    console.log('   - Subtract 1 from successful referrals');
    
  } catch (error) {
    console.error('\n❌ ERROR:', error.message);
    console.error(error);
  }
  
  process.exit(0);
}

testReferral();
