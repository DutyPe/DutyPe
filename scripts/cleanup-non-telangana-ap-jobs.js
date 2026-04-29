'use strict';

const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const TARGET_LOCATION_KEYWORDS = [
  'telangana',
  'andhra pradesh',
  'andhra',
  'hyderabad',
  'secunderabad',
  'cyberabad',
  'ranga reddy',
  'rangareddy',
  'medchal',
  'malkajgiri',
  'sangareddy',
  'warangal',
  'hanamkonda',
  'karimnagar',
  'khammam',
  'nizamabad',
  'mahabubnagar',
  'nalgonda',
  'adilabad',
  'siddipet',
  'suryapet',
  'jagtial',
  'mancherial',
  'nirmal',
  'kamareddy',
  'bhadradri',
  'kothagudem',
  'mahabubabad',
  'mulugu',
  'narayanpet',
  'nagarkurnool',
  'wanaparthy',
  'gadwal',
  'vikarabad',
  'medak',
  'yadadri',
  'bhuvanagiri',
  'peddapalli',
  'sircilla',
  'jangaon',
  'visakhapatnam',
  'vizag',
  'vijayawada',
  'guntur',
  'tirupati',
  'kakinada',
  'rajahmundry',
  'rajamahendravaram',
  'nellore',
  'kurnool',
  'kadapa',
  'cuddapah',
  'anantapur',
  'ananthapur',
  'eluru',
  'ongole',
  'srikakulam',
  'vizianagaram',
  'chittoor',
  'machilipatnam',
  'tenali',
  'proddatur',
  'hindupur',
  'madanapalle',
  'nandyal',
  'bhimavaram',
  'tadepalligudem',
  'narasaraopet',
  'amalapuram',
  'gudivada',
  'adoni',
  'markapur',
  'chilakaluripet'
];

const TARGET_CITY_CENTERS = [
  { name: 'Hyderabad', lat: 17.385, lng: 78.487 },
  { name: 'Khammam', lat: 17.247, lng: 80.151 },
  { name: 'Visakhapatnam', lat: 17.6868, lng: 83.2185 },
  { name: 'Vijayawada', lat: 16.5062, lng: 80.648 },
  { name: 'Guntur', lat: 16.3067, lng: 80.4365 },
  { name: 'Tirupati', lat: 13.6288, lng: 79.4192 },
  { name: 'Kakinada', lat: 16.9891, lng: 82.2475 },
  { name: 'Rajahmundry', lat: 17.0, lng: 81.804 },
  { name: 'Nellore', lat: 14.4426, lng: 79.9865 },
  { name: 'Kurnool', lat: 15.8281, lng: 78.0373 },
  { name: 'Kadapa', lat: 14.4673, lng: 78.8242 },
  { name: 'Anantapur', lat: 14.6819, lng: 77.6006 },
  { name: 'Srikakulam', lat: 18.2949, lng: 83.8938 },
  { name: 'Vizianagaram', lat: 18.1067, lng: 83.3956 },
  { name: 'Warangal', lat: 17.9784, lng: 79.5941 },
  { name: 'Karimnagar', lat: 18.4386, lng: 79.1288 },
  { name: 'Nizamabad', lat: 18.6725, lng: 78.0941 }
];

const GEO_MATCH_RADIUS_KM = 70;
const BATCH_LIMIT = 450;

function parseArgs(argv) {
  return {
    apply: argv.includes('--apply')
  };
}

function normalize(value) {
  return String(value || '').toLowerCase().trim();
}

