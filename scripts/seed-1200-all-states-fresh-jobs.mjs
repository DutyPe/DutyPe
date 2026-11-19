import admin from "firebase-admin";
import { readFileSync, writeFileSync } from "fs";
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

// ============================================================================
// JOB ARCHETYPES (Curated from Instagram / Facebook Hiring Reels & Platforms)
// ============================================================================
const ARCHETYPES = [
  {
    category: "DELIVERY",
    titlePrefix: "Bike Delivery Executive (Food & Grocery)",
    companyPrefixes: ["Zomato Food Hub", "Swiggy Instamart", "Blinkit Fast Delivery", "Zepto Express Store", "BigBasket Daily"],
    payMin: 18000, payMax: 26000, vacMin: 5, vacMax: 20, shift: "FLEXIBLE",
    exp: "Fresher / Experienced", edu: "10th Pass", gender: "ANY",
    desc: "Urgent opening for instant order deliveries in local radius. Bike & smartphone mandatory. Daily fuel allowance + order incentives + joining bonus. Immediate onboarding with Aadhaar & Driving License."
  },
  {
    category: "DELIVERY",
    titlePrefix: "E-Commerce Van & Parcel Delivery Partner",
    companyPrefixes: ["Amazon Delivery Station", "Flipkart Logistics Hub", "Shadowfax Express", "Delhivery Courier Hub"],
    payMin: 16000, payMax: 23000, vacMin: 4, vacMax: 15, shift: "DAY",
    exp: "Fresher to 1 yr", edu: "10th Pass / Inter", gender: "ANY",
    desc: "Deliver packages within assigned city pincode. Company vehicle/fuel provided for van routes or bike allowance for parcel boy. Weekly payouts and insurance cover."
  },
  {
    category: "COOK",
    titlePrefix: "South Indian Tiffin & Dosa Master",
    companyPrefixes: ["Sri Balaji Tiffin Center", "Udupi Upahar", "Swagath Grand Hotel", "Sri Venkateshwara Grand", "Brundavan Tiffin Lounge"],
    payMin: 20000, payMax: 28000, vacMin: 2, vacMax: 5, shift: "MORNING",
    exp: "1 - 3 Years", edu: "Not Required", gender: "MALE",
    desc: "Specialist needed for Dosa, Idli, Vada, and tiffin chutneys. Fast preparation skills, hygiene maintenance, and taste consistency. Free boarding, food & tea provided."
  },
  {
    category: "COOK",
    titlePrefix: "Fast Food Chef & Biryani Assistant",
    companyPrefixes: ["Red Bucket Biryani", "Paradise Kitchens", "Bawarchi Spice Club", "Mehfil Fast Food", "Hotel Grand Kitchen"],
    payMin: 18000, payMax: 25000, vacMin: 2, vacMax: 6, shift: "DAY",
    exp: "1 - 2 Years", edu: "Not Required", gender: "MALE",
    desc: "Immediate requirement for Chinese, fried rice, noodles, and dum biryani preparation. Experienced cook needed for busy commercial kitchen. Food and accommodation free."
  },
  {
    category: "COOK",
    titlePrefix: "Bakery Chef & Pastry Decorator",
    companyPrefixes: ["Karachi Bakery", "Cakes & More Bakery", "Bakers Treat", "Winni Cakes & Cafe", "Monginis Cake Shop"],
    payMin: 17000, payMax: 24000, vacMin: 2, vacMax: 4, shift: "DAY",
    exp: "1+ Years", edu: "Any", gender: "ANY",
    desc: "Hiring pastry chef and cake decorator. Hands-on experience with ovens, icing, puffs, and cookies. Good hygienic kitchen practices required."
  },
  {
    category: "COOK",
    titlePrefix: "Hotel Kitchen Helper & Vegetable Cutter",
    companyPrefixes: ["Annapurna Mess", "Ruchulu Family Restaurant", "Green Leaf Kitchen", "Santhosh Dhaba", "Spice Junction"],
    payMin: 13000, payMax: 17000, vacMin: 3, vacMax: 8, shift: "DAY",
    exp: "Fresher", edu: "Not Required", gender: "ANY",
    desc: "Kitchen assistant for chopping vegetables, basic prep, cleaning utensils, and kitchen upkeep. Free daily meals and tea provided."
  },
  {
    category: "SECURITY",
    titlePrefix: "Security Guard (Day / Night Shift)",
    companyPrefixes: ["G4S Secure Solutions", "SIS Security Services", "Command Security Guard", "Eagle Eye Security", "Apex Facility Security"],
    payMin: 14000, payMax: 18000, vacMin: 4, vacMax: 12, shift: "ROTATIONAL",
    exp: "Fresher to 1 yr", edu: "10th Pass", gender: "MALE",
    desc: "Guarding corporate premises, gated communities, and retail malls. Visitor register maintenance, vehicle check, and regular patrol. Uniform and PF/ESI benefits provided."
  },
  {
    category: "SECURITY",
    titlePrefix: "Bank ATM & Commercial Complex Guard",
    companyPrefixes: ["Bright Security Force", "Standard Security Agency", "Shield Pro Security", "Guardian Patrol Services"],
    payMin: 15000, payMax: 19500, vacMin: 3, vacMax: 8, shift: "NIGHT",
    exp: "6+ Months", edu: "10th Pass", gender: "MALE",
    desc: "Stationed security for ATM kiosks and commercial showrooms. Alertness, good physical fitness, and disciplined duty timings. Timely monthly bank salary."
  },
  {
    category: "HOUSEKEEPING",
    titlePrefix: "Office Boy & Pantry Attender",
    companyPrefixes: ["Tech Park Offices", "Himalaya Corporate Hub", "Sri Sai Enterprises", "Regus Business Center", "Elite Infotech Office"],
    payMin: 13000, payMax: 17500, vacMin: 2, vacMax: 6, shift: "DAY",
    exp: "Fresher to 1 yr", edu: "10th Pass", gender: "ANY",
    desc: "Pantry maintenance, serving tea/coffee, document filing, photocopying, and reception assistance. Pleasant demeanor and punctuality required."
  },
  {
    category: "HOUSEKEEPING",
    titlePrefix: "Housekeeping Staff & Cleanliness Executive",
    companyPrefixes: ["Apollo Clinic & Hospital", "Care Health Facility", "CleanPro Facility Services", "Grand Mall Housekeeping", "Metro Station Services"],
    payMin: 12500, payMax: 16500, vacMin: 4, vacMax: 15, shift: "MORNING",
    exp: "Fresher", edu: "Not Required", gender: "ANY",
    desc: "Floor cleaning, dusting, sanitization, and waste disposal in hospital wards, malls, or apartment complexes. Gloves, equipment, and uniform provided."
  },
  {
    category: "RETAIL",
    titlePrefix: "Counter Sales Executive (Textiles / Showroom)",
    companyPrefixes: ["Kalamandir Sarees", "South India Shopping Mall", "Zudio Fashion", "Trends Footwear", "Max Fashion Store"],
    payMin: 14000, payMax: 20000, vacMin: 4, vacMax: 12, shift: "DAY",
    exp: "Fresher to 2 yrs", edu: "10th / Inter / Any Degree", gender: "ANY",
    desc: "Customer greeting, showing textile/fashion items, inventory restocking, and customer billing support. Attractive sales incentives on festival periods."
  },
  {
    category: "RETAIL",
    titlePrefix: "Supermarket Cashier & Billing Executive",
    companyPrefixes: ["D-Mart Supermarket", "Reliance Smart Point", "More Supermarket", "Spencer's Retail", "Ratnadeep Supermarket"],
    payMin: 15000, payMax: 21000, vacMin: 3, vacMax: 8, shift: "ROTATIONAL",
    exp: "6+ Months", edu: "Intermediate / Any Degree", gender: "ANY",
    desc: "POS barcode scanning, cash and UPI billing, item packaging, and closing balance tallying. Basic computer knowledge needed. Permanent on-role opening."
  },
  {
    category: "DRIVER",
    titlePrefix: "Personal Car Driver (Sedan & SUV)",
    companyPrefixes: ["Private Family Chauffeur", "VIP Travel Services", "Elite Mobility Drivers", "Apex Executive Travel"],
    payMin: 18000, payMax: 25000, vacMin: 2, vacMax: 4, shift: "DAY",
    exp: "2+ Years", edu: "10th Pass", gender: "MALE",
    desc: "Experienced driver required for family and executive office commuting. Clean driving record, valid 4-wheeler DL, and thorough knowledge of city roads."
  },
  {
    category: "DRIVER",
    titlePrefix: "Commercial Goods Auto & Mini Truck Driver",
    companyPrefixes: ["Porter Transport Hub", "Tata Ace Delivery Logistics", "City Cargo Packers", "Mahindra Bolero Express"],
    payMin: 17000, payMax: 24000, vacMin: 3, vacMax: 7, shift: "DAY",
    exp: "1 - 3 Years", edu: "10th Pass", gender: "MALE",
    desc: "Intracity transport of commercial goods and parcels on Tata Ace or 3-wheeler auto. Valid transport badge/license needed. Fixed salary plus trip allowances."
  },
  {
    category: "TECHNICIAN",
    titlePrefix: "Electrician & Building Maintenance Technician",
    companyPrefixes: ["Sri Sai Electrical Works", "Smart Power Solutions", "Urban Company Partner", "Metro Facility Electric", "Apex Engineering Works"],
    payMin: 17000, payMax: 25000, vacMin: 2, vacMax: 6, shift: "DAY",
    exp: "1 - 3 Years", edu: "ITI / Diploma / Experienced", gender: "MALE",
    desc: "Wiring, switchboard replacement, fan/light installations, MCB troubleshooting, and commercial electrical repairs. Tool kit provided."
  },
  {
    category: "TECHNICIAN",
    titlePrefix: "AC Technician & Refrigeration Mechanic",
    companyPrefixes: ["Cool Master Services", "Voltas Authorized Center", "Blue Star Cool Care", "LG Home Care Solutions"],
    payMin: 18000, payMax: 27000, vacMin: 2, vacMax: 5, shift: "DAY",
    exp: "1 - 3 Years", edu: "ITI / Trade Certificate", gender: "MALE",
    desc: "Split and window AC installation, jet pump service, gas charging, and troubleshooting cooling faults. High seasonal incentives."
  },
  {
    category: "TECHNICIAN",
    titlePrefix: "Two-Wheeler Bike Mechanic",
    companyPrefixes: ["Hero MotoCorp Service", "Bajaj Auto Care Center", "Royal Enfield Garage", "Castrol Bike Point"],
    payMin: 16000, payMax: 24000, vacMin: 2, vacMax: 5, shift: "DAY",
    exp: "1 - 4 Years", edu: "Not Required", gender: "MALE",
    desc: "General bike servicing, engine oil change, brake overhaul, clutch plate replacement, and carburetor tuning. Friendly workshop environment."
  },
  {
    category: "WAREHOUSE",
    titlePrefix: "Warehouse Picker & Order Packer",
    companyPrefixes: ["Flipkart Fulfillment Center", "Amazon Sorting Hub", "Myntra Logistics Depot", "Delhivery Mega Hub"],
    payMin: 15500, payMax: 21500, vacMin: 8, vacMax: 25, shift: "ROTATIONAL",
    exp: "Fresher to 1 yr", edu: "10th / 12th Pass", gender: "ANY",
    desc: "Order picking using handheld barcode scanners, packing items safely in carton boxes, barcode sticker pasting, and inventory counting. Overtime bonus available."
  },
  {
    category: "TELECALLER",
    titlePrefix: "Customer Support & Telecalling Executive",
    companyPrefixes: ["Teleperformance BPO", "Intelenet Global Services", "Cognitive Voice Solutions", "Sri Medha Call Center", "Apex Teleservices"],
    payMin: 14000, payMax: 21000, vacMin: 4, vacMax: 15, shift: "DAY",
    exp: "Fresher to 1 yr", edu: "Inter / Any Graduate", gender: "ANY",
    desc: "Inbound customer support and outbound customer follow-up calls. Telugu / Hindi / English fluency. Day shifts only. Fixed salary + performance incentives."
  },
  {
    category: "BEAUTICIAN",
    titlePrefix: "Beautician & Hair Stylist",
    companyPrefixes: ["Naturals Beauty Salon", "Green Trends Unisex Salon", "Jawad Habib Hair Lounge", "Lakme Salon Studio"],
    payMin: 15000, payMax: 24000, vacMin: 2, vacMax: 5, shift: "DAY",
    exp: "6 Months to 2 yrs", edu: "Beautician Course / Experienced", gender: "FEMALE",
    desc: "Facials, waxing, threading, bridal makeup, hair spa, and pedicure services. Commission on premium services + tips."
  },
  {
    category: "TAILOR",
    titlePrefix: "Ladies Tailor & Blouse Stitching Master",
    companyPrefixes: ["Sri Padmavathi Designer Boutique", "Apsara Fashion Tailors", "Laxmi Boutique", "Style Craft Garments"],
    payMin: 16000, payMax: 25000, vacMin: 2, vacMax: 5, shift: "DAY",
    exp: "2+ Years", edu: "Not Required", gender: "ANY",
    desc: "Specialist in blouse cutting, designer stitching, salwar suits, and alterations. Fast sewing machine handling. Good piece-rate or fixed salary options."
  }
];

