/**
 * Job Database Audit Script
 * 
 * Analyzes all jobs in Firestore and identifies issues:
 * - Missing required fields
 * - Expired jobs
 * - Filled jobs
 * - Inactive jobs
 * - Invalid data
 * 
 * Usage: node scripts/audit-jobs.js
 */

const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');
const serviceAccount = loadServiceAccount();

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Required fields for a valid job
const REQUIRED_FIELDS = [
  'jobId',
  'employerId',
  'title',
  'location',
  'latitude',
  'longitude',
  'payAmount',
  'payType',
  'category',
  'isActive',
  'createdAt',
  'expiresAt'
];

async function auditJobs() {
  console.log('🔍 Starting Job Database Audit...\n');
  
  try {
    // Fetch ALL jobs (no filters)
    const snapshot = await db.collection('jobs').get();
    
    const stats = {
      total: snapshot.size,
      valid: 0,
      expired: 0,
      filled: 0,
      inactive: 0,
      missingFields: 0,
      invalidCoordinates: 0,
      issues: []
    };
    
    const currentTime = Date.now();
    
    console.log(`📊 Total jobs in database: ${stats.total}\n`);
    console.log('Analyzing each job...\n');
    
    snapshot.forEach(doc => {
      const job = doc.data();
      const jobId = doc.id;
      const issues = [];
      
      // Check 1: Required fields
      const missingFields = REQUIRED_FIELDS.filter(field => !job[field]);
      if (missingFields.length > 0) {
        issues.push(`Missing fields: ${missingFields.join(', ')}`);
        stats.missingFields++;
      }
      
      // Check 2: isActive flag
      if (job.isActive === false) {
        issues.push('Job is inactive (isActive=false)');
        stats.inactive++;
      }
      
      // Check 3: isFilled flag
      if (job.isFilled === true) {
        issues.push('Job is filled (isFilled=true)');
        stats.filled++;
      }
      
      // Check 4: Expiry
      const expiresAt = job.expiresAt || 0;
      if (expiresAt > 0 && expiresAt < currentTime) {
        const daysExpired = Math.floor((currentTime - expiresAt) / (1000 * 60 * 60 * 24));
        issues.push(`Job expired ${daysExpired} days ago`);
        stats.expired++;
      }
      
      // Check 5: Coordinates
      const lat = job.latitude;
      const lon = job.longitude;
      if (typeof lat !== 'number' || typeof lon !== 'number' || lat === 0 || lon === 0) {
        issues.push(`Invalid coordinates: lat=${lat}, lon=${lon}`);
        stats.invalidCoordinates++;
      }
      
      // Check 6: Category
      if (!job.category || job.category === '') {
        issues.push('Missing or empty category');
      }
      
      // If job has issues, record them
      if (issues.length > 0) {
        stats.issues.push({
          jobId,
          title: job.title || 'NO TITLE',
          location: job.location || 'NO LOCATION',
          createdAt: job.createdAt ? new Date(job.createdAt).toISOString() : 'UNKNOWN',
          issues
        });
      } else {
        stats.valid++;
      }
    });
    
    // Print summary
    console.log('═══════════════════════════════════════════════════════');
    console.log('📊 AUDIT SUMMARY');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`Total Jobs:              ${stats.total}`);
    console.log(`✅ Valid Jobs:           ${stats.valid} (${((stats.valid/stats.total)*100).toFixed(1)}%)`);
    console.log(`❌ Jobs with Issues:     ${stats.issues.length} (${((stats.issues.length/stats.total)*100).toFixed(1)}%)`);
    console.log('');
    console.log('Issue Breakdown:');
    console.log(`  🚫 Inactive:           ${stats.inactive}`);
    console.log(`  ✔️  Filled:             ${stats.filled}`);
    console.log(`  ⏰ Expired:            ${stats.expired}`);
    console.log(`  📝 Missing Fields:     ${stats.missingFields}`);
    console.log(`  📍 Invalid Coords:     ${stats.invalidCoordinates}`);
    console.log('═══════════════════════════════════════════════════════\n');
    
    // Print detailed issues
    if (stats.issues.length > 0) {
      console.log('📋 DETAILED ISSUES:\n');
      
      stats.issues.forEach((job, index) => {
        console.log(`${index + 1}. Job ID: ${job.jobId}`);
        console.log(`   Title: ${job.title}`);
        console.log(`   Location: ${job.location}`);
        console.log(`   Created: ${job.createdAt}`);
        console.log(`   Issues:`);
        job.issues.forEach(issue => {
          console.log(`     - ${issue}`);
        });
        console.log('');
      });
    }
    
    // Recommendations
    console.log('═══════════════════════════════════════════════════════');
    console.log('💡 RECOMMENDATIONS');
    console.log('═══════════════════════════════════════════════════════');
    
    if (stats.expired > 0) {
      console.log(`\n⏰ ${stats.expired} expired jobs found`);
      console.log('   Action: Run cleanup script to remove or extend expiry');
      console.log('   Command: node scripts/cleanup-expired-jobs.js');
    }
    
    if (stats.inactive > 0) {
      console.log(`\n🚫 ${stats.inactive} inactive jobs found`);
      console.log('   Action: Review and reactivate or delete');
      console.log('   Command: node scripts/reactivate-jobs.js');
    }
    
    if (stats.filled > 0) {
      console.log(`\n✔️  ${stats.filled} filled jobs found`);
      console.log('   Action: These are normal - jobs that have been filled');
      console.log('   Note: They won\'t show in worker feeds');
    }
    
    if (stats.missingFields > 0) {
      console.log(`\n📝 ${stats.missingFields} jobs with missing fields`);
      console.log('   Action: Fix missing data or delete invalid jobs');
      console.log('   Command: node scripts/fix-missing-fields.js');
    }
    
    if (stats.invalidCoordinates > 0) {
      console.log(`\n📍 ${stats.invalidCoordinates} jobs with invalid coordinates`);
      console.log('   Action: Update coordinates or delete jobs');
      console.log('   Command: node scripts/fix-coordinates.js');
    }
    
    console.log('\n═══════════════════════════════════════════════════════\n');
    
    // Export to JSON for further analysis
    const report = {
      timestamp: new Date().toISOString(),
      stats,
      issues: stats.issues
    };
    
    const fs = require('fs');
    fs.writeFileSync('scripts/job-audit-report.json', JSON.stringify(report, null, 2));
    console.log('📄 Full report saved to: scripts/job-audit-report.json\n');
    
  } catch (error) {
    console.error('❌ Error during audit:', error);
  } finally {
    process.exit(0);
  }
}

// Run audit
auditJobs();
