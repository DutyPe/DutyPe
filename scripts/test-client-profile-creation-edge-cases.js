const admin = require('../functions/node_modules/firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account.js');
const { initializeApp } = require('../web/node_modules/firebase/app');
const { getAuth, signInWithCustomToken } = require('../web/node_modules/firebase/auth');
const { getFirestore, doc, setDoc, writeBatch, Timestamp, deleteField } = require('../web/node_modules/firebase/firestore');
const { getFunctions, httpsCallable } = require('../web/node_modules/firebase/functions');

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
const clientFunctions = getFunctions(clientApp, "us-central1");

async function runEdgeCaseTests() {
  console.log('=== RUNNING FIREBASE EDGE CASE TESTS FOR NEW USER PROFILES ===\n');

  const testUid = `edge_${Date.now()}`;
  const testPhone = `+9198765${Math.floor(10000 + Math.random() * 90000)}`;

  const customToken = await admin.auth().createCustomToken(testUid, {
    phone_number: testPhone
  });
  await signInWithCustomToken(clientAuth, customToken);
  console.log(`Signed in client user: ${testUid} (${testPhone})\n`);

  // Test 1: Short Bio (< 20 characters)
  console.log('Test 1: Short Bio (e.g. "Electrician") in worker_profiles');
  try {
    const workerRef = doc(clientDb, 'worker_profiles', testUid);
    await setDoc(workerRef, {
      userId: testUid,
      fullName: 'Test Short Bio',
      phone: testPhone,
      role: 'WORKER',
      bio: 'Electrician', // 11 chars
      createdAt: Timestamp.now(),
      updatedAt: Timestamp.now()
    }, { merge: true });
    console.log('  -> Short Bio: ALLOWED');
  } catch (e) {
    console.error('  -> Short Bio: FAILED! Error:', e.code, e.message);
  }

  // Test 1b: Full Worker Profile creation with location containing address
  console.log('\nTest 1b: Worker Profile batch with location metadata (address)');
  try {
    const batch = writeBatch(clientDb);
    const now = Timestamp.now();
    const phoneRef = doc(clientDb, 'phoneRoles', testPhone);
    batch.set(phoneRef, {
      phoneNumber: testPhone,
      role: 'WORKER',
      name: 'Test Full Worker',
      uid: testUid,
      createdAt: now,
      updatedAt: now
    }, { merge: true });

    const workerRef = doc(clientDb, 'worker_profiles', testUid);
    batch.set(workerRef, {
      userId: testUid,
      fullName: 'Test Full Worker',
      phone: testPhone,
      role: 'WORKER',
      updatedAt: now,
      createdAt: now,
      skills: ['electrician', 'plumber'],
      isAvailable: deleteField(),
      bio: 'Driver',
      location: {
        lat: 17.3850,
        lng: 78.4867,
        address: 'Banjara Hills, Hyderabad'
      },
      geohash: 'tepg15',
      address: 'Banjara Hills, Hyderabad',
      profileCompleted: true,
      isProfileComplete: true
    }, { merge: true });

    const userRef = doc(clientDb, 'users', testUid);
    batch.set(userRef, {
      userId: testUid,
      fullName: 'Test Full Worker',
      name: 'Test Full Worker',
      phone: testPhone,
      role: 'WORKER',
      activeRole: 'WORKER',
      profileCompleted: true,
      isProfileComplete: true,
      updatedAt: now
    }, { merge: true });

    await batch.commit();
    console.log('  -> Worker Profile Batch with location.address: ALLOWED');
  } catch (e) {
    console.error('  -> Worker Profile Batch with location.address: FAILED! Error:', e.code, e.message);
  }

  // Test 2: FieldValue.delete() for isAvailable on worker_profiles
  console.log('\nTest 2: deleteField() on isAvailable for worker_profiles');
  try {
    const workerRef = doc(clientDb, 'worker_profiles', testUid);
    await setDoc(workerRef, {
      updatedAt: Timestamp.now(),
      isAvailable: deleteField()
    }, { merge: true });
    console.log('  -> deleteField(isAvailable): ALLOWED');
  } catch (e) {
    console.error('  -> deleteField(isAvailable): FAILED! Error:', e.code, e.message);
  }


  // Test 3: Employer Profile Creation
  console.log('\nTest 3: Employer Profile creation');
  const employerUid = `emp_${Date.now()}`;
  const employerPhone = `+9198764${Math.floor(10000 + Math.random() * 90000)}`;
  const empToken = await admin.auth().createCustomToken(employerUid, {
    phone_number: employerPhone
  });
  await signInWithCustomToken(clientAuth, empToken);

  try {
    const batch = writeBatch(clientDb);
    const now = Timestamp.now();
    const phoneRef = doc(clientDb, 'phoneRoles', employerPhone);
    batch.set(phoneRef, {
      phoneNumber: employerPhone,
      role: 'EMPLOYER',
      name: 'Test Employer Company',
      uid: employerUid,
      employerType: 'COMPANY',
      createdAt: now,
      updatedAt: now
    });

    const empRef = doc(clientDb, 'employer_profiles', employerUid);
    batch.set(empRef, {
      userId: employerUid,
      fullName: 'Test Employer Company',
      companyName: 'Test Employer Company',
      phone: employerPhone,
      role: 'EMPLOYER',
      employerType: 'COMPANY',
      profileCompleted: true,
      isProfileComplete: true,
      createdAt: now,
      updatedAt: now,
      subscription: {
        status: 'NONE',
        planId: '',
        startDate: 0,
        expiryDate: 0,
        credits: { normal: 0, instant: 0 },
        trialJobsUsed: 0,
        trialInstantJobsUsed: 0
      }
    }, { merge: true });

    const userRef = doc(clientDb, 'users', employerUid);
    batch.set(userRef, {
      userId: employerUid,
      employerType: 'COMPANY',
      companyName: 'Test Employer Company',
      fullName: 'Test Employer Company',
      name: 'Test Employer Company',
      phone: employerPhone,
      role: 'EMPLOYER',
      activeRole: 'EMPLOYER',
      profileCompleted: true,
      isProfileComplete: true,
      updatedAt: now
    }, { merge: true });

    await batch.commit();
    console.log('  -> Employer Profile batch: ALLOWED');
  } catch (e) {
    console.error('  -> Employer Profile batch: FAILED! Error:', e.code, e.message);
  }

  // Test 4: Profile photo update in ProfileCompletionService line 297:
  // firestore.collection(profileCollectionForRole(userRole)).document(userId).set({ profileImageUrl, updatedAt }, { merge: true })
  console.log('\nTest 4: uploadProfileImage updating profile document with only { profileImageUrl, updatedAt }');
  try {
    const workerImageUid = `img_${Date.now()}`;
    const imgToken = await admin.auth().createCustomToken(workerImageUid, {
      phone_number: '+919999999999'
    });
    await signInWithCustomToken(clientAuth, imgToken);

    // This doc does NOT exist yet!
    const profileRef = doc(clientDb, 'worker_profiles', workerImageUid);
    await setDoc(profileRef, {
      profileImageUrl: 'https://storage.googleapis.com/test.jpg',
      updatedAt: Timestamp.now()
    }, { merge: true });
    console.log('  -> Photo update on non-existent worker_profile: ALLOWED');
  } catch (e) {
    console.error('  -> Photo update on non-existent worker_profile: FAILED! Error:', e.code, e.message);
  }

  // Test 5: Call applyReferralCode callable
  console.log('\nTest 5: Call applyReferralCode Cloud Function');
  try {
    const applyRef = httpsCallable(clientFunctions, 'applyReferralCode');
    const result = await applyRef({
      referralCode: 'DUTY28AK',
      userRole: 'WORKER',
      userName: 'Test Callable User',
      userPhone: '+919999999999'
    });
    console.log('  -> applyReferralCode result:', result.data);
  } catch (e) {
    console.error('  -> applyReferralCode FAILED! Error:', e.code, e.message);
  }

  // Cleanup
  console.log('\nCleaning up test documents...');
  await admin.firestore().collection('worker_profiles').doc(testUid).delete();
  await admin.firestore().collection('phoneRoles').doc(testPhone).delete();
  await admin.firestore().collection('users').doc(testUid).delete();
  await admin.firestore().collection('phoneRoles').doc(employerPhone).delete();
  await admin.firestore().collection('employer_profiles').doc(employerUid).delete();
  await admin.firestore().collection('users').doc(employerUid).delete();

  console.log('\nDone with tests!');
  process.exit(0);
}

runEdgeCaseTests().catch((e) => {
  console.error('Test execution error:', e);
  process.exit(1);
});
