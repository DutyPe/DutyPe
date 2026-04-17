/**
 * Fix jobs with undefined isFilled field
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

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

const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutype-860ac'
});

const db = admin.firestore();

async function fixIsFilledField() {
  console.log('🔧 Fixing jobs with undefined isFilled...\n');

  const snapshot = await db.collection('jobs')
    .where('isActive', '==', true)
    .get();

  const batch = db.batch();
  let count = 0;

  snapshot.forEach(doc => {
    const job = doc.data();
    if (job.isFilled === undefined) {
      batch.update(doc.ref, { isFilled: false });
      count++;
      console.log(`✅ Fixing: ${job.title} (${job.companyName})`);
    }
  });

  if (count > 0) {
    await batch.commit();
    console.log(`\n✅ Fixed ${count} jobs`);
  } else {
    console.log('\n✅ All jobs already have isFilled field');
  }
}

fixIsFilledField()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('Error:', error);
    process.exit(1);
  });
