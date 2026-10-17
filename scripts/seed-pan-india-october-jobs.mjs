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
// REGION DEFINITIONS COVERING EVERY STATE IN INDIA
// ============================================================================

const REGIONS = [
  // 1. MAHARASHTRA - MUMBAI / BHAYANDAR WEST & THANE (Focal point: 60+ jobs)
  {
    state: "Maharashtra", stateId: 27, district: "Thane", districtId: 497, phonePrefix: "9820",
    areas: [
      { name: "Maxus Mall, Bhayandar West", lat: 19.3005, lng: 72.8465 },
      { name: "Station Road, Bhayandar West", lat: 19.2965, lng: 72.8524 },
      { name: "Phatak Road, Bhayandar West", lat: 19.2921, lng: 72.8498 },
      { name: "Uttan Road, Bhayandar West", lat: 19.2884, lng: 72.8415 },
      { name: "150 Feet Road, Bhayandar West", lat: 19.3032, lng: 72.8450 },
      { name: "Jesal Park, Bhayandar East", lat: 19.3012, lng: 72.8610 },
      { name: "Indralok Phase 3, Bhayandar East", lat: 19.2980, lng: 72.8645 },
      { name: "Navghar Road, Bhayandar East", lat: 19.3045, lng: 72.8570 },
      { name: "Shanti Park, Mira Road", lat: 19.2860, lng: 72.8715 },
      { name: "Silver Park, Mira Road", lat: 19.2890, lng: 72.8680 },
      { name: "Kanakia Park, Mira Road", lat: 19.2810, lng: 72.8745 },
      { name: "Naupada, Thane West", lat: 19.1860, lng: 72.9720 },
      { name: "Ghodbunder Road, Thane West", lat: 19.2450, lng: 72.9730 },
      { name: "Wagle Estate, Thane", lat: 19.1920, lng: 72.9510 }
    ],
    targetCount: 50
  },
  {
    state: "Maharashtra", stateId: 27, district: "Mumbai Suburban", districtId: 483, phonePrefix: "9833",
    areas: [
      { name: "Borivali West, Mumbai Suburban", lat: 19.2307, lng: 72.8567 },
      { name: "Kandivali West, Mumbai Suburban", lat: 19.2062, lng: 72.8492 },
      { name: "Malad West Link Road, Mumbai", lat: 19.1865, lng: 72.8360 },
      { name: "Goregaon East, Mumbai", lat: 19.1663, lng: 72.8526 },
      { name: "Andheri East MIDC, Mumbai", lat: 19.1197, lng: 72.8697 },
      { name: "Andheri West Lokhandwala, Mumbai", lat: 19.1410, lng: 72.8290 },
      { name: "Bandra Kurla Complex, Mumbai", lat: 19.0660, lng: 72.8680 },
      { name: "Dadar West Market, Mumbai", lat: 19.0178, lng: 72.8478 }
    ],
    targetCount: 30
  },

  // 2. JHARKHAND (Focal point: 60+ jobs)
  {
    state: "Jharkhand", stateId: 20, district: "Ranchi", districtId: 339, phonePrefix: "9431",
    areas: [
      { name: "Main Road, Ranchi", lat: 23.3610, lng: 85.3250 },
      { name: "Lalpur Chowk, Ranchi", lat: 23.3725, lng: 85.3340 },
      { name: "Doranda, Ranchi", lat: 23.3400, lng: 85.3210 },
      { name: "Ratu Road, Ranchi", lat: 23.3750, lng: 85.3050 },
      { name: "Hinoo, Ranchi", lat: 23.3280, lng: 85.3240 },
      { name: "Bariatu, Ranchi", lat: 23.3980, lng: 85.3520 },
      { name: "Harmu Housing Colony, Ranchi", lat: 23.3520, lng: 85.3010 },
      { name: "Kanke Road, Ranchi", lat: 23.4020, lng: 85.3180 },
      { name: "Dhurwa HEC Sector 2, Ranchi", lat: 23.3150, lng: 85.2890 }
    ],
    targetCount: 30
  },
  {
    state: "Jharkhand", stateId: 20, district: "East Singhbum", districtId: 327, phonePrefix: "9470",
    areas: [
      { name: "Bistupur Market, Jamshedpur", lat: 22.8020, lng: 86.1850 },
      { name: "Sakchi, Jamshedpur", lat: 22.8150, lng: 86.2080 },
      { name: "Telco Colony, Jamshedpur", lat: 22.7750, lng: 86.2550 },
      { name: "Golmuri, Jamshedpur", lat: 22.7950, lng: 86.2250 },
      { name: "Kadma, Jamshedpur", lat: 22.7980, lng: 86.1620 },
      { name: "Gamharia Industrial Area, Jamshedpur", lat: 22.8120, lng: 86.0950 }
    ],
    targetCount: 20
  },
  {
    state: "Jharkhand", stateId: 20, district: "Dhanbad", districtId: 325, phonePrefix: "9471",
    areas: [
      { name: "Bank More, Dhanbad", lat: 23.7880, lng: 86.4250 },
      { name: "Saraidhela, Dhanbad", lat: 23.8150, lng: 86.4480 },
      { name: "Hirapur, Dhanbad", lat: 23.8010, lng: 86.4380 },
      { name: "Steel Gate, Dhanbad", lat: 23.8190, lng: 86.4520 },
      { name: "Katras Bazar, Dhanbad", lat: 23.8050, lng: 86.2950 }
    ],
    targetCount: 15
  },
  {
    state: "Jharkhand", stateId: 20, district: "Bokaro", districtId: 322, phonePrefix: "9430",
    areas: [
      { name: "City Centre Sector 4, Bokaro", lat: 23.6650, lng: 86.1480 },
      { name: "Chas Main Road, Bokaro", lat: 23.6350, lng: 86.1750 },
      { name: "Sector 1 Market, Bokaro", lat: 23.6780, lng: 86.1320 },
      { name: "Bokaro Steel City Industrial Area", lat: 23.6520, lng: 86.1680 }
    ],
    targetCount: 12
  },

  // 3. DELHI NCR & HARYANA
  {
    state: "Delhi", stateId: 7, district: "New Delhi", districtId: 79, phonePrefix: "9811",
    areas: [
      { name: "Connaught Place, New Delhi", lat: 28.6315, lng: 77.2167 },
      { name: "Lajpat Nagar Central Market, Delhi", lat: 28.5680, lng: 77.2430 },
      { name: "Karol Bagh Market, New Delhi", lat: 28.6520, lng: 77.1900 },
      { name: "Nehru Place IT Hub, Delhi", lat: 28.5490, lng: 77.2520 },
      { name: "Saket District Centre, Delhi", lat: 28.5280, lng: 77.2180 }
    ],
    targetCount: 20
  },
  {
    state: "Haryana", stateId: 6, district: "Gurugram", districtId: 62, phonePrefix: "9810",
    areas: [
      { name: "DLF Cyber City, Gurugram", lat: 28.4950, lng: 77.0890 },
      { name: "Sector 29 Market, Gurugram", lat: 28.4680, lng: 77.0650 },
      { name: "Sohna Road, Gurugram", lat: 28.4150, lng: 77.0420 },
      { name: "Udyog Vihar Phase 4, Gurugram", lat: 28.5020, lng: 77.0780 }
    ],
    targetCount: 15
  },

  // 4. TELANGANA & ANDHRA PRADESH
  {
    state: "Telangana", stateId: 36, district: "Hyderabad", districtId: 507, phonePrefix: "9849",
    areas: [
      { name: "Gachibowli Financial District, Hyderabad", lat: 17.4401, lng: 78.3489 },
      { name: "Madhapur Hitec City, Hyderabad", lat: 17.4486, lng: 78.3908 },
      { name: "Kukatpally Housing Board, Hyderabad", lat: 17.4849, lng: 78.4138 },
      { name: "Dilsukhnagar Main Road, Hyderabad", lat: 17.3688, lng: 78.5247 },
      { name: "Banjara Hills Road No 12, Hyderabad", lat: 17.4156, lng: 78.4347 }
    ],
    targetCount: 20
  },
  {
    state: "Telangana", stateId: 36, district: "Khammam", districtId: 509, phonePrefix: "9949",
    areas: [
      { name: "Wyra Road, Khammam", lat: 17.2512, lng: 80.1584 },
      { name: "Gandhi Chowk, Khammam", lat: 17.2488, lng: 80.1491 },
      { name: "Mayuri Centre, Khammam", lat: 17.2450, lng: 80.1480 },
      { name: "Mustafa Nagar, Khammam", lat: 17.2620, lng: 80.1650 },
      { name: "Mamillagudem, Khammam", lat: 17.2410, lng: 80.1420 }
    ],
    targetCount: 15
  },
  {
    state: "Andhra Pradesh", stateId: 28, district: "Visakhapatnam", districtId: 520, phonePrefix: "9848",
    areas: [
      { name: "Gajuwaka Industrial Belt, Visakhapatnam", lat: 17.6910, lng: 83.2120 },
      { name: "Dwaraka Nagar, Visakhapatnam", lat: 17.7280, lng: 83.3150 },
      { name: "MVP Colony, Visakhapatnam", lat: 17.7420, lng: 83.3380 },
      { name: "Siripuram Junction, Visakhapatnam", lat: 17.7210, lng: 83.3180 }
    ],
    targetCount: 15
  },
  {
    state: "Andhra Pradesh", stateId: 28, district: "NTR", districtId: 749, phonePrefix: "9849",
    areas: [
      { name: "Benz Circle, Vijayawada", lat: 16.5020, lng: 80.6550 },
      { name: "Governorpet, Vijayawada", lat: 16.5120, lng: 80.6280 },
      { name: "Auto Nagar, Vijayawada", lat: 16.4950, lng: 80.6820 }
    ],
    targetCount: 12
  },

  // 5. KARNATAKA & TAMIL NADU
  {
    state: "Karnataka", stateId: 29, district: "Bengaluru Urban", districtId: 525, phonePrefix: "9845",
    areas: [
      { name: "Koramangala 5th Block, Bengaluru", lat: 12.9352, lng: 77.6245 },
      { name: "Indiranagar 100 Feet Road, Bengaluru", lat: 12.9784, lng: 77.6408 },
      { name: "HSR Layout Sector 1, Bengaluru", lat: 12.9120, lng: 77.6440 },
      { name: "Whitefield ITPL Main Road, Bengaluru", lat: 12.9698, lng: 77.7499 },
      { name: "Jayanagar 4th Block, Bengaluru", lat: 12.9290, lng: 77.5830 }
    ],
    targetCount: 20
  },
  {
    state: "Tamil Nadu", stateId: 33, district: "Chennai", districtId: 568, phonePrefix: "9840",
    areas: [
      { name: "T. Nagar Ranganathan St, Chennai", lat: 13.0418, lng: 80.2341 },
      { name: "Anna Nagar Roundtana, Chennai", lat: 13.0850, lng: 80.2120 },
      { name: "Velachery Main Road, Chennai", lat: 12.9810, lng: 80.2180 },
      { name: "OMR Thoraipakkam, Chennai", lat: 12.9350, lng: 80.2380 }
    ],
    targetCount: 18
  },
  {
    state: "Tamil Nadu", stateId: 33, district: "Coimbatore", districtId: 569, phonePrefix: "9842",
    areas: [
      { name: "RS Puram DB Road, Coimbatore", lat: 11.0110, lng: 76.9520 },
      { name: "Gandhipuram Cross Cut Road, Coimbatore", lat: 11.0180, lng: 76.9680 },
      { name: "Peelamedu Avinashi Road, Coimbatore", lat: 11.0280, lng: 77.0080 }
    ],
    targetCount: 12
  },

  // 6. GUJARAT & RAJASTHAN
  {
    state: "Gujarat", stateId: 24, district: "Ahmadabad", districtId: 438, phonePrefix: "9824",
    areas: [
      { name: "C.G. Road Navrangpura, Ahmadabad", lat: 23.0330, lng: 72.5610 },
      { name: "Prahlad Nagar SG Highway, Ahmadabad", lat: 23.0110, lng: 72.5080 },
      { name: "Maninagar Railway Cross, Ahmadabad", lat: 22.9980, lng: 72.6050 },
      { name: "Vatva GIDC Phase 2, Ahmadabad", lat: 22.9550, lng: 72.6350 }
    ],
    targetCount: 16
  },
  {
    state: "Gujarat", stateId: 24, district: "Surat", districtId: 459, phonePrefix: "9825",
    areas: [
      { name: "Ring Road Textile Market, Surat", lat: 21.1850, lng: 72.8450 },
      { name: "Varachha Diamond Market, Surat", lat: 21.2180, lng: 72.8680 },
      { name: "Athwa Lines VIP Road, Surat", lat: 21.1620, lng: 72.7950 }
    ],
    targetCount: 14
  },
  {
    state: "Rajasthan", stateId: 8, district: "Jaipur", districtId: 102, phonePrefix: "9829",
    areas: [
      { name: "MI Road Panch Batti, Jaipur", lat: 26.9180, lng: 75.8150 },
      { name: "Malviya Nagar Gaurav Tower, Jaipur", lat: 26.8520, lng: 75.8080 },
      { name: "Mansarovar Sector 3, Jaipur", lat: 26.8650, lng: 75.7620 },
      { name: "Vaishali Nagar Amrapali Circle, Jaipur", lat: 26.9080, lng: 75.7420 }
    ],
    targetCount: 16
  },

  // 7. UTTAR PRADESH & BIHAR
  {
    state: "Uttar Pradesh", stateId: 9, district: "Lucknow", districtId: 162, phonePrefix: "9415",
    areas: [
      { name: "Hazratganj Main Market, Lucknow", lat: 26.8520, lng: 80.9460 },
      { name: "Gomti Nagar Patrakarpuram, Lucknow", lat: 26.8620, lng: 81.0020 },
      { name: "Alambagh Bus Stand, Lucknow", lat: 26.8150, lng: 80.9080 },
      { name: "Indira Nagar Munshi Pulia, Lucknow", lat: 26.8850, lng: 80.9850 }
    ],
    targetCount: 16
  },
  {
    state: "Uttar Pradesh", stateId: 9, district: "Kanpur Nagar", districtId: 157, phonePrefix: "9450",
    areas: [
      { name: "Mall Road, Kanpur", lat: 26.4680, lng: 80.3520 },
      { name: "Gumti No. 5 Market, Kanpur", lat: 26.4780, lng: 80.3150 },
      { name: "Jajmau Industrial Area, Kanpur", lat: 26.4350, lng: 80.3950 }
    ],
    targetCount: 12
  },
  {
    state: "Bihar", stateId: 10, district: "Patna", districtId: 212, phonePrefix: "9431",
    areas: [
      { name: "Boring Road Crossing, Patna", lat: 25.6150, lng: 85.1220 },
      { name: "Kankarbagh Main Road, Patna", lat: 25.5980, lng: 85.1580 },
      { name: "Fraser Road Dak Bunglow, Patna", lat: 25.6080, lng: 85.1380 },
      { name: "Bailey Road Saguna More, Patna", lat: 25.6250, lng: 85.0680 }
    ],
    targetCount: 16
  },

  // 8. WEST BENGAL & MADHYA PRADESH
  {
    state: "West Bengal", stateId: 19, district: "Kolkata", districtId: 315, phonePrefix: "9830",
    areas: [
      { name: "Park Street Camac St, Kolkata", lat: 22.5510, lng: 88.3520 },
      { name: "Salt Lake Sector 5 IT Hub, Kolkata", lat: 22.5780, lng: 88.4320 },
      { name: "Gariahat Pantaloons Crossing, Kolkata", lat: 22.5180, lng: 88.3680 },
      { name: "New Market Esplanade, Kolkata", lat: 22.5620, lng: 88.3550 }
    ],
    targetCount: 18
  },
  {
    state: "Madhya Pradesh", stateId: 23, district: "Indore", districtId: 410, phonePrefix: "9826",
    areas: [
      { name: "Vijay Nagar AB Road, Indore", lat: 22.7520, lng: 75.8950 },
      { name: "Chhappan Dukan New Palasia, Indore", lat: 22.7240, lng: 75.8820 },
      { name: "Sapna Sangeeta Road, Indore", lat: 22.7050, lng: 75.8650 }
    ],
    targetCount: 14
  },
  {
    state: "Madhya Pradesh", stateId: 23, district: "Bhopal", districtId: 396, phonePrefix: "9827",
    areas: [
      { name: "MP Nagar Zone 1, Bhopal", lat: 23.2350, lng: 77.4350 },
      { name: "New Market TT Nagar, Bhopal", lat: 23.2310, lng: 77.4020 },
      { name: "Hoshangabad Road Habibganj, Bhopal", lat: 23.2080, lng: 77.4420 }
    ],
    targetCount: 12
  },

  // 9. KERALA, PUNJAB & ODISHA
  {
    state: "Kerala", stateId: 32, district: "Ernakulam", districtId: 555, phonePrefix: "9847",
    areas: [
      { name: "MG Road Marine Drive, Ernakulam", lat: 9.9810, lng: 76.2750 },
      { name: "Edappally Lulu Mall Area, Kochi", lat: 10.0250, lng: 76.3080 },
      { name: "Kakkanad Infopark Express, Kochi", lat: 10.0120, lng: 76.3580 }
    ],
    targetCount: 15
  },
  {
    state: "Punjab", stateId: 3, district: "Ludhiana", districtId: 36, phonePrefix: "9814",
    areas: [
      { name: "Focal Point Phase 5, Ludhiana", lat: 30.8950, lng: 75.9150 },
      { name: "Model Town Market, Ludhiana", lat: 30.8850, lng: 75.8380 },
      { name: "Ferozepur Road Westend Mall, Ludhiana", lat: 30.8980, lng: 75.8020 }
    ],
    targetCount: 14
  },
  {
    state: "Odisha", stateId: 21, district: "Khordha", districtId: 362, phonePrefix: "9437",
    areas: [
      { name: "Saheed Nagar Janpath, Bhubaneswar", lat: 20.2880, lng: 85.8450 },
      { name: "Chandrasekharpur Patia, Bhubaneswar", lat: 20.3450, lng: 85.8180 },
      { name: "Master Canteen Station Sq, Bhubaneswar", lat: 20.2710, lng: 85.8410 }
    ],
    targetCount: 14
  },

  // 10. ASSAM, CHHATTISGARH, UTTARAKHAND, GOA, HIMACHAL & J&K
  {
    state: "Assam", stateId: 18, district: "Kamrup Metro", districtId: 289, phonePrefix: "9435",
    areas: [
      { name: "GS Road Christian Basti, Guwahati", lat: 26.1550, lng: 91.7780 },
      { name: "Paltan Bazaar Railway Area, Guwahati", lat: 26.1780, lng: 91.7510 },
      { name: "Beltola Tiniali, Guwahati", lat: 26.1350, lng: 91.7950 }
    ],
    targetCount: 12
  },
  {
    state: "Chhattisgarh", stateId: 22, district: "Raipur", districtId: 387, phonePrefix: "9425",
    areas: [
      { name: "Pandri Cloth Market, Raipur", lat: 21.2580, lng: 81.6520 },
      { name: "Jaistambh Chowk, Raipur", lat: 21.2420, lng: 81.6350 },
      { name: "Urla Industrial Growth Centre, Raipur", lat: 21.3150, lng: 81.6050 }
    ],
    targetCount: 12
  },
  {
    state: "Uttarakhand", stateId: 5, district: "Dehradun", districtId: 49, phonePrefix: "9412",
    areas: [
      { name: "Rajpur Road Clock Tower, Dehradun", lat: 30.3350, lng: 78.0550 },
      { name: "Paltan Bazaar, Dehradun", lat: 30.3210, lng: 78.0410 },
      { name: "ISBT Transport Nagar, Dehradun", lat: 30.2850, lng: 78.0050 }
    ],
    targetCount: 12
  },
  {
    state: "Goa", stateId: 30, district: "North Goa", districtId: 551, phonePrefix: "9822",
    areas: [
      { name: "Panaji Miramar Beach Road, Goa", lat: 15.4850, lng: 73.8120 },
      { name: "Calangute Beach Promenade, North Goa", lat: 15.5450, lng: 73.7550 },
      { name: "Mapusa Friday Market, North Goa", lat: 15.5920, lng: 73.8150 }
    ],
    targetCount: 10
  },
  {
    state: "Himachal Pradesh", stateId: 2, district: "Shimla", districtId: 23, phonePrefix: "9418",
    areas: [
      { name: "The Mall Road Scandal Point, Shimla", lat: 31.1040, lng: 77.1730 },
      { name: "Sanjauli Chowk, Shimla", lat: 31.1010, lng: 77.1950 },
      { name: "Chotta Shimla Secretariat Road, Shimla", lat: 31.0890, lng: 77.1780 }
    ],
    targetCount: 10
  },
  {
    state: "Jammu and Kashmir", stateId: 1, district: "Jammu", districtId: 5, phonePrefix: "9419",
    areas: [
      { name: "Gandhi Nagar Gole Market, Jammu", lat: 32.7050, lng: 74.8650 },
      { name: "Raghunath Bazar, Jammu", lat: 32.7280, lng: 74.8620 }
    ],
    targetCount: 8
  },
  {
    state: "Jammu and Kashmir", stateId: 1, district: "Srinagar", districtId: 13, phonePrefix: "9419",
    areas: [
      { name: "Lal Chowk City Centre, Srinagar", lat: 34.0720, lng: 74.8110 },
      { name: "Rajbagh Zero Bridge, Srinagar", lat: 34.0610, lng: 74.8250 }
    ],
    targetCount: 8
  }
];

