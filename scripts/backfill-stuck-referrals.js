/**
 * Backfill stuck referrals for real users whose initial referral failed
 * due to previous Firestore query indexing or missing user doc race conditions.
 */

const admin = require('../functions/node_modules/firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account.js');

admin.initializeApp({
  credential: admin.credential.cert(loadServiceAccount())
});

const db = admin.firestore();
const { executeReferralApplication, processPendingReferralOnProfileComplete } = require('../functions/lib/referral-system.js');

const CANDIDATES = [
  {
    userId: '2gkT99uQt0V1wGeEEfASwTAUgKe2',
    code: 'DUTY28AK',
    name: 'Aniket',
    phone: '+918319953475',
    role: 'WORKER'
  },
  {
    userId: '9EwTyj8Gioh4KQQ2NGmh2smUgqI3',
    code: 'DUTY28AK',
    name: 'Priyanshu Raj',
    phone: '+919661888473',
    role: 'WORKER'
  },
  {
    userId: 'OEca4wuEJPZb8c8po3y18sGYYh32',
    code: 'TABARE2219',
    name: 'Tairun',
    phone: '+918007313191',
    role: 'WORKER'
  },
  {
    userId: 'HZeZXdLKweTzNLFESUppjAGnZlH2',
    code: 'TABARE2219',
    name: 'Nadim Alam',
    phone: '+918879496056',
    role: 'EMPLOYER'
  },
  {
    userId: 'v2T008SstNNp99ikjH6PWXwRZK42',
    code: 'ROHIT5716',
    name: 'Subhash',
    phone: '+919917215809',
    role: 'WORKER'
  },
  {
    userId: 'xmS8zcrFnQX2PdsP0xmD47CYDB52',
    code: 'SHIVA1181',
    name: 'Aman',
    phone: '+918081692491',
    role: 'WORKER'
  },
  {
    userId: 'y8TeoP6upXP6nga0XWmTjsUIMtJ2',
    code: 'VENUBA9463',
    name: 'Banne Srihari',
    phone: '+919966425163',
    role: 'WORKER'
  },
  {
    userId: '6EHP2bpaYgcpUhllggUr2dXDCcG3',
    code: 'KAJAL2801',
    name: 'Ramesh',
    phone: '+919130533255',
    role: 'WORKER'
  }
];

async function runBackfill() {
  console.log('Starting backfill for', CANDIDATES.length, 'users...');

  for (const c of CANDIDATES) {
    console.log(`\nProcessing ${c.name} (${c.userId}) with code ${c.code}...`);
    try {
      const res = await executeReferralApplication({
        newUserId: c.userId,
        referralCode: c.code,
        newUserRole: c.role,
        newUserName: c.name,
        newUserPhone: c.phone,
        ipAddress: '127.0.0.1'
      });
      console.log('Application Result:', JSON.stringify(res, null, 2));

      // Re-trigger profile completion check
      const completeRes = await processPendingReferralOnProfileComplete(c.userId);
      console.log('Completion Result:', JSON.stringify(completeRes, null, 2));
    } catch (err) {
      console.error(`Failed for ${c.name}:`, err.message);
    }
  }

  console.log('\n--- VERIFYING REFERRALS COLLECTION ---');
  const refDocs = await db.collection('referrals').get();
  console.log('Total completed referrals in collection:', refDocs.size);
  refDocs.forEach(d => {
    const data = d.data();
    console.log('Referral:', d.id, 'referrer:', data.referrerUserId, 'referred:', data.referredUserName, 'amount:', data.rewardAmount, 'status:', data.status);
  });
}

runBackfill().catch(console.error);
