const admin = require('firebase-admin');
const serviceAccount = require('../app/google-services.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: serviceAccount.project_info.project_id
});

const db = admin.firestore();

async function checkJobs() {
  console.log('🔍 Checking jobs in database...\n');
  
  try {
    // Check total active jobs
    const activeJobs = await db.collection('jobs')
      .where('isActive', '==', true)
      .get();
    
    console.log(`✅ Total ACTIVE jobs: ${activeJobs.size}`);
    
    // Check by category
    const categories = ['DELIVERY', 'HELPER', 'MAID', 'COOK', 'DRIVER', 'SECURITY'];
    
    for (const category of categories) {
      const categoryJobs = await db.collection('jobs')
        .where('isActive', '==', true)
        .where('category', '==', category)
        .get();
      
      console.log(`  - ${category}: ${categoryJobs.size} jobs`);
    }
    
    // Show first 5 jobs
    console.log('\n📋 First 5 active jobs:');
    const firstFive = await db.collection('jobs')
      .where('isActive', '==', true)
      .orderBy('createdAt', 'desc')
      .limit(5)
      .get();
    
    firstFive.forEach((doc, index) => {
      const data = doc.data();
      console.log(`\n${index + 1}. ${data.title}`);
      console.log(`   ID: ${doc.id}`);
      console.log(`   Category: ${data.category}`);
      console.log(`   isActive: ${data.isActive}`);
      console.log(`   isFilled: ${data.isFilled || false}`);
      console.log(`   Location: ${data.location}`);
    });
    
    // Check for any jobs without isActive field
    const allJobs = await db.collection('jobs').limit(10).get();
    console.log(`\n📊 Total jobs (including inactive): ${allJobs.size}`);
    
    let withoutIsActive = 0;
    allJobs.forEach(doc => {
      if (doc.data().isActive === undefined) {
        withoutIsActive++;
      }
    });
    
    if (withoutIsActive > 0) {
      console.log(`⚠️  WARNING: ${withoutIsActive} jobs missing 'isActive' field`);
    }
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
  
  process.exit(0);
}

checkJobs();
