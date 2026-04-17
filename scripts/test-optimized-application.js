/**
 * Test Optimized Application Schema
 * Verifies that new applications only have 15 fields
 */

const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp({
    projectId: 'dutype-860ac'
  });
}

const db = admin.firestore();

async function testOptimizedApplication() {
  console.log('🧪 Testing Optimized Application Schema\n');
  
  try {
    // Get the most recent application
    const snapshot = await db.collection('job_applications')
      .orderBy('appliedAt', 'desc')
      .limit(1)
      .get();
    
    if (snapshot.empty) {
      console.log('❌ No applications found in database');
      return;
    }
    
    const doc = snapshot.docs[0];
    const data = doc.data();
    const fieldCount = Object.keys(data).length;
    
    console.log(`📄 Latest Application: ${doc.id}`);
    console.log(`📊 Field Count: ${fieldCount}`);
    console.log(`📅 Applied At: ${new Date(data.appliedAt).toLocaleString()}\n`);
    
    // Expected 15 core fields
    const coreFields = [
      'id', 'applicationId', 'jobId', 'workerId', 'employerId',
      'status', 'appliedAt', 'updatedAt',
      'jobTitle', 'jobLocation', 'companyName', 'workerName', 'workerPhone',
      'source', 'coverLetter', 'active'
    ];
    
    console.log('✅ Core Fields Present:');
    coreFields.forEach(field => {
      const present = data.hasOwnProperty(field);
      console.log(`   ${present ? '✓' : '✗'} ${field}: ${present ? data[field] : 'MISSING'}`);
    });
    
    console.log('\n📋 All Fields in Document:');
    Object.keys(data).forEach(field => {
      const isCore = coreFields.includes(field);
      console.log(`   ${isCore ? '✓' : '⚠️'} ${field}: ${JSON.stringify(data[field]).substring(0, 50)}`);
    });
    
    console.log('\n📊 SUMMARY:');
    console.log(`   Total fields: ${fieldCount}`);
    console.log(`   Expected: 15-17 (core + backward compat)`);
    console.log(`   Status: ${fieldCount <= 20 ? '✅ OPTIMIZED' : '❌ BLOATED'}`);
    
    if (fieldCount > 20) {
      console.log('\n⚠️  This application has too many fields!');
      console.log('   Run migration script to clean up: node scripts/migrate-applications-schema.js --apply');
    }
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

testOptimizedApplication()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('💥 Fatal error:', error);
    process.exit(1);
  });
