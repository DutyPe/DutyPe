/**
 * Seed 5 South Delhi Driver Jobs as Admin
 *
 * Posts to jobmetadata + job_details using the live schema.
 * employerId = 'admin', status = 'open', no review gate.
 *
 * Usage:
 *   node scripts/seed-south-delhi-driver-jobs.js           (dry-run, prints what would be written)
 *   node scripts/seed-south-delhi-driver-jobs.js --apply   (actually writes to Firestore)
 */

const admin = require('firebase-admin');
const fs    = require('fs');
const path  = require('path');
const crypto = require('crypto');

// ─── Firebase init ─────────────────────────────────────────────────────────────
const keyFiles = ['serviceAccountKey.json', 'dutype-860ac-firebase-adminsdk.json'];
let keyPath = null;
for (const f of keyFiles) {
  // Look in scripts/ first, then project root
  for (const dir of [__dirname, path.join(__dirname, '..')]) {
    const p = path.join(dir, f);
    if (fs.existsSync(p)) { keyPath = p; break; }
  }
  if (keyPath) break;
}
if (!keyPath) { console.error('❌ Service account key not found in scripts/'); process.exit(1); }

admin.initializeApp({ credential: admin.credential.cert(require(keyPath)), projectId: 'dutype-860ac' });
const db = admin.firestore();

const APPLY = process.argv.includes('--apply');

// ─── Helpers ───────────────────────────────────────────────────────────────────
const GEOHASH32 = '0123456789bcdefghjkmnpqrstuvwxyz';
function encodeGeohash(lat, lng, precision = 6) {
  let latMin = -90, latMax = 90, lonMin = -180, lonMax = 180;
  let hash = '', isEven = true, bit = 0, ch = 0;
  while (hash.length < precision) {
    if (isEven) {
      const mid = (lonMin + lonMax) / 2;
      if (lng > mid) { ch |= 1 << (4 - bit); lonMin = mid; } else lonMax = mid;
    } else {
      const mid = (latMin + latMax) / 2;
      if (lat > mid) { ch |= 1 << (4 - bit); latMin = mid; } else latMax = mid;
    }
    isEven = !isEven;
    if (bit < 4) { bit++; } else { hash += GEOHASH32[ch]; bit = 0; ch = 0; }
  }
  return hash;
}

function makeJobId(seed) {
  return 'admin_' + crypto.createHash('sha1').update(seed).digest('hex').slice(0, 22);
}

function ts(date) { return admin.firestore.Timestamp.fromDate(date); }

function searchKeywords(title, company, city) {
  const words = new Set(
    [title, company, city, 'driver', 'south delhi', 'delhi']
      .join(' ')
      .toLowerCase()
      .split(/\W+/)
      .filter(w => w.length > 2)
  );
  return [...words];
}

// ─── Job data ──────────────────────────────────────────────────────────────────
const now   = new Date();
const exp30 = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);

