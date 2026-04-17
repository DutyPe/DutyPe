const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const keyCandidates = [
  path.join(__dirname, 'serviceAccountKey.json'),
  path.join(__dirname, 'dutype-860ac-firebase-adminsdk.json'),
  path.join(__dirname, '..', 'serviceAccountKey.json'),
  path.join(__dirname, '..', 'dutype-860ac-firebase-adminsdk.json')
];

const keyPath = keyCandidates.find((p) => fs.existsSync(p));
if (!keyPath) {
  console.error('Service account key not found.');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(require(keyPath)),
  projectId: 'dutype-860ac'
});

const db = admin.firestore();

const KHAMMAM = { lat: 17.24916, lng: 80.140014 };

function toRadians(value) {
  return (value * Math.PI) / 180;
}

function distanceKm(lat1, lon1, lat2, lon2) {
  const earthRadius = 6371;
  const dLat = toRadians(lat2 - lat1);
  const dLon = toRadians(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadius * c;
}

function getLatLng(job) {
  const locationObj = job.location && typeof job.location === 'object' ? job.location : null;
  const lat = locationObj?.lat ?? job.lat ?? job.latitude;
  const lng = locationObj?.lng ?? job.lng ?? job.longitude;
  if (typeof lat !== 'number' || typeof lng !== 'number') return null;
  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null;
  return { lat, lng };
}

function isOpen(job) {
  const status = String(job.status || '').toLowerCase();
  if (status) return status === 'open';
  return job.isActive !== false && job.isFilled !== true;
}

function isNotExpired(job, now) {
  const expiresAt = job.expiresAt || 0;
  return !(expiresAt > 0 && expiresAt < now);
}

async function run() {
  const snapshot = await db.collection('jobs').get();
  const now = Date.now();

  let openAndNotExpired = 0;
  let withValidCoords = 0;
  const within10 = [];
  const within15 = [];
  const topLocations = new Map();

  snapshot.forEach((doc) => {
    const job = doc.data();
    if (!isOpen(job) || !isNotExpired(job, now)) return;

    openAndNotExpired += 1;

    const locationText = String(job.addressText || job.locationText || (typeof job.location === 'string' ? job.location : '') || '').trim();
    if (locationText) {
      topLocations.set(locationText, (topLocations.get(locationText) || 0) + 1);
    }

    const coords = getLatLng(job);
    if (!coords) return;

    withValidCoords += 1;
    const km = distanceKm(KHAMMAM.lat, KHAMMAM.lng, coords.lat, coords.lng);

    const summary = {
      id: doc.id,
      title: String(job.title || ''),
      company: String(job.companyName || ''),
      location: locationText,
      km: Number(km.toFixed(2)),
      expiresAt: Number(job.expiresAt || 0)
    };

    if (km <= 10) within10.push(summary);
    if (km <= 15) within15.push(summary);
  });

  within10.sort((a, b) => a.km - b.km);
  within15.sort((a, b) => a.km - b.km);

  const locationsSorted = Array.from(topLocations.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 12);

  console.log(`open_not_expired=${openAndNotExpired}`);
  console.log(`with_valid_coords=${withValidCoords}`);
  console.log(`within_10km=${within10.length}`);
  console.log(`within_15km=${within15.length}`);

  console.log('\nTop locations among open jobs:');
  locationsSorted.forEach(([location, count]) => {
    console.log(`${count} | ${location}`);
  });

  console.log('\nSample jobs within 15km:');
  within15.slice(0, 20).forEach((j) => {
    console.log(`${j.km}km | ${j.title} | ${j.company} | ${j.location}`);
  });

  process.exit(0);
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
