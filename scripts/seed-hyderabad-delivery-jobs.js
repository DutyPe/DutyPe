/**
 * Seed Delivery Jobs for Hyderabad, Telangana
 * 
 * Creates delivery jobs for major companies (Zomato, Blinkit, BigBasket, etc.)
 * with random phone numbers and locations in Hyderabad
 * 
 * Usage: node scripts/seed-hyderabad-delivery-jobs.js
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
  console.error('Please download the service account key and place it in the scripts folder');
  process.exit(1);
}

// Initialize Firebase Admin
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutypeapp'
});

const db = admin.firestore();

// Generate random phone numbers (Indian format)
function generateRandomPhoneNumbers(count) {
  const numbers = [];
  const prefixes = ['9', '8', '7', '6']; // Valid Indian mobile prefixes
  
  for (let i = 0; i < count; i++) {
    const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
    const remaining = Math.floor(Math.random() * 900000000) + 100000000; // 9 digits
    numbers.push(prefix + remaining.toString());
  }
  
  return numbers;
}

// Generate 50 random phone numbers for variety
const phoneNumbers = generateRandomPhoneNumbers(50);

// Delivery companies and job titles
const deliveryCompanies = [
  {
    name: 'Zomato',
    titles: [
      'Zomato Delivery Partner',
      'Zomato Food Delivery Executive',
      'Zomato Delivery Boy',
      'Zomato Rider',
      'Zomato Delivery Associate',
      'Zomato Fleet Partner'
    ],
    payRange: { min: 18000, max: 30000 }
  },
  {
    name: 'Blinkit',
    titles: [
      'Blinkit Delivery Partner',
      'Blinkit Delivery Executive',
      'Blinkit Rider',
      'Blinkit Delivery Boy',
      'Blinkit Quick Commerce Delivery',
      'Blinkit Express Delivery Partner'
    ],
    payRange: { min: 20000, max: 32000 }
  },
  {
    name: 'BigBasket',
    titles: [
      'BigBasket Delivery Partner',
      'BigBasket Delivery Executive',
      'BigBasket Delivery Boy',
      'BigBasket Rider',
      'BigBasket Grocery Delivery',
      'BigBasket BB Daily Partner'
    ],
    payRange: { min: 18000, max: 28000 }
  },
  {
    name: 'Swiggy',
    titles: [
      'Swiggy Delivery Partner',
      'Swiggy Delivery Executive',
      'Swiggy Delivery Boy',
      'Swiggy Rider',
      'Swiggy Instamart Delivery',
      'Swiggy Genie Partner'
    ],
    payRange: { min: 18000, max: 30000 }
  },
  {
    name: 'Urban Company',
    titles: [
      'Urban Company Delivery Partner',
      'Urban Company Service Partner',
      'Urban Company Delivery Executive',
      'Urban Company Logistics Partner'
    ],
    payRange: { min: 19000, max: 29000 }
  },
  {
    name: 'JioMart',
    titles: [
      'JioMart Delivery Partner',
      'JioMart Delivery Executive',
      'JioMart Delivery Boy',
      'JioMart Rider',
      'JioMart Express Delivery'
    ],
    payRange: { min: 18000, max: 28000 }
  },
  {
    name: 'Zepto',
    titles: [
      'Zepto Delivery Partner',
      'Zepto Quick Delivery Executive',
      'Zepto Rider',
      'Zepto Delivery Boy',
      'Zepto 10-Min Delivery Partner'
    ],
    payRange: { min: 20000, max: 33000 }
  },
  {
    name: 'Dunzo',
    titles: [
      'Dunzo Delivery Partner',
      'Dunzo Delivery Executive',
      'Dunzo Rider',
      'Dunzo Quick Delivery',
      'Dunzo Daily Partner'
    ],
    payRange: { min: 17000, max: 27000 }
  },
  {
    name: 'Amazon',
    titles: [
      'Amazon Delivery Associate',
      'Amazon Flex Partner',
      'Amazon Delivery Boy',
      'Amazon Logistics Partner'
    ],
    payRange: { min: 19000, max: 30000 }
  },
  {
    name: 'Flipkart',
    titles: [
      'Flipkart Delivery Executive',
      'Flipkart Delivery Boy',
      'Flipkart Logistics Partner',
      'Flipkart Ekart Partner'
    ],
    payRange: { min: 18000, max: 29000 }
  }
];

// Hyderabad locations (major areas)
const hyderabadLocations = [
  { area: 'Hitech City', lat: 17.4485, lng: 78.3908 },
  { area: 'Gachibowli', lat: 17.4399, lng: 78.3482 },
  { area: 'Madhapur', lat: 17.4483, lng: 78.3915 },
  { area: 'Kondapur', lat: 17.4617, lng: 78.3617 },
  { area: 'Kukatpally', lat: 17.4849, lng: 78.4138 },
  { area: 'Miyapur', lat: 17.4967, lng: 78.3583 },
  { area: 'Ameerpet', lat: 17.4374, lng: 78.4482 },
  { area: 'Begumpet', lat: 17.4399, lng: 78.4683 },
  { area: 'Secunderabad', lat: 17.4399, lng: 78.4983 },
  { area: 'Banjara Hills', lat: 17.4239, lng: 78.4738 },
  { area: 'Jubilee Hills', lat: 17.4326, lng: 78.4071 },
  { area: 'Mehdipatnam', lat: 17.3915, lng: 78.4361 },
  { area: 'Dilsukhnagar', lat: 17.3687, lng: 78.5243 },
  { area: 'LB Nagar', lat: 17.3420, lng: 78.5526 },
  { area: 'Uppal', lat: 17.4065, lng: 78.5591 },
  { area: 'Kompally', lat: 17.5404, lng: 78.4909 },
  { area: 'Nizampet', lat: 17.5081, lng: 78.3889 },
  { area: 'Manikonda', lat: 17.4026, lng: 78.3771 },
  { area: 'Financial District', lat: 17.4239, lng: 78.3373 },
  { area: 'Shamshabad', lat: 17.2473, lng: 78.3984 },
  { area: 'Attapur', lat: 17.3667, lng: 78.4167 },
  { area: 'Tolichowki', lat: 17.3950, lng: 78.4050 },
  { area: 'Malakpet', lat: 17.3850, lng: 78.4950 },
  { area: 'Charminar', lat: 17.3616, lng: 78.4747 },
  { area: 'Abids', lat: 17.3850, lng: 78.4867 },
  { area: 'Nampally', lat: 17.3850, lng: 78.4650 },
  { area: 'Koti', lat: 17.3750, lng: 78.4850 },
  { area: 'Moosapet', lat: 17.4700, lng: 78.4300 },
  { area: 'SR Nagar', lat: 17.4400, lng: 78.4400 },
  { area: 'Erragadda', lat: 17.4500, lng: 78.4200 },
  { area: 'Panjagutta', lat: 17.4250, lng: 78.4500 },
  { area: 'Somajiguda', lat: 17.4300, lng: 78.4600 },
  { area: 'Lakdikapul', lat: 17.4100, lng: 78.4600 },
  { area: 'Masab Tank', lat: 17.4000, lng: 78.4700 },
  { area: 'Narayanguda', lat: 17.3900, lng: 78.4900 }
];

// Job descriptions templates
const jobDescriptions = {
  delivery: [
    'Join our delivery team and earn attractive incentives! Flexible working hours, weekly payouts, and fuel allowance provided. Immediate joining available.',
    'Urgent requirement for delivery partners in Hyderabad. Own vehicle required. Earn up to ₹35,000/month with incentives and bonuses. Apply now!',
    'Be your own boss! Flexible timings, daily earnings, and performance bonuses. Join India\'s fastest growing delivery platform today.',
    'Delivery partners needed urgently! Own two-wheeler required. Weekly payouts, insurance coverage, and fuel reimbursement provided.',
    'Earn while you ride! Join our delivery team with attractive pay, flexible hours, and growth opportunities. Start earning from day one.',
    'Immediate openings for delivery boys in Hyderabad. Smartphone and bike required. Earn ₹600-1000 daily with incentives.',
    'Join the delivery revolution! Competitive pay, flexible schedules, and excellent support. No experience required, training provided.',
    'Delivery executives wanted! Own vehicle mandatory. Daily payouts, fuel allowance, and performance bonuses. Apply today!',
    'Start earning today! Flexible part-time and full-time positions available. Own bike required. Join thousands of happy partners.',
    'Hyderabad\'s best delivery opportunity! High earnings, flexible hours, and great support. Be part of India\'s gig economy revolution.'
  ]
};

// Requirements templates
const requirements = [
  'Own two-wheeler (bike/scooter) mandatory\nValid driving license\nSmartphone with internet\nAge: 18-40 years\nGood knowledge of Hyderabad city',
  'Two-wheeler with valid documents\nAndroid smartphone\nBasic English/Telugu/Hindi communication\nWillingness to work flexible hours\nLocal resident preferred',
  'Own bike/scooter required\nValid DL and RC\nSmartphone mandatory\nGood communication skills\nReady to work in all weather conditions',
  'Vehicle: Bike/Scooter (mandatory)\nDocuments: DL, Aadhar, PAN\nAge: 18-45 years\nGood knowledge of local areas\nHardworking and punctual',
  'Two-wheeler ownership mandatory\nValid driving license\nSmartphone with GPS\nBasic Telugu/Hindi/English\nReady for immediate joining',
  'Own vehicle with valid papers\nSmart phone required\nAge 18-40 years\nKnowledge of Hyderabad roads\nGood attitude and punctuality'
];

// Benefits templates
const benefits = [
  'Weekly payouts\nFuel allowance ₹2500-4000/month\nPerformance incentives\nFlexible working hours\nInsurance coverage\nFree training provided',
  'Daily/Weekly earnings\nFuel reimbursement\nAttendance bonus\nReferral bonus ₹500\nAccident insurance\nCareer growth opportunities',
  'Attractive salary\nFuel allowance\nIncentive on deliveries\nFlexible timings\nMedical insurance\nFree uniform and delivery bag',
  'Competitive pay\nWeekly payouts\nPerformance bonuses up to ₹5000\nFuel support\nInsurance benefits\nTraining and 24/7 support',
  'High earning potential\nDaily/Weekly payments\nFuel reimbursement ₹3000/month\nIncentive structure\nAccident coverage\nGrowth opportunities',
  'Guaranteed minimum earnings\nFuel allowance\nPeak hour bonuses\nWeekly payouts\nInsurance\nFree training and support'
];

// Generate jobs
function generateJobs() {
  const jobs = [];
  let jobCounter = 0;
  
  // Create 200 jobs total (distributed across phone numbers)
  const totalJobs = 200;
  
  for (let i = 0; i < totalJobs; i++) {
    const company = deliveryCompanies[jobCounter % deliveryCompanies.length];
    const location = hyderabadLocations[Math.floor(Math.random() * hyderabadLocations.length)];
    const phone = phoneNumbers[Math.floor(Math.random() * phoneNumbers.length)];
    const title = company.titles[Math.floor(Math.random() * company.titles.length)];
    
    // Random pay amount within company range
    const payAmount = Math.floor(
      company.payRange.min + Math.random() * (company.payRange.max - company.payRange.min)
    );
    
    // Random vacancies between 20-100
    const vacancies = 20 + Math.floor(Math.random() * 81);

    const job = {
      id: `hyderabad_delivery_${Date.now()}_${jobCounter}`,
      title: title,
      companyName: company.name,
      description: jobDescriptions.delivery[Math.floor(Math.random() * jobDescriptions.delivery.length)],
      requirements: requirements[Math.floor(Math.random() * requirements.length)],
      benefits: benefits[Math.floor(Math.random() * benefits.length)],
      
      // Location
      location: `${location.area}, Hyderabad, Telangana`,
      city: 'Hyderabad',
      state: 'Telangana',
      latitude: location.lat,
      longitude: location.lng,
      
      // Pay
      payAmount: payAmount.toString(), // Convert to string for display
      payType: 'MONTHLY',
      
      // Contact
      contactNumber: phone,
      whatsappNumber: phone,
      
      // Job details
      jobType: 'FULL_TIME',
      workingHours: '8-10 hours',
      vacancies: vacancies,
      
      // Experience
      experienceRequired: 'FRESHER',
      educationRequired: '10th Pass',
      
      // Status
      isActive: true,
      isFilled: false,
      isVerified: true,
      
      // Metadata
      postedAt: admin.firestore.FieldValue.serverTimestamp(),
      createdAt: admin.firestore.FieldValue.serverTimestamp(), // CRITICAL: Required for query ordering
      expiresAt: admin.firestore.Timestamp.fromDate(
        new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) // 30 days
      ),
      
      // Employer info (system generated)
      employerId: 'system_hyderabad_delivery',
      employerName: company.name,
      employerRating: 4.0 + Math.random() * 0.9, // 4.0-4.9
      
      // Additional fields
      tags: ['delivery', 'bike', 'immediate-joining', 'flexible-hours'],
      applicationCount: 0,
      viewCount: 0,
      shareCount: 0
    };

    jobs.push(job);
    jobCounter++;
  }

  return jobs;
}

// Deploy jobs to Firestore
async function deployJobs() {
  console.log('🚀 Generating delivery jobs for Hyderabad, Telangana...\n');
  
  const jobs = generateJobs();
  console.log(`📦 Generated ${jobs.length} jobs with ${phoneNumbers.length} random phone numbers\n`);
  
  let successCount = 0;
  let errorCount = 0;
  
  // Group jobs by company for better logging
  const jobsByCompany = {};
  jobs.forEach(job => {
    if (!jobsByCompany[job.companyName]) {
      jobsByCompany[job.companyName] = 0;
    }
    jobsByCompany[job.companyName]++;
  });
  
  console.log('📊 Jobs by Company:');
  Object.entries(jobsByCompany).forEach(([company, count]) => {
    console.log(`   ${company}: ${count} jobs`);
  });
  console.log('');
  
  // Calculate vacancy statistics
  const totalVacancies = jobs.reduce((sum, job) => sum + job.vacancies, 0);
  const avgVacancies = Math.floor(totalVacancies / jobs.length);
  console.log(`📈 Total Vacancies: ${totalVacancies} (avg: ${avgVacancies} per job)\n`);
  
  console.log('📍 Uploading to Firestore...\n');
  
  for (const job of jobs) {
    try {
      await db.collection('jobs').doc(job.id).set(job);
      console.log(`✅ ${job.companyName} - ${job.title} (${job.location}) - ${job.vacancies} vacancies - ${job.contactNumber}`);
      successCount++;
    } catch (error) {
      console.error(`❌ Error creating job: ${error.message}`);
      errorCount++;
    }
  }
  
  console.log('\n🎉 Deployment complete!');
  console.log(`✅ Successfully created: ${successCount} jobs`);
  if (errorCount > 0) {
    console.log(`❌ Failed: ${errorCount} jobs`);
  }
  
  console.log(`\n📱 Using ${phoneNumbers.length} random phone numbers`);
  console.log(`📍 Covering ${hyderabadLocations.length} locations in Hyderabad`);
  console.log(`💰 Salary range: ₹17,000 - ₹33,000/month`);
  console.log(`👥 Vacancy range: 20-100 per job`);
}

// Run deployment
deployJobs()
  .then(() => {
    console.log('\n✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
