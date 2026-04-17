/**
 * DutyPe - Job Re-seeding Script
 * 
 * This script deletes old seeded jobs and re-uploads with correct createdAt field.
 * Run with: node reseed-jobs.js
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Check if service account key exists - try multiple possible filenames
const possibleKeyFiles = [
  'serviceAccountKey.json',
  'dutype-860ac-firebase-adminsdk.json'
];

let serviceAccountPath = null;
for (const filename of possibleKeyFiles) {
  const testPath = path.join(__dirname, filename);
  if (fs.existsSync(testPath)) {
    serviceAccountPath = testPath;
    console.log(`✅ Found service account key: ${filename}`);
    break;
  }
}

if (!serviceAccountPath) {
  console.error('❌ ERROR: Service account key not found!');
  process.exit(1);
}

// Initialize Firebase Admin with service account
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutype-860ac'
});

const db = admin.firestore();

async function deleteOldSeededJobs() {
  console.log('🗑️ Deleting old seeded jobs...');
  
  // Get all jobs that were seeded (they have employerId starting with EMP_SEED_)
  const snapshot = await db.collection('jobs')
    .where('employerId', '>=', 'EMP_SEED_')
    .where('employerId', '<=', 'EMP_SEED_\uf8ff')
    .get();
  
  console.log(`📊 Found ${snapshot.size} seeded jobs to delete`);
  
  // Delete in batches of 500 (Firestore limit)
  const batchSize = 500;
  let deleted = 0;
  
  while (deleted < snapshot.size) {
    const batch = db.batch();
    const docs = snapshot.docs.slice(deleted, deleted + batchSize);
    
    docs.forEach(doc => {
      batch.delete(doc.ref);
    });
    
    await batch.commit();
    deleted += docs.length;
    console.log(`   ✅ Deleted ${deleted}/${snapshot.size} jobs`);
  }
  
  console.log('✅ All old seeded jobs deleted');
}

// Run deletion
deleteOldSeededJobs()
  .then(() => {
    console.log('\n✅ Deletion complete. Now run: node seed-jobs.js');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