const RAW_JOBS = [
  {
    title: 'Car Driver',
    companyName: 'Sharma Family Residence',
    salary: '18000-22000',
    salaryType: 'MONTHLY',
    description:
      'We are looking for a reliable and experienced car driver for a private family in Defence Colony, South Delhi.\n\n' +
      'Responsibilities:\n- Drive the family members to office, school, and appointments.\n- Maintain the vehicle (cleanliness, basic upkeep).\n- Follow traffic rules strictly.\n\n' +
      'Requirements:\n- Valid LMV driving licence (minimum 3 years).\n- Familiar with South Delhi and NCR roads.\n- Honest, punctual, and presentable.\n\n' +
      'Benefits:\nFixed monthly salary + annual bonus.\nAccommodation can be discussed.',
    contactNumber: '9810045678',
    location: { lat: 28.5705, lng: 77.2322 },
    addressText: 'Defence Colony, South Delhi, Delhi 110024',
    companyCity: 'New Delhi',
    jobType: 'Full-time',
    experienceRequired: '3 years',
    educationRequired: '10th Pass',
    gender: 'Male',
    shiftTiming: '7 AM – 8 PM (split duty)',
    vacancies: 1,
  },
  {
    title: 'Delivery Driver (Two-Wheeler)',
    companyName: 'Green Basket Organics',
    salary: '16000-20000',
    salaryType: 'MONTHLY',
    description:
      'Green Basket Organics, a fast-growing organic grocery delivery brand based in Saket, is hiring delivery riders.\n\n' +
      'Responsibilities:\n- Deliver grocery orders within South Delhi (Malviya Nagar, GK, Lajpat Nagar, Hauz Khas).\n- Handle cash on delivery and digital payments.\n- Ensure timely and careful delivery.\n\n' +
      'Requirements:\n- Two-wheeler (petrol or EV) with valid DL.\n- Smartphone with active internet.\n- Good knowledge of South Delhi localities.\n\n' +
      'Benefits:\nFuel reimbursement ₹2500/month.\nPerformance incentive up to ₹3000/month.\nFlexible 6-day work week.',
    contactNumber: '9999512345',
    location: { lat: 28.5253, lng: 77.2070 },
    addressText: 'Saket, South Delhi, Delhi 110017',
    companyCity: 'New Delhi',
    jobType: 'Full-time',
    experienceRequired: '1 year',
    educationRequired: '10th Pass',
    gender: '',
    shiftTiming: '9 AM – 6 PM',
    vacancies: 3,
  },
  {
    title: 'Office Cab Driver',
    companyName: 'TechBridge Solutions Pvt Ltd',
    salary: '20000-25000',
    salaryType: 'MONTHLY',
    description:
      'TechBridge Solutions Pvt Ltd requires an office cab driver for employee transportation in South Delhi.\n\n' +
      'Responsibilities:\n- Pick-up and drop employees to office in Nehru Place, South Delhi.\n- Handle multiple routes across Noida, Gurgaon, and Greater Noida when needed.\n- Maintain vehicle log and fuel records.\n\n' +
      'Requirements:\n- Valid commercial (yellow number plate) licence.\n- Min. 5 years driving experience.\n- Police verification certificate required.\n\n' +
      'Benefits:\nFixed salary + DA.\nOT allowance for night routes.\nPF and ESI as per norms.',
    contactNumber: '9871234560',
    location: { lat: 28.5494, lng: 77.2512 },
    addressText: 'Nehru Place, South Delhi, Delhi 110019',
    companyCity: 'New Delhi',
    jobType: 'Full-time',
    experienceRequired: '5 years',
    educationRequired: '12th Pass',
    gender: 'Male',
    shiftTiming: '8 AM – 7 PM',
    vacancies: 2,
  },
  {
    title: 'School Van Driver',
    companyName: 'Sunrise Public School',
    salary: '15000-18000',
    salaryType: 'MONTHLY',
    description:
      'Sunrise Public School, Greater Kailash – I, is hiring a school van driver for student transportation.\n\n' +
      'Responsibilities:\n- Pick and drop students along fixed routes in GK I, GK II, and Lajpat Nagar.\n- Ensure student safety and discipline inside the vehicle.\n- Co-ordinate with parents and school administration.\n\n' +
      'Requirements:\n- Valid commercial driving licence for LMV.\n- Police verification mandatory.\n- Preferably 30–50 years of age.\n- Kind and patient temperament.\n\n' +
      'Benefits:\nFixed salary paid on 1st of every month.\n2 months advance on joining.\nSchool holidays off.',
    contactNumber: '9818765432',
    location: { lat: 28.5458, lng: 77.2410 },
    addressText: 'Greater Kailash Part I, South Delhi, Delhi 110048',
    companyCity: 'New Delhi',
    jobType: 'Full-time',
    experienceRequired: '2 years',
    educationRequired: '10th Pass',
    gender: 'Male',
    shiftTiming: '6:30 AM – 2 PM and 1 PM – 5 PM (split)',
    vacancies: 1,
  },
  {
    title: 'E-Commerce Delivery Driver (LMV)',
    companyName: 'QuickShip Logistics',
    salary: '18000-23000',
    salaryType: 'MONTHLY',
    description:
      'QuickShip Logistics is expanding its last-mile delivery network in South Delhi and is hiring LMV van drivers.\n\n' +
      'Responsibilities:\n- Load, transport, and deliver parcels in Hauz Khas, Malviya Nagar, Vasant Kunj, and Vasant Vihar.\n- Scan packages and update delivery status in app.\n- Handle COD and digital payment collections.\n\n' +
      'Requirements:\n- LMV commercial driving licence.\n- Own Android smartphone.\n- Ability to handle 60–80 deliveries per day.\n\n' +
      'Benefits:\nFixed salary + per-delivery incentive (₹5–10 per extra delivery above target).\nFuel and vehicle provided by company.\nJoining bonus ₹2000 after 30 days.',
    contactNumber: '9654321098',
    location: { lat: 28.5586, lng: 77.1993 },
    addressText: 'Vasant Kunj, South Delhi, Delhi 110070',
    companyCity: 'New Delhi',
    jobType: 'Full-time',
    experienceRequired: '1 year',
    educationRequired: '10th Pass',
    gender: '',
    shiftTiming: '8 AM – 6 PM',
    vacancies: 5,
  },
];

