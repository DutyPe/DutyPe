/**
 * Admin Script: Post Reliance Retail jobs in Khammam
 * Company: Reliance Retail
 * Location: Khammam District, Telangana
 * Contact: 9390515834
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Check if service account key exists - try multiple possible filenames
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
  console.error('');
  console.error('Please follow these steps:');
  console.error('1. Go to Firebase Console: https://console.firebase.google.com/project/dutypeapp/settings/serviceaccounts/adminsdk');
  console.error('2. Click "Generate new private key"');
  console.error('3. Save the downloaded file in the scripts folder');
  console.error('');
  process.exit(1);
}

// Initialize Firebase Admin with service account
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutypeapp'
});

const db = admin.firestore();

// Khammam coordinates
const KHAMMAM_LAT = 17.2473;
const KHAMMAM_LNG = 80.1514;

const relianceJobs = [
  {
    title: 'Customer Service Associate',
    description: 'Handle customer queries, assist with product selection, and ensure excellent customer experience at Reliance Retail store. Good communication skills required.',
    payAmount: '12000-15000',
    payType: 'MONTHLY',
    shiftTiming: '9 AM - 9 PM (Rotational shifts)',
    vacancies: 5,
    requirements: [
      'Good communication skills in Telugu and English',
      'Customer service experience preferred',
      'Friendly and helpful attitude',
      'Basic computer knowledge'
    ],
    benefits: ['PF', 'ESI', 'Performance incentives', 'Employee discount'],
    ageRange: '18-35',
    gender: 'Any',
    jobType: 'FULL_TIME'
  },
  {
    title: 'Cashier',
    description: 'Handle cash and card transactions, maintain billing accuracy, and provide quick checkout service to customers at Reliance Retail store.',
    payAmount: '10000-12000',
    payType: 'MONTHLY',
    shiftTiming: '9 AM - 9 PM (Rotational shifts)',
    vacancies: 3,
    requirements: [
      'Basic math and computer skills',
      'Cash handling experience preferred',
      'Attention to detail',
      'Honest and reliable'
    ],
    benefits: ['PF', 'ESI', 'Performance bonus', 'Employee discount'],
    ageRange: '18-35',
    gender: 'Any',
    jobType: 'FULL_TIME'
  },
  {
    title: 'Fashion Consultant',
    description: 'Assist customers with fashion choices, provide styling advice, and help with product selection in the fashion section of Reliance Retail store.',
    payAmount: '13000-16000',
    payType: 'MONTHLY',
    shiftTiming: '9 AM - 9 PM (Rotational shifts)',
    vacancies: 4,
    requirements: [
      'Interest in fashion and trends',
      'Good communication and styling sense',
      'Customer service experience preferred',
      'Presentable appearance'
    ],
    benefits: ['PF', 'ESI', 'Sales incentives', 'Employee discount', 'Fashion training'],
    ageRange: '18-35',
    gender: 'Any',
    jobType: 'FULL_TIME'
  },
  {
    title: 'Sr Customer Service Associate',
    description: 'Lead customer service team, handle escalations, train new associates, and ensure smooth store operations at Reliance Retail. Previous retail experience required.',
    payAmount: '16000-20000',
    payType: 'MONTHLY',
    shiftTiming: '9 AM - 9 PM (Rotational shifts)',
    vacancies: 2,
    requirements: [
      '2+ years retail experience',
      'Team handling experience',
      'Excellent communication skills',
      'Problem-solving abilities',
      'Leadership qualities'
    ],
    benefits: ['PF', 'ESI', 'Performance incentives', 'Employee discount', 'Career growth'],
    ageRange: '22-40',
    gender: 'Any',
    jobType: 'FULL_TIME'
  }
];

async function postRelianceJobs() {
  console.log('🏢 Posting Reliance Retail jobs in Khammam...\n');

  const jobIds = [];
  const companyName = 'Reliance Retail';
  const contactNumber = '9390515834';
  const location = 'Khammam District, Telangana';
  const landmark = 'Near Khammam Bus Stand';

  for (const job of relianceJobs) {
    try {
      const jobRef = db.collection('jobs').doc();
      const jobData = {
        id: jobRef.id,
        jobId: jobRef.id,
        employerId: 'admin-reliance-retail',
        title: job.title,
        companyName: companyName,
        location: location,
        latitude: KHAMMAM_LAT,
        longitude: KHAMMAM_LNG,
        payAmount: job.payAmount,
        payType: job.payType,
        shiftTiming: job.shiftTiming,
        description: job.description,
        benefits: job.benefits,
        requirements: job.requirements,
        vacancies: job.vacancies,
        isActive: true,
        postedAt: Date.now(),
        createdAt: Date.now(), // CRITICAL: Required for Firestore query ordering
        category: 'OTHER', // CRITICAL: Required for Firestore query filtering
        contactNumber: contactNumber,
        jobType: job.jobType,
        ageRange: job.ageRange,
        gender: job.gender,
        applicationCount: 0,
        landmark: landmark,
        urgency: 'NORMAL',
        isFilled: false,
        jobImageUrl: '',
        expiryDays: 30
      };

      await jobRef.set(jobData);
      jobIds.push(jobRef.id);
      console.log(`✅ Posted: ${job.title}`);
      console.log(`   💰 Salary: ₹${job.payAmount} ${job.payType}`);
      console.log(`   👥 Vacancies: ${job.vacancies}`);
      console.log('');
    } catch (error) {
      console.error(`❌ Error posting ${job.title}:`, error.message);
    }
  }

  console.log(`\n✅ Successfully posted ${jobIds.length} Reliance Retail jobs in Khammam`);
  console.log(`📍 Location: ${location}`);
  console.log(`📞 Contact: ${contactNumber}`);
  console.log(`🏢 Company: ${companyName}`);
  console.log('\nJob IDs:', jobIds);
}

postRelianceJobs()
  .then(() => {
    console.log('\n✨ Done!');
    process.exit(0);
  })
  .catch(error => {
    console.error('Error:', error);
    process.exit(1);
  });
