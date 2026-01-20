/**
 * DutyPe - Real Jobs Seeding Script
 * 
 * This script uploads 20 REAL jobs from other platforms to Firestore.
 * These jobs have real phone numbers and company names.
 * 
 * Run with: node seed-real-jobs.js
 * 
 * Prerequisites:
 * 1. Install firebase-admin: npm install firebase-admin
 * 2. Service account key should already exist from previous seeding
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
  console.error('Please download it from Firebase Console and place in scripts folder');
  process.exit(1);
}

// Initialize Firebase Admin
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutypeapp'
});

const db = admin.firestore();

// Hyderabad location coordinates (approximate)
const hyderabadLocations = {
  "bownepally": { lat: 17.4833, lng: 78.5000, city: "Hyderabad", area: "Bownepally" },
  "madapur": { lat: 17.4486, lng: 78.3908, city: "Hyderabad", area: "Madhapur" },
  "gachibowli": { lat: 17.4401, lng: 78.3489, city: "Hyderabad", area: "Gachibowli" },
  "uppal": { lat: 17.4065, lng: 78.5593, city: "Hyderabad", area: "Uppal" },
  "uppal kalan": { lat: 17.4065, lng: 78.5593, city: "Hyderabad", area: "Uppal Kalan" },
  "somajiguda": { lat: 17.4239, lng: 78.4538, city: "Hyderabad", area: "Somajiguda" },
  "panjagutta": { lat: 17.4260, lng: 78.4506, city: "Hyderabad", area: "Panjagutta" },
  "hyderabad": { lat: 17.3850, lng: 78.4867, city: "Hyderabad", area: "Hyderabad" },
  "shivam road": { lat: 17.4400, lng: 78.4500, city: "Hyderabad", area: "Shivam Road" },
  "aghapura": { lat: 17.3700, lng: 78.4800, city: "Hyderabad", area: "Aghapura" },
  "dilsukhnagar": { lat: 17.3688, lng: 78.5247, city: "Hyderabad", area: "Dilsukhnagar" },
  "lb nagar": { lat: 17.3457, lng: 78.5522, city: "Hyderabad", area: "LB Nagar" },
  "guntur": { lat: 16.3067, lng: 80.4365, city: "Guntur", area: "Guntur" }
};

// Helper function to get location data
function getLocationData(locationStr) {
  const normalized = locationStr.toLowerCase().trim();
  
  // Try exact match first
  if (hyderabadLocations[normalized]) {
    return hyderabadLocations[normalized];
  }
  
  // Try partial match
  for (const [key, value] of Object.entries(hyderabadLocations)) {
    if (normalized.includes(key) || key.includes(normalized)) {
      return value;
    }
  }
  
  // Default to Hyderabad center
  return hyderabadLocations["hyderabad"];
}

// Helper function to map job title to category
function getCategoryFromTitle(title) {
  const titleLower = title.toLowerCase();
  
  if (titleLower.includes('clean') || titleLower.includes('housekeeping')) return 'MAID';
  if (titleLower.includes('reception')) return 'RECEPTIONIST';
  if (titleLower.includes('manager') || titleLower.includes('assistant')) return 'OTHER';
  if (titleLower.includes('office')) return 'OTHER';
  if (titleLower.includes('xerox') || titleLower.includes('machine')) return 'OTHER';
  if (titleLower.includes('wash') || titleLower.includes('iron')) return 'MAID';
  if (titleLower.includes('hostel') || titleLower.includes('owner')) return 'OTHER';
  if (titleLower.includes('office boy')) return 'HELPER';
  if (titleLower.includes('care') || titleLower.includes('taker')) return 'CARETAKER';
  if (titleLower.includes('pack')) return 'PACKER';
  if (titleLower.includes('solar') || titleLower.includes('technician')) return 'ELECTRICIAN';
  if (titleLower.includes('cook')) return 'COOK';
  if (titleLower.includes('catering') || titleLower.includes('staff')) return 'WAITER';
  if (titleLower.includes('driver')) return 'DRIVER';
  if (titleLower.includes('pharmac')) return 'OTHER';
  if (titleLower.includes('nurs')) return 'CARETAKER';
  
  return 'OTHER';
}

// Helper function to generate smart description
function generateDescription(title, category, companyName) {
  const descriptions = {
    'MAID': `We are hiring ${title} for ${companyName}. Responsibilities include cleaning, maintaining hygiene standards, and ensuring a spotless environment. Candidates should be reliable, hardworking, and detail-oriented. Experience in housekeeping is preferred but freshers can also apply.`,
    'RECEPTIONIST': `${companyName} is looking for a professional ${title}. Handle front desk operations, guest management, phone calls, and administrative tasks. Good communication skills required. Computer knowledge is a plus.`,
    'COOK': `Experienced ${title} needed at ${companyName}. Must know various cuisines and maintain kitchen hygiene. Ability to prepare meals for large groups. Food safety knowledge required.`,
    'CARETAKER': `${companyName} requires compassionate ${title}. Provide care and assistance to patients/elderly. Monitor health, administer medicines, and maintain records. Prior experience in caregiving preferred.`,
    'DRIVER': `${companyName} needs reliable ${title}. Valid driving license mandatory. Knowledge of local routes, safe driving skills, and vehicle maintenance basics required. Clean driving record preferred.`,
    'PACKER': `Join ${companyName} as ${title}. Pack products carefully, label items, maintain quality standards. Physical stamina required. Training will be provided.`,
    'ELECTRICIAN': `${companyName} hiring skilled ${title}. Handle electrical installations, repairs, and maintenance. Knowledge of wiring, safety protocols mandatory. Experience with solar systems is a plus.`,
    'WAITER': `${companyName} looking for energetic ${title}. Serve customers, take orders, maintain cleanliness. Good communication and customer service skills required. Experience in hospitality preferred.`,
    'HELPER': `${companyName} needs hardworking ${title}. Assist with daily operations, loading/unloading, cleaning, and general support tasks. Willingness to learn and work in a team.`,
    'OTHER': `${companyName} is hiring for ${title} position. Candidate should be dedicated, punctual, and willing to learn. Good work ethic and communication skills required. Immediate joining preferred.`
  };
  
  return descriptions[category] || descriptions['OTHER'];
}

// Helper function to determine shift timing
function getShiftTiming(title) {
  const titleLower = title.toLowerCase();
  
  if (titleLower.includes('night')) return 'NIGHT';
  if (titleLower.includes('morning')) return 'MORNING';
  if (titleLower.includes('evening')) return 'EVENING';
  if (titleLower.includes('full') || titleLower.includes('day')) return 'FULL_DAY';
  
  // Default based on job type
  if (titleLower.includes('security') || titleLower.includes('guard')) return 'NIGHT';
  if (titleLower.includes('cook') || titleLower.includes('chef')) return 'FULL_DAY';
  if (titleLower.includes('clean') || titleLower.includes('maid')) return 'MORNING';
  if (titleLower.includes('office')) return 'FULL_DAY';
  
  return 'FLEXIBLE';
}

// Helper function to determine urgency
function getUrgency(vacancies) {
  if (vacancies >= 20) return 'IMMEDIATE';
  if (vacancies >= 10) return 'URGENT';
  if (vacancies >= 5) return 'URGENT';
  return 'NORMAL';
}

// Helper function to parse pay amount
function parsePayAmount(payStr) {
  // Remove all non-numeric characters except hyphen and plus
  const cleaned = payStr.replace(/[^0-9\-+]/g, '');
  
  // Extract first number
  const match = cleaned.match(/(\d+)/);
  if (match) {
    return match[1];
  }
  
  return '15000'; // Default
}

// Helper function to determine pay type
function getPayType(payStr, category) {
  const payNum = parseInt(parsePayAmount(payStr));
  
  // If pay is less than 2000, likely daily
  if (payNum < 2000) return 'DAILY';
  
  // If pay is between 2000-5000, could be daily or monthly (check category)
  if (payNum >= 2000 && payNum < 5000) {
    if (category === 'MAID' || category === 'HELPER') return 'DAILY';
    return 'MONTHLY';
  }
  
  // If pay is 5000+, likely monthly
  return 'MONTHLY';
}

// 20 REAL JOBS DATA
const realJobs = [
  {
    title: "House cleaner",
    payAmount: "13000-15000",
    location: "bownepally hyderabad",
    contactNumber: "8000062623",
    vacancies: 10,
    companyName: "cleanzy"
  },
  {
    title: "cleaner",
    payAmount: "25000+",
    location: "madapur hyderabad",
    contactNumber: "7893798348",
    vacancies: 40,
    companyName: "urban company"
  },
  {
    title: "house keeping staff",
    payAmount: "10000-13000",
    location: "gachibowli",
    contactNumber: "9542126633",
    vacancies: 1,
    companyName: "varshinin executive pg women hostel"
  },
  {
    title: "receptionist cum hotel supervisor",
    payAmount: "12000-13000",
    location: "Uppal kalan",
    contactNumber: "8978632828",
    vacancies: 6,
    companyName: "HOTEL SL9"
  },
  {
    title: "Assistant manager",
    payAmount: "25000-25000+",
    location: "somajiguda, hyderabad",
    contactNumber: "8370969696",
    vacancies: 5,
    companyName: "auctionbazaar.com"
  },
  {
    title: "cleaning",
    payAmount: "20000+",
    location: "panjagutta",
    contactNumber: "7893798348",
    vacancies: 10,
    companyName: "urban company"
  },
  {
    title: "office assistant",
    payAmount: "10000-15000",
    location: "hyderabad",
    contactNumber: "9399979608",
    vacancies: 2,
    companyName: "milan labels"
  },
  {
    title: "xerox machine operator",
    payAmount: "10000-10000",
    location: "hyderabad",
    contactNumber: "8686364646",
    vacancies: 2,
    companyName: "KLICK N BROWSE"
  },
  {
    title: "WASHING /IRONING",
    payAmount: "20000-25000",
    location: "guntur",
    contactNumber: "9515711541",
    vacancies: 4,
    companyName: "ragharitha drywash"
  },
  {
    title: "Hostel warden",
    payAmount: "8000-13500",
    location: "hyderabad",
    contactNumber: "8984007999",
    vacancies: 2,
    companyName: "agasthya hostels"
  },
  {
    title: "washing /ironing",
    payAmount: "15000-22000",
    location: "shivam road, hyderabad",
    contactNumber: "9000683274",
    vacancies: 2,
    companyName: "tumble dry solutions"
  },
  {
    title: "office boy",
    payAmount: "20000-25000",
    location: "aghapura hyderabad",
    contactNumber: "9110587467",
    vacancies: 2,
    companyName: "sheikh saheena"
  },
  {
    title: "care takers",
    payAmount: "10000-15000",
    location: "uppal hyderabad",
    contactNumber: "8008069707",
    vacancies: 40,
    companyName: "maanyatha old age home and geriatric center"
  },
  {
    title: "packing",
    payAmount: "25000+",
    location: "hyderabad",
    contactNumber: "9391857678",
    vacancies: 40,
    companyName: "yakshit facility services"
  },
  {
    title: "solar technician",
    payAmount: "10000-20000",
    location: "dilsukhnagar, hyderabad",
    contactNumber: "6304202209",
    vacancies: 5,
    companyName: "tekjawa solar solutions"
  },
  {
    title: "chief cook",
    payAmount: "8000-10000",
    location: "shivam road hyderabad",
    contactNumber: "8977645467",
    vacancies: 1,
    companyName: "satisfied foods"
  },
  {
    title: "catering staff",
    payAmount: "12000-20000",
    location: "hyderabad",
    contactNumber: "8978122566",
    vacancies: 40,
    companyName: "onehm technology"
  },
  {
    title: "car driver",
    payAmount: "25000-25000+",
    location: "hyderabad",
    contactNumber: "9059423233",
    vacancies: 40,
    companyName: "mallikarjuna fleet"
  },
  {
    title: "pharmacist",
    payAmount: "10000-15000",
    location: "lb nagar",
    contactNumber: "9849234834",
    vacancies: 5,
    companyName: "vasavi medicals"
  },
  {
    title: "home nursing",
    payAmount: "10000-15000",
    location: "lb nagar",
    contactNumber: "9849234834",
    vacancies: 4,
    companyName: "vasavi medicals"
  }
];

// Generate job ID
function generateJobId() {
  return 'REAL_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

// Generate employer ID
function generateEmployerId(companyName) {
  return 'EMP_REAL_' + companyName.replace(/\s+/g, '_').toUpperCase().substr(0, 15) + '_' + Math.random().toString(36).substr(2, 5);
}

// Process and upload jobs
async function seedRealJobs() {
  console.log('🚀 Starting REAL jobs seeding...');
  console.log(`📊 Total jobs to upload: ${realJobs.length}\n`);
  
  let successCount = 0;
  let failCount = 0;
  
  for (let i = 0; i < realJobs.length; i++) {
    const job = realJobs[i];
    const jobNum = i + 1;
    
    try {
      // Get location data
      const locationData = getLocationData(job.location);
      
      // Determine category
      const category = getCategoryFromTitle(job.title);
      
      // Parse pay
      const payAmount = parsePayAmount(job.payAmount);
      const payType = getPayType(job.payAmount, category);
      
      // Generate IDs
      const jobId = generateJobId();
      const employerId = generateEmployerId(job.companyName);
      
      // Timestamps
      const now = Date.now();
      const postedAt = now - Math.floor(Math.random() * 3 * 24 * 60 * 60 * 1000); // Random time in last 3 days
      const expiresAt = postedAt + (15 * 24 * 60 * 60 * 1000); // 15 days from posting
      
      // Build full job object
      const jobData = {
        id: jobId,
        jobId: jobId,
        employerId: employerId,
        title: job.title,
        companyName: job.companyName,
        company: job.companyName,
        location: `${locationData.area}, ${locationData.city}`,
        specificLocation: `${locationData.area}, ${locationData.city}`,
        locationNearby: locationData.area,
        area: locationData.area,
        city: locationData.city,
        latitude: locationData.lat + (Math.random() - 0.5) * 0.01,
        longitude: locationData.lng + (Math.random() - 0.5) * 0.01,
        payRate: parseInt(payAmount),
        payAmount: payAmount,
        payType: payType.toLowerCase(),
        payPeriod: payType.toLowerCase(),
        timing: getShiftTiming(job.title),
        shiftTiming: getShiftTiming(job.title),
        description: generateDescription(job.title, category, job.companyName),
        preferences: ["Experienced preferred", "Local candidates preferred"],
        benefits: [], // No benefits as per user request
        requirements: ["Valid ID proof", "Local address proof", "Immediate joining"],
        skills: [category.toLowerCase(), "communication", "punctuality"],
        vacancies: job.vacancies,
        isActive: true,
        isTrending: job.vacancies >= 20,
        isRemote: false,
        isVerified: true, // Mark real jobs as verified
        isSaved: false,
        postedAt: postedAt,
        createdAt: postedAt,
        postedTime: new Date(postedAt).toISOString(),
        postedDate: new Date(postedAt).toLocaleDateString('en-IN'),
        imageUrl: "",
        phoneNumber: job.contactNumber,
        contactNumber: job.contactNumber,
        contactInfo: job.contactNumber,
        category: category,
        jobType: payType === 'MONTHLY' ? 'FULL_TIME' : 'PART_TIME',
        experienceLevel: job.vacancies >= 10 ? "Fresher" : "1-2 years",
        experienceRequired: job.vacancies >= 10 ? "No experience required" : "1+ year experience",
        workingHours: payType === 'MONTHLY' ? "8 hours" : "Flexible",
        applicationDeadline: "",
        ageRange: "18-45",
        gender: "Any",
        companySize: job.vacancies >= 20 ? "Large" : job.vacancies >= 5 ? "Medium" : "Small",
        industry: category,
        applicationCount: 0,
        distance: null,
        landmark: "Near Main Road",
        salary: `₹${payAmount} ${payType.toLowerCase()}`,
        urgency: getUrgency(job.vacancies),
        employerCreatedAt: postedAt - Math.floor(Math.random() * 180 * 24 * 60 * 60 * 1000), // Random 1-6 months ago
        employerPaidOnTimePercentage: Math.floor(Math.random() * 20) + 80, // 80-100%
        isFilled: false,
        employerTrustTier: "VERIFIED",
        jobImageUrl: "",
        expiresAt: expiresAt,
        expiryDays: 15
      };
      
      // Upload to Firestore
      await db.collection('jobs').doc(jobId).set(jobData);
      
      successCount++;
      console.log(`✅ [${jobNum}/${realJobs.length}] ${job.title} - ${job.companyName}`);
      console.log(`   📍 ${locationData.area}, ${locationData.city}`);
      console.log(`   💰 ₹${payAmount} ${payType}`);
      console.log(`   📞 ${job.contactNumber}`);
      console.log(`   👥 ${job.vacancies} vacancies`);
      console.log(`   🏷️  Category: ${category}\n`);
      
    } catch (error) {
      failCount++;
      console.error(`❌ [${jobNum}/${realJobs.length}] Failed: ${job.title}`);
      console.error(`   Error: ${error.message}\n`);
    }
  }
  
  console.log('\n' + '='.repeat(50));
  console.log('🎉 REAL JOBS SEEDING COMPLETE!');
  console.log('='.repeat(50));
  console.log(`✅ Successfully uploaded: ${successCount} jobs`);
  console.log(`❌ Failed: ${failCount} jobs`);
  console.log(`📊 Total: ${realJobs.length} jobs`);
  console.log('='.repeat(50));
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
