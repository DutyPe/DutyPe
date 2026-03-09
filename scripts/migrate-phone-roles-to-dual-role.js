/**
 * Migration Script: Convert phone_roles from single role to dual-role format
 * 
 * This script updates the phone_roles collection to support dual roles:
 * - Converts "role" field to "roles" array
 * - Adds "activeRole" field
 * - Preserves existing data (joinedAt, deviceFingerprint, etc.)
 * 
 * Run this script ONCE after deploying the dual-role authentication fix
 * 
 * Usage:
 *   node scripts/migrate-phone-roles-to-dual-role.js
 */

const admin = require('firebase-admin');
const serviceAccount = require('../serviceAccountKey.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function migratePhoneRoles() {
  console.log('🚀 Starting phone_roles migration to dual-role format...\n');
  
  try {
    // Get all documents from phone_roles collection
    const phoneRolesSnapshot = await db.collection('phone_roles').get();
    
    if (phoneRolesSnapshot.empty) {
      console.log('ℹ️  No phone_roles documents found. Nothing to migrate.');
      return;
    }
    
    console.log(`📊 Found ${phoneRolesSnapshot.size} phone_roles documents to migrate\n`);
    
    let migratedCount = 0;
    let skippedCount = 0;
    let errorCount = 0;
    
    // Process each document
    for (const doc of phoneRolesSnapshot.docs) {
      const phone = doc.id;
      const data = doc.data();
      
      try {
        // Check if already migrated (has "roles" array)
        if (data.roles && Array.isArray(data.roles)) {
          console.log(`⏭️  Skipping ${phone} - Already migrated`);
          skippedCount++;
          continue;
        }
        
        // Check if has old "role" field
        if (!data.role) {
          console.log(`⚠️  Skipping ${phone} - No role field found`);
          skippedCount++;
          continue;
        }
        
        // Convert to new format
        const oldRole = data.role.toUpperCase();
        const updateData = {
          roles: [oldRole], // Convert single role to array
          activeRole: oldRole, // Set as active role
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        };
        
        // Preserve existing fields
        if (data.joinedAt) {
          updateData.joinedAt = data.joinedAt;
        }
        if (data.deviceFingerprint) {
          updateData.deviceFingerprint = data.deviceFingerprint;
        }
        if (data.deviceModel) {
          updateData.deviceModel = data.deviceModel;
        }
        if (data.androidId) {
          updateData.androidId = data.androidId;
        }
        
        // Update document
        await doc.ref.set(updateData, { merge: true });
        
        // Delete old "role" field
        await doc.ref.update({
          role: admin.firestore.FieldValue.delete()
        });
        
        console.log(`✅ Migrated ${phone}: ${oldRole} → [${oldRole}]`);
        migratedCount++;
        
      } catch (error) {
        console.error(`❌ Error migrating ${phone}:`, error.message);
        errorCount++;
      }
    }
    
    // Summary
    console.log('\n' + '='.repeat(60));
    console.log('📊 Migration Summary:');
    console.log('='.repeat(60));
    console.log(`✅ Successfully migrated: ${migratedCount}`);
    console.log(`⏭️  Skipped (already migrated): ${skippedCount}`);
    console.log(`❌ Errors: ${errorCount}`);
    console.log(`📊 Total processed: ${phoneRolesSnapshot.size}`);
    console.log('='.repeat(60));
    
    if (errorCount === 0) {
      console.log('\n🎉 Migration completed successfully!');
    } else {
      console.log('\n⚠️  Migration completed with errors. Please review the logs above.');
    }
    
  } catch (error) {
    console.error('❌ Fatal error during migration:', error);
    process.exit(1);
  }
}

// Verify migration (optional check)
async function verifyMigration() {
  console.log('\n🔍 Verifying migration...\n');
  
  try {
    const phoneRolesSnapshot = await db.collection('phone_roles').get();
    
    let validCount = 0;
    let invalidCount = 0;
    
    for (const doc of phoneRolesSnapshot.docs) {
      const data = doc.data();
      
      // Check if has new format
      if (data.roles && Array.isArray(data.roles) && data.activeRole) {
        validCount++;
      } else {
        console.log(`⚠️  Invalid format: ${doc.id}`);
        invalidCount++;
      }
    }
    
    console.log('='.repeat(60));
    console.log('🔍 Verification Results:');
    console.log('='.repeat(60));
    console.log(`✅ Valid (new format): ${validCount}`);
    console.log(`❌ Invalid (old format): ${invalidCount}`);
    console.log('='.repeat(60));
    
    if (invalidCount === 0) {
      console.log('\n✅ All documents are in the correct format!');
    } else {
      console.log('\n⚠️  Some documents still need migration. Run the script again.');
    }
    
  } catch (error) {
    console.error('❌ Error during verification:', error);
  }
}

// Main execution
async function main() {
  console.log('╔════════════════════════════════════════════════════════════╗');
  console.log('║   Phone Roles Migration: Single Role → Dual Role Format   ║');
  console.log('╚════════════════════════════════════════════════════════════╝\n');
  
  // Ask for confirmation
  const readline = require('readline').createInterface({
    input: process.stdin,
    output: process.stdout
  });
  
  readline.question('⚠️  This will modify the phone_roles collection. Continue? (yes/no): ', async (answer) => {
    readline.close();
    
    if (answer.toLowerCase() !== 'yes') {
      console.log('❌ Migration cancelled.');
      process.exit(0);
    }
    
    await migratePhoneRoles();
    await verifyMigration();
    
    process.exit(0);
  });
}

main();