// ─── Build Firestore docs ───────────────────────────────────────────────────────
function buildDocs(raw) {
  const jobId   = makeJobId(`${raw.title}|${raw.companyName}|${raw.addressText}|south-delhi-2026`);
  const geohash = encodeGeohash(raw.location.lat, raw.location.lng, 6);

  const cardData = {
    title:       raw.title,
    companyName: raw.companyName,
    salary:      raw.salary,
    salaryType:  raw.salaryType,
    location:    raw.location,
    geohash,
    addressText: raw.addressText,
    jobType:     raw.jobType,
    status:      'open',
    createdAt:   ts(now),
    employerId:  'admin',
    vacancies:   raw.vacancies,
    searchKeywords: searchKeywords(raw.title, raw.companyName, raw.companyCity),
  };

  const detailsData = {
    employerId:          'admin',
    expiresAt:           ts(exp30),
    contactNumber:       raw.contactNumber,
    description:         raw.description,
    gender:              raw.gender || '',
    experienceRequired:  raw.experienceRequired || '',
    educationRequired:   raw.educationRequired || '',
    shiftTiming:         raw.shiftTiming || '',
    companyCity:         raw.companyCity,
    applicationCount:    0,
    createdAt:           ts(now),
    idempotencyKey:      `admin_south_delhi_driver:${jobId}`,
  };

  return { jobId, cardData, detailsData };
}

// ─── Main ───────────────────────────────────────────────────────────────────────
async function run() {
  console.log(`\n🚗  South Delhi Driver Jobs — ${APPLY ? '🔴 APPLY MODE' : '🟡 DRY-RUN'}\n`);

  const docs = RAW_JOBS.map(buildDocs);

  for (const { jobId, cardData, detailsData } of docs) {
    console.log(`\n📋 ${cardData.title}  ─  ${cardData.companyName}`);
    console.log(`   ID:        ${jobId}`);
    console.log(`   Address:   ${cardData.addressText}`);
    console.log(`   Salary:    ₹${cardData.salary} / ${cardData.salaryType}`);
    console.log(`   Vacancies: ${cardData.vacancies}`);
    console.log(`   Contact:   ${detailsData.contactNumber}`);
    console.log(`   Geohash:   ${cardData.geohash}`);
    console.log(`   Keywords:  ${cardData.searchKeywords.join(', ')}`);

    if (APPLY) {
      try {
        await db.collection('jobmetadata').doc(jobId).set(cardData);
        await db.collection('job_details').doc(jobId).set(detailsData);
        console.log(`   ✅ Written to Firestore`);
      } catch (err) {
        console.error(`   ❌ Error: ${err.message}`);
      }
    }
  }

  if (!APPLY) {
    console.log('\n──────────────────────────────────────────────────────');
    console.log('DRY-RUN complete. Run with --apply to write to Firestore.');
  } else {
    console.log('\n✅ All 5 jobs written to jobmetadata + job_details.');
  }
}

run().then(() => process.exit(0)).catch(e => { console.error(e); process.exit(1); });
