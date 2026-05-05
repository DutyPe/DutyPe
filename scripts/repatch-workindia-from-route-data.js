/**
 * Re-patch all 21 scraped South Delhi driver jobs with accurate data from
 * WorkIndia's embedded __ROUTE_DATA__ JSON:
 *  - Real job description (profile_job_description)
 *  - Actual vacancy count (no_of_openings)
 *  - Interview timing (interview_details)
 *  - Contact person name (branch_contact_person_name)
 *  - Work timings (job_timings)
 *  - Exact address (branch_address)
 *
 * Usage:
 *   node repatch-workindia-from-route-data.js          -- dry-run, prints what will change
 *   node repatch-workindia-from-route-data.js --apply  -- writes to Firestore
 */
'use strict';
const fs = require('fs');
const path = require('path');
const review = require('./output/south-delhi-driver-review-2026-05-05.json');
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';
const APPLY = process.argv.includes('--apply');

async function fetchText(url) {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), 25000);
  try {
    const r = await fetch(url, { signal: ctrl.signal, headers: { 'User-Agent': UA } });
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    return r.text();
  } finally { clearTimeout(t); }
}

function extractRouteData(html) {
  // WorkIndia SSR embeds: window.__ROUTE_DATA__={...}</script>
  const m = html.match(/window\.__ROUTE_DATA__=(\{[\s\S]*?\})<\/script>/);
  if (!m) return null;
  try { return JSON.parse(m[1]); } catch { return null; }
}

function buildEnrichedDescription(d) {
  const parts = [];
  if (d.profile_job_description) parts.push(d.profile_job_description);
  if (d.profile_requirement_checklist) parts.push(`Requirements: ${d.profile_requirement_checklist.trim()}`);
  if (d.profile_qualification_required) parts.push(`Qualification: ${d.profile_qualification_required}`);
  if (d.job_timings) parts.push(`Work Hours: ${d.job_timings}`);
  if (d.interview_details) parts.push(`Interview Timing: ${d.interview_details}`);
  if (d.interview_address) parts.push(`Interview Address: ${d.interview_address}`);
  if (d.branch_address) parts.push(`Location: ${d.branch_address}`);
  if (d.branch_contact_person_name) parts.push(`Contact Person: ${d.branch_contact_person_name}`);
  return parts.filter(Boolean).join('\n');
}

(async () => {
  console.log(`\n🔄 Repatch WorkIndia Jobs from __ROUTE_DATA__`);
  console.log(`   Mode: ${APPLY ? '🔴 APPLY' : '🟡 DRY-RUN'}  |  Jobs: ${review.jobs.length}\n`);

  const patches = [];
  for (const job of review.jobs) {
    try {
      const html = await fetchText(job.sourceUrl);
      const d = extractRouteData(html);
      if (!d) { console.log(`⚠️  No __ROUTE_DATA__ for ${job.jobId}`); continue; }

      const description = buildEnrichedDescription(d);
      const vacancies = Number.isInteger(d.no_of_openings) && d.no_of_openings >= 1 && d.no_of_openings <= 50
        ? d.no_of_openings : job.meta.vacancies;
      const shiftTiming = d.job_timings || job.details.shiftTiming || '';
      const contactPerson = d.branch_contact_person_name || '';

      console.log(`\n📋 ${job.meta.title} – ${job.meta.companyName}`);
      console.log(`   Vacancies: ${vacancies}  |  Contact person: ${contactPerson}`);
      console.log(`   Work hours: ${shiftTiming}`);
      console.log(`   Description (first 150): ${description.slice(0, 150)}`);

      patches.push({ jobId: job.jobId, vacancies, description, shiftTiming, contactPerson });
    } catch (e) {
      console.log(`❌ ${job.jobId}: ${e.message}`);
    }
  }

  console.log(`\n📝 ${patches.length} patches ready`);

  if (!APPLY) {
    console.log('\nDry-run done. Run with --apply to write to Firestore.');
    return;
  }

  const keyFiles = ['serviceAccountKey.json', 'dutype-860ac-firebase-adminsdk.json'];
  let keyPath = null;
  for (const f of keyFiles) {
    for (const dir of [__dirname, path.join(__dirname, '..')]) {
      const p = path.join(dir, f);
      if (fs.existsSync(p)) { keyPath = p; break; }
    }
    if (keyPath) break;
  }
  const admin = require('firebase-admin');
  if (!admin.apps.length) admin.initializeApp({ credential: admin.credential.cert(require(keyPath)), projectId: 'dutype-860ac' });
  const db = admin.firestore();

  let ok = 0;
  for (const p of patches) {
    await db.collection('job_details').doc(p.jobId).update({
      description: p.description,
      shiftTiming: p.shiftTiming,
      contactPerson: p.contactPerson,
    });
    await db.collection('jobmetadata').doc(p.jobId).update({
      vacancies: p.vacancies,
    });
    console.log(`  ✅ ${p.jobId} (vacancies: ${p.vacancies}, contact: ${p.contactPerson})`);
    ok++;
  }
  console.log(`\n🎉 Patched ${ok} jobs.`);
})().catch(console.error);
