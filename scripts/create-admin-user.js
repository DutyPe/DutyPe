/**
 * Create Admin User for DutyPe Admin Panel
 * 
 * This script creates an admin user in Firebase Authentication
 * Run: node scripts/create-admin-user.js
 */

const admin = require('firebase-admin');
const readline = require('readline');

// Initialize Firebase Admin
const serviceAccount = require('../serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutypeapp'
});

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

function question(query) {
  return new Promise(resolve => rl.question(query, resolve));
}

async function createAdminUser() {
  console.log('\n🔐 DutyPe Admin User Creation\n');
  console.log('This will create an admin user for the job posting panel.\n');

  try {
    const email = await question('Enter admin email (e.g., admin@dutype.com): ');
    const password = await question('Enter admin password (min 6 characters): ');
    const displayName = await question('Enter admin name (optional): ') || 'DutyPe Admin';

    if (!email || !password) {
      console.error('❌ Email and password are required!');
      rl.close();
      return;
    }

    if (password.length < 6) {
      console.error('❌ Password must be at least 6 characters!');
      rl.close();
      return;
    }

    console.log('\n⏳ Creating admin user...\n');

    const userRecord = await admin.auth().createUser({
      email: email,
      password: password,
      displayName: displayName,
      emailVerified: true
    });

    console.log('✅ Admin user created successfully!\n');
    console.log('📧 Email:', userRecord.email);
    console.log('🆔 UID:', userRecord.uid);
    console.log('👤 Name:', userRecord.displayName);
    console.log('\n🎉 You can now login at: https://dutypeapp.web.app/admin/login.html\n');

  } catch (error) {
    if (error.code === 'auth/email-already-exists') {
      console.error('❌ Error: This email is already registered!');
      console.log('\n💡 Tip: Use a different email or reset the password in Firebase Console.');
    } else {
      console.error('❌ Error creating admin user:', error.message);
    }
  } finally {
    rl.close();
  }
}

createAdminUser();
