#!/usr/bin/env node
/**
 * backfillPhoneRoles.js - ADMIN one-off backfill of `phoneRoles/{phoneE164}`.
 *
 * WHY
 *   The app's login/registration pre-check reads `phoneRoles/{phone}` directly (one cheap
 *   document read). Legacy accounts created before that collection existed have no such doc,
 *   so the client has to fall back to the `lookupPhoneRole` Cloud Function (slower, cold starts).
 *   This script creates the missing docs so the fallback is never needed.
 *
 * WHAT IT DOES
 *   For every doc in worker_profiles, employer_profiles and (legacy) users it derives
 *   { phone, role, uid, name } and creates `phoneRoles/{phone}` IF AND ONLY IF that doc does not
 *   exist yet. It never overwrites or deletes anything (writes use create(), so it is
 *   idempotent and safe to re-run or run concurrently with the live app).
 *
 *   Doc-id normalization is identical to functions/src/identity-mirror.ts `normalizePhone`
 *   (which matches the client PhoneNumberUtils.normalize for real E.164 Firebase Auth numbers):
 *     "+919876543210" -> "+919876543210"   (already E.164)
 *     "9876543210"    -> "+919876543210"   (10 digits -> India)
 *     "919876543210"  -> "+919876543210"   (7..15 digits -> "+" + digits)
 *   Values that cannot be normalized are reported and skipped.
 *
 *   Conflicts (same phone owned by a different uid or a different role, in the sources or versus
 *   an existing phoneRoles doc) are NEVER auto-resolved: they are counted and printed for manual
 *   review.
 *
 *   Pagination is by document id (orderBy __name__, startAfter), pages of --page-size docs,
 *   writes are batched (<= 400 per batch, hard cap below Firestore's 500).
 *
 * NOTE: creating phoneRoles docs fires the `mirrorPhoneRoleIdentity` Cloud Function (merges into
 *   users/{uid} and fills a missing profile). That is harmless/idempotent but means N extra
 *   function invocations; run off-peak for large datasets.
 *
 * USAGE (from the functions/ directory; needs Firestore admin access)
 *   1. gcloud auth application-default login          (or set GOOGLE_APPLICATION_CREDENTIALS
 *                                                       to a service-account key; never commit it)
 *   2. node scripts/backfillPhoneRoles.js --project <firebase-project-id> --dry-run
 *        -> prints counts + a sample of what WOULD be created; writes nothing.
 *   3. node scripts/backfillPhoneRoles.js --project <firebase-project-id>
 *        -> performs the backfill.
 *   Options:
 *     --project <id>     Firebase project id (defaults to GCLOUD_PROJECT / .firebaserc ADC project)
 *     --dry-run          Do not write anything
 *     --page-size <n>    Docs read per page (default 300, max 500)
 *     --only <name>      Restrict to one source: worker_profiles | employer_profiles | users
 *   Emulator: FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node scripts/backfillPhoneRoles.js --dry-run
 *
 * Re-running after a completed run must report created=0.
 */
"use strict";

const admin = require("firebase-admin");

const PHONE_ROLES = "phoneRoles";
const MAX_BATCH = 400; // Firestore limit is 500; keep headroom.
const GETALL_CHUNK = 300;

// Source collections, in priority order (profiles are authoritative over the legacy users doc).
const SOURCES = [
  { name: "worker_profiles", fixedRole: "WORKER" },
  { name: "employer_profiles", fixedRole: "EMPLOYER" },
  { name: "users", fixedRole: null },
];

// ---------------------------------------------------------------------------
// args
// ---------------------------------------------------------------------------
function parseArgs(argv) {
  const args = { dryRun: false, pageSize: 300, project: undefined, only: undefined };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--dry-run") args.dryRun = true;
    else if (a === "--project") args.project = argv[++i];
    else if (a === "--page-size") args.pageSize = Math.min(500, Math.max(1, parseInt(argv[++i], 10) || 300));
    else if (a === "--only") args.only = argv[++i];
    else if (a === "--help" || a === "-h") {
      console.log("See the header comment of this file for usage.");
      process.exit(0);
    } else {
      console.error(`Unknown argument: ${a}`);
      process.exit(2);
    }
  }
  if (args.only && !SOURCES.some((s) => s.name === args.only)) {
    console.error(`--only must be one of: ${SOURCES.map((s) => s.name).join(", ")}`);
    process.exit(2);
  }
  return args;
}

