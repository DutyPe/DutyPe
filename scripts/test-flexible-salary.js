/**
 * Test script to verify flexible salary format works
 * Tests: direct amount, range, and text-based salary
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin
const serviceAccount = require('../app/google-services.json');

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: serviceAccount.project_id,
      clientEmail: serviceAccount.client_email,
      privateKey: serviceAccount.private_key
    })
  });
}

const db = admin.firestore();

async function testFlexibleSalary() {
  console.log('🧪 Testing flexible salary format...\n');

  const testJobs = [
    {
      title: 'Delivery Executive',
      companyName: 'Test Company',
      payAmount: '15000', // Direct amount
      payType: 'MONTHLY',
      location: 'Khammam, Telangana',
      description: 'Test job with direct salary amount'
    },
    {
      title: 'Sales Executive',
      companyName: 'Test Company',
      payAmount: '11000-15000', // Range
      payType: 'MONTHLY',
      location: 'Khammam, Telangana',
      description: 'Test job with salary range'
    },
    {
      title: 'Manager',
      companyName: 'Test Company',
      payAmount: 'Based on experience', // Text
      payType: 'MONTHLY',
      location: 'Khammam, Telangana',
      description: 'Test job with text-based salary'
    }
  ];

  const jobIds = [];

  for (const job of testJobs) {
    try {
      const jobRef = db.collection('jobs').doc();
      const jobData = {
        ...job,
        id: jobRef.id,
        jobId: jobRef.id,
        employerId: 'test-employer',
        latitude: 17.2473,
        longitude: 80.1514,
        shiftTiming: '9 AM - 6 PM',
        benefits: ['Test benefit'],
        requirements: ['Test requirement'],
        vacancies: 1,
        isActive: true,
        postedAt: Date.now(),
        contactNumber: '9390515834',
        jobType: 'FULL_TIME',
        gender: 'Any',
        applicationCount: 0,
        landmark: 'Test Landmark',
        urgency: 'NORMAL',
        isFilled: false,
        jobImageUrl: '',
        expiryDays: 30
      };

      await jobRef.set(jobData);
      jobIds.push(jobRef.id);
      console.log(`✅ Posted: ${job.title} - Salary: ₹${job.payAmount}`);
    } catch (error) {
      console.error(`❌ Error posting ${job.title}:`, error.message);
    }
  }

  console.log(`\n✅ Posted ${jobIds.length} test jobs`);
  console.log('Job IDs:', jobIds);
  console.log('\n📱 Open the app and check if salaries display correctly');
  console.log('💡 To delete these test jobs, run: node scripts/delete-test-jobs.js');
}

testFlexibleSalary().catch(console.error);
