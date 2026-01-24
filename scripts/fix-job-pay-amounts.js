/**
 * Fix Pay Amounts in Existing Jobs
 * 
 * This script updates all jobs with payAmount = 0 or missing
 * to have proper salary values as strings
 * 
 * Usage: node scripts/fix-job-pay-amounts.js
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Check if service account key exists
const possibleKeyFiles = [
  'serviceAccountKey.json',
  'dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json'
];

let serviceAccountPath = null;
for (const filename of possibleKeyFiles) {
  const testPath = path.join(__dirname, filename);
  if (fs.existsSync(testPath)) {
    serviceAccountPath = testPath;
    console.log(`✅ Found service account key: ${filename}`);
    break;
  }
}

if (!serviceAccountPath) {
  console.error('❌ ERROR: Service account key not found!');
  process.exit(1);
}

// Initialize Firebase Admin
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutypeapp'
});

const db = admin.firestore();

// Company pay ranges
const companyPayRanges = {
  'Zomato': { min: 18000, max: 30000 },
  'Blinkit': { min: 20000, max: 32000 },
  'BigBasket': { min: 18000, max: 28000 },
  'Swiggy': { min: 18000, max: 30000 },
  'Urban Company': { min: 19000, max: 29000 },
  'JioMart': { min: 18000, max: 28000 },
  'Zepto': { min: 20000, max: 33000 },
  'Dunzo': { min: 17000, max: 27000 },
  'Amazon': { min: 19000, max: 30000 },
  'Flipkart': { min: 18000, max: 29000 }
};

async function fixPayAmounts() {
  console.log('🔧 Fixing pay amounts in existing jobs...\n');
  
  try {
    // Get all jobs
    const snapshot = await db.collection('jobs').get();
    
    let fixCount = 0;
    let skipCount = 0;
    const batchSize = 500; // Firestore batch limit
    let batch = db.batch();
    let batchCount = 0;
    
    for (const doc of snapshot.docs) {
      const data = doc.data();
      const payAmount = data.payAmount;
      
      // Check if payAmount needs fixing (is 0, empty string, or missing)
      if (!payAmount || payAmount === '0' || payAmount === 0 || payAmount === '') {
        const companyName = data.companyName || 'Unknown';
        const payRange = companyPayRanges[companyName] || { min: 18000, max: 28000 };
        
        // Generate random pay amount within range
        const newPayAmount = Math.floor(
          payRange.min + Math.random() * (payRange.max - payRange.min)
        );
        
        // Update the job
        batch.update(doc.ref, {
          payAmount: newPayAmount.toString(),
          payType: 'MONTHLY'
        });
        
        fixCount++;
        batchCount++;
        
        console.log(`✅ Fixed: ${doc.id} - ${companyName} - ₹${newPayAmount}/month`);
        
        // Commit batch if we hit the limit
        if (batchCount >= batchSize) {
          await batch.commit();
          console.log(`\n📦 Committed batch of ${batchCount} updates\n`);
          batch = db.batch();
          batchCount = 0;
        }
      } else {
        skipCount++;
      }
    }
    
    // Commit remaining updates
    if (batchCount > 0) {
      await batch.commit();
      console.log(`\n📦 Committed final batch of ${batchCount} updates\n`);
    }
    
    console.log('\n🎉 Fix completed!');
    console.log(`✅ Fixed: ${fixCount} jobs`);
    console.log(`⏭️  Skipped: ${skipCount} jobs (already have pay amounts)`);
    
    if (fixCount > 0) {
      console.log('\n📱 Open your app and pull-to-refresh to see the updated pay amounts!');
    }
    
  } catch (error) {
    console.error('❌ Error fixing pay amounts:', error.message);
    throw error;
  }
}

// Run the script
fixPayAmounts()
  .then(() => {
    console.log('\n✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
