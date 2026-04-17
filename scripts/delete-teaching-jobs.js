/**
 * Delete old teaching jobs before posting new ones
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

async function deleteTeachingJobs() {
  console.log('🗑️  Deleting old teaching jobs...\n');
  
  try {
    const snapshot = await db.collection('jobs')
      .where('contactNumber', '==', '9505549949')
      .get();
    
    if (snapshot.empty) {
      console.log('✅ No teaching jobs to delete');
      return;
    }
    
    console.log(`Found ${snapshot.size} teaching jobs to delete`);
    
    const batch = db.batch();
    snapshot.docs.forEach(doc => {
      batch.delete(doc.ref);
      console.log(`   Deleting: ${doc.data().title}`);
    });
    
    await batch.commit();
    console.log(`\n✅ Deleted ${snapshot.size} teaching jobs successfully!`);
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

deleteTeachingJobs()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });
