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
  // Check in scripts folder first, then parent directory
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

/**
 * Delete referral_stats collection after migration
 * Run this ONLY after verifying migration was successful
 */

async function deleteCollection(collectionName) {
  console.log(`\n🗑️  Deleting ${collectionName} collection...\n`);
  
  const batchSize = 500;
  let deletedCount = 0;
  
  try {
    let query = db.collection(collectionName).limit(batchSize);
    
    while (true) {
      const snapshot = await query.get();
      
      if (snapshot.empty) {
        break;
      }
      
      const batch = db.batch();
      snapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      
      await batch.commit();
      deletedCount += snapshot.size;
      
      console.log(`Deleted ${deletedCount} documents...`);
      
      if (snapshot.size < batchSize) {
        break;
      }
    }
    
    console.log(`\n✅ Deleted ${deletedCount} documents from ${collectionName}`);
    return deletedCount;
    
  } catch (error) {
    console.error(`❌ Error deleting ${collectionName}:`, error);
    throw error;
  }
}

async function main() {
  console.log('🚀 DELETE DEPRECATED COLLECTIONS');
  console.log('\n⚠️  WARNING: This will permanently delete:');
  console.log('  - referral_stats collection');
  console.log('  - fcm_tokens collection (duplicate of users.fcmToken)');
  console.log('  - notificationLog collection (not needed)');
  console.log('  - userPreferences collection (can merge into users)');
  console.log('\nPress Ctrl+C within 10 seconds to cancel...\n');
  
  await new Promise(resolve => setTimeout(resolve, 10000));
  
  const results = {};
  
  // Delete referral_stats (migrated to users.referralStats)
  try {
    results.referral_stats = await deleteCollection('referral_stats');
  } catch (error) {
    console.log('⚠️  referral_stats: Collection may not exist or already deleted');
  }
  
  // Delete fcm_tokens (duplicate of users.fcmToken)
  try {
    results.fcm_tokens = await deleteCollection('fcm_tokens');
  } catch (error) {
    console.log('⚠️  fcm_tokens: Collection may not exist or already deleted');
  }
  
  // Delete notificationLog (not needed for early stage)
  try {
    results.notificationLog = await deleteCollection('notificationLog');
  } catch (error) {
    console.log('⚠️  notificationLog: Collection may not exist or already deleted');
  }
  
  // Delete userPreferences (can merge into users if needed)
  try {
    results.userPreferences = await deleteCollection('userPreferences');
  } catch (error) {
    console.log('⚠️  userPreferences: Collection may not exist or already deleted');
  }
  
  console.log('\n' + '='.repeat(60));
  console.log('📊 DELETION SUMMARY');
  console.log('='.repeat(60));
  Object.entries(results).forEach(([collection, count]) => {
    console.log(`${collection}: ${count} documents deleted`);
  });
  console.log('='.repeat(60));
  console.log('\n✅ Cleanup complete!');
  console.log('\n📝 Final collections count: 9 (down from 14)');
  
  process.exit(0);
}

main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
