import admin from "firebase-admin";
import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const saPath = resolve(__dirname, "../serviceAccountKey.json.json");
const sa = JSON.parse(readFileSync(saPath, "utf-8"));

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(sa),
    projectId: "dutype-860ac"
  });
}

const db = admin.firestore();
const { Timestamp } = admin.firestore;

const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
function encodeGeohash(lat, lng, precision = 9) {
  let latMin = -90, latMax = 90;
  let lngMin = -180, lngMax = 180;
  let hash = "";
  let bit = 0, ch = 0, even = true;
  while (hash.length < precision) {
    if (even) {
      const mid = (lngMin + lngMax) / 2;
      if (lng >= mid) { ch = (ch << 1) | 1; lngMin = mid; } else { ch = ch << 1; lngMax = mid; }
    } else {
      const mid = (latMin + latMax) / 2;
      if (lat >= mid) { ch = (ch << 1) | 1; latMin = mid; } else { lat = lat << 1; latMax = mid; }
    }
    even = !even;
    if (++bit === 5) { hash += BASE32[ch]; bit = 0; ch = 0; }
  }
  return hash;
}

const SPLIT = /[^\p{L}\p{M}\p{N}]+/u;
function keywordsOf(...parts) {
  const out = new Set();
  for (const part of parts) {
    for (const word of String(part ?? "").normalize("NFC").toLowerCase().split(SPLIT)) {
      if (word.length >= 2) out.add(word);
      if (out.size >= 45) return Array.from(out);
    }
  }
  return Array.from(out);
}

