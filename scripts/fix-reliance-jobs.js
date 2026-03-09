/**
 * Fix Reliance Retail jobs - add missing category and createdAt fields
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const possibleKeyFiles = [
  'serviceAccountKey.json',
  'dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json'
];

let serviceAccountPath = null;
for (const filename of possibleKeyFiles) {
  const testPath = path.join(__dirname, filename);
  if (fs.existsSync(testPath)) {
    serviceAccountPath = testPath;
    break;
  }
}

if (!serviceAccountPath) {
  console.error('❌ Service account key not found!');
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutypeapp'
});

const db = admin.firestore();

async function fixRelianceJobs() {
  console.log('🔧 Fixing Reliance Retail jobs...\n');

  const snapshot = await db.collection('jobs')
    .where('companyName', '==', 'Reliance Retail')
    .get();

  console.log(`Found ${snapshot.size} Reliance Retail jobs to fix\n`);

  const batch = db.batch();
  let count = 0;

  snapshot.forEach(doc => {
    const job = doc.data();
    const updates = {};

    // Add createdAt if missing (use postedAt value)
    if (!job.createdAt && job.postedAt) {
      updates.createdAt = job.postedAt;
    }

    // Add category if missing
    if (!job.category) {
      // Detect category from title
      const title = job.title.toLowerCase();
      if (title.includes('cashier')) {
        updates.category = 'CASHIER';
      } else if (title.includes('fashion') || title.includes('consultant')) {
        updates.category = 'OTHER';
      } else if (title.includes('customer service') || title.includes('associate')) {
        updates.category = 'OTHER';
      } else {
        updates.category = 'OTHER';
      }
    }

    if (Object.keys(updates).length > 0) {
      batch.update(doc.ref, updates);
      count++;
      console.log(`✅ Updating: ${job.title}`);
      console.log(`   Adding: ${JSON.stringify(updates)}`);
      console.log('');
    }
  });

  if (count > 0) {
    await batch.commit();
    console.log(`\n✅ Fixed ${count} jobs`);
  } else {
    console.log('\n✅ All jobs already have required fields');
  }
}

fixRelianceJobs()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('Error:', error);
    process.exit(1);
  });
