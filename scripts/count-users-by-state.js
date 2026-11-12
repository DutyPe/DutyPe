const path = require('path');
const admin = require(path.resolve(__dirname, '../functions/node_modules/firebase-admin'));

if (!admin.apps.length) {
  admin.initializeApp({
    projectId: 'dutype-860ac'
  });
}

const db = admin.firestore();

// Known Indian states and state deduction mapping from cities/districts/pincodes
const CITY_TO_STATE = {
  // Telangana
  'khammam': 'Telangana',
  'hyderabad': 'Telangana',
  'secunderabad': 'Telangana',
  'warangal': 'Telangana',
  'karimnagar': 'Telangana',
  'nizamabad': 'Telangana',
  'nalgonda': 'Telangana',
  'suryapet': 'Telangana',
  'kothagudem': 'Telangana',
  'bhadradri': 'Telangana',
  'mahabubnagar': 'Telangana',
  'rangareddy': 'Telangana',
  'medchal': 'Telangana',
  'sangareddy': 'Telangana',
  'siddipet': 'Telangana',
  'mancherial': 'Telangana',
  'adilabad': 'Telangana',
  'jagvtial': 'Telangana',
  'peddapalli': 'Telangana',
  'kamareddy': 'Telangana',
  'gadwal': 'Telangana',
  'wanaparthy': 'Telangana',
  'nagarkurnool': 'Telangana',
  'vikarabad': 'Telangana',
  'asifabad': 'Telangana',
  'bhupalpally': 'Telangana',
  'mulugu': 'Telangana',
  'narayanpet': 'Telangana',
  'jangaon': 'Telangana',
  'madhira': 'Telangana',
  'sathupalli': 'Telangana',
  'paloncha': 'Telangana',
  'yellandu': 'Telangana',
  'wyra': 'Telangana',
  'kalluru': 'Telangana',

  // Andhra Pradesh
  'vijayawada': 'Andhra Pradesh',
  'visakhapatnam': 'Andhra Pradesh',
  'vizag': 'Andhra Pradesh',
  'guntur': 'Andhra Pradesh',
  'tirupati': 'Andhra Pradesh',
  'kurnool': 'Andhra Pradesh',
  'nellore': 'Andhra Pradesh',
  'rajahmundry': 'Andhra Pradesh',
  'kakinada': 'Andhra Pradesh',
  'kadapa': 'Andhra Pradesh',
  'anantapur': 'Andhra Pradesh',
  'eluru': 'Andhra Pradesh',
  'ongole': 'Andhra Pradesh',
  'chittoor': 'Andhra Pradesh',
  'srikakulam': 'Andhra Pradesh',
  'vizianagaram': 'Andhra Pradesh',
  'machilipatnam': 'Andhra Pradesh',
  'tenali': 'Andhra Pradesh',
  'proddatur': 'Andhra Pradesh',
  'hindupur': 'Andhra Pradesh',
  'bhimavaram': 'Andhra Pradesh',
  'nandyal': 'Andhra Pradesh',
  'tadepalligudem': 'Andhra Pradesh',
  'amaravati': 'Andhra Pradesh',
  'nandigama': 'Andhra Pradesh',
  'jaggayyapeta': 'Andhra Pradesh',

  // Karnataka
  'bangalore': 'Karnataka',
  'bengaluru': 'Karnataka',
  'mysore': 'Karnataka',
  'mysuru': 'Karnataka',
  'hubli': 'Karnataka',
  'mangalore': 'Karnataka',
  'belgaum': 'Karnataka',
  'gulbarga': 'Karnataka',
  'bellary': 'Karnataka',

  // Tamil Nadu
  'chennai': 'Tamil Nadu',
  'coimbatore': 'Tamil Nadu',
  'madurai': 'Tamil Nadu',
  'salem': 'Tamil Nadu',
  'tiruchirappalli': 'Tamil Nadu',

  // Maharashtra
  'mumbai': 'Maharashtra',
  'pune': 'Maharashtra',
  'nagpur': 'Maharashtra',
  'nashik': 'Maharashtra',
  'thane': 'Maharashtra',

  // Delhi / NCR
  'delhi': 'Delhi',
  'new delhi': 'Delhi',
  'noida': 'Uttar Pradesh',
  'gurgaon': 'Haryana',
  'gurugram': 'Haryana',
  'ghaziabad': 'Uttar Pradesh',
  'faridabad': 'Haryana',

  // Others
  'kolkata': 'West Bengal',
  'patna': 'Bihar',
  'jaipur': 'Rajasthan',
  'ahmedabad': 'Gujarat',
  'surat': 'Gujarat',
  'bhopal': 'Madhya Pradesh',
  'indore': 'Madhya Pradesh',
  'lucknow': 'Uttar Pradesh',
  'kanpur': 'Uttar Pradesh',
  'chandigarh': 'Punjab/Chandigarh',
  'bhubaneswar': 'Odisha',
  'kochi': 'Kerala',
  'thiruvananthapuram': 'Kerala',
};

