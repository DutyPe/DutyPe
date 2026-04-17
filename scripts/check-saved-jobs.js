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

// Initialize Firebase Admin
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkSavedJobs() {
  try {
    console.log('🔍 Checking saved jobs issue...\n');
    
    // Get a user with saved jobs
    const userId = 'g0XbC8DpPCa94DJI00YEWh8eyid2'; // From logs
    
    console.log(`📋 Checking user: ${userId}`);
    const userDoc = await db.collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      console.log('❌ User not found');
      return;
    }
    
    const userData = userDoc.data();
    const savedJobIds = userData.savedJobs || [];
    
    console.log(`\n✅ User has ${savedJobIds.length} saved job IDs:`);
    savedJobIds.forEach((id, index) => {
      console.log(`   ${index + 1}. ${id}`);
    });
    
    if (savedJobIds.length === 0) {
      console.log('\n⚠️ No saved jobs found in user document');
      return;
    }
    
    // Check if these jobs exist in jobs collection
    console.log(`\n🔍 Checking if these jobs exist in 'jobs' collection...`);
    
    for (const jobId of savedJobIds) {
      const jobDoc = await db.collection('jobs').doc(jobId).get();
      
      if (jobDoc.exists) {
        const job = jobDoc.data();
        console.log(`   ✅ ${jobId}: EXISTS`);
        console.log(`      - title: ${job.title}`);
        console.log(`      - id field: ${job.id || 'NOT SET'}`);
        console.log(`      - jobId field: ${job.jobId || 'NOT SET'}`);
        console.log(`      - isActive: ${job.isActive}`);
      } else {
        console.log(`   ❌ ${jobId}: NOT FOUND`);
      }
    }
    
    // Try the query that's failing
    console.log(`\n🔍 Testing whereIn query with 'jobId' field...`);
    const chunk = savedJobIds.slice(0, 10);
    const querySnapshot = await db.collection('jobs')
      .where('jobId', 'in', chunk)
      .get();
    
    console.log(`   Query returned ${querySnapshot.size} documents`);
    
    // Try with 'id' field
    console.log(`\n🔍 Testing whereIn query with 'id' field...`);
    const querySnapshot2 = await db.collection('jobs')
      .where('id', 'in', chunk)
      .get();
    
    console.log(`   Query returned ${querySnapshot2.size} documents`);
    
    // Try using document IDs directly
    console.log(`\n🔍 Testing direct document fetch...`);
    const promises = chunk.map(id => db.collection('jobs').doc(id).get());
    const docs = await Promise.all(promises);
    const existingDocs = docs.filter(doc => doc.exists);
    console.log(`   Found ${existingDocs.length} documents by ID`);
    
  } catch (error) {
    console.error('❌ Error:', error);
  } finally {
    process.exit(0);
  }
}

checkSavedJobs();
