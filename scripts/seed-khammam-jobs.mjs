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
      if (lat >= mid) { ch = (ch << 1) | 1; latMin = mid; } else { ch = ch << 1; latMax = mid; }
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

const KHAMMAM_JOBS = [
  // 1. Retail & Stores
  {
    title: "Kirana Store Helper & Stocking",
    companyName: "Sri Balaji Super Bazar",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 11000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Gandhi Chowk, Khammam",
    lat: 17.2485,
    lng: 80.1492,
    contactNumber: "9848123456",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Looking for energetic shop helper for shelf stocking, customer assistance, and grocery pack handling. Working hours: 9 AM to 8 PM with tea break. Lunch provided."
  },
  {
    title: "Cloth Showroom Salesman",
    companyName: "Sri Krishna Silks & Sarees",
    category: "SALES",
    employmentType: "FULL_TIME",
    payAmount: 13500,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Kaman Bazar, Khammam",
    lat: 17.2510,
    lng: 80.1480,
    contactNumber: "9440187654",
    gender: "ANY",
    experienceRequired: "6 Months",
    educationRequired: "10th Pass",
    description: "Retail sales staff required for showing sarees and dress materials to customers. Polite speaking and customer friendly attitude needed. Weekly holiday provided."
  },
  {
    title: "Medical Shop Billing Assistant",
    companyName: "Sanjeevani Pharmacy",
    category: "PHARMACY",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2435,
    lng: 80.1568,
    contactNumber: "9989234567",
    gender: "ANY",
    experienceRequired: "1 Year",
    educationRequired: "Intermediate",
    description: "Medical store assistant needed for medicine dispatch and computer billing. Basic knowledge of medicine names required. Immediate joining."
  },
  {
    title: "Supermarket Cashier & Billing",
    companyName: "Ratnadeep Supermarket",
    category: "CASHIER",
    employmentType: "FULL_TIME",
    payAmount: 14500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Mayuri Centre, Khammam",
    lat: 17.2492,
    lng: 80.1528,
    contactNumber: "9866345678",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "10th Pass",
    description: "Cash counter billing executive needed for barcode scanning and POS handling. Training will be provided on the software. Friendly team environment."
  },
  {
    title: "Mobile Shop Sales Assistant",
    companyName: "Khammam Mobile World",
    category: "SALES",
    employmentType: "FULL_TIME",
    payAmount: 12000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Bus Stand Road, Khammam",
    lat: 17.2455,
    lng: 80.1445,
    contactNumber: "9701456789",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "10th Pass",
    description: "Assistant needed for explaining mobile features, screen guard application, and pouch sales. Sales incentive on top of fixed monthly salary."
  },
  {
    title: "Footwear Store Sales Assistant",
    companyName: "Bata Footwear Store",
    category: "SALES",
    employmentType: "FULL_TIME",
    payAmount: 11500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Trunk Road, Khammam",
    lat: 17.2460,
    lng: 80.1500,
    contactNumber: "9949567890",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Footwear store helper required for customer shoe sizing and shoe box arrangement. Clean and comfortable indoor working environment."
  },
  {
    title: "Hardware & Paints Store Helper",
    companyName: "Lakshmi Hardware & Electricals",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 12000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2435,
    lng: 80.1568,
    contactNumber: "9849678901",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Helper required for handling hardware fittings, electrical pipes, and paint cans. Hardworking and sincere candidates preferred."
  },
  {
    title: "Gift & Stationery Store Assistant",
    companyName: "Archies Gift Planet",
    category: "SALES",
    employmentType: "PART_TIME",
    payAmount: 7000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Rotary Nagar, Khammam",
    lat: 17.2440,
    lng: 80.1470,
    contactNumber: "9490789012",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "10th Pass",
    description: "Evening part-time store assistant needed (3 PM to 9 PM). Helping customers with greeting cards, toys, and gift wrapping. Good for college students."
  },
  {
    title: "Jewelry Showroom Security Guard",
    companyName: "Sri Rama Jewellers",
    category: "SECURITY",
    employmentType: "FULL_TIME",
    payAmount: 13000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Gandhi Chowk, Khammam",
    lat: 17.2485,
    lng: 80.1492,
    contactNumber: "9848890123",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Day shift security guard needed for showroom entrance. Uniform provided. Punctual and disciplined behavior required."
  },
  {
    title: "Rice Mill Loading & Unloading Helper",
    companyName: "Venkateshwara Modern Rice Mill",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 600,
    payType: "DAILY",
    vacancies: 4,
    shift: "DAY",
    area: "Ricemill Road, Khammam",
    lat: 17.2580,
    lng: 80.1600,
    contactNumber: "9177901234",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Daily wage loading workers needed for paddy bag stacking and lorry loading. Daily cash settlement + tea allowance."
  },

  // 2. Food & Hospitality
  {
    title: "Tiffin Master (Dosa & Idli)",
    companyName: "Hotel Swagath Udupi",
    category: "COOK",
    employmentType: "FULL_TIME",
    payAmount: 18000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Bus Stand Road, Khammam",
    lat: 17.2455,
    lng: 80.1445,
    contactNumber: "9959012345",
    gender: "MALE",
    experienceRequired: "2 Years",
    educationRequired: "Any",
    description: "Experienced tiffin master needed for quick breakfast dosa, poori, vada, and chutney preparation. Morning 6 AM to 12 PM & evening 4 PM to 9 PM. Food and room provided."
  },
  {
    title: "Fast Food Chinese Master",
    companyName: "Red Chilli Fast Food Centre",
    category: "COOK",
    employmentType: "FULL_TIME",
    payAmount: 17500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "ANY",
    area: "Mayuri Centre, Khammam",
    lat: 17.2492,
    lng: 80.1528,
    contactNumber: "9848112233",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Fast food cook needed for Fried Rice, Noodles, Manchurian, and Chilli Chicken. Shift: 1 PM to 10:30 PM. Daily incentive on sales."
  },
  {
    title: "Irani Tea Master",
    companyName: "Al-Madina Irani Chai",
    category: "COOK",
    employmentType: "FULL_TIME",
    payAmount: 15000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Kaman Bazar, Khammam",
    lat: 17.2510,
    lng: 80.1480,
    contactNumber: "9440223344",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Experienced tea master needed for Dum Chai, Osmania biscuits, and coffee making. Morning shift. Food and accommodation free."
  },
  {
    title: "Hotel Waiters / Service Boys",
    companyName: "Hotel Mayuri Grand",
    category: "WAITER",
    employmentType: "FULL_TIME",
    payAmount: 11000,
    payType: "MONTHLY",
    vacancies: 4,
    shift: "DAY",
    area: "Mayuri Centre, Khammam",
    lat: 17.2492,
    lng: 80.1528,
    contactNumber: "9989334455",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Friendly service staff required for family dining tables. Order serving and table cleaning. Tips + food + uniform provided."
  },
  {
    title: "Bakery & Cake Making Assistant",
    companyName: "Khammam Sweet House & Bakery",
    category: "COOK",
    employmentType: "FULL_TIME",
    payAmount: 13000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Gandhi Chowk, Khammam",
    lat: 17.2485,
    lng: 80.1492,
    contactNumber: "9866445566",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Assistant needed for cake decorating, puff baking, and sweet box packing. Clean working environment. Free snacks and tea."
  },
  {
    title: "Hotel Kitchen Cleaner & Dishwasher",
    companyName: "Grand Gayathri Hotel",
    category: "CLEANER",
    employmentType: "FULL_TIME",
    payAmount: 10500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "ANY",
    area: "Wyra Road, Khammam",
    lat: 17.2435,
    lng: 80.1568,
    contactNumber: "9701556677",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Vessel washing and kitchen cleaning staff required. Three meals a day provided. Immediate requirement."
  },
  {
    title: "Parotta & Roti Master",
    companyName: "Sri Balaji Mess",
    category: "COOK",
    employmentType: "FULL_TIME",
    payAmount: 16000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "VDO's Colony, Khammam",
    lat: 17.2526,
    lng: 80.1545,
    contactNumber: "9949667788",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Malabar parotta, chapati, and pulka making cook needed for dinner rush. Working hours: 3 PM to 10 PM."
  },
  {
    title: "Juice & Milkshake Maker",
    companyName: "Cool Spot Fruit Juice Bar",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 11000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Trunk Road, Khammam",
    lat: 17.2460,
    lng: 80.1500,
    contactNumber: "9849778899",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Fruit peeling, cutting, and mixer blender operation. Neat, clean and active persons wanted."
  },
  {
    title: "Ice Cream Parlour Counter Staff",
    companyName: "Amul Ice Cream Parlour",
    category: "CASHIER",
    employmentType: "FULL_TIME",
    payAmount: 10000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Rotary Nagar, Khammam",
    lat: 17.2440,
    lng: 80.1470,
    contactNumber: "9490889900",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "10th Pass",
    description: "Scooping ice creams and counter customer billing. Relaxed AC shop environment. Shift: 12 PM to 9 PM."
  },
  {
    title: "Shawarma & Grill Master",
    companyName: "Arabian Delight Shawarma",
    category: "COOK",
    employmentType: "FULL_TIME",
    payAmount: 16500,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Prakash Nagar, Khammam",
    lat: 17.2550,
    lng: 80.1510,
    contactNumber: "9177990011",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Chicken shawarma slicing and roll making master needed. Shift: 2 PM to 10:30 PM. Good salary + performance bonus."
  },

  // 3. Drivers & Delivery
  {
    title: "Bike Delivery Boys (Local Parcels)",
    companyName: "Khammam Express Couriers",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 15000,
    payType: "MONTHLY",
    vacancies: 5,
    shift: "DAY",
    area: "Bus Stand Road, Khammam",
    lat: 17.2455,
    lng: 80.1445,
    contactNumber: "9959112244",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Must have valid driving licence and own two-wheeler. Delivering e-commerce parcels in Khammam city limits. Petrol allowance separate."
  },
  {
    title: "Personal Car Driver",
    companyName: "Family Residence",
    category: "DRIVER",
    employmentType: "FULL_TIME",
    payAmount: 16000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "VDO's Colony, Khammam",
    lat: 17.2526,
    lng: 80.1545,
    contactNumber: "9848223355",
    gender: "MALE",
    experienceRequired: "2 Years",
    educationRequired: "Any",
    description: "Experienced car driver needed for family sedan. Safe driving, local road knowledge, and good behavior required. 8:30 AM to 7 PM."
  },
  {
    title: "Auto Driver for School Van",
    companyName: "Sri Chaitanya High School Route",
    category: "DRIVER",
    employmentType: "PART_TIME",
    payAmount: 9500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Mamillagudem, Khammam",
    lat: 17.2530,
    lng: 80.1465,
    contactNumber: "9440334466",
    gender: "MALE",
    experienceRequired: "2 Years",
    educationRequired: "Any",
    description: "Morning 7:30 to 9:30 AM and evening 4 to 6 PM school pickup and drop. Safe and responsible driver with badge licence."
  },
  {
    title: "Commercial Tata Ace Driver",
    companyName: "Balaji Transport Agency",
    category: "DRIVER",
    employmentType: "FULL_TIME",
    payAmount: 17000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2435,
    lng: 80.1568,
    contactNumber: "9989445577",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Tata Ace mini-truck driver for moving goods from wholesale godowns to local shops. Clean driving record required."
  },
  {
    title: "Water Can Delivery Boy",
    companyName: "Bisleri Mineral Water Agency",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 12500,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Prakash Nagar, Khammam",
    lat: 17.2550,
    lng: 80.1510,
    contactNumber: "9866556688",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Delivering 20L water cans to houses and offices using 3-wheeler auto or bike carrier. Daily target incentive."
  },
  {
    title: "LPG Gas Cylinder Delivery Staff",
    companyName: "Bharat Gas Agency",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Ricemill Road, Khammam",
    lat: 17.2580,
    lng: 80.1600,
    contactNumber: "9701667799",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Domestic cylinder home delivery with trolley/auto. Fixed salary + tip earnings. Hardworking candidates required."
  },
  {
    title: "Food Delivery Partner",
    companyName: "Khammam Food Hub",
    category: "DELIVERY",
    employmentType: "FULL_TIME",
    payAmount: 16000,
    payType: "MONTHLY",
    vacancies: 6,
    shift: "DAY",
    area: "Gandhi Chowk, Khammam",
    lat: 17.2485,
    lng: 80.1492,
    contactNumber: "9949778800",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Flexible food delivery work in Khammam city. Weekly payouts + joining bonus. Own bike and smartphone required."
  },

  // 4. Skilled Trades & Maintenance
  {
    title: "House Electrician & Inverter Tech",
    companyName: "Sri Sai Electrical Works",
    category: "ELECTRICIAN",
    employmentType: "FULL_TIME",
    payAmount: 16500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2435,
    lng: 80.1568,
    contactNumber: "9849889911",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "ITI / 10th",
    description: "House wiring, switchboard fixing, ceiling fan installation, and inverter battery connections. Local travel allowance provided."
  },
  {
    title: "Plumber & Pipe Fitting Specialist",
    companyName: "Vinayaka Plumbing Services",
    category: "PLUMBER",
    employmentType: "FULL_TIME",
    payAmount: 16000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Mamillagudem, Khammam",
    lat: 17.2530,
    lng: 80.1465,
    contactNumber: "9490990022",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Bathroom tap fittings, motor installation, PVC pipeline repair, and water tank cleaning. Steady year-round work."
  },
  {
    title: "AC & Refrigerator Repair Tech",
    companyName: "Cool Point Air Conditioning",
    category: "MECHANIC",
    employmentType: "FULL_TIME",
    payAmount: 19000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Mayuri Centre, Khammam",
    lat: 17.2492,
    lng: 80.1528,
    contactNumber: "9177001133",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "ITI / 10th",
    description: "Split AC installation, gas charging, foam jet deep cleaning, and fridge repair. High season incentives."
  },
  {
    title: "Two-Wheeler Bike Mechanic Helper",
    companyName: "Sri Rama Auto Works",
    category: "MECHANIC",
    employmentType: "FULL_TIME",
    payAmount: 11000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Trunk Road, Khammam",
    lat: 17.2460,
    lng: 80.1500,
    contactNumber: "9959223355",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "General servicing helper for scooters and bikes. Oil change, washing, and puncture repair. Good opportunity to learn complete mechanic work."
  },
  {
    title: "Car Spa Wash & Polishing Staff",
    companyName: "Speedy Car Spa & Detailing",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 11500,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2435,
    lng: 80.1568,
    contactNumber: "9848334466",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "High-pressure car washing, interior foam shampooing, and vacuuming. Modern equipment and training provided."
  },
  {
    title: "Welder & Gate Fabrication Worker",
    companyName: "Srinivasa Steel & Iron Works",
    category: "CONSTRUCTION",
    employmentType: "FULL_TIME",
    payAmount: 550,
    payType: "DAILY",
    vacancies: 3,
    shift: "DAY",
    area: "Industrial Area, Khammam",
    lat: 17.2620,
    lng: 80.1380,
    contactNumber: "9440445577",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Arc welding and cutting for window grills, compound gates, and shed structures. Daily payments."
  },
  {
    title: "Carpenter & Furniture Assembler",
    companyName: "Modern Woodcraft Interior",
    category: "CARPENTER",
    employmentType: "FULL_TIME",
    payAmount: 18000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Kaman Bazar, Khammam",
    lat: 17.2510,
    lng: 80.1480,
    contactNumber: "9989556688",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Plywood cutting, laminate pasting, modular kitchen fittings, and door alignment work. Continuous projects."
  },
  {
    title: "House Painter & Roller Specialist",
    companyName: "Sri Durga Painting Works",
    category: "PAINTER",
    employmentType: "FULL_TIME",
    payAmount: 600,
    payType: "DAILY",
    vacancies: 4,
    shift: "DAY",
    area: "Rotary Nagar, Khammam",
    lat: 17.2440,
    lng: 80.1470,
    contactNumber: "9866667799",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Interior putty application, wall primer, and roller painting. New apartments and repainting work. Daily pay."
  },
  {
    title: "False Ceiling & Gypsum Worker",
    companyName: "Interior Arts Studio",
    category: "CONSTRUCTION",
    employmentType: "FULL_TIME",
    payAmount: 16500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "VDO's Colony, Khammam",
    lat: 17.2526,
    lng: 80.1545,
    contactNumber: "9701778800",
    gender: "MALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Gypsum channel fixing, sheet screwing, and joint tape finishing for residential false ceilings."
  },
  {
    title: "Tiles & Flooring Helper",
    companyName: "Khammam Tiles & Marble Hub",
    category: "CONSTRUCTION",
    employmentType: "FULL_TIME",
    payAmount: 500,
    payType: "DAILY",
    vacancies: 3,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2435,
    lng: 80.1568,
    contactNumber: "9949889911",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Cement mortar mixing, tile carrying, and grout filling helper. Daily cash settlement."
  },
  {
    title: "CCTV Camera Installation Technician",
    companyName: "SecureTech Solutions",
    category: "ELECTRICIAN",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Mayuri Centre, Khammam",
    lat: 17.2492,
    lng: 80.1528,
    contactNumber: "9849990022",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "10th Pass",
    description: "CCTV camera cabling, DVR setup, and network router connection for shops and homes. Bike required."
  },

  // 5. Domestic & Household Care
  {
    title: "House Maid (Daily Cleaning & Mopping)",
    companyName: "Green Park Apartments",
    category: "MAID",
    employmentType: "PART_TIME",
    payAmount: 6500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "VDO's Colony, Khammam",
    lat: 17.2526,
    lng: 80.1545,
    contactNumber: "9490001133",
    gender: "FEMALE",
    experienceRequired: "Experienced",
    educationRequired: "Any",
    description: "Morning house maid needed for sweeping, wet mopping, and vessel washing for two 2BHK flats. Timings: 7 AM to 10 AM. Respectful family."
  },
  {
    title: "Home Cook for Family",
    companyName: "Private Residence",
    category: "COOK",
    employmentType: "PART_TIME",
    payAmount: 8000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Rotary Nagar, Khammam",
    lat: 17.2440,
    lng: 80.1470,
    contactNumber: "9177112244",
    gender: "FEMALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Home cook needed for Andhra Telugu meals (curry, dal, rasam, rice, chapati). Morning 7:30 to 9:30 AM and Evening 6:30 to 8 PM."
  },
  {
    title: "Patient Attendant / Elderly Care",
    companyName: "Senior Home Care Assistance",
    category: "CARETAKER",
    employmentType: "FULL_TIME",
    payAmount: 14000,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Mamillagudem, Khammam",
    lat: 17.2530,
    lng: 80.1465,
    contactNumber: "9959334466",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Caring for senior citizen at home: medicine reminder, walking support, and companionship. Patient and caring persons required."
  },
  {
    title: "Baby Caretaker / Day Nanny",
    companyName: "Private Doctor Family",
    category: "CARETAKER",
    employmentType: "FULL_TIME",
    payAmount: 12000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Prakash Nagar, Khammam",
    lat: 17.2550,
    lng: 80.1510,
    contactNumber: "9848445577",
    gender: "FEMALE",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Looking after 2-year-old child: feeding, playing, and day nap routine. Timings: 9 AM to 5 PM. Food provided."
  },
  {
    title: "Clothes Ironing & Laundry Worker",
    companyName: "Sri Srinivasa Dry Cleaners",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 11500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Kaman Bazar, Khammam",
    lat: 17.2510,
    lng: 80.1480,
    contactNumber: "9440556688",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Steam iron handling, clothes folding, and tag numbering. Neat and systematic work."
  },
  {
    title: "Ladies Beautician Assistant",
    companyName: "Glamour Women's Beauty Parlour",
    category: "BEAUTICIAN",
    employmentType: "FULL_TIME",
    payAmount: 12500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2435,
    lng: 80.1568,
    contactNumber: "9989667799",
    gender: "FEMALE",
    experienceRequired: "6 Months",
    educationRequired: "10th Pass",
    description: "Eyebrow threading, waxing, facial, and hair spa assistant needed for busy women's parlour. Friendly environment."
  },
  {
    title: "Ladies Tailor & Blouse Cutting Master",
    companyName: "Sri Lakshmi Ladies Tailors",
    category: "TAILOR",
    employmentType: "FULL_TIME",
    payAmount: 15500,
    payType: "MONTHLY",
    vacancies: 2,
    shift: "DAY",
    area: "Gandhi Chowk, Khammam",
    lat: 17.2485,
    lng: 80.1492,
    contactNumber: "9866778800",
    gender: "ANY",
    experienceRequired: "1 Year",
    educationRequired: "Any",
    description: "Blouse stitching, dress alteration, and piping specialist. Piece-rate earnings also available. Continuous orders."
  },

  // 6. Corporate, Warehouse & Office
  {
    title: "Office Boy & Document Runner",
    companyName: "Sri Finance & Chits Ltd",
    category: "OFFICE_STAFF",
    employmentType: "FULL_TIME",
    payAmount: 10500,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Wyra Road, Khammam",
    lat: 17.2435,
    lng: 80.1568,
    contactNumber: "9701889911",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "10th Pass",
    description: "Office file dispatch, bank deposits, tea serving, and photocopy work. Timings: 9:30 AM to 6 PM."
  },
  {
    title: "Computer Data Entry & Xerox Operator",
    companyName: "Sri Sai Net Centre & Xerox",
    category: "DATA_ENTRY",
    employmentType: "FULL_TIME",
    payAmount: 11000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "DAY",
    area: "Bus Stand Road, Khammam",
    lat: 17.2455,
    lng: 80.1445,
    contactNumber: "9949990022",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Intermediate",
    description: "Online application filling, typing, scanning, and Xerox machine operation. Basic computer skills needed."
  },
  {
    title: "Warehouse Goods Packing Worker",
    companyName: "Khammam Logistics Godown",
    category: "PACKER",
    employmentType: "FULL_TIME",
    payAmount: 11500,
    payType: "MONTHLY",
    vacancies: 4,
    shift: "DAY",
    area: "Tekulapally, Khammam",
    lat: 17.2620,
    lng: 80.1380,
    contactNumber: "9849001133",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Carton box taping, barcode labelling, and goods sorting in central dispatch warehouse. Overtime allowance provided."
  },
  {
    title: "Vegetable Market Loading Worker",
    companyName: "Rythu Bazar Wholesale Commission",
    category: "HELPER",
    employmentType: "FULL_TIME",
    payAmount: 500,
    payType: "DAILY",
    vacancies: 4,
    shift: "DAY",
    area: "Gandhi Chowk, Khammam",
    lat: 17.2485,
    lng: 80.1492,
    contactNumber: "9490112244",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Early morning tomato and vegetable crate unloading from trucks (4 AM to 10 AM). Daily cash pay + breakfast."
  },
  {
    title: "Apartment Night Watchman",
    companyName: "Sri Residency Apartment Welfare",
    category: "SECURITY",
    employmentType: "FULL_TIME",
    payAmount: 11000,
    payType: "MONTHLY",
    vacancies: 1,
    shift: "NIGHT",
    area: "VDO's Colony, Khammam",
    lat: 17.2526,
    lng: 80.1545,
    contactNumber: "9177223355",
    gender: "MALE",
    experienceRequired: "Fresher",
    educationRequired: "Any",
    description: "Night security guard for residential gate. Night 9 PM to 7 AM. Single room with fan provided for guard."
  },
  {
    title: "Hospital Ward Attendant",
    companyName: "Care Multispeciality Hospital",
    category: "HEALTHCARE",
    employmentType: "FULL_TIME",
    payAmount: 12000,
    payType: "MONTHLY",
    vacancies: 3,
    shift: "ANY",
    area: "Mamillagudem, Khammam",
    lat: 17.2530,
    lng: 80.1465,
    contactNumber: "9959445577",
    gender: "ANY",
    experienceRequired: "Fresher",
    educationRequired: "10th Pass",
    description: "Patient wheelchair assistance, stretcher pushing, and ward medicine delivery. ESI and PF benefits included."
  }
];

