const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Find service account key in scripts folder
const serviceAccountFiles = [
  path.join(__dirname, 'dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json'),
  path.join(__dirname, '..', 'dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json'),
  path.join(__dirname, 'serviceAccountKey.json'),
  path.join(__dirname, '..', 'serviceAccountKey.json')
];

let serviceAccount = null;
for (const filePath of serviceAccountFiles) {
  if (fs.existsSync(filePath)) {
    console.log(`✅ Found service account key: ${path.basename(filePath)}`);
    serviceAccount = require(filePath);
    break;
  }
}

if (!serviceAccount) {
  console.error('❌ No service account key found!');
  process.exit(1);
}

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function fixDuplicateSavedJobs() {
  console.log('🔧 FIXING DUPLICATE SAVED JOBS\n');
  console.log('=' .repeat(60));
  
  try {
    // Find all users with saved jobs
    const usersSnapshot = await db.collection('users').get();
    
    let totalUsers = 0;
    let usersWithDuplicates = 0;
    let totalDuplicatesRemoved = 0;
    
    for (const userDoc of usersSnapshot.docs) {
      const userData = userDoc.data();
      const savedJobs = userData.savedJobs || [];
      
      if (savedJobs.length === 0) continue;
      
      totalUsers++;
      
      // Remove duplicates
      const uniqueSavedJobs = [...new Set(savedJobs)];
      
      if (uniqueSavedJobs.length < savedJobs.length) {
        const duplicatesCount = savedJobs.length - uniqueSavedJobs.length;
        usersWithDuplicates++;
        totalDuplicatesRemoved += duplicatesCount;
        
        console.log(`\n👤 User: ${userDoc.id}`);
        console.log(`   Name: ${userData.fullName || 'N/A'}`);
        console.log(`   Before: ${savedJobs.length} saved jobs`);
        console.log(`   After: ${uniqueSavedJobs.length} saved jobs`);
        console.log(`   Removed: ${duplicatesCount} duplicates`);
        
        // Update the user document
        await db.collection('users').doc(userDoc.id).update({
          savedJobs: uniqueSavedJobs
        });
        
        console.log(`   ✅ Updated successfully`);
      }
    }
    
    console.log(`\n${'='.repeat(60)}`);
    console.log('\n📊 SUMMARY:');
    console.log(`   Total users with saved jobs: ${totalUsers}`);
    console.log(`   Users with duplicates: ${usersWithDuplicates}`);
    console.log(`   Total duplicates removed: ${totalDuplicatesRemoved}`);
    console.log('\n✅ FIX COMPLETE\n');
    
  } catch (error) {
    console.error('❌ Error fixing duplicates:', error);
    throw error;
  }
}

// Run the fix
fixDuplicateSavedJobs()
  .then(() => {
    console.log('Done!');
    process.exit(0);
  })
  .catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
  });
