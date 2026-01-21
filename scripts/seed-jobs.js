/**
 * DutyPe - Job Seeding Script
 * 
 * This script uploads sample jobs to Firestore for testing purposes.
 * Run with: node seed-jobs.js
 * 
 * Prerequisites:
 * 1. Install firebase-admin: npm install firebase-admin
 * 2. Download service account key from Firebase Console:
 *    - Go to Firebase Console → Project Settings → Service Accounts
 *    - Click "Generate new private key"
 *    - Save the file as "serviceAccountKey.json" in this scripts folder
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

// Hyderabad areas for realistic locations
const hyderabadAreas = [
  { area: "Gachibowli", city: "Hyderabad", lat: 17.4401, lng: 78.3489 },
  { area: "Madhapur", city: "Hyderabad", lat: 17.4486, lng: 78.3908 },
  { area: "Kondapur", city: "Hyderabad", lat: 17.4600, lng: 78.3548 },
  { area: "Kukatpally", city: "Hyderabad", lat: 17.4849, lng: 78.4138 },
  { area: "Miyapur", city: "Hyderabad", lat: 17.4969, lng: 78.3548 },
  { area: "Hitech City", city: "Hyderabad", lat: 17.4435, lng: 78.3772 },
  { area: "Jubilee Hills", city: "Hyderabad", lat: 17.4325, lng: 78.4073 },
  { area: "Banjara Hills", city: "Hyderabad", lat: 17.4156, lng: 78.4347 },
  { area: "Ameerpet", city: "Hyderabad", lat: 17.4375, lng: 78.4483 },
  { area: "Secunderabad", city: "Hyderabad", lat: 17.4399, lng: 78.4983 },
  { area: "Begumpet", city: "Hyderabad", lat: 17.4432, lng: 78.4672 },
  { area: "Somajiguda", city: "Hyderabad", lat: 17.4239, lng: 78.4538 },
  { area: "Punjagutta", city: "Hyderabad", lat: 17.4260, lng: 78.4506 },
  { area: "Manikonda", city: "Hyderabad", lat: 17.4052, lng: 78.3872 },
  { area: "Nallagandla", city: "Hyderabad", lat: 17.4550, lng: 78.3100 },
  { area: "Chandanagar", city: "Hyderabad", lat: 17.4969, lng: 78.3248 },
  { area: "LB Nagar", city: "Hyderabad", lat: 17.3457, lng: 78.5522 },
  { area: "Dilsukhnagar", city: "Hyderabad", lat: 17.3688, lng: 78.5247 },
  { area: "Uppal", city: "Hyderabad", lat: 17.4065, lng: 78.5593 },
  { area: "ECIL", city: "Hyderabad", lat: 17.4700, lng: 78.5700 },
];

// Company names for different categories
const companyNames = {
  COOK: ["Spice Kitchen", "Royal Caterers", "Home Chef Services", "Tasty Bites", "Family Kitchen", "Gourmet House", "Desi Flavors", "Fresh Cook", "Kitchen Masters", "Food Paradise"],
  MAID: ["Clean Home Services", "Sparkle Maids", "Home Care Plus", "Neat & Tidy", "Domestic Help Hub", "House Helpers", "Clean Sweep", "Home Angels", "Maid Brigade", "Perfect Home"],
  DRIVER: ["Safe Drive Services", "City Cabs", "Personal Drivers", "Drive Easy", "Road Masters", "Quick Transport", "Elite Drivers", "Reliable Rides", "Pro Drivers", "Swift Transport"],
  HELPER: ["Helping Hands", "All-Round Helpers", "Quick Assist", "Support Services", "General Help", "Multi-Task Helpers", "Assist Pro", "Helper Hub", "Work Support", "Task Masters"],
  SECURITY: ["Shield Security", "Safe Guard Services", "Secure Zone", "Watchman Services", "Protection Plus", "Guard Force", "Safety First", "Secure Home", "Vigilant Guards", "Trust Security"],
  GARDENER: ["Green Thumb Gardens", "Nature Care", "Garden Masters", "Plant Paradise", "Lawn Care Pro", "Garden Angels", "Green Space", "Bloom Gardens", "Eco Gardens", "Fresh Greens"],
  CARETAKER: ["Care Plus", "Elder Care Services", "Home Nursing", "Patient Care", "Compassion Care", "Health Helpers", "Senior Support", "Care Companions", "Wellness Care", "Loving Care"],
  DELIVERY: ["Quick Delivery", "Fast Parcels", "Speedy Courier", "Door Step Delivery", "Express Logistics", "Swift Parcels", "Instant Delivery", "Rapid Transport", "Package Pro", "Deliver Now"],
  WAITER: ["Fine Dine Services", "Restaurant Staff", "Hospitality Hub", "Service Stars", "Dining Excellence", "Table Masters", "Guest Services", "Food Service Pro", "Banquet Staff", "Event Servers"],
  ELECTRICIAN: ["Power Solutions", "Electric Pro", "Wiring Experts", "Spark Electric", "Current Masters", "Safe Electric", "Wire Works", "Power Fix", "Electric Care", "Volt Services"],
  PLUMBER: ["Pipe Masters", "Water Works", "Plumb Pro", "Drain Experts", "Flow Fix", "Pipe Solutions", "Leak Stop", "Water Care", "Plumbing Plus", "Fix Flow"],
  PAINTER: ["Color Masters", "Paint Pro", "Wall Art", "Fresh Coat", "Brush Works", "Paint Perfect", "Color Care", "Wall Masters", "Decor Paint", "Finish Pro"],
  CARPENTER: ["Wood Works", "Furniture Masters", "Craft Wood", "Timber Pro", "Wood Art", "Carpenter Plus", "Build Right", "Wood Care", "Joinery Experts", "Custom Wood"],
  RECEPTIONIST: ["Front Desk Pro", "Welcome Services", "Office Reception", "Guest Relations", "Corporate Front", "Reception Plus", "First Impression", "Desk Masters", "Office Welcome", "Reception Hub"],
  CASHIER: ["Cash Pro", "Billing Experts", "Counter Services", "Payment Hub", "Cash Masters", "Checkout Pro", "Register Experts", "Bill Desk", "Cash Care", "Payment Pro"],
  PACKER: ["Pack Pro", "Box Masters", "Packing Experts", "Wrap & Ship", "Package Care", "Pack Right", "Moving Packers", "Safe Pack", "Quick Pack", "Pack Plus"],
  OTHER: ["General Services", "Multi Services", "All Tasks", "Flex Work", "Various Jobs", "Task Hub", "Work Plus", "Job Masters", "Service Pro", "Help Desk"]
};

// Job titles for each category
const jobTitles = {
  COOK: ["Home Cook", "Kitchen Helper", "Chef Assistant", "Breakfast Cook", "Lunch Cook", "Dinner Cook", "Party Cook", "Tiffin Service Cook", "Restaurant Cook", "Catering Cook", "South Indian Cook", "North Indian Cook", "Chinese Cook", "Multi-Cuisine Cook", "Mess Cook"],
  MAID: ["House Maid", "Part-time Maid", "Full-time Maid", "Cleaning Staff", "Housekeeping", "Domestic Helper", "Home Cleaner", "Office Cleaner", "Deep Cleaning Staff", "Daily Maid", "Live-in Maid", "Washing & Ironing", "Kitchen Cleaner", "Bathroom Cleaner", "Floor Cleaner"],
  DRIVER: ["Personal Driver", "Car Driver", "Office Driver", "Family Driver", "Part-time Driver", "Full-time Driver", "Night Driver", "Outstation Driver", "Local Driver", "Cab Driver", "Delivery Driver", "School Van Driver", "Company Driver", "VIP Driver", "Female Driver"],
  HELPER: ["Shop Helper", "Office Helper", "Store Helper", "Warehouse Helper", "Loading Helper", "Unloading Helper", "General Helper", "Kitchen Helper", "Event Helper", "Moving Helper", "Packing Helper", "Cleaning Helper", "Maintenance Helper", "Construction Helper", "Factory Helper"],
  SECURITY: ["Security Guard", "Night Watchman", "Day Security", "Gate Security", "Building Security", "Office Security", "Residential Security", "Mall Security", "Event Security", "Bank Security", "ATM Security", "Factory Security", "Warehouse Security", "Hospital Security", "School Security"],
  GARDENER: ["Garden Maintenance", "Lawn Care", "Plant Care", "Terrace Garden", "Indoor Plants", "Landscaping Helper", "Tree Trimming", "Flower Garden", "Vegetable Garden", "Garden Cleaner", "Watering Staff", "Nursery Helper", "Park Maintenance", "Society Garden", "Farm Helper"],
  CARETAKER: ["Baby Caretaker", "Child Caretaker", "Elder Caretaker", "Patient Caretaker", "Night Caretaker", "Day Caretaker", "Live-in Caretaker", "Part-time Caretaker", "Senior Care", "Disabled Care", "Post-Surgery Care", "Newborn Care", "Toddler Care", "Special Needs Care", "Home Nurse"],
  DELIVERY: ["Food Delivery", "Parcel Delivery", "Document Delivery", "Medicine Delivery", "Grocery Delivery", "E-commerce Delivery", "Local Delivery", "Express Delivery", "Same Day Delivery", "Courier Boy", "Delivery Executive", "Bike Delivery", "Cycle Delivery", "Walking Delivery", "Night Delivery"],
  WAITER: ["Restaurant Waiter", "Hotel Waiter", "Cafe Waiter", "Bar Waiter", "Banquet Waiter", "Event Waiter", "Party Waiter", "Catering Waiter", "Fine Dining Waiter", "Fast Food Staff", "Counter Staff", "Food Server", "Buffet Staff", "Room Service", "Outdoor Catering"],
  ELECTRICIAN: ["Home Electrician", "Office Electrician", "AC Repair", "Fan Repair", "Wiring Work", "Electrical Maintenance", "Switch Board Repair", "Light Fitting", "Inverter Repair", "Generator Repair", "Motor Repair", "Industrial Electrician", "Building Electrician", "Emergency Electrician", "Solar Panel Work"],
  PLUMBER: ["Home Plumber", "Pipe Fitting", "Tap Repair", "Toilet Repair", "Drainage Work", "Water Tank Cleaning", "Leak Repair", "Bathroom Fitting", "Kitchen Plumbing", "Water Heater Repair", "Pump Repair", "Sewage Work", "Pipeline Work", "Emergency Plumber", "Commercial Plumber"],
  PAINTER: ["House Painter", "Wall Painter", "Interior Painter", "Exterior Painter", "Texture Painter", "POP Work", "Waterproofing", "Wood Polishing", "Metal Painting", "Spray Painting", "Touch-up Work", "Commercial Painter", "Industrial Painter", "Decorative Painter", "Whitewash Work"],
  CARPENTER: ["Furniture Carpenter", "Door Repair", "Window Repair", "Cupboard Making", "Bed Making", "Table Making", "Chair Repair", "Wood Polishing", "Modular Kitchen", "Wardrobe Making", "False Ceiling", "Partition Work", "Wood Flooring", "Repair Work", "Custom Furniture"],
  RECEPTIONIST: ["Front Desk", "Office Receptionist", "Hotel Receptionist", "Hospital Receptionist", "Clinic Receptionist", "Gym Receptionist", "Salon Receptionist", "School Receptionist", "Corporate Receptionist", "Guest Relations", "Visitor Management", "Phone Operator", "Admin Assistant", "Office Coordinator", "Welcome Desk"],
  CASHIER: ["Shop Cashier", "Supermarket Cashier", "Restaurant Cashier", "Hotel Cashier", "Mall Cashier", "Billing Executive", "Counter Cashier", "Night Cashier", "Express Counter", "Self-Checkout Helper", "Cash Handler", "Payment Collector", "Accounts Cashier", "Petrol Pump Cashier", "Medical Store Cashier"],
  PACKER: ["Warehouse Packer", "E-commerce Packer", "Gift Packer", "Food Packer", "Medicine Packer", "Garment Packer", "Moving Packer", "Export Packer", "Retail Packer", "Industrial Packer", "Quality Checker", "Sorting Staff", "Labeling Staff", "Box Maker", "Dispatch Helper"],
  OTHER: ["Office Boy", "Tea Boy", "Pantry Boy", "Lift Operator", "Housekeeping Supervisor", "Maintenance Staff", "Multi-tasking Staff", "General Staff", "Support Staff", "Utility Worker", "Facility Staff", "Building Maintenance", "Campus Staff", "Errand Boy", "All-rounder"]
};

// Pay ranges for each category (min, max in INR)
const payRanges = {
  COOK: { daily: [400, 800], hourly: [80, 150], monthly: [12000, 25000] },
  MAID: { daily: [300, 600], hourly: [60, 120], monthly: [8000, 18000] },
  DRIVER: { daily: [500, 1000], hourly: [100, 200], monthly: [15000, 30000] },
  HELPER: { daily: [350, 600], hourly: [70, 120], monthly: [10000, 18000] },
  SECURITY: { daily: [400, 700], hourly: [80, 140], monthly: [12000, 22000] },
  GARDENER: { daily: [350, 600], hourly: [70, 120], monthly: [10000, 18000] },
  CARETAKER: { daily: [500, 1000], hourly: [100, 200], monthly: [15000, 30000] },
  DELIVERY: { daily: [400, 700], hourly: [80, 140], monthly: [12000, 22000] },
  WAITER: { daily: [400, 700], hourly: [80, 140], monthly: [12000, 20000] },
  ELECTRICIAN: { daily: [500, 1000], hourly: [100, 200], monthly: [15000, 30000] },
  PLUMBER: { daily: [500, 900], hourly: [100, 180], monthly: [15000, 28000] },
  PAINTER: { daily: [450, 800], hourly: [90, 160], monthly: [14000, 25000] },
  CARPENTER: { daily: [500, 1000], hourly: [100, 200], monthly: [15000, 30000] },
  RECEPTIONIST: { daily: [400, 700], hourly: [80, 140], monthly: [12000, 22000] },
  CASHIER: { daily: [350, 600], hourly: [70, 120], monthly: [10000, 18000] },
  PACKER: { daily: [350, 550], hourly: [70, 110], monthly: [10000, 16000] },
  OTHER: { daily: [350, 600], hourly: [70, 120], monthly: [10000, 18000] }
};

// Shift timings
const shiftTimings = ["Morning (6 AM - 12 PM)", "Afternoon (12 PM - 6 PM)", "Evening (6 PM - 12 AM)", "Night (12 AM - 6 AM)", "Full Day", "Flexible"];

// Urgency levels
const urgencyLevels = ["IMMEDIATE", "URGENT", "NORMAL", "FLEXIBLE"];

// Benefits
const benefits = ["Free Meals", "Transport Provided", "Accommodation", "Overtime Pay", "Performance Bonus", "Medical Benefits", "Paid Leaves", "Training Provided"];

// Helper functions
function randomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function randomNumber(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function generatePhoneNumber() {
  const prefixes = ["98", "99", "97", "96", "95", "94", "93", "91", "90", "89", "88", "87", "86", "85", "84", "83", "82", "81", "80", "79", "78", "77", "76", "75", "74", "73", "72", "71", "70"];
  return prefixes[Math.floor(Math.random() * prefixes.length)] + Math.floor(10000000 + Math.random() * 90000000);
}

function generateJobId() {
  return 'JOB_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

function generateEmployerId() {
  return 'EMP_SEED_' + Math.random().toString(36).substr(2, 9);
}

// Generate a single job
function generateJob(category) {
  const location = randomElement(hyderabadAreas);
  const payTypes = ["DAILY", "HOURLY", "MONTHLY"];
  const payType = randomElement(payTypes);
  const payRange = payRanges[category][payType.toLowerCase()];
  const payAmount = randomNumber(payRange[0], payRange[1]);
  
  const jobId = generateJobId();
  const employerId = generateEmployerId();
  const now = Date.now();
  const postedAt = now - randomNumber(0, 7 * 24 * 60 * 60 * 1000); // Random time in last 7 days
  const expiresAt = postedAt + (15 * 24 * 60 * 60 * 1000); // 15 days from posting
  
  const title = randomElement(jobTitles[category]);
  const company = randomElement(companyNames[category]);
  const vacancies = randomNumber(1, 5);
  const selectedBenefits = benefits.filter(() => Math.random() > 0.6);
  const phoneNumber = generatePhoneNumber();
  const shiftTiming = randomElement(shiftTimings);
  
  return {
    // Core fields
    id: jobId,
    jobId: jobId,
    employerId: employerId,
    title: title,
    companyName: company,
    location: `${location.area}, ${location.city}`,
    area: location.area,
    city: location.city,
    latitude: location.lat + (Math.random() - 0.5) * 0.02,
    longitude: location.lng + (Math.random() - 0.5) * 0.02,
    payRate: payAmount,
    payAmount: payAmount.toString(),
    payType: payType,
    shiftTiming: shiftTiming,
    description: `We are looking for a reliable ${title.toLowerCase()} to join our team at ${company}. This is a great opportunity for someone with experience in ${category.toLowerCase()} work. The position offers competitive pay and a friendly work environment. Immediate joining preferred.`,
    benefits: selectedBenefits,
    requirements: ["Valid ID proof", "Local address proof"],
    vacancies: vacancies,
    isActive: true,
    isVerified: Math.random() > 0.3,
    postedAt: postedAt,
    createdAt: postedAt,
    contactNumber: phoneNumber,
    category: category,
    jobType: payType === "MONTHLY" ? "FULL_TIME" : "PART_TIME",
    experienceRequired: randomElement(["No experience required", "1+ year experience", "2+ years experience"]),
    ageRange: randomElement(["18-35", "20-40", "25-45", "18-50", "Any"]),
    gender: randomElement(["Male", "Female", "Any"]),
    applicationCount: randomNumber(0, 20),
    landmark: randomElement(["Near Metro", "Near Bus Stop", "Main Road", "Near Market", "Near Hospital"]),
    urgency: randomElement(urgencyLevels),
    employerCreatedAt: postedAt - randomNumber(30, 365) * 24 * 60 * 60 * 1000,
    employerPaidOnTimePercentage: randomNumber(70, 100),
    isFilled: false,
    employerTrustTier: randomElement(["VERIFIED", "TRUSTED", "BUSINESS"]),
    jobImageUrl: "",
    expiresAt: expiresAt,
    expiryDays: 15
  };
}
    expiresAt: expiresAt,
    expiryDays: 15
  };
}

// Main seeding function
async function seedJobs() {
  const categories = Object.keys(jobTitles);
  const jobsPerCategory = 25; // 25 jobs per category
  
  console.log(`🚀 Starting job seeding...`);
  console.log(`📋 Categories: ${categories.length}`);
  console.log(`📝 Jobs per category: ${jobsPerCategory}`);
  console.log(`📊 Total jobs to create: ${categories.length * jobsPerCategory}`);
  console.log('');
  
  let totalCreated = 0;
  
  for (const category of categories) {
    console.log(`\n📁 Creating jobs for category: ${category}`);
    
    const batch = db.batch();
    const jobs = [];
    
    for (let i = 0; i < jobsPerCategory; i++) {
      const job = generateJob(category);
      jobs.push(job);
      
      const docRef = db.collection('jobs').doc(job.jobId);
      batch.set(docRef, job);
    }
    
    try {
      await batch.commit();
      totalCreated += jobsPerCategory;
      console.log(`   ✅ Created ${jobsPerCategory} ${category} jobs`);
    } catch (error) {
      console.error(`   ❌ Error creating ${category} jobs:`, error.message);
    }
  }
  
  console.log(`\n🎉 Seeding complete!`);
  console.log(`📊 Total jobs created: ${totalCreated}`);
}

// Run the seeding
seedJobs()
  .then(() => {
    console.log('\n✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
