/**
 * DutyPe - Sunflower School Teaching Jobs
 * Posts 2 teacher positions from Sunflower School (EM), Payakarao Peta
 * 
 * Run with: node seed-sunflower-school-jobs.js
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

// Helper functions
function generateJobId() {
  return 'JOB_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

function generateEmployerId() {
  return 'EMP_SUNFLOWER_' + Math.random().toString(36).substr(2, 9);
}

// Sunflower School job positions
const sunflowerJobs = [
  {
    title: "English Teacher",
    subject: "English",
    description: "Sunflower School (EM) in Payakarao Peta is looking for a qualified English Teacher. We are seeking a dedicated educator with strong communication skills and a passion for teaching. The ideal candidate should be able to engage students and create an effective learning environment. Any gender can apply.",
    requirements: ["Qualification in English/Education", "Good communication skills", "Teaching experience preferred", "Passionate about education"],
    benefits: ["Competitive salary", "Professional work environment", "Career growth opportunities"],
    vacancies: 1
  },
  {
    title: "Biology Teacher",
    subject: "Biology",
    description: "Sunflower School (EM) in Payakarao Peta is hiring a Biology Teacher. We need an experienced educator with strong knowledge of biological sciences. The candidate should be able to teach effectively and inspire students to learn. Any gender can apply.",
    requirements: ["Degree in Biology/Life Sciences", "Strong subject knowledge", "Teaching experience preferred", "Ability to conduct practical sessions"],
    benefits: ["Good salary package", "Supportive management", "Professional development"],
    vacancies: 1
  }
];

// Location coordinates for Payakarao Peta (approximate)
const location = {
  name: "Payakarao Peta",
  city: "Vijayawada",
  state: "Andhra Pradesh",
  lat: 16.5062,
  lng: 80.6480
};

// Generate job data
function createSunflowerJob(jobData) {
  const jobId = generateJobId();
  const employerId = generateEmployerId();
  const now = admin.firestore.Timestamp.now();
  const expiresAt = Date.now() + (30 * 24 * 60 * 60 * 1000); // 30 days from now
  
  return {
    id: jobId,
    employerId: employerId,
    title: jobData.title,
    companyName: "Sunflower School (EM)",
    employerName: "Sunflower School (EM)",
    location: `${location.name}, ${location.city}`,
    city: location.city,
    state: location.state,
    latitude: location.lat,
    longitude: location.lng,
    payAmount: "Negotiable",
    payType: "MONTHLY",
    workingHours: "6-7 hours",
    description: jobData.description,
    benefits: jobData.benefits,
    requirements: jobData.requirements,
    vacancies: jobData.vacancies,
    isActive: true,
    postedAt: now,
    createdAt: now,
    updatedAt: now,
    contactNumber: "9347299567",
    whatsappNumber: "9347299567",
    jobType: "FULL_TIME",
    category: "Teacher",
    experienceRequired: "EXPERIENCED",
    educationRequired: "Graduate",
    applicationCount: 0,
    isFilled: false,
    expiresAt: expiresAt,
    expiryDays: 30,
    isVerified: true,
    employerRating: 4.5,
    viewCount: 0,
    shareCount: 0,
    tags: ["teacher", "education", jobData.subject.toLowerCase(), "school", "any-gender"]
  };
}

// Main seeding function
async function seedSunflowerJobs() {
  console.log(`🚀 Starting Sunflower School jobs seeding...`);
  console.log(`📝 Total jobs to create: ${sunflowerJobs.length}`);
  console.log('');
  
  const batch = db.batch();
  const jobs = [];
  
  for (const jobData of sunflowerJobs) {
    const job = createSunflowerJob(jobData);
    jobs.push(job);
    
    const docRef = db.collection('jobs').doc(job.id);
    batch.set(docRef, job);
    
    console.log(`📋 Prepared: ${job.title} (${jobData.subject})`);
  }
  
  try {
    await batch.commit();
    console.log(`\n✅ Successfully created ${sunflowerJobs.length} Sunflower School jobs`);
    console.log(`📞 Contact number: 9347299567`);
    console.log(`🏫 School: Sunflower School (EM)`);
    console.log(`📍 Location: Payakarao Peta, Vijayawada`);
    console.log(`👥 Gender: Any gender can apply`);
  } catch (error) {
    console.error(`❌ Error creating jobs:`, error.message);
    throw error;
  }
}

// Run the seeding
seedSunflowerJobs()
  .then(() => {
    console.log('\n🎉 Sunflower School jobs posted successfully!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
