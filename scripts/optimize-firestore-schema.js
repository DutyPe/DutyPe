/**
 * Firestore Schema Optimization Script
 * 
 * Reduces 39 collections to 8 core collections
 * Based on Urban Company, TaskRabbit, Swiggy best practices
 * 
 * PHASE 1: Remove duplicate collections
 * PHASE 2: Merge profile collections into users
 * PHASE 3: Denormalize data
 */

const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp({
    projectId: 'dutype-860ac'
  });
}

const db = admin.firestore();
const BATCH_SIZE = 500;

console.log('🚀 Firestore Schema Optimization');
console.log('================================================================================\n');

async function phase1_removeDuplicates() {
  console.log('PHASE 1: Remove Duplicate Collections\n');
  
  // 1. Merge applications → job_applications (keep job_applications)
  console.log('1. Checking applications vs job_applications...');
  const applicationsCount = (await db.collection('applications').count().get()).data().count;
  const jobApplicationsCount = (await db.collection('job_applications').count().get()).data().count;
  
  console.log(`   applications: ${applicationsCount} docs`);
  console.log(`   job_applications: ${jobApplicationsCount} docs`);
  
  if (applicationsCount > 0 && jobApplicationsCount > 0) {
    console.log('   ⚠️  Both collections exist - manual review needed');
  } else if (applicationsCount > 0) {
    console.log('   ✅ Will use applications collection');
  } else {
    console.log('   ✅ Using job_applications (standard)');
  }
  
  // 2. Check saved_jobs vs savedJobs
  console.log('\n2. Checking saved_jobs vs savedJobs...');
  try {
    const savedJobsCount = (await db.collection('saved_jobs').count().get()).data().count;
    const savedJobsAltCount = (await db.collection('savedJobs').count().get()).data().count;
    
    console.log(`   saved_jobs: ${savedJobsCount} docs`);
    console.log(`   savedJobs: ${savedJobsAltCount} docs`);
    
    if (savedJobsCount > 0 && savedJobsAltCount > 0) {
      console.log('   ⚠️  Both collections exist - manual review needed');
    }
  } catch (e) {
    console.log('   ✅ Only one collection exists');
  }
  
  console.log('\n');
}

async function phase2_analyzeProfiles() {
  console.log('PHASE 2: Analyze Profile Collections\n');
  
  // Check if worker_profiles and employer_profiles exist
  console.log('1. Checking profile collections...');
  
  try {
    const workerProfilesCount = (await db.collection('worker_profiles').count().get()).data().count;
    console.log(`   worker_profiles: ${workerProfilesCount} docs`);
    
    if (workerProfilesCount > 0) {
      console.log('   ⚠️  worker_profiles should be merged into users');
    }
  } catch (e) {
    console.log('   ✅ worker_profiles not found');
  }
  
  try {
    const employerProfilesCount = (await db.collection('employer_profiles').count().get()).data().count;
    console.log(`   employer_profiles: ${employerProfilesCount} docs`);
    
    if (employerProfilesCount > 0) {
      console.log('   ⚠️  employer_profiles should be merged into users');
    }
  } catch (e) {
    console.log('   ✅ employer_profiles not found');
  }
  
  console.log('\n');
}

async function phase3_analyzeUsers() {
  console.log('PHASE 3: Analyze Users Collection\n');
  
  const usersSnapshot = await db.collection('users').limit(5).get();
  
  if (usersSnapshot.empty) {
    console.log('   ❌ No users found');
    return;
  }
  
  const sampleUser = usersSnapshot.docs[0].data();
  const fieldCount = Object.keys(sampleUser).length;
  
  console.log(`   Sample user fields (${fieldCount} total):`);
  console.log(`   ${Object.keys(sampleUser).slice(0, 20).join(', ')}`);
  
  // Check for redundant fields
  const redundantFields = [];
  if ('workerProfile' in sampleUser) redundantFields.push('workerProfile');
  if ('employerProfile' in sampleUser) redundantFields.push('employerProfile');
  
  if (redundantFields.length > 0) {
    console.log(`\n   ⚠️  Redundant nested objects found: ${redundantFields.join(', ')}`);
    console.log('   💡 Should flatten these into top-level fields');
  } else {
    console.log('\n   ✅ No redundant nested objects');
  }
  
  console.log('\n');
}

async function generateReport() {
  console.log('================================================================================');
  console.log('OPTIMIZATION REPORT\n');
  
  const collections = await db.listCollections();
  console.log(`Total Collections: ${collections.length}\n`);
  
  const collectionCounts = [];
  
  for (const collection of collections) {
    try {
      const count = (await collection.count().get()).data().count;
      collectionCounts.push({ name: collection.id, count });
    } catch (e) {
      collectionCounts.push({ name: collection.id, count: 'error' });
    }
  }
  
  collectionCounts.sort((a, b) => {
    if (typeof a.count === 'number' && typeof b.count === 'number') {
      return b.count - a.count;
    }
    return 0;
  });
  
  console.log('Collection'.padEnd(35) + 'Documents');
  console.log('─'.repeat(50));
  
  collectionCounts.forEach(col => {
    console.log(col.name.padEnd(35) + col.count);
  });
  
  console.log('\n================================================================================');
  console.log('RECOMMENDATIONS:\n');
  
  console.log('1. MERGE DUPLICATES:');
  console.log('   - Keep job_applications (remove applications if duplicate)');
  console.log('   - Keep saved_jobs (remove savedJobs if duplicate)\n');
  
  console.log('2. MERGE PROFILES INTO USERS:');
  console.log('   - worker_profiles → users');
  console.log('   - employer_profiles → users');
  console.log('   - phone_roles → users (role field)\n');
  
  console.log('3. DENORMALIZE:');
  console.log('   - rating_summaries → users (ratings field)');
  console.log('   - achievements → users (achievements field)');
  console.log('   - subscriptions → users (subscription field)\n');
  
  console.log('4. USE SUBCOLLECTIONS:');
  console.log('   - fcm_tokens → users/{id}/tokens');
  console.log('   - user_activity → users/{id}/activity');
  console.log('   - notification_tracking → users/{id}/notifications\n');
  
  console.log('5. MERGE ADMIN COLLECTIONS:');
  console.log('   - Create single admin collection with subcollections');
  console.log('   - metadata, announcements, blacklists, etc.\n');
  
  console.log('TARGET: Reduce from ' + collections.length + ' to 8 core collections');
  console.log('SAVINGS: ~' + Math.round((1 - 8/collections.length) * 100) + '% reduction\n');
  
  console.log('================================================================================');
}

async function main() {
  try {
    await phase1_removeDuplicates();
    await phase2_analyzeProfiles();
    await phase3_analyzeUsers();
    await generateReport();
    
    console.log('\n✅ Analysis complete!');
    console.log('\n💡 Next steps:');
    console.log('   1. Review recommendations above');
    console.log('   2. Backup your database');
    console.log('   3. Run migration scripts for each phase');
    console.log('   4. Update service files');
    console.log('   5. Deploy new Firestore rules\n');
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

main()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('💥 Fatal error:', error);
    process.exit(1);
  });
