/**
 * Promote or demote a Firebase Auth user with DutyPe admin custom claims.
 *
 * Usage:
 *   node scripts/set-admin-claims.js --email admin@dutype.com
 *   node scripts/set-admin-claims.js --uid USER_UID
 *   node scripts/set-admin-claims.js --email admin@dutype.com --remove
 */

const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

function readArg(flag) {
  const index = process.argv.indexOf(flag);
  if (index === -1) {
    return null;
  }

  return process.argv[index + 1] ?? null;
}

function hasFlag(flag) {
  return process.argv.includes(flag);
}

async function resolveUser(auth) {
  const uid = readArg('--uid');
  const email = readArg('--email');

  if (uid) {
    return auth.getUser(uid);
  }

  if (email) {
    return auth.getUserByEmail(email);
  }

  throw new Error('Pass either --uid USER_UID or --email user@example.com.');
}

async function main() {
  const shouldRemove = hasFlag('--remove');
  const serviceAccount = loadServiceAccount();

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: serviceAccount.project_id || serviceAccount.projectId || 'dutypeapp'
  });

  const auth = admin.auth();
  const userRecord = await resolveUser(auth);
  const nextClaims = shouldRemove ? {} : { admin: true, role: 'ADMIN' };

  await auth.setCustomUserClaims(userRecord.uid, nextClaims);

  console.log(
    shouldRemove
      ? `Removed admin custom claims for ${userRecord.email || userRecord.uid}.`
      : `Set admin custom claims for ${userRecord.email || userRecord.uid}: ${JSON.stringify(nextClaims)}`
  );
  console.log('Ask the user to sign out and sign in again so refreshed ID tokens pick up the claim change.');
}

main().catch((error) => {
  console.error(error.message || error);
  process.exitCode = 1;
});
