/**
 * Fix UNKNOWN Category Jobs
 * 
 * This script automatically detects and assigns proper categories to jobs
 * that have category="UNKNOWN" based on their title and description.
 */

const admin = require('firebase-admin');
const { loadServiceAccount } = require('./lib/firebase-admin-service-account');

// Initialize Firebase Admin
const serviceAccount = loadServiceAccount();
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'dutype-860ac'
});

const db = admin.firestore();

// Category detection keywords
const categoryKeywords = {
  MAID: ['maid', 'housekeeping', 'cleaning', 'cleaner', 'domestic', 'house keeping'],
  COOK: ['cook', 'chef', 'kitchen', 'cooking', 'food preparation'],
  DRIVER: ['driver', 'driving', 'chauffeur', 'transport'],
  DELIVERY: ['delivery', 'courier', 'deliver', 'logistics', 'dispatch'],
  SECURITY: ['security', 'guard', 'watchman', 'safety'],
  WAITER: ['waiter', 'waitress', 'server', 'restaurant', 'cafe', 'hotel staff'],
  HELPER: ['helper', 'assistant', 'support staff', 'general worker'],
  CARETAKER: ['caretaker', 'care taker', 'elderly care', 'patient care', 'nursing'],
  ELECTRICIAN: ['electrician', 'electrical', 'wiring', 'electric'],
  PLUMBER: ['plumber', 'plumbing', 'pipe', 'water'],
  CARPENTER: ['carpenter', 'carpentry', 'wood', 'furniture'],
  PAINTER: ['painter', 'painting', 'paint'],
  RECEPTIONIST: ['receptionist', 'front desk', 'reception'],
  CASHIER: ['cashier', 'billing', 'counter'],
  PACKER: ['packer', 'packing', 'packaging', 'warehouse'],
  MECHANIC: ['mechanic', 'repair', 'maintenance', 'technician'],
  GARDENER: ['gardener', 'gardening', 'landscaping', 'plants'],
  TAILOR: ['tailor', 'stitching', 'sewing', 'alteration']
};

/**
 * Detect category from job title and description
 */
function detectCategory(title, description = '') {
  const text = `${title} ${description}`.toLowerCase();
  
  for (const [category, keywords] of Object.entries(categoryKeywords)) {
    for (const keyword of keywords) {
      if (text.includes(keyword)) {
        return category;
      }
    }
  }
  
  return 'OTHER'; // Default if no match found
}

async function fixUnknownCategories() {
  console.log('🔍 Finding jobs with UNKNOWN or missing category...\n');
  
  try {
    // Get ALL jobs and filter client-side (Firestore doesn't support OR on same field)
    const snapshot = await db.collection('jobs').get();
    
    const unknownJobs = [];
    snapshot.forEach(doc => {
      const data = doc.data();
      const category = data.category || 'UNKNOWN';
      if (category === 'UNKNOWN' || category === '' || !data.category) {
        unknownJobs.push({ doc, data });
      }
    });
    
    console.log(`Found ${unknownJobs.length} jobs with UNKNOWN or missing category\n`);
    
    if (unknownJobs.length === 0) {
      console.log('✅ No UNKNOWN category jobs found!');
      return;
    }
    
    let fixed = 0;
    let failed = 0;
    const categoryCounts = {};
    
    // Process each job
    for (const { doc, data } of unknownJobs) {
      const jobId = data.jobId || doc.id;
      const title = data.title || '';
      const description = data.description || '';
      
      // Detect proper category
      const newCategory = detectCategory(title, description);
      
      try {
        // Update the job
        await doc.ref.update({
          category: newCategory,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        
        // Track stats
        categoryCounts[newCategory] = (categoryCounts[newCategory] || 0) + 1;
        fixed++;
        
        console.log(`✅ ${jobId}: "${title}" → ${newCategory}`);
      } catch (error) {
        console.error(`❌ Failed to update ${jobId}: ${error.message}`);
        failed++;
      }
    }
    
    // Print summary
    console.log('\n' + '='.repeat(60));
    console.log('📊 SUMMARY');
    console.log('='.repeat(60));
    console.log(`Total processed: ${unknownJobs.length}`);
    console.log(`✅ Successfully fixed: ${fixed}`);
    console.log(`❌ Failed: ${failed}`);
    console.log('\nNew category distribution:');
    
    // Sort by count
    const sorted = Object.entries(categoryCounts).sort((a, b) => b[1] - a[1]);
    for (const [category, count] of sorted) {
      const percentage = ((count / fixed) * 100).toFixed(1);
      console.log(`  ${category.padEnd(20)} ${count.toString().padStart(3)} (${percentage}%)`);
    }
    console.log('='.repeat(60));
    
  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

// Run the fix
fixUnknownCategories()
  .then(() => {
    console.log('\n✅ Script completed successfully');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Script failed:', error);
    process.exit(1);
  });
