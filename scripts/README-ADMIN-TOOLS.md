# 🔧 Admin Tools for Job Management

These scripts help you audit and fix job issues in your Firestore database.

## 📋 Available Scripts

### 1. Quick Job Count
**Shows a summary of jobs by status**

```bash
node scripts/count-jobs.js
```

**Output:**
- Total jobs in database
- Active vs inactive jobs
- Filled vs unfilled jobs
- Expired jobs
- Valid jobs (shown in app)
- Jobs by category

**Use this first** to get a quick overview of your database.

---

### 2. Full Job Audit
**Detailed analysis of all jobs and their issues**

```bash
node scripts/audit-jobs.js
```

**What it checks:**
- ✅ Missing required fields (jobId, title, location, etc.)
- ✅ Invalid coordinates (lat/lon = 0 or missing)
- ✅ Expired jobs
- ✅ Filled jobs
- ✅ Inactive jobs
- ✅ Missing categories

**Output:**
- Detailed report in console
- JSON report saved to `scripts/job-audit-report.json`
- Recommendations for fixes

---

### 3. Fix Job Issues
**Automatically fix common problems**

```bash
# Preview changes (dry run)
node scripts/fix-job-issues.js --all --dry-run

# Apply all fixes
node scripts/fix-job-issues.js --all

# Fix specific issues
node scripts/fix-job-issues.js --extend-expiry
node scripts/fix-job-issues.js --reactivate
node scripts/fix-job-issues.js --fix-missing
```

**Options:**
- `--extend-expiry` - Extend expired jobs by 30 days
- `--reactivate` - Reactivate inactive jobs (isActive=false → true)
- `--fix-missing` - Add missing required fields with defaults
- `--all` - Apply all fixes
- `--dry-run` - Preview changes without applying them

**What it fixes:**
- ⏰ Expired jobs → Extended by 30 days
- 🚫 Inactive jobs → Reactivated (isActive=true)
- 📝 Missing fields → Added with defaults
  - jobId, isActive, isFilled, createdAt, expiresAt, etc.

---

## 🎯 Recommended Workflow

### Step 1: Check Current Status
```bash
node scripts/count-jobs.js
```

This shows you:
- How many jobs are in the database
- How many are actually showing in the app
- Why jobs might be hidden

### Step 2: Run Full Audit
```bash
node scripts/audit-jobs.js
```

This gives you:
- Detailed list of all issues
- Specific jobs with problems
- Recommendations for fixes

### Step 3: Preview Fixes
```bash
node scripts/fix-job-issues.js --all --dry-run
```

This shows you:
- What changes will be made
- How many jobs will be affected
- No actual changes are made

### Step 4: Apply Fixes
```bash
node scripts/fix-job-issues.js --all
```

This will:
- Extend expired jobs
- Reactivate inactive jobs
- Fix missing fields
- Make jobs visible in the app

### Step 5: Verify
```bash
node scripts/count-jobs.js
```

Check that the "VALID JOBS" count increased!

---

## 📊 Understanding Job Visibility

A job is **visible in the app** only if ALL of these are true:

1. ✅ `isActive = true` (or undefined)
2. ✅ `isFilled = false` (or undefined)
3. ✅ `expiresAt > currentTime` (or 0)
4. ✅ Has all required fields
5. ✅ Has valid coordinates (lat/lon)

If any condition fails, the job is **hidden from workers**.

---

## 🔍 Common Issues & Solutions

### Issue: "Only 100 jobs showing, but I have 500+"

**Cause:** Jobs are hidden due to:
- Expired (expiresAt < now)
- Inactive (isActive=false)
- Filled (isFilled=true)

**Solution:**
```bash
node scripts/count-jobs.js  # Check status
node scripts/fix-job-issues.js --extend-expiry --reactivate
```

---

### Issue: "Jobs missing from specific categories"

**Cause:** 
- Missing or invalid category field
- Category name mismatch

**Solution:**
```bash
node scripts/audit-jobs.js  # Find jobs with missing categories
# Manually fix in Firebase Console or update seed scripts
```

---

### Issue: "Jobs have invalid coordinates"

**Cause:**
- lat/lon = 0 or missing
- Jobs won't show on map

**Solution:**
```bash
node scripts/audit-jobs.js  # Find jobs with invalid coords
# Manually fix coordinates in Firebase Console
```

---

## 🚀 Quick Reference

```bash
# Quick check
node scripts/count-jobs.js

# Full audit
node scripts/audit-jobs.js

# Preview fixes
node scripts/fix-job-issues.js --all --dry-run

# Apply fixes
node scripts/fix-job-issues.js --all

# Extend only expired jobs
node scripts/fix-job-issues.js --extend-expiry

# Reactivate only inactive jobs
node scripts/fix-job-issues.js --reactivate

# Fix only missing fields
node scripts/fix-job-issues.js --fix-missing
```

---

## ⚠️ Important Notes

1. **Always run with `--dry-run` first** to preview changes
2. **Backup your database** before running fixes (Firebase Console → Export)
3. **Test in development** before running in production
4. **Review audit report** before applying fixes
5. **Scripts require Firebase Admin SDK** credentials

---

## 📝 Example Output

### Count Jobs:
```
📊 JOB COUNT SUMMARY
═══════════════════════════════════════════════════════
Total Jobs in DB:        523

By Status:
  ✅ Active:             500
  🚫 Inactive:           23
  ✔️  Filled:             45
  📋 Unfilled:           478
  ⏰ Expired:            378

🎯 VALID JOBS (shown in app): 100
   (active + unfilled + not expired)
```

This tells you:
- You have 523 total jobs
- Only 100 are showing in the app
- 378 are expired (need extension)
- 23 are inactive (need reactivation)
- 45 are filled (normal - completed jobs)

---

## 🆘 Need Help?

If you're seeing unexpected results:

1. Check Firebase Console → Firestore → jobs collection
2. Look at a few job documents manually
3. Compare with the audit report
4. Run fixes with `--dry-run` first
5. Contact dev team if issues persist

---

**Happy job management! 🎉**
