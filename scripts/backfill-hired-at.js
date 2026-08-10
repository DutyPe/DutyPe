/**
 * One-off backfill: stamps hiredAt on applications that were already sitting in hired
 * before auto-completion shipped.
 *
 * Without hiredAt the scheduled sweep cannot see them, so they would stay hired forever.
 *
 * hiredAt is set to NOW rather than backdated on purpose:
 *   - backdating would make the very next sweep close everything at once
 *   - the sweep also ignores anything older than 30 days, so a real backdate would
 *     simply be skipped and the backfill would do nothing
 * Setting it to now gives each record the normal grace period from the moment of backfill.
 *
 * autoCompleteSilent is set so the sweep closes these without notifying workers about
 * jobs that may be months old.
 *
 * Usage:
 *   node scripts/backfill-hired-at.js                  # dry run, changes nothing
 *   node scripts/backfill-hired-at.js --apply          # writes
 *   node scripts/backfill-hired-at.js --apply --limit 500
 *   node scripts/backfill-hired-at.js --apply --notify # let workers be notified
 *
 * Requires GOOGLE_APPLICATION_CREDENTIALS or `firebase login:ci` style admin access.
 */

const admin = require("firebase-admin");

const args = process.argv.slice(2);
const APPLY = args.includes("--apply");
const NOTIFY = args.includes("--notify");
const limitArg = args.indexOf("--limit");
const LIMIT = limitArg !== -1 ? parseInt(args[limitArg + 1], 10) : Infinity;
const BATCH_SIZE = 400;

const ACTIVE_STATUSES = ["hired", "accepted", "in_progress"];

if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();

async function main() {
  console.log(APPLY ? "MODE: APPLY (will write)" : "MODE: DRY RUN (no writes)");
  console.log(`notify workers on completion: ${NOTIFY}`);
  console.log(`limit: ${LIMIT === Infinity ? "none" : LIMIT}`);
  console.log("");

  let scanned = 0;
  let needsBackfill = 0;
  let alreadyStamped = 0;
  let written = 0;
  const byStatus = {};

  for (const status of ACTIVE_STATUSES) {
    let last = null;

    for (;;) {
      if (needsBackfill >= LIMIT) break;

      let q = db.collection("applications")
        .where("status", "==", status)
        .orderBy(admin.firestore.FieldPath.documentId())
        .limit(BATCH_SIZE);
      if (last) q = q.startAfter(last);

      const snap = await q.get();
      if (snap.empty) break;
      last = snap.docs[snap.docs.length - 1].id;
      scanned += snap.size;

      const batch = db.batch();
      let inBatch = 0;

      for (const doc of snap.docs) {
        if (doc.get("hiredAt")) { alreadyStamped++; continue; }
        if (needsBackfill >= LIMIT) break;

        needsBackfill++;
        byStatus[status] = (byStatus[status] || 0) + 1;

        if (APPLY) {
          const update = { hiredAt: admin.firestore.FieldValue.serverTimestamp() };
          if (!NOTIFY) update.autoCompleteSilent = true;
          update.hiredAtBackfilled = true;
          batch.set(doc.ref, update, { merge: true });
          inBatch++;
        }
      }

      if (APPLY && inBatch > 0) {
        await batch.commit();
        written += inBatch;
        console.log(`  committed ${inBatch} (running total ${written})`);
      }

      if (snap.size < BATCH_SIZE) break;
    }
  }

  console.log("");
  console.log(`scanned              : ${scanned}`);
  console.log(`already had hiredAt  : ${alreadyStamped}`);
  console.log(`needing backfill     : ${needsBackfill}`);
  for (const s of Object.keys(byStatus)) {
    console.log(`   status=${s}: ${byStatus[s]}`);
  }
  console.log(`written              : ${written}`);
  console.log("");

  if (!APPLY && needsBackfill > 0) {
    const hours = 6;
    const perRun = 200;
    const runsPerHour = 2;
    const drainHours = Math.ceil(needsBackfill / (perRun * runsPerHour));
    console.log(`If applied: these become eligible in ${hours}h, then drain at`);
    console.log(`${perRun} per 30min, so roughly ${drainHours}h to clear ${needsBackfill}.`);
    console.log("");
    console.log("Re-run with --apply to write.");
  }
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("backfill failed:", err);
    process.exit(1);
  });
