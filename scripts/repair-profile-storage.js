const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const APPLY = process.argv.includes('--apply');
const DELETE_LEGACY_AVAILABILITY = process.argv.includes('--delete-legacy-availability');
const BATCH_LIMIT = 450;
const BASE32 = '0123456789bcdefghjkmnpqrstuvwxyz';

admin.initializeApp({ credential: admin.credential.cert(loadServiceAccount()) });

const db = admin.firestore();
const FieldValue = admin.firestore.FieldValue;

function nonBlankString(value) {
  return typeof value === 'string' && value.trim().length > 0 ? value.trim() : '';
}

function normalizePhone(value) {
  return nonBlankString(value).replace(/[\s()-]/g, '');
}

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

function validLocation(data, fieldName) {
  const value = data[fieldName];
  if (!value || typeof value !== 'object') return null;
  const lat = value.lat;
  const lng = value.lng;
  return hasValidCoordinates(lat, lng) ? { lat, lng } : null;
}

function queueSet(ops, ref, data) {
  if (Object.keys(data).length === 0) return;
  ops.push({ type: 'set', ref, data });
}

function queueUpdate(ops, ref, data) {
  if (Object.keys(data).length === 0) return;
  ops.push({ type: 'update', ref, data });
}

async function commitOps(ops) {
  if (!APPLY || ops.length === 0) return 0;
  let committed = 0;
  for (let index = 0; index < ops.length; index += BATCH_LIMIT) {
    const batch = db.batch();
    const chunk = ops.slice(index, index + BATCH_LIMIT);
    for (const op of chunk) {
      if (op.type === 'set') {
        batch.set(op.ref, op.data, { merge: true });
      } else {
        batch.update(op.ref, op.data);
      }
    }
    await batch.commit();
    committed += chunk.length;
  }
  return committed;
}

async function loadProfileName(uid, role) {
  if (!uid) return '';
  const collections = role === 'EMPLOYER'
    ? ['employer_profiles', 'worker_profiles']
    : ['worker_profiles', 'employer_profiles'];

  for (const collectionName of collections) {
    const snap = await db.collection(collectionName).doc(uid).get();
    if (!snap.exists) continue;
    const data = snap.data() || {};
    return nonBlankString(data.fullName) || nonBlankString(data.companyName);
  }

  return '';
}

async function loadPhoneRoleIndex() {
  const snap = await db.collection('phoneRoles').get();
  const byUid = new Map();
  for (const doc of snap.docs) {
    const data = doc.data() || {};
    const uid = nonBlankString(data.uid);
    if (!uid) continue;
    byUid.set(uid, {
      phoneNumber: nonBlankString(data.phoneNumber) || doc.id,
      name: nonBlankString(data.name),
      role: nonBlankString(data.role).toUpperCase()
    });
  }
  return byUid;
}

async function loadAuthPhone(uid) {
  try {
    const user = await admin.auth().getUser(uid);
    return normalizePhone(user.phoneNumber);
  } catch (_) {
    return '';
  }
}

function buildPhoneRoleRepair(phone, role, uid, name) {
  const data = {
    phoneNumber: phone,
    role,
    uid,
    updatedAt: FieldValue.serverTimestamp()
  };
  if (name) data.name = name;
  return data;
}

async function auditPhoneRoles() {
  const snap = await db.collection('phoneRoles').get();
  const ops = [];
  const summary = {
    total: snap.size,
    missingName: 0,
    missingRole: 0,
    missingUid: 0,
    missingPhoneNumber: 0,
    phoneNumberMismatch: 0,
    repairedNameFromProfile: 0,
    repairedPhoneNumber: 0
  };

  for (const doc of snap.docs) {
    const data = doc.data() || {};
    const role = nonBlankString(data.role).toUpperCase();
    const uid = nonBlankString(data.uid);
    const phoneNumber = nonBlankString(data.phoneNumber);
    const updates = {};

    if (!nonBlankString(data.name)) {
      summary.missingName += 1;
      const profileName = await loadProfileName(uid, role);
      if (profileName) {
        updates.name = profileName;
        summary.repairedNameFromProfile += 1;
      }
    }
    if (!role) summary.missingRole += 1;
    if (!uid) summary.missingUid += 1;
    if (!phoneNumber) {
      summary.missingPhoneNumber += 1;
      updates.phoneNumber = doc.id;
      summary.repairedPhoneNumber += 1;
    } else if (phoneNumber !== doc.id) {
      summary.phoneNumberMismatch += 1;
      updates.phoneNumber = doc.id;
      summary.repairedPhoneNumber += 1;
    }

    queueSet(ops, doc.ref, updates);
  }

  summary.committedWrites = await commitOps(ops);
  return summary;
}

