# Firebase Job Import Steps

Use these steps from a machine or agent that has Firebase Admin credentials for the DutyPe Firebase project.

## Goal

Push the prepared online job review file into the active DutyPe job posting schema.

Prepared file:

```text
scripts/output/india-fresh-phone-verified-merged-strict-validated.json
```

Expected prepared count:

```text
502 valid jobs
0 skipped jobs
```

## Hard Rules

- Do not scrape again.
- Do not invent, rewrite, or enrich job fields.
- Do not write to the old `jobs` collection.
- Do not write to `job_cards`.
- Do not write to audit/import collections.
- Write only these two collections:
  - `jobmetadata/{jobId}`
  - `job_details/{jobId}`
- Do not use `--approved-only`; the prepared rows are intentionally `approved:false` in the review file, and that review flag is not written to Firestore.
- Keep `contactNumber` only in `job_details`, not in `jobmetadata`.

## Firestore Schema To Write

`jobmetadata/{jobId}` must contain only card/listing fields:

```text
title
companyName
salary
salaryType
location
geohash
addressText
jobType
status
createdAt
employerId
vacancies
```

`job_details/{jobId}` must contain only detail fields:

```text
employerId
expiresAt
contactNumber
description
gender
experienceRequired
educationRequired
shiftTiming
companyCity
applicationCount
```

## Step 1: Open The Repo

```powershell
cd C:\Users\91961\DutyPe
```

## Step 2: Install Script Dependencies

Run this if `scripts/node_modules` is missing or stale:

```powershell
Push-Location scripts
npm install
Pop-Location
```

## Step 3: Configure Firebase Admin Credentials

Preferred method: keep the service account JSON outside the repo and set the path only in the terminal session.

```powershell
$env:FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH="C:\path\to\firebase-admin-service-account.json"
```

Alternative method, if the environment already stores separate admin fields:

```powershell
$env:FIREBASE_ADMIN_PROJECT_ID="dutype-860ac"
$env:FIREBASE_ADMIN_CLIENT_EMAIL="your-admin-service-account-email"
$env:FIREBASE_ADMIN_PRIVATE_KEY="your-admin-private-key"
```

Do not commit a service account JSON file or raw private key to the repo.

## Step 4: Confirm The Importer Uses Only The New Schema

Open `scripts/import-online-jobs.js` and confirm the apply path writes only:

```js
db.collection('jobmetadata')
db.collection('job_details')
```

Also confirm it does not write to:

```text
jobs
job_cards
job_import_sources
```

The current intended apply behavior is:

- Build a fresh card document for `jobmetadata` from `job.meta`.
- Build a fresh details document for `job_details` from `job.details`.
- Skip rows whose `jobmetadata/{jobId}` already exists.
- Batch write the matching two documents per new job.

## Step 5: Dry Validate Before Writing

Run this first. It must not write to Firestore.

```powershell
node scripts\import-online-jobs.js `
  --from-review scripts\output\india-fresh-phone-verified-merged-strict-validated.json `
  --review-out scripts\output\india-fresh-phone-verified-merged-strict-preapply.json
```

Expected output:

```text
Review file: ...india-fresh-phone-verified-merged-strict-preapply.json
Valid jobs: 502
Skipped: 0
Dry-run only. No Firestore writes were made.
```

Stop if the output is not `Valid jobs: 502` and `Skipped: 0`.

## Step 6: Apply To Firebase

Run the apply command only after the dry validation passes.

```powershell
node scripts\import-online-jobs.js `
  --from-review scripts\output\india-fresh-phone-verified-merged-strict-validated.json `
  --review-out scripts\output\india-fresh-phone-verified-merged-strict-applied.json `
  --apply
```

Expected output shape:

```text
Review file: ...india-fresh-phone-verified-merged-strict-applied.json
Valid jobs: 502
Skipped: 0
Created: <number>
Duplicates skipped: <number>
Invalid skipped at write time: 0
```

`Created + Duplicates skipped` should equal `502`.

If `Invalid skipped at write time` is not `0`, stop and inspect the invalid rows before retrying.

## Step 7: Verify Firestore

Pick a few `jobId` values from:

```text
scripts/output/india-fresh-phone-verified-merged-strict-validated.json
```

For each sample `jobId`, verify:

- `jobmetadata/{jobId}` exists.
- `job_details/{jobId}` exists.
- `jobmetadata/{jobId}` does not contain `contactNumber`.
- `job_details/{jobId}` contains `contactNumber`.
- `jobmetadata/{jobId}.status` is `open`.
- No documents were created in old `jobs`.
- No documents were created in `job_cards`.
- No audit/import collection was created by this import.

Optional verification command for the credentialed environment:

```powershell
node -e "const fs=require('fs'); const admin=require('firebase-admin'); const {loadServiceAccount}=require('./scripts/lib/firebase-admin-service-account'); if(!admin.apps.length) admin.initializeApp({credential:admin.credential.cert(loadServiceAccount())}); const db=admin.firestore(); const r=JSON.parse(fs.readFileSync('scripts/output/india-fresh-phone-verified-merged-strict-validated.json','utf8')); (async()=>{const sample=(r.jobs||[]).slice(0,10); let ok=0; for(const job of sample){const [m,d]=await Promise.all([db.collection('jobmetadata').doc(job.jobId).get(), db.collection('job_details').doc(job.jobId).get()]); const meta=m.data()||{}; const details=d.data()||{}; if(m.exists&&d.exists&&!('contactNumber' in meta)&&details.contactNumber&&meta.status==='open') ok++; else console.log('bad', job.jobId, {metaExists:m.exists, detailsExists:d.exists, metaHasPhone:'contactNumber' in meta, detailsHasPhone:!!details.contactNumber, status:meta.status}); } console.log('verifiedSamples', ok, 'of', sample.length); process.exit(ok===sample.length?0:1);})().catch(e=>{console.error(e); process.exit(1);});"
```

Expected output:

```text
verifiedSamples 10 of 10
```

## Step 8: Clean Credential Environment Variables

After the import and verification finish, clear credential environment variables from the terminal session.

```powershell
Remove-Item Env:\FIREBASE_ADMIN_SERVICE_ACCOUNT_PATH -ErrorAction SilentlyContinue
Remove-Item Env:\FIREBASE_ADMIN_SERVICE_ACCOUNT_JSON -ErrorAction SilentlyContinue
Remove-Item Env:\FIREBASE_ADMIN_PROJECT_ID -ErrorAction SilentlyContinue
Remove-Item Env:\FIREBASE_ADMIN_CLIENT_EMAIL -ErrorAction SilentlyContinue
Remove-Item Env:\FIREBASE_ADMIN_PRIVATE_KEY -ErrorAction SilentlyContinue
```

## Security Follow-Up

If any raw Firebase private key exists in repo files or deployment scripts, rotate that service account key in Google Cloud and remove the raw secret from the repo. Keep future admin credentials in environment variables, secret manager, or local files outside the repo.