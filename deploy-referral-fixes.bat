@echo off
REM Referral System Fix Deployment Script (Windows)
REM This script deploys all fixes for the referral system

echo.
echo 🎁 REFERRAL SYSTEM FIX DEPLOYMENT
echo ==================================
echo.

REM Step 1: Deploy Firestore Indexes
echo 📊 Step 1: Deploying Firestore Indexes...
call firebase deploy --only firestore:indexes
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Failed to deploy Firestore indexes
    exit /b 1
)
echo ✅ Firestore indexes deployed successfully
echo.

REM Step 2: Deploy Firestore Rules
echo 🔒 Step 2: Deploying Firestore Rules...
call firebase deploy --only firestore:rules
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Failed to deploy Firestore rules
    exit /b 1
)
echo ✅ Firestore rules deployed successfully
echo.

REM Step 3: Build and Deploy Cloud Functions
echo ☁️  Step 3: Building and Deploying Cloud Functions...
cd functions
echo 📦 Installing dependencies...
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Failed to install dependencies
    cd ..
    exit /b 1
)

echo 🔨 Building TypeScript...
call npm run build
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Failed to build TypeScript
    cd ..
    exit /b 1
)

cd ..
echo 🚀 Deploying functions...
call firebase deploy --only functions
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Failed to deploy Cloud Functions
    exit /b 1
)
echo ✅ Cloud Functions deployed successfully
echo.

REM Step 4: Verify Deployment
echo 🔍 Step 4: Verifying Deployment...
echo.
echo Checking deployed functions...
call firebase functions:list
echo.

echo ✅ DEPLOYMENT COMPLETE!
echo.
echo 📋 NEXT STEPS:
echo 1. Test referral code creation (complete a user profile)
echo 2. Test referral code application (sign up with a code)
echo 3. Test reward credit (complete referred user's profile)
echo 4. Check Cloud Function logs: firebase functions:log
echo 5. Verify Firestore data in Firebase Console
echo.
echo 🐛 DEBUGGING:
echo - View logs: firebase functions:log --only onUserProfileComplete
echo - Check indexes: firebase firestore:indexes
echo - Test in Firebase Console ^> Firestore
echo.
pause
