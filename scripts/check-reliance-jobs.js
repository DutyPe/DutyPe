/**
 * Check Reliance Retail jobs fields
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

async function checkRelianceJobs() {
  console.log('🔍 Checking Reliance Retail jobs fields...\n');

  const snapshot = await db.collection('jobs')
    .where('companyName', '==', 'Reliance Retail')
    .get();

  console.log(`Found ${snapshot.size} Reliance Retail jobs\n`);

  snapshot.forEach(doc => {
    const job = doc.data();
    console.log(`📋 Job: ${job.title}`);
    console.log(`   ID: ${doc.id}`);
    console.log(`   jobId: ${job.jobId}`);
    console.log(`   Has 'category' field: ${job.category !== undefined}`);
    console.log(`   Category value: ${job.category || 'NOT SET'}`);
    console.log(`   Has 'createdAt' field: ${job.createdAt !== undefined}`);
    console.log(`   createdAt value: ${job.createdAt}`);
    console.log(`   Has 'postedAt' field: ${job.postedAt !== undefined}`);
    console.log(`   postedAt value: ${job.postedAt}`);
    console.log(`   isActive: ${job.isActive}`);
    console.log(`   isFilled: ${job.isFilled}`);
    console.log(`   expiryDays: ${job.expiryDays}`);
    console.log('');
  });
}

checkRelianceJobs()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('Error:', error);
    process.exit(1);
  });