// ============================================================================
// DIVERSE BLUE-COLLAR & RETAIL JOB TEMPLATES
// ============================================================================

const TEMPLATES = [
  {
    title: "Instant Grocery Delivery Rider",
    company: "Zepto / Blinkit Local Hub",
    category: "DELIVERY",
    payMin: 18000, payMax: 24000,
    vacMin: 4, vacMax: 10,
    shift: "ROTATIONAL",
    exp: "Fresher welcome",
    edu: "10th Pass",
    desc: "10-minute grocery parcel delivery within 3.5 km delivery radius. Android phone and valid driving license required. Per-order delivery incentives + weekly payout."
  },
  {
    title: "Retail Billing Cashier & POS Operator",
    company: "Smart Point Supermarket",
    category: "CASHIER",
    payMin: 13000, payMax: 16500,
    vacMin: 2, vacMax: 5,
    shift: "DAY",
    exp: "Fresher to 1 yr",
    edu: "12th Pass",
    desc: "Barcode scanning, cash/UPI counter management, customer receipt issuance, and checkout counter upkeep. Friendly communication in local language."
  },
  {
    title: "South & North Indian Tiffin Cook",
    company: "Swad Veg Restaurant",
    category: "COOK",
    payMin: 16000, payMax: 21000,
    vacMin: 1, vacMax: 3,
    shift: "DAY",
    exp: "1 - 3 Years",
    edu: "Any",
    desc: "Preparing delicious breakfast items, thali meals, gravies, and hot snacks. Duty breakfast and lunch provided. Clean commercial kitchen setting."
  },
  {
    title: "Store Sales Executive - Garments & Footwear",
    company: "Trends Retail Store",
    category: "SALES",
    payMin: 14000, payMax: 18000,
    vacMin: 2, vacMax: 6,
    shift: "DAY",
    exp: "Fresher or 6 months",
    edu: "10th / 12th Pass",
    desc: "Assisting customers with sizing and styling choices, garment folding, stock replenishment, and counter display. Attractive monthly sales incentives."
  },
  {
    title: "Commercial & Residential Security Guard",
    company: "Securitas Guarding Service",
    category: "SECURITY",
    payMin: 13500, payMax: 16000,
    vacMin: 3, vacMax: 8,
    shift: "ROTATIONAL",
    exp: "Fresher",
    edu: "10th Pass",
    desc: "Society/complex entrance security, visitor register entry, parking monitoring, and night vigilance rounds. Free uniform and PF/ESI coverage."
  },
  {
    title: "Warehouse Inventory Packer & Sorter",
    company: "E-Kart Logistics Hub",
    category: "PACKER",
    payMin: 14000, payMax: 17500,
    vacMin: 5, vacMax: 12,
    shift: "ROTATIONAL",
    exp: "Fresher",
    edu: "10th Pass",
    desc: "Unloading courier cages, scanning parcel barcodes, sorting shipments into locality bags, and final carton sealing. Safe indoor environment."
  },
  {
    title: "Kitchen Assistant & Vegetable Chopper",
    company: "Urban Spice Cloud Kitchen",
    category: "HELPER",
    payMin: 12000, payMax: 14500,
    vacMin: 2, vacMax: 4,
    shift: "DAY",
    exp: "Fresher",
    edu: "Any",
    desc: "Vegetable washing and cutting, maintaining clean kitchen stations, dishwashing assistance, and packing meal containers for delivery dispatch."
  },
  {
    title: "Appartment Maintenance Electrician",
    company: "PowerGrid Electrical Care",
    category: "ELECTRICIAN",
    payMin: 16500, payMax: 22000,
    vacMin: 2, vacMax: 4,
    shift: "DAY",
    exp: "1 - 3 Years",
    edu: "ITI / Practical Exp",
    desc: "Attending home service calls for MCB tripping, fan installation, switchboard repair, inverter wiring, and lighting fixes. Travel allowance paid."
  },
  {
    title: "Doorstep Sanitary & Pipe Plumber",
    company: "RapidFlow Plumbing Works",
    category: "PLUMBER",
    payMin: 16000, payMax: 20000,
    vacMin: 1, vacMax: 3,
    shift: "DAY",
    exp: "1+ Years",
    edu: "Any",
    desc: "Fixing leaking taps, shower mixers, flush cisterns, toilet blockages, water motor connections, and CPVC piping in flats and offices."
  },
  {
    title: "Domestic House Maid & Housekeeping",
    company: "HomeJoy Maid Services",
    category: "MAID",
    payMin: 11000, payMax: 14000,
    vacMin: 3, vacMax: 6,
    shift: "DAY",
    exp: "Any",
    edu: "Any",
    desc: "Daily floor sweeping, mopping, utensil cleaning, and dust cleaning in family residences. Morning or afternoon flexible work shifts."
  },
  {
    title: "Boutique Tailor & Master Cutter",
    company: "Elite Stitches Studio",
    category: "TAILOR",
    payMin: 16000, payMax: 22000,
    vacMin: 1, vacMax: 3,
    shift: "DAY",
    exp: "2+ Years",
    edu: "Any",
    desc: "Stitching designer suits, blouses, kurtas, and alterations. Proficient with motorized sewing machines, overlocking, and measurement fitting."
  },
  {
    title: "Salon Beautician & Skincare Specialist",
    company: "Rose Beauty Parlour",
    category: "BEAUTICIAN",
    payMin: 15000, payMax: 20000,
    vacMin: 2, vacMax: 4,
    shift: "DAY",
    exp: "1 - 2 Years",
    edu: "Beautician Certificate",
    desc: "Facials, waxing, threading, cleanup, bleaching, manicure/pedicure, and hair styling for female clientele. Commission on high-value packages."
  },
  {
    title: "Inbound Customer Care Executive",
    company: "Vertex BPO Solutions",
    category: "CUSTOMER_SUPPORT",
    payMin: 16000, payMax: 21000,
    vacMin: 4, vacMax: 8,
    shift: "ROTATIONAL",
    exp: "Fresher to 1 yr",
    edu: "12th / Graduate",
    desc: "Resolving customer inquiries regarding order deliveries, billing status, and subscription plans over voice calls. Training provided."
  },
  {
    title: "Corporate Office Boy & Runner",
    company: "Prime Corporate Services",
    category: "OFFICE_STAFF",
    payMin: 12500, payMax: 15000,
    vacMin: 1, vacMax: 3,
    shift: "DAY",
    exp: "Fresher",
    edu: "10th Pass",
    desc: "Tea/coffee preparation for staff, office pantry maintenance, photocopying documents, and local bank/courier errand runs. Day shift."
  },
  {
    title: "Data Entry & Invoice Billing Clerk",
    company: "Shreeji Logistics & Trading",
    category: "DATA_ENTRY",
    payMin: 14500, payMax: 18000,
    vacMin: 2, vacMax: 4,
    shift: "DAY",
    exp: "Fresher to 1 yr",
    edu: "12th Pass",
    desc: "Daily purchase bill data entry in Excel, generating dispatch invoices, filing challans, and printing delivery labels. Typing speed 25+ WPM."
  },
  {
    title: "Two-Wheeler Service & Bike Mechanic",
    company: "AutoCare Multi-Brand Workshop",
    category: "MECHANIC",
    payMin: 16000, payMax: 21500,
    vacMin: 2, vacMax: 4,
    shift: "DAY",
    exp: "1 - 3 Years",
    edu: "ITI / Experienced",
    desc: "Engine oil change, carburetor tuning, clutch overhaul, brake shoe replacement, chain lubrication, and electrical checkups for bikes and scooters."
  },
  {
    title: "Hospital Ward Patient Attendant",
    company: "CareWell Clinic & Hospital",
    category: "HEALTHCARE",
    payMin: 13500, payMax: 16500,
    vacMin: 3, vacMax: 6,
    shift: "ROTATIONAL",
    exp: "Fresher to 1 yr",
    edu: "10th Pass",
    desc: "Wheelchair transport for patients, sample transport to diagnostic lab, checking oxygen cylinder fittings, and patient bed linen management."
  },
  {
    title: "Commercial Goods Delivery Driver",
    company: "CityCargo Logistics",
    category: "DRIVER",
    payMin: 19000, payMax: 24000,
    vacMin: 2, vacMax: 5,
    shift: "DAY",
    exp: "2+ Years (Commercial DL)",
    edu: "10th Pass",
    desc: "Driving Tata Ace / Mahindra Bolero pickup for retail FMCG and departmental deliveries across local city routes. Fuel and daily allowance provided."
  }
];

