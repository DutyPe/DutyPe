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
      if (out.size >= 40) return Array.from(out);
    }
  }
  return Array.from(out);
}

const BATCH_2_JOBS = [
  {
    title: "Automobile Water Wash & Vacuum Cleaner",
    companyName: "Sri Anjaneya Water Wash",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 12000,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Mamillagudem, Khammam",
    lat: 17.2532,
    lng: 80.1455,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9848129384",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Car high pressure washing, interior vacuum cleaning, and glass wiping. Fast paced shop with daily tip sharing."
  },
  {
    title: "Bakery Sales Girl & Pastry Counter Staff",
    companyName: "Daily Fresh Bakery",
    category: "SALES",
    employmentType: "FULL_TIME",
    payAmount: 13000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Prakash Nagar, Khammam",
    lat: 17.2465,
    lng: 80.1543,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9866129385",
    gender: "FEMALE",
    experienceRequired: "Fresher",
    educationRequired: "10th / Inter",
    description: "Counter sales of birthday cakes, pastries, biscuits, and puffs. Customer service and neat packaging."
  },
  {
    title: "Gold Appraiser & Loan Verification Assistant",
    companyName: "Manappuram Finance Khammam",
    category: "FINANCE",
    employmentType: "FULL_TIME",
    payAmount: 16500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2514,
    lng: 80.1579,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9948129386",
    gender: "ANY",
    experienceRequired: "Fresher / 1 Yr",
    educationRequired: "Intermediate / Degree",
    description: "Gold testing on touchstone, weighing verification, pledge documentation, and customer KYC verification."
  },
  {
    title: "General Grocery Delivery Boy",
    companyName: "Spencers Retail Store",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 17000,
    payType: "MONTHLY",
    vacancies: 5,
    shift: "DAY",
    area: "Gandhi Chowk, Khammam",
    lat: 17.2483,
    lng: 80.1493,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9177129387",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Doorstep delivery of retail grocery packages to homes in Khammam. Must have two wheeler with license."
  },
  {
    title: "Commercial Building Watchman & Gatekeeper",
    companyName: "Sri Venkateswara Commercial Towers",
    category: "SECURITY",
    employmentType: "FULL_TIME",
    payAmount: 11500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Trunk Road, Khammam",
    lat: 17.2416,
    lng: 80.1481,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9440129388",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Checking vehicles at building entrance, operating parking gate, maintaining visitor logbook."
  },
  {
    title: "Rice Mill Bag Stitching & Paddy Unloading Worker",
    companyName: "Laxmi Rice Mill & Agro Traders",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 4,
    shift: "DAY",
    area: "Industrial Area, Khammam",
    lat: 17.2638,
    lng: 80.1745,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9959129389",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Operating portable bag mouth stitcher for 26kg/50kg rice bags, truck unloading, and godown stacking."
  },
  {
    title: "Opticals Frame Sales & Lens Fitting Assistant",
    companyName: "Lenskart Store Khammam",
    category: "SALES",
    employmentType: "FULL_TIME",
    payAmount: 15500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2518,
    lng: 80.1582,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9701129390",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Intermediate / Degree",
    description: "Spectacle frame selection advice for customers, lens power verification, and order packaging."
  },
  {
    title: "Two-Wheeler Battery Charging & Dynamo Repair Boy",
    companyName: "Sri Rama Battery Works",
    category: "TECHNICIAN",
    employmentType: "FULL_TIME",
    payAmount: 13500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Bypass Road, Khammam",
    lat: 17.2573,
    lng: 80.1545,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9848230491",
    gender: "MALE",
    experienceRequired: "Fresher to 1 Yr",
    educationRequired: "Any",
    description: "Two-wheeler battery testing, distilled water topping, bench charging, and starter motor checking."
  },
  {
    title: "Saree Showroom Helper & Parcel Packer",
    companyName: "Chandana Brothers Shopping Mall",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 11500,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Gandhi Chowk, Khammam",
    lat: 17.2486,
    lng: 80.1494,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9866230492",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Folding silk and pattu sarees, placing in gift boxes, tagging price labels, and assisting showroom sales staff."
  },
  {
    title: "Home Care Patient Attender & Night Caretaker",
    companyName: "DutyPe Care Khammam",
    category: "CARETAKER",
    employmentType: "FULL_TIME",
    payAmount: 15000,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "NIGHT",
    area: "Danavaigudem, Khammam",
    lat: 17.2543,
    lng: 80.1655,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9948230493",
    gender: "ANY",
    experienceRequired: "Patient Care / Home Nursing",
    educationRequired: "Any",
    description: "Night time elder care, assisting with medication timings, mobility assistance, and bedside companionship."
  },
  {
    title: "Fast Food Biryani Serving & Tiffin Boy",
    companyName: "Paradise Biryani & Grill Hub",
    category: "WAITER",
    employmentType: "FULL_TIME",
    payAmount: 13500,
    payType: "MONTHLY",
    vacancies: 4,
    shift: "DAY",
    area: "Mayuri Centre, Khammam",
    lat: 17.2459,
    lng: 80.1475,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9177230494",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Serving authentic dum biryani orders, parcel packing for Swiggy/Zomato, and dining floor service."
  },
  {
    title: "Timber Depot Wood Stacking & Unloading Labour",
    companyName: "Balaji Saw Mill & Timber",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 4,
    shift: "DAY",
    area: "Yellandu Road, Khammam",
    lat: 17.2591,
    lng: 80.1615,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9440230495",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Unloading timber wood logs, plank stacking, sawdust packing, and carpentry orders loading."
  },
  {
    title: "Auto Spare Parts Delivery Boy",
    companyName: "Royal Auto Spares",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 14500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Kaman Bazar, Khammam",
    lat: 17.2476,
    lng: 80.1436,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9959230496",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Delivering clutch plates, cables, and bearings to mechanics around Khammam town."
  },
  {
    title: "Plastic Pipes & Water Tank Retail Salesman",
    companyName: "Sudhakar Pipes Dealer",
    category: "SALES",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Rotary Nagar, Khammam",
    lat: 17.2428,
    lng: 80.1536,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9701230497",
    gender: "MALE",
    experienceRequired: "Fresher to 1 Yr",
    educationRequired: "10th / Any",
    description: "Handling customer orders for PVC agricultural pipes, drip lines, water tanks, and fittings."
  },
  {
    title: "Hotel Kitchen Vegetable Cutter & Utensil Cleaner",
    companyName: "Dwaraka Grand Hotel",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 11000,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Station Road, Khammam",
    lat: 17.2443,
    lng: 80.1491,
    districtId: 509,
    district: "Khammam",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9848341502",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Vegetable peeling and chopping for morning buffet, washing kitchen cooking pots. Meals provided."
  },

  // 15 More Telangana & Andhra Pradesh Openings
  {
    title: "Apparel Mall Security Supervisor",
    companyName: "Inorbit Mall Security Operations",
    category: "SECURITY",
    employmentType: "FULL_TIME",
    payAmount: 18500,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Madhapur, Hyderabad",
    lat: 17.4352,
    lng: 78.3812,
    districtId: 508,
    district: "Hyderabad",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9866341503",
    gender: "MALE",
    experienceRequired: "1 - 2 Years",
    educationRequired: "10th / Intermediate",
    description: "Supervising security guards, metal detector door checks, parking flow management."
  },
  {
    title: "Quick Commerce Bike Delivery Executive",
    companyName: "Blinkit Fast Delivery",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 19000,
    payType: "MONTHLY",
    vacancies: 10,
    shift: "FLEXIBLE",
    area: "Ameerpet, Hyderabad",
    lat: 17.4379,
    lng: 78.4485,
    districtId: 508,
    district: "Hyderabad",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9948341504",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "10-minute grocery delivery rider in Ameerpet and SR Nagar. High per-order incentives and insurance."
  },
  {
    title: "Electronics Sales Promoter - Laptops",
    companyName: "Reliance Digital Megastore",
    category: "SALES",
    employmentType: "FULL_TIME",
    payAmount: 18000,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Kukatpally, Hyderabad",
    lat: 17.4935,
    lng: 78.4021,
    districtId: 508,
    district: "Hyderabad",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9177341505",
    gender: "ANY",
    experienceRequired: "1 Year",
    educationRequired: "Intermediate / Degree",
    description: "Showcasing HP, Dell, and Lenovo laptops to customers. Demonstrating specs and processing invoices."
  },
  {
    title: "Hotel Receptionist - Morning Shift",
    companyName: "Minerva Grand Hotel",
    category: "RECEPTIONIST",
    employmentType: "FULL_TIME",
    payAmount: 18500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Secunderabad Station, Hyderabad",
    lat: 17.4402,
    lng: 78.5021,
    districtId: 508,
    district: "Hyderabad",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9440341506",
    gender: "ANY",
    experienceRequired: "1 Year",
    educationRequired: "Graduate",
    description: "Handling hotel room check-in and check-out, key card programming, guest greetings, and billing."
  },
  {
    title: "Warehouse Barcode Tagging Operator",
    companyName: "Amazon Fulfillment Center",
    category: "PACKER",
    employmentType: "FULL_TIME",
    payAmount: 17500,
    payType: "MONTHLY",
    vacancies: 8,
    shift: "DAY",
    area: "Shamshabad, Hyderabad",
    lat: 17.2405,
    lng: 78.4298,
    districtId: 508,
    district: "Hyderabad",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9959341507",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "10th / Inter",
    description: "Scanning packages with handheld scanner, applying shipping barcodes, sorting cartons by pin code."
  },
  {
    title: "Diagnostic Phlebotomist & Sample Collector",
    companyName: "Vijaya Diagnostic Centre",
    category: "HEALTHCARE",
    employmentType: "FULL_TIME",
    payAmount: 16000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Nayeem Nagar, Hanamkonda",
    lat: 18.0135,
    lng: 79.5542,
    districtId: 512,
    district: "Hanumakonda",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9701341508",
    gender: "ANY",
    experienceRequired: "DMLT / MLT",
    educationRequired: "DMLT / BSc MLT",
    description: "Blood and urine sample collection at lab counter and patient homes in Warangal. Vehicle allowance."
  },
  {
    title: "Fertilizer Retail Counter Salesman",
    companyName: "Rythu Seva Kendram",
    category: "SALES",
    employmentType: "FULL_TIME",
    payAmount: 13500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Market Road, Karimnagar",
    lat: 18.4372,
    lng: 79.1301,
    districtId: 510,
    district: "Karimnagar",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9848452613",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "10th / Any",
    description: "Selling urea, DAP, micro-nutrients to farmers, maintaining stock register, and bill generation."
  },
  {
    title: "Automobile Service Advisor Assistant",
    companyName: "Maruti Suzuki Arena",
    category: "OFFICE_STAFF",
    employmentType: "FULL_TIME",
    payAmount: 15000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Khaleelwadi, Nizamabad",
    lat: 18.6732,
    lng: 78.0955,
    districtId: 511,
    district: "Nizamabad",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9866452614",
    gender: "ANY",
    experienceRequired: "Fresher to 1 Yr",
    educationRequired: "Inter / Degree",
    description: "Job card opening on computer system, recording customer complaints, tracking vehicle delivery status."
  },
  {
    title: "Cotton Ginning Bale Strapping Helper",
    companyName: "Cotton Corporation Center",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 5,
    shift: "DAY",
    area: "Miryalaguda, Nalgonda",
    lat: 16.8715,
    lng: 79.5634,
    districtId: 513,
    district: "Nalgonda",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9948452615",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Operating pneumatic bale strapping tool, stencil printing lot numbers, and warehouse stacking."
  },
  {
    title: "Highway Food Court Dosa Master",
    companyName: "7 Food Court Highway Hub",
    category: "COOK",
    employmentType: "FULL_TIME",
    payAmount: 17000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "NH65, Suryapet",
    lat: 17.1432,
    lng: 79.6241,
    districtId: 514,
    district: "Suryapet",
    stateId: 36,
    state: "Telangana",
    contactNumber: "9177452616",
    gender: "MALE",
    experienceRequired: "1 - 2 Years",
    educationRequired: "Any",
    description: "Preparing live tiffins, dosas, vadas, and idlis on high speed commercial tawa for highway travellers. Room provided."
  },
  {
    title: "Chemical & Fertilizer Godown Helper",
    companyName: "Coromandel Fertilisers Limited",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 14500,
    payType: "MONTHLY",
    vacancies: 4,
    shift: "DAY",
    area: "Auto Nagar, Vijayawada",
    lat: 16.5025,
    lng: 80.6738,
    districtId: 515,
    district: "NTR",
    stateId: 28,
    state: "Andhra Pradesh",
    contactNumber: "9440452617",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Bag handling, stacking on pallets, and assisting with dispatch invoices in logistics godown."
  },
  {
    title: "Textile Store Saree Display Assistant",
    companyName: "Kalanikethan Silks Guntur",
    category: "SALES",
    employmentType: "FULL_TIME",
    payAmount: 13500,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Brodipet, Guntur",
    lat: 16.3072,
    lng: 80.4371,
    districtId: 516,
    district: "Guntur",
    stateId: 28,
    state: "Andhra Pradesh",
    contactNumber: "9959452618",
    gender: "FEMALE",
    experienceRequired: "Fresher",
    educationRequired: "10th / Inter",
    description: "Displaying silk sarees to wedding shoppers, maintaining aesthetic showroom presentation."
  },
  {
    title: "Marine Fish Processing & Cold Storage Helper",
    companyName: "Coastal Fisheries Export Ltd",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 15500,
    payType: "MONTHLY",
    vacancies: 6,
    shift: "DAY",
    area: "Fishing Harbour, Visakhapatnam",
    lat: 17.6945,
    lng: 83.2975,
    districtId: 517,
    district: "Visakhapatnam",
    stateId: 28,
    state: "Andhra Pradesh",
    contactNumber: "9701452619",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Sorting fish and prawns by grade, ice packing in thermocol boxes, and cold storage room stacking."
  },
  {
    title: "Temple Prasadam Packaging & Counter Worker",
    companyName: "Sri Venkateswara Prasadam Canteen",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 13000,
    payType: "MONTHLY",
    vacancies: 4,
    shift: "DAY",
    area: "Alipiri Gate, Tirupati",
    lat: 13.6395,
    lng: 79.4085,
    districtId: 518,
    district: "Tirupati",
    stateId: 28,
    state: "Andhra Pradesh",
    contactNumber: "9848563724",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Packing prasadam packets, managing counter distribution, and keeping counter neat and clean."
  },
  {
    title: "Automobile Body Painting Assistant",
    companyName: "Bosch Car Service Vijayawada",
    category: "PAINTER",
    employmentType: "FULL_TIME",
    payAmount: 17000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "MG Road, Vijayawada",
    lat: 16.5052,
    lng: 80.6495,
    districtId: 515,
    district: "NTR",
    stateId: 28,
    state: "Andhra Pradesh",
    contactNumber: "9866563725",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Body putty sanding, masking paper taping, primer spraying, and buffing polish for cars."
  }
];

