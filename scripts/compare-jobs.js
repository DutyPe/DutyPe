/**
 * Compare Teaching Jobs with Existing Jobs
 * Find out why teaching jobs aren't showing in the app
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

async function compareJobs() {
  console.log('🔍 Comparing teaching jobs with existing jobs...\n');
  
  try {
    // Get one teaching job
    const teachingSnapshot = await db.collection('jobs')
      .where('contactNumber', '==', '9505549949')
      .limit(1)
      .get();
    
    // Get one existing job (not teaching)
    const existingSnapshot = await db.collection('jobs')
      .orderBy('postedAt', 'desc')
      .limit(10)
      .get();
    
    if (teachingSnapshot.empty) {
      console.log('❌ No teaching jobs found!');
      return;
    }
    
    const teachingJob = teachingSnapshot.docs[0].data();
    console.log('📚 TEACHING JOB STRUCTURE:');
    console.log(JSON.stringify(teachingJob, null, 2));
    console.log('\n' + '='.repeat(80) + '\n');
    
    console.log('📋 EXISTING JOBS (Last 5):');
    existingSnapshot.forEach((doc, index) => {
      const job = doc.data();
      console.log(`\n${index + 1}. ${job.title}`);
      console.log(`   Contact: ${job.contactNumber}`);
      console.log(`   Active: ${job.isActive}`);
      console.log(`   Posted: ${new Date(job.postedAt).toLocaleString()}`);
      console.log(`   Pay: ${job.payAmount} ${job.payType}`);
      console.log(`   Location: ${job.location}`);
      console.log(`   Employer ID: ${job.employerId}`);
      
      // Show all fields
      if (index === 0) {
        console.log('\n   ALL FIELDS:');
        console.log(JSON.stringify(job, null, 2));
      }
    });
    
    console.log('\n' + '='.repeat(80));
    console.log('\n🔍 FIELD COMPARISON:');
    
    const existingJob = existingSnapshot.docs[0].data();
    const teachingFields = Object.keys(teachingJob).sort();
    const existingFields = Object.keys(existingJob).sort();
    
    console.log('\n📚 Teaching job fields:', teachingFields.length);
    console.log('📋 Existing job fields:', existingFields.length);
    
    const missingInTeaching = existingFields.filter(f => !teachingFields.includes(f));
    const extraInTeaching = teachingFields.filter(f => !existingFields.includes(f));
    
    if (missingInTeaching.length > 0) {
      console.log('\n⚠️  MISSING in teaching jobs:');
      missingInTeaching.forEach(field => {
        console.log(`   - ${field}: ${JSON.stringify(existingJob[field])}`);
      });
    }
    
    if (extraInTeaching.length > 0) {
      console.log('\n➕ EXTRA in teaching jobs:');
      extraInTeaching.forEach(field => {
        console.log(`   - ${field}: ${JSON.stringify(teachingJob[field])}`);
      });
    }
    
    // Check critical fields
    console.log('\n🔑 CRITICAL FIELDS CHECK:');
    const criticalFields = ['id', 'jobId', 'employerId', 'isActive', 'postedAt', 'title', 'location', 'latitude', 'longitude', 'payAmount', 'payType'];
    
    criticalFields.forEach(field => {
      const teachingValue = teachingJob[field];
      const existingValue = existingJob[field];
      const teachingType = typeof teachingValue;
      const existingType = typeof existingValue;
      
      if (teachingType !== existingType) {
        console.log(`   ⚠️  ${field}: Teaching=${teachingType}, Existing=${existingType}`);
      } else {
        console.log(`   ✅ ${field}: ${teachingType}`);
      }
    });
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error(error.stack);
  }
}

compareJobs()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });
