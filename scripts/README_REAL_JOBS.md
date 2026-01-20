# Real Jobs Seeding Script

This script uploads 20 REAL jobs from other platforms to your Firebase Firestore database.

## 📋 What This Script Does

- Uploads 20 real jobs with actual phone numbers and company names
- Automatically determines job category from title
- Generates smart descriptions based on job type
- Adds realistic dummy data for missing fields (shift timing, urgency, etc.)
- Sets proper GPS coordinates for Hyderabad locations
- **NO benefits added** (as per your request)

## 🚀 How to Run

### Step 1: Install Dependencies
```bash
cd scripts
npm install firebase-admin
```

### Step 2: Get Firebase Service Account Key
If you don't already have it:
1. Go to [Firebase Console](https://console.firebase.google.com/project/dutypeapp/settings/serviceaccounts/adminsdk)
2. Click "Generate new private key"
3. Save the downloaded JSON file in the `scripts` folder
4. Rename it to `serviceAccountKey.json` (or keep the original name)

### Step 3: Run the Script
```bash
node seed-real-jobs.js
```

## 📊 Jobs Being Uploaded

| # | Title | Company | Location | Phone | Vacancies |
|---|-------|---------|----------|-------|-----------|
| 1 | House cleaner | cleanzy | Bownepally | 8000062623 | 10 |
| 2 | cleaner | urban company | Madhapur | 7893798348 | 40 |
| 3 | house keeping staff | varshinin executive pg | Gachibowli | 9542126633 | 1 |
| 4 | receptionist cum hotel supervisor | HOTEL SL9 | Uppal Kalan | 8978632828 | 6 |
| 5 | Assistant manager | auctionbazaar.com | Somajiguda | 8370969696 | 5 |
| 6 | cleaning | urban company | Panjagutta | 7893798348 | 10 |
| 7 | office assistant | milan labels | Hyderabad | 9399979608 | 2 |
| 8 | xerox machine operator | KLICK N BROWSE | Hyderabad | 8686364646 | 2 |
| 9 | WASHING /IRONING | ragharitha drywash | Guntur | 9515711541 | 4 |
| 10 | Hostel warden | agasthya hostels | Hyderabad | 8984007999 | 2 |
| 11 | washing /ironing | tumble dry solutions | Shivam Road | 9000683274 | 2 |
| 12 | office boy | sheikh saheena | Aghapura | 9110587467 | 2 |
| 13 | care takers | maanyatha old age home | Uppal | 8008069707 | 40 |
| 14 | packing | yakshit facility services | Hyderabad | 9391857678 | 40 |
| 15 | solar technician | tekjawa solar solutions | Dilsukhnagar | 6304202209 | 5 |
| 16 | chief cook | satisfied foods | Shivam Road | 8977645467 | 1 |
| 17 | catering staff | onehm technology | Hyderabad | 8978122566 | 40 |
| 18 | car driver | mallikarjuna fleet | Hyderabad | 9059423233 | 40 |
| 19 | pharmacist | vasavi medicals | LB Nagar | 9849234834 | 5 |
| 20 | home nursing | vasavi medicals | LB Nagar | 9849234834 | 4 |

## 🎯 Smart Features

### Automatic Category Detection
The script intelligently maps job titles to categories:
- "cleaner", "housekeeping" → MAID
- "receptionist" → RECEPTIONIST
- "manager", "assistant" → OTHER
- "washing", "ironing" → MAID
- "care taker" → CARETAKER
- "packing" → PACKER
- "solar", "technician" → ELECTRICIAN
- "cook" → COOK
- "catering", "staff" → WAITER
- "driver" → DRIVER
- "nursing" → CARETAKER

### Smart Pay Type Detection
- Pay < ₹2,000 → Daily
- Pay ₹2,000-5,000 → Daily (for Maid/Helper) or Monthly (others)
- Pay > ₹5,000 → Monthly

### Smart Shift Timing
- "night" in title → Night shift
- "morning" in title → Morning shift
- Security/Guard → Night shift
- Cook/Chef → Full Day
- Cleaner/Maid → Morning shift
- Office jobs → Full Day
- Default → Flexible

### Smart Urgency
- 20+ vacancies → IMMEDIATE
- 10-19 vacancies → URGENT
- 5-9 vacancies → URGENT
- < 5 vacancies → NORMAL

### Smart Descriptions
Each job gets a professional description based on its category:
- Highlights key responsibilities
- Mentions required skills
- Includes company name
- Emphasizes reliability and experience

## 📍 Location Mapping

All locations are mapped to proper GPS coordinates:
- Bownepally: 17.4833, 78.5000
- Madhapur: 17.4486, 78.3908
- Gachibowli: 17.4401, 78.3489
- Uppal: 17.4065, 78.5593
- Somajiguda: 17.4239, 78.4538
- Panjagutta: 17.4260, 78.4506
- Dilsukhnagar: 17.3688, 78.5247
- LB Nagar: 17.3457, 78.5522
- Guntur: 16.3067, 80.4365

## ✅ Expected Output

```
🚀 Starting REAL jobs seeding...
📊 Total jobs to upload: 20

✅ [1/20] House cleaner - cleanzy
   📍 Bownepally, Hyderabad
   💰 ₹14000 MONTHLY
   📞 8000062623
   👥 10 vacancies
   🏷️  Category: MAID

✅ [2/20] cleaner - urban company
   📍 Madhapur, Hyderabad
   💰 ₹25000 MONTHLY
   📞 7893798348
   👥 40 vacancies
   🏷️  Category: MAID

... (continues for all 20 jobs)

==================================================
🎉 REAL JOBS SEEDING COMPLETE!
==================================================
✅ Successfully uploaded: 20 jobs
❌ Failed: 0 jobs
📊 Total: 20 jobs
==================================================
```

## 🔍 Verify in Firebase Console

After running the script:
1. Go to [Firebase Console](https://console.firebase.google.com/project/dutypeapp/firestore/data)
2. Navigate to `jobs` collection
3. Look for jobs with IDs starting with `REAL_`
4. Check that all fields are populated correctly

## 📱 Test in App

1. Open DutyPe app
2. Login as Worker
3. Go to "All Jobs" screen
4. You should see the 20 new real jobs
5. Try filtering by category
6. Check that phone numbers are clickable
7. Test WhatsApp apply feature

## ⚠️ Important Notes

- All jobs are marked as `isVerified: true` (real jobs)
- Jobs with 20+ vacancies are marked as `isTrending: true`
- No benefits added (empty array)
- All jobs expire in 15 days from posting
- Posted dates are randomized within last 3 days
- Employer accounts are auto-generated with prefix `EMP_REAL_`

## 🛠️ Troubleshooting

### Error: "Service account key not found"
- Download the key from Firebase Console
- Place it in the `scripts` folder
- Make sure it's named `serviceAccountKey.json`

### Error: "Permission denied"
- Check Firebase Security Rules
- Make sure your service account has write access

### Error: "Invalid phone number"
- Phone numbers are stored as-is from your data
- No validation is performed (Firebase doesn't validate)

## 🔄 Re-running the Script

If you need to re-run:
1. Delete existing jobs from Firebase Console (filter by `REAL_` prefix)
2. Run the script again
3. New job IDs will be generated

## 📞 Support

If you encounter any issues:
- Check the console output for error messages
- Verify Firebase credentials
- Check internet connection
- Review Firebase Security Rules
