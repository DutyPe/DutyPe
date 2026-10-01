/**
 * Deletes jobs not posted by a real employer: employerId "admin" (web panel) or an employerId with
 * no employer profile (bulk / AI posts). Removes card, details, contact, applications and saved
 * entries. Dry run by default; --apply deletes. Backup: backups/firestore-2026-10-01T06-28-02-983Z.json
 */
const path = require("path");
const admin = require(path.join(__dirname, "../functions/node_modules/firebase-admin"));
admin.initializeApp({ credential: admin.credential.cert(require(path.join(__dirname, "../serviceAccountKey.json.json"))) });
const db = admin.firestore();
(async () => {
  const [jobs, emps] = await Promise.all([db.collection("jobmetadata").get(), db.collection("employer_profiles").get()]);
  const employers = new Set(emps.docs.map((d) => d.id));
  const doomed = jobs.docs.filter((j) => { const e = j.get("employerId"); return e === "admin" || !employers.has(e); });
  const ids = new Set(doomed.map((d) => d.id));
  const refs = [];
  for (const j of doomed) refs.push(j.ref, db.collection("job_details").doc(j.id), db.collection("job_contacts").doc(j.id));
  const apps = (await db.collection("applications").get()).docs.filter((a) => ids.has(a.get("jobId")));
  const saved = (await db.collection("saved_jobs").get()).docs.filter((s) => ids.has(s.get("jobId")));
  apps.forEach((a) => refs.push(a.ref)); saved.forEach((s) => refs.push(s.ref));
  console.log(`jobs to delete: ${doomed.length} (keeping ${jobs.size - doomed.length}) | applications: ${apps.length} | saved: ${saved.length}`);
  if (!process.argv.includes("--apply")) return;
  const w = db.bulkWriter();
  refs.forEach((r) => w.delete(r));
  await w.close();
  console.log(`deleted ${refs.length} documents`);
  process.exit(0);
})().catch((e) => { console.error(e); process.exit(1); });
