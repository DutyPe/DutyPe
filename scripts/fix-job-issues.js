/**
 * Fix Job Issues Script
 * 
 * Automatically fixes common job issues:
 * - Extends expiry for expired jobs
 * - Reactivates inactive jobs (optional)
 * - Adds missing fields with defaults
 * 
 * Usage: 
 *   node scripts/fix-job-issues.js --extend-expiry
 *   node scripts/fix-job-issues.js --reactivate
 *   node scripts/fix-job-issues.js --fix-missing
 *   node scripts/fix-job-issues.js --all
 */

const admin = require('firebase-admin');
const serviceAccount = require('./dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Parse command line arguments
const args = process.argv.slice(2);
const options = {
  extendExpiry: args.includes('--extend-expiry') || args.includes('--all'),
  reactivate: args.includes('--reactivate') || args.includes('--all'),
  fixMissing: args.includes('--fix-missing') || args.includes('--all'),
  dryRun: args.includes('--dry-run')
};

async function fixJobIssues() {
  console.log('🔧 Starting Job Fix Script...\n');
  
  if (options.dryRun) {
    console.log('⚠️  DRY RUN MODE - No changes will be made\n');
  }
  
  console.log('Options:');
  console.log(`  Extend Expiry: ${options.extendExpiry}`);
  console.log(`  Reactivate: ${options.reactivate}`);
  console.log(`  Fix Missing Fields: ${options.fixMissing}`);
  console.log('');
  
  try {
    const snapshot = await db.collection('jobs').get();
    const currentTime = Date.now();
    
    const stats = {
      total: snapshot.size,
      extended: 0,
      reactivated: 0,
      fixed: 0,
      errors: 0
    };
    
    console.log(`📊 Processing ${stats.total} jobs...\n`);
    
    const batch = db.batch();
    let batchCount = 0;
    const BATCH_SIZE = 500; // Firestore batch limit
    
    for (const doc of snapshot.docs) {
      const job = doc.data();
      const jobRef = doc.ref;
      const updates = {};
      let needsUpdate = false;
      
      // Fix 1: Extend expired jobs
      if (options.extendExpiry) {
        const expiresAt = job.expiresAt || 0;
        if (expiresAt > 0 && expiresAt < currentTime) {
          // Extend by 30 days from now
          updates.expiresAt = currentTime + (30 * 24 * 60 * 60 * 1000);
          updates.expiryDays = 30;
          stats.extended++;
          needsUpdate = true;
          console.log(`⏰ Extending expiry for: ${job.title || doc.id}`);
        }
      }
      
      // Fix 2: Reactivate inactive jobs
      if (options.reactivate && job.isActive === false) {
        updates.isActive = true;
        stats.reactivated++;
        needsUpdate = true;
        console.log(`🔄 Reactivating: ${job.title || doc.id}`);
      }
      
      // Fix 3: Add missing fields
      if (options.fixMissing) {
        if (!job.jobId) {
          updates.jobId = doc.id;
          needsUpdate = true;
        }
        if (job.isActive === undefined) {
          updates.isActive = true;
          needsUpdate = true;
        }
        if (job.isFilled === undefined) {
          updates.isFilled = false;
          needsUpdate = true;
        }
        if (!job.createdAt) {
          updates.createdAt = currentTime;
          needsUpdate = true;
        }
        if (!job.updatedAt) {
          updates.updatedAt = currentTime;
          needsUpdate = true;
        }
        if (!job.expiresAt) {
          updates.expiresAt = currentTime + (15 * 24 * 60 * 60 * 1000); // 15 days default
          updates.expiryDays = 15;
          needsUpdate = true;
        }
        if (!job.applicationCount) {
          updates.applicationCount = 0;
          needsUpdate = true;
        }
        
        if (needsUpdate) {
          stats.fixed++;
          console.log(`📝 Fixing missing fields for: ${job.title || doc.id}`);
        }
      }
      
      // Apply updates
      if (needsUpdate && !options.dryRun) {
        batch.update(jobRef, updates);
        batchCount++;
        
        // Commit batch if we hit the limit
        if (batchCount >= BATCH_SIZE) {
          await batch.commit();
          console.log(`✅ Committed batch of ${batchCount} updates`);
          batchCount = 0;
        }
      }
    }
    
    // Commit remaining updates
    if (batchCount > 0 && !options.dryRun) {
      await batch.commit();
      console.log(`✅ Committed final batch of ${batchCount} updates`);
    }
    
    // Print summary
    console.log('\n═══════════════════════════════════════════════════════');
    console.log('📊 FIX SUMMARY');
    console.log('═══════════════════════════════════════════════════════');
    console.log(`Total Jobs Processed:    ${stats.total}`);
    console.log(`⏰ Expiry Extended:      ${stats.extended}`);
    console.log(`🔄 Jobs Reactivated:     ${stats.reactivated}`);
    console.log(`📝 Missing Fields Fixed: ${stats.fixed}`);
    console.log(`❌ Errors:               ${stats.errors}`);
    console.log('═══════════════════════════════════════════════════════\n');
    
    if (options.dryRun) {
      console.log('⚠️  DRY RUN - No changes were made');
      console.log('   Remove --dry-run flag to apply changes\n');
    } else {
      console.log('✅ All fixes applied successfully!\n');
    }
    
  } catch (error) {
    console.error('❌ Error during fix:', error);
  } finally {
    process.exit(0);
  }
}

// Show usage if no options provided
if (!options.extendExpiry && !options.reactivate && !options.fixMissing) {
  console.log('Usage: node scripts/fix-job-issues.js [options]\n');
  console.log('Options:');
  console.log('  --extend-expiry    Extend expiry for expired jobs by 30 days');
  console.log('  --reactivate       Reactivate inactive jobs');
  console.log('  --fix-missing      Add missing required fields');
  console.log('  --all              Apply all fixes');
  console.log('  --dry-run          Preview changes without applying them\n');
  console.log('Examples:');
  console.log('  node scripts/fix-job-issues.js --extend-expiry --dry-run');
  console.log('  node scripts/fix-job-issues.js --all');
  process.exit(0);
}

// Run fixes
fixJobIssues();
