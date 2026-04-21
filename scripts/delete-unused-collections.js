const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

// Initialize Firebase Admin
// Use the same service-account resolution strategy as other maintenance scripts.
if (!admin.apps.length) {
  try {
    try {
      const serviceAccount = loadServiceAccount();
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        projectId: serviceAccount.project_id || 'dutype-860ac'
      });
      console.log(`✅ Firebase Admin initialized with service account project: ${serviceAccount.project_id || 'dutype-860ac'}`);
    } catch (serviceAccountError) {
      admin.initializeApp({ projectId: 'dutype-860ac' });
      console.log('✅ Firebase Admin initialized with default credentials for project: dutype-860ac');
    }
  } catch (error) {
    console.error('❌ Failed to initialize Firebase Admin:', error.message);
    console.log('\n💡 To fix this, set FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH / FIREBASE_ADMIN_SERVICE_ACCOUNT_JSON or configure Application Default Credentials');
    process.exit(1);
  }
}

const db = admin.firestore();

/**
 * COLLECTIONS TO DELETE (NOT USED IN CODEBASE):
 * 
 * 1. _rate_limits - NOT USED (legacy internal rate-limit collection)
 * 2. rate_limits - NOT USED (legacy alias)
 * 3. payment_transactions - NOT USED (no code references)
 * 4. broadcast_notifications - NOT USED (no code references)
 * 5. moderation_queue - NOT USED (no code references)
 * 6. activity_logs - NOT USED (no code references)
 * 7. suspicious_ips - NOT USED (no code references)
 * 8. subscription_usage - NOT USED (no code references)
 * 
 * COLLECTIONS TO KEEP (ACTIVELY USED):
 * - blacklists - USED in BlacklistService.kt
 * - subscriptions - USED in UserMetadata.kt
 * - notificationLog - USED in NotificationScheduler.kt
 * - achievements - USED in UserMetadata.kt and ApplicationDetailScreen.kt
 * - announcements - USED in WorkerHomeScreen.kt
 * - dismissed_announcements - USED in AnnouncementService
 * - notification_tracking - USED in NotificationScheduler
 * - app_feedback - USED in FeedbackBottomSheet
 * - referral_* collections - USED in ReferralService
 * - withdrawal_requests - USED in ReferralService
 */

const COLLECTIONS_TO_DELETE = [
  '_rate_limits',
  'rate_limits',
  'payment_transactions',
  'broadcast_notifications',
  'moderation_queue',
  'activity_logs',
  'suspicious_ips',
  'subscription_usage'
];

async function deleteCollection(collectionName) {
  console.log(`\n🗑️  Deleting collection: ${collectionName}`);
  
  try {
    const snapshot = await db.collection(collectionName).get();
    
    if (snapshot.empty) {
      console.log(`   ✅ Collection "${collectionName}" is already empty or doesn't exist`);
      return { collection: collectionName, deleted: 0, status: 'empty' };
    }
    
    console.log(`   📊 Found ${snapshot.size} documents in "${collectionName}"`);
    
    // Delete in batches of 500 (Firestore limit)
    const batchSize = 500;
    let deletedCount = 0;
    
    while (true) {
      const batch = db.batch();
      const docs = await db.collection(collectionName).limit(batchSize).get();
      
      if (docs.empty) break;
      
      docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      
      await batch.commit();
      deletedCount += docs.size;
      console.log(`   🔄 Deleted ${deletedCount} documents...`);
      
      if (docs.size < batchSize) break;
    }
    
    console.log(`   ✅ Successfully deleted ${deletedCount} documents from "${collectionName}"`);
    return { collection: collectionName, deleted: deletedCount, status: 'success' };
    
  } catch (error) {
    console.error(`   ❌ Error deleting "${collectionName}":`, error.message);
    return { collection: collectionName, deleted: 0, status: 'error', error: error.message };
  }
}

async function main() {
  console.log('🚀 Starting deletion of unused collections...\n');
  console.log('📋 Collections to delete:', COLLECTIONS_TO_DELETE.join(', '));
  
  const results = [];
  
  for (const collectionName of COLLECTIONS_TO_DELETE) {
    const result = await deleteCollection(collectionName);
    results.push(result);
  }
  
  // Summary
  console.log('\n' + '='.repeat(60));
  console.log('📊 DELETION SUMMARY');
  console.log('='.repeat(60));
  
  const totalDeleted = results.reduce((sum, r) => sum + r.deleted, 0);
  const successCount = results.filter(r => r.status === 'success').length;
  const emptyCount = results.filter(r => r.status === 'empty').length;
  const errorCount = results.filter(r => r.status === 'error').length;
  
  console.log(`\n✅ Successfully deleted: ${successCount} collections`);
  console.log(`📭 Already empty: ${emptyCount} collections`);
  console.log(`❌ Errors: ${errorCount} collections`);
  console.log(`📄 Total documents deleted: ${totalDeleted}`);
  
  console.log('\n📋 Detailed Results:');
  results.forEach(r => {
    const icon = r.status === 'success' ? '✅' : r.status === 'empty' ? '📭' : '❌';
    console.log(`${icon} ${r.collection}: ${r.deleted} docs deleted (${r.status})`);
  });
  
  console.log('\n✨ Deletion complete!');
}

main()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('❌ Fatal error:', error);
    process.exit(1);
  });
