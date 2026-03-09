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
  const keyPath = path.join(__dirname, '..', keyFile);
  if (fs.existsSync(keyPath)) {
    serviceAccount = require(keyPath);
    console.log(`✓ Using service account key: ${keyFile}`);
    break;
  }
}

if (!serviceAccount) {
  console.error('❌ Service account key not found!');
  console.error('Tried:', possibleKeyFiles.join(', '));
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

/**
 * FINAL SCHEMA CLEANUP - Remove ALL unnecessary fields
 * Keep ONLY the minimal fields defined in models
 */

// Define minimal schemas
const SCHEMAS = {
  users: [
    'id', 'phone', 'fullName', 'profileImageUrl', 'email',
    'roles', 'activeRole', 'latitude', 'longitude', 'address',
    'bio', 'skills', 'experience', 'companyName', 'trustTier',
    'fcmToken', 'createdAt', 'isActive', 'profileCompleted'
  ],
  jobs: [
    'id', 'employerId', 'title', 'companyName', 'description',
    'location', 'latitude', 'longitude', 'payAmount', 'payType',
    'shiftTiming', 'isActive', 'isFilled', 'postedAt',
    'contactNumber', 'vacancies', 'jobType', 'gender'
  ],
  job_applications: [
    'id', 'jobId', 'workerId', 'employerId', 'status',
    'appliedAt', 'updatedAt', 'jobTitle', 'jobLocation',
    'companyName', 'workerName', 'coverLetter', 'source'
  ],
  notifications: [
    'id', 'recipientId', 'title', 'message', 'type',
    'data', 'createdAt', 'isRead'
  ],
  referrals: [
    'id', 'referrerUserId', 'referredUserId', 'referralCode',
    'status', 'rewardAmount', 'bonusAmount', 'createdAt',
    'completedAt', 'deviceFingerprint'
  ],
  referral_codes: [
    'code', 'userId', 'userRole', 'userName', 'isActive', 'createdAt'
  ],
  referral_stats: [
    'userId', 'referralCode', 'totalReferrals', 'successfulReferrals',
    'totalEarnings', 'availableBalance', 'withdrawnAmount',
    'canWithdraw', 'lastUpdated'
  ],
  withdrawal_requests: [
    'id', 'userId', 'amount', 'status', 'paymentMethod',
    'upiId', 'createdAt', 'processedAt', 'transactionId'
  ],
  conversations: [
    'id', 'participants', 'lastMessage', 'lastMessageAt', 'unreadCount', 'updatedAt'
  ],
  messages: [
    'id', 'conversationId', 'senderId', 'text', 'timestamp', 'type'
  ]
};

async function cleanupCollection(collectionName, allowedFields) {
  console.log(`\n🧹 Cleaning ${collectionName}...`);
  
  const snapshot = await db.collection(collectionName).get();
  let processed = 0;
  let updated = 0;
  
  const batch = db.batch();
  let batchCount = 0;
  
  for (const doc of snapshot.docs) {
    const data = doc.data();
    const fieldsToRemove = {};
    let hasChanges = false;
    
    // Find fields not in allowed list
    for (const field of Object.keys(data)) {
      if (!allowedFields.includes(field)) {
        fieldsToRemove[field] = admin.firestore.FieldValue.delete();
        hasChanges = true;
      }
    }
    
    if (hasChanges) {
      batch.update(doc.ref, fieldsToRemove);
      updated++;
      batchCount++;
      
      if (batchCount >= 500) {
        await batch.commit();
        console.log(`  ✓ Committed batch of ${batchCount} updates`);
        batchCount = 0;
      }
    }
    
    processed++;
    if (processed % 100 === 0) {
      console.log(`  Processed ${processed}/${snapshot.size} documents...`);
    }
  }
  
  if (batchCount > 0) {
    await batch.commit();
    console.log(`  ✓ Committed final batch of ${batchCount} updates`);
  }
  
  console.log(`✅ ${collectionName}: ${updated}/${processed} documents cleaned`);
  return { processed, updated };
}

async function main() {
  console.log('🚀 Starting FINAL schema cleanup...\n');
  console.log('This will remove ALL fields not in the minimal schema');
  console.log('Press Ctrl+C within 5 seconds to cancel...\n');
  
  await new Promise(resolve => setTimeout(resolve, 5000));
  
  const startTime = Date.now();
  const results = {};
  
  for (const [collection, fields] of Object.entries(SCHEMAS)) {
    try {
      results[collection] = await cleanupCollection(collection, fields);
    } catch (error) {
      console.error(`❌ Error cleaning ${collection}:`, error.message);
      results[collection] = { processed: 0, updated: 0, error: error.message };
    }
  }
  
  const duration = ((Date.now() - startTime) / 1000).toFixed(2);
  
  console.log('\n' + '='.repeat(60));
  console.log('📊 FINAL CLEANUP SUMMARY');
  console.log('='.repeat(60));
  
  let totalProcessed = 0;
  let totalUpdated = 0;
  
  for (const [collection, result] of Object.entries(results)) {
    console.log(`${collection}: ${result.updated}/${result.processed} cleaned`);
    totalProcessed += result.processed;
    totalUpdated += result.updated;
  }
  
  console.log('='.repeat(60));
  console.log(`Total: ${totalUpdated}/${totalProcessed} documents cleaned`);
  console.log(`Duration: ${duration}s`);
  console.log('='.repeat(60));
  
  process.exit(0);
}

main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
