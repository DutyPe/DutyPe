const fs = require('fs');
const path = require('path');

function loadServiceAccount() {
  const explicitPath = process.env.FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH;
  const candidatePaths = [
    explicitPath,
    path.join(__dirname, '..', '..', 'serviceAccountKey.json'),
    path.join(__dirname, '..', 'serviceAccountKey.json'),
    path.join(__dirname, '..', 'dutypeapp-firebase-adminsdk-fbsvc-695bd9746e.json')
  ].filter(Boolean);

  for (const candidatePath of candidatePaths) {
    const resolvedPath = path.isAbsolute(candidatePath)
      ? candidatePath
      : path.resolve(candidatePath);

    if (fs.existsSync(resolvedPath)) {
      return require(resolvedPath);
    }
  }

  throw new Error(
    'No Firebase service account file was found. Set FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH or place serviceAccountKey.json at the repo root.'
  );
}

module.exports = {
  loadServiceAccount
};