// ---------------------------------------------------------------------------
// normalization (mirrors functions/src/identity-mirror.ts)
// ---------------------------------------------------------------------------
function cleanString(v) {
  return typeof v === "string" ? v.trim() : "";
}
function firstNonEmpty(...vals) {
  for (const v of vals) {
    const t = cleanString(v);
    if (t) return t;
  }
  return "";
}
function normalizePhone(value) {
  const text = cleanString(typeof value === "number" ? String(value) : value);
  if (!text) return "";
  if (/^\+[1-9]\d{6,14}$/.test(text)) return text;
  const digits = text.replace(/\D/g, "");
  if (digits.length === 10) return `+91${digits}`;
  if (digits.length >= 7 && digits.length <= 15) return `+${digits}`;
  return "";
}
function normalizeRole(v) {
  const r = cleanString(v).toUpperCase();
  return r === "WORKER" || r === "EMPLOYER" ? r : "";
}

function candidateFromDoc(source, doc) {
  const d = doc.data() || {};
  const role = source.fixedRole || normalizeRole(firstNonEmpty(d.role, d.activeRole));
  const rawPhone = firstNonEmpty(d.phone, d.phoneNumber) || (typeof d.phone === "number" ? String(d.phone) : "");
  const phone = normalizePhone(rawPhone);
  const uid = firstNonEmpty(d.userId, d.uid) || doc.id;
  return {
    rawPhone,
    phone,
    role,
    uid,
    name: firstNonEmpty(d.fullName, d.name, d.companyName),
    createdAt: d.createdAt && typeof d.createdAt.toDate === "function" ? d.createdAt : null,
    referralCode: firstNonEmpty(d.referralCode),
    referredByCode: firstNonEmpty(d.referredByCode),
    referredByUserId: firstNonEmpty(d.referredByUserId),
    source: source.name,
    docId: doc.id,
  };
}

function buildPayload(c) {
  const payload = {
    phoneNumber: c.phone,
    uid: c.uid,
    role: c.role,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    createdAt: c.createdAt || admin.firestore.FieldValue.serverTimestamp(),
    backfilledFrom: c.source,
  };
  if (c.name) payload.name = c.name;
  if (c.referralCode) payload.referralCode = c.referralCode;
  if (c.referredByCode) payload.referredByCode = c.referredByCode;
  if (c.referredByUserId) payload.referredByUserId = c.referredByUserId;
  return payload;
}

