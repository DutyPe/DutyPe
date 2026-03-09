/**
 * Audit Jobs Database
 * Checks:
 * 1. Total jobs count
 * 2. Active vs inactive jobs
 * 3. Expired jobs
 * 4. Jobs with missing fields
 * 5. Category distribution
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

async function auditDatabase() {
  console.log('🔍 ========== DATABASE AUDIT START ==========\n');
  
  try {
    // Fetch all jobs
    const jobsSnapshot = await db.collection('jobs').get();
    const allJobs = jobsSnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    
    console.log(`📊 TOTAL JOBS IN DATABASE: ${allJobs.length}\n`);
    
    // 1. Active vs Inactive
    const activeJobs = allJobs.filter(job => job.isActive === true);
    const inactiveJobs = allJobs.filter(job => job.isActive !== true);
    console.log(`✅ Active Jobs: ${activeJobs.length}`);
    console.log(`❌ Inactive Jobs: ${inactiveJobs.length}\n`);
    
    // 2. Expired Jobs
    const now = Date.now();
    const expiredJobs = activeJobs.filter(job => {
      const expiresAt = job.expiresAt || 0;
      return expiresAt > 0 && expiresAt < now;
    });
    console.log(`⏰ Expired Jobs (but still active): ${expiredJobs.length}`);
    if (expiredJobs.length > 0) {
      console.log('   Sample expired jobs:');
      expiredJobs.slice(0, 3).forEach(job => {
        const expiredDays = Math.floor((now - job.expiresAt) / (1000 * 60 * 60 * 24));
        console.log(`   - ${job.title} (expired ${expiredDays} days ago)`);
      });
    }
    console.log('');
    
    // 3. Filled Jobs
    const filledJobs = activeJobs.filter(job => job.isFilled === true);
    console.log(`📦 Filled Jobs: ${filledJobs.length}\n`);
    
    // 4. Available Jobs (active, not expired, not filled)
    const availableJobs = activeJobs.filter(job => {
      const expiresAt = job.expiresAt || 0;
      const isExpired = expiresAt > 0 && expiresAt < now;
      return !isExpired && job.isFilled !== true;
    });
    console.log(`🟢 AVAILABLE JOBS (should show in app): ${availableJobs.length}\n`);
    
    // 5. Missing Fields Check
    console.log('🔍 MISSING FIELDS ANALYSIS:');
    const requiredFields = ['title', 'companyName', 'location', 'payAmount', 'category', 'postedAt'];
    const jobsWithMissingFields = [];
    
    availableJobs.forEach(job => {
      const missing = requiredFields.filter(field => !job[field] || job[field] === '');
      if (missing.length > 0) {
        jobsWithMissingFields.push({ id: job.id, title: job.title, missing });
      }
    });
    
    console.log(`   Jobs with missing fields: ${jobsWithMissingFields.length}`);
    if (jobsWithMissingFields.length > 0) {
      console.log('   Sample jobs with issues:');
      jobsWithMissingFields.slice(0, 5).forEach(job => {
        console.log(`   - ${job.title || 'NO TITLE'} (missing: ${job.missing.join(', ')})`);
      });
    }
    console.log('');
    
    // 6. Category Distribution
    console.log('📂 CATEGORY DISTRIBUTION:');
    const categoryCount = {};
    availableJobs.forEach(job => {
      const cat = job.category || 'UNKNOWN';
      categoryCount[cat] = (categoryCount[cat] || 0) + 1;
    });
    
    Object.entries(categoryCount)
      .sort((a, b) => b[1] - a[1])
      .forEach(([cat, count]) => {
        console.log(`   ${cat}: ${count} jobs`);
      });
    console.log('');
    
    // 7. Jobs without coordinates
    const jobsWithoutLocation = availableJobs.filter(job => 
      !job.latitude || !job.longitude || job.latitude === 0 || job.longitude === 0
    );
    console.log(`📍 Jobs without coordinates: ${jobsWithoutLocation.length}`);
    if (jobsWithoutLocation.length > 0) {
      console.log('   Sample:');
      jobsWithoutLocation.slice(0, 3).forEach(job => {
        console.log(`   - ${job.title} at ${job.location}`);
      });
    }
    console.log('');
    
    // 8. Summary
    console.log('📋 SUMMARY:');
    console.log(`   Total in DB: ${allJobs.length}`);
    console.log(`   Should show in app: ${availableJobs.length}`);
    console.log(`   Hidden (expired): ${expiredJobs.length}`);
    console.log(`   Hidden (filled): ${filledJobs.length}`);
    console.log(`   Hidden (inactive): ${inactiveJobs.length}`);
    console.log(`   Issues (missing fields): ${jobsWithMissingFields.length}`);
    
    console.log('\n🔍 ========== DATABASE AUDIT COMPLETE ==========');
    
  } catch (error) {
    console.error('❌ Error during audit:', error);
  }
  
  process.exit(0);
}

auditDatabase();
