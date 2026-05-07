import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

const HIDDEN_PROFILE_FIELDS = new Set([
  "profileImage",
  "profileImageUrl",
  "photoUrl",
  "imageUrl",
  "geohash",
  "businessGeohash",
  "phoneRolePhoneNumber",
  "phoneRoleName",
  "phoneNumber",
  "contactPhone"
]);

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

function firstNonEmptyString(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }

  return "";
}

function sanitizeProfile(userId: string, raw: Record<string, unknown>) {
  const row: Record<string, unknown> = { id: userId };
  const phone = firstNonEmptyString(raw.phone, raw.phoneNumber, raw.contactPhone);

  Object.entries(raw).forEach(([field, value]) => {
    if (HIDDEN_PROFILE_FIELDS.has(field)) return;
    row[field] = value;
  });

  if (phone) {
    row.phone = phone;
  }

  return row;
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const snapshot = await db.collection("employer_profiles").limit(5000).get();
    const profiles = snapshot.docs.map((doc) => sanitizeProfile(doc.id, asRecord(doc.data())));

    return NextResponse.json({ profiles });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load employer profiles.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
