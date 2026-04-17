/**
 * Analyze Firestore Collections
 * Lists all collections and their document counts
 */

const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp({
    projectId: 'dutype-860ac'
  });
}

const db = admin.firestore();

async function analyzeCollections() {
  console.log('🔍 Analyzing Firestore Collections\n');
  
  try {
    const collections = await db.listCollections();
    
    console.log(`📊 Found ${collections.length} collections:\n`);
    
    const collectionData = [];
    
    for (const collection of collections) {
      try {
        const snapshot = await collection.limit(1).get();
        const count = await collection.count().get();
        const docCount = count.data().count;
        
        let sampleDoc = null;
        if (!snapshot.empty) {
          const doc = snapshot.docs[0];
          sampleDoc = doc.data();
        }
        
        collectionData.push({
          name: collection.id,
          count: docCount,
          fields: sampleDoc ? Object.keys(sampleDoc).length : 0,
          sample: sampleDoc
        });
        
      } catch (error) {
        console.error(`Error analyzing ${collection.id}:`, error.message);
      }
    }
    
    // Sort by document count
    collectionData.sort((a, b) => b.count - a.count);
    
    console.log('Collection Name'.padEnd(30) + 'Documents'.padEnd(15) + 'Fields');
    console.log('─'.repeat(60));
    
    collectionData.forEach(col => {
      console.log(
        col.name.padEnd(30) + 
        col.count.toString().padEnd(15) + 
        col.fields
      );
    });
    
    console.log('\n📋 Detailed Analysis:\n');
    
    collectionData.forEach(col => {
      console.log(`\n${col.name}:`);
      console.log(`  Documents: ${col.count}`);
      console.log(`  Fields: ${col.fields}`);
      if (col.sample) {
        console.log(`  Sample fields: ${Object.keys(col.sample).slice(0, 10).join(', ')}`);
      }
    });
    
  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

analyzeCollections()
  .then(() => process.exit(0))
  .catch(error => {
    console.error('💥 Fatal error:', error);
    process.exit(1);
  });
