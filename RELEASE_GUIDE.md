# Complete Release Guide for Google Play Console

## 🔴 CRITICAL: Finding the Correct Keystore

Your current keystore has SHA1: `01:8C:32:2C:75:D2:3E:65:A2:A3:7F:A9:FF:67:21:B9:BF:E2:73:51`
But Google Play expects SHA1: `BC:C7:8D:E1:A6:D7:BD:F3:3F:90:B5:43:D6:32:F1:63:D2:F9:02:D3`

### Step 1: Find the Correct Keystore File

You need to locate the keystore file that matches the expected SHA1 fingerprint. Check these locations:

1. **Check Google Play Console:**
   - Go to Google Play Console → Your App → Setup → App signing
   - Look at the "App signing key certificate" section
   - This shows the certificate details but not the keystore location

2. **Search your computer for keystore files:**
   ```powershell
   # Search for all .keystore and .jks files
   Get-ChildItem -Path C:\ -Recurse -Include *.keystore,*.jks -ErrorAction SilentlyContinue | Select-Object FullName
   ```

3. **Check common locations:**
   - `C:\Users\banot\Android\` or `C:\Users\banot\.android\`
   - Project directories
   - Backup folders
   - Cloud storage (Google Drive, OneDrive, etc.)

4. **Check with team members** who might have created the original keystore

5. **Check your backup/version control** - if the keystore was ever committed (not recommended but might help)

### Step 2: Verify Keystore Fingerprint

Once you find a potential keystore file, verify its SHA1 fingerprint:

```powershell
# Replace with your keystore path and alias
keytool -list -v -keystore "PATH_TO_KEYSTORE" -alias "KEY_ALIAS"
```

Look for the SHA1 fingerprint in the output. It must match: `BC:C7:8D:E1:A6:D7:BD:F3:3F:90:B5:43:D6:32:F1:63:D2:F9:02:D3`

### Step 3: Update keystore.properties

Once you find the correct keystore, update `keystore.properties`:

```properties
storeFile=C:/path/to/correct/keystore.keystore
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

---

## 📱 CLOSED TESTING RELEASE - Step by Step

### Prerequisites
- ✅ Correct keystore file with matching SHA1 fingerprint
- ✅ `keystore.properties` configured correctly
- ✅ App tested and working
- ✅ Version code incremented (if updating existing release)

### Step 1: Update Version Information

Edit `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.parttime.dutype"
    minSdk = 24
    targetSdk = 35
    versionCode = 2  // Increment this for each release
    versionName = "1.0.1"  // Update version name
    // ...
}
```

### Step 2: Clean and Build Release AAB

```powershell
# Navigate to project root
cd C:\Users\banot\StudioProjects\DutyPe

# Clean previous builds
.\gradlew clean

# Build release AAB
.\gradlew bundleRelease
```

The AAB will be generated at: `app\build\outputs\bundle\release\app-release.aab`

### Step 3: Verify AAB Signature

Before uploading, verify the AAB is signed correctly:

```powershell
# Verify signature
jarsigner -verify -verbose -certs app\build\outputs\bundle\release\app-release.aab

# Check certificate fingerprint
keytool -printcert -jarfile app\build\outputs\bundle\release\app-release.aab
```

The SHA1 fingerprint should match: `BC:C7:8D:E1:A6:D7:BD:F3:3F:90:B5:43:D6:32:F1:63:D2:F9:02:D3`

### Step 4: Upload to Google Play Console (Closed Testing)

1. **Go to Google Play Console:**
   - Navigate to: https://play.google.com/console
   - Select your app: **DutyPe**

2. **Navigate to Testing:**
   - Click on **"Testing"** in the left sidebar
   - Click on **"Closed testing"** (or "Internal testing" for faster testing)

3. **Create/Select Track:**
   - If first time: Click **"Create new release"**
   - If updating: Click on existing track → **"Create new release"**

4. **Upload AAB:**
   - Click **"Upload"** or drag and drop
   - Select: `app\build\outputs\bundle\release\app-release.aab`
   - Wait for upload and processing to complete

