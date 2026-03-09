/**
 * Verify Sunflower School Jobs
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

async function verifySunflowerJobs() {
  console.log('🔍 Searching for Sunflower School jobs...\n');
  
  try {
    const snapshot = await db.collection('jobs')
      .where('contactNumber', '==', '9347299567')
      .get();
    
    if (snapshot.empty) {
      console.log('❌ No Sunflower School jobs found!');
      return;
    }
    
    console.log(`✅ Found ${snapshot.size} Sunflower School jobs!\n`);
    
    snapshot.forEach((doc, index) => {
      const job = doc.data();
      console.log(`📚 Job ${index + 1}:`);
      console.log(`   Title: ${job.title}`);
      console.log(`   School: ${job.companyName}`);
      console.log(`   Location: ${job.location}`);
      console.log(`   Contact: ${job.contactNumber}`);
      console.log(`   Category: ${job.category}`);
      console.log(`   Active: ${job.isActive}`);
      console.log('');
    });
    
    console.log('✅ All Sunflower School jobs are in the database!');
    console.log('\n💡 The jobs should now be visible in your app.');
    console.log('   Pull down to refresh if needed.');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

verifySunflowerJobs()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });
