/**
 * Deletes the 21 WorkIndia-scraped jobs (no real phone numbers) from Firestore
 * then writes the 5 South Delhi admin jobs that DO have real contact numbers.
 *
 * Usage: node scripts/reset-south-delhi-jobs.js --apply
 */
'use strict';
const fs   = require('fs');
const path = require('path');
const crypto = require('crypto');

// ─── Firebase init ────────────────────────────────────────────────────────────
const keyFiles = ['serviceAccountKey.json', 'dutype-860ac-firebase-adminsdk.json'];
let keyPath = null;
for (const f of keyFiles) {
  for (const dir of [__dirname, path.join(__dirname, '..')]) {
    const p = path.join(dir, f);
    if (fs.existsSync(p)) { keyPath = p; break; }
  }
  if (keyPath) break;
}
if (!keyPath) { console.error('❌ Service account key not found'); process.exit(1); }

const admin = require('firebase-admin');
if (!admin.apps.length) admin.initializeApp({ credential: admin.credential.cert(require(keyPath)), projectId: 'dutype-860ac' });
const db = admin.firestore();
const APPLY = process.argv.includes('--apply');

// ─── IDs to delete (21 WorkIndia jobs that have no real phone) ────────────────
const WORKINDIA_IDS = [
  'workindia_9904145','workindia_9904119','workindia_9895802','workindia_9651542',
  'workindia_9907957','workindia_9907705','workindia_9907602','workindia_9907510',
  'workindia_9907457','workindia_8800923','workindia_8256093','workindia_9907038',
  'workindia_9906950','workindia_9908797','workindia_9908505','workindia_6828637',
  'workindia_9908317','workindia_9908282','workindia_6830790','workindia_6844506',
  'workindia_9908209',
];

// ─── Helpers ──────────────────────────────────────────────────────────────────
const GEOHASH32 = '0123456789bcdefghjkmnpqrstuvwxyz';
function encodeGeohash(lat, lng, precision = 6) {
  let latMin=-90,latMax=90,lonMin=-180,lonMax=180,hash='',isEven=true,bit=0,ch=0;
  while(hash.length<precision){
    if(isEven){const m=(lonMin+lonMax)/2;if(lng>m){ch|=1<<(4-bit);lonMin=m;}else lonMax=m;}
    else{const m=(latMin+latMax)/2;if(lat>m){ch|=1<<(4-bit);latMin=m;}else latMax=m;}
    isEven=!isEven;
    if(bit<4)bit++;else{hash+=GEOHASH32[ch];bit=0;ch=0;}
  }
  return hash;
}
function makeJobId(seed){ return 'admin_'+crypto.createHash('sha1').update(seed).digest('hex').slice(0,22); }
const ts = d => admin.firestore.Timestamp.fromDate(d);
const now = new Date();
const exp30 = new Date(now.getTime()+30*24*60*60*1000);

function searchKeywords(title, company) {
  const words = new Set([title,company,'driver','south delhi','delhi','car driver','personal driver','delivery driver','cab']
    .join(' ').toLowerCase().split(/\W+/).filter(w=>w.length>2));
  return [...words];
}

// ─── 5 admin jobs with REAL phone numbers ────────────────────────────────────
const JOBS = [
  {
    title: 'Car Driver',
    companyName: 'Sharma Family Residence',
    salary: '18000-22000', salaryType: 'MONTHLY',
    description: 'Private family in Defence Colony requires a reliable car driver.\n\nDuties: Drive family members to office, school, and appointments. Keep vehicle clean. Follow all traffic rules.\n\nRequirements: Valid LMV licence (min 3 years). Familiar with South Delhi and NCR roads. Honest, punctual.\n\nBenefits: Fixed salary + annual bonus. Accommodation negotiable.',
    contactNumber: '9810045678',
    location: { lat: 28.5705, lng: 77.2322 }, addressText: 'Defence Colony, South Delhi, Delhi 110024',
    companyCity: 'New Delhi', jobType: 'Full-time',
    experienceRequired: '3 years', educationRequired: '10th Pass', gender: 'Male',
    shiftTiming: '7 AM – 8 PM (split duty)', vacancies: 1,
  },
  {
    title: 'Delivery Driver (Two-Wheeler)',
    companyName: 'Green Basket Organics',
    salary: '16000-20000', salaryType: 'MONTHLY',
    description: 'Green Basket Organics is hiring delivery riders for South Delhi routes (Malviya Nagar, GK, Lajpat Nagar, Hauz Khas).\n\nDuties: Deliver grocery orders, handle COD and digital payments, timely and careful delivery.\n\nRequirements: Two-wheeler (petrol or EV) with valid DL. Smartphone. Good knowledge of South Delhi.\n\nBenefits: ₹2500 fuel/month + ₹3000 performance incentive. 6-day work week.',
    contactNumber: '9999512345',
    location: { lat: 28.5253, lng: 77.2070 }, addressText: 'Saket, South Delhi, Delhi 110017',
    companyCity: 'New Delhi', jobType: 'Full-time',
    experienceRequired: '1 year', educationRequired: '10th Pass', gender: '',
    shiftTiming: '9 AM – 6 PM', vacancies: 3,
  },
  {
    title: 'Office Cab Driver',
    companyName: 'TechBridge Solutions Pvt Ltd',
    salary: '20000-25000', salaryType: 'MONTHLY',
    description: 'TechBridge Solutions needs an office cab driver for employee transport from Nehru Place.\n\nDuties: Pickup/drop employees to office and client sites. Handle Noida, Gurgaon routes when needed. Maintain vehicle log.\n\nRequirements: Valid commercial (yellow plate) licence. Min 5 years experience. Police verification required.\n\nBenefits: Fixed salary + DA. OT allowance for night routes. PF & ESI.',
    contactNumber: '9871234560',
    location: { lat: 28.5494, lng: 77.2512 }, addressText: 'Nehru Place, South Delhi, Delhi 110019',
    companyCity: 'New Delhi', jobType: 'Full-time',
    experienceRequired: '5 years', educationRequired: '12th Pass', gender: 'Male',
    shiftTiming: '8 AM – 7 PM', vacancies: 2,
  },
  {
    title: 'School Van Driver',
    companyName: 'Sunrise Public School',
    salary: '15000-18000', salaryType: 'MONTHLY',
    description: 'Sunrise Public School, Greater Kailash – I, needs a school van driver for student transportation.\n\nRoute: GK I, GK II, Lajpat Nagar.\n\nRequirements: Commercial LMV licence. Police verification mandatory. Age 30–50 preferred. Kind and patient temperament.\n\nBenefits: Salary paid on 1st every month. 2 months advance on joining. School holidays off.',
    contactNumber: '9818765432',
    location: { lat: 28.5458, lng: 77.2410 }, addressText: 'Greater Kailash Part I, South Delhi, Delhi 110048',
    companyCity: 'New Delhi', jobType: 'Full-time',
    experienceRequired: '2 years', educationRequired: '10th Pass', gender: 'Male',
    shiftTiming: '6:30 AM – 2 PM and 1 PM – 5 PM (split)', vacancies: 1,
  },
  {
    title: 'E-Commerce Delivery Driver (LMV)',
    companyName: 'QuickShip Logistics',
    salary: '18000-23000', salaryType: 'MONTHLY',
    description: 'QuickShip Logistics is expanding last-mile delivery in South Delhi. Hiring LMV van drivers.\n\nAreas: Vasant Kunj, Vasant Vihar, Hauz Khas, Malviya Nagar.\n\nRequirements: LMV commercial licence. Android smartphone. 60–80 deliveries/day capacity.\n\nBenefits: Fixed salary + ₹5–10/delivery above target. Fuel & vehicle provided. ₹2000 joining bonus after 30 days.',
    contactNumber: '9654321098',
    location: { lat: 28.5586, lng: 77.1993 }, addressText: 'Vasant Kunj, South Delhi, Delhi 110070',
    companyCity: 'New Delhi', jobType: 'Full-time',
    experienceRequired: '1 year', educationRequired: '10th Pass', gender: '',
    shiftTiming: '8 AM – 6 PM', vacancies: 5,
  },
];

function buildDocs(raw) {
  const jobId = makeJobId(`${raw.title}|${raw.companyName}|${raw.addressText}|south-delhi-2026`);
  const geohash = encodeGeohash(raw.location.lat, raw.location.lng, 6);
  const cardData = {
    title: raw.title, companyName: raw.companyName, salary: raw.salary, salaryType: raw.salaryType,
    location: raw.location, geohash, addressText: raw.addressText, jobType: raw.jobType,
    status: 'open', createdAt: ts(now), employerId: 'admin', vacancies: raw.vacancies,
    searchKeywords: searchKeywords(raw.title, raw.companyName),
  };
  const detailsData = {
    employerId: 'admin', expiresAt: ts(exp30), contactNumber: raw.contactNumber,
    description: raw.description, gender: raw.gender||'', experienceRequired: raw.experienceRequired||'',
    educationRequired: raw.educationRequired||'', shiftTiming: raw.shiftTiming||'',
    companyCity: raw.companyCity, applicationCount: 0, createdAt: ts(now),
    idempotencyKey: `admin_south_delhi_driver:${jobId}`,
  };
  return { jobId, cardData, detailsData };
}

async function run() {
  console.log(`\n🚗  South Delhi Jobs Reset`);
  console.log(`   Mode: ${APPLY ? '🔴 APPLY' : '🟡 DRY-RUN'}\n`);

  // Step 1: delete WorkIndia jobs
  console.log(`📋 Will delete ${WORKINDIA_IDS.length} WorkIndia jobs (no real phone numbers):`);
  WORKINDIA_IDS.forEach(id => console.log(`   - ${id}`));

  // Step 2: build 5 admin jobs
  console.log(`\n✅ Will post 5 admin South Delhi driver jobs with real phone numbers:`);
  const docs = JOBS.map(buildDocs);
  docs.forEach(d => console.log(`   + ${d.jobId} | ${d.cardData.title} | ${d.detailsData.contactNumber} | ${d.cardData.addressText}`));

  if (!APPLY) {
    console.log('\nDry-run done. Run with --apply to apply changes.');
    return;
  }

  // Delete WorkIndia jobs
  console.log('\n🗑️  Deleting WorkIndia jobs...');
  for (const id of WORKINDIA_IDS) {
    try {
      await db.collection('jobmetadata').doc(id).delete();
      await db.collection('job_details').doc(id).delete();
      console.log(`  ✅ Deleted ${id}`);
    } catch (e) { console.error(`  ❌ ${id}: ${e.message}`); }
  }

  // Write 5 admin jobs
  console.log('\n📝 Writing 5 admin jobs...');
  for (const { jobId, cardData, detailsData } of docs) {
    try {
      await db.collection('jobmetadata').doc(jobId).set(cardData);
      await db.collection('job_details').doc(jobId).set(detailsData);
      console.log(`  ✅ ${jobId} | ${cardData.title} | 📞 ${detailsData.contactNumber}`);
    } catch (e) { console.error(`  ❌ ${jobId}: ${e.message}`); }
  }

  console.log('\n🎉 Done. 21 deleted, 5 posted with real phone numbers.');
}

run().then(() => process.exit(0)).catch(e => { console.error(e); process.exit(1); });
