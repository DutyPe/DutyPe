// Verify the contact_detail JSON extraction across all 21 scraped jobs, then patch Firestore records
const fs = require('fs');
const path = require('path');
const review = require('./output/south-delhi-driver-review-2026-05-05.json');
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';

function extractContactFromHtml(html) {
  // WorkIndia embeds: "contact_detail":{"contact_type":"phone_no","contact":"XXXXXXXXXX"}
  const m = html.match(/"contact_detail"\s*:\s*\{[^}]*"contact"\s*:\s*"([^"]+)"/);
  if (m) {
    const raw = m[1];
    const d = raw.replace(/\D/g,'');
    if (d.length === 10 && /^[6-9]/.test(d)) return d;
    if (d.length === 12 && d.startsWith('91') && /^[6-9]/.test(d.slice(2))) return d.slice(2);
    return raw; // return as-is if not standard mobile
  }
  return '';
}

async function fetchText(url) {
  const r = await fetch(url, { headers: { 'User-Agent': UA } });
  if (!r.ok) throw new Error(`HTTP ${r.status}`);
  return r.text();
}

(async () => {
  console.log(`Fetching contact details for ${review.jobs.length} jobs...\n`);
  const results = [];
  let found = 0, empty = 0;

  for (const job of review.jobs) {
    try {
      const html = await fetchText(job.sourceUrl);
      const phone = extractContactFromHtml(html);
      results.push({ jobId: job.jobId, title: job.meta.title, company: job.meta.companyName, phone, url: job.sourceUrl });
      if (phone) { found++; process.stdout.write(`✅ ${job.meta.title} – ${job.meta.companyName}: ${phone}\n`); }
      else { empty++; process.stdout.write(`⚠️  ${job.meta.title} – ${job.meta.companyName}: no phone\n`); }
    } catch (e) {
      results.push({ jobId: job.jobId, title: job.meta.title, company: job.meta.companyName, phone: '', error: e.message });
      empty++;
      process.stdout.write(`❌ ${job.meta.title}: ${e.message}\n`);
    }
  }

  console.log(`\n✅ ${found} with phone, ⚠️ ${empty} without`);

  // Save results
  const outPath = path.join(__dirname, 'output', 'south-delhi-driver-phones.json');
  fs.writeFileSync(outPath, JSON.stringify(results, null, 2));
  console.log(`\nSaved to: ${outPath}`);
  console.log('\nRun with --apply to patch Firestore job_details records.');

  if (process.argv.includes('--apply')) {
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
    let ok = 0, skip = 0;
    for (const r of results) {
      if (!r.phone) { skip++; continue; }
      await db.collection('job_details').doc(r.jobId).update({ contactNumber: r.phone });
      await db.collection('jobmetadata').doc(r.jobId).update({ contactNumber: r.phone });
      console.log(`  ✅ Patched ${r.jobId}: ${r.phone}`);
      ok++;
    }
    console.log(`\nPatched: ${ok}, Skipped (no phone): ${skip}`);
  }
})().catch(console.error);