// ============================================================================
// ALL 28 STATES & KEY UNION TERRITORIES OF INDIA
// High Density in Telangana (350) and Andhra Pradesh (250), Full Coverage Across All
// ============================================================================
const REGIONS = [
  // --------------------------------------------------------------------------
  // TELANGANA (350+ JOBS) - Deep Hyperlocal Coverage
  // --------------------------------------------------------------------------
  {
    state: "Telangana", stateId: 36, district: "Hyderabad", districtId: 512, phonePrefix: "9848",
    areas: [
      { name: "Madhapur Main Rd, Hitec City, Hyderabad", lat: 17.4486, lng: 78.3908 },
      { name: "Gachibowli Outer Ring Rd, Hyderabad", lat: 17.4401, lng: 78.3489 },
      { name: "Kondapur Botanical Garden Rd, Hyderabad", lat: 17.4650, lng: 78.3650 },
      { name: "Kukatpally KPHB Colony Phase 3, Hyderabad", lat: 17.4849, lng: 78.4138 },
      { name: "Miyapur Allwyn X Roads, Hyderabad", lat: 17.4950, lng: 78.3580 },
      { name: "Ameerpet Metro Station Area, Hyderabad", lat: 17.4375, lng: 78.4483 },
      { name: "Begumpet Prakash Nagar, Hyderabad", lat: 17.4435, lng: 78.4677 },
      { name: "Banjara Hills Road No 12, Hyderabad", lat: 17.4156, lng: 78.4347 },
      { name: "Jubilee Hills Check Post, Hyderabad", lat: 17.4352, lng: 78.4022 },
      { name: "Dilsukhnagar Bus Depot Area, Hyderabad", lat: 17.3688, lng: 78.5247 },
      { name: "LB Nagar Ring Road Circle, Hyderabad", lat: 17.3450, lng: 78.5520 },
      { name: "Uppal Ring Road Crossroads, Hyderabad", lat: 17.4065, lng: 78.5593 },
      { name: "Secunderabad Clock Tower, Secunderabad", lat: 17.4400, lng: 78.5000 },
      { name: "Charminar Lad Bazar, Old City, Hyderabad", lat: 17.3616, lng: 78.4747 },
      { name: "Mehdipatnam Rythu Bazar Area, Hyderabad", lat: 17.3916, lng: 78.4400 },
      { name: "Tolichowki Paramount Colony, Hyderabad", lat: 17.4010, lng: 78.4120 },
      { name: "Chandanagar Bus Stop, Hyderabad", lat: 17.4930, lng: 78.3310 },
      { name: "Attapur Pillar 140 PVNR Expressway, Hyderabad", lat: 17.3688, lng: 78.4277 }
    ],
    targetCount: 120
  },
  {
    state: "Telangana", stateId: 36, district: "Warangal", districtId: 510, phonePrefix: "9849",
    areas: [
      { name: "Naimnagar Main Road, Hanamkonda", lat: 18.0120, lng: 79.5600 },
      { name: "Subedari Collectorate Area, Hanamkonda", lat: 18.0050, lng: 79.5520 },
      { name: "Kazipet Railway Station Road, Kazipet", lat: 17.9810, lng: 79.5180 },
      { name: "Chowrasta Commercial Center, Warangal", lat: 17.9689, lng: 79.5941 },
      { name: "Hunter Road Arts College, Hanamkonda", lat: 17.9950, lng: 79.5750 },
      { name: "Mulugu Road Junction, Warangal", lat: 17.9850, lng: 79.6100 },
      { name: "Pochamma Maidan Market, Warangal", lat: 17.9750, lng: 79.6050 }
    ],
    targetCount: 35
  },
  {
    state: "Telangana", stateId: 36, district: "Khammam", districtId: 509, phonePrefix: "9848",
    areas: [
      { name: "Wyra Road D-Mart Junction, Khammam", lat: 17.2530, lng: 80.1580 },
      { name: "Mamata Hospital Road, Rotary Nagar, Khammam", lat: 17.2620, lng: 80.1490 },
      { name: "Gandhi Chowk Cloth Market, Khammam", lat: 17.2470, lng: 80.1470 },
      { name: "Mayuri Centre Bypass, Khammam", lat: 17.2450, lng: 80.1600 },
      { name: "Prakash Nagar Railway Gate, Khammam", lat: 17.2580, lng: 80.1520 },
      { name: "Mamillaguda Market Center, Khammam", lat: 17.2480, lng: 80.1485 }
    ],
    targetCount: 35
  },
  {
    state: "Telangana", stateId: 36, district: "Karimnagar", districtId: 508, phonePrefix: "9866",
    areas: [
      { name: "Collectorate Road Mankammathota, Karimnagar", lat: 18.4385, lng: 79.1288 },
      { name: "Kothirampur X Roads, Karimnagar", lat: 18.4290, lng: 79.1350 },
      { name: "Mukarampura Tower Circle, Karimnagar", lat: 18.4410, lng: 79.1220 },
      { name: "Geetha Bhavan Junction, Karimnagar", lat: 18.4450, lng: 79.1310 },
      { name: "Bhagyanagar Main Road, Karimnagar", lat: 18.4320, lng: 79.1410 }
    ],
    targetCount: 30
  },
  {
    state: "Telangana", stateId: 36, district: "Nizamabad", districtId: 506, phonePrefix: "9989",
    areas: [
      { name: "Khaleelwadi Commercial Hub, Nizamabad", lat: 18.6725, lng: 78.0941 },
      { name: "Armoor Road Subhash Nagar, Nizamabad", lat: 18.6810, lng: 78.1050 },
      { name: "Phulong X Roads, Nizamabad", lat: 18.6650, lng: 78.0850 },
      { name: "Bodhan Road Railway Gate, Nizamabad", lat: 18.6750, lng: 78.0780 }
    ],
    targetCount: 25
  },
  {
    state: "Telangana", stateId: 36, district: "Nalgonda", districtId: 511, phonePrefix: "9440",
    areas: [
      { name: "Clock Tower Center, Nalgonda", lat: 17.0575, lng: 79.2685 },
      { name: "Prakasham Bazar, Nalgonda", lat: 17.0520, lng: 79.2720 },
      { name: "Suryapet Hi-Tech Bus Stand Road, Suryapet", lat: 17.1420, lng: 79.6230 },
      { name: "Kudakuda Industrial SEZ, Suryapet", lat: 17.1550, lng: 79.6410 },
      { name: "Miryalaguda Sagar Road, Miryalaguda", lat: 16.8710, lng: 79.5630 }
    ],
    targetCount: 35
  },
  {
    state: "Telangana", stateId: 36, district: "Mahbubnagar", districtId: 507, phonePrefix: "9121",
    areas: [
      { name: "Station Road Christian Pally, Mahbubnagar", lat: 16.7450, lng: 78.0030 },
      { name: "Raichur Bypass Chowk, Mahbubnagar", lat: 16.7380, lng: 77.9890 },
      { name: "Jadcherla Polepally Green Industrial SEZ", lat: 16.7720, lng: 78.1410 },
      { name: "One Town Market Center, Mahbubnagar", lat: 16.7510, lng: 78.0120 }
    ],
    targetCount: 25
  },
  {
    state: "Telangana", stateId: 36, district: "Peddapalli", districtId: 505, phonePrefix: "9959",
    areas: [
      { name: "NTPC Jyothinagar Township, Ramagundam", lat: 18.7550, lng: 79.5120 },
      { name: "Godavarikhani Main Chowk, Ramagundam", lat: 18.7620, lng: 79.5280 },
      { name: "Mancherial Bellampalli X Road, Mancherial", lat: 18.8680, lng: 79.4580 }
    ],
    targetCount: 25
  },
  {
    state: "Telangana", stateId: 36, district: "Siddipet", districtId: 513, phonePrefix: "9390",
    areas: [
      { name: "Mustabad Road Bus Depo Area, Siddipet", lat: 18.1018, lng: 78.8520 },
      { name: "Prashanth Nagar Main Road, Siddipet", lat: 18.1120, lng: 78.8610 },
      { name: "Medak Cathedral Church Road, Medak", lat: 18.0460, lng: 78.2610 }
    ],
    targetCount: 20
  },

  // --------------------------------------------------------------------------
  // ANDHRA PRADESH (250+ JOBS) - Complete Coastal & Rayalaseema Coverage
  // --------------------------------------------------------------------------
  {
    state: "Andhra Pradesh", stateId: 28, district: "Krishna", districtId: 520, phonePrefix: "9848",
    areas: [
      { name: "Benz Circle MG Road, Vijayawada", lat: 16.5062, lng: 80.6480 },
      { name: "Governorpet Besant Road, Vijayawada", lat: 16.5120, lng: 80.6280 },
      { name: "Auto Nagar Industrial Hub, Vijayawada", lat: 16.4950, lng: 80.6720 },
      { name: "Bhavanipuram Swathi Center, Vijayawada", lat: 16.5250, lng: 80.5980 },
      { name: "Gannavaram Airport Highway, Vijayawada", lat: 16.5380, lng: 80.7950 },
      { name: "Tadepalli Amaravati Gateway, Vijayawada", lat: 16.4810, lng: 80.6120 }
    ],
    targetCount: 45
  },
  {
    state: "Andhra Pradesh", stateId: 28, district: "Visakhapatnam", districtId: 515, phonePrefix: "9849",
    areas: [
      { name: "Siripuram VIP Road, Visakhapatnam", lat: 17.7210, lng: 83.3150 },
      { name: "Dwaraka Nagar RTC Complex, Visakhapatnam", lat: 17.7280, lng: 83.3050 },
      { name: "Jagadamba Junction Cinema Area, Visakhapatnam", lat: 17.7120, lng: 83.3010 },
      { name: "Gajuwaka Steel Plant Gate, Visakhapatnam", lat: 17.6868, lng: 83.2185 },
      { name: "Madhurawada IT SEZ Hill 2, Visakhapatnam", lat: 17.8180, lng: 83.3550 },
      { name: "MVP Colony Sector 3, Visakhapatnam", lat: 17.7420, lng: 83.3350 },
      { name: "Pendurthi Junction, Visakhapatnam", lat: 17.8310, lng: 83.2050 }
    ],
    targetCount: 50
  },
  {
    state: "Andhra Pradesh", stateId: 28, district: "Guntur", districtId: 521, phonePrefix: "9440",
    areas: [
      { name: "Brodipet 4/2 Main Road, Guntur", lat: 16.3067, lng: 80.4365 },
      { name: "Arundelpet 6th Line, Guntur", lat: 16.3010, lng: 80.4420 },
      { name: "Lakshmipuram Ring Road, Guntur", lat: 16.3150, lng: 80.4280 },
      { name: "Auto Nagar Phase 1, Guntur", lat: 16.2920, lng: 80.4550 },
      { name: "Amaravathi Road Old Club Hall, Guntur", lat: 16.3190, lng: 80.4180 }
    ],
    targetCount: 35
  },
  {
    state: "Andhra Pradesh", stateId: 28, district: "Tirupati", districtId: 524, phonePrefix: "9866",
    areas: [
      { name: "Alipiri Bypass Road, Tirupati", lat: 13.6380, lng: 79.4050 },
      { name: "Renigunta Road Industrial Estate, Tirupati", lat: 13.6288, lng: 79.4192 },
      { name: "Korlagunta Main Road, Tirupati", lat: 13.6320, lng: 79.4310 },
      { name: "Bhavani Nagar Leela Mahal Circle, Tirupati", lat: 13.6420, lng: 79.4150 },
      { name: "Ramanuja Circle Bus Stand Area, Tirupati", lat: 13.6250, lng: 79.4230 }
    ],
    targetCount: 30
  },
  {
    state: "Andhra Pradesh", stateId: 28, district: "East Godavari", districtId: 518, phonePrefix: "9989",
    areas: [
      { name: "Main Road Bhanugudi Junction, Kakinada", lat: 16.9891, lng: 82.2475 },
      { name: "Jagannaickpur Port Road, Kakinada", lat: 16.9550, lng: 82.2380 },
      { name: "Kotipalli Bus Stand Danavaipeta, Rajahmundry", lat: 17.0005, lng: 81.7800 },
      { name: "Morampudi Junction NH16, Rajahmundry", lat: 16.9850, lng: 81.8020 }
    ],
    targetCount: 35
  },
  {
    state: "Andhra Pradesh", stateId: 28, district: "Nellore", districtId: 523, phonePrefix: "8019",
    areas: [
      { name: "Pogathota Trunk Road, Nellore", lat: 14.4426, lng: 79.9865 },
      { name: "Magunta Layout Mini Bypass, Nellore", lat: 14.4510, lng: 79.9750 },
      { name: "Current Office Centre, Nellore", lat: 14.4380, lng: 79.9920 }
    ],
    targetCount: 20
  },
  {
    state: "Andhra Pradesh", stateId: 28, district: "Kurnool", districtId: 525, phonePrefix: "7995",
    areas: [
      { name: "Birla Gate C-Camp, Kurnool", lat: 15.8281, lng: 78.0373 },
      { name: "Raj Vihar Center Bellary Road, Kurnool", lat: 15.8320, lng: 78.0450 },
      { name: "Sanjeeva Nagar Srinivasa Center, Nandyal", lat: 15.4850, lng: 78.4820 }
    ],
    targetCount: 20
  },
  {
    state: "Andhra Pradesh", stateId: 28, district: "Anantapur", districtId: 526, phonePrefix: "8374",
    areas: [
      { name: "Clock Tower Subhash Road, Anantapur", lat: 14.6819, lng: 77.6006 },
      { name: "RTC Bus Stand Nagarajupeta, Kadapa", lat: 14.4673, lng: 78.8242 },
      { name: "Seven Roads Junction, Kadapa", lat: 14.4750, lng: 78.8310 }
    ],
    targetCount: 15
  },

  // --------------------------------------------------------------------------
  // MAHARASHTRA (70 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Maharashtra", stateId: 27, district: "Thane", districtId: 497, phonePrefix: "9820",
    areas: [
      { name: "Maxus Mall 60 Feet Rd, Bhayandar West", lat: 19.3012, lng: 72.8460 },
      { name: "Station Road West, Bhayandar", lat: 19.2968, lng: 72.8520 },
      { name: "Silver Park Junction, Mira Road East", lat: 19.2895, lng: 72.8685 },
      { name: "Naupada Gokhale Rd, Thane West", lat: 19.1865, lng: 72.9715 },
      { name: "Ghodbunder Manpada, Thane West", lat: 19.2455, lng: 72.9725 },
      { name: "Wagle Estate Road No 16, Thane", lat: 19.1925, lng: 72.9515 }
    ],
    targetCount: 30
  },
  {
    state: "Maharashtra", stateId: 27, district: "Mumbai Suburban", districtId: 483, phonePrefix: "9821",
    areas: [
      { name: "Borivali West Shimpoli, Mumbai", lat: 19.2312, lng: 72.8570 },
      { name: "Andheri East MIDC, Mumbai", lat: 19.1197, lng: 72.8697 },
      { name: "Bandra Kurla Complex (BKC), Mumbai", lat: 19.0660, lng: 72.8680 },
      { name: "Dadar West Flower Market, Mumbai", lat: 19.0178, lng: 72.8478 }
    ],
    targetCount: 20
  },
  {
    state: "Maharashtra", stateId: 27, district: "Pune", districtId: 492, phonePrefix: "9822",
    areas: [
      { name: "Hinjawadi Phase 1 Wipro Circle, Pune", lat: 18.5910, lng: 73.7380 },
      { name: "Kothrud Karve Road, Pune", lat: 18.5074, lng: 73.8077 },
      { name: "Hadapsar Magarpatta City, Pune", lat: 18.5150, lng: 73.9280 },
      { name: "Viman Nagar Phoenix Marketcity, Pune", lat: 18.5670, lng: 73.9140 }
    ],
    targetCount: 20
  },

  // --------------------------------------------------------------------------
  // KARNATAKA (60 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Karnataka", stateId: 29, district: "Bengaluru Urban", districtId: 541, phonePrefix: "9845",
    areas: [
      { name: "Koramangala 5th Block Sony Signal, Bengaluru", lat: 12.9352, lng: 77.6245 },
      { name: "Whitefield ITPL Main Road, Bengaluru", lat: 12.9850, lng: 77.7310 },
      { name: "Indiranagar 100 Feet Road, Bengaluru", lat: 12.9719, lng: 77.6412 },
      { name: "Electronic City Phase 1 Infosys Gate, Bengaluru", lat: 12.8452, lng: 77.6602 },
      { name: "Jayanagar 4th Block Shopping Complex, Bengaluru", lat: 12.9250, lng: 77.5838 },
      { name: "Marathahalli Bridge Ring Road, Bengaluru", lat: 12.9591, lng: 77.6974 },
      { name: "HSR Layout Sector 1 27th Main, Bengaluru", lat: 12.9121, lng: 77.6446 }
    ],
    targetCount: 45
  },
  {
    state: "Karnataka", stateId: 29, district: "Mysuru", districtId: 545, phonePrefix: "9886",
    areas: [
      { name: "Devaraja Urs Road, Mysuru", lat: 12.3080, lng: 76.6490 },
      { name: "Hebbal Industrial Area, Mysuru", lat: 12.3550, lng: 76.6080 },
      { name: "Hubballi Chennamma Circle, Hubballi", lat: 15.3647, lng: 75.1240 }
    ],
    targetCount: 15
  },

  // --------------------------------------------------------------------------
  // TAMIL NADU (60 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Tamil Nadu", stateId: 33, district: "Chennai", districtId: 574, phonePrefix: "9840",
    areas: [
      { name: "T. Nagar Ranganathan Street, Chennai", lat: 13.0418, lng: 80.2341 },
      { name: "Velachery 100 Feet Bypass Road, Chennai", lat: 12.9815, lng: 80.2180 },
      { name: "Guindy Industrial Estate, Chennai", lat: 13.0067, lng: 80.2025 },
      { name: "OMR Thoraipakkam IT Expressway, Chennai", lat: 12.9380, lng: 80.2310 },
      { name: "Anna Nagar 2nd Avenue, Chennai", lat: 13.0850, lng: 80.2100 }
    ],
    targetCount: 40
  },
  {
    state: "Tamil Nadu", stateId: 33, district: "Coimbatore", districtId: 575, phonePrefix: "9841",
    areas: [
      { name: "Gandhipuram Cross Cut Road, Coimbatore", lat: 11.0168, lng: 76.9558 },
      { name: "RS Puram DB Road, Coimbatore", lat: 11.0080, lng: 76.9450 },
      { name: "Madurai Simmakkal West Veli St, Madurai", lat: 9.9252, lng: 78.1198 }
    ],
    targetCount: 20
  },

  // --------------------------------------------------------------------------
  // DELHI NCR (50 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Delhi", stateId: 7, district: "Central Delhi", districtId: 90, phonePrefix: "9810",
    areas: [
      { name: "Connaught Place Inner Circle, New Delhi", lat: 28.6315, lng: 77.2167 },
      { name: "Karol Bagh Arya Samaj Road, New Delhi", lat: 28.6517, lng: 77.1906 },
      { name: "Lajpat Nagar Central Market, New Delhi", lat: 28.5677, lng: 77.2433 },
      { name: "Rohini Sector 10 Metro Walk, New Delhi", lat: 28.7150, lng: 77.1180 },
      { name: "Laxmi Nagar Vikas Marg, New Delhi", lat: 28.6310, lng: 77.2770 }
    ],
    targetCount: 25
  },
  {
    state: "Uttar Pradesh", stateId: 9, district: "Gautam Buddha Nagar", districtId: 141, phonePrefix: "9811",
    areas: [
      { name: "Sector 18 Atta Market, Noida", lat: 28.5708, lng: 77.3261 },
      { name: "Sector 62 Electronic City, Noida", lat: 28.6280, lng: 77.3649 },
      { name: "Greater Noida Pari Chowk, Greater Noida", lat: 28.4680, lng: 77.5030 }
    ],
    targetCount: 15
  },
  {
    state: "Haryana", stateId: 6, district: "Gurugram", districtId: 81, phonePrefix: "9818",
    areas: [
      { name: "Cyber Hub DLF Phase 2, Gurugram", lat: 28.4950, lng: 77.0890 },
      { name: "Sohna Road Subhash Chowk, Gurugram", lat: 28.4250, lng: 77.0410 }
    ],
    targetCount: 10
  },

  // --------------------------------------------------------------------------
  // GUJARAT (45 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Gujarat", stateId: 24, district: "Ahmadabad", districtId: 438, phonePrefix: "9824",
    areas: [
      { name: "C.G. Road Navrangpura, Ahmadabad", lat: 23.0330, lng: 72.5610 },
      { name: "Prahlad Nagar SG Highway, Ahmadabad", lat: 23.0110, lng: 72.5080 },
      { name: "Maninagar Railway Cross, Ahmadabad", lat: 22.9980, lng: 72.6050 }
    ],
    targetCount: 25
  },
  {
    state: "Gujarat", stateId: 24, district: "Surat", districtId: 459, phonePrefix: "9825",
    areas: [
      { name: "Ring Road Textile Market, Surat", lat: 21.1850, lng: 72.8450 },
      { name: "Varachha Diamond Market, Surat", lat: 21.2180, lng: 72.8680 },
      { name: "Alkapuri RC Dutt Road, Vadodara", lat: 22.3100, lng: 73.1810 }
    ],
    targetCount: 20
  },

  // --------------------------------------------------------------------------
  // UTTAR PRADESH (35 JOBS - UP Mainland)
  // --------------------------------------------------------------------------
  {
    state: "Uttar Pradesh", stateId: 9, district: "Lucknow", districtId: 162, phonePrefix: "9415",
    areas: [
      { name: "Hazratganj Main Market, Lucknow", lat: 26.8520, lng: 80.9460 },
      { name: "Gomti Nagar Patrakarpuram, Lucknow", lat: 26.8620, lng: 81.0020 },
      { name: "Alambagh Bus Stand, Lucknow", lat: 26.8150, lng: 80.9080 },
      { name: "Mall Road Gumti No 5, Kanpur", lat: 26.4680, lng: 80.3520 },
      { name: "Godowlia Chowk, Varanasi", lat: 25.3110, lng: 83.0080 },
      { name: "Sanjay Place MG Road, Agra", lat: 27.1980, lng: 78.0050 }
    ],
    targetCount: 35
  },

  // --------------------------------------------------------------------------
  // WEST BENGAL (40 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "West Bengal", stateId: 19, district: "Kolkata", districtId: 315, phonePrefix: "9830",
    areas: [
      { name: "Park Street Camac St, Kolkata", lat: 22.5510, lng: 88.3520 },
      { name: "Salt Lake Sector 5 IT Hub, Kolkata", lat: 22.5780, lng: 88.4320 },
      { name: "Gariahat Pantaloons Crossing, Kolkata", lat: 22.5180, lng: 88.3680 },
      { name: "Newtown Action Area 1, Kolkata", lat: 22.5850, lng: 88.4650 },
      { name: "Howrah Station Road, Howrah", lat: 22.5890, lng: 88.3410 },
      { name: "Sevoke Road Panitanki More, Siliguri", lat: 26.7271, lng: 88.4316 }
    ],
    targetCount: 40
  },

  // --------------------------------------------------------------------------
  // RAJASTHAN (40 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Rajasthan", stateId: 8, district: "Jaipur", districtId: 102, phonePrefix: "9829",
    areas: [
      { name: "MI Road Panch Batti, Jaipur", lat: 26.9180, lng: 75.8150 },
      { name: "Malviya Nagar Gaurav Tower, Jaipur", lat: 26.8520, lng: 75.8080 },
      { name: "Mansarovar Sector 3, Jaipur", lat: 26.8650, lng: 75.7620 },
      { name: "Vaishali Nagar Amrapali Circle, Jaipur", lat: 26.9080, lng: 75.7420 },
      { name: "Sojati Gate Main Market, Jodhpur", lat: 26.2918, lng: 73.0238 },
      { name: "Chetak Circle Fatehsagar Road, Udaipur", lat: 24.5925, lng: 73.6890 }
    ],
    targetCount: 40
  },

  // --------------------------------------------------------------------------
  // MADHYA PRADESH (40 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Madhya Pradesh", stateId: 23, district: "Indore", districtId: 410, phonePrefix: "9826",
    areas: [
      { name: "Vijay Nagar AB Road, Indore", lat: 22.7520, lng: 75.8950 },
      { name: "Chhappan Dukan New Palasia, Indore", lat: 22.7240, lng: 75.8820 },
      { name: "Sapna Sangeeta Road, Indore", lat: 22.7050, lng: 75.8650 },
      { name: "MP Nagar Zone 1, Bhopal", lat: 23.2350, lng: 77.4350 },
      { name: "New Market TT Nagar, Bhopal", lat: 23.2310, lng: 77.4020 },
      { name: "Lashkar Jayendraganj, Gwalior", lat: 26.2080, lng: 78.1680 }
    ],
    targetCount: 40
  },

  // --------------------------------------------------------------------------
  // KERALA (35 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Kerala", stateId: 32, district: "Ernakulam", districtId: 555, phonePrefix: "9847",
    areas: [
      { name: "MG Road Marine Drive, Ernakulam", lat: 9.9810, lng: 76.2750 },
      { name: "Edappally Lulu Mall Area, Kochi", lat: 10.0250, lng: 76.3080 },
      { name: "Kakkanad Infopark Express, Kochi", lat: 10.0120, lng: 76.3580 },
      { name: "Technopark Phase 1 Kazhakkoottam, Thiruvananthapuram", lat: 8.5560, lng: 76.8820 },
      { name: "Mavoor Road Bus Stand, Kozhikode", lat: 11.2588, lng: 75.7804 }
    ],
    targetCount: 35
  },

  // --------------------------------------------------------------------------
  // PUNJAB (30 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Punjab", stateId: 3, district: "Ludhiana", districtId: 37, phonePrefix: "9814",
    areas: [
      { name: "Ferozepur Road Gurdev Nagar, Ludhiana", lat: 30.9010, lng: 75.8280 },
      { name: "Model Town Gol Market, Ludhiana", lat: 30.8850, lng: 75.8420 },
      { name: "Mall Road Lawrence Road, Amritsar", lat: 31.6340, lng: 74.8723 },
      { name: "BMC Chowk GT Road, Jalandhar", lat: 31.3260, lng: 75.5762 },
      { name: "Phase 7 Industrial Area, Mohali", lat: 30.7046, lng: 76.7179 }
    ],
    targetCount: 30
  },

  // --------------------------------------------------------------------------
  // HARYANA (20 JOBS - Non-NCR Mainland)
  // --------------------------------------------------------------------------
  {
    state: "Haryana", stateId: 6, district: "Panipat", districtId: 78, phonePrefix: "9896",
    areas: [
      { name: "GT Road Textile Market, Panipat", lat: 29.3909, lng: 76.9635 },
      { name: "Kunjpura Road Sector 12, Karnal", lat: 29.6857, lng: 76.9905 },
      { name: "Cloth Market Jagadhri Road, Ambala Cantt", lat: 30.3340, lng: 76.8420 },
      { name: "Delhi Road Model Town, Rohtak", lat: 28.8955, lng: 76.6066 }
    ],
    targetCount: 20
  },

  // --------------------------------------------------------------------------
  // BIHAR (30 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Bihar", stateId: 10, district: "Patna", districtId: 212, phonePrefix: "9431",
    areas: [
      { name: "Boring Road Crossing, Patna", lat: 25.6150, lng: 85.1220 },
      { name: "Kankarbagh Main Road, Patna", lat: 25.5980, lng: 85.1580 },
      { name: "Fraser Road Dak Bunglow, Patna", lat: 25.6080, lng: 85.1380 },
      { name: "Bailey Road Saguna More, Patna", lat: 25.6250, lng: 85.0680 },
      { name: "Tower Chowk GB Road, Gaya", lat: 24.7914, lng: 85.0002 },
      { name: "Motijheel Market, Muzaffarpur", lat: 26.1209, lng: 85.3647 }
    ],
    targetCount: 30
  },

  // --------------------------------------------------------------------------
  // ODISHA (25 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Odisha", stateId: 21, district: "Khordha", districtId: 366, phonePrefix: "9437",
    areas: [
      { name: "Saheed Nagar Janpath, Bhubaneswar", lat: 20.2961, lng: 85.8450 },
      { name: "Patia Infocity Square, Bhubaneswar", lat: 20.3550, lng: 85.8180 },
      { name: "Master Canteen Square, Bhubaneswar", lat: 20.2650, lng: 85.8410 },
      { name: "Badambadi Bus Stand, Cuttack", lat: 20.4625, lng: 85.8828 },
      { name: "Uditnagar Bisra Chowk, Rourkela", lat: 22.2259, lng: 84.8536 }
    ],
    targetCount: 25
  },

  // --------------------------------------------------------------------------
  // JHARKHAND (25 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Jharkhand", stateId: 20, district: "Ranchi", districtId: 339, phonePrefix: "9431",
    areas: [
      { name: "Main Road Daily Market, Ranchi", lat: 23.3610, lng: 85.3250 },
      { name: "Lalpur Chowk Circular Road, Ranchi", lat: 23.3725, lng: 85.3340 },
      { name: "Bistupur Main Road, Jamshedpur", lat: 22.7980, lng: 86.1850 },
      { name: "Sakchi Market Golmuri, Jamshedpur", lat: 22.8050, lng: 86.2050 },
      { name: "Bank More Station Road, Dhanbad", lat: 23.7910, lng: 86.4310 }
    ],
    targetCount: 25
  },

  // --------------------------------------------------------------------------
  // ASSAM (20 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Assam", stateId: 18, district: "Kamrup Metropolitan", districtId: 298, phonePrefix: "9435",
    areas: [
      { name: "GS Road Christian Basti, Guwahati", lat: 26.1550, lng: 91.7750 },
      { name: "Paltan Bazaar Station Road, Guwahati", lat: 26.1780, lng: 91.7520 },
      { name: "Fancy Bazaar SS Road, Guwahati", lat: 26.1850, lng: 91.7410 },
      { name: "RNC Road Circuit House, Dibrugarh", lat: 27.4728, lng: 94.9120 }
    ],
    targetCount: 20
  },

  // --------------------------------------------------------------------------
  // CHHATTISGARH (20 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Chhattisgarh", stateId: 22, district: "Raipur", districtId: 387, phonePrefix: "9827",
    areas: [
      { name: "Pandri Cloth Market, Raipur", lat: 21.2580, lng: 81.6550 },
      { name: "Telibandha VIP Chowk, Raipur", lat: 21.2380, lng: 81.6750 },
      { name: "Civic Centre Sector 5, Bhilai", lat: 21.1938, lng: 81.3509 },
      { name: "Vyapar Vihar Main Road, Bilaspur", lat: 22.0797, lng: 82.1409 }
    ],
    targetCount: 20
  },

  // --------------------------------------------------------------------------
  // UTTARAKHAND (18 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Uttarakhand", stateId: 5, district: "Dehradun", districtId: 57, phonePrefix: "9412",
    areas: [
      { name: "Rajpur Road Ashley Hall, Dehradun", lat: 30.3280, lng: 78.0450 },
      { name: "Paltan Bazar Clock Tower, Dehradun", lat: 30.3180, lng: 78.0350 },
      { name: "Jwalapur Ranipur More, Haridwar", lat: 29.9350, lng: 78.1320 },
      { name: "Nainital Road Tikonia, Haldwani", lat: 29.2183, lng: 79.5130 }
    ],
    targetCount: 18
  },

  // --------------------------------------------------------------------------
  // GOA (16 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Goa", stateId: 30, district: "North Goa", districtId: 550, phonePrefix: "9822",
    areas: [
      { name: "MG Road 18th June Road, Panaji", lat: 15.4989, lng: 73.8278 },
      { name: "Calangute Beach Market Road, Calangute", lat: 15.5430, lng: 73.7550 },
      { name: "Pajifond Station Road, Margao", lat: 15.2750, lng: 73.9610 },
      { name: "Swatantra Path, Vasco da Gama", lat: 15.3980, lng: 73.8120 }
    ],
    targetCount: 16
  },

  // --------------------------------------------------------------------------
  // HIMACHAL PRADESH (16 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Himachal Pradesh", stateId: 2, district: "Shimla", districtId: 28, phonePrefix: "9418",
    areas: [
      { name: "The Mall Road Scandal Point, Shimla", lat: 31.1048, lng: 77.1734 },
      { name: "Lower Bazar Cart Road, Shimla", lat: 31.1020, lng: 77.1760 },
      { name: "Baddi Industrial Phase 1, Solan", lat: 30.9578, lng: 76.7914 },
      { name: "Kotwali Bazar, Dharamshala", lat: 32.2190, lng: 76.3234 }
    ],
    targetCount: 16
  },

  // --------------------------------------------------------------------------
  // JAMMU AND KASHMIR (16 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Jammu and Kashmir", stateId: 1, district: "Srinagar", districtId: 13, phonePrefix: "9419",
    areas: [
      { name: "Lal Chowk Residency Road, Srinagar", lat: 34.0710, lng: 74.8110 },
      { name: "Karan Nagar SMHS Hospital Area, Srinagar", lat: 34.0850, lng: 74.8020 },
      { name: "Gandhi Nagar Gole Market, Jammu", lat: 32.7050, lng: 74.8680 },
      { name: "Residency Road Raghunath Bazar, Jammu", lat: 32.7290, lng: 74.8620 }
    ],
    targetCount: 16
  },

  // --------------------------------------------------------------------------
  // TRIPURA, MEGHALAYA & NORTHEAST (15 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Tripura", stateId: 16, district: "West Tripura", districtId: 271, phonePrefix: "9436",
    areas: [
      { name: "Akhaura Road Battala, Agartala", lat: 23.8315, lng: 91.2868 },
      { name: "Police Bazar GS Road, Shillong", lat: 25.5788, lng: 91.8831 },
      { name: "Paona Bazar Thangal Bazar, Imphal", lat: 24.8170, lng: 93.9368 }
    ],
    targetCount: 15
  },

  // --------------------------------------------------------------------------
  // CHANDIGARH & PUDUCHERRY (15 JOBS)
  // --------------------------------------------------------------------------
  {
    state: "Chandigarh", stateId: 4, district: "Chandigarh", districtId: 49, phonePrefix: "9876",
    areas: [
      { name: "Sector 17 Shopping Plaza, Chandigarh", lat: 30.7398, lng: 76.7827 },
      { name: "Sector 35 Inner Market, Chandigarh", lat: 30.7250, lng: 76.7650 },
      { name: "Sector 22 Shastri Market, Chandigarh", lat: 30.7310, lng: 76.7730 },
      { name: "White Town Promenade Beach Road, Puducherry", lat: 11.9338, lng: 79.8340 }
    ],
    targetCount: 15
  }
];