5. **Add Release Notes:**
   - Enter release notes (what's new, bug fixes, etc.)
   - Example:
     ```
     Version 1.0.1 - Closed Testing
     - Bug fixes and improvements
     - Enhanced user experience
     ```

6. **Review and Rollout:**
   - Review all information
   - Click **"Review release"**
   - Click **"Start rollout to Closed testing"**

7. **Add Testers (if needed):**
   - Go to **"Testers"** tab
   - Add email addresses or create a Google Group
   - Testers will receive an email with testing link

### Step 5: Monitor Release

- Check **"Release"** tab for status
- Monitor for any errors or warnings
- Test the app yourself using the testing link

---

## 🚀 PRODUCTION RELEASE - Step by Step

### Prerequisites
- ✅ App successfully tested in Closed Testing
- ✅ All bugs fixed
- ✅ App Store Listing complete (screenshots, description, etc.)
- ✅ Content rating completed
- ✅ Privacy policy URL added (if required)
- ✅ Target audience and content set

### Step 1: Prepare Production Build

1. **Update Version:**
   ```kotlin
   defaultConfig {
       versionCode = 3  // Increment from closed testing
       versionName = "1.0.0"  // Production version
   }
   ```

2. **Build Production AAB:**
   ```powershell
   .\gradlew clean bundleRelease
   ```

3. **Verify Signature:**
   ```powershell
   keytool -printcert -jarfile app\build\outputs\bundle\release\app-release.aab
   ```

### Step 2: Complete App Store Listing

1. **Go to Google Play Console → Your App**

2. **Complete Required Sections:**
   - **Main store listing:**
     - App name
     - Short description (80 chars)
     - Full description (4000 chars)
     - App icon (512x512 PNG)
     - Feature graphic (1024x500 PNG)
     - Screenshots (at least 2, up to 8)
     - Phone screenshots: 16:9 or 9:16 ratio
     - Tablet screenshots (optional)

   - **Content rating:**
     - Complete questionnaire
     - Get rating certificate

   - **Target audience and content:**
     - Set target age group
     - Content guidelines compliance

   - **Privacy policy:**
     - Add privacy policy URL (required for most apps)

   - **App access:**
     - Declare app access restrictions if any

### Step 3: Upload Production Release

1. **Navigate to Production:**
   - Go to **"Production"** in left sidebar
   - Click **"Create new release"**

2. **Upload AAB:**
   - Upload: `app\build\outputs\bundle\release\app-release.aab`
   - Wait for processing

3. **Add Release Notes:**
   ```
   Version 1.0.0 - Initial Release
   - First production release
   - Core features implemented
   ```

4. **Review Release:**
   - Check all warnings/errors
   - Ensure version code is higher than previous releases
   - Review release notes

### Step 4: Review and Publish

1. **Check Pre-launch Report:**
   - Go to **"Release" → "Pre-launch report"**
   - Fix any critical issues

2. **Review Store Listing:**
   - Ensure all required fields are complete
   - Check for any warnings (yellow triangles)

3. **Submit for Review:**
   - Click **"Review release"**
   - Review all sections
   - Click **"Start rollout to Production"**

4. **Wait for Review:**
   - Google typically reviews within 1-3 days
   - You'll receive email notifications
   - Check status in Play Console

5. **After Approval:**
   - App will be live on Google Play Store
   - Monitor reviews and ratings
   - Respond to user feedback

---

## 🔧 Troubleshooting

### Issue: "Wrong signing key" error

**Solution:**
1. Verify you're using the correct keystore file
2. Check SHA1 fingerprint matches expected value
3. Ensure `keystore.properties` has correct path
4. Rebuild AAB after fixing keystore configuration

### Issue: "Version code already used"

**Solution:**
- Increment `versionCode` in `build.gradle.kts`
- Each release must have a unique, higher version code

### Issue: "Missing required fields"

**Solution:**
- Complete all required sections in Store Listing
- Add privacy policy URL if required
- Complete content rating questionnaire

### Issue: "App rejected"

**Solution:**
- Check rejection reason in Play Console
- Fix issues mentioned
- Resubmit after fixes

---

## 📝 Checklist Before Production Release

- [ ] Correct keystore file with matching SHA1 fingerprint
- [ ] `keystore.properties` configured correctly
- [ ] Version code incremented
- [ ] Version name updated
- [ ] AAB built and verified
- [ ] App tested thoroughly
- [ ] Store listing complete (screenshots, description, etc.)
- [ ] Content rating completed
- [ ] Privacy policy URL added
- [ ] Target audience set
- [ ] App access declared
- [ ] Pre-launch report reviewed
- [ ] No critical warnings in Play Console

---

## 🔐 Security Best Practices

1. **Never commit keystore files to version control**
   - Already added to `.gitignore` ✅

2. **Never commit `keystore.properties`**
   - Already added to `.gitignore` ✅

3. **Backup your keystore securely**
   - Store in encrypted backup
   - Keep multiple secure copies
   - Share with trusted team members securely

4. **Use strong passwords**
   - For keystore and key alias

5. **Document keystore location**
   - Keep secure notes (not in code)
   - Share with team securely

---

## 📞 Need Help?

If you cannot find the correct keystore:
1. Check Google Play Console → App signing → App signing key certificate
2. Contact Google Play Support for assistance
3. If keystore is truly lost, you may need to:
   - Create a new app listing (new package name)
   - Or contact Google for key reset (complex process)

---

**Last Updated:** Based on current project configuration
**Project:** DutyPe (com.parttime.dutype)

