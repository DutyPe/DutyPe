const fs = require('fs');
const path = require('path');

function loadServiceAccount() {
  const inlineJson = process.env.FIREBASE_ADMIN_SERVICE_ACCOUNT_JSON;
  if (inlineJson) {
    return normalizeServiceAccount(JSON.parse(inlineJson));
  }

  const fromParts = normalizeServiceAccount({
    projectId: process.env.FIREBASE_ADMIN_PROJECT_ID || process.env.FIREBASE_PROJECT_ID || 'dutype-860ac',
    clientEmail: process.env.FIREBASE_ADMIN_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_ADMIN_PRIVATE_KEY
  });
  if (fromParts) return fromParts;

  const targetProjectId =
    process.env.FIREBASE_ADMIN_PROJECT_ID ||
    process.env.FIREBASE_PROJECT_ID ||
    'dutype-860ac';

  const explicitPath = process.env.FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH;
  const repoRoot = path.join(__dirname, '..', '..');
  const scriptsRoot = path.join(__dirname, '..');
  const candidatePaths = [
    explicitPath,
    path.join(repoRoot, 'serviceAccountKey.json'),
    path.join(scriptsRoot, 'serviceAccountKey.json'),
    path.join(scriptsRoot, 'dutype-860ac-firebase-adminsdk.json')
  ].filter(Boolean);

  for (const candidatePath of candidatePaths) {
    const resolvedPath = path.isAbsolute(candidatePath)
      ? candidatePath
      : path.resolve(candidatePath);

    if (fs.existsSync(resolvedPath)) {
      return normalizeServiceAccount(JSON.parse(fs.readFileSync(resolvedPath, 'utf8')));
    }
  }

  const scanDirs = [repoRoot, scriptsRoot];
  for (const dir of scanDirs) {
    if (!fs.existsSync(dir)) continue;
    const jsonFiles = fs
      .readdirSync(dir)
      .filter((name) => name.endsWith('.json'))
      .filter((name) => name === 'serviceAccountKey.json' || name.includes('firebase-adminsdk'));

    for (const fileName of jsonFiles) {
      const fullPath = path.join(dir, fileName);
      try {
        const parsed = JSON.parse(fs.readFileSync(fullPath, 'utf8'));
        const projectId = parsed.project_id || parsed.projectId;
        const isServiceAccount = parsed.type === 'service_account' && parsed.private_key;

        if (!isServiceAccount) continue;
        if (!projectId || projectId === targetProjectId) {
          return normalizeServiceAccount(parsed);
        }
      } catch (_) {
        // Ignore invalid JSON candidates and continue scanning.
      }
    }
  }

  throw new Error(
    `No Firebase service account file was found for project '${targetProjectId}'. ` +
      'Set FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH or FIREBASE_ADMIN_SERVICE_ACCOUNT_JSON.'
  );
}

function normalizeServiceAccount(raw) {
  if (!raw || typeof raw !== 'object') return null;

  const projectId = raw.project_id || raw.projectId;
  const clientEmail = raw.client_email || raw.clientEmail;
  const privateKey = String(raw.private_key || raw.privateKey || '').replace(/\\n/g, '\n');

  if (!projectId || !clientEmail || !privateKey) return null;

  return {
    ...raw,
    project_id: projectId,
    client_email: clientEmail,
    private_key: privateKey
  };
}

module.exports = {
  loadServiceAccount
};