const KNOWN_STATES = [
  'Telangana',
  'Andhra Pradesh',
  'Karnataka',
  'Tamil Nadu',
  'Maharashtra',
  'Kerala',
  'Uttar Pradesh',
  'Madhya Pradesh',
  'Rajasthan',
  'Gujarat',
  'West Bengal',
  'Bihar',
  'Odisha',
  'Punjab',
  'Haryana',
  'Delhi',
  'Assam',
  'Jharkhand',
  'Chhattisgarh',
  'Uttarakhand',
  'Himachal Pradesh',
  'Goa',
];

function detectState(text) {
  if (!text || typeof text !== 'string') return null;
  const lower = text.toLowerCase();
  
  for (const st of KNOWN_STATES) {
    if (lower.includes(st.toLowerCase())) return st;
  }
  
  for (const [city, state] of Object.entries(CITY_TO_STATE)) {
    if (lower.includes(city)) return state;
  }
  
  return null;
}

async function analyzeStateDistribution() {
  console.log('Fetching collections from Firestore...');
  
  const [usersSnap, workersSnap, employersSnap] = await Promise.all([
    db.collection('users').get(),
    db.collection('worker_profiles').get(),
    db.collection('employer_profiles').get()
  ]);

  console.log(`Total docs found:`);
  console.log(`- users: ${usersSnap.size}`);
  console.log(`- worker_profiles: ${workersSnap.size}`);
  console.log(`- employer_profiles: ${employersSnap.size}`);

  const userStats = {};
  const workerStats = {};
  const employerStats = {};
  const cityCounts = {};

  function addStat(map, state) {
    const key = state || 'Unknown / Unspecified';
    map[key] = (map[key] || 0) + 1;
  }

  // 1. Analyze users
  usersSnap.forEach(doc => {
    const d = doc.data();
    const locStr = [d.state, d.city, d.area, d.address, d.locationName, d.district].filter(Boolean).join(' ');
    const state = detectState(locStr) || (d.state ? d.state.trim() : null);
    addStat(userStats, state);
    
    const city = (d.city || d.area || '').trim().toLowerCase();
    if (city) {
      cityCounts[city] = (cityCounts[city] || 0) + 1;
    }
  });

  // 2. Analyze worker_profiles
  workersSnap.forEach(doc => {
    const d = doc.data();
    const locStr = [d.state, d.city, d.area, d.address, d.locationName, d.currentAddress].filter(Boolean).join(' ');
    const state = detectState(locStr) || (d.state ? d.state.trim() : null);
    addStat(workerStats, state);
  });

  // 3. Analyze employer_profiles
  employersSnap.forEach(doc => {
    const d = doc.data();
    const locStr = [d.state, d.city, d.area, d.address, d.companyAddress, d.locationName].filter(Boolean).join(' ');
    const state = detectState(locStr) || (d.state ? d.state.trim() : null);
    addStat(employerStats, state);
  });

  console.log('\n=== USERS COLLECTION BY STATE ===');
  console.table(Object.entries(userStats).sort((a,b) => b[1] - a[1]));

  console.log('\n=== WORKER PROFILES BY STATE ===');
  console.table(Object.entries(workerStats).sort((a,b) => b[1] - a[1]));

  console.log('\n=== EMPLOYER PROFILES BY STATE ===');
  console.table(Object.entries(employerStats).sort((a,b) => b[1] - a[1]));

  console.log('\n=== TOP 20 CITIES / LOCALITIES (from users collection) ===');
  console.table(Object.entries(cityCounts).sort((a,b) => b[1] - a[1]).slice(0, 20));
}

analyzeStateDistribution().catch(err => {
  console.error('Error analyzing states:', err);
  process.exit(1);
});