async function seedJobs() {
  console.log(`Starting to seed ${KHAMMAM_JOBS.length} hyper-local Khammam jobs into Firestore...`);
  const now = Date.now();
  const DAY_MS = 24 * 60 * 60 * 1000;
  const JOB_LIFETIME_MS = 30 * DAY_MS;

  let count = 0;
  // Process in batches of 20 (Firestore batch limit is 500 ops)
  for (let i = 0; i < KHAMMAM_JOBS.length; i += 15) {
    const chunk = KHAMMAM_JOBS.slice(i, i + 15);
    const batch = db.batch();

    for (const item of chunk) {
      // Deterministic ID for repeatability
      const slug = item.title.toLowerCase().replace(/[^a-z0-9]/g, "").slice(0, 8);
      const companySlug = item.companyName.toLowerCase().replace(/[^a-z0-9]/g, "").slice(0, 8);
      const jobId = `khm_${slug}_${companySlug}_${i + chunk.indexOf(item)}`;

      const lat = item.lat;
      const lng = item.lng;
      const geohash = encodeGeohash(lat, lng, 9);
      const cell = encodeGeohash(lat, lng, 5);
      const keywords = keywordsOf(item.title, item.companyName, item.area, "Khammam", item.category.replace(/_/g, " "));

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
        districtId: 509,
        district: "Khammam",
        stateId: 36,
        state: "Telangana",
        status: "open",
        applicationCount: 0,
        keywords: keywords,
        createdAt: Timestamp.fromMillis(now),
        expiresAt: Timestamp.fromMillis(now + JOB_LIFETIME_MS)
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

      // Set authoritative collections: jobmetadata, job_details, job_contacts
      const metaRef = db.collection("jobmetadata").doc(jobId);
      batch.set(metaRef, cardData);

      const detailsRef = db.collection("job_details").doc(jobId);
      batch.set(detailsRef, detailsData);

      const contactRef = db.collection("job_contacts").doc(jobId);
      batch.set(contactRef, contactData);

      // Also mirror into "jobs" collection so any query targeting "jobs" works seamlessly
      const jobsRef = db.collection("jobs").doc(jobId);
      batch.set(jobsRef, cardData);

      count++;
    }

    await batch.commit();
    console.log(`Committed chunk up to job ${count}/${KHAMMAM_JOBS.length}`);
  }

  console.log(`\n Successfully seeded ${count} Khammam district jobs across jobmetadata, job_details, job_contacts & jobs!`);
}

seedJobs()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error("Failed to seed jobs:", e);
    process.exit(1);
  });
