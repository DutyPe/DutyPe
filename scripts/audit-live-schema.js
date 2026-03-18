const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

const SAMPLE_LIMIT = 200;

function flattenFields(obj, prefix = '', result = new Set()) {
  if (!obj || typeof obj !== 'object') return result;

  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    result.add(path);

    if (Array.isArray(value)) {
      if (value.length > 0 && typeof value[0] === 'object' && value[0] !== null) {
        flattenFields(value[0], `${path}[]`, result);
      }
    } else if (value && typeof value === 'object') {
      flattenFields(value, path, result);
    }
  }

  return result;
}

async function main() {
  const serviceAccount = loadServiceAccount();
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

  const db = admin.firestore();
  const collections = await db.listCollections();
  const report = [];

  for (const collectionRef of collections) {
    const [sampleSnapshot, countSnap] = await Promise.all([
      collectionRef.limit(SAMPLE_LIMIT).get(),
      collectionRef.count().get()
    ]);

    const fields = new Set();
    sampleSnapshot.forEach((doc) => flattenFields(doc.data(), '', fields));

    report.push({
      collection: collectionRef.id,
      count: countSnap.data().count,
      sampledDocs: sampleSnapshot.size,
      fields: [...fields].sort()
    });
  }

  report.sort((a, b) => b.count - a.count);

  console.log(JSON.stringify({ generatedAt: new Date().toISOString(), sampleLimit: SAMPLE_LIMIT, collections: report }, null, 2));
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
