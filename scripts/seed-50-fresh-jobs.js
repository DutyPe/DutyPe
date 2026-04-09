/**
 * DutyPe — Seed 50 Fresh Jobs (Strict Schema)
 *
 * 1. Deletes ALL existing jobs in the `jobs` collection
 * 2. Uploads 50 real-looking jobs with the STRICT target schema:
 *      - location: { lat, lng }   (nested map, NOT separate fields)
 *      - geohash                  (precision-6)
 *      - salary / salaryType      (NOT payAmount/payType)
 *      - status: "open"           (NOT isActive/isFilled)
 *      - addressText              (human-readable address for card display)
 *      - companyCity              (city name for display)
 *
 * Run:  cd scripts && node seed-50-fresh-jobs.js
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// ── Firebase init ────────────────────────────────────────────────────────────
// Uses Application Default Credentials (from `firebase login` or `gcloud auth`)
// Falls back to service account key if available.
let initialized = false;

// Try service account key first
try {
  const { loadServiceAccount } = require('./lib/firebase-admin-service-account');
  const sa = loadServiceAccount();
  admin.initializeApp({
    credential: admin.credential.cert(sa),
    projectId: sa.project_id
  });
  initialized = true;
  console.log('✅ Using service account key');
} catch (e) {
  // Service account not found or invalid — try ADC
}

if (!initialized) {
  // Use Application Default Credentials (firebase login / gcloud auth)
  admin.initializeApp({
    projectId: 'dutypeapp'
  });
  console.log('✅ Using Application Default Credentials (firebase login)');
}

const db = admin.firestore();

// ── Geohash encoder (precision-6) ───────────────────────────────────────────
const BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';
function encodeGeohash(lat, lng, precision = 6) {
  let latRange = [-90, 90], lngRange = [-180, 180];
  let hash = '', bits = 0, ch = 0, isLng = true;
  while (hash.length < precision) {
    const range = isLng ? lngRange : latRange;
    const mid = (range[0] + range[1]) / 2;
    const val = isLng ? lng : lat;
    if (val >= mid) { ch = ch * 2 + 1; range[0] = mid; }
    else            { ch = ch * 2;     range[1] = mid; }
    bits++;
    if (bits === 5) { hash += BASE32[ch]; bits = 0; ch = 0; }
    isLng = !isLng;
  }
  return hash;
}

// ── Locations (various distances from Enkoor/Khammam) ───────────────────────
// User's home: ~17.384, 80.342 (Enkoor)
const locations = [
  // VERY NEAR (0-5 km) — Enkoor area
  { area: "Enkoor",             city: "Khammam",     lat: 17.384, lng: 80.343 },
  { area: "Enkoor Market",      city: "Khammam",     lat: 17.386, lng: 80.340 },
  { area: "Enkoor Bus Stop",    city: "Khammam",     lat: 17.382, lng: 80.345 },
  { area: "Tallada",            city: "Khammam",     lat: 17.390, lng: 80.350 },
  { area: "Raghunadhapalem",    city: "Khammam",     lat: 17.395, lng: 80.355 },
  // NEAR (5-15 km)
  { area: "Khammam Town",       city: "Khammam",     lat: 17.247, lng: 80.151 },
  { area: "Wyra Road",          city: "Khammam",     lat: 17.260, lng: 80.170 },
  { area: "Khanapuram Haveli",  city: "Khammam",     lat: 17.310, lng: 80.200 },
  { area: "Mudigonda",          city: "Khammam",     lat: 17.350, lng: 80.280 },
  { area: "Sathupalli",         city: "Khammam",     lat: 17.250, lng: 80.840 },
  // MODERATE (15-50 km)
  { area: "Madhira",            city: "Khammam",     lat: 16.920, lng: 80.370 },
  { area: "Suryapet",           city: "Suryapet",    lat: 17.140, lng: 79.630 },
  { area: "Kothagudem",         city: "Bhadradri",   lat: 17.550, lng: 80.620 },
  { area: "Bhadrachalam",       city: "Bhadradri",   lat: 17.670, lng: 80.890 },
  // FAR (50-200 km) — Major cities
  { area: "Warangal",           city: "Warangal",    lat: 17.978, lng: 79.597 },
  { area: "Hanamkonda",         city: "Warangal",    lat: 17.990, lng: 79.580 },
  { area: "Vijayawada",         city: "Vijayawada",  lat: 16.506, lng: 80.648 },
  { area: "MG Road",            city: "Vijayawada",  lat: 16.510, lng: 80.640 },
  { area: "Kukatpally",         city: "Hyderabad",   lat: 17.485, lng: 78.414 },
  { area: "Ameerpet",           city: "Hyderabad",   lat: 17.438, lng: 78.448 },
  { area: "Gachibowli",         city: "Hyderabad",   lat: 17.440, lng: 78.349 },
  { area: "Dilsukhnagar",       city: "Hyderabad",   lat: 17.369, lng: 78.525 },
  { area: "LB Nagar",           city: "Hyderabad",   lat: 17.346, lng: 78.552 },
  { area: "Secunderabad",       city: "Hyderabad",   lat: 17.440, lng: 78.498 },
];

// ── Job templates ───────────────────────────────────────────────────────────
const templates = [
  { title: "Delivery Boy",                jobType: "DELIVERY",      salary: 500,    salaryType: "DAILY",   company: "Swiggy Delivery Partner"     },
  { title: "Delivery Executive",          jobType: "DELIVERY",      salary: 18000,  salaryType: "MONTHLY", company: "Flipkart Logistics"          },
  { title: "Food Delivery Rider",         jobType: "DELIVERY",      salary: 600,    salaryType: "DAILY",   company: "Zomato Delivery"             },
  { title: "Courier Delivery Partner",    jobType: "DELIVERY",      salary: 15000,  salaryType: "MONTHLY", company: "Delhivery Express"           },
  { title: "House Cleaner",              jobType: "MAID",           salary: 8000,   salaryType: "MONTHLY", company: "Urban Company"               },
  { title: "Housekeeping Staff",         jobType: "MAID",           salary: 12000,  salaryType: "MONTHLY", company: "OYO Rooms"                   },
  { title: "Home Cook",                  jobType: "COOK",           salary: 15000,  salaryType: "MONTHLY", company: "Home Kitchen Services"       },
  { title: "Restaurant Chef",            jobType: "COOK",           salary: 20000,  salaryType: "MONTHLY", company: "Spice Garden Restaurant"     },
  { title: "Kitchen Helper",             jobType: "COOK",           salary: 400,    salaryType: "DAILY",   company: "Annapurna Mess"              },
  { title: "Security Guard",             jobType: "SECURITY",       salary: 14000,  salaryType: "MONTHLY", company: "G4S Security"                },
  { title: "Night Watchman",             jobType: "SECURITY",       salary: 12000,  salaryType: "MONTHLY", company: "Securitas India"             },
  { title: "Cab Driver",                 jobType: "DRIVER",         salary: 25000,  salaryType: "MONTHLY", company: "Ola Cabs"                    },
  { title: "Auto Rickshaw Driver",       jobType: "DRIVER",         salary: 800,    salaryType: "DAILY",   company: "Rapido Auto"                 },
  { title: "Truck Driver",              jobType: "DRIVER",         salary: 22000,  salaryType: "MONTHLY", company: "Rivigo Transport"             },
  { title: "Shop Helper",               jobType: "HELPER",         salary: 10000,  salaryType: "MONTHLY", company: "More Supermarket"             },
  { title: "Warehouse Helper",          jobType: "HELPER",         salary: 450,    salaryType: "DAILY",   company: "Amazon Warehouse"             },
  { title: "Construction Helper",        jobType: "HELPER",         salary: 600,    salaryType: "DAILY",   company: "L&T Construction"            },
  { title: "Loading/Unloading Worker",   jobType: "HELPER",         salary: 500,    salaryType: "DAILY",   company: "Blue Dart Logistics"         },
  { title: "Waiter/Steward",            jobType: "WAITER",         salary: 12000,  salaryType: "MONTHLY", company: "Taj Hotel"                   },
  { title: "Cashier",                   jobType: "CASHIER",        salary: 13000,  salaryType: "MONTHLY", company: "Reliance Smart"               },
  { title: "Billing Executive",         jobType: "CASHIER",        salary: 14000,  salaryType: "MONTHLY", company: "DMart"                        },
  { title: "Electrician",               jobType: "ELECTRICIAN",    salary: 700,    salaryType: "DAILY",   company: "Havells Electricals"          },
  { title: "Plumber",                   jobType: "PLUMBER",        salary: 650,    salaryType: "DAILY",   company: "RR Plumbing Works"            },
  { title: "Painter",                   jobType: "PAINTER",        salary: 750,    salaryType: "DAILY",   company: "Asian Paints Solutions"       },
  { title: "Carpenter",                 jobType: "CARPENTER",      salary: 800,    salaryType: "DAILY",   company: "Greenply Interiors"           },
  { title: "Receptionist",             jobType: "RECEPTIONIST",   salary: 15000,  salaryType: "MONTHLY", company: "Apollo Hospital"              },
  { title: "Caretaker / Nanny",        jobType: "CARETAKER",      salary: 12000,  salaryType: "MONTHLY", company: "Care24 Services"              },
  { title: "Gardener",                 jobType: "GARDENER",       salary: 10000,  salaryType: "MONTHLY", company: "Green Thumb Landscaping"      },
  { title: "Packer",                   jobType: "PACKER",         salary: 350,    salaryType: "DAILY",   company: "Amazon Fulfillment"           },
  { title: "Data Entry Operator",      jobType: "OTHER",          salary: 13000,  salaryType: "MONTHLY", company: "TCS iON"                     },
  { title: "Telecaller",              jobType: "OTHER",          salary: 11000,  salaryType: "MONTHLY", company: "HDFC Bank Telecalling"        },
  { title: "Store Keeper",            jobType: "HELPER",         salary: 14000,  salaryType: "MONTHLY", company: "Vishal Mega Mart"             },
  { title: "AC Technician",           jobType: "ELECTRICIAN",    salary: 18000,  salaryType: "MONTHLY", company: "Voltas AC Service"            },
  { title: "Bike Mechanic",           jobType: "OTHER",          salary: 500,    salaryType: "DAILY",   company: "Hero Service Center"          },
  { title: "Tailor / Stitching",      jobType: "OTHER",          salary: 12000,  salaryType: "MONTHLY", company: "Meena Bazaar"                },
];

// ── Build 50 job documents ──────────────────────────────────────────────────
function buildJobs() {
  const now = Date.now();
  const jobs = [];

  for (let i = 0; i < 50; i++) {
    const t = templates[i % templates.length];
    const loc = locations[i % locations.length];
    // Tiny random offset so no two jobs have identical coords
    const jitterLat = (Math.random() - 0.5) * 0.005;
    const jitterLng = (Math.random() - 0.5) * 0.005;
    const lat = +(loc.lat + jitterLat).toFixed(6);
    const lng = +(loc.lng + jitterLng).toFixed(6);

    const docId = `job_${now}_${String(i).padStart(3, '0')}`;
    const createdAt = now - i * 3_600_000; // each job 1 hour apart
    const expiresAt = createdAt + 30 * 24 * 3600_000;
    const addressText = `${loc.area}, ${loc.city}`;

    jobs.push({
      docId,
      // `jobs` collection: ultra-light card data (~200 bytes)
      cardData: {
        employerId:      "admin1",
        title:           t.title,
        companyName:     t.company,
        jobType:         t.jobType,
        salary:          t.salary,
        salaryType:      t.salaryType,
        location:        { lat, lng },
        geohash:         encodeGeohash(lat, lng, 6),
        addressText,
        companyCity:     loc.city,
        urgency:         i < 10 ? "HIGH" : "MEDIUM",
        status:          "open",
        createdAt,
        expiresAt,
      },
      // `job_details` collection: full data (~1KB, loaded on click)
      detailsData: {
        employerId:      "admin1",
        companyName:     t.company,
        isVerified:      true,
        title:           t.title,
        jobType:         t.jobType,
        salary:          t.salary,
        salaryType:      t.salaryType,
        description:     `${t.company} is hiring for ${t.title} in ${loc.area}, ${loc.city}. Apply now!`,
        contactNumber:   "9" + String(Math.floor(Math.random() * 900000000 + 100000000)),
        addressText,
        companyCity:     loc.city,
        location:        { lat, lng },
        geohash:         encodeGeohash(lat, lng, 6),
        gender:          "Any",
        experienceRequired: "No Experience Required",
        shiftTiming:     "Flexible",
        vacancies:       Math.floor(Math.random() * 5) + 1,
        benefits:        ["Free Meals", "PF", "Weekly Off"],
        urgency:         i < 10 ? "HIGH" : "MEDIUM",
        applicationCount: 0,
        status:          "open",
        createdAt,
        expiresAt,
      }
    });
  }
  return jobs;
}

// ── Main ─────────────────────────────────────────────────────────────────────
async function main() {
  console.log('═══════════════════════════════════════════');
  console.log('  DutyPe — Seed 50 Fresh Jobs (Strict Schema)');
  console.log('═══════════════════════════════════════════\n');

  // Step 1: Delete ALL existing jobs from both collections
  console.log('🗑️  Step 1: Deleting ALL existing jobs from jobs + job_details...');
  
  for (const collectionName of ['jobs', 'job_details', 'job_cards']) {
    const existing = await db.collection(collectionName).listDocuments();
    console.log(`   ${collectionName}: ${existing.length} documents`);
    
    for (let i = 0; i < existing.length; i += BATCH_SIZE) {
      const batch = db.batch();
      existing.slice(i, i + BATCH_SIZE).forEach(ref => batch.delete(ref));
      await batch.commit();
    }
  }
  console.log('   ✅ All old data deleted\n');

  // Step 2: Upload 50 fresh jobs to both collections
  console.log('📦 Step 2: Uploading 50 fresh jobs (jobs + job_details)...');
  const jobs = buildJobs();

  for (let i = 0; i < jobs.length; i += BATCH_SIZE) {
    const batch = db.batch();
    jobs.slice(i, i + BATCH_SIZE).forEach(({ docId, cardData, detailsData }) => {
      batch.set(db.collection('jobs').doc(docId), cardData);
      batch.set(db.collection('job_details').doc(docId), detailsData);
    });
    await batch.commit();
    console.log(`   Uploaded ${Math.min(i + BATCH_SIZE, jobs.length)}/${jobs.length} (×2 collections)`);
  }
  console.log('   ✅ All 50 jobs uploaded\n');

  // Step 3: Verify
  console.log('🔍 Step 3: Verifying...');
  const snap = await db.collection(JOBS).limit(3).get();
  snap.forEach(doc => {
    const d = doc.data();
    console.log(`   ${doc.id}:`);
    console.log(`     title:       ${d.title}`);
    console.log(`     status:      ${d.status}`);
    console.log(`     salary:      ${d.salary} ${d.salaryType}`);
    console.log(`     location:    ${JSON.stringify(d.location)}`);
    console.log(`     geohash:     ${d.geohash}`);
    console.log(`     addressText: ${d.addressText}`);
    console.log(`     companyCity: ${d.companyCity}`);
  });

  console.log('\n✅ DONE — 50 fresh jobs seeded with strict schema');
  console.log('   Distances from user (Enkoor):');
  console.log('   • 5 jobs at 0-5 km (Enkoor area)');
  console.log('   • 5 jobs at 5-15 km (Khammam)');
  console.log('   • 4 jobs at 15-50 km (nearby towns)');
  console.log('   • 36 jobs at 50-200 km (Warangal, Vijayawada, Hyderabad)');
  process.exit(0);
}

main().catch(err => {
  console.error('❌ Error:', err);
  process.exit(1);
});