// ---------------------------------------------------------------------------
// main
// ---------------------------------------------------------------------------
async function main() {
  const args = parseArgs(process.argv.slice(2));
  const initOpts = args.project ? { projectId: args.project } : {};
  admin.initializeApp(initOpts);
  const db = admin.firestore();

  const stats = {
    scanned: 0,
    noPhone: 0,
    badPhone: 0,
    noRole: 0,
    alreadyExists: 0,
    created: 0,
    conflictsInSources: 0,
    conflictsWithExisting: 0,
    raceSkipped: 0,
    errors: 0,
  };
  const conflicts = [];
  const sample = [];
  // Phones claimed during THIS run (phone -> "uid|role") to detect source-vs-source conflicts.
  const claimed = new Map();

  console.log(
    `[backfillPhoneRoles] start project=${args.project || process.env.GCLOUD_PROJECT || "(ADC default)"} ` +
      `dryRun=${args.dryRun} pageSize=${args.pageSize}` +
      (process.env.FIRESTORE_EMULATOR_HOST ? ` emulator=${process.env.FIRESTORE_EMULATOR_HOST}` : "")
  );

  async function flush(batchItems) {
    if (!batchItems.length) return;
    if (args.dryRun) {
      stats.created += batchItems.length;
      for (const c of batchItems) if (sample.length < 10) sample.push(`${c.phone} -> ${c.role} (${c.uid}) from ${c.source}`);
      return;
    }
    const batch = db.batch();
    for (const c of batchItems) batch.create(db.collection(PHONE_ROLES).doc(c.phone), buildPayload(c));
    try {
      await batch.commit();
      stats.created += batchItems.length;
    } catch (err) {
      // ALREADY_EXISTS (gRPC 6) means someone created one of these docs after our existence check.
      // Fall back to one-by-one create() so the rest still land.
      console.warn(`[backfillPhoneRoles] batch failed (${err.code || err.message}); retrying individually`);
      for (const c of batchItems) {
        try {
          await db.collection(PHONE_ROLES).doc(c.phone).create(buildPayload(c));
          stats.created += 1;
        } catch (e) {
          if (e.code === 6 || /already exists/i.test(String(e.message))) stats.raceSkipped += 1;
          else {
            stats.errors += 1;
            console.error(`[backfillPhoneRoles] create failed for ${c.phone}: ${e.message}`);
          }
        }
      }
    }
  }

  for (const source of SOURCES) {
    if (args.only && args.only !== source.name) continue;
    console.log(`[backfillPhoneRoles] scanning ${source.name} ...`);
    let last = null;
    let pages = 0;
    for (;;) {
      let q = db.collection(source.name).orderBy(admin.firestore.FieldPath.documentId()).limit(args.pageSize);
      if (last) q = q.startAfter(last);
      const snap = await q.get();
      if (snap.empty) break;
      pages += 1;
      last = snap.docs[snap.docs.length - 1];

      // 1) derive candidates for this page, de-duplicated by phone
      const pageCandidates = new Map();
      for (const doc of snap.docs) {
        stats.scanned += 1;
        const c = candidateFromDoc(source, doc);
        if (!c.rawPhone) {
          stats.noPhone += 1;
          continue;
        }
        if (!c.phone) {
          stats.badPhone += 1;
          console.warn(`[backfillPhoneRoles] cannot normalize phone "${c.rawPhone}" (${source.name}/${doc.id})`);
          continue;
        }
        if (!c.role) {
          stats.noRole += 1;
          continue;
        }
        const key = `${c.uid}|${c.role}`;
        const prior = claimed.get(c.phone);
        if (prior && prior !== key) {
          stats.conflictsInSources += 1;
          conflicts.push(`${c.phone}: ${prior} vs ${key} (${source.name}/${doc.id})`);
          continue;
        }
        if (prior) continue; // same owner already handled (e.g. profile + users doc)
        claimed.set(c.phone, key);
        pageCandidates.set(c.phone, c);
      }
      if (!pageCandidates.size) continue;

      // 2) which of them already have a phoneRoles doc?
      const candidates = [...pageCandidates.values()];
      const missing = [];
      for (let i = 0; i < candidates.length; i += GETALL_CHUNK) {
        const chunk = candidates.slice(i, i + GETALL_CHUNK);
        const refs = chunk.map((c) => db.collection(PHONE_ROLES).doc(c.phone));
        const snaps = await db.getAll(...refs);
        snaps.forEach((s, idx) => {
          const c = chunk[idx];
          if (!s.exists) {
            missing.push(c);
            return;
          }
          const ex = s.data() || {};
          const exUid = firstNonEmpty(ex.uid, ex.userId);
          const exRole = normalizeRole(ex.role);
          if ((exUid && exUid !== c.uid) || (exRole && exRole !== c.role)) {
            stats.conflictsWithExisting += 1;
            conflicts.push(`${c.phone}: existing phoneRoles ${exUid}|${exRole} vs ${c.uid}|${c.role} (${c.source}/${c.docId})`);
          } else {
            stats.alreadyExists += 1;
          }
        });
      }

      // 3) write missing ones in batches <= MAX_BATCH
      for (let i = 0; i < missing.length; i += MAX_BATCH) {
        await flush(missing.slice(i, i + MAX_BATCH));
      }
      if (pages % 10 === 0) {
        console.log(`[backfillPhoneRoles] ${source.name}: pages=${pages} scanned=${stats.scanned} created=${stats.created}`);
      }
    }
    console.log(`[backfillPhoneRoles] finished ${source.name} (pages=${pages})`);
  }

  console.log("[backfillPhoneRoles] ---- summary ----");
  console.log(JSON.stringify({ dryRun: args.dryRun, ...stats }, null, 2));
  if (args.dryRun && sample.length) {
    console.log("[backfillPhoneRoles] sample of docs that WOULD be created:");
    sample.forEach((s) => console.log("  " + s));
  }
  if (conflicts.length) {
    console.log(`[backfillPhoneRoles] ${conflicts.length} CONFLICT(S) need manual review (not written):`);
    conflicts.slice(0, 200).forEach((c) => console.log("  " + c));
  }
  if (stats.errors > 0) process.exitCode = 1;
}

main().catch((err) => {
  console.error("[backfillPhoneRoles] FATAL", err);
  process.exit(1);
});