function escapeRegex(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function findTargetKeyword(text) {
  for (const keyword of TARGET_LOCATION_KEYWORDS) {
    const phrase = keyword.split(/\s+/).map(escapeRegex).join('\\s+');
    const pattern = new RegExp(`(^|[^a-z])${phrase}([^a-z]|$)`, 'i');
    if (pattern.test(text)) return keyword;
  }
  return '';
}

function toRad(degrees) {
  return degrees * Math.PI / 180;
}

function distanceKm(fromLat, fromLng, toLat, toLng) {
  const earthRadiusKm = 6371;
  const deltaLat = toRad(toLat - fromLat);
  const deltaLng = toRad(toLng - fromLng);
  const a = Math.sin(deltaLat / 2) ** 2 +
    Math.cos(toRad(fromLat)) * Math.cos(toRad(toLat)) * Math.sin(deltaLng / 2) ** 2;
  return 2 * earthRadiusKm * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function locationText(meta, details) {
  return [
    meta.companyCity,
    meta.addressText,
    meta.city,
    meta.locationName,
    meta.locationText,
    details.companyCity,
    details.addressText,
    details.city,
    details.locationName,
    details.locationText
  ].map(normalize).filter(Boolean).join(' | ');
}

function locationCoordinates(meta, details) {
  const metaLocation = meta.location || {};
  const detailsLocation = details.location || {};
  const lat = Number(metaLocation.lat ?? detailsLocation.lat ?? meta.latitude ?? details.latitude);
  const lng = Number(metaLocation.lng ?? detailsLocation.lng ?? meta.longitude ?? details.longitude);
  return { lat, lng };
}

function classifyLocation(meta, details) {
  const text = locationText(meta, details);
  const keyword = findTargetKeyword(text);
  if (keyword) {
    return { keep: true, reason: `text:${keyword}` };
  }

  const { lat, lng } = locationCoordinates(meta, details);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return { keep: false, reason: 'outside' };
  }

  let nearest = null;
  for (const city of TARGET_CITY_CENTERS) {
    const km = distanceKm(lat, lng, city.lat, city.lng);
    if (!nearest || km < nearest.km) {
      nearest = { name: city.name, km };
    }
  }

  if (nearest && nearest.km <= GEO_MATCH_RADIUS_KM) {
    return { keep: true, reason: `near:${nearest.name}:${nearest.km.toFixed(1)}km` };
  }

  return { keep: false, reason: 'outside' };
}

function cityOf(meta, details) {
  return details.companyCity || meta.companyCity || meta.city || details.city || '';
}

function sampleOf(id, meta, details, reason) {
  const { lat, lng } = locationCoordinates(meta, details);
  return {
    id,
    title: meta.title || details.title || '',
    status: meta.status || '',
    city: cityOf(meta, details),
    address: meta.addressText || details.addressText || '',
    lat: Number.isFinite(lat) ? lat : null,
    lng: Number.isFinite(lng) ? lng : null,
    reason
  };
}

async function commitWhenReady(db, state, force = false) {
  if (state.ops === 0) return;
  if (!force && state.ops < BATCH_LIMIT) return;
  await state.batch.commit();
  state.batch = db.batch();
  state.ops = 0;
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  const admin = require('firebase-admin');
  if (admin.apps.length === 0) {
    admin.initializeApp({ credential: admin.credential.cert(loadServiceAccount()) });
  }

  const db = admin.firestore();
  const [metadataSnapshot, detailsSnapshot] = await Promise.all([
    db.collection('jobmetadata').get(),
    db.collection('job_details').get()
  ]);

  const detailsById = new Map(detailsSnapshot.docs.map((doc) => [doc.id, doc.data() || {}]));
  const metadataIds = new Set(metadataSnapshot.docs.map((doc) => doc.id));
  const keepSamples = [];
  const deleteSamples = [];
  const deleteIds = [];
  const keepByReason = new Map();
  const deleteByCity = new Map();
  const counts = {
    metadataTotal: metadataSnapshot.size,
    detailsTotal: detailsSnapshot.size,
    keep: 0,
    delete: 0,
    openKeep: 0,
    openDelete: 0,
    orphanDetailsDelete: 0
  };

  for (const doc of metadataSnapshot.docs) {
    const meta = doc.data() || {};
    const details = detailsById.get(doc.id) || {};
    const decision = classifyLocation(meta, details);
    if (decision.keep) {
      counts.keep += 1;
      if (meta.status === 'open') counts.openKeep += 1;
      keepByReason.set(decision.reason, (keepByReason.get(decision.reason) || 0) + 1);
      if (keepSamples.length < 30) keepSamples.push(sampleOf(doc.id, meta, details, decision.reason));
    } else {
      counts.delete += 1;
      if (meta.status === 'open') counts.openDelete += 1;
      deleteIds.push(doc.id);
      const city = cityOf(meta, details) || '(blank)';
      deleteByCity.set(city, (deleteByCity.get(city) || 0) + 1);
      if (deleteSamples.length < 30) deleteSamples.push(sampleOf(doc.id, meta, details, decision.reason));
    }
  }

  const orphanDetailIdsToDelete = [];
  for (const doc of detailsSnapshot.docs) {
    if (metadataIds.has(doc.id)) continue;
    const details = doc.data() || {};
    const decision = classifyLocation({}, details);
    if (!decision.keep) {
      counts.orphanDetailsDelete += 1;
      orphanDetailIdsToDelete.push(doc.id);
    }
  }

  const report = {
    mode: options.apply ? 'apply' : 'dry-run',
    counts,
    keepByReason: Object.fromEntries([...keepByReason.entries()].sort((a, b) => b[1] - a[1])),
    deleteByCityTop: [...deleteByCity.entries()].sort((a, b) => b[1] - a[1]).slice(0, 30),
    keepSamples,
    deleteSamples
  };
  console.log(JSON.stringify(report, null, 2));

  if (!options.apply) {
    console.log('Dry-run only. Add --apply to delete non-Telangana/AP jobs from jobmetadata and job_details.');
    return;
  }

  const state = { batch: db.batch(), ops: 0 };
  let metadataDeleted = 0;
  let detailsDeleted = 0;

  for (const id of deleteIds) {
    state.batch.delete(db.collection('jobmetadata').doc(id));
    state.batch.delete(db.collection('job_details').doc(id));
    state.ops += 2;
    metadataDeleted += 1;
    detailsDeleted += 1;
    await commitWhenReady(db, state);
  }

  for (const id of orphanDetailIdsToDelete) {
    state.batch.delete(db.collection('job_details').doc(id));
    state.ops += 1;
    detailsDeleted += 1;
    await commitWhenReady(db, state);
  }

  await commitWhenReady(db, state, true);
  console.log(JSON.stringify({ metadataDeleted, detailsDeleted }, null, 2));
}

main().catch((error) => {
  console.error(error.stack || error.message || error);
  process.exit(1);
});
