/**
 * Referral System Testing Script
 * 
 * This script helps test the referral system by checking:
 * 1. User's referral code exists
 * 2. Referral stats are properly initialized
 * 3. Pending/completed referrals
 * 4. Balance and earnings
 * 5. Notifications sent
 * 
 * Usage:
 * node test-referral-system.js <userId>
 * 
 * Example:
 * node test-referral-system.js abc123xyz
 */

const admin = require('firebase-admin');
const serviceAccount = require('./dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json');

// Initialize Firebase Admin
if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: 'https://dutypeapp.firebaseio.com'
    });
}

const db = admin.firestore();

// Colors for console output
const colors = {
    reset: '\x1b[0m',
    bright: '\x1b[1m',
    green: '\x1b[32m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    cyan: '\x1b[36m'
};

function log(message, color = 'reset') {
    console.log(`${colors[color]}${message}${colors.reset}`);
}

function section(title) {
    console.log('\n' + '='.repeat(60));
    log(title, 'bright');
    console.log('='.repeat(60));
}

async function testReferralSystem(userId) {
    try {
        section('🎁 REFERRAL SYSTEM TEST');
        log(`Testing for User ID: ${userId}`, 'cyan');

        // 1. Check if user exists
        section('1️⃣ USER PROFILE CHECK');
        const userDoc = await db.collection('users').doc(userId).get();
        
        if (!userDoc.exists) {
            log('❌ User not found!', 'red');
            return;
        }

        const userData = userDoc.data();
        log(`✅ User found: ${userData.fullName || userData.name || 'Unknown'}`, 'green');
        log(`   Role: ${userData.role || 'Unknown'}`, 'blue');
        log(`   Phone: ${userData.phoneNumber || 'N/A'}`, 'blue');
        log(`   Profile Complete: ${userData.profileCompleted ? 'Yes' : 'No'}`, userData.profileCompleted ? 'green' : 'yellow');

        // 2. Check referral code
        section('2️⃣ REFERRAL CODE CHECK');
        const statsDoc = await db.collection('referral_stats').doc(userId).get();
        
        if (!statsDoc.exists) {
            log('❌ No referral stats found - User may not have completed profile', 'red');
            log('   Referral code is created when profile is completed', 'yellow');
        } else {
            const stats = statsDoc.data();
            log(`✅ Referral code: ${stats.referralCode}`, 'green');
            log(`   Total Referrals: ${stats.totalReferrals || 0}`, 'blue');
            log(`   Successful: ${stats.successfulReferrals || 0}`, 'green');
            log(`   Pending: ${stats.pendingReferrals || 0}`, 'yellow');
            log(`   Expired: ${stats.expiredReferrals || 0}`, 'red');
            log(`   Total Earnings: ₹${stats.totalEarnings || 0}`, 'cyan');
            log(`   Available Balance: ₹${stats.availableBalance || 0}`, 'green');
            log(`   Withdrawn: ₹${stats.withdrawnAmount || 0}`, 'blue');
            log(`   Current Tier: ${stats.currentTier || 'BRONZE'}`, 'cyan');
            log(`   Can Withdraw: ${stats.canWithdraw ? 'Yes' : 'No'}`, stats.canWithdraw ? 'green' : 'yellow');
            log(`   Next Milestone: ${stats.nextMilestone || 5}`, 'blue');

            // Check if code exists in referral_codes collection
            if (stats.referralCode) {
                const codeDoc = await db.collection('referral_codes').doc(stats.referralCode).get();
                if (codeDoc.exists) {
                    const codeData = codeDoc.data();
                    log(`   Code Active: ${codeData.isActive ? 'Yes' : 'No'}`, codeData.isActive ? 'green' : 'red');
                    log(`   Times Used: ${codeData.totalUsed || 0}`, 'blue');
                } else {
                    log('   ⚠️ Code not found in referral_codes collection', 'yellow');
                }
            }
        }

        // 3. Check if user was referred by someone
        section('3️⃣ REFERRED BY CHECK');
        if (statsDoc.exists) {
            const stats = statsDoc.data();
            if (stats.referredByCode) {
                log(`✅ User was referred with code: ${stats.referredByCode}`, 'green');
                log(`   Referred by User ID: ${stats.referredByUserId || 'Unknown'}`, 'blue');
                log(`   Signup Bonus Received: ${stats.signupBonusReceived ? 'Yes' : 'No'}`, stats.signupBonusReceived ? 'green' : 'yellow');
                if (stats.signupBonusAmount) {
                    log(`   Signup Bonus Amount: ₹${stats.signupBonusAmount}`, 'cyan');
                }
            } else {
                log('ℹ️ User was not referred by anyone', 'blue');
            }
        }

        // 4. Check referrals made by this user
        section('4️⃣ REFERRALS MADE BY USER');
        const referralsSnapshot = await db.collection('referrals')
            .where('referrerUserId', '==', userId)
            .orderBy('createdAt', 'desc')
            .limit(10)
            .get();

        if (referralsSnapshot.empty) {
            log('ℹ️ No referrals made yet', 'blue');
        } else {
            log(`✅ Found ${referralsSnapshot.size} referral(s)`, 'green');
            referralsSnapshot.forEach((doc, index) => {
                const ref = doc.data();
                const statusColor = ref.status === 'COMPLETED' ? 'green' : 
                                   ref.status === 'PENDING' ? 'yellow' : 'red';
                log(`\n   Referral ${index + 1}:`, 'bright');
                log(`   - ID: ${doc.id}`, 'blue');
                log(`   - Status: ${ref.status}`, statusColor);
                log(`   - Referred User: ${ref.referredUserId}`, 'blue');
                log(`   - Referred Name: ${ref.referredUserName || 'Unknown'}`, 'blue');
                log(`   - Code Used: ${ref.referralCode}`, 'cyan');
                log(`   - Reward: ₹${ref.rewardAmount || 0}`, 'green');
                log(`   - Bonus: ₹${ref.bonusAmount || 0}`, 'cyan');
                log(`   - Created: ${ref.createdAt ? new Date(ref.createdAt.toMillis()).toLocaleString() : 'Unknown'}`, 'blue');
                if (ref.completedAt) {
                    log(`   - Completed: ${new Date(ref.completedAt.toMillis()).toLocaleString()}`, 'green');
                }
            });
        }

        // 5. Check notifications
        section('5️⃣ REFERRAL NOTIFICATIONS');
        const notificationsSnapshot = await db.collection('notifications')
            .where('recipientId', '==', userId)
            .where('type', 'in', ['REFERRAL_REWARD', 'SIGNUP_BONUS'])
            .orderBy('createdAt', 'desc')
            .limit(5)
            .get();

        if (notificationsSnapshot.empty) {
            log('ℹ️ No referral notifications found', 'blue');
        } else {
            log(`✅ Found ${notificationsSnapshot.size} notification(s)`, 'green');
            notificationsSnapshot.forEach((doc, index) => {
                const notif = doc.data();
                log(`\n   Notification ${index + 1}:`, 'bright');
                log(`   - Type: ${notif.type}`, 'cyan');
                log(`   - Title: ${notif.title}`, 'blue');
                log(`   - Message: ${notif.message}`, 'blue');
                log(`   - Read: ${notif.isRead ? 'Yes' : 'No'}`, notif.isRead ? 'green' : 'yellow');
                log(`   - Created: ${notif.createdAt ? new Date(notif.createdAt.toMillis()).toLocaleString() : 'Unknown'}`, 'blue');
            });
        }

        // 6. Check withdrawal requests
        section('6️⃣ WITHDRAWAL REQUESTS');
        const withdrawalsSnapshot = await db.collection('withdrawal_requests')
            .where('userId', '==', userId)
            .orderBy('createdAt', 'desc')
            .limit(5)
            .get();

        if (withdrawalsSnapshot.empty) {
            log('ℹ️ No withdrawal requests found', 'blue');
        } else {
            log(`✅ Found ${withdrawalsSnapshot.size} withdrawal(s)`, 'green');
            withdrawalsSnapshot.forEach((doc, index) => {
                const withdrawal = doc.data();
                const statusColor = withdrawal.status === 'COMPLETED' ? 'green' : 
                                   withdrawal.status === 'PENDING' ? 'yellow' : 'red';
                log(`\n   Withdrawal ${index + 1}:`, 'bright');
                log(`   - ID: ${doc.id}`, 'blue');
                log(`   - Status: ${withdrawal.status}`, statusColor);
                log(`   - Amount: ₹${withdrawal.amount}`, 'cyan');
                log(`   - Method: ${withdrawal.paymentMethod}`, 'blue');
                log(`   - Created: ${withdrawal.createdAt ? new Date(withdrawal.createdAt.toMillis()).toLocaleString() : 'Unknown'}`, 'blue');
            });
        }

        // 7. Summary
        section('📊 SUMMARY');
        if (statsDoc.exists) {
            const stats = statsDoc.data();
            log('Referral System Status: ✅ ACTIVE', 'green');
            log(`Total Earnings: ₹${stats.totalEarnings || 0}`, 'cyan');
            log(`Available to Withdraw: ₹${stats.availableBalance || 0}`, 'green');
            log(`Successful Referrals: ${stats.successfulReferrals || 0}`, 'green');
            
            if (stats.successfulReferrals >= 5) {
                log('🎉 Eligible for withdrawal!', 'green');
            } else {
                log(`Need ${5 - (stats.successfulReferrals || 0)} more referrals to withdraw`, 'yellow');
            }
        } else {
            log('Referral System Status: ⚠️ NOT INITIALIZED', 'yellow');
            log('Complete profile to activate referral system', 'blue');
        }

        section('✅ TEST COMPLETE');

    } catch (error) {
        log(`\n❌ Error: ${error.message}`, 'red');
        console.error(error);
    }
}

// Main execution
const userId = process.argv[2];

if (!userId) {
    log('❌ Please provide a user ID', 'red');
    log('Usage: node test-referral-system.js <userId>', 'yellow');
    log('Example: node test-referral-system.js abc123xyz', 'blue');
    process.exit(1);
}

testReferralSystem(userId)
    .then(() => {
        log('\n✅ Script completed successfully', 'green');
        process.exit(0);
    })
    .catch((error) => {
        log(`\n❌ Script failed: ${error.message}`, 'red');
        process.exit(1);
    });
