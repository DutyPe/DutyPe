/**
 * Check Jobs Status in Database
 * 
 * This script analyzes all jobs in the database and reports:
 * - Total jobs count
 * - Active jobs (isActive: true)
 * - Expired jobs (expiresAt < now)
 * - Filled jobs (isFilled: true)
 * - Jobs that should be visible to workers
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
    console.log(`✅ Found service account key: ${filename}`);
    break;
  }
}

if (!serviceAccountPath) {
  console.error('❌ Service account key not found!');
  console.error('   Please add one of these files to the scripts folder:');
  possibleKeyFiles.forEach(f => console.error(`   - ${f}`));
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkJobsStatus() {
  console.log('🔍 ========== CHECKING JOBS STATUS ==========\n');
  
  try {
    // Get ALL jobs from database
    const jobsSnapshot = await db.collection('jobs').get();
    const totalJobs = jobsSnapshot.size;
    
    console.log(`📊 TOTAL JOBS IN DATABASE: ${totalJobs}\n`);
    
    if (totalJobs === 0) {
      console.log('❌ NO JOBS FOUND IN DATABASE!');
      console.log('   Run: node scripts/seed-real-jobs.js to add jobs\n');
      process.exit(0);
    }
    
    // Analyze each job
    const now = Date.now();
    let activeCount = 0;
    let inactiveCount = 0;
    let expiredCount = 0;
    let notExpiredCount = 0;
    let filledCount = 0;
    let notFilledCount = 0;
    let visibleToWorkersCount = 0;
    let missingIsActiveCount = 0;
    let missingIsFilledCount = 0;
    
    const expiredJobs = [];
    const filledJobs = [];
    const visibleJobs = [];
    const jobsToFix = [];
    
    jobsSnapshot.forEach(doc => {
      const job = doc.data();
      const jobId = doc.id;
      let needsFix = false;
      const fixes = {};
      
      // Check isActive
      if (job.isActive === undefined || job.isActive === null) {
        missingIsActiveCount++;
        fixes.isActive = true;
        needsFix = true;
      }
      const isActive = job.isActive === true || job.isActive === undefined;
      if (isActive) activeCount++;
      else inactiveCount++;
      
      // Check isFilled
      if (job.isFilled === undefined || job.isFilled === null) {
        missingIsFilledCount++;
        fixes.isFilled = false;
        needsFix = true;
      }
      const isFilled = job.isFilled === true;
      if (isFilled) {
        filledCount++;
        filledJobs.push({
          id: jobId,
          title: job.title,
          category: job.category
        });
      } else {
        notFilledCount++;
      }
      
      // Check expiry
      const expiresAt = job.expiresAt || 0;
      const isExpired = expiresAt > 0 && expiresAt < now;
      if (isExpired) {
        expiredCount++;
        expiredJobs.push({
          id: jobId,
          title: job.title,
          expiresAt: new Date(expiresAt).toLocaleString(),
          daysAgo: Math.floor((now - expiresAt) / (1000 * 60 * 60 * 24))
        });
      } else {
        notExpiredCount++;
      }
      
      // Check if visible to workers (isActive=true, not expired, not filled)
      const willBeVisible = (isActive || needsFix) && !isExpired && !isFilled;
      if (willBeVisible) {
        visibleToWorkersCount++;
        visibleJobs.push({
          id: jobId,
          title: job.title,
          category: job.category,
          location: job.location,
          payAmount: job.payAmount
        });
      }
      
      if (needsFix) {
        jobsToFix.push({ id: jobId, title: job.title, fixes });
      }
    });
    
    // Print summary
    console.log('📈 SUMMARY:');
    console.log('─'.repeat(60));
    console.log(`✅ Active (isActive=true):        ${activeCount} (${(activeCount/totalJobs*100).toFixed(1)}%)`);
    console.log(`❌ Inactive (isActive=false):     ${inactiveCount} (${(inactiveCount/totalJobs*100).toFixed(1)}%)`);
    console.log(`⚠️  Missing isActive field:       ${missingIsActiveCount}`);
    console.log('');
    console.log(`⏰ Not Expired:                   ${notExpiredCount} (${(notExpiredCount/totalJobs*100).toFixed(1)}%)`);
    console.log(`⏱️  Expired:                       ${expiredCount} (${(expiredCount/totalJobs*100).toFixed(1)}%)`);
    console.log('');
    console.log(`📦 Not Filled (isFilled=false):   ${notFilledCount} (${(notFilledCount/totalJobs*100).toFixed(1)}%)`);
    console.log(`✔️  Filled (isFilled=true):        ${filledCount} (${(filledCount/totalJobs*100).toFixed(1)}%)`);
    console.log(`⚠️  Missing isFilled field:       ${missingIsFilledCount}`);
    console.log('');
    console.log('─'.repeat(60));
    console.log(`👁️  VISIBLE TO WORKERS:            ${visibleToWorkersCount} (${(visibleToWorkersCount/totalJobs*100).toFixed(1)}%)`);
    console.log('   (isActive=true AND not expired AND not filled)');
    console.log('─'.repeat(60));
    console.log('');
    
    // FIX MISSING FIELDS
    if (jobsToFix.length > 0) {
      console.log(`\n🔧 FIXING ${jobsToFix.length} JOBS WITH MISSING FIELDS...\n`);
      
      const batch = db.batch();
      let batchCount = 0;
      
      for (const job of jobsToFix) {
        const jobRef = db.collection('jobs').doc(job.id);
        batch.update(jobRef, job.fixes);
        batchCount++;
        
        console.log(`✅ Fixed: ${job.title}`);
        Object.keys(job.fixes).forEach(field => {
          console.log(`   - Set ${field} = ${job.fixes[field]}`);
        });
        
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
      
      console.log(`\n✅ FIXED ${jobsToFix.length} JOBS!\n`);
      console.log(`👁️  NEW VISIBLE COUNT: ${visibleToWorkersCount} jobs\n`);
    }
    
    // Show expired jobs details
    if (expiredJobs.length > 0) {
      console.log(`\n⏱️  EXPIRED JOBS (${expiredJobs.length}):`);
      console.log('─'.repeat(60));
      expiredJobs.slice(0, 10).forEach((job, index) => {
        console.log(`${index + 1}. ${job.title}`);
        console.log(`   Expired: ${job.expiresAt} (${job.daysAgo} days ago)`);
      });
      if (expiredJobs.length > 10) {
        console.log(`   ... and ${expiredJobs.length - 10} more`);
      }
    }
    
    // Show filled jobs details
    if (filledJobs.length > 0) {
      console.log(`\n✔️  FILLED JOBS (${filledJobs.length}):`);
      console.log('─'.repeat(60));
      filledJobs.slice(0, 10).forEach((job, index) => {
        console.log(`${index + 1}. ${job.title} (${job.category})`);
      });
      if (filledJobs.length > 10) {
        console.log(`   ... and ${filledJobs.length - 10} more`);
      }
    }
    
    // Show sample visible jobs
    if (visibleJobs.length > 0) {
      console.log(`\n👁️  SAMPLE VISIBLE JOBS (showing ${Math.min(10, visibleJobs.length)} of ${visibleJobs.length}):`);
      console.log('─'.repeat(60));
      visibleJobs.slice(0, 10).forEach((job, index) => {
        console.log(`${index + 1}. ${job.title}`);
        console.log(`   Category: ${job.category} | Location: ${job.location}`);
        console.log(`   Pay: ${job.payAmount}`);
      });
    } else {
      console.log('\n❌ NO JOBS VISIBLE TO WORKERS!');
      console.log('\nPossible reasons:');
      console.log('1. All jobs have isActive=false');
      console.log('2. All jobs are expired');
      console.log('3. All jobs are filled (isFilled=true)');
      console.log('\n💡 SOLUTION: Run seed script to add fresh jobs:');
      console.log('   node scripts/seed-real-jobs.js');
    }
    
    console.log('\n' + '='.repeat(60));
    console.log('✅ Analysis complete!');
    if (jobsToFix.length > 0) {
      console.log(`✅ Fixed ${jobsToFix.length} jobs - they should now be visible in the app!`);
    }
    console.log('='.repeat(60) + '\n');
    
  } catch (error) {
    console.error('❌ Error checking jobs:', error);
    process.exit(1);
  }
  
  process.exit(0);
}

// Run the check
checkJobsStatus();