const FRESH_JOBS = [
  // =========================================================================
  // GURUGRAM / SECTOR 91 / KANKROLA / IMT MANESAR (Plus Code: 9WR9+6CR)
  // =========================================================================
  {
    title: "Residential Society Security Guard & Gatekeeper",
    companyName: "Garud Facility & Security Services",
    category: "SECURITY_GUARD",
    employmentType: "FULL_TIME",
    payAmount: 18500,
    payType: "MONTHLY",
    vacancies: 6,
    shift: "ROTATIONAL",
    area: "Sector 91, Kankrola, Gurugram",
    lat: 28.3915,
    lng: 76.9250,
    district: "Gurugram",
    state: "Haryana",
    contactNumber: "9818451290",
    gender: "MALE",
    experienceRequired: "0-2 Years",
    educationRequired: "10th Pass",
    description: "Urgent hiring for Security Guards and Gatekeepers for premium residential high-rise society in Sector 91 (near Kankrola / New Gurugram). Responsibilities include visitor entry logging on MyGate / NoBrokerHood app, boom barrier management, and perimeter patrolling. 12-hour shifts (Day/Night rotational). Uniform, ESI, PF, and room accommodation provided for outstation candidates."
  },
  {
    title: "Night Watchman / Night Security Guard",
    companyName: "Apex Industrial Security Solutions",
    category: "SECURITY_GUARD",
    employmentType: "FULL_TIME",
    payAmount: 17000,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "NIGHT",
    area: "IMT Manesar Sector 8, Gurugram",
    lat: 28.3680,
    lng: 76.9180,
    district: "Gurugram",
    state: "Haryana",
    contactNumber: "9873124567",
    gender: "MALE",
    experienceRequired: "1+ Year",
    educationRequired: "8th / 10th Pass",
    description: "Night security watchman required for warehouse and manufacturing plant in IMT Manesar (close to Sector 91). Fixed night shift from 8:00 PM to 8:00 AM. Duties include gate locking, CCTV check monitoring, and truck dispatch verification. Tea and resting room provided. Immediate joining."
  },
  {
    title: "Commercial Warehouse Supervisor & Security Officer",
    companyName: "KMS Secure Group Haryana",
    category: "SECURITY_GUARD",
    employmentType: "FULL_TIME",
    payAmount: 22000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Sector 92 / Kankrola Road, Gurugram",
    lat: 28.3980,
    lng: 76.9320,
    district: "Gurugram",
    state: "Haryana",
    contactNumber: "9910543219",
    gender: "MALE",
    experienceRequired: "2-4 Years",
    educationRequired: "12th Pass / Ex-Serviceman preferred",
    description: "Security Field Supervisor required to oversee 15 security guards across commercial warehouses in Sector 91-92 corridor. Must handle shift muster rolls, discipline, emergency response, and client coordination. Bike conveyance allowance provided."
  },
  {
    title: "Grocery & Packet Delivery Executive",
    companyName: "Blinkit & Quick Commerce Hub",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 800,
    payType: "DAILY",
    vacancies: 8,
    shift: "FLEXIBLE",
    area: "Sector 90 / 91 Commercial Hub, Gurugram",
    lat: 28.3940,
    lng: 76.9290,
    district: "Gurugram",
    state: "Haryana",
    contactNumber: "9650123498",
    gender: "ANY",
    experienceRequired: "Fresher / Experienced",
    educationRequired: "Any",
    description: "Immediate delivery partners needed for dark store grocery delivery covering Sector 91, 92, and Kankrola. Earn daily payout ₹800 - ₹1,200 per day (orders + distance allowance + peak surge). Must have two-wheeler (bike/scooter) and smartphone."
  },
  {
    title: "Warehouse Helper & Material Loader",
    companyName: "Manesar Logistics Hub",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 15500,
    payType: "MONTHLY",
    vacancies: 5,
    shift: "DAY",
    area: "IMT Manesar Phase 1, Gurugram",
    lat: 28.3620,
    lng: 76.9350,
    district: "Gurugram",
    state: "Haryana",
    contactNumber: "9811237890",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Warehouse assistants needed for parcel sorting, packing, barcode scanning, and truck loading/unloading in IMT Manesar. 9:00 AM - 6:00 PM shift. Overtime ₹60/hour extra. Direct company hiring, no agency charge."
  },
  {
    title: "Office Boy & Pantry Boy",
    companyName: "TechSpace Business Park Offices",
    category: "OFFICE_BOY",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Sector 89 / 91 Main Road, Gurugram",
    lat: 28.3970,
    lng: 76.9360,
    district: "Gurugram",
    state: "Haryana",
    contactNumber: "9717894561",
    gender: "MALE",
    experienceRequired: "Fresher / 1 Year",
    educationRequired: "10th Pass",
    description: "Office boy needed for IT office in New Gurugram (near Sector 91). Duties: tea/coffee service to clients, dusting desks, photocopying documents, and basic courier handling. 9:30 AM to 6:30 PM, Sunday fixed off."
  },

  // =========================================================================
  // KHAMMAM (Telangana)
  // =========================================================================
  {
    title: "Apartment Security Guard & Watchman",
    companyName: "Sri Balaji Security Services Khammam",
    category: "SECURITY_GUARD",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 4,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2472,
    lng: 80.1514,
    district: "Khammam",
    state: "Telangana",
    contactNumber: "9848123890",
    gender: "MALE",
    experienceRequired: "Fresher / 1 Year",
    educationRequired: "Any",
    description: "Gated apartment watchman required along Wyra Road, Khammam. Responsibilities: gate opening, visitor registration, parking monitoring, and motor pump operation. Day shift (8 AM - 8 PM). Room and free electricity provided at the building premises."
  },
  {
    title: "Two-Wheeler Delivery Rider (Food & Groceries)",
    companyName: "Khammam Express Logistics",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 600,
    payType: "DAILY",
    vacancies: 6,
    shift: "FLEXIBLE",
    area: "Khanapuram Haveli, Khammam",
    lat: 17.2580,
    lng: 80.1650,
    district: "Khammam",
    state: "Telangana",
    contactNumber: "9440567812",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Immediate opening for delivery boys in Khammam town covering Khanapuram, Mayuri Center, and Mamillagudem. Daily earnings ₹600 - ₹900 with petrol reimbursement. Must have own bike and smartphone."
  },
  {
    title: "Supermarket Sales & Stock Helper",
    companyName: "Sri Sai Supermarket & Mart",
    category: "CASHIER",
    employmentType: "FULL_TIME",
    payAmount: 13500,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Mayuri Center, Khammam",
    lat: 17.2510,
    lng: 80.1490,
    district: "Khammam",
    state: "Telangana",
    contactNumber: "9989012345",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "10th Pass",
    description: "Sales assistant and rack stocker needed for retail supermarket at Mayuri Center, Khammam. Tasks include customer billing, product shelving, and grocery packaging. Timings: 9:00 AM - 9:00 PM with lunch break."
  },
  {
    title: "South Indian Cook & Tiffin Master",
    companyName: "Swagath Tiffins & Mess",
    category: "COOK",
    employmentType: "FULL_TIME",
    payAmount: 20000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Old Bus Stand, Khammam",
    lat: 17.2430,
    lng: 80.1420,
    district: "Khammam",
    state: "Telangana",
    contactNumber: "9121987654",
    gender: "MALE",
    experienceRequired: "2+ Years",
    educationRequired: "Any",
    description: "Experienced tiffin master required for morning tiffins (idli, dosa, puri, upma, vada). Must maintain clean kitchen and high speed during morning breakfast rush. Room and meals provided free."
  },

  // =========================================================================
  // HYDERABAD (Telangana)
  // =========================================================================
  {
    title: "Commercial & Tech Park Security Guard",
    companyName: "Securitas India Facility Services",
    category: "SECURITY_GUARD",
    employmentType: "FULL_TIME",
    payAmount: 19500,
    payType: "MONTHLY",
    vacancies: 8,
    shift: "ROTATIONAL",
    area: "Hitec City / Madhapur, Hyderabad",
    lat: 17.4485,
    lng: 78.3770,
    district: "Hyderabad",
    state: "Telangana",
    contactNumber: "9963456789",
    gender: "MALE",
    experienceRequired: "0-3 Years",
    educationRequired: "10th Pass",
    description: "Security personnel needed for prestigious corporate IT campus in Hitec City. 8-hour and 12-hour shifts available. Smart uniform, ESIC, PF, and annual bonus included. Clean background and basic Hindi/Telugu or English required."
  },
  {
    title: "Gated Villa Security Watchman",
    companyName: "Cyber Security & Housekeeping",
    category: "SECURITY_GUARD",
    employmentType: "FULL_TIME",
    payAmount: 16500,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "NIGHT",
    area: "Kondapur, Hyderabad",
    lat: 17.4680,
    lng: 78.3580,
    district: "Hyderabad",
    state: "Telangana",
    contactNumber: "9849012378",
    gender: "MALE",
    experienceRequired: "Fresher / 1 Year",
    educationRequired: "Any",
    description: "Night security guard required for gated luxury villa community in Kondapur. Duties: main gate monitoring, CCTV observation, and hourly night clocking. Quiet environment, uniform and tea provided."
  },
  {
    title: "Office Boy & Administrative Peon",
    companyName: "NexGen Financial Advisory",
    category: "OFFICE_BOY",
    employmentType: "FULL_TIME",
    payAmount: 15000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Gachibowli, Hyderabad",
    lat: 17.4400,
    lng: 78.3480,
    district: "Hyderabad",
    state: "Telangana",
    contactNumber: "9000123456",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "10th Pass",
    description: "Office assistant needed for financial services firm in Gachibowli. Tasks: serving water and beverages to guests, file movement between cabins, stationery stock tracking, and local bank/post office visits. 9:30 AM - 6:30 PM."
  },
  {
    title: "Personal / Family Car Driver",
    companyName: "Private Placement Services",
    category: "DRIVER",
    employmentType: "FULL_TIME",
    payAmount: 22000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Jubilee Hills / Banjara Hills, Hyderabad",
    lat: 17.4320,
    lng: 78.4070,
    district: "Hyderabad",
    state: "Telangana",
    contactNumber: "9848056789",
    gender: "MALE",
    experienceRequired: "3+ Years",
    educationRequired: "10th Pass",
    description: "Experienced family car driver required for automatic and manual luxury vehicles (Innova Crysta, Fortuner). Good knowledge of Hyderabad roads, punctual, non-smoker, clean driving license. Overtime paid extra after 10 hours."
  },
  {
    title: "Quick Commerce Grocery Delivery Partner",
    companyName: "Zepto / Blinkit Fulfillment Hub",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 750,
    payType: "DAILY",
    vacancies: 10,
    shift: "FLEXIBLE",
    area: "Kukatpally / KPHB Colony, Hyderabad",
    lat: 17.4930,
    lng: 78.4010,
    district: "Hyderabad",
    state: "Telangana",
    contactNumber: "9100234567",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Instant delivery partners needed for KPHB & Kukatpally zone. 10-15 minute delivery radius. Earn ₹700 - ₹1,100 daily with weekly incentives, joining bonus, and insurance cover. Bike/EV and phone needed."
  }
];