async function auditWorkerProfiles(phoneRoleByUid) {
  const snap = await db.collection('worker_profiles').get();
  const ops = [];
  const summary = {
    total: snap.size,
    missingFullName: 0,
    missingPhone: 0,
    wrongOrMissingRole: 0,
    validLocationMissingGeohash: 0,
    legacyIsAvailable: 0,
    repairedFullNameFromPhoneRole: 0,
    repairedPhoneFromPhoneRole: 0,
    repairedPhoneFromAuth: 0,
    createdPhoneRoleFromAuth: 0,
    repairedRole: 0,
    repairedGeohash: 0,
    deletedLegacyIsAvailable: 0
  };

  for (const doc of snap.docs) {
    const data = doc.data() || {};
    const phoneRole = phoneRoleByUid.get(doc.id) || {};
    const updates = {};
    const location = validLocation(data, 'location');

    if (!nonBlankString(data.fullName)) {
      summary.missingFullName += 1;
      if (phoneRole.name) {
        updates.fullName = phoneRole.name;
        summary.repairedFullNameFromPhoneRole += 1;
      }
    }
    if (!nonBlankString(data.phone)) {
      summary.missingPhone += 1;
      if (phoneRole.phoneNumber) {
        updates.phone = phoneRole.phoneNumber;
        summary.repairedPhoneFromPhoneRole += 1;
      } else {
        const authPhone = await loadAuthPhone(doc.id);
        if (authPhone) {
          updates.phone = authPhone;
          summary.repairedPhoneFromAuth += 1;
          queueSet(
            ops,
            db.collection('phoneRoles').doc(authPhone),
            buildPhoneRoleRepair(authPhone, 'WORKER', doc.id, nonBlankString(data.fullName) || phoneRole.name)
          );
          summary.createdPhoneRoleFromAuth += 1;
        }
      }
    }
    if (nonBlankString(data.role).toUpperCase() !== 'WORKER') {
      summary.wrongOrMissingRole += 1;
      updates.role = 'WORKER';
      summary.repairedRole += 1;
    }

    if (location) {
      const expectedGeohash = encodeGeohash(location.lat, location.lng);
      if (nonBlankString(data.geohash) !== expectedGeohash) {
        summary.validLocationMissingGeohash += 1;
        updates.geohash = expectedGeohash;
        summary.repairedGeohash += 1;
      }
    }

    if (Object.prototype.hasOwnProperty.call(data, 'isAvailable')) {
      summary.legacyIsAvailable += 1;
      if (DELETE_LEGACY_AVAILABILITY) {
        updates.isAvailable = FieldValue.delete();
        summary.deletedLegacyIsAvailable += 1;
      }
    }

    queueUpdate(ops, doc.ref, updates);
  }

  summary.committedWrites = await commitOps(ops);
  return summary;
}

async function auditEmployerProfiles(phoneRoleByUid) {
  const snap = await db.collection('employer_profiles').get();
  const ops = [];
  const summary = {
    total: snap.size,
    missingFullName: 0,
    missingCompanyName: 0,
    missingPhone: 0,
    wrongOrMissingRole: 0,
    validBusinessLocationMissingGeohash: 0,
    repairedFullNameFromCompanyName: 0,
    repairedFullNameFromPhoneRole: 0,
    repairedCompanyNameFromFullName: 0,
    repairedPhoneFromPhoneRole: 0,
    repairedPhoneFromAuth: 0,
    createdPhoneRoleFromAuth: 0,
    repairedRole: 0,
    repairedGeohash: 0
  };

  for (const doc of snap.docs) {
    const data = doc.data() || {};
    const phoneRole = phoneRoleByUid.get(doc.id) || {};
    const fullName = nonBlankString(data.fullName);
    const companyName = nonBlankString(data.companyName);
    const updates = {};
    const businessLocation = validLocation(data, 'businessLocation');

    if (!fullName) {
      summary.missingFullName += 1;
      if (companyName) {
        updates.fullName = companyName;
        summary.repairedFullNameFromCompanyName += 1;
      } else if (phoneRole.name) {
        updates.fullName = phoneRole.name;
        summary.repairedFullNameFromPhoneRole += 1;
      }
    }
    if (!companyName) {
      summary.missingCompanyName += 1;
      if (fullName) {
        updates.companyName = fullName;
        summary.repairedCompanyNameFromFullName += 1;
      }
    }
    if (!nonBlankString(data.phone)) {
      summary.missingPhone += 1;
      if (phoneRole.phoneNumber) {
        updates.phone = phoneRole.phoneNumber;
        summary.repairedPhoneFromPhoneRole += 1;
      } else {
        const authPhone = await loadAuthPhone(doc.id);
        if (authPhone) {
          updates.phone = authPhone;
          summary.repairedPhoneFromAuth += 1;
          queueSet(
            ops,
            db.collection('phoneRoles').doc(authPhone),
            buildPhoneRoleRepair(authPhone, 'EMPLOYER', doc.id, fullName || companyName || phoneRole.name)
          );
          summary.createdPhoneRoleFromAuth += 1;
        }
      }
    }
    if (nonBlankString(data.role).toUpperCase() !== 'EMPLOYER') {
      summary.wrongOrMissingRole += 1;
      updates.role = 'EMPLOYER';
      summary.repairedRole += 1;
    }

    if (businessLocation) {
      const expectedGeohash = encodeGeohash(businessLocation.lat, businessLocation.lng);
      if (nonBlankString(data.geohash) !== expectedGeohash) {
        summary.validBusinessLocationMissingGeohash += 1;
        updates.geohash = expectedGeohash;
        summary.repairedGeohash += 1;
      }
    }

    queueUpdate(ops, doc.ref, updates);
  }

  summary.committedWrites = await commitOps(ops);
  return summary;
}

async function main() {
  const phoneRoles = await auditPhoneRoles();
  const phoneRoleByUid = await loadPhoneRoleIndex();
  const summary = {
    mode: APPLY ? 'apply' : 'dry-run',
    deleteLegacyAvailability: DELETE_LEGACY_AVAILABILITY,
    generatedAt: new Date().toISOString(),
    phoneRoles,
    workerProfiles: await auditWorkerProfiles(phoneRoleByUid),
    employerProfiles: await auditEmployerProfiles(phoneRoleByUid)
  };

  console.log(JSON.stringify(summary, null, 2));
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});