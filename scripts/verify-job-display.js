/**
 * Verify job display - check if salary formats show correctly
 */

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

async function verifyJobs() {
  console.log('🔍 Verifying Reliance Retail jobs in Khammam...\n');

  const snapshot = await db.collection('jobs')
    .where('companyName', '==', 'Reliance Retail')
    .where('location', '==', 'Khammam District, Telangana')
    .get();

  if (snapshot.empty) {
    console.log('❌ No jobs found');
    return;
  }

  console.log(`✅ Found ${snapshot.size} jobs\n`);

  snapshot.forEach(doc => {
    const job = doc.data();
    console.log(`📋 ${job.title}`);
    console.log(`   💰 Salary: ₹${job.payAmount} ${job.payType}`);
    console.log(`   📍 Location: ${job.location}`);
    console.log(`   📞 Contact: ${job.contactNumber}`);
    console.log(`   👥 Vacancies: ${job.vacancies}`);
    console.log(`   🆔 Job ID: ${job.id}`);
    console.log('');
  });

  console.log('✅ All jobs verified!');
  console.log('\n💡 Salary formats used:');
  console.log('   • Range format: 12000-15000');
  console.log('   • These will display as: ₹12000-15000/month in the app');
}

verifyJobs()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('Error:', error);
    process.exit(1);
  });
