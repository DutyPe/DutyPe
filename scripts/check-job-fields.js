/**
 * Check if jobs have category field in Firestore
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

async function checkJobFields() {
  console.log('🔍 Checking job fields in Firestore...\n');

  const snapshot = await db.collection('jobs')
    .where('isActive', '==', true)
    .limit(10)
    .get();

  console.log(`Found ${snapshot.size} active jobs\n`);

  snapshot.forEach(doc => {
    const job = doc.data();
    console.log(`📋 Job: ${job.title}`);
    console.log(`   Company: ${job.companyName}`);
    console.log(`   Has 'category' field: ${job.category !== undefined}`);
    console.log(`   Category value: ${job.category || 'NOT SET'}`);
    console.log(`   Has 'createdAt' field: ${job.createdAt !== undefined}`);
    console.log(`   Has 'postedAt' field: ${job.postedAt !== undefined}`);
    console.log(`   isActive: ${job.isActive}`);
    console.log(`   isFilled: ${job.isFilled}`);
    console.log('');
  });
}

checkJobFields()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('Error:', error);
    process.exit(1);
  });