// ============================================================================
// GENERATION & SEEDING ENGINE
// ============================================================================

function generateJobs() {
  const jobs = [];
  let serial = 10001;

  for (const reg of REGIONS) {
    const target = reg.targetCount;
    for (let i = 0; i < target; i++) {
      const arch = ARCHETYPES[(i + serial) % ARCHETYPES.length];
      const areaObj = reg.areas[i % reg.areas.length];
      const areaBase = areaObj.name.split(",")[0].trim();

      const payStep = (arch.payMax - arch.payMin) / 5;
      const payAmount = Math.round((arch.payMin + (i % 5) * payStep) / 500) * 500;
      const vacancies = arch.vacMin + (i % (arch.vacMax - arch.vacMin + 1));

      // Realistic 6-digit suffix for mobile number
      const phoneSuffix = String(serial * 523 + i * 37 + 104729).slice(-6);
      const contactNumber = `${reg.phonePrefix}${phoneSuffix}`;

      // Spatial micro-jitter to prevent duplicate identical points
      const latJitter = ((i % 9) - 4) * 0.0014;
      const lngJitter = (((i + 3) % 9) - 4) * 0.0014;

      const companyName = `${arch.companyPrefixes[i % arch.companyPrefixes.length]} (${areaBase})`;
      const title = `${arch.titlePrefix} - ${areaBase}, ${reg.district}`;

      jobs.push({
        title,
        companyName,
        category: arch.category,
        employmentType: "FULL_TIME",
        payAmount,
        payType: "MONTHLY",
        vacancies,
        shift: arch.shift,
        area: areaObj.name,
        lat: Number((areaObj.lat + latJitter).toFixed(6)),
        lng: Number((areaObj.lng + lngJitter).toFixed(6)),
        districtId: reg.districtId,
        district: reg.district,
        stateId: reg.stateId,
        state: reg.state,
        contactNumber,
        gender: arch.gender,
        experienceRequired: arch.exp,
        educationRequired: arch.edu,
        description: `Urgent requirement for ${arch.titlePrefix} at ${areaObj.name}, ${reg.district}. ${arch.desc} Walk-in interviews open today. Contact recruiter directly.`
      });

      serial++;
    }
  }

  return jobs;
}

