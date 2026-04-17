/**
 * DutyPe - Metadata Seeding Script
 * 
 * This script creates the initial metadata documents in Firestore.
 * Run with: node seed-metadata.js
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Check if service account key exists
const possibleKeyFiles = [
  'serviceAccountKey.json',
  'dutype-860ac-firebase-adminsdk.json'
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
  projectId: 'dutype-860ac'
});

const db = admin.firestore();

async function seedMetadata() {
  console.log('🚀 Seeding metadata...\n');
  
  // ==========================================
  // 1. PLATFORM STATS
  // ==========================================
  console.log('📊 Creating platform_stats...');
  await db.collection('metadata').doc('platform_stats').set({
    totalJobs: 425,
    activeJobs: 400,
    totalWorkers: 0,
    totalEmployers: 0,
    totalApplications: 0,
    jobsPostedToday: 25,
    applicationsToday: 0,
    averageResponseTime: 2.5, // hours
    topCategories: ['COOK', 'MAID', 'DRIVER', 'DELIVERY', 'SECURITY'],
    topLocations: ['Hyderabad', 'Gachibowli', 'Madhapur', 'Kondapur', 'Kukatpally'],
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  console.log('   ✅ platform_stats created');
  
  // ==========================================
  // 2. FEATURE FLAGS
  // ==========================================
  console.log('📊 Creating feature_flags...');
  await db.collection('metadata').doc('feature_flags').set({
    // Feature toggles
    isChatEnabled: true,
    isMapViewEnabled: true,
    isSubscriptionEnabled: true,
    isReferralEnabled: true,
    isWorkVerificationEnabled: true,
    isRatingEnabled: true,
    isWhatsAppApplyEnabled: true,
    isDigitalCardEnabled: true,
    
    // Limits
    maxFreeJobPosts: 3,
    maxFreeApplications: 10,
    jobExpiryDays: 15,
    
    // App control
    maintenanceMode: false,
    maintenanceMessage: '',
    minAppVersion: '1.0.0',
    forceUpdateVersion: '',
    
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  console.log('   ✅ feature_flags created');
  
  // ==========================================
  // 3. CATEGORY STATS
  // ==========================================
  console.log('📊 Creating category_stats...');
  const categories = {
    COOK: { totalJobs: 25, activeJobs: 23, averagePay: 15000, payTypes: ['DAILY', 'MONTHLY'] },
    MAID: { totalJobs: 25, activeJobs: 24, averagePay: 12000, payTypes: ['DAILY', 'MONTHLY'] },
    DRIVER: { totalJobs: 25, activeJobs: 22, averagePay: 20000, payTypes: ['DAILY', 'MONTHLY'] },
    HELPER: { totalJobs: 25, activeJobs: 25, averagePay: 12000, payTypes: ['DAILY', 'HOURLY'] },
    SECURITY: { totalJobs: 25, activeJobs: 24, averagePay: 15000, payTypes: ['DAILY', 'MONTHLY'] },
    GARDENER: { totalJobs: 25, activeJobs: 23, averagePay: 12000, payTypes: ['DAILY', 'MONTHLY'] },
    CARETAKER: { totalJobs: 25, activeJobs: 22, averagePay: 18000, payTypes: ['DAILY', 'MONTHLY'] },
    DELIVERY: { totalJobs: 25, activeJobs: 25, averagePay: 15000, payTypes: ['DAILY', 'HOURLY'] },
    WAITER: { totalJobs: 25, activeJobs: 24, averagePay: 14000, payTypes: ['DAILY', 'MONTHLY'] },
    ELECTRICIAN: { totalJobs: 25, activeJobs: 23, averagePay: 20000, payTypes: ['DAILY', 'HOURLY'] },
    PLUMBER: { totalJobs: 25, activeJobs: 22, averagePay: 18000, payTypes: ['DAILY', 'HOURLY'] },
    PAINTER: { totalJobs: 25, activeJobs: 24, averagePay: 16000, payTypes: ['DAILY', 'HOURLY'] },
    CARPENTER: { totalJobs: 25, activeJobs: 23, averagePay: 18000, payTypes: ['DAILY', 'HOURLY'] },
    RECEPTIONIST: { totalJobs: 25, activeJobs: 24, averagePay: 15000, payTypes: ['MONTHLY'] },
    CASHIER: { totalJobs: 25, activeJobs: 25, averagePay: 12000, payTypes: ['DAILY', 'MONTHLY'] },
    PACKER: { totalJobs: 25, activeJobs: 24, averagePay: 10000, payTypes: ['DAILY', 'HOURLY'] },
    OTHER: { totalJobs: 25, activeJobs: 23, averagePay: 12000, payTypes: ['DAILY', 'HOURLY', 'MONTHLY'] }
  };
  
  await db.collection('metadata').doc('category_stats').set({
    ...categories,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  console.log('   ✅ category_stats created');
  
  // ==========================================
  // 4. LOCATION STATS
  // ==========================================
  console.log('📊 Creating location_stats...');
  const locations = {
    'Hyderabad': { totalJobs: 425, activeJobs: 400, topCategories: ['COOK', 'MAID', 'DRIVER'] },
    'Gachibowli': { totalJobs: 45, activeJobs: 42, topCategories: ['COOK', 'MAID', 'SECURITY'] },
    'Madhapur': { totalJobs: 40, activeJobs: 38, topCategories: ['DELIVERY', 'DRIVER', 'COOK'] },
    'Kondapur': { totalJobs: 35, activeJobs: 33, topCategories: ['MAID', 'COOK', 'HELPER'] },
    'Kukatpally': { totalJobs: 38, activeJobs: 36, topCategories: ['DELIVERY', 'HELPER', 'SECURITY'] },
    'Miyapur': { totalJobs: 30, activeJobs: 28, topCategories: ['COOK', 'MAID', 'DRIVER'] },
    'Hitech City': { totalJobs: 42, activeJobs: 40, topCategories: ['DELIVERY', 'DRIVER', 'SECURITY'] },
    'Jubilee Hills': { totalJobs: 35, activeJobs: 33, topCategories: ['COOK', 'MAID', 'GARDENER'] },
    'Banjara Hills': { totalJobs: 32, activeJobs: 30, topCategories: ['COOK', 'MAID', 'CARETAKER'] },
    'Secunderabad': { totalJobs: 40, activeJobs: 38, topCategories: ['DELIVERY', 'HELPER', 'DRIVER'] }
  };
  
  await db.collection('metadata').doc('location_stats').set({
    ...locations,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  console.log('   ✅ location_stats created');
  
  // ==========================================
  // 5. TRENDING DATA
  // ==========================================
  console.log('📊 Creating trending...');
  await db.collection('metadata').doc('trending').set({
    categories: [
      { category: 'DELIVERY', jobCount: 25, trend: 'UP' },
      { category: 'COOK', jobCount: 25, trend: 'STABLE' },
      { category: 'MAID', jobCount: 25, trend: 'UP' },
      { category: 'DRIVER', jobCount: 25, trend: 'STABLE' },
      { category: 'SECURITY', jobCount: 25, trend: 'UP' },
      { category: 'HELPER', jobCount: 25, trend: 'STABLE' },
      { category: 'ELECTRICIAN', jobCount: 25, trend: 'UP' },
      { category: 'PLUMBER', jobCount: 25, trend: 'STABLE' }
    ],
    locations: [
      'Gachibowli',
      'Madhapur',
      'Hitech City',
      'Kondapur',
      'Kukatpally',
      'Secunderabad',
      'Jubilee Hills',
      'Banjara Hills'
    ],
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  console.log('   ✅ trending created');
  
  // ==========================================
  // 6. PAY STATS
  // ==========================================
  console.log('📊 Creating pay_stats...');
  await db.collection('metadata').doc('pay_stats').set({
    minPay: 300,      // ₹300/day minimum
    maxPay: 30000,    // ₹30,000/month maximum
    averagePay: 15000, // ₹15,000/month average
    medianPay: 14000,  // ₹14,000/month median
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });
  console.log('   ✅ pay_stats created');
  
  console.log('\n🎉 Metadata seeding complete!');
  console.log('📊 Created 6 metadata documents in Firestore');
}

// Run the seeding
seedMetadata()
  .then(() => {
    console.log('\n✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
