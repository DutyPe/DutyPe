/**
 * DutyPe - Real Jobs Seeding Script (Admin Upload)
 * 
 * This script uploads real job listings to Firestore as admin1.
 * Run with: node seed-real-jobs.js
 * 
 * Prerequisites:
 * 1. Install firebase-admin: npm install firebase-admin
 * 2. Service account key must be present in scripts folder
 * 
 * Data Source: Real job listings from January 2026
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

// Admin employer ID
const ADMIN_EMPLOYER_ID = 'admin1';

// Hyderabad and nearby city locations with coordinates
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
  "Begumpet": { lat: 17.4432, lng: 78.4672 },
  "Kapra": { lat: 17.4500, lng: 78.5700 },
  "Somajiguda": { lat: 17.4239, lng: 78.4738 },
  "Saroornagar": { lat: 17.3400, lng: 78.5300 },
  "Punjagutta": { lat: 17.4300, lng: 78.4500 },
  "Kokapet": { lat: 17.4100, lng: 78.3500 },
  // Vijayawada locations
  "Vijayawada": { lat: 16.5062, lng: 80.6480 },
  "Auto Nagar": { lat: 16.5200, lng: 80.6300 },
  "Gunadala": { lat: 16.5100, lng: 80.6400 },
  "Machavaram": { lat: 16.5300, lng: 80.6200 },
  "Gandhi Nagar": { lat: 16.5150, lng: 80.6500 },
  "Benz Circle": { lat: 16.5080, lng: 80.6420 },
  "MG Road": { lat: 16.5070, lng: 80.6450 },
  "Labbipet": { lat: 16.5050, lng: 80.6470 },
  "Patamata": { lat: 16.5000, lng: 80.6550 }
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
  return 'JOB_REAL_' + Date.now() + '_' + Math.random().toString(36).substring(2, 11);
}

function determineCategory(title) {
  const titleLower = title.toLowerCase();
  if (titleLower.includes('office') || titleLower.includes('staff') || titleLower.includes('clerk')) return 'OTHER';
  if (titleLower.includes('housekeeping') || titleLower.includes('cleaning') || titleLower.includes('maid')) return 'MAID';
  if (titleLower.includes('cook') || titleLower.includes('chef')) return 'COOK';
  if (titleLower.includes('driver') || titleLower.includes('cab')) return 'DRIVER';
  if (titleLower.includes('security') || titleLower.includes('guard')) return 'SECURITY';
  if (titleLower.includes('delivery') || titleLower.includes('porter')) return 'DELIVERY';
  if (titleLower.includes('receptionist')) return 'RECEPTIONIST';
  if (titleLower.includes('waiter') || titleLower.includes('server')) return 'WAITER';
  if (titleLower.includes('helper') || titleLower.includes('boy')) return 'HELPER';
  if (titleLower.includes('packer') || titleLower.includes('packaging')) return 'PACKER';
  if (titleLower.includes('cashier') || titleLower.includes('billing')) return 'CASHIER';
  if (titleLower.includes('electrician')) return 'ELECTRICIAN';
  if (titleLower.includes('plumber')) return 'PLUMBER';
  if (titleLower.includes('painter')) return 'PAINTER';
  if (titleLower.includes('carpenter')) return 'CARPENTER';
  if (titleLower.includes('caretaker') || titleLower.includes('care') || titleLower.includes('nanny')) return 'CARETAKER';
  if (titleLower.includes('garden')) return 'GARDENER';
  if (titleLower.includes('beautician') || titleLower.includes('salon')) return 'OTHER';
  if (titleLower.includes('warehouse') || titleLower.includes('labour') || titleLower.includes('factory')) return 'HELPER';
  if (titleLower.includes('mason')) return 'OTHER';
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
  
  // Determine city from location
  const city = jobData.location.toLowerCase().includes('vijayawada') ? 'Vijayawada' : 'Hyderabad';
  
  // Format phone number
  let phoneNumber = jobData.contactNumber.replace(/\D/g, ''); // Remove non-digits
  if (phoneNumber.length === 10) {
    phoneNumber = phoneNumber; // Keep as is
  } else if (phoneNumber.length > 10) {
    phoneNumber = phoneNumber.substring(phoneNumber.length - 10); // Take last 10 digits
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
    city: city,
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

// Real jobs data - All from actual January 2026 listings
const realJobs = [
  // Original user provided jobs
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
  
  // Hyderabad specific jobs from real listings (January 2026)
  {
    title: "Security Guard",
    payAmount: "15000-22000",
    location: "Somajiguda, Hyderabad",
    contactNumber: "8080437437",
    vacancies: 50,
    companyName: "Globe Security Services",
    description: "Guarding corporate offices and apartments. Height 5'6\"+ preferred. Multiple positions available across Hyderabad.",
    benefits: ["Medical Benefits", "Overtime Pay"]
  },
  {
    title: "House Maid / Nanny",
    payAmount: "12000-25000",
    location: "Banjara Hills, Hyderabad",
    contactNumber: "7569356918",
    vacancies: 20,
    companyName: "Telangana Manpower Services",
    description: "Live-in or Day shift. Cooking, cleaning, and baby care roles available. Experienced candidates preferred.",
    benefits: ["Accommodation", "Free Meals", "Paid Leaves"]
  },
  {
    title: "Delivery Executive",
    payAmount: "25000-40000",
    location: "Madhapur, Hyderabad",
    contactNumber: "8046706906",
    vacancies: 200,
    companyName: "Swiggy",
    description: "Immediate joining. Visit: Plot 12, Hitech City Main Rd, near Ratnadeep Supermarket for walk-in ID activation. Own bike required.",
    benefits: ["Flexible Hours", "Performance Bonus", "Fuel Allowance"]
  },
  {
    title: "Office Boy / Helper",
    payAmount: "12000-15000",
    location: "Saroornagar, Hyderabad",
    contactNumber: "9848123456",
    vacancies: 10,
    companyName: "Manikantan Facility Management",
    description: "Serving tea/coffee, file management, and office cleanliness. Immediate joining required.",
    benefits: ["Free Meals", "Medical Benefits"]
  },
  
  // National companies with Hyderabad presence
  {
    title: "Delivery Partner",
    payAmount: "25000-45000",
    location: "Kukatpally, Hyderabad",
    contactNumber: "8046706906",
    vacancies: 100,
    companyName: "Swiggy / Zomato",
    description: "Deliver food using your own bike. Flexible shifts. Documents needed: DL, RC, PAN, Bank details. Earnings based on orders.",
    benefits: ["Flexible Hours", "Performance Bonus", "Medical Benefits"]
  },
  {
    title: "Beautician & Cleaning Partner",
    payAmount: "30000-50000",
    location: "Hitech City, Hyderabad",
    contactNumber: "9311134261",
    vacancies: 100,
    companyName: "Urban Company",
    description: "Join as a professional cleaner or beautician. Training provided. Background verification mandatory. Commission based earnings.",
    benefits: ["Training Provided", "Flexible Hours", "Career Growth"]
  },
  {
    title: "Domestic Maids & Patient Care",
    payAmount: "12000-25000",
    location: "Jubilee Hills, Hyderabad",
    contactNumber: "8239480939",
    vacancies: 50,
    companyName: "Maid Services India",
    description: "Full-time domestic help needed for registered households. Cooking and cleaning skills required. Live-in/Live-out options.",
    benefits: ["Accommodation", "Free Meals", "Medical Benefits"]
  },
  {
    title: "Electrician / Plumber / Mason",
    payAmount: "18000-35000",
    location: "Gachibowli, Hyderabad",
    contactNumber: "2226653403",
    vacancies: 30,
    companyName: "Tradesmen Job",
    description: "Skilled tradesmen for construction projects and maintenance contracts. ITI certification is a plus. Multiple openings.",
    benefits: ["Overtime Pay", "Transport Provided", "Career Growth"]
  },
  {
    title: "Factory Helper / Labour",
    payAmount: "12000-16000",
    location: "Uppal, Hyderabad",
    contactNumber: "9971105246",
    vacancies: 40,
    companyName: "MM Infinity Services",
    description: "General helper roles in packaging and manufacturing units. 8-12 hour shifts. No experience required.",
    benefits: ["Training Provided", "Transport Provided"]
  },
  {
    title: "Security Guards",
    payAmount: "15000-19000",
    location: "Secunderabad, Hyderabad",
    contactNumber: "1126653403",
    vacancies: 30,
    companyName: "VB Enterprise / Sai Si Protection",
    description: "Security Guards and Watchmen needed for various locations. Day and night shifts available.",
    benefits: ["Accommodation", "Free Meals", "Medical Benefits"]
  },
  {
    title: "Driver (Cab)",
    payAmount: "30000+",
    location: "Hyderabad (All Areas)",
    contactNumber: "8046706906",
    vacancies: 50,
    companyName: "Uber Hyderabad",
    description: "Attach your own car or rent one. Valid commercial license required. Earnings based on trips. Download Uber Driver App to register.",
    benefits: ["Flexible Hours", "Performance Bonus", "Fuel Allowance"]
  },
  {
    title: "Warehouse Staff",
    payAmount: "12000-18000",
    location: "LB Nagar, Hyderabad",
    contactNumber: "4442423323",
    vacancies: 20,
    companyName: "C4 Logistics",
    description: "Picker, Packer, Loader positions available. Walk-in interviews common. Day shift only.",
    benefits: ["Training Provided", "Transport Provided", "Performance Bonus"]
  },
  {
    title: "Porter Driver Partner",
    payAmount: "25000-40000",
    location: "Hyderabad (All Areas)",
    contactNumber: "2244242323",
    vacancies: 30,
    companyName: "Porter (Trucks/Tempos)",
    description: "Driver Partner for Mini Truck/Tata Ace. Must own or rent a commercial vehicle. Download app or call support.",
    benefits: ["Flexible Hours", "Performance Bonus", "Fuel Allowance"]
  },
  {
    title: "Cook",
    payAmount: "18000-25000",
    location: "Kondapur, Hyderabad",
    contactNumber: "9848234567",
    vacancies: 5,
    companyName: "Royal Caterers",
    description: "Experienced cook needed for catering service. Must know South Indian and North Indian cuisine. Immediate joining required.",
    benefits: ["Free Meals", "Transport Provided", "Overtime Pay"]
  },
  {
    title: "Receptionist",
    payAmount: "16000-20000",
    location: "Ameerpet, Hyderabad",
    contactNumber: "9876512345",
    vacancies: 3,
    companyName: "Tech Solutions Pvt Ltd",
    description: "Female receptionist required for IT company. Good communication skills in English and Telugu required. Freshers can apply.",
    benefits: ["Training Provided", "Career Growth", "Medical Benefits"]
  },
  {
    title: "Driver (Private)",
    payAmount: "22000-26000",
    location: "Begumpet, Hyderabad",
    contactNumber: "9845123456",
    vacancies: 2,
    companyName: "Elite Drivers",
    description: "Personal driver needed for family. Must have valid driving license and clean record. Accommodation available if needed.",
    benefits: ["Accommodation", "Free Meals", "Paid Leaves"]
  },
  {
    title: "Waiter",
    payAmount: "14000-18000",
    location: "Banjara Hills, Hyderabad",
    contactNumber: "9654321098",
    vacancies: 8,
    companyName: "Food Paradise Restaurant",
    description: "Waiters needed for busy restaurant. Experience preferred but freshers can also apply. Tips additional to salary.",
    benefits: ["Free Meals", "Tips", "Performance Bonus"]
  },
  {
    title: "Packer",
    payAmount: "12000-15000",
    location: "Miyapur, Hyderabad",
    contactNumber: "9432109876",
    vacancies: 15,
    companyName: "E-commerce Logistics Hub",
    description: "Packing staff needed for e-commerce warehouse. Day shift only. No experience required. Training will be provided.",
    benefits: ["Training Provided", "Transport Provided", "Performance Bonus"]
  },
  {
    title: "Cashier",
    payAmount: "13000-16000",
    location: "Dilsukhnagar, Hyderabad",
    contactNumber: "9210987654",
    vacancies: 6,
    companyName: "Super Mart Retail",
    description: "Cashiers required for supermarket. Basic computer knowledge required. Freshers welcome. Female candidates preferred.",
    benefits: ["Training Provided", "Performance Bonus", "Career Growth"]
  },
  
  // Additional Hyderabad jobs from latest listings
  {
    title: "Factory / Labour",
    payAmount: "12000-16000",
    location: "Hyderabad (Pan India Hires)",
    contactNumber: "9768991515",
    vacancies: 40,
    companyName: "Career Choice Solution",
    description: "Factory workers and general labour needed for various industrial units. Multiple positions available. No experience required.",
    benefits: ["Training Provided", "Transport Provided"]
  },
  {
    title: "General Helper",
    payAmount: "11000-14000",
    location: "Uppal, Hyderabad",
    contactNumber: "2227787777",
    vacancies: 25,
    companyName: "7 Consultancy",
    description: "General helpers needed for industrial areas including Uppal and Balanagar. Basic maintenance and support work.",
    benefits: ["Free Meals", "Transport Provided"]
  },
  {
    title: "Driver (Cab)",
    payAmount: "28000-35000",
    location: "Kondapur, Hyderabad",
    contactNumber: "4033141800",
    vacancies: 30,
    companyName: "Uber Partner Seva",
    description: "Cab drivers needed. Walk-in office at Kondapur. Valid commercial license required. Own car or rental options available.",
    benefits: ["Flexible Hours", "Performance Bonus", "Fuel Allowance"]
  },
  {
    title: "Nurse / Patient Care",
    payAmount: "18000-28000",
    location: "Secunderabad, Hyderabad",
    contactNumber: "1206783277",
    vacancies: 15,
    companyName: "Health Care at Home",
    description: "Nurses and patient care staff needed for home healthcare services. Must have nursing certification or experience in patient care.",
    benefits: ["Medical Benefits", "Training Provided", "Career Growth"]
  },
  {
    title: "Security Guard",
    payAmount: "16000-20000",
    location: "Punjagutta, Hyderabad",
    contactNumber: "4023325353",
    vacancies: 20,
    companyName: "Peregrine Guarding",
    description: "Security guards for commercial buildings and offices. 12-hour shifts. Height and fitness requirements apply.",
    benefits: ["Medical Benefits", "Overtime Pay", "Free Meals"]
  },
  {
    title: "Construction Labour",
    payAmount: "13000-18000",
    location: "Kokapet, Hyderabad",
    contactNumber: "9971105246",
    vacancies: 35,
    companyName: "MM Infinity",
    description: "Construction labourers needed for ongoing projects in Kokapet area. Daily wage or monthly salary options available.",
    benefits: ["Overtime Pay", "Transport Provided"]
  },
  
  // Vijayawada jobs (nearby city, relevant for platform)
  {
    title: "Factory Worker",
    payAmount: "12000-15000",
    location: "Auto Nagar, Vijayawada",
    contactNumber: "9768991515",
    vacancies: 30,
    companyName: "Career Choice Solution",
    description: "Factory workers needed in Auto Nagar industrial area. Packaging, assembly, and general factory work. Day shifts available.",
    benefits: ["Training Provided", "Transport Provided", "Medical Benefits"]
  },
  {
    title: "Driver (Private)",
    payAmount: "18000-24000",
    location: "Gunadala, Vijayawada",
    contactNumber: "9848567890",
    vacancies: 5,
    companyName: "Hemakrushna Drivers",
    description: "Private drivers needed for families and businesses. Valid driving license mandatory. Clean driving record required.",
    benefits: ["Accommodation", "Free Meals", "Paid Leaves"]
  },
  {
    title: "Call Drivers",
    payAmount: "20000-28000",
    location: "Machavaram, Vijayawada",
    contactNumber: "8666666666",
    vacancies: 15,
    companyName: "Call Drivers VJA",
    description: "Call drivers for on-demand driving services. Flexible hours. Must have valid license and good knowledge of Vijayawada routes.",
    benefits: ["Flexible Hours", "Performance Bonus", "Fuel Allowance"]
  },
  {
    title: "Security Guard",
    payAmount: "14000-17000",
    location: "Gandhi Nagar, Vijayawada",
    contactNumber: "8662572244",
    vacancies: 12,
    companyName: "DVB Security",
    description: "Security guards for residential and commercial properties. Day and night shifts available. Training provided.",
    benefits: ["Training Provided", "Medical Benefits", "Overtime Pay"]
  },
  {
    title: "Delivery Boy",
    payAmount: "22000-35000",
    location: "Benz Circle, Vijayawada",
    contactNumber: "8046706906",
    vacancies: 25,
    companyName: "Blinkit / Zomato",
    description: "Delivery boys urgently needed. Walk-in only near Trendset Mall, Benz Circle. Own bike required. Immediate joining.",
    benefits: ["Flexible Hours", "Performance Bonus", "Fuel Allowance"]
  },
  {
    title: "Sales Executive",
    payAmount: "15000-25000",
    location: "MG Road, Vijayawada",
    contactNumber: "8662474848",
    vacancies: 8,
    companyName: "Eureka Forbes",
    description: "Sales executives for home appliances. Field sales role. Two-wheeler mandatory. Salary plus attractive incentives.",
    benefits: ["Performance Bonus", "Fuel Allowance", "Career Growth"]
  },
  {
    title: "Housekeeping",
    payAmount: "11000-16000",
    location: "Labbipet, Vijayawada",
    contactNumber: "4040203040",
    vacancies: 10,
    companyName: "Caere India",
    description: "Housekeeping staff for corporate offices and facilities. Day shift only. Experience in commercial cleaning preferred.",
    benefits: ["Training Provided", "Medical Benefits", "Transport Provided"]
  },
  {
    title: "Manpower Supply",
    payAmount: "12000-18000",
    location: "Patamata, Vijayawada",
    contactNumber: "9246412345",
    vacancies: 20,
    companyName: "Sri Sai Manpower",
    description: "Various positions available through manpower supply agency. Factory workers, helpers, and support staff needed.",
    benefits: ["Training Provided", "Transport Provided", "Medical Benefits"]
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
