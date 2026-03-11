#!/usr/bin/env node

/**
 * Admin Debug Tool - Check User by Phone Number
 * 
 * Usage: node scripts/check-user-by-phone.js <phone_number>
 * Example: node scripts/check-user-by-phone.js +919876543210
 * 
 * This script helps debug login issues by:
 * 1. Checking if user exists in Firestore (users collection)
 * 2. Checking if user exists in Firebase Authentication
 * 3. Showing all phone number variants tried
 * 4. Displaying user data if found
 */

const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

/**
 * Normalize phone number to consistent format
 * Matches PhoneNumberUtils.kt logic
 */
function normalizePhone(phoneNumber) {
  const cleaned = phoneNumber.replace(/[^0-9+]/g, '');
  
  if (cleaned.startsWith('+')) {
    return cleaned;
  } else if (cleaned.startsWith('91')) {
    return '+' + cleaned;
  } else {
    return '+91' + cleaned;
  }
}

/**
 * Get all phone number variants to try
 * Matches PhoneNumberUtils.kt logic
 */
function getPhoneVariants(phoneNumber) {
  const normalized = normalizePhone(phoneNumber);
  
  return [
    normalized,                      // +919876543210
    normalized.replace('+', ''),     // 919876543210
    normalized.replace('+91', '')    // 9876543210
  ].filter((v, i, a) => a.indexOf(v) === i); // Remove duplicates
}

/**
 * Check if user exists in Firestore
 */
async function checkFirestore(phoneNumber) {
  console.log(`\n🔍 === FIRESTORE CHECK ===\n`);
  console.log(`Input phone: ${phoneNumber}`);
  
  const variants = getPhoneVariants(phoneNumber);
  console.log(`Variants to try: ${variants.join(', ')}\n`);
  
  // Try phone field
  for (const variant of variants) {
    console.log(`Trying phone field with variant: ${variant}`);
    
    const phoneSnapshot = await db.collection('users')
      .where('phone', '==', variant)
      .limit(1)
      .get();
    
    console.log(`  Query returned ${phoneSnapshot.size} documents`);
    
    if (!phoneSnapshot.empty) {
      const doc = phoneSnapshot.docs[0];
      console.log(`\n✅ FOUND with phone field!`);
      console.log(`Document ID: ${doc.id}`);
      console.log(`\nUser Data:`);
      console.log(JSON.stringify(doc.data(), null, 2));
      return { found: true, uid: doc.id, data: doc.data() };
    }
  }
  
  // Try phoneNumber field
  for (const variant of variants) {
    console.log(`Trying phoneNumber field with variant: ${variant}`);
    
    const phoneNumberSnapshot = await db.collection('users')
      .where('phoneNumber', '==', variant)
      .limit(1)
      .get();
    
    console.log(`  Query returned ${phoneNumberSnapshot.size} documents`);
    
    if (!phoneNumberSnapshot.empty) {
      const doc = phoneNumberSnapshot.docs[0];
      console.log(`\n✅ FOUND with phoneNumber field!`);
      console.log(`Document ID: ${doc.id}`);
      console.log(`\nUser Data:`);
      console.log(JSON.stringify(doc.data(), null, 2));
      return { found: true, uid: doc.id, data: doc.data() };
    }
  }
  
  console.log(`\n❌ User NOT FOUND in Firestore`);
  return { found: false };
}

/**
 * Check if user exists in Firebase Authentication
 */
async function checkAuth(phoneNumber) {
  console.log(`\n🔍 === FIREBASE AUTH CHECK ===\n`);
  
  try {
    const normalized = normalizePhone(phoneNumber);
    console.log(`Checking Auth with: ${normalized}`);
    
    const userRecord = await admin.auth().getUserByPhoneNumber(normalized);
    
    console.log(`\n✅ FOUND in Firebase Auth!`);
    console.log(`UID: ${userRecord.uid}`);
    console.log(`Phone: ${userRecord.phoneNumber}`);
    console.log(`Created: ${new Date(userRecord.metadata.creationTime).toISOString()}`);
    console.log(`Last Sign In: ${new Date(userRecord.metadata.lastSignInTime).toISOString()}`);
    
    return { found: true, uid: userRecord.uid, phone: userRecord.phoneNumber };
  } catch (authError) {
    console.log(`\n❌ User NOT FOUND in Firebase Auth`);
    console.log(`Error: ${authError.message}`);
    return { found: false };
  }
}

/**
 * Main function
 */
async function main() {
  const phone = process.argv[2];
  
  if (!phone) {
    console.error('\n❌ Error: Phone number required');
    console.error('\nUsage: node scripts/check-user-by-phone.js <phone_number>');
    console.error('Example: node scripts/check-user-by-phone.js +919876543210\n');
    process.exit(1);
  }
  
  console.log(`\n${'='.repeat(60)}`);
  console.log(`USER LOOKUP DEBUG TOOL`);
  console.log(`${'='.repeat(60)}`);
  
  // Check Firestore
  const firestoreResult = await checkFirestore(phone);
  
  // Check Firebase Auth
  const authResult = await checkAuth(phone);
  
  // Summary
  console.log(`\n${'='.repeat(60)}`);
  console.log(`SUMMARY`);
  console.log(`${'='.repeat(60)}\n`);
  
  if (firestoreResult.found && authResult.found) {
    console.log(`✅ User exists in BOTH Firestore and Auth`);
    console.log(`   Firestore UID: ${firestoreResult.uid}`);
    console.log(`   Auth UID: ${authResult.uid}`);
    
    if (firestoreResult.uid === authResult.uid) {
      console.log(`   ✅ UIDs match - everything is correct!`);
    } else {
      console.log(`   ⚠️  UIDs DON'T MATCH - data inconsistency!`);
    }
  } else if (firestoreResult.found && !authResult.found) {
    console.log(`⚠️  User exists in Firestore but NOT in Auth`);
    console.log(`   This is unusual - user may have been deleted from Auth`);
  } else if (!firestoreResult.found && authResult.found) {
    console.log(`⚠️  User exists in Auth but NOT in Firestore`);
    console.log(`   This causes "no account existed" login error!`);
    console.log(`   Solution: Recreate Firestore document for UID: ${authResult.uid}`);
  } else {
    console.log(`❌ User does NOT exist in either Firestore or Auth`);
    console.log(`   User needs to register`);
  }
  
  console.log();
}

// Run
main()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('\n❌ Fatal Error:', error);
    process.exit(1);
  });
