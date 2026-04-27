import { randomUUID } from "node:crypto";

import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminStorageBucket } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
const SUPPORTED_MIME_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

function extensionForMimeType(mimeType: string) {
  switch (mimeType) {
    case "image/jpeg":
      return "jpg";
    case "image/png":
      return "png";
    case "image/webp":
      return "webp";
    default:
      return "jpg";
  }
}

function sanitizePathSegment(value: string) {
  const sanitized = value.replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 80);
  return sanitized || "admin";
}

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
    return NextResponse.json(
      { error: "Image must be 5 MB or smaller." },
      { status: 400 }
    );
  }

  const mimeType = uploaded.type || "";
  if (!SUPPORTED_MIME_TYPES.has(mimeType)) {
    return NextResponse.json(
      { error: "Only JPG, PNG, and WEBP images are supported." },
      { status: 400 }
    );
  }

  const employerIdRaw = String(formData.get("employerId") ?? "admin").trim();
  const employerId = sanitizePathSegment(employerIdRaw);

  try {
    const bytes = Buffer.from(await uploaded.arrayBuffer());
    const token = randomUUID();
    const extension = extensionForMimeType(mimeType);
    const filename = `job_image_${Date.now()}_${randomUUID().slice(0, 8)}.${extension}`;
    const objectPath = `job_images/${employerId}/${filename}`;

    const bucket = getFirebaseAdminStorageBucket();
    const file = bucket.file(objectPath);

    await file.save(bytes, {
      resumable: false,
      metadata: {
        contentType: mimeType,
        metadata: {
          firebaseStorageDownloadTokens: token
        }
      }
    });

    const encodedPath = encodeURIComponent(objectPath);
    const publicUrl = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodedPath}?alt=media&token=${token}`;

    return NextResponse.json({
      ok: true,
      url: publicUrl,
      path: objectPath,
      contentType: mimeType,
      size: uploaded.size
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to upload image.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