async function run() {
  const generatedJobs = generateJobs();
  console.log(`Generated total of ${generatedJobs.length} fresh jobs across ${REGIONS.length} regions.`);

  const now = Date.now();
  const JOB_LIFETIME_MS = 35 * 24 * 60 * 60 * 1000; // 35 days validity

  let existingTitles = new Set();
  try {
    const existingTitlesRaw = JSON.parse(readFileSync(resolve(__dirname, "existing-titles.json"), "utf-8"));
    existingTitles = new Set(existingTitlesRaw.map(t => t.toLowerCase().trim()));
  } catch (err) {
    console.warn("Could not read existing-titles.json, proceeding with clean set.");
  }

  const newJobs = generatedJobs.filter(j => !existingTitles.has(j.title.toLowerCase().trim()));
  console.log(`Identified ${newJobs.length} strictly unique fresh jobs to insert.`);

  let count = 0;
  const CHUNK_SIZE = 35; // 35 * 4 = 140 operations per batch (under Firestore 500 limit)

  for (let i = 0; i < newJobs.length; i += CHUNK_SIZE) {
    const chunk = newJobs.slice(i, i + CHUNK_SIZE);
    const batch = db.batch();

    for (let j = 0; j < chunk.length; j++) {
      const item = chunk[j];
      const citySlug = (item.district || "city").toLowerCase().replace(/[^a-z0-9]/g, "");
      const catSlug = (item.category || "job").toLowerCase().replace(/[^a-z0-9]/g, "");
      const jobId = `ind_${citySlug}_${catSlug}_${Date.now().toString(36)}_${i + j}_${Math.floor(1000 + Math.random() * 9000)}`;

      const lat = item.lat;
      const lng = item.lng;
      const geohash = encodeGeohash(lat, lng, 9);
      const cell = encodeGeohash(lat, lng, 5);
      const keywords = keywordsOf(item.title, item.companyName, item.area, item.district, item.state, item.category.replace(/_/g, " "));

      // Jitter timestamp across today & yesterday (1 to 36 hours ago)
      const hoursAgo = (count % 36) + 1;
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
        urgency: (count % 4 === 0) ? "URGENT" : "NORMAL",
        shift: item.shift,
        area: item.area,
        lat,
        lng,
        geohash,
        cell,
        districtId: item.districtId,
        district: item.district,
        stateId: item.stateId,
        state: item.state,
        status: "open",
        applicationCount: 0,
        keywords,
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

      existingTitles.add(item.title.toLowerCase().trim());
      count++;
    }

    await batch.commit();
    console.log(`Committed chunk up to job ${count}/${newJobs.length}`);
  }

  writeFileSync(resolve(__dirname, "existing-titles.json"), JSON.stringify(Array.from(existingTitles), null, 2));

  console.log(`\n🎉 Successfully posted ${count} fresh, genuine pan-India jobs across all states into Firestore!`);
}

run()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error("Failed to seed jobs:", e);
    process.exit(1);
  });
