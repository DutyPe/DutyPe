/**
 * Test Dual Role Implementation
 * 
 * This script tests if dual role data is being saved correctly in Firestore
 */

const admin = require('firebase-admin');

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.applicationDefault()
  });
}

const db = admin.firestore();

async function testDualRoleStructure() {
  console.log('🧪 Testing Dual Role Data Structure...\n');
  
  try {
    // Query for users with multiple roles
    const usersSnapshot = await db.collection('users')
      .where('roles', 'array-contains-any', ['WORKER', 'EMPLOYER'])
      .limit(5)
      .get();
    
    if (usersSnapshot.empty) {
      console.log('⚠️  No users found with roles array. This might be expected if no dual-role users exist yet.\n');
      
      // Show example of what the structure should look like
      console.log('📋 Expected User Document Structure:');
      console.log(JSON.stringify({
        id: "user123",
        phone: "+919876543210",
        fullName: "John Doe",
        email: "john@example.com",
        roles: ["WORKER", "EMPLOYER"],
        activeRole: "WORKER",
        profileCompleted: true,
        
        // Worker-specific fields
        skills: "Plumber, Electrician",
        experience: "5 years",
        dateOfBirth: "1990-01-01",
        gender: "Male",
        
        // Employer-specific fields
        companyName: "ABC Company",
        industry: "Construction",
        companySize: "10-50",
        businessAddress: "123 Main St",
        gstNumber: "GST123456",
        
        // Shared fields
        profileImageUrl: "https://...",
        address: "123 Main St",
        latitude: 28.6139,
        longitude: 77.2090,
        fcmToken: "fcm_token_here",
        createdAt: 1234567890,
        isActive: true
      }, null, 2));
      
      return;
    }
    
    console.log(`✅ Found ${usersSnapshot.size} user(s) with roles array\n`);
    
    usersSnapshot.forEach((doc) => {
      const data = doc.data();
      console.log(`\n📄 User ID: ${doc.id}`);
      console.log(`   Phone: ${data.phone || 'N/A'}`);
      console.log(`   Name: ${data.fullName || data.companyName || 'N/A'}`);
      console.log(`   Roles: ${JSON.stringify(data.roles || [])}`);
      console.log(`   Active Role: ${data.activeRole || 'N/A'}`);
      console.log(`   Profile Completed: ${data.profileCompleted || false}`);
      
      // Check for worker-specific fields
      const hasWorkerData = !!(data.skills || data.experience);
      console.log(`   Has Worker Data: ${hasWorkerData ? '✅' : '❌'}`);
      if (hasWorkerData) {
        console.log(`     - Skills: ${data.skills || 'N/A'}`);
        console.log(`     - Experience: ${data.experience || 'N/A'}`);
      }
      
      // Check for employer-specific fields
      const hasEmployerData = !!(data.companyName || data.industry);
      console.log(`   Has Employer Data: ${hasEmployerData ? '✅' : '❌'}`);
      if (hasEmployerData) {
        console.log(`     - Company: ${data.companyName || 'N/A'}`);
        console.log(`     - Industry: ${data.industry || 'N/A'}`);
      }
      
      // Validate dual role structure
      const isDualRole = data.roles && data.roles.length > 1;
      console.log(`   Is Dual Role: ${isDualRole ? '✅' : '❌'}`);
      
      if (isDualRole) {
        const hasActiveRole = data.activeRole && data.roles.includes(data.activeRole);
        console.log(`   Active Role Valid: ${hasActiveRole ? '✅' : '❌'}`);
        
        if (!hasActiveRole) {
          console.log(`   ⚠️  WARNING: activeRole "${data.activeRole}" not in roles array ${JSON.stringify(data.roles)}`);
        }
      }
    });
    
    console.log('\n✅ Dual role structure test complete!\n');
    
  } catch (error) {
    console.error('❌ Error testing dual role structure:', error);
  }
}

async function testSpecificUser(userId) {
  console.log(`\n🔍 Testing specific user: ${userId}\n`);
  
  try {
    const userDoc = await db.collection('users').doc(userId).get();
    
    if (!userDoc.exists) {
      console.log('❌ User not found');
      return;
    }
    
    const data = userDoc.data();
    console.log('📄 User Document:');
    console.log(JSON.stringify(data, null, 2));
    
    // Validate structure
    console.log('\n🔍 Validation:');
    console.log(`   Has roles array: ${Array.isArray(data.roles) ? '✅' : '❌'}`);
    console.log(`   Has activeRole: ${data.activeRole ? '✅' : '❌'}`);
    console.log(`   Roles count: ${data.roles?.length || 0}`);
    
    if (data.roles && data.activeRole) {
      console.log(`   Active role in roles array: ${data.roles.includes(data.activeRole) ? '✅' : '❌'}`);
    }
    
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

// Run tests
const args = process.argv.slice(2);
if (args.length > 0) {
  testSpecificUser(args[0]);
} else {
  testDualRoleStructure();
}
