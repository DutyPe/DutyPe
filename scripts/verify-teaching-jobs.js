/**
 * Verify Teaching Jobs Script
 * Checks if the teaching jobs were successfully posted to Firestore
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Find service account key
const possibleKeyFiles = [
  'serviceAccountKey.json',
  'dutype-860ac-firebase-adminsdk.json'
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

// Initialize Firebase Admin
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutype-860ac'
});

const db = admin.firestore();

async function verifyTeachingJobs() {
  console.log('🔍 Searching for teaching jobs with contact number 9505549949...\n');
  
  try {
    // Query jobs with the specific contact number
    const snapshot = await db.collection('jobs')
      .where('contactNumber', '==', '9505549949')
      .get();
    
    if (snapshot.empty) {
      console.log('❌ No jobs found with contact number 9505549949');
      console.log('\nTrying to find recent jobs...');
      
      // Try to find recent jobs
      const recentSnapshot = await db.collection('jobs')
        .orderBy('postedAt', 'desc')
        .limit(10)
        .get();
      
      console.log(`\n📋 Last 10 jobs posted:`);
      recentSnapshot.forEach(doc => {
        const job = doc.data();
        console.log(`  - ${job.title} (${job.contactNumber}) - Posted: ${new Date(job.postedAt).toLocaleString()}`);
      });
      
      return;
    }
    
    console.log(`✅ Found ${snapshot.size} teaching jobs!\n`);
    
    snapshot.forEach((doc, index) => {
      const job = doc.data();
      console.log(`📚 Job ${index + 1}:`);
      console.log(`   ID: ${doc.id}`);
      console.log(`   Title: ${job.title}`);
      console.log(`   Location: ${job.location}`);
      console.log(`   Contact: ${job.contactNumber}`);
      console.log(`   Active: ${job.isActive}`);
      console.log(`   Posted: ${new Date(job.postedAt).toLocaleString()}`);
      console.log(`   Coordinates: ${job.latitude}, ${job.longitude}`);
      console.log('');
    });
    
    console.log('✅ All teaching jobs are in the database!');
    console.log('\n💡 If you still can\'t see them in the app:');
    console.log('   1. Pull down to refresh the jobs list');
    console.log('   2. Check if you have any filters applied');
    console.log('   3. Try searching for "teacher" in the search bar');
    console.log('   4. Make sure your app is connected to the internet');
    console.log('   5. Clear app cache and restart');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

verifyTeachingJobs()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });
