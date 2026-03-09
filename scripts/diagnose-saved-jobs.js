const admin = require('firebase-admin');

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.applicationDefault()
  });
}

const db = admin.firestore();

async function diagnoseSavedJobs() {
  console.log('🔍 SAVED JOBS DIAGNOSTIC TOOL\n');
  console.log('=' .repeat(60));
  
  try {
    // Step 1: Find users with saved jobs
    console.log('\n📋 Step 1: Finding users with saved jobs...\n');
    const usersSnapshot = await db.collection('users')
      .where('savedJobs', '!=', [])
      .limit(5)
      .get();
    
    if (usersSnapshot.empty) {
      console.log('❌ No users found with saved jobs');
      console.log('\n💡 Checking if any users exist with savedJobs field...\n');
      
      const allUsersSnapshot = await db.collection('users').limit(10).get();
      allUsersSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`User ${doc.id}:`);
        console.log(`  - savedJobs field exists: ${data.savedJobs !== undefined}`);
        console.log(`  - savedJobs value: ${JSON.stringify(data.savedJobs || 'undefined')}`);
      });
      return;
    }
    
    console.log(`✅ Found ${usersSnapshot.size} users with saved jobs\n`);
    
    // Step 2: Analyze each user's saved jobs
    for (const userDoc of usersSnapshot.docs) {
      const userData = userDoc.data();
      const userId = userDoc.id;
      const savedJobIds = userData.savedJobs || [];
      
      console.log(`\n${'='.repeat(60)}`);
      console.log(`👤 User: ${userId}`);
      console.log(`   Name: ${userData.fullName || 'N/A'}`);
      console.log(`   Phone: ${userData.phone || 'N/A'}`);
      console.log(`   Saved Jobs Count: ${savedJobIds.length}`);
      console.log(`   Saved Job IDs: ${JSON.stringify(savedJobIds)}`);
      
      if (savedJobIds.length === 0) {
        console.log('   ⚠️  Empty savedJobs array');
        continue;
      }
      
      // Step 3: Check if saved job documents exist
      console.log(`\n   📦 Checking job documents...`);
      
      let foundJobs = 0;
      let activeJobs = 0;
      let inactiveJobs = 0;
      let missingJobs = 0;
      
      for (const jobId of savedJobIds) {
        try {
          const jobDoc = await db.collection('jobs').doc(jobId).get();
          
          if (!jobDoc.exists) {
            console.log(`   ❌ Job ${jobId}: NOT FOUND`);
            missingJobs++;
          } else {
            const jobData = jobDoc.data();
            foundJobs++;
            
            if (jobData.isActive === true) {
              activeJobs++;
              console.log(`   ✅ Job ${jobId}: ACTIVE`);
              console.log(`      Title: ${jobData.title || 'N/A'}`);
              console.log(`      Company: ${jobData.companyName || 'N/A'}`);
              console.log(`      Location: ${jobData.location || 'N/A'}`);
            } else {
              inactiveJobs++;
              console.log(`   ⚠️  Job ${jobId}: INACTIVE`);
              console.log(`      Title: ${jobData.title || 'N/A'}`);
              console.log(`      isActive: ${jobData.isActive}`);
            }
          }
        } catch (error) {
          console.log(`   ❌ Job ${jobId}: ERROR - ${error.message}`);
          missingJobs++;
        }
      }
      
      // Step 4: Summary
      console.log(`\n   📊 Summary for ${userId}:`);
      console.log(`      Total Saved: ${savedJobIds.length}`);
      console.log(`      Found: ${foundJobs}`);
      console.log(`      Active: ${activeJobs}`);
      console.log(`      Inactive: ${inactiveJobs}`);
      console.log(`      Missing: ${missingJobs}`);
      
      if (activeJobs === 0) {
        console.log(`\n   ⚠️  WARNING: No active jobs found! User will see empty saved jobs list.`);
      }
    }
    
    // Step 5: Check old saved_jobs collection (deprecated)
    console.log(`\n${'='.repeat(60)}`);
    console.log('\n📋 Step 5: Checking deprecated saved_jobs collection...\n');
    
    const oldSavedJobsSnapshot = await db.collection('saved_jobs').limit(5).get();
    
    if (oldSavedJobsSnapshot.empty) {
      console.log('✅ No documents in deprecated saved_jobs collection (good!)');
    } else {
      console.log(`⚠️  Found ${oldSavedJobsSnapshot.size} documents in deprecated saved_jobs collection`);
      console.log('   These should be migrated to users.savedJobs array');
      
      oldSavedJobsSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`   - ${doc.id}: workerId=${data.workerId}, jobId=${data.jobId}`);
      });
    }
    
    console.log(`\n${'='.repeat(60)}`);
    console.log('\n✅ DIAGNOSTIC COMPLETE\n');
    
  } catch (error) {
    console.error('❌ Error during diagnosis:', error);
    throw error;
  }
}

// Run the diagnostic
diagnoseSavedJobs()
  .then(() => {
    console.log('Done!');
    process.exit(0);
  })
  .catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
  });
