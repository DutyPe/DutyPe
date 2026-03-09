/**
 * Fix Expired Jobs
 * Makes expired jobs active again by extending their expiry date
 */

const admin = require('firebase-admin');
const serviceAccount = require('../app/google-services.json');

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: serviceAccount.project_id,
      clientEmail: serviceAccount.client[0].oauth_client[0].client_id + '@' + serviceAccount.project_id + '.iam.gserviceaccount.com',
      privateKey: '-----BEGIN PRIVATE KEY-----\nDUMMY_KEY\n-----END PRIVATE KEY-----\n'
    })
  });
}

const db = admin.firestore();

async function fixExpiredJobs() {
  console.log('🔧 ========== FIXING EXPIRED JOBS ==========\n');
  
  try {
    // Fetch all active jobs
    const jobsSnapshot = await db.collection('jobs').where('isActive', '==', true).get();
    const jobs = jobsSnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    
    console.log(`📊 Found ${jobs.length} active jobs\n`);
    
    // Find expired jobs
    const now = Date.now();
    const expiredJobs = jobs.filter(job => {
      const expiresAt = job.expiresAt || 0;
      return expiresAt > 0 && expiresAt < now;
    });
    
    console.log(`⏰ Found ${expiredJobs.length} expired jobs\n`);
    
    if (expiredJobs.length === 0) {
      console.log('✅ No expired jobs to fix!');
      process.exit(0);
      return;
    }
    
    // Extend expiry by 30 days
    const thirtyDaysInMs = 30 * 24 * 60 * 60 * 1000;
    const newExpiryDate = now + thirtyDaysInMs;
    
    console.log('🔄 Extending expiry date by 30 days...\n');
    
    const batch = db.batch();
    let count = 0;
    
    for (const job of expiredJobs) {
      const jobRef = db.collection('jobs').doc(job.id);
      batch.update(jobRef, {
        expiresAt: newExpiryDate,
        updatedAt: now
      });
      
      count++;
      console.log(`   ✓ ${count}. ${job.title} - Extended to ${new Date(newExpiryDate).toLocaleDateString()}`);
      
      // Firestore batch limit is 500
      if (count % 500 === 0) {
        await batch.commit();
        console.log(`\n   💾 Committed batch of 500 updates\n`);
      }
    }
    
    // Commit remaining
    if (count % 500 !== 0) {
      await batch.commit();
    }
    
    console.log(`\n✅ Successfully extended expiry for ${count} jobs!`);
    console.log(`   New expiry date: ${new Date(newExpiryDate).toLocaleDateString()}`);
    
    console.log('\n🔧 ========== FIX COMPLETE ==========');
    
  } catch (error) {
    console.error('❌ Error fixing expired jobs:', error);
  }
  
  process.exit(0);
}

fixExpiredJobs();