async function seedBatch2() {
  console.log(`Starting insertion of ${BATCH_2_JOBS.length} additional fresh jobs...`);
  const now = Date.now();
  const JOB_LIFETIME_MS = 30 * 24 * 60 * 60 * 1000;

  let count = 0;
  const CHUNK_SIZE = 20;

  for (let i = 0; i < BATCH_2_JOBS.length; i += CHUNK_SIZE) {
    const chunk = BATCH_2_JOBS.slice(i, i + CHUNK_SIZE);
    const batch = db.batch();

    for (let j = 0; j < chunk.length; j++) {
      const item = chunk[j];
      const citySlug = (item.district || "khm").toLowerCase().replace(/[^a-z0-9]/g, "");
      const catSlug = (item.category || "job").toLowerCase().replace(/[^a-z0-9]/g, "");
      const jobId = `oct2_${citySlug}_${catSlug}_${Date.now().toString(36)}_${i + j}_${Math.floor(1000 + Math.random() * 9000)}`;

      const lat = item.lat;
      const lng = item.lng;
      const geohash = encodeGeohash(lat, lng, 9);
      const cell = encodeGeohash(lat, lng, 5);
      const keywords = keywordsOf(item.title, item.companyName, item.area, item.district, item.state, item.category.replace(/_/g, " "));

      // Jitter timestamp within Oct 12-14 (last 24-36h)
      const hoursAgo = ((count * 5) % 30) + 3;
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
        districtId: item.districtId,
        district: item.district,
        stateId: item.stateId,
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
    console.log(`Committed chunk up to ${count}/${BATCH_2_JOBS.length}`);
  }

  console.log(`\n Successfully posted ${count} more fresh jobs!`);
}

seedBatch2()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error("Failed to seed batch 2:", e);
    process.exit(1);
  });
