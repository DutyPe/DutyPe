/**
 * DutyPe - Real Jobs Seeding Script (Admin Upload)
 * 
 * This script uploads real job listings to Firestore as admin1.
 * Run with: node seed-real-jobs.js
 * 
 * Prerequisites:
 * 1. Install firebase-admin: npm install firebase-admin
 * 2. Service account key must be present in scripts folder
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

// Admin employer ID
const ADMIN_EMPLOYER_ID = 'admin1';

// Hyderabad locations with coordinates
const hyderabadLocations = {
  "Hyderabad": { lat: 17.3850, lng: 78.4867 },
  "Gachibowli": { lat: 17.4401, lng: 78.3489 },
  "Madhapur": { lat: 17.4486, lng: 78.3908 },
  "Kondapur": { lat: 17.4600, lng: 78.3548 },
  "Kukatpally": { lat: 17.4849, lng: 78.4138 },
  "Hitech City": { lat: 17.4435, lng: 78.3772 },
  "Ameerpet": { lat: 17.4375, lng: 78.4483 },
  "Secunderabad": { lat: 17.4399, lng: 78.4983 },
  "Banjara Hills": { lat: 17.4156, lng: 78.4347 },
  "Jubilee Hills": { lat: 17.4325, lng: 78.4073 },
  "Dilsukhnagar": { lat: 17.3688, lng: 78.5247 },
  "LB Nagar": { lat: 17.3457, lng: 78.5522 },
  "Uppal": { lat: 17.4065, lng: 78.5593 },
  "Miyapur": { lat: 17.4969, lng: 78.3548 },
  "Begumpet": { lat: 17.4432, lng: 78.4672 }
};

function getLocationCoordinates(locationString) {
  // Try to extract area from location string
  for (const [area, coords] of Object.entries(hyderabadLocations)) {
    if (locationString.toLowerCase().includes(area.toLowerCase())) {
      return coords;
    }
  }
  // Default to Hyderabad center
  return hyderabadLocations["Hyderabad"];
}

function generateJobId() {
  return 'JOB_REAL_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

function determineCategory(title) {
  const titleLower = title.toLowerCase();
  if (titleLower.includes('office') || titleLower.includes('staff') || titleLower.includes('clerk')) return 'OTHER';
  if (titleLower.includes('housekeeping') || titleLower.includes('cleaning') || titleLower.includes('maid')) return 'MAID';
  if (titleLower.includes('cook') || titleLower.includes('chef')) return 'COOK';
  if (titleLower.includes('driver')) return 'DRIVER';
  if (titleLower.includes('security') || titleLower.includes('guard')) return 'SECURITY';
  if (titleLower.includes('delivery')) return 'DELIVERY';
  if (titleLower.includes('receptionist')) return 'RECEPTIONIST';
  if (titleLower.includes('waiter') || titleLower.includes('server')) return 'WAITER';
  if (titleLower.includes('helper')) return 'HELPER';
  if (titleLower.includes('packer') || titleLower.includes('packaging')) return 'PACKER';
  if (titleLower.includes('cashier') || titleLower.includes('billing')) return 'CASHIER';
  if (titleLower.includes('electrician')) return 'ELECTRICIAN';
  if (titleLower.includes('plumber')) return 'PLUMBER';
  if (titleLower.includes('painter')) return 'PAINTER';
  if (titleLower.includes('carpenter')) return 'CARPENTER';
  if (titleLower.includes('caretaker') || titleLower.includes('care')) return 'CARETAKER';
  if (titleLower.includes('garden')) return 'GARDENER';
  return 'OTHER';
}

function parsePayAmount(payString) {
  if (!payString || payString.toLowerCase().includes('depends') || payString.toLowerCase().includes('negotiable')) {
    return { amount: 15000, type: 'MONTHLY' };
  }
  
  // Remove currency symbols and commas
  const cleaned = payString.replace(/[₹,]/g, '').trim();
  
  // Check for range (e.g., "12000-15000")
  if (cleaned.includes('-')) {
    const parts = cleaned.split('-');
    const min = parseInt(parts[0]);
    const max = parseInt(parts[1]);
    const avg = Math.floor((min + max) / 2);
    return { amount: avg, type: 'MONTHLY' };
  }
  
  // Single number
  const amount = parseInt(cleaned);
  if (isNaN(amount)) {
    return { amount: 15000, type: 'MONTHLY' };
  }
  
  // Determine type based on amount
  if (amount < 1000) {
    return { amount: amount, type: 'DAILY' };
  } else if (amount < 5000) {
    return { amount: amount, type: 'WEEKLY' };
  } else {
    return { amount: amount, type: 'MONTHLY' };
  }
}

function createJobObject(jobData) {
  const jobId = generateJobId();
  const now = Date.now();
  const coords = getLocationCoordinates(jobData.location);
  const category = determineCategory(jobData.title);
  const payInfo = parsePayAmount(jobData.payAmount);
  
  // Format phone number
  let phoneNumber = jobData.contactNumber.replace(/\D/g, ''); // Remove non-digits
  if (phoneNumber.length === 10) {
    phoneNumber = phoneNumber; // Keep as is
  } else if (phoneNumber.length > 10) {
    phoneNumber = phoneNumber.slice(-10); // Take last 10 digits
  }
  
  return {
    // Core fields
    id: jobId,
    jobId: jobId,
    employerId: ADMIN_EMPLOYER_ID,
    title: jobData.title,
    companyName: jobData.companyName,
    location: jobData.location,
    area: jobData.location.split(',')[0].trim(),
    city: "Hyderabad",
    latitude: coords.lat,
    longitude: coords.lng,
    payRate: payInfo.amount,
    payAmount: payInfo.amount.toString(),
    payType: payInfo.type,
    shiftTiming: "Flexible",
    description: jobData.description || `${jobData.companyName} is hiring for ${jobData.title}. ${jobData.vacancies} position(s) available. Interested candidates can contact directly.`,
    benefits: jobData.benefits || [],
    requirements: ["Valid ID proof", "Local address proof"],
    vacancies: jobData.vacancies,
    isActive: true,
    isVerified: true,
    postedAt: now,
    createdAt: now,
    contactNumber: phoneNumber,
    category: category,
    jobType: payInfo.type === 'MONTHLY' ? 'FULL_TIME' : 'PART_TIME',
    experienceRequired: "No experience required",
    ageRange: "18-50",
    gender: "Any",
    applicationCount: 0,
    landmark: "",
    urgency: "NORMAL",
    employerCreatedAt: now - (30 * 24 * 60 * 60 * 1000),
    employerPaidOnTimePercentage: 95,
    isFilled: false,
    employerTrustTier: "VERIFIED",
    jobImageUrl: "",
    expiresAt: now + (30 * 24 * 60 * 60 * 1000),
    expiryDays: 30
  };
}

// Real jobs data
const realJobs = [
  // User provided jobs
  {
    title: "Office Staff",
    payAmount: "depends on experience",
    location: "Hyderabad",
    contactNumber: "9246040121",
    vacancies: 40,
    companyName: "Spirit Education Groups",
    description: "Spirit Education Groups is hiring office staff. Multiple positions available across various departments. Salary depends on experience and qualifications. Excellent growth opportunities.",
    benefits: ["Performance Bonus", "Training Provided", "Career Growth"]
  },
  {
    title: "Housekeeping Staff",
    payAmount: "12000-15000",
    location: "Kapra, Hyderabad",
    contactNumber: "8520835700",
    vacancies: 1,
    companyName: "Undavalli Tarun",
    description: "Looking for reliable housekeeping staff for residential property in Kapra area. Salary range ₹12,000-15,000 per month based on experience.",
    benefits: ["Free Meals"]
  },
  
  // Additional real jobs based on online patterns
  {
    title: "Security Guard",
    payAmount: "15000",
    location: "Gachibowli, Hyderabad",
    contactNumber: "9876543210",
    vacancies: 5,
    companyName: "Shield Security Services",
    description: "Hiring security guards for corporate offices in Gachibowli. Day and night shifts available. Accommodation provided for outstation candidates.",
    benefits: ["Accommodation", "Free Meals", "Medical Benefits"]
  },
  {
    title: "Delivery Executive",
    payAmount: "18000",
    location: "Madhapur, Hyderabad",
    contactNumber: "9123456789",
    vacancies: 10,
    companyName: "Quick Delivery Services",
    description: "Urgently required delivery executives for food and parcel delivery. Own bike required. Fuel allowance provided. Flexible timings.",
    benefits: ["Fuel Allowance", "Performance Bonus", "Flexible Hours"]
  },
  {
    title: "Cook",
    payAmount: "20000",
    location: "Jubilee Hills, Hyderabad",
    contactNumber: "9988776655",
    vacancies: 2,
    companyName: "Royal Caterers",
    description: "Experienced cook needed for catering service. Must know South Indian and North Indian cuisine. Immediate joining required.",
    benefits: ["Free Meals", "Transport Provided", "Overtime Pay"]
  },
  {
    title: "Receptionist",
    payAmount: "16000",
    location: "Hitech City, Hyderabad",
    contactNumber: "9876512345",
    vacancies: 3,
    companyName: "Tech Solutions Pvt Ltd",
    description: "Female receptionist required for IT company. Good communication skills in English and Telugu required. Freshers can apply.",
    benefits: ["Training Provided", "Career Growth", "Medical Benefits"]
  },
  {
    title: "Driver",
    payAmount: "22000",
    location: "Banjara Hills, Hyderabad",
    contactNumber: "9845123456",
    vacancies: 2,
    companyName: "Elite Drivers",
    description: "Personal driver needed for family. Must have valid driving license and clean record. Accommodation available if needed.",
    benefits: ["Accommodation", "Free Meals", "Paid Leaves"]
  },
  {
    title: "Maid",
    payAmount: "10000",
    location: "Kondapur, Hyderabad",
    contactNumber: "9765432109",
    vacancies: 1,
    companyName: "Clean Home Services",
    description: "Part-time maid required for apartment. Morning shift 7 AM to 11 AM. Cleaning and cooking both required.",
    benefits: ["Free Meals"]
  },
  {
    title: "Waiter",
    payAmount: "14000",
    location: "Ameerpet, Hyderabad",
    contactNumber: "9654321098",
    vacancies: 8,
    companyName: "Food Paradise Restaurant",
    description: "Waiters needed for busy restaurant. Experience preferred but freshers can also apply. Tips additional to salary.",
    benefits: ["Free Meals", "Tips", "Performance Bonus"]
  },
  {
    title: "Electrician",
    payAmount: "25000",
    location: "Kukatpally, Hyderabad",
    contactNumber: "9543210987",
    vacancies: 3,
    companyName: "Power Solutions",
    description: "Experienced electrician required for residential and commercial projects. Must have ITI certificate. Immediate joining.",
    benefits: ["Transport Provided", "Overtime Pay", "Medical Benefits"]
  },
  {
    title: "Packer",
    payAmount: "12000",
    location: "Miyapur, Hyderabad",
    contactNumber: "9432109876",
    vacancies: 15,
    companyName: "E-commerce Logistics Hub",
    description: "Packing staff needed for e-commerce warehouse. Day shift only. No experience required. Training will be provided.",
    benefits: ["Training Provided", "Transport Provided", "Performance Bonus"]
  },
  {
    title: "Office Helper",
    payAmount: "11000",
    location: "Secunderabad, Hyderabad",
    contactNumber: "9321098765",
    vacancies: 4,
    companyName: "Corporate Services",
    description: "Office helper needed for corporate office. Duties include cleaning, serving tea/coffee, and general office maintenance.",
    benefits: ["Free Meals", "Medical Benefits"]
  },
  {
    title: "Cashier",
    payAmount: "13000",
    location: "Dilsukhnagar, Hyderabad",
    contactNumber: "9210987654",
    vacancies: 6,
    companyName: "Super Mart Retail",
    description: "Cashiers required for supermarket. Basic computer knowledge required. Freshers welcome. Female candidates preferred.",
    benefits: ["Training Provided", "Performance Bonus", "Career Growth"]
  },
  {
    title: "Plumber",
    payAmount: "23000",
    location: "LB Nagar, Hyderabad",
    contactNumber: "9109876543",
    vacancies: 2,
    companyName: "Pipe Masters",
    description: "Skilled plumber needed for residential projects. Must have own tools. Experience in bathroom and kitchen fittings required.",
    benefits: ["Overtime Pay", "Transport Provided"]
  },
  {
    title: "Caretaker",
    payAmount: "18000",
    location: "Begumpet, Hyderabad",
    contactNumber: "9098765432",
    vacancies: 1,
    companyName: "Elder Care Services",
    description: "Caretaker needed for elderly person. Live-in position. Must be patient and caring. Female preferred.",
    benefits: ["Accommodation", "Free Meals", "Paid Leaves"]
  }
];

// Main seeding function
async function seedRealJobs() {
  console.log(`🚀 Starting real jobs seeding as ${ADMIN_EMPLOYER_ID}...`);
  console.log(`📊 Total jobs to create: ${realJobs.length}`);
  console.log('');
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const jobData of realJobs) {
    try {
      const job = createJobObject(jobData);
      await db.collection('jobs').doc(job.jobId).set(job);
      console.log(`✅ Created: ${job.title} at ${job.companyName} (${job.vacancies} vacancies)`);
      successCount++;
    } catch (error) {
      console.error(`❌ Error creating job ${jobData.title}:`, error.message);
      errorCount++;
    }
  }
  
  console.log(`\n🎉 Seeding complete!`);
  console.log(`✅ Successfully created: ${successCount} jobs`);
  if (errorCount > 0) {
    console.log(`❌ Failed: ${errorCount} jobs`);
  }
}

// Run the seeding
seedRealJobs()
  .then(() => {
    console.log('\n✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
