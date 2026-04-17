/**
 * Audit ALL jobs in database for required fields
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

async function auditAllJobs() {
  console.log('🔍 Auditing ALL jobs in database...\n');

  const snapshot = await db.collection('jobs')
    .where('isActive', '==', true)
    .get();

  console.log(`Found ${snapshot.size} active jobs\n`);

  let missingCategory = 0;
  let missingCreatedAt = 0;
  let missingPostedAt = 0;
  let hasIsFilled = 0;
  let total = 0;

  const jobsToFix = [];

  snapshot.forEach(doc => {
    const job = doc.data();
    total++;

    const issues = [];
    if (!job.category) {
      missingCategory++;
      issues.push('NO CATEGORY');
    }
    if (!job.createdAt) {
      missingCreatedAt++;
      issues.push('NO createdAt');
    }
    if (!job.postedAt) {
      missingPostedAt++;
      issues.push('NO postedAt');
    }
    if (job.isFilled === undefined) {
      hasIsFilled++;
      issues.push('isFilled undefined');
    }

    if (issues.length > 0) {
      jobsToFix.push({
        id: doc.id,
        title: job.title,
        company: job.companyName,
        issues: issues
      });
    }
  });

  console.log('📊 AUDIT RESULTS:');
  console.log(`   Total active jobs: ${total}`);
  console.log(`   Missing 'category': ${missingCategory}`);
  console.log(`   Missing 'createdAt': ${missingCreatedAt}`);
  console.log(`   Missing 'postedAt': ${missingPostedAt}`);
  console.log(`   Missing 'isFilled': ${hasIsFilled}`);
  console.log('');

  if (jobsToFix.length > 0) {
    console.log(`❌ ${jobsToFix.length} jobs need fixing:\n`);
    jobsToFix.forEach((job, index) => {
      console.log(`${index + 1}. ${job.title} (${job.company})`);
      console.log(`   Issues: ${job.issues.join(', ')}`);
      console.log(`   ID: ${job.id}`);
      console.log('');
    });
  } else {
    console.log('✅ All jobs have required fields!');
  }
}

auditAllJobs()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('Error:', error);
    process.exit(1);
  });
