/**
 * Fix Aligarh (Uttar Pradesh) Jobs
 * 
 * Deletes all Aligarh jobs and redeploys with correct pay amounts
 * 
 * Usage: node scripts/fix-aligarh-jobs.js
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

// Phone numbers for Aligarh
const phoneNumbers = [
  '6262366144',
  '9244124358',
  '6262360899',
  '6262358916',
  '6262364300',
  '9244124355',
  '6262363500',
  '6262359600',
  '6262362800',
  '6262363800',
  '6262361700'
];

// Delivery companies
const deliveryCompanies = [
  {
    name: 'Zomato',
    titles: [
      'Zomato Delivery Partner',
      'Zomato Food Delivery Executive',
      'Zomato Delivery Boy',
      'Zomato Rider',
      'Zomato Delivery Associate'
    ],
    payRange: { min: 15000, max: 25000 }
  },
  {
    name: 'Blinkit',
    titles: [
      'Blinkit Delivery Partner',
      'Blinkit Delivery Executive',
      'Blinkit Rider',
      'Blinkit Delivery Boy',
      'Blinkit Quick Commerce Delivery'
    ],
    payRange: { min: 18000, max: 28000 }
  },
  {
    name: 'BigBasket',
    titles: [
      'BigBasket Delivery Partner',
      'BigBasket Delivery Executive',
      'BigBasket Delivery Boy',
      'BigBasket Rider',
      'BigBasket Grocery Delivery'
    ],
    payRange: { min: 16000, max: 24000 }
  },
  {
    name: 'Swiggy',
    titles: [
      'Swiggy Delivery Partner',
      'Swiggy Delivery Executive',
      'Swiggy Delivery Boy',
      'Swiggy Rider',
      'Swiggy Instamart Delivery'
    ],
    payRange: { min: 15000, max: 26000 }
  },
  {
    name: 'Urban Company',
    titles: [
      'Urban Company Delivery Partner',
      'Urban Company Service Partner',
      'Urban Company Delivery Executive',
      'Urban Company Logistics Partner'
    ],
    payRange: { min: 17000, max: 27000 }
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
    payRange: { min: 16000, max: 25000 }
  },
  {
    name: 'Zepto',
    titles: [
      'Zepto Delivery Partner',
      'Zepto Quick Delivery Executive',
      'Zepto Rider',
      'Zepto Delivery Boy'
    ],
    payRange: { min: 18000, max: 29000 }
  },
  {
    name: 'Dunzo',
    titles: [
      'Dunzo Delivery Partner',
      'Dunzo Delivery Executive',
      'Dunzo Rider',
      'Dunzo Quick Delivery'
    ],
    payRange: { min: 15000, max: 24000 }
  }
];

// Aligarh locations
const aligarhLocations = [
  { area: 'Civil Lines', lat: 27.8974, lng: 78.0880 },
  { area: 'Ramghat Road', lat: 27.8820, lng: 78.0820 },
  { area: 'Marris Road', lat: 27.8900, lng: 78.0750 },
  { area: 'Sasni Gate', lat: 27.8850, lng: 78.0900 },
  { area: 'Delhi Gate', lat: 27.8800, lng: 78.0850 },
  { area: 'Quarsi', lat: 27.9100, lng: 78.0950 },
  { area: 'Dodhpur', lat: 27.8750, lng: 78.1000 },
  { area: 'Jamalpur', lat: 27.8650, lng: 78.0800 },
  { area: 'Centre Point', lat: 27.8920, lng: 78.0810 },
  { area: 'AMU Campus', lat: 27.8990, lng: 78.0730 },
  { area: 'Achal Tal', lat: 27.8880, lng: 78.0920 },
  { area: 'Badar Bagh', lat: 27.8780, lng: 78.0780 },
  { area: 'Nagla Masani', lat: 27.9050, lng: 78.0850 },
  { area: 'Jeevangarh', lat: 27.8950, lng: 78.0950 },
  { area: 'Sarai Bala', lat: 27.8700, lng: 78.0900 }
];

const jobDescriptions = [
  'Join our delivery team and earn attractive incentives! Flexible working hours, weekly payouts, and fuel allowance provided.',
  'Immediate joining for delivery partners. Own vehicle required. Earn up to ₹30,000/month with incentives and bonuses.',
  'Urgent requirement for delivery executives. Bike mandatory. Attractive salary + fuel + incentives. Apply now!',
  'Be your own boss! Flexible timings, daily earnings, and performance bonuses. Join India\'s fastest growing delivery platform.',
  'Delivery partners needed urgently! Own two-wheeler required. Weekly payouts, insurance coverage, and fuel reimbursement.',
  'Earn while you ride! Join our delivery team with attractive pay, flexible hours, and growth opportunities.',
  'Immediate openings for delivery boys. Smartphone and bike required. Earn ₹500-800 daily with incentives.',
  'Join the delivery revolution! Competitive pay, flexible schedules, and excellent support. Apply today!',
  'Delivery executives wanted! Own vehicle mandatory. Daily payouts, fuel allowance, and performance bonuses.',
  'Start earning today! Flexible part-time and full-time positions available. Own bike required.'
];

const requirements = [
  'Own two-wheeler (bike/scooter) mandatory\nValid driving license\nSmartphone with internet\nAge: 18-40 years\nGood knowledge of Aligarh city',
  'Two-wheeler with valid documents\nAndroid smartphone\nBasic English/Hindi communication\nWillingness to work flexible hours\nLocal resident preferred',
  'Own bike/scooter required\nValid DL and RC\nSmartphone mandatory\nGood communication skills\nReady to work in all weather conditions',
  'Vehicle: Bike/Scooter (mandatory)\nDocuments: DL, Aadhar, PAN\nAge: 18-45 years\nGood knowledge of local areas\nHardworking and punctual',
  'Two-wheeler ownership mandatory\nValid driving license\nSmartphone with GPS\nBasic Hindi/English\nReady for immediate joining'
];

const benefits = [
  'Weekly payouts\nFuel allowance\nPerformance incentives\nFlexible working hours\nInsurance coverage\nFree training provided',
  'Daily/Weekly earnings\nFuel reimbursement\nAttendance bonus\nReferral bonus\nAccident insurance\nCareer growth opportunities',
  'Attractive salary\nFuel allowance ₹2000-3000/month\nIncentive on deliveries\nFlexible timings\nMedical insurance\nFree uniform and bag',
  'Competitive pay\nWeekly payouts\nPerformance bonuses\nFuel support\nInsurance benefits\nTraining and support',
  'High earning potential\nDaily/Weekly payments\nFuel reimbursement\nIncentive structure\nAccident coverage\nGrowth opportunities'
];

async function deleteAligarhJobs() {
  console.log('🗑️  Deleting old Aligarh jobs...\n');
  
  try {
    const snapshot = await db.collection('jobs')
      .where('state', '==', 'Uttar Pradesh')
      .get();
    
    let deleteCount = 0;
    const batch = db.batch();
    
    snapshot.docs.forEach(doc => {
      batch.delete(doc.ref);
      deleteCount++;
      console.log(`🗑️  Deleting: ${doc.id} - ${doc.data().title}`);
    });
    
    if (deleteCount > 0) {
      await batch.commit();
      console.log(`\n✅ Deleted ${deleteCount} Aligarh jobs\n`);
    } else {
      console.log('ℹ️  No Aligarh jobs found to delete\n');
    }
    
    return deleteCount;
  } catch (error) {
    console.error('❌ Error deleting jobs:', error.message);
    throw error;
  }
}

function generateJobs() {
  const jobs = [];
  let jobCounter = 0;

  phoneNumbers.forEach((phone) => {
    const jobsPerNumber = 10 + Math.floor(Math.random() * 6); // 10-15 jobs

    for (let i = 0; i < jobsPerNumber; i++) {
      const company = deliveryCompanies[jobCounter % deliveryCompanies.length];
      const location = aligarhLocations[jobCounter % aligarhLocations.length];
      const title = company.titles[Math.floor(Math.random() * company.titles.length)];
      
      const payAmount = Math.floor(
        company.payRange.min + Math.random() * (company.payRange.max - company.payRange.min)
      );
      
      const vacancies = 30 + Math.floor(Math.random() * 41);

      const job = {
        id: `aligarh_delivery_${Date.now()}_${jobCounter}_${Math.random().toString(36).substr(2, 9)}`,
        title: title,
        companyName: company.name,
        description: jobDescriptions[Math.floor(Math.random() * jobDescriptions.length)],
        requirements: requirements[Math.floor(Math.random() * requirements.length)],
        benefits: benefits[Math.floor(Math.random() * benefits.length)],
        
        location: `${location.area}, Aligarh, Uttar Pradesh`,
        city: 'Aligarh',
        state: 'Uttar Pradesh',
        latitude: location.lat,
        longitude: location.lng,
        
        payAmount: payAmount.toString(), // STRING - CRITICAL!
        payType: 'MONTHLY',
        
        contactNumber: phone,
        whatsappNumber: phone,
        
        jobType: 'FULL_TIME',
        workingHours: '8-10 hours',
        vacancies: vacancies,
        
        experienceRequired: 'FRESHER',
        educationRequired: '10th Pass',
        
        isActive: true,
        isFilled: false,
        isVerified: true,
        
        postedAt: admin.firestore.FieldValue.serverTimestamp(),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt: admin.firestore.Timestamp.fromDate(
          new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
        ),
        
        employerId: 'system_aligarh_delivery',
        employerName: company.name,
        employerRating: 4.0 + Math.random() * 0.9,
        
        tags: ['delivery', 'bike', 'immediate-joining', 'flexible-hours'],
        applicationCount: 0,
        viewCount: 0,
        shareCount: 0
      };

      jobs.push(job);
      jobCounter++;
    }
  });

  return jobs;
}

async function deployJobs(jobs) {
  console.log('📦 Uploading new Aligarh jobs with correct pay amounts...\n');
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const job of jobs) {
    try {
      await db.collection('jobs').doc(job.id).set(job);
      console.log(`✅ ${job.companyName} - ${job.title} - ₹${job.payAmount}/month - ${job.vacancies} vacancies - ${job.contactNumber}`);
      successCount++;
    } catch (error) {
      console.error(`❌ Error: ${error.message}`);
      errorCount++;
    }
  }
  
  console.log(`\n🎉 Deployment complete!`);
  console.log(`✅ Successfully created: ${successCount} jobs`);
  if (errorCount > 0) {
    console.log(`❌ Failed: ${errorCount} jobs`);
  }
  
  return successCount;
}

async function main() {
  console.log('🔄 Fixing Aligarh (Uttar Pradesh) jobs...\n');
  
  try {
    // Step 1: Delete old jobs
    await deleteAligarhJobs();
    
    console.log('⏳ Waiting 2 seconds...\n');
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    // Step 2: Generate new jobs
    console.log('🚀 Generating new jobs...\n');
    const jobs = generateJobs();
    console.log(`📦 Generated ${jobs.length} jobs\n`);
    
    // Step 3: Deploy
    await deployJobs(jobs);
    
    console.log('\n✅ Process completed!');
    console.log('\n📱 Open your app and pull-to-refresh to see Aligarh jobs with correct pay amounts!');
    console.log(`💰 Pay range: ₹15,000 - ₹29,000/month`);
    console.log(`📍 ${aligarhLocations.length} locations in Aligarh`);
    console.log(`📱 ${phoneNumbers.length} phone numbers`);
    
    process.exit(0);
  } catch (error) {
    console.error('\n❌ Process failed:', error);
    process.exit(1);
  }
}

main();
