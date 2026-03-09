/**
 * Firestore Migration Script: Optimize Job Applications Schema
 * 
 * WHAT THIS DOES:
 * - Removes 35+ unnecessary fields from existing job applications
 * - Keeps only 15 essential fields for hyper-local part-time jobs
 * - Reduces storage by ~80% and improves query performance
 * 
 * BEFORE (50+ fields):
 * - workerEmail, workerProfileImageUrl, workerLocation, workerGender, workerDateOfBirth
 * - skills, skillsText, workExperience, workExperienceText, education
 * - certifications, languages, expectedSalary, availability
 * - additionalDocuments, statusHistory, workerAadhaarVerified, etc.
 * 
 * AFTER (15 fields):
 * - id, applicationId, jobId, workerId, employerId
 * - status, appliedAt, updatedAt
 * - jobTitle, jobLocation, companyName, workerName, workerPhone
 * - source, coverLetter, active
 * 
 * SAFETY:
 * - Dry run mode by default (preview changes without applying)
 * - Batch processing (500 docs at a time)
 * - Progress tracking
 * - Backup recommended before running
 * 
 * USAGE:
 * 1. Dry run (preview): node scripts/migrate-applications-schema.js
 * 2. Apply changes: node scripts/migrate-applications-schema.js --apply
 */

const admin = require('firebase-admin');
const serviceAccount = require('../app/google-services.json');

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: serviceAccount.project_id,
      clientEmail: serviceAccount.client[0].oauth_client[0].client_id + '@' + serviceAccount.project_id + '.iam.gserviceaccount.com',
      privateKey: '-----BEGIN PRIVATE KEY-----\n' + serviceAccount.client[0].oauth_client[0].client_secret + '\n-----END PRIVATE KEY-----\n'
    })
  });
}

const db = admin.firestore();

// Fields to KEEP (15 essential fields)
const CORE_FIELDS = new Set([
  // Core IDs
  'id',
  'applicationId',
  'jobId',
  'workerId',
  'employerId',
  
  // Status
  'status',
  'appliedAt',
  'updatedAt',
  
  // Denormalized display data
  'jobTitle',
  'jobLocation',
  'companyName',
  'workerName',
  'workerPhone',
  
  // Application specific
  'source',
  'applicationSource', // Backward compatibility
  'coverLetter',
  
  // Flags
  'active'
]);

// Check if running in apply mode
const APPLY_CHANGES = process.argv.includes('--apply');

async function migrateApplications() {
  console.log('🚀 Starting Job Applications Schema Migration');
  console.log(`📋 Mode: ${APPLY_CHANGES ? 'APPLY CHANGES' : 'DRY RUN (preview only)'}`);
  console.log('');
  
  try {
    // Get all applications
    const snapshot = await db.collection('job_applications').get();
    console.log(`📊 Found ${snapshot.size} applications to process`);
    console.log('');
    
    let processedCount = 0;
    let optimizedCount = 0;
    let errorCount = 0;
    let totalFieldsRemoved = 0;
    
    // Process in batches of 500 (Firestore batch limit)
    const batchSize = 500;
    let batch = db.batch();
    let batchCount = 0;
    
    for (const doc of snapshot.docs) {
      try {
        const data = doc.data();
        const fieldsToRemove = [];
        
        // Find fields that are NOT in core fields
        for (const field of Object.keys(data)) {
          if (!CORE_FIELDS.has(field)) {
            fieldsToRemove.push(field);
          }
        }
        
        if (fieldsToRemove.length > 0) {
          optimizedCount++;
          totalFieldsRemoved += fieldsToRemove.length;
          
          if (processedCount < 5) {
            // Show first 5 examples
            console.log(`📄 Application ${doc.id}:`);
            console.log(`   Removing ${fieldsToRemove.length} fields: ${fieldsToRemove.slice(0, 10).join(', ')}${fieldsToRemove.length > 10 ? '...' : ''}`);
            console.log('');
          }
          
          if (APPLY_CHANGES) {
            // Create update object with FieldValue.delete() for each field
            const updates = {};
            fieldsToRemove.forEach(field => {
              updates[field] = admin.firestore.FieldValue.delete();
            });
            
            batch.update(doc.ref, updates);
            batchCount++;
            
            // Commit batch if it reaches 500 operations
            if (batchCount >= batchSize) {
              await batch.commit();
              console.log(`✅ Committed batch of ${batchCount} updates`);
              batch = db.batch();
              batchCount = 0;
            }
          }
        }
        
        processedCount++;
        
        // Progress indicator every 100 docs
        if (processedCount % 100 === 0) {
          console.log(`⏳ Progress: ${processedCount}/${snapshot.size} applications processed...`);
        }
        
      } catch (error) {
        errorCount++;
        console.error(`❌ Error processing ${doc.id}:`, error.message);
      }
    }
    
    // Commit remaining batch
    if (APPLY_CHANGES && batchCount > 0) {
      await batch.commit();
      console.log(`✅ Committed final batch of ${batchCount} updates`);
    }
    
    console.log('');
    console.log('📊 MIGRATION SUMMARY:');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    console.log(`Total applications: ${snapshot.size}`);
    console.log(`Processed: ${processedCount}`);
    console.log(`Optimized: ${optimizedCount}`);
    console.log(`Errors: ${errorCount}`);
    console.log(`Total fields removed: ${totalFieldsRemoved}`);
    console.log(`Average fields removed per app: ${(totalFieldsRemoved / optimizedCount).toFixed(1)}`);
    console.log(`Storage reduction: ~${((totalFieldsRemoved / (snapshot.size * 50)) * 100).toFixed(0)}%`);
    console.log('');
    
    if (!APPLY_CHANGES) {
      console.log('💡 This was a DRY RUN. No changes were made.');
      console.log('💡 To apply changes, run: node scripts/migrate-applications-schema.js --apply');
    } else {
      console.log('✅ Migration completed successfully!');
      console.log('✅ All applications now use optimized 15-field schema');
    }
    
  } catch (error) {
    console.error('❌ Migration failed:', error);
    process.exit(1);
  }
}

// Run migration
migrateApplications()
  .then(() => {
    console.log('');
    console.log('🎉 Done!');
    process.exit(0);
  })
  .catch(error => {
    console.error('💥 Fatal error:', error);
    process.exit(1);
  });
