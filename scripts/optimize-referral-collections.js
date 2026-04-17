const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Find service account key
const possibleKeyFiles = [
  'serviceAccountKey.json',
  'dutype-860ac-firebase-adminsdk.json'
];

let serviceAccount = null;
for (const keyFile of possibleKeyFiles) {
  // Check in scripts folder first, then parent directory
  const keyPaths = [
    path.join(__dirname, keyFile),
    path.join(__dirname, '..', keyFile)
  ];
  
  for (const keyPath of keyPaths) {
    if (fs.existsSync(keyPath)) {
      serviceAccount = require(keyPath);
      console.log(`✓ Using service account key: ${keyFile}`);
      break;
    }
  }
  
  if (serviceAccount) break;
}

if (!serviceAccount) {
  console.error('❌ Service account key not found!');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

/**
 * REFERRAL SYSTEM OPTIMIZATION
 * Merge referral_stats collection into users.referralStats field
 * 
 * BEFORE: 3 collections (referrals, referral_codes, referral_stats)
 * AFTER: 2 collections (referrals, referral_codes) + users.referralStats
 * 
 * Benefits:
 * - 33% fewer collections
 * - 50% faster dashboard loads (1 read instead of 2)
 * - Follows Dropbox/Airbnb pattern
 */

async function migrateReferralStats() {
  console.log('\n🔄 Starting referral stats migration...\n');
  
  const startTime = Date.now();
  let processed = 0;
  let migrated = 0;
  let errors = 0;
  
  try {
    // Get all referral_stats documents
    const statsSnapshot = await db.collection('referral_stats').get();
    console.log(`Found ${statsSnapshot.size} referral_stats documents\n`);
    
    const batch = db.batch();
    let batchCount = 0;
    
    for (const statsDoc of statsSnapshot.docs) {
      try {
        const statsData = statsDoc.data();
        const userId = statsDoc.id;
        
        // Get user document
        const userRef = db.collection('users').doc(userId);
        const userDoc = await userRef.get();
        
        if (!userDoc.exists) {
          console.log(`⚠️  User ${userId} not found, skipping...`);
          processed++;
          continue;
        }
        
        // Prepare referralStats object
        const referralStats = {
          totalReferrals: statsData.totalReferrals || 0,
          successfulReferrals: statsData.successfulReferrals || 0,
          totalEarnings: statsData.totalEarnings || 0.0,
          availableBalance: statsData.availableBalance || 0.0,
          withdrawnAmount: statsData.withdrawnAmount || 0.0,
          canWithdraw: statsData.canWithdraw || false,
          lastUpdated: statsData.lastUpdated || Date.now()
        };
        
        // Update user document
        batch.update(userRef, {
          referralCode: statsData.referralCode || null,
          referralStats: referralStats
        });
        
        migrated++;
        batchCount++;
        
        // Commit batch every 500 operations
        if (batchCount >= 500) {
          await batch.commit();
          console.log(`✓ Committed batch of ${batchCount} updates`);
          batchCount = 0;
        }
        
      } catch (error) {
        console.error(`❌ Error processing ${statsDoc.id}:`, error.message);
        errors++;
      }
      
      processed++;
      if (processed % 50 === 0) {
        console.log(`Progress: ${processed}/${statsSnapshot.size} processed...`);
      }
    }
    
    // Commit remaining batch
    if (batchCount > 0) {
      await batch.commit();
      console.log(`✓ Committed final batch of ${batchCount} updates`);
    }
    
    const duration = ((Date.now() - startTime) / 1000).toFixed(2);
    
    console.log('\n' + '='.repeat(60));
    console.log('📊 MIGRATION SUMMARY');
    console.log('='.repeat(60));
    console.log(`Total processed: ${processed}`);
    console.log(`Successfully migrated: ${migrated}`);
    console.log(`Errors: ${errors}`);
    console.log(`Duration: ${duration}s`);
    console.log('='.repeat(60));
    
    if (errors === 0) {
      console.log('\n✅ Migration completed successfully!');
      console.log('\n📝 Next steps:');
      console.log('1. Test the app to ensure referral features work');
      console.log('2. Run: node scripts/delete-referral-stats-collection.js');
      console.log('3. Update Firestore rules to remove referral_stats');
    } else {
      console.log('\n⚠️  Migration completed with errors. Review logs above.');
    }
    
  } catch (error) {
    console.error('\n❌ Fatal error:', error);
    process.exit(1);
  }
}

async function verifyMigration() {
  console.log('\n🔍 Verifying migration...\n');
  
  try {
    // Sample 10 users with referral stats
    const usersSnapshot = await db.collection('users')
      .where('referralCode', '!=', null)
      .limit(10)
      .get();
    
    console.log(`Checking ${usersSnapshot.size} users with referral codes:\n`);
    
    for (const userDoc of usersSnapshot.docs) {
      const userData = userDoc.data();
      const hasStats = userData.referralStats !== undefined;
      const hasCode = userData.referralCode !== undefined;
      
      console.log(`User ${userDoc.id}:`);
      console.log(`  ✓ Referral Code: ${userData.referralCode || 'N/A'}`);
      console.log(`  ${hasStats ? '✓' : '❌'} Referral Stats: ${hasStats ? 'Present' : 'Missing'}`);
      if (hasStats) {
        console.log(`    - Total Referrals: ${userData.referralStats.totalReferrals}`);
        console.log(`    - Successful: ${userData.referralStats.successfulReferrals}`);
        console.log(`    - Balance: ₹${userData.referralStats.availableBalance}`);
      }
      console.log('');
    }
    
    console.log('✅ Verification complete\n');
    
  } catch (error) {
    console.error('❌ Verification error:', error);
  }
}

async function main() {
  console.log('🚀 REFERRAL SYSTEM OPTIMIZATION');
  console.log('Merging referral_stats → users.referralStats\n');
  console.log('Press Ctrl+C within 5 seconds to cancel...\n');
  
  await new Promise(resolve => setTimeout(resolve, 5000));
  
  await migrateReferralStats();
  await verifyMigration();
  
  process.exit(0);
}

main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
