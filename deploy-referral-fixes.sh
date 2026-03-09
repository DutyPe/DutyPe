#!/bin/bash

# Referral System Fix Deployment Script
# This script deploys all fixes for the referral system

echo "🎁 REFERRAL SYSTEM FIX DEPLOYMENT"
echo "=================================="
echo ""

# Step 1: Deploy Firestore Indexes
echo "📊 Step 1: Deploying Firestore Indexes..."
firebase deploy --only firestore:indexes
if [ $? -eq 0 ]; then
    echo "✅ Firestore indexes deployed successfully"
else
    echo "❌ Failed to deploy Firestore indexes"
    exit 1
fi
echo ""

# Step 2: Deploy Firestore Rules
echo "🔒 Step 2: Deploying Firestore Rules..."
firebase deploy --only firestore:rules
if [ $? -eq 0 ]; then
    echo "✅ Firestore rules deployed successfully"
else
    echo "❌ Failed to deploy Firestore rules"
    exit 1
fi
echo ""

# Step 3: Build and Deploy Cloud Functions
echo "☁️  Step 3: Building and Deploying Cloud Functions..."
cd functions
echo "📦 Installing dependencies..."
npm install
if [ $? -ne 0 ]; then
    echo "❌ Failed to install dependencies"
    exit 1
fi

echo "🔨 Building TypeScript..."
npm run build
if [ $? -ne 0 ]; then
    echo "❌ Failed to build TypeScript"
    exit 1
fi

cd ..
echo "🚀 Deploying functions..."
firebase deploy --only functions
if [ $? -eq 0 ]; then
    echo "✅ Cloud Functions deployed successfully"
else
    echo "❌ Failed to deploy Cloud Functions"
    exit 1
fi
echo ""

# Step 4: Verify Deployment
echo "🔍 Step 4: Verifying Deployment..."
echo ""
echo "Checking deployed functions..."
firebase functions:list | grep -E "(onUserProfileComplete|applyReferralCode|onReferredUserProfileComplete|requestWithdrawal)"
echo ""

echo "✅ DEPLOYMENT COMPLETE!"
echo ""
echo "📋 NEXT STEPS:"
echo "1. Test referral code creation (complete a user profile)"
echo "2. Test referral code application (sign up with a code)"
echo "3. Test reward credit (complete referred user's profile)"
echo "4. Check Cloud Function logs: firebase functions:log"
echo "5. Verify Firestore data in Firebase Console"
echo ""
echo "🐛 DEBUGGING:"
echo "- View logs: firebase functions:log --only onUserProfileComplete"
echo "- Check indexes: firebase firestore:indexes"
echo "- Test in Firebase Console > Firestore"
echo ""
