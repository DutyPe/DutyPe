/**
 * Reactivate Expired Jobs
 * 
 * This script finds all expired jobs and extends their expiry date
 * by 30 days from now, making them visible again.
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Check if service account key exists
const possibleKeyFiles = [
  'serviceAccountKey.json',
  'dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json'
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
  console.error('❌ Service account key not found!');
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function reactivateExpiredJobs() {
  console.log('🔄 ========== REACTIVATING EXPIRED JOBS ==========\n');
  
  try {
    // Get ALL jobs
    const jobsSnapshot = await db.collection('jobs').get();
    const now = Date.now();
    const thirtyDaysFromNow = now + (30 * 24 * 60 * 60 * 1000); // 30 days in milliseconds
    
    console.log(`📊 Total jobs in database: ${jobsSnapshot.size}\n`);
    
    // Find expired jobs
    const expiredJobs = [];
    
    jobsSnapshot.forEach(doc => {
      const job = doc.data();
      const expiresAt = job.expiresAt || 0;
      const isExpired = expiresAt > 0 && expiresAt < now;
      
      if (isExpired) {
        expiredJobs.push({
          id: doc.id,
          title: job.title,
          category: job.category,
          location: job.location,
          oldExpiresAt: expiresAt,
          daysAgo: Math.floor((now - expiresAt) / (1000 * 60 * 60 * 24))
        });
      }
    });
    
    console.log(`⏱️  Found ${expiredJobs.length} expired jobs\n`);
    
    if (expiredJobs.length === 0) {
      console.log('✅ No expired jobs to reactivate!');
      process.exit(0);
    }
    
    // Show expired jobs
    console.log('📋 Expired jobs to reactivate:');
    console.log('─'.repeat(60));
    expiredJobs.forEach((job, index) => {
      console.log(`${index + 1}. ${job.title}`);
      console.log(`   Category: ${job.category} | Location: ${job.location}`);
      console.log(`   Expired: ${new Date(job.oldExpiresAt).toLocaleString()} (${job.daysAgo} days ago)`);
    });
    console.log('─'.repeat(60));
    console.log('');
    
    // Reactivate jobs
    console.log(`🔄 Reactivating ${expiredJobs.length} jobs...\n`);
    
    const batch = db.batch();
    let batchCount = 0;
    
    for (const job of expiredJobs) {
      const jobRef = db.collection('jobs').doc(job.id);
      
      // Extend expiry by 30 days from now
      batch.update(jobRef, {
        expiresAt: thirtyDaysFromNow,
        isActive: true,
        isFilled: false
      });
      
      batchCount++;
      console.log(`✅ Reactivated: ${job.title}`);
      console.log(`   New expiry: ${new Date(thirtyDaysFromNow).toLocaleString()} (30 days from now)`);
      
      // Commit batch every 500 updates
      if (batchCount >= 500) {
        await batch.commit();
        console.log(`\n✅ Committed batch of ${batchCount} updates\n`);
        batchCount = 0;
      }
    }
    
    // Commit remaining
    if (batchCount > 0) {
      await batch.commit();
      console.log(`\n✅ Committed final batch of ${batchCount} updates\n`);
    }
    
    console.log('\n' + '='.repeat(60));
    console.log(`✅ Successfully reactivated ${expiredJobs.length} jobs!`);
    console.log(`📅 All jobs now expire on: ${new Date(thirtyDaysFromNow).toLocaleDateString()}`);
    console.log('='.repeat(60) + '\n');
    
  } catch (error) {
    console.error('❌ Error reactivating jobs:', error);
    process.exit(1);
  }
  
  process.exit(0);
}

// Run the script
reactivateExpiredJobs();
