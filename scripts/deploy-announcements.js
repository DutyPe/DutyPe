/**
 * Deploy Announcements to Firebase
 * 
 * This script deploys sample announcements to Firestore for testing
 * the announcement banner feature in the app.
 * 
 * Usage:
 * node scripts/deploy-announcements.js
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
  console.error('Please download the service account key and place it in the scripts folder');
  process.exit(1);
}

// Initialize Firebase Admin
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutype-860ac'
});

const db = admin.firestore();

// Sample announcements
const announcements = [
  {
    id: 'welcome_2026',
    title: 'Welcome to DutyPe! 🎉',
    message: 'Find jobs and workers instantly. Start your journey today!',
    type: 'INFO',
    priority: 'HIGH',
    targetRole: null, // null = all users
    startDate: admin.firestore.Timestamp.now(),
    endDate: admin.firestore.Timestamp.fromDate(new Date('2026-12-31')),
    isActive: true,
    isDismissible: true,
    actionText: 'Get Started',
    actionRoute: 'dutype://home',
    imageUrl: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    createdBy: 'system'
  },
  {
    id: 'new_features_jan_2026',
    title: 'New Features Available! 🚀',
    message: 'Check out our new smart notifications and deep linking features',
    type: 'FEATURE',
    priority: 'MEDIUM',
    targetRole: null, // null = all users
    startDate: admin.firestore.Timestamp.now(),
    endDate: admin.firestore.Timestamp.fromDate(new Date('2026-02-28')),
    isActive: true,
    isDismissible: true,
    actionText: 'Learn More',
    actionRoute: 'dutype://notifications',
    imageUrl: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    createdBy: 'system'
  },
  {
    id: 'worker_bonus_jan_2026',
    title: 'Workers: Earn Bonus! 💰',
    message: 'Complete your profile and get ₹50 bonus on your first job',
    type: 'PROMOTION',
    priority: 'HIGH',
    targetRole: 'worker', // workers only
    startDate: admin.firestore.Timestamp.now(),
    endDate: admin.firestore.Timestamp.fromDate(new Date('2026-03-31')),
    isActive: true,
    isDismissible: true,
    actionText: 'Complete Profile',
    actionRoute: 'dutype://worker/profile',
    imageUrl: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    createdBy: 'system'
  },
  {
    id: 'employer_discount_jan_2026',
    title: 'Employers: First Job Free! 🎁',
    message: 'Post your first job for free and find skilled workers instantly',
    type: 'PROMOTION',
    priority: 'HIGH',
    targetRole: 'employer', // employers only
    startDate: admin.firestore.Timestamp.now(),
    endDate: admin.firestore.Timestamp.fromDate(new Date('2026-03-31')),
    isActive: true,
    isDismissible: true,
    actionText: 'Post Job',
    actionRoute: 'dutype://employer/post-job',
    imageUrl: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    createdBy: 'system'
  },
  {
    id: 'referral_program_2026',
    title: 'Refer & Earn ₹250! 🎁',
    message: 'Invite friends and earn ₹25 for each successful referral',
    type: 'PROMOTION',
    priority: 'MEDIUM',
    targetRole: null, // null = all users
    startDate: admin.firestore.Timestamp.now(),
    endDate: admin.firestore.Timestamp.fromDate(new Date('2026-12-31')),
    isActive: true,
    isDismissible: true,
    actionText: 'Refer Now',
    actionRoute: 'dutype://refer',
    imageUrl: null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    createdBy: 'system'
  }
];

async function deployAnnouncements() {
  console.log('🚀 Deploying announcements to Firebase...\n');
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const announcement of announcements) {
    try {
      const docRef = db.collection('announcements').doc(announcement.id);
      await docRef.set(announcement);
      console.log(`✅ Created: ${announcement.title} (${announcement.targetRole || 'all'})`);
      successCount++;
    } catch (error) {
      console.error(`❌ Error creating ${announcement.title}:`, error.message);
      errorCount++;
    }
  }
  
  console.log('\n🎉 Deployment complete!');
  console.log(`✅ Successfully created: ${successCount} announcements`);
  if (errorCount > 0) {
    console.log(`❌ Failed: ${errorCount} announcements`);
  }
}

// Run deployment
deployAnnouncements()
  .then(() => {
    console.log('\n✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
