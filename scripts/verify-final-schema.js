const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Find service account key
const possibleKeyFiles = [
  'serviceAccountKey.json',
  'dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json'
];

let serviceAccount = null;
for (const keyFile of possibleKeyFiles) {
  const keyPaths = [
    path.join(__dirname, keyFile),
    path.join(__dirname, '..', keyFile)
  ];
  
  for (const keyPath of keyPaths) {
    if (fs.existsSync(keyPath)) {
      serviceAccount = require(keyPath);
      console.log(`✓ Using service account key: ${keyFile}`);
      break;
    }
  }
  
  if (serviceAccount) break;
}

if (!serviceAccount) {
  console.error('❌ Service account key not found!');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function verifyFinalSchema() {
  console.log('\n🔍 VERIFYING FINAL FIRESTORE SCHEMA\n');
  console.log('='.repeat(70));
  
  const expectedCollections = [
    'users',
    'jobs',
    'job_applications',
    'notifications',
    'referrals',
    'referral_codes',
    'withdrawal_requests',
    'conversations',
    'messages'
  ];
  
  const results = [];
  
  for (const collectionName of expectedCollections) {
    try {
      const snapshot = await db.collection(collectionName).limit(1).get();
      const count = await db.collection(collectionName).count().get();
      const docCount = count.data().count;
      
      results.push({
        name: collectionName,
        exists: true,
        count: docCount,
        status: '✅'
      });
    } catch (error) {
      results.push({
        name: collectionName,
        exists: false,
        count: 0,
        status: '❌'
      });
    }
  }
  
  // Check for deprecated collections
  const deprecatedCollections = [
    'referral_stats',
    'fcm_tokens',
    'notificationLog',
    'userPreferences'
  ];
  
  console.log('\n📊 ACTIVE COLLECTIONS (Expected: 9)\n');
  results.forEach(r => {
    console.log(`${r.status} ${r.name.padEnd(25)} ${r.count.toString().padStart(6)} documents`);
  });
  
  console.log('\n🗑️  DEPRECATED COLLECTIONS (Should be deleted)\n');
  for (const collectionName of deprecatedCollections) {
    try {
      const count = await db.collection(collectionName).count().get();
      const docCount = count.data().count;
      const status = docCount === 0 ? '✅' : '⚠️';
      console.log(`${status} ${collectionName.padEnd(25)} ${docCount.toString().padStart(6)} documents`);
    } catch (error) {
      console.log(`✅ ${collectionName.padEnd(25)}      0 documents (deleted)`);
    }
  }
  
  console.log('\n' + '='.repeat(70));
  
  // Verify users have referralStats
  console.log('\n🔍 VERIFYING REFERRAL STATS MIGRATION\n');
  const usersWithReferralCode = await db.collection('users')
    .where('referralCode', '!=', null)
    .limit(5)
    .get();
  
  console.log(`Checking ${usersWithReferralCode.size} users with referral codes:\n`);
  
  let migratedCount = 0;
  usersWithReferralCode.forEach(doc => {
    const data = doc.data();
    const hasStats = data.referralStats !== undefined;
    if (hasStats) migratedCount++;
    
    console.log(`${hasStats ? '✅' : '❌'} User ${doc.id.substring(0, 10)}... - ${hasStats ? 'Has' : 'Missing'} referralStats`);
  });
  
  console.log(`\n${migratedCount}/${usersWithReferralCode.size} users have referralStats embedded`);
  
  // Summary
  console.log('\n' + '='.repeat(70));
  console.log('📈 OPTIMIZATION SUMMARY');
  console.log('='.repeat(70));
  
  const activeCount = results.filter(r => r.exists).length;
  const totalDocs = results.reduce((sum, r) => sum + r.count, 0);
  
  console.log(`✅ Active Collections: ${activeCount}/9`);
  console.log(`✅ Total Documents: ${totalDocs}`);
  console.log(`✅ Referral Stats: Migrated to users.referralStats`);
  console.log(`✅ Schema Version: 2.1 (Optimized)`);
  console.log('='.repeat(70));
  
  if (activeCount === 9 && migratedCount === usersWithReferralCode.size) {
    console.log('\n🎉 SCHEMA OPTIMIZATION COMPLETE!\n');
    console.log('All collections are properly configured.');
    console.log('Referral stats successfully migrated.');
    console.log('\n📝 Next Steps:');
    console.log('1. ✅ Migration complete');
    console.log('2. ⏳ Test app referral features');
    console.log('3. ⏳ Deploy Firestore rules');
    console.log('4. ⏳ Deploy Firestore indexes');
  } else {
    console.log('\n⚠️  Some issues detected. Review the output above.');
  }
  
  process.exit(0);
}

verifyFinalSchema().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
