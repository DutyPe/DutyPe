const fs = require('node:fs');
const path = require('node:path');
const { createHash } = require('node:crypto');
const { parseArgs } = require('node:util');
const { createRequire } = require('node:module');
const root = path.resolve(__dirname, '..');
const demoProject = 'demo-dutype-android-fixes';

function hash(value) {
  return createHash('sha256').update(JSON.stringify(value)).digest('hex');
}

function parse(argv) {
  const { values, positionals } = parseArgs({ args: argv, allowPositionals: true, options: {
    environment: { type: 'string', default: 'emulator' }, project: { type: 'string', default: demoProject },
    kind: { type: 'string' }, out: { type: 'string' }, plan: { type: 'string' },
    'page-size': { type: 'string', default: '50' }, after: { type: 'string' },
    'review-sha': { type: 'string' }, 'confirm-project': { type: 'string' },
    'confirm-staging': { type: 'string' }, 'maintenance-ack': { type: 'boolean', default: false },
    help: { type: 'boolean', default: false }
  } });
  if (values.help) return { help: true };
  if (positionals.length !== 1 || !['plan', 'apply'].includes(positionals[0])) throw new Error('Use plan or apply; no implicit write mode is supported.');
  if (!['emulator', 'staging'].includes(values.environment)) throw new Error('Only emulator or explicitly confirmed staging targets are supported.');
  if (!/^[a-z][a-z0-9-]{5,62}$/.test(values.project)) throw new Error('Invalid project ID.');
  const productionConfig = JSON.parse(fs.readFileSync(path.join(root, 'app/google-services.json'), 'utf8'));
  if (values.project === productionConfig.project_info.project_id) throw new Error('Refusing the Android production Firebase project.');
  if (values.environment === 'emulator' && values.project !== demoProject) throw new Error('Local migrations are restricted to the isolated demo project.');
  if (values.environment === 'staging' && (values.project.startsWith('demo-') || values['confirm-staging'] !== values.project || process.env.FIRESTORE_EMULATOR_HOST)) {
    throw new Error('A real staging ID, matching --confirm-staging, and no emulator environment are required.');
  }
  const pageSize = Number(values['page-size']);
  if (!Number.isInteger(pageSize) || pageSize < 1 || pageSize > 100) throw new Error('Page size must be between 1 and 100.');
  if (!values.out) throw new Error('--out is required; reports are never overwritten.');
  const output = path.resolve(values.out);
  if (fs.existsSync(output)) throw new Error('Output already exists. Choose a new report path.');
  if (positionals[0] === 'apply' && (!values.plan || !values['review-sha'] || !values['maintenance-ack'] || values['confirm-project'] !== values.project)) {
    throw new Error('Apply requires --plan, --review-sha, --maintenance-ack, and --confirm-project matching the explicit target.');
  }
  if (positionals[0] === 'plan' && !['profiles', 'orphan-profiles', 'users', 'jobs'].includes(values.kind)) throw new Error('Choose --kind profiles, orphan-profiles, users, or jobs.');
  return { ...values, command: positionals[0], pageSize, output };
}

async function main(argv = process.argv.slice(2)) {
  const options = parse(argv);
  if (options.help) {
    console.log('Dry-run: node scripts/android-data-migration.cjs plan --kind profiles --out app/build/reports/migration/profiles-1.json');
    console.log('Resume with --after <nextCursor>. Review the JSON and its SHA before apply. Authoritative balances are never changed.');
    console.log('Apply additionally requires --plan <file> --review-sha <sha> --confirm-project <id> --maintenance-ack.');
    console.log('Remote staging requires --environment staging --project <id> --confirm-staging <id>; production is refused.');
    return;
  }
  let reviewed;
  if (options.command === 'apply') {
    reviewed = JSON.parse(fs.readFileSync(path.resolve(options.plan), 'utf8'));
    const { reviewSha, ...body } = reviewed;
    if (reviewed.schemaVersion !== 1 || reviewed.project !== options.project || reviewed.environment !== options.environment ||
        reviewSha !== options['review-sha'] || hash(body) !== reviewSha || !Array.isArray(reviewed.page?.items) || reviewed.page.items.length > 100) {
      throw new Error('Review SHA, plan schema, or target does not match. Nothing was written.');
    }
    if (reviewed.page.items.some(item => item.issues?.length)) throw new Error('This page has unresolved reconciliation issues; review and repair the source before applying.');
    if (!['profiles', 'orphan-profiles', 'users', 'jobs'].includes(reviewed.kind)) throw new Error('Unsupported reviewed plan.');
  }
  if (options.environment === 'emulator') {
    process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8185';
    process.env.GCLOUD_PROJECT = options.project;
  }
  const requireBackend = createRequire(path.join(root, 'functions/package.json'));
  const admin = requireBackend('firebase-admin');
  const app = admin.initializeApp({ projectId: options.project });
  const db = app.firestore();
  let outputDescriptor;
  try {
    const migration = require(path.join(root, 'functions/lib/migration.js'));
    fs.mkdirSync(path.dirname(options.output), { recursive: true });
    outputDescriptor = fs.openSync(options.output, 'wx');
    if (options.command === 'plan') {
      const paging = { pageSize: options.pageSize, afterId: options.after };
      const page = options.kind === 'profiles' || options.kind === 'orphan-profiles'
        ? await migration.backfillPublicProfilePage(db, { ...paging, collection: options.kind === 'profiles' ? 'users' : 'public_profiles' })
        : await migration.reconciliationPage(db, options.kind, paging);
      const report = { schemaVersion: 1, environment: options.environment, project: options.project, kind: options.kind,
        createdAt: new Date().toISOString(), page };
      const reviewSha = hash(report);
      fs.writeFileSync(outputDescriptor, JSON.stringify({ ...report, reviewSha }, null, 2) + '\n');
      console.log(JSON.stringify({ mode: 'dry-run', scanned: page.scanned, blocked: page.items.filter(item => item.issues?.length).length,
        nextCursor: page.nextCursor, reviewSha, output: options.output }));
    } else {
      const runId = `migration-${options['review-sha'].slice(0, 32)}`;
      const results = [];
      for (const item of reviewed.page.items) {
        const result = reviewed.kind === 'profiles' || reviewed.kind === 'orphan-profiles'
          ? await migration.applyPublicProfileItem(db, item, runId)
          : await migration.applyReconciliationItem(db, item, runId);
        results.push(result);
        fs.writeSync(outputDescriptor, JSON.stringify({ runId, ...result }) + '\n');
        fs.fsyncSync(outputDescriptor);
        if (result.status === 'blocked') throw new Error('An item became blocked. Stop and generate a new dry run; completed items have audit receipts.');
      }
      console.log(JSON.stringify({ mode: 'apply', runId, processed: results.length, output: options.output }));
    }
  } finally {
    if (outputDescriptor !== undefined) fs.closeSync(outputDescriptor);
    await db.terminate();
    await app.delete();
  }
}

if (require.main === module) main().catch(error => { console.error(error.message); process.exitCode = 1; });
module.exports = { parse, hash };