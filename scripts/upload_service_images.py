"""
DutyPe Service Image Uploader (Python)
-------------------------------------
Uploads 3D isometric clay render images for DutyPe Home Services to Firebase Storage.

Prerequisites:
    pip install firebase-admin

Usage:
    1. Place generated images in:
         scripts/images/services/<service_id>.webp
         scripts/images/categories/<category_id>.webp

    2. Run:
         python scripts/upload_service_images.py
"""

import os
import mimetypes
from pathlib import Path

try:
    import firebase_admin
    from firebase_admin import credentials, storage
except ImportError:
    print("Error: firebase-admin is not installed. Run: pip install firebase-admin")
    exit(1)

BUCKET_NAME = os.getenv("FIREBASE_STORAGE_BUCKET", "dutype-860ac.firebasestorage.app")
SCRIPT_DIR = Path(__file__).resolve().parent
KEY_PATH = Path(os.getenv("GOOGLE_APPLICATION_CREDENTIALS", SCRIPT_DIR / "serviceAccountKey.json"))

def main():
    print("=" * 52)
    print("  DutyPe 3D Clay Service Image Uploader (Python)")
    print(f"  Target Bucket: {BUCKET_NAME}")
    print("=" * 52 + "\n")

    if KEY_PATH.exists():
        cred = credentials.Certificate(str(KEY_PATH))
        firebase_admin.initialize_app(cred, {"storageBucket": BUCKET_NAME})
        print(f"✓ Authenticated using service account: {KEY_PATH}")
    else:
        firebase_admin.initialize_app(options={"storageBucket": BUCKET_NAME})
        print("ℹ Authenticating using Application Default Credentials")

    bucket = storage.bucket()
    images_dir = SCRIPT_DIR / "images"

    targets = [
        ("services", "services"),
        ("categories", "categories"),
    ]

    total_uploaded = 0

    for folder, storage_prefix in targets:
        dir_path = images_dir / folder
        if not dir_path.exists():
            dir_path.mkdir(parents=True, exist_ok=True)
            print(f"Created folder: {dir_path}")
            print(f"  -> Place your 3D isometric {folder} images (.webp / .png) here.\n")
            continue

        files = [f for f in dir_path.iterdir() if f.suffix.lower() in [".webp", ".png", ".jpg", ".jpeg"]]
        if not files:
            print(f"ℹ No image files found in {dir_path}")
            print(f"  -> Drop your 3D clay images into scripts/images/{folder}/ and run again.\n")
            continue

        print(f"\nUploading {len(files)} images from {folder}/...")

        for file_path in files:
            dest_blob = f"{storage_prefix}/{file_path.name}"
            content_type, _ = mimetypes.guess_type(str(file_path))
            if not content_type:
                content_type = "image/webp"

            try:
                blob = bucket.blob(dest_blob)
                blob.cache_control = "public, max-age=31536000, immutable"
                blob.upload_from_filename(str(file_path), content_type=content_type)

                public_url = f"https://firebasestorage.googleapis.com/v0/b/{BUCKET_NAME}/o/{dest_blob.replace('/', '%2F')}?alt=media"
                print(f"  ✓ Uploaded: {dest_blob}")
                print(f"    URL: {public_url}")
                total_uploaded += 1
            except Exception as e:
                print(f"  ✗ Failed to upload {file_path.name}: {e}")

    print("\n" + "=" * 52)
    print(f"Upload Complete! Total images uploaded: {total_uploaded}")
    print("All images are instantly cached on CDN & client devices.")
    print("=" * 52 + "\n")

if __name__ == "__main__":
    main()
