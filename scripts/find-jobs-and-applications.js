/**
 * Find Jobs and Applications by Contact Number
 * Searches for jobs with contact number 9390515834 and retrieves all applications
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

async function findJobsAndApplications() {
  const phoneNumber = '9390515834';
  console.log(`🔍 Searching for jobs with contact number: ${phoneNumber}\n`);
  
  try {
    // Find jobs with this contact number
    const jobsSnapshot = await db.collection('jobs')
      .where('contactNumber', '==', phoneNumber)
      .get();
    
    if (jobsSnapshot.empty) {
      console.log(`❌ No jobs found with contact number ${phoneNumber}`);
      return;
    }
    
    console.log(`✅ Found ${jobsSnapshot.size} job(s) with contact number ${phoneNumber}\n`);
    console.log('=' .repeat(80));
    
    const jobIds = [];
    
    // Display job details
    jobsSnapshot.forEach((doc, index) => {
      const job = doc.data();
      jobIds.push(doc.id);
      
      console.log(`\n📋 JOB ${index + 1}:`);
      console.log(`   Job ID: ${doc.id}`);
      console.log(`   Title: ${job.title}`);
      console.log(`   Company: ${job.companyName || 'N/A'}`);
      console.log(`   Location: ${job.location}`);
      console.log(`   Pay: ₹${job.payAmount} ${job.payType}`);
      console.log(`   Contact: ${job.contactNumber}`);
      console.log(`   Posted: ${job.postedAt ? new Date(job.postedAt._seconds * 1000 || job.postedAt).toLocaleString() : 'N/A'}`);
      console.log(`   Active: ${job.isActive}`);
      console.log(`   Application Count: ${job.applicationCount || 0}`);
    });
    
    console.log('\n' + '='.repeat(80));
    console.log('\n🔍 Fetching applications for these jobs...\n');
    
    // Fetch applications for each job
    let totalApplications = 0;
    
    for (let i = 0; i < jobIds.length; i++) {
      const jobId = jobIds[i];
      const job = jobsSnapshot.docs[i].data();
      
      console.log(`\n📊 APPLICATIONS FOR: ${job.title}`);
      console.log(`   Job ID: ${jobId}`);
      console.log('   ' + '-'.repeat(76));
      
      const applicationsSnapshot = await db.collection('applications')
        .where('jobId', '==', jobId)
        .get();
      
      if (applicationsSnapshot.empty) {
        console.log(`   ℹ️  No applications yet`);
      } else {
        console.log(`   ✅ Found ${applicationsSnapshot.size} application(s)\n`);
        
        applicationsSnapshot.forEach((appDoc, appIndex) => {
          const app = appDoc.data();
          totalApplications++;
          
          console.log(`   ${appIndex + 1}. Application ID: ${appDoc.id}`);
          console.log(`      Worker ID: ${app.workerId || 'N/A'}`);
          console.log(`      Worker Name: ${app.workerName || 'N/A'}`);
          console.log(`      Worker Phone: ${app.workerPhone || 'N/A'}`);
          console.log(`      Status: ${app.status || 'N/A'}`);
          console.log(`      Applied At: ${app.appliedAt ? new Date(app.appliedAt._seconds * 1000 || app.appliedAt).toLocaleString() : 'N/A'}`);
          console.log(`      Cover Letter: ${app.coverLetter || 'None'}`);
          console.log('');
        });
      }
    }
    
    console.log('=' .repeat(80));
    console.log('\n📈 SUMMARY:');
    console.log(`   Total Jobs: ${jobsSnapshot.size}`);
    console.log(`   Total Applications: ${totalApplications}`);
    console.log(`   Contact Number: ${phoneNumber}`);
    
    // Export to JSON file
    const exportData = {
      phoneNumber: phoneNumber,
      searchDate: new Date().toISOString(),
      totalJobs: jobsSnapshot.size,
      totalApplications: totalApplications,
      jobs: []
    };
    
    for (let i = 0; i < jobIds.length; i++) {
      const jobId = jobIds[i];
      const job = jobsSnapshot.docs[i].data();
      
      const applicationsSnapshot = await db.collection('applications')
        .where('jobId', '==', jobId)
        .get();
      
      const applications = [];
      applicationsSnapshot.forEach(appDoc => {
        applications.push({
          applicationId: appDoc.id,
          ...appDoc.data()
        });
      });
      
      exportData.jobs.push({
        jobId: jobId,
        jobData: job,
        applications: applications
      });
    }
    
    // Save to file
    const outputFile = path.join(__dirname, `jobs_applications_${phoneNumber}_${Date.now()}.json`);
    fs.writeFileSync(outputFile, JSON.stringify(exportData, null, 2));
    console.log(`\n💾 Data exported to: ${outputFile}`);
    
  } catch (error) {
    console.error('❌ Error:', error.message);
    console.error(error.stack);
  }
}

findJobsAndApplications()
  .then(() => {
    console.log('\n✅ Search completed successfully!');
    process.exit(0);
  })
  .catch(error => {
    console.error('❌ Script failed:', error);
    process.exit(1);
  });