function generateJobs() {
  const jobs = [];
  let serial = 1000;

  for (const region of REGIONS) {
    const target = region.targetCount;
    for (let i = 0; i < target; i++) {
      const tmpl = TEMPLATES[i % TEMPLATES.length];
      const areaObj = region.areas[i % region.areas.length];
      const areaBase = areaObj.name.split(",")[0].trim();

      const payStep = (tmpl.payMax - tmpl.payMin) / 5;
      const payAmount = Math.round((tmpl.payMin + (i % 5) * payStep) / 500) * 500;
      const vacancies = tmpl.vacMin + (i % (tmpl.vacMax - tmpl.vacMin + 1));

      // Realistic localized phone number
      const phoneSuffix = String(serial * 739 + i * 19).slice(-6);
      const contactNumber = `${region.phonePrefix}${phoneSuffix}`;

      // Coordinates with small micro-jitter (+/- 300 meters)
      const latJitter = ((i % 7) - 3) * 0.0018;
      const lngJitter = (((i + 2) % 7) - 3) * 0.0018;

      const title = `${tmpl.title} - ${areaBase}`;
      const companyName = `${tmpl.company.split("/")[0].trim()} (${areaBase})`;

      jobs.push({
        title,
        companyName,
        category: tmpl.category,
        employmentType: "FULL_TIME",
        payAmount,
        payType: "MONTHLY",
        vacancies,
        shift: tmpl.shift,
        area: areaObj.name,
        lat: Number((areaObj.lat + latJitter).toFixed(6)),
        lng: Number((areaObj.lng + lngJitter).toFixed(6)),
        districtId: region.districtId,
        district: region.district,
        stateId: region.stateId,
        state: region.state,
        contactNumber,
        gender: (tmpl.category === "MAID" || tmpl.category === "BEAUTICIAN") ? "FEMALE" : (tmpl.category === "SECURITY" ? "MALE" : "ANY"),
        experienceRequired: tmpl.exp,
        educationRequired: tmpl.edu,
        description: `Immediate requirement for ${tmpl.title} at ${areaObj.name}, ${region.district}. ${tmpl.desc} Walk-in interviews open today. Contact recruiter directly.`
      });

      serial++;
    }
  }

  return jobs;
}

