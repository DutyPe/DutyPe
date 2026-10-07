/**
 * DutyPe Service Image Uploader
 * -----------------------------
 * Uploads 3D isometric clay render images for DutyPe Home Services to Firebase Storage.
 * 
 * Usage:
 *   1. Place your generated images into:
 *        scripts/images/services/<service_id>.webp
 *        scripts/images/categories/<category_id>.webp
 * 
 *   2. Run:
 *        node scripts/upload_service_images.mjs
 * 
 * If you have a service account JSON, put it at scripts/serviceAccountKey.json or set GOOGLE_APPLICATION_CREDENTIALS.
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const BUCKET_NAME = process.env.FIREBASE_STORAGE_BUCKET || 'dutype-860ac.firebasestorage.app';

async function main() {
  console.log('====================================================');
  console.log('  DutyPe 3D Clay Service Image Uploader');
  console.log(`  Target Bucket: ${BUCKET_NAME}`);
  console.log('====================================================\n');

  // Try to load firebase-admin from functions/node_modules if not in root
  let admin;
  try {
    admin = (await import('firebase-admin')).default;
  } catch {
    const fallbackPath = path.resolve(__dirname, '../functions/node_modules/firebase-admin');
    admin = (await import(fallbackPath)).default;
  }

  // Initialize Firebase Admin
  const keyPath = process.env.GOOGLE_APPLICATION_CREDENTIALS || path.resolve(__dirname, 'serviceAccountKey.json');
  if (fs.existsSync(keyPath)) {
    const serviceAccount = JSON.parse(fs.readFileSync(keyPath, 'utf8'));
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      storageBucket: BUCKET_NAME,
    });
    console.log(`✓ Authenticated using service account: ${keyPath}`);
  } else {
    admin.initializeApp({
      storageBucket: BUCKET_NAME,
    });
    console.log('ℹ Authenticating using default Google Application Credentials / Firebase CLI');
  }

  const bucket = admin.storage().bucket();
  const imagesBaseDir = path.resolve(__dirname, 'images');

  const targets = [
    { folder: 'services', storagePrefix: 'services' },
    { folder: 'categories', storagePrefix: 'categories' },
  ];

  let totalUploaded = 0;

  for (const { folder, storagePrefix } of targets) {
    const dir = path.join(imagesBaseDir, folder);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
      console.log(`Created folder: ${dir}`);
      console.log(`  -> Place your 3D isometric ${folder} images (.webp / .png) here.\n`);
      continue;
    }

    const files = fs.readdirSync(dir).filter(f => /\.(webp|png|jpg|jpeg)$/i.test(f));
    if (files.length === 0) {
      console.log(`ℹ No image files found in ${dir}`);
      console.log(`  -> Drop your 3D clay images into scripts/images/${folder}/ and run again.\n`);
      continue;
    }

    console.log(`\nUploading ${files.length} images from ${folder}/...`);

    for (const file of files) {
      const filePath = path.join(dir, file);
      const ext = path.extname(file).toLowerCase();
      const contentType = ext === '.webp' ? 'image/webp' : ext === '.png' ? 'image/png' : 'image/jpeg';
      const destination = `${storagePrefix}/${file}`;

      try {
        await bucket.upload(filePath, {
          destination,
          metadata: {
            contentType,
            cacheControl: 'public, max-age=31536000, immutable',
          },
        });

        const publicUrl = `https://firebasestorage.googleapis.com/v0/b/${BUCKET_NAME}/o/${encodeURIComponent(destination)}?alt=media`;
        console.log(`  ✓ Uploaded: ${destination}`);
        console.log(`    URL: ${publicUrl}`);
        totalUploaded++;
      } catch (err) {
        console.error(`  ✗ Failed to upload ${file}:`, err.message);
      }
    }
  }

  console.log('\n====================================================');
  console.log(`Upload Complete! Total images uploaded: ${totalUploaded}`);
  console.log('All images are instantly cached on CDN & client devices.');
  console.log('====================================================\n');
}

main().catch(err => {
  console.error('\nError running uploader:', err);
  process.exit(1);
});