async function seedJobs() {
  const now = Date.now();
  const JOB_LIFETIME_MS = 14 * 24 * 60 * 60 * 1000; // 14 days active
  console.log(`Starting to seed ${FRESH_JOBS.length} fresh, genuine jobs...`);

  let count = 0;
  const batch = db.batch();

  for (const item of FRESH_JOBS) {
    const jobId = `job_${Date.now()}_${count}_${Math.random().toString(36).substring(2, 7)}`;
    const lat = item.lat;
    const lng = item.lng;
    const geohash = encodeGeohash(lat, lng, 9);
    const cell = encodeGeohash(lat, lng, 5);
    const keywords = keywordsOf(item.title, item.companyName, item.area, item.district, item.state, item.category.replace(/_/g, " "));

    // Jitter timestamp: between 2 and 18 hours ago (freshly posted today / yesterday)
    const hoursAgo = ((count * 2) % 16) + 2;
    const jobCreatedAt = now - (hoursAgo * 60 * 60 * 1000);

    const cardData = {
      employerId: "admin_dutype",
      title: item.title,
      category: item.category,
      employmentType: item.employmentType,
      companyName: item.companyName,
      payAmount: item.payAmount,
      payType: item.payType,
      vacancies: item.vacancies,
      urgency: "NORMAL",
      shift: item.shift,
      area: item.area,
      lat: lat,
      lng: lng,
      geohash: geohash,
      cell: cell,
      district: item.district,
      state: item.state,
      status: "open",
      applicationCount: 0,
      keywords: keywords,
      createdAt: Timestamp.fromMillis(jobCreatedAt),
      expiresAt: Timestamp.fromMillis(jobCreatedAt + JOB_LIFETIME_MS)
    };

    const detailsData = {
      employerId: "admin_dutype",
      description: item.description,
      addressText: `${item.companyName}, ${item.area}`,
      gender: item.gender,
      experienceRequired: item.experienceRequired,
      educationRequired: item.educationRequired
    };

    const contactData = {
      employerId: "admin_dutype",
      contactNumber: item.contactNumber
    };

    batch.set(db.collection("jobmetadata").doc(jobId), cardData);
    batch.set(db.collection("job_details").doc(jobId), detailsData);
    batch.set(db.collection("job_contacts").doc(jobId), contactData);
    batch.set(db.collection("jobs").doc(jobId), cardData);

    count++;
  }

  await batch.commit();
  console.log(`\n Successfully posted all ${count} fresh, genuine jobs into Firestore!`);
  console.log(`- Gurugram (Sector 91 / Kankrola / Manesar): 6 jobs`);
  console.log(`- Khammam: 4 jobs`);
  console.log(`- Hyderabad: 5 jobs`);
}

seedJobs()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error("Failed to seed jobs:", e);
    process.exit(1);
  });
