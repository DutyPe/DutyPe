/**
 * Quick Job Count Script
 * 
 * Shows a quick breakdown of jobs by status
 * 
 * Usage: node scripts/count-jobs.js
 */

const admin = require('firebase-admin');
const serviceAccount = require('./dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function countJobs() {
  console.log('📊 Counting jobs in database...\n');
  
  try {
    const snapshot = await db.collection('jobs').get();
    const currentTime = Date.now();
    
    const counts = {
      total: snapshot.size,
      active: 0,
      inactive: 0,
      filled: 0,
      unfilled: 0,
      expired: 0,
      valid: 0,
      byCategory: {}
    };
    
    snapshot.forEach(doc => {
      const job = doc.data();
      
      // Count by status
      if (job.isActive === true) counts.active++;
      if (job.isActive === false) counts.inactive++;
      if (job.isFilled === true) counts.filled++;
      if (job.isFilled === false || job.isFilled === undefined) counts.unfilled++;
      
      // Count expired
      const expiresAt = job.expiresAt || 0;
      if (expiresAt > 0 && expiresAt < currentTime) {
        counts.expired++;
      }
      
      // Count valid (active, unfilled, not expired)
      const isActive = job.isActive !== false;
      const isFilled = job.isFilled === true;
      const isExpired = expiresAt > 0 && expiresAt < currentTime;
      
      if (isActive && !isFilled && !isExpired) {
        counts.valid++;
      }
      
      // Count by category
      const category = job.category || 'UNKNOWN';
      counts.byCategory[category] = (counts.byCategory[category] || 0) + 1;
    });
    
    // Print results
    console.log('═══════════════════════════════════════════════════════');
    console.log('📊 JOB COUNT SUMMARY');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`Total Jobs in DB:        ${counts.total}`);
    console.log('');
    console.log('By Status:');
    console.log(`  ✅ Active:             ${counts.active}`);
    console.log(`  🚫 Inactive:           ${counts.inactive}`);
    console.log(`  ✔️  Filled:             ${counts.filled}`);
    console.log(`  📋 Unfilled:           ${counts.unfilled}`);
    console.log(`  ⏰ Expired:            ${counts.expired}`);
    console.log('');
    console.log(`🎯 VALID JOBS (shown in app): ${counts.valid}`);
    console.log('   (active + unfilled + not expired)');
    console.log('');
    console.log('By Category:');
    
    // Sort categories by count
    const sortedCategories = Object.entries(counts.byCategory)
      .sort((a, b) => b[1] - a[1]);
    
    sortedCategories.forEach(([category, count]) => {
      const percentage = ((count / counts.total) * 100).toFixed(1);
      console.log(`  ${category.padEnd(20)} ${count.toString().padStart(4)} (${percentage}%)`);
    });
    
    console.log('═══════════════════════════════════════════════════════\n');
    
    // Show why jobs might not be showing
    const hidden = counts.total - counts.valid;
    if (hidden > 0) {
      console.log(`⚠️  ${hidden} jobs are hidden from workers because:`);
      console.log(`   - ${counts.inactive} are inactive (isActive=false)`);
      console.log(`   - ${counts.filled} are filled (isFilled=true)`);
      console.log(`   - ${counts.expired} are expired (expiresAt < now)`);
      console.log('');
      console.log('To fix these issues, run:');
      console.log('  node scripts/fix-job-issues.js --all --dry-run  (preview)');
      console.log('  node scripts/fix-job-issues.js --all            (apply fixes)');
      console.log('');
    }
    
  } catch (error) {
    console.error('❌ Error counting jobs:', error);
  } finally {
    process.exit(0);
  }
}

// Run count
countJobs();
