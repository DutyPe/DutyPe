const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const DRY_RUN = process.argv.includes('--dry-run');

function hasValidCoordinates(lat, lng) {
  return (
    typeof lat === 'number' &&
    typeof lng === 'number' &&
    lat >= -90 &&
    lat <= 90 &&
    lng >= -180 &&
    lng <= 180 &&
    !(lat === 0 && lng === 0)
  );
}

const BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';

function encodeGeohash(lat, lon, precision = 6) {
  let latMin = -90.0;
  let latMax = 90.0;
  let lonMin = -180.0;
  let lonMax = 180.0;

  let geohash = '';
  let isEven = true;
  let bit = 0;
  let ch = 0;

  while (geohash.length < precision) {
    if (isEven) {
      const mid = (lonMin + lonMax) / 2;
      if (lon > mid) {
        ch |= 1 << (4 - bit);
        lonMin = mid;
      } else {
        lonMax = mid;
      }
    } else {
      const mid = (latMin + latMax) / 2;
      if (lat > mid) {
        ch |= 1 << (4 - bit);
        latMin = mid;
      } else {
        latMax = mid;
      }
    }

    isEven = !isEven;

    if (bit < 4) {
      bit += 1;
    } else {
      geohash += BASE32[ch];
      bit = 0;
      ch = 0;
    }
  }

  return geohash;
}

function asBool(value, fallback) {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  if (typeof value === 'string') return value.toLowerCase() === 'true' || value === '1';
  return fallback;
}

function computeUpdates(data) {
  const updates = {};

  const locationMap = data.location && typeof data.location === 'object' ? data.location : null;
  const lat = Number.isFinite(locationMap?.lat)
    ? locationMap.lat
    : Number.isFinite(data.latitude)
      ? data.latitude
      : null;
  const lng = Number.isFinite(locationMap?.lng)
    ? locationMap.lng
    : Number.isFinite(data.longitude)
      ? data.longitude
      : null;

  if (!hasValidCoordinates(lat, lng)) {
    return { updates: null, reason: 'noCoords' };
  }

  const existingGeohash = typeof data.geohash === 'string' ? data.geohash : '';
  if (!existingGeohash.trim()) {
    updates.geohash = encodeGeohash(lat, lng, 6);
  }

  const hasCanonicalLocation =
    locationMap && Number.isFinite(locationMap.lat) && Number.isFinite(locationMap.lng);
  if (!hasCanonicalLocation) {
    updates.location = { lat, lng };
  }

  const existingStatus = typeof data.status === 'string' ? data.status : '';
  if (!existingStatus.trim()) {
    const isActive = asBool(data.isActive, true);
    const isFilled = asBool(data.isFilled, false);
    updates.status = isActive && !isFilled ? 'open' : 'closed';
  }

  const currentJobType = typeof data.jobType === 'string' ? data.jobType.toUpperCase() : '';
  const employmentTypes = new Set([
    'FULL_TIME',
    'PART_TIME',
    'CONTRACT',
    'FREELANCE',
    'INTERNSHIP',
    'FULL-TIME',
    'PART-TIME'
  ]);

  if (employmentTypes.has(currentJobType)) {
    const categoryValue =
      (typeof data.category === 'string' ? data.category.toUpperCase() : '') ||
      (typeof data.industry === 'string' ? data.industry.toUpperCase() : '');

    if (categoryValue) {
      updates.jobType = categoryValue;
      updates.employmentType = currentJobType;
    }
  }

  if (Object.keys(updates).length === 0) {
    return { updates: null, reason: 'alreadyOk' };
  }

  return { updates, reason: 'updated' };
}

async function verify(db) {
  const snapshot = await db.collection('jobs').get();

  let total = 0;
  let missingGeohash = 0;
  let missingStatus = 0;
  let missingCanonicalLocation = 0;
  let invalidCoords = 0;

  snapshot.forEach((doc) => {
    total += 1;
    const data = doc.data() || {};

    const geohash = typeof data.geohash === 'string' ? data.geohash : '';
    if (!geohash.trim()) missingGeohash += 1;

    const status = typeof data.status === 'string' ? data.status : '';
    if (!status.trim()) missingStatus += 1;

    const locationMap = data.location && typeof data.location === 'object' ? data.location : null;
    const hasCanonicalLocation =
      locationMap && Number.isFinite(locationMap.lat) && Number.isFinite(locationMap.lng);
    if (!hasCanonicalLocation) missingCanonicalLocation += 1;

    const lat = Number.isFinite(locationMap?.lat)
      ? locationMap.lat
      : Number.isFinite(data.latitude)
        ? data.latitude
        : null;
    const lng = Number.isFinite(locationMap?.lng)
      ? locationMap.lng
      : Number.isFinite(data.longitude)
        ? data.longitude
        : null;

    if (!hasValidCoordinates(lat, lng)) invalidCoords += 1;
  });

  return {
    total,
    missingGeohash,
    missingStatus,
    missingCanonicalLocation,
    invalidCoords
  };
}

async function main() {
  const serviceAccount = loadServiceAccount();
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
  const db = admin.firestore();

  console.log('Starting strict-schema jobs backfill as Firebase Admin...');
  console.log(`Mode: ${DRY_RUN ? 'DRY RUN' : 'APPLY'}`);

  const snapshot = await db.collection('jobs').get();
  console.log(`Loaded ${snapshot.size} jobs`);

  let updated = 0;
  let skippedAlreadyOk = 0;
  let noCoords = 0;
  let failed = 0;

  let batch = db.batch();
  let batchOps = 0;
  const commitPromises = [];

  for (const doc of snapshot.docs) {
    try {
      const data = doc.data() || {};
      const result = computeUpdates(data);

      if (result.reason === 'noCoords') {
        noCoords += 1;
        continue;
      }
      if (result.reason === 'alreadyOk' || !result.updates) {
        skippedAlreadyOk += 1;
        continue;
      }

      if (!DRY_RUN) {
        batch.update(doc.ref, result.updates);
        batchOps += 1;

        if (batchOps >= 450) {
          commitPromises.push(batch.commit());
          batch = db.batch();
          batchOps = 0;
        }
      }

      updated += 1;
    } catch (error) {
      failed += 1;
      console.error(`Failed processing ${doc.id}:`, error.message);
    }
  }

  if (!DRY_RUN && batchOps > 0) {
    commitPromises.push(batch.commit());
  }

  if (!DRY_RUN && commitPromises.length > 0) {
    await Promise.all(commitPromises);
  }

  console.log('');
  console.log('Backfill summary:');
  console.log(`- Updated: ${updated}`);
  console.log(`- Already OK: ${skippedAlreadyOk}`);
  console.log(`- No valid coords: ${noCoords}`);
  console.log(`- Failed: ${failed}`);

  console.log('');
  console.log('Verifying strict fields after run...');
  const verification = await verify(db);
  console.log(`- Total jobs: ${verification.total}`);
  console.log(`- Missing geohash: ${verification.missingGeohash}`);
  console.log(`- Missing status: ${verification.missingStatus}`);
  console.log(`- Missing canonical location map: ${verification.missingCanonicalLocation}`);
  console.log(`- Invalid/no coordinates (cannot be fully backfilled): ${verification.invalidCoords}`);

  if (
    verification.missingGeohash === 0 &&
    verification.missingStatus === 0 &&
    verification.missingCanonicalLocation === 0
  ) {
    console.log('Verification PASS: strict fields are fully populated for all jobs.');
  } else {
    console.log('Verification PARTIAL: some jobs still miss fields (usually due to invalid/missing coordinates).');
  }
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error('Backfill failed:', error);
    process.exit(1);
  });
