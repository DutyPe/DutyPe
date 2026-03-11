// Run this with: firebase firestore:get job_applications --where workerId==g0XbC8DpPCa94DJI00YEWh8eyid2

// Or use this Node script with firebase-admin
const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');

// Initialize with application default credentials
initializeApp();
const db = getFirestore();

async function debugApplications() {
  const workerId = 'g0XbC8DpPCa94DJI00YEWh8eyid2';
  
  console.log(`\n🔍 Debugging applications for workerId: ${workerId}\n`);
  
  try {
    const snapshot = await db.collection('job_applications')
      .where('workerId', '==', workerId)
      .get();
    
    console.log(`📊 Found ${snapshot.size} documents\n`);
    
    snapshot.forEach((doc, index) => {
      const data = doc.data();
      console.log(`\n📄 Document ${index + 1}/${snapshot.size}: ${doc.id}`);
      console.log(`   All fields (${Object.keys(data).length}): ${Object.keys(data).sort().join(', ')}`);
      console.log(`   Status: ${data.status} (type: ${typeof data.status})`);
      console.log(`   Active: ${data.active} (type: ${typeof data.active})`);
      console.log(`   Source: ${data.source} (type: ${typeof data.source})`);
      
      // Check for enum fields that might have wrong type
      if (data.status && typeof data.status !== 'string') {
        console.log(`   ❌ PROBLEM: Status is not a string!`);
      }
      if (data.source && typeof data.source !== 'string') {
        console.log(`   ❌ PROBLEM: Source is not a string!`);
      }
      
      // Print full document for first one
      if (index === 0) {
        console.log(`\n   📋 Full document data:`);
        console.log(JSON.stringify(data, null, 2));
      }
    });
    
    console.log('\n✅ Debug complete\n');
    
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

debugApplications()
  .then(() => process.exit(0))
  .catch(error => {
    console.error(error);
    process.exit(1);
  });
