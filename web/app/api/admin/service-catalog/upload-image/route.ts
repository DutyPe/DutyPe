import { randomUUID } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";
import sharp from "sharp";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb, getFirebaseAdminStorageBucket } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;
const SUPPORTED_MIME_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let formData: FormData;
  try {
    formData = await request.formData();
  } catch {
    return NextResponse.json({ error: "Invalid multipart form data." }, { status: 400 });
  }

  const uploaded = formData.get("image");
  if (!(uploaded instanceof File)) {
    return NextResponse.json({ error: "Image file is required." }, { status: 400 });
  }

  if (uploaded.size <= 0) {
    return NextResponse.json({ error: "Image file is empty." }, { status: 400 });
  }

  if (uploaded.size > MAX_IMAGE_SIZE_BYTES) {
    return NextResponse.json({ error: "Image must be 10 MB or smaller." }, { status: 400 });
  }

  const id = String(formData.get("id") ?? "").trim().toLowerCase();
  if (!id) {
    return NextResponse.json({ error: "Item ID is required." }, { status: 400 });
  }

  const type = String(formData.get("type") ?? "service").trim().toLowerCase();
  const isCategory = type === "category";
  const isBanner = type === "banner";
  const isJson = uploaded.name.endsWith(".json") || uploaded.type === "application/json";
  const ext = isJson ? "json" : "webp";
  const contentType = isJson ? "application/json" : "image/webp";

  const storagePrefix = isBanner ? "banners" : (isCategory ? "categories" : "services");
  const objectPath = isBanner ? `banners/${id}_${Date.now()}.${ext}` : `${storagePrefix}/${id}.webp`;

  try {
    let uploadBytes: Buffer<ArrayBufferLike> = Buffer.from(await uploaded.arrayBuffer());

    if (!isJson) {
      try {
        const maxWidth = isBanner ? 1000 : 360;
        const maxHeight = isBanner ? 500 : 360;
        let quality = isBanner ? 80 : 75;

        let processed = await sharp(uploadBytes)
          .resize({
            width: maxWidth,
            height: maxHeight,
            fit: isBanner ? "inside" : "cover",
            withoutEnlargement: true
          })
          .webp({ quality, effort: 4 })
          .toBuffer();

        // Target <= 35-40 KB for service/category icons and <= 75 KB for banners
        const targetMaxBytes = isBanner ? 75 * 1024 : 35 * 1024;
        if (processed.length > targetMaxBytes) {
          processed = await sharp(uploadBytes)
            .resize({
              width: isBanner ? 800 : 280,
              height: isBanner ? 400 : 280,
              fit: isBanner ? "inside" : "cover",
              withoutEnlargement: true
            })
            .webp({ quality: 60, effort: 6 })
            .toBuffer();
        }
        uploadBytes = processed;
      } catch (sharpError) {
        console.warn("Sharp WebP compression failed, using original buffer:", sharpError);
      }
    }

    const token = randomUUID();

    const bucket = getFirebaseAdminStorageBucket();
    const file = bucket.file(objectPath);

    await file.save(uploadBytes, {
      resumable: false,
      metadata: {
        contentType,
        cacheControl: "public, max-age=31536000, immutable",
        metadata: {
          firebaseStorageDownloadTokens: token
        }
      }
    });

    const encodedPath = encodeURIComponent(objectPath);
    const publicUrl = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodedPath}?alt=media&token=${token}`;

    if (!isBanner) {
      // Update Firestore app_config/services directly with Admin SDK
      const db = getFirebaseAdminDb();
      const docRef = db.collection("app_config").doc("services");
      const snap = await docRef.get();
      const data = (snap.data() || {}) as Record<string, unknown>;

      if (isCategory) {
        const existingCats = Array.isArray(data.categories) ? [...data.categories] : [];
        const idx = existingCats.findIndex((c: any) => String(c?.id).toLowerCase() === id);
        if (idx >= 0) {
          existingCats[idx] = { ...existingCats[idx], imageUrl: publicUrl };
        } else {
          existingCats.push({ id, imageUrl: publicUrl });
        }
        await docRef.set({ ...data, categories: existingCats }, { merge: true });
      } else {
        const existingSvcs = Array.isArray(data.services) ? [...data.services] : [];
        const idx = existingSvcs.findIndex((s: any) => String(s?.id).toLowerCase() === id);
        if (idx >= 0) {
          existingSvcs[idx] = { ...existingSvcs[idx], imageUrl: publicUrl };
        } else {
          existingSvcs.push({ id, imageUrl: publicUrl });
        }
        await docRef.set({ ...data, services: existingSvcs }, { merge: true });
      }
    }

    return NextResponse.json({
      ok: true,
      url: publicUrl,
      id,
      type: isBanner ? "banner" : (isCategory ? "category" : "service"),
      size: uploaded.size
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to upload image.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
