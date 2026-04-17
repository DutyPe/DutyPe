const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Check if service account key exists
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
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutype-860ac'
});

const db = admin.firestore();

async function deleteKhammamJobs() {
  console.log('🗑️  Deleting Reliance Retail jobs in Khammam...\n');

  try {
    // Query for jobs in Khammam with company name Reliance Retail
    const snapshot = await db.collection('jobs')
      .where('city', '==', 'Khammam')
      .where('companyName', '==', 'Reliance Retail')
      .get();

    if (snapshot.empty) {
      console.log('ℹ️  No Khammam Reliance Retail jobs found to delete.');
      process.exit(0);
    }

    console.log(`Found ${snapshot.size} jobs to delete:\n`);

    const batch = db.batch();
    let count = 0;

    snapshot.forEach(doc => {
      const job = doc.data();
      console.log(`❌ Deleting: ${job.title}`);
      console.log(`   - Salary: ${job.salary}`);
      console.log(`   - Location: ${job.location}`);
      console.log('');
      
      batch.delete(doc.ref);
      count++;
    });

    await batch.commit();

    console.log(`\n✅ Successfully deleted ${count} Khammam Reliance Retail jobs!`);

  } catch (error) {
    console.error('❌ Error deleting jobs:', error);
    process.exit(1);
  }

  process.exit(0);
}

// Run the deletion
deleteKhammamJobs();
