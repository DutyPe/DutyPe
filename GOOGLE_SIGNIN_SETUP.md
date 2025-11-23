# Google Sign-In Setup Guide for DutyPe

## ❌ Issue: "No credentials available" after Play Store Download

This error typically occurs when the app's signing certificate SHA-1 fingerprint doesn't match what's configured in Google Cloud Console.

---

## ✅ Solution: Configure SHA-1 Fingerprints

### Step 1: Get Your Release SHA-1 Fingerprint

Run the provided PowerShell script:

```powershell
.\GET_RELEASE_SHA1.ps1
```

This will output something like:
```
SHA1: AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12
```

**Copy only the fingerprint without the "SHA1:" prefix**

### Step 2: Add Both SHA-1 Fingerprints to Google Cloud Console

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Select your DutyPe project
3. Navigate to **APIs & Services > Credentials**
4. Find your OAuth 2.0 Client ID (Android)
5. Click to edit it
6. Add both fingerprints under "SHA-1 certificate fingerprints":

   - **Debug SHA-1**: `F1:DE:6A:E3:C5:88:C3:16:B7:5A:F0:53:EE:7D:92:36:5A:93:9B:13` (for development)
   - **Release SHA-1**: (Your generated fingerprint from Step 1)

### Step 3: Verify Configuration

- ✅ Google Services JSON is correct
- ✅ Both debug and release SHA-1 are added
- ✅ OAuth Consent Screen is configured
- ✅ OAuth Client ID is set correctly
- ✅ App signing certificate is uploaded to Play Console

---

## 🔍 Debugging Tips

If you still get "no credentials available":

### 1. **Check Logcat for details:**
```
adb logcat | grep "GetCredentialException"
adb logcat | grep "no_credentials"
```

### 2. **Clear Credential State:**
- App Settings → Clear Cache and Data
- Google Play Services → Clear Cache and Data
- Try Sign-In again

### 3. **Verify Package Name:**
```
adb shell cmd package list packages | grep dutype
```
Should show: `com.dutype.app`

### 4. **Check SHA-1 Format:**
- Must be UPPERCASE letters with colons
- Example: `AB:CD:EF:12:34:56:78:90`
- NOT: `abcdef1234567890` (no colons, lowercase)

### 5. **Try Phone Sign-In as Alternative:**
If Google Sign-In fails, users can use Phone OTP authentication.

---

## 📋 Release Checklist

Before uploading to Play Store:

- [ ] Release SHA-1 added to Google Cloud Console
- [ ] google-services.json updated with latest config
- [ ] Keystore.properties configured with release signing key
- [ ] Build Variant set to "release"
- [ ] Test Google Sign-In on release build (`./gradlew assembleRelease`)
- [ ] Upload signed AAB to Play Store

---

## 🚀 Quick Commands

```powershell
# Get release SHA-1
.\GET_RELEASE_SHA1.ps1

# Build release APK for testing
./gradlew.bat assembleRelease

# Install release APK on device
adb install -r app/build/outputs/apk/release/app-release.apk

# View logs
adb logcat -v all | grep -i credential

# Clean and rebuild
./gradlew.bat clean assembleRelease
```

---

## 🔗 Relevant Files

- `strings.xml` - Contains `default_web_client_id`
- `google-services.json` - Firebase configuration
- `keystore.properties` - Release signing credentials
- `EnhancedLoginScreen.kt` - Google Sign-In implementation
- `GoogleSignInManager.kt` - Authentication logic

---

## ⚠️ Common Mistakes

1. ❌ Using debug SHA-1 for release build
2. ❌ SHA-1 fingerprint in wrong format (missing colons)
3. ❌ Old google-services.json file
4. ❌ OAuth consent screen not configured
5. ❌ Wrong package name in configuration

---

## 📞 Support

If credentials are still unavailable:

1. Check error messages in Logcat
2. Verify SHA-1 fingerprints exactly match
3. Try Phone Sign-In as fallback
4. Contact Google Cloud Support with project ID

---

## 🔄 Phone Sign-In Alternative

Users can also authenticate via:
- Phone OTP (SMS-based)
- Phone authentication is more reliable in countries with restricted Google services

Route: **Login Screen → Phone Sign-In Button**

---

Last Updated: November 23, 2025
