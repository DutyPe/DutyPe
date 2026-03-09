const admin = require('firebase-admin');

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.applicationDefault()
  });
}

const db = admin.firestore();

async function fixFilledJobs() {
  console.log('🔍 Checking jobs with isFilled=true or missing fields...\n');
  
  try {
    // Get ALL jobs
    const jobsSnapshot = await db.collection('jobs').get();
    console.log(`📊 Total jobs in database: ${jobsSnapshot.size}\n`);
    
    let filledCount = 0;
    let missingIsFilledCount = 0;
    let expiredCount = 0;
    let activeCount = 0;
    let missingIsActiveCount = 0;
    
    const batch = db.batch();
    let batchCount = 0;
    const MAX_BATCH = 500;
    
    const currentTime = Date.now();
    
    for (const doc of jobsSnapshot.docs) {
      const data = doc.data();
      const jobId = doc.id;
      let needsUpdate = false;
      const updates = {};
      
      // Check isFilled
      if (data.isFilled === undefined || data.isFilled === null) {
        console.log(`❌ Job ${jobId} (${data.title}) - Missing isFilled field`);
        updates.isFilled = false;
        needsUpdate = true;
        missingIsFilledCount++;
      } else if (data.isFilled === true) {
        console.log(`⚠️  Job ${jobId} (${data.title}) - isFilled=true`);
        filledCount++;
      }
      
      // Check isActive
      if (data.isActive === undefined || data.isActive === null) {
        console.log(`❌ Job ${jobId} (${data.title}) - Missing isActive field`);
        updates.isActive = true;
        needsUpdate = true;
        missingIsActiveCount++;
      } else if (data.isActive === false) {
        console.log(`⚠️  Job ${jobId} (${data.title}) - isActive=false`);
      } else {
        activeCount++;
      }
      
      // Check expiry
      if (data.expiresAt && data.expiresAt > 0 && data.expiresAt < currentTime) {
        console.log(`⏰ Job ${jobId} (${data.title}) - Expired (${new Date(data.expiresAt).toLocaleDateString()})`);
        expiredCount++;
      }
      
      // Apply updates if needed
      if (needsUpdate) {
        batch.update(doc.ref, updates);
        batchCount++;
        
        // Commit batch if it reaches 500
        if (batchCount >= MAX_BATCH) {
          await batch.commit();
          console.log(`\n✅ Committed batch of ${batchCount} updates\n`);
          batchCount = 0;
        }
      }
    }
    
    // Commit remaining updates
    if (batchCount > 0) {
      await batch.commit();
      console.log(`\n✅ Committed final batch of ${batchCount} updates\n`);
    }
    
    // Summary
    console.log('\n📊 ========== SUMMARY ==========');
    console.log(`Total jobs: ${jobsSnapshot.size}`);
    console.log(`Jobs with isFilled=true: ${filledCount}`);
    console.log(`Jobs missing isFilled (fixed): ${missingIsFilledCount}`);
    console.log(`Jobs with isActive=true: ${activeCount}`);
    console.log(`Jobs missing isActive (fixed): ${missingIsActiveCount}`);
    console.log(`Expired jobs: ${expiredCount}`);
    console.log(`\nExpected visible jobs: ${jobsSnapshot.size - filledCount - expiredCount}`);
    console.log('================================\n');
    
    // Now check how many jobs should be visible
    const visibleJobsQuery = await db.collection('jobs')
      .where('isActive', '==', true)
      .where('isFilled', '==', false)
      .get();
    
    console.log(`✅ Jobs that should be visible now: ${visibleJobsQuery.size}`);
    
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

// Run the script
fixFilledJobs()
  .then(() => {
    console.log('\n✅ Script completed!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
