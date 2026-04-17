/**
 * Quick Referral System Status Check
 * Shows overview of referral system state
 */

const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');
const serviceAccount = loadServiceAccount();

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}

const db = admin.firestore();

async function checkStatus() {
    console.log('\n🎁 REFERRAL SYSTEM STATUS CHECK\n');
    console.log('='.repeat(60));

    try {
        // 1. Count referral codes
        const codesSnapshot = await db.collection('referral_codes').limit(100).get();
        console.log(`\n📋 Referral Codes: ${codesSnapshot.size} active codes`);
        
        if (codesSnapshot.size > 0) {
            console.log('\nSample codes:');
            codesSnapshot.docs.slice(0, 5).forEach(doc => {
                const data = doc.data();
                console.log(`  - ${data.code} (${data.userRole}) - Used ${data.totalUsed || 0} times`);
            });
        }

        // 2. Count referral stats
        const statsSnapshot = await db.collection('referral_stats').limit(100).get();
        console.log(`\n📊 User Stats: ${statsSnapshot.size} users with referral stats`);
        
        let totalEarnings = 0;
        let totalReferrals = 0;
        let usersWithBalance = 0;
        
        statsSnapshot.forEach(doc => {
            const data = doc.data();
            totalEarnings += data.totalEarnings || 0;
            totalReferrals += data.successfulReferrals || 0;
            if ((data.availableBalance || 0) > 0) usersWithBalance++;
        });
        
        console.log(`  - Total Earnings: ₹${totalEarnings}`);
        console.log(`  - Total Successful Referrals: ${totalReferrals}`);
        console.log(`  - Users with Balance: ${usersWithBalance}`);

        // 3. Count referrals by status
        const referralsSnapshot = await db.collection('referrals').limit(100).get();
        console.log(`\n🔗 Referrals: ${referralsSnapshot.size} total referrals`);
        
        const statusCount = { PENDING: 0, COMPLETED: 0, EXPIRED: 0, REJECTED: 0 };
        referralsSnapshot.forEach(doc => {
            const status = doc.data().status || 'UNKNOWN';
            statusCount[status] = (statusCount[status] || 0) + 1;
        });
        
        console.log(`  - Pending: ${statusCount.PENDING}`);
        console.log(`  - Completed: ${statusCount.COMPLETED}`);
        console.log(`  - Expired: ${statusCount.EXPIRED}`);
        console.log(`  - Rejected: ${statusCount.REJECTED}`);

        // 4. Count notifications
        const notifSnapshot = await db.collection('notifications')
            .where('type', 'in', ['REFERRAL_REWARD', 'SIGNUP_BONUS'])
            .limit(100)
            .get();
        console.log(`\n🔔 Notifications: ${notifSnapshot.size} referral notifications sent`);

        // 5. Count withdrawals
        const withdrawalSnapshot = await db.collection('withdrawal_requests').limit(100).get();
        console.log(`\n💰 Withdrawals: ${withdrawalSnapshot.size} withdrawal requests`);
        
        if (withdrawalSnapshot.size > 0) {
            const withdrawalStatus = { PENDING: 0, COMPLETED: 0, REJECTED: 0 };
            withdrawalSnapshot.forEach(doc => {
                const status = doc.data().status || 'UNKNOWN';
                withdrawalStatus[status] = (withdrawalStatus[status] || 0) + 1;
            });
            console.log(`  - Pending: ${withdrawalStatus.PENDING}`);
            console.log(`  - Completed: ${withdrawalStatus.COMPLETED}`);
            console.log(`  - Rejected: ${withdrawalStatus.REJECTED}`);
        }

        // 6. Recent activity
        console.log('\n📅 Recent Activity:');
        const recentReferrals = await db.collection('referrals')
            .orderBy('createdAt', 'desc')
            .limit(3)
            .get();
        
        if (recentReferrals.empty) {
            console.log('  No recent referrals');
        } else {
            recentReferrals.forEach((doc, i) => {
                const data = doc.data();
                const date = data.createdAt ? new Date(data.createdAt.toMillis()).toLocaleString() : 'Unknown';
                console.log(`  ${i + 1}. ${data.status} - ${data.referralCode} - ${date}`);
            });
        }

        console.log('\n' + '='.repeat(60));
        console.log('✅ Status check complete!\n');

        // Summary
        if (codesSnapshot.size === 0) {
            console.log('⚠️  No referral codes found. Users need to complete their profiles.');
        } else if (referralsSnapshot.size === 0) {
            console.log('ℹ️  Codes exist but no referrals yet. Share codes to test!');
        } else if (statusCount.COMPLETED > 0) {
            console.log('✅ Referral system is working! Rewards have been credited.');
        } else if (statusCount.PENDING > 0) {
            console.log('⏳ Pending referrals exist. Complete profiles to trigger rewards.');
        }

    } catch (error) {
        console.error('❌ Error:', error.message);
    }

    process.exit(0);
}

checkStatus();
