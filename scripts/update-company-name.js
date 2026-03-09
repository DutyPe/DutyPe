/**
 * Update Company Name for Cashier Job
 * Changes company name from "VAMSI BANOTH" to "Reliance Retail"
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Find service account key
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

// Initialize Firebase Admin
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutypeapp'
});

const db = admin.firestore();

async function updateCompanyName() {
  const jobId = 'LtJLafvyEsfPE6430Hc4';
  
  console.log('🔄 Updating company name for Cashier job...\n');
  
  try {
    // Get the job first
    const jobRef = db.collection('jobs').doc(jobId);
    const jobDoc = await jobRef.get();
    
    if (!jobDoc.exists) {
      console.log('❌ Job not found!');
      return;
    }
    
    const jobData = jobDoc.data();
    console.log('📋 Current Job Details:');
    console.log(`   Job ID: ${jobId}`);
    console.log(`   Title: ${jobData.title}`);
    console.log(`   Current Company: ${jobData.companyName}`);
    console.log(`   Location: ${jobData.location}`);
    console.log('');
    
    // Update the company name
    await jobRef.update({
      companyName: 'Reliance Retail',
      employerName: 'Reliance Retail',
      updatedAt: admin.firestore.Timestamp.now()
    });
    
    console.log('✅ Company name updated successfully!');
    console.log('   New Company: Reliance Retail');
    
    // Verify the update
    const updatedDoc = await jobRef.get();
    const updatedData = updatedDoc.data();
    
    console.log('\n📋 Updated Job Details:');
    console.log(`   Job ID: ${jobId}`);
    console.log(`   Title: ${updatedData.title}`);
    console.log(`   Company: ${updatedData.companyName}`);
    console.log(`   Employer Name: ${updatedData.employerName}`);
    console.log(`   Location: ${updatedData.location}`);
    console.log(`   Contact: ${updatedData.contactNumber}`);
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error(error.stack);
  }
}

updateCompanyName()
  .then(() => {
    console.log('\n✅ Update completed successfully!');
    process.exit(0);
  })
  .catch(error => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });
