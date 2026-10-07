import { randomUUID } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb, getFirebaseAdminStorageBucket } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
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
    return NextResponse.json({ error: "Image must be 5 MB or smaller." }, { status: 400 });
  }

  const id = String(formData.get("id") ?? "").trim().toLowerCase();
  if (!id) {
    return NextResponse.json({ error: "Item ID is required." }, { status: 400 });
  }

  const type = String(formData.get("type") ?? "service").trim().toLowerCase();
  const isCategory = type === "category";
  const storagePrefix = isCategory ? "categories" : "services";
  const objectPath = `${storagePrefix}/${id}.webp`;

  try {
    const bytes = Buffer.from(await uploaded.arrayBuffer());
    const token = randomUUID();

    const bucket = getFirebaseAdminStorageBucket();
    const file = bucket.file(objectPath);

    await file.save(bytes, {
      resumable: false,
      metadata: {
        contentType: "image/webp",
        cacheControl: "public, max-age=31536000, immutable",
        metadata: {
          firebaseStorageDownloadTokens: token
        }
      }
    });

    const encodedPath = encodeURIComponent(objectPath);
    const publicUrl = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodedPath}?alt=media&token=${token}`;

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

    return NextResponse.json({
      ok: true,
      url: publicUrl,
      id,
      type: isCategory ? "category" : "service",
      size: uploaded.size
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to upload image.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
