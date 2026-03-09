/**
 * DutyPe - Teaching Jobs Seeding Script
 * Posts 7 female teacher positions in Alwyn Colony and New Bowenpally
 * 
 * Run with: node seed-teaching-jobs.js
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

// Helper functions
function generateJobId() {
  return 'JOB_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

function generateEmployerId() {
  return 'EMP_TEACHING_' + Math.random().toString(36).substr(2, 9);
}

// Teaching job positions
const teachingJobs = [
  {
    title: "English Teacher - Classes 6th to 9th (Female Only)",
    description: "We are looking for a qualified and dedicated Female English Teacher for classes 6th to 9th. The ideal candidate should have strong communication skills and a passion for teaching. Immediate joining preferred.",
    requirements: ["Female candidates only", "Qualification in English/Education", "Experience in teaching 6th-9th classes", "Strong communication skills"],
    benefits: ["Competitive salary", "Professional environment", "Growth opportunities"],
    location: "Alwyn Colony, Jagadgiri Gutta"
  },
  {
    title: "Social Studies Teacher - Classes 6th to 9th (Female Only)",
    description: "We are hiring a qualified Female Social Studies Teacher for classes 6th to 9th. The candidate should have good knowledge of history, geography, and civics. Passionate teachers are encouraged to apply.",
    requirements: ["Female candidates only", "Degree in Social Studies/History/Geography", "Teaching experience for 6th-9th classes", "Good subject knowledge"],
    benefits: ["Attractive salary package", "Supportive work environment", "Career development"],
    location: "New Bowenpally"
  },
  {
    title: "Telugu Primary Teacher - Classes 3rd to 5th (Female Only)",
    description: "We need an experienced Female Telugu Primary Teacher for classes 3rd to 5th. The candidate should be fluent in Telugu and have experience teaching young children. Join our dedicated teaching team.",
    requirements: ["Female candidates only", "Fluency in Telugu language", "Experience with primary classes (3rd-5th)", "Patient and caring approach"],
    benefits: ["Good salary", "Friendly atmosphere", "Professional growth"],
    location: "Alwyn Colony, Jagadgiri Gutta"
  },
  {
    title: "Physics Teacher - Olympiad Only, Classes 6th to 9th (Female Only)",
    description: "We are seeking a Female Physics Teacher specifically for Olympiad preparation for classes 6th to 9th. The candidate should have strong conceptual knowledge and experience in competitive exam coaching.",
    requirements: ["Female candidates only", "Strong Physics background", "Olympiad coaching experience", "Ability to teach advanced concepts"],
    benefits: ["Competitive compensation", "Olympiad training support", "Recognition for results"],
    location: "New Bowenpally"
  },
  {
    title: "Pre-Primary Teacher (Female Only)",
    description: "We are looking for caring and enthusiastic Female Pre-Primary Teachers. The ideal candidate should love working with young children and have experience in early childhood education. Create a nurturing learning environment.",
    requirements: ["Female candidates only", "Pre-primary teaching experience", "Patience with young children", "Creative teaching methods"],
    benefits: ["Good salary package", "Child-friendly environment", "Training provided"],
    location: "Alwyn Colony, Jagadgiri Gutta"
  },
  {
    title: "Calligraphy Teacher - Lucida Handwriting (Female Only)",
    description: "We need a skilled Female Calligraphy Teacher specializing in Lucida Handwriting. The candidate should have excellent handwriting skills and experience teaching calligraphy to students. Help students develop beautiful handwriting.",
    requirements: ["Female candidates only", "Expertise in Lucida Handwriting", "Calligraphy teaching experience", "Excellent handwriting skills"],
    benefits: ["Attractive pay", "Flexible schedule", "Creative work environment"],
    location: "New Bowenpally"
  },
  {
    title: "Soft Skills Trainer (Female Only)",
    description: "We are hiring a Female Soft Skills Trainer to help students develop communication, personality, and interpersonal skills. The candidate should be confident, articulate, and experienced in training programs.",
    requirements: ["Female candidates only", "Experience in soft skills training", "Excellent communication skills", "Confident personality"],
    benefits: ["Competitive salary", "Professional development", "Impactful work"],
    location: "Alwyn Colony, Jagadgiri Gutta"
  }
];

// Location coordinates
const locations = {
  "Alwyn Colony, Jagadgiri Gutta": { lat: 17.4850, lng: 78.4950 },
  "New Bowenpally": { lat: 17.4900, lng: 78.4850 }
};

// Generate job data
function createTeachingJob(jobData) {
  const jobId = generateJobId();
  const employerId = generateEmployerId();
  const now = admin.firestore.Timestamp.now();
  const locationCoords = locations[jobData.location];
  const expiresAt = Date.now() + (30 * 24 * 60 * 60 * 1000); // 30 days from now
  
  // Extract city from location
  const city = jobData.location.includes('Alwyn') ? 'Hyderabad' : 'Hyderabad';
  
  return {
    id: jobId,
    employerId: employerId,
    title: jobData.title,
    companyName: "Educational Institution",
    employerName: "Educational Institution",
    location: jobData.location,
    city: city,
    state: "Telangana",
    latitude: locationCoords.lat,
    longitude: locationCoords.lng,
    payAmount: "Negotiable",
    payType: "MONTHLY",
    workingHours: "6 hours",
    description: jobData.description,
    benefits: jobData.benefits,
    requirements: jobData.requirements,
    vacancies: 1,
    isActive: true,
    postedAt: now,
    createdAt: now,
    updatedAt: now,
    contactNumber: "9505549949",
    whatsappNumber: "9505549949",
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
    tags: ["teacher", "education", "female-only", "immediate-joining"]
  };
}

// Main seeding function
async function seedTeachingJobs() {
  console.log(`🚀 Starting teaching jobs seeding...`);
  console.log(`📝 Total jobs to create: ${teachingJobs.length}`);
  console.log('');
  
  const batch = db.batch();
  const jobs = [];
  
  for (const jobData of teachingJobs) {
    const job = createTeachingJob(jobData);
    jobs.push(job);
    
    const docRef = db.collection('jobs').doc(job.id);
    batch.set(docRef, job);
    
    console.log(`📋 Prepared: ${job.title}`);
  }
  
  try {
    await batch.commit();
    console.log(`\n✅ Successfully created ${teachingJobs.length} teaching jobs`);
    console.log(`📞 Contact number for all jobs: 9505549949`);
    console.log(`📍 Locations: Alwyn Colony (Jagadgiri Gutta) & New Bowenpally`);
  } catch (error) {
    console.error(`❌ Error creating jobs:`, error.message);
    throw error;
  }
}

// Run the seeding
seedTeachingJobs()
  .then(() => {
    console.log('\n🎉 Teaching jobs posted successfully!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
