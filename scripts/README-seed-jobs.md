# DutyPe Job Seeding Script

This script creates sample jobs for all categories in your DutyPe app.

## Categories (17 total)
- COOK (25 jobs)
- MAID (25 jobs)
- DRIVER (25 jobs)
- HELPER (25 jobs)
- SECURITY (25 jobs)
- GARDENER (25 jobs)
- CARETAKER (25 jobs)
- DELIVERY (25 jobs)
- WAITER (25 jobs)
- ELECTRICIAN (25 jobs)
- PLUMBER (25 jobs)
- PAINTER (25 jobs)
- CARPENTER (25 jobs)
- RECEPTIONIST (25 jobs)
- CASHIER (25 jobs)
- PACKER (25 jobs)
- OTHER (25 jobs)

**Total: 425 sample jobs**

## How to Run

### Option 1: Using Node.js (Recommended)

1. Navigate to the scripts folder:
   ```bash
   cd scripts
   ```

2. Install firebase-admin:
   ```bash
   npm install firebase-admin
   ```

3. Set up authentication (choose one):
   
   **Option A: Service Account Key**
   - Go to Firebase Console → Project Settings → Service Accounts
   - Click "Generate new private key"
   - Save the JSON file as `serviceAccountKey.json` in the scripts folder
   - Uncomment lines 18-19 in `seed-jobs.js` and comment line 22

   **Option B: Default Credentials (if you have gcloud CLI)**
   ```bash
   gcloud auth application-default login
   ```

4. Run the script:
   ```bash
   node seed-jobs.js
   ```

### Option 2: Using Firebase Console (Manual)

You can also import jobs directly through Firebase Console:
1. Go to Firebase Console → Firestore Database
2. Click "Start collection" or select "jobs" collection
3. Add documents manually or use the import feature

### Option 3: Deploy as Cloud Function

Add this to your `functions/src/index.ts`:

```typescript
export const seedSampleJobs = functions.https.onRequest(async (req, res) => {
  // Only allow POST requests with admin key
  if (req.method !== 'POST' || req.headers['x-admin-key'] !== 'YOUR_SECRET_KEY') {
    res.status(403).send('Forbidden');
    return;
  }
  
  // Copy the job generation logic here
  // ...
  
  res.send('Jobs seeded successfully');
});
```

## Job Data Structure

Each job includes:
- Realistic Hyderabad locations (20 areas)
- Appropriate pay ranges per category
- Random shift timings
- Urgency levels
- Benefits
- Contact numbers
- Expiry dates (15 days from posting)

## Customization

Edit `seed-jobs.js` to:
- Change `jobsPerCategory` (default: 25)
- Add more locations to `hyderabadAreas`
- Modify pay ranges in `payRanges`
- Add more job titles in `jobTitles`
- Add more company names in `companyNames`

## Notes

- Jobs are created with random posting times (within last 7 days)
- Each job has a unique `jobId` and `employerId`
- Phone numbers are randomly generated Indian mobile numbers
- Locations are slightly randomized around the base coordinates