async function runSeed() {
  const generatedJobs = generateJobs();
  console.log(`Generated ${generatedJobs.length} pan-India jobs across ${REGIONS.length} target regions.`);

  const now = Date.now();
  const JOB_LIFETIME_MS = 30 * 24 * 60 * 60 * 1000;

  let existingTitles = new Set();
  try {
    const existingTitlesRaw = JSON.parse(readFileSync(resolve(__dirname, "existing-titles.json"), "utf-8"));
    existingTitles = new Set(existingTitlesRaw.map(t => t.toLowerCase().trim()));
  } catch (err) {
    console.warn("Could not read existing-titles.json, proceeding with clean set.");
  }

  const newJobs = generatedJobs.filter(j => !existingTitles.has(j.title.toLowerCase().trim()));
  console.log(`Identified ${newJobs.length} strictly unique fresh jobs to insert (0 existing duplicates).`);

  let count = 0;
  const CHUNK_SIZE = 35; // 35 * 4 = 140 writes per batch (well below 500 limit)

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

      // Jitter timestamp across October 14 and 15 (within the last 2 to 34 hours)
      const hoursAgo = (count % 32) + 2; // 2 to 34 hours ago
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
        urgency: (count % 6 === 0) ? "URGENT" : "NORMAL",
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

  console.log(`\n Successfully posted ${count} fresh, genuine pan-India jobs across Mumbai/Bhayandar West, Jharkhand, and all major Indian states!`);
}

runSeed()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error("Failed to seed pan-India jobs:", e);
    process.exit(1);
  });
