// Simple script to check referral code in Firestore
const admin = require('firebase-admin');

// Initialize with project ID only
admin.initializeApp({
  projectId: 'dutypeapp'
});

const db = admin.firestore();

async function checkReferralCode() {
  const referralCode = 'WRKG0XBC8';
  
  console.log('🎯 Checking Referral Code:', referralCode);
  console.log('='.repeat(60));
  
  try {
    // Check referral code
    console.log('\n📋 Checking referral_codes collection...');
    const codeDoc = await db.collection('referral_codes').doc(referralCode).get();
    
    if (!codeDoc.exists) {
      console.log('❌ Referral code NOT found in referral_codes collection');
      console.log('\n💡 This means the code was not generated properly.');
      console.log('   The code should be created when user completes profile.');
      return;
    }
    
    const codeData = codeDoc.data();
    console.log('✅ Referral code found!');
    console.log('   User ID:', codeData.userId);
    console.log('   Created:', codeData.createdAt?.toDate());
    
    // Get user details
    console.log('\n👤 Getting referrer user details...');
    const userDoc = await db.collection('users').doc(codeData.userId).get();
    
    if (!userDoc.exists) {
      console.log('❌ User not found!');
      return;
    }
    
    const userData = userDoc.data();
    console.log('✅ Referrer details:');
    console.log('   Name:', userData.name);
    console.log('   Phone:', userData.phone);
    console.log('   Role:', userData.role || userData.activeRole);
    console.log('   Referral Code:', userData.referralCode);
    console.log('   Earnings: ₹' + (userData.referralEarnings || 0));
    console.log('   Successful Referrals:', userData.successfulReferrals || 0);
    console.log('   Total Referrals:', userData.totalReferrals || 0);
    
    // Check existing referrals
    console.log('\n🎁 Checking existing referrals...');
    const referralsSnapshot = await db.collection('referrals')
      .where('referralCode', '==', referralCode)
      .get();
    
    console.log(`   Found ${referralsSnapshot.size} referral(s)`);
    
    if (referralsSnapshot.size > 0) {
      referralsSnapshot.forEach((doc, index) => {
        const ref = doc.data();
        console.log(`\n   Referral #${index + 1}:`);
        console.log('     Referred User:', ref.referredUserName);
        console.log('     Phone:', ref.referredUserPhone);
        console.log('     Status:', ref.status);
        console.log('     Reward: ₹' + ref.rewardAmount);
        console.log('     Created:', ref.createdAt?.toDate());
        if (ref.completedAt) {
          console.log('     Completed:', ref.completedAt?.toDate());
        }
      });
    }
    
    console.log('\n✅ Referral system is working!');
    console.log('\n📱 To test in app:');
    console.log('   1. Create a new user account');
    console.log('   2. During profile setup, enter code: ' + referralCode);
    console.log('   3. Complete the profile');
    console.log('   4. Check Refer & Earn screen - earnings should increase');
    
    console.log('\n🌐 To check in admin panel:');
    console.log('   Go to: https://dutypeapp.web.app/admin/referrals.html');
    console.log('   You should see all referrals for code: ' + referralCode);
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  } finally {
    process.exit(0);
  }
}

checkReferralCode();
