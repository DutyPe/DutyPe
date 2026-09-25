const admin = require('../functions/node_modules/firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account.js');
const { initializeApp } = require('../web/node_modules/firebase/app');
const { getAuth, signInWithCustomToken } = require('../web/node_modules/firebase/auth');
const { getFirestore, doc, setDoc, writeBatch, serverTimestamp, Timestamp } = require('../web/node_modules/firebase/firestore');

admin.initializeApp({ credential: admin.credential.cert(loadServiceAccount()) });

const firebaseConfig = {
  apiKey: "AIzaSyDFIzb8G90t3OjUShiceM8AeEdKzJ640_c",
  authDomain: "dutype-860ac.firebaseapp.com",
  projectId: "dutype-860ac",
  storageBucket: "dutype-860ac.firebasestorage.app",
  messagingSenderId: "1024525807494",
  appId: "1:1024525807494:web:b425aaecaf4c873235c607"
};

const clientApp = initializeApp(firebaseConfig);
const clientAuth = getAuth(clientApp);
const clientDb = getFirestore(clientApp);

async function runTest() {
  const testUid = `test_worker_${Date.now()}`;
  const testPhone = `+9198765${Math.floor(10000 + Math.random() * 90000)}`;
  console.log(`Testing with UID: ${testUid}, Phone: ${testPhone}`);

  // 1. Create custom token with phone_number claim
  const customToken = await admin.auth().createCustomToken(testUid, {
    phone_number: testPhone
  });

  // 2. Sign in as client
  const userCredential = await signInWithCustomToken(clientAuth, customToken);
  console.log(`Signed in as client user: ${userCredential.user.uid}`);

  // 3. Test Step 1: completeRegistration
  console.log('\n--- Step 1: Testing completeRegistration ---');
  try {
    const batch = writeBatch(clientDb);
    const now = Timestamp.now();

    // phoneRoles doc
    const phoneRoleRef = doc(clientDb, 'phoneRoles', testPhone);
    batch.set(phoneRoleRef, {
      phoneNumber: testPhone,
      role: 'WORKER',
      name: 'Test Worker',
      uid: testUid,
      createdAt: now,
      updatedAt: now
    });

    // worker_profiles doc
    const profileRef = doc(clientDb, 'worker_profiles', testUid);
    batch.set(profileRef, {
      userId: testUid,
      phone: testPhone,
      fullName: 'Test Worker',
      role: 'WORKER',
      createdAt: now,
      updatedAt: now
    }, { merge: true });

    // users doc
    const userRef = doc(clientDb, 'users', testUid);
    batch.set(userRef, {
      userId: testUid,
      phone: testPhone,
      phoneNumber: testPhone,
      fullName: 'Test Worker',
      name: 'Test Worker',
      role: 'WORKER',
      createdAt: now,
      updatedAt: now
    }, { merge: true });

    await batch.commit();
    console.log('✓ completeRegistration succeeded');
  } catch (err) {
    console.error('✗ completeRegistration failed:', err);
  }

  // 4. Test Step 2: saveWorkerProfileData (as sent by MandatoryWorkerProfileSetupScreen)
  console.log('\n--- Step 2: Testing saveWorkerProfileData ---');
  try {
    const batch = writeBatch(clientDb);
    const now = Timestamp.now();

    // phoneRoles
    const phoneRoleRef = doc(clientDb, 'phoneRoles', testPhone);
    batch.set(phoneRoleRef, {
      phoneNumber: testPhone,
      role: 'WORKER',
      name: 'Test Worker',
      uid: testUid,
      updatedAt: now
    }, { merge: true });

    // worker_profiles
    const workerRef = doc(clientDb, 'worker_profiles', testUid);
    const workerProfile = {
      userId: testUid,
      fullName: 'Test Worker',
      phone: testPhone,
      role: 'WORKER',
      updatedAt: now,
      skills: ['plumber', 'electrician'],
      address: 'Madhapur, Hyderabad, Telangana, India',
      dateOfBirth: '15/08/1995',
      gender: 'Male',
      experience: '1-3 years',
      educationQualification: '10th pass',
      bio: 'Experienced electrician and plumber with 3 years experience',
      location: { lat: 17.4483, lng: 78.3915 },
      geohash: 'tepfz1',
      profileCompleted: true,
      isProfileComplete: true
    };
    batch.set(workerRef, workerProfile, { merge: true });

    // users
    const userRef = doc(clientDb, 'users', testUid);
    batch.set(userRef, {
      userId: testUid,
      fullName: 'Test Worker',
      name: 'Test Worker',
      phone: testPhone,
      role: 'WORKER',
      activeRole: 'WORKER',
      profileCompleted: true,
      isProfileComplete: true,
      updatedAt: now
    }, { merge: true });

    await batch.commit();
    console.log('✓ saveWorkerProfileData succeeded');
  } catch (err) {
    console.error('✗ saveWorkerProfileData failed:', err);
  }

  // 5. Clean up test documents
  console.log('\nCleaning up test docs...');
  await admin.firestore().collection('phoneRoles').doc(testPhone).delete();
  await admin.firestore().collection('worker_profiles').doc(testUid).delete();
  await admin.firestore().collection('users').doc(testUid).delete();
  console.log('Done!');
  process.exit(0);
}

runTest().catch((err) => {
  console.error('FATAL:', err);
  process.exit(1);
});
