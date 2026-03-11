const admin = require('firebase-admin');
const serviceAccount = require('../app/google-services.json');

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: serviceAccount.project_info.project_id,
      clientEmail: `firebase-adminsdk@${serviceAccount.project_info.project_id}.iam.gserviceaccount.com`,
      privateKey: process.env.FIREBASE_PRIVATE_KEY || ''
    }),
    databaseURL: `https://${serviceAccount.project_info.project_id}.firebaseio.com`
  });
}

const db = admin.firestore();

async function debugApplications() {
  const workerId = 'g0XbC8DpPCa94DJI00YEWh8eyid2';
  
  console.log(`\n🔍 Debugging applications for workerId: ${workerId}\n`);
  
  try {
    const snapshot = await db.collection('job_applications')
      .where('workerId', '==', workerId)
      .get();
    
    console.log(`📊 Found ${snapshot.size} documents\n`);
    
    snapshot.forEach((doc, index) => {
      const data = doc.data();
      console.log(`\n📄 Document ${index + 1}/${snapshot.size}: ${doc.id}`);
      console.log(`   Fields: ${Object.keys(data).join(', ')}`);
      console.log(`   Status: ${data.status}`);
      console.log(`   Active: ${data.active}`);
      console.log(`   JobId: ${data.jobId}`);
      console.log(`   AppliedAt: ${data.appliedAt}`);
      
      // Check for problematic fields
      const requiredFields = ['id', 'jobId', 'workerId', 'employerId', 'status', 'appliedAt', 'updatedAt', 'active'];
      const missingFields = requiredFields.filter(field => !(field in data) && field !== 'id');
      if (missingFields.length > 0) {
        console.log(`   ⚠️  Missing fields: ${missingFields.join(', ')}`);
      }
      
      // Check for extra fields that might cause issues
      const modelFields = [
        'id', 'jobId', 'workerId', 'employerId', 'status', 'appliedAt', 'updatedAt', 'active',
        'jobTitle', 'jobLocation', 'companyName', 'workerName', 'coverLetter', 'source',
        'lastPendingNotificationSent', 'workerEmail', 'workerPhone', 'workerLocation',
        'workerGender', 'workerDateOfBirth', 'workerProfileImageUrl', 'workExperience',
        'workExperienceText', 'skills', 'skillsText', 'education', 'certifications',
        'languages', 'availability', 'expectedSalary', 'resumeUrl', 'workerAadhaarVerified',
        'workerPhoneVerified', 'workerJobsInArea', 'workerLocalRating', 'workerTotalReviews',
        'workerBackgroundCheckPassed', 'workerIdentityVerified', 'homeEntryJob',
        'additionalDocuments', 'statusHistory'
      ];
      
      const extraFields = Object.keys(data).filter(field => !modelFields.includes(field));
      if (extraFields.length > 0) {
        console.log(`   ℹ️  Extra fields: ${extraFields.join(', ')}`);
      }
      
      // Check status field type
      if (typeof data.status !== 'string') {
        console.log(`   ❌ Status field is not a string: ${typeof data.status}`);
      }
      
      // Check source field type
      if (data.source && typeof data.source !== 'string') {
        console.log(`   ❌ Source field is not a string: ${typeof data.source}`);
      }
    });
    
    console.log('\n✅ Debug complete\n');
    
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

debugApplications()
  .then(() => process.exit(0))
  .catch(error => {
    console.error(error);
    process.exit(1);
  });
