import { NextRequest, NextResponse } from "next/server";

import { normalizeUserRecord } from "@/lib/firebase/admin-normalizers";
import { getAdminSession } from "@/lib/firebase/admin-session";
import { getFirebaseAdminDb, isFirebaseAdminConfigured } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

async function requireAuthorizedAdmin() {
  const session = await getAdminSession();

  if (!session) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  if (!isFirebaseAdminConfigured()) {
    return NextResponse.json(
      { error: "Firebase Admin SDK is not configured for this environment." },
      { status: 500 }
    );
  }

  return null;
}

export async function GET() {
  const unauthorized = await requireAuthorizedAdmin();
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();

    const [usersSnapshot, referralCodesSnapshot, workerProfilesSnapshot, employerProfilesSnapshot] =
      await Promise.all([
        db.collection("users").limit(500).get(),
        db.collection("referral_codes").limit(1000).get(),
        db.collection("worker_profiles").limit(1000).get(),
        db.collection("employer_profiles").limit(1000).get()
      ]);

    const referralCodeByUserId = new Map<string, string>();
    referralCodesSnapshot.forEach((item) => {
      const raw = asRecord(item.data());
      const userId = typeof raw.userId === "string" ? raw.userId : "";
      const code = typeof raw.code === "string" && raw.code.trim() ? raw.code.trim() : item.id;

      if (userId && code) {
        referralCodeByUserId.set(userId, code);
      }
    });

    const workerProfileById = new Map<string, Record<string, unknown>>();
    workerProfilesSnapshot.forEach((item) => {
      workerProfileById.set(item.id, asRecord(item.data()));
    });

    const employerProfileById = new Map<string, Record<string, unknown>>();
    employerProfilesSnapshot.forEach((item) => {
      employerProfileById.set(item.id, asRecord(item.data()));
    });

    const users = usersSnapshot.docs.map((item) => {
      const raw = asRecord(item.data());
      const normalized = normalizeUserRecord(item.id, raw, {
        workerProfile: workerProfileById.get(item.id) ?? null,
        employerProfile: employerProfileById.get(item.id) ?? null,
        referralCodeByUserId: referralCodeByUserId.get(item.id)
      });

      return {
        id: item.id,
        fullName: normalized.fullName,
        name: normalized.fullName,
        phone: normalized.phone,
        email: normalized.email,
        role: normalized.role,
        activeRole: normalized.activeRole,
        roles: normalized.roles,
        referralCode: normalized.referralCode,
        createdAt: normalized.joinedAt
      };
    });

    return NextResponse.json({ users });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load users.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateUserBody = {
  userId?: string;
  newRole?: string;
};

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdmin();
  if (unauthorized) {
    return unauthorized;
  }

  let body: UpdateUserBody;

  try {
    body = (await request.json()) as UpdateUserBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const userId = body.userId?.trim();
  const newRole = body.newRole?.trim().toUpperCase();

  if (!userId || !newRole || (newRole !== "WORKER" && newRole !== "EMPLOYER")) {
    return NextResponse.json({ error: "Invalid userId or role." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    const ref = db.collection("users").doc(userId);
    const snap = await ref.get();

    if (!snap.exists) {
      return NextResponse.json({ error: "User not found." }, { status: 404 });
    }

    const raw = asRecord(snap.data());
    const currentRoles = Array.isArray(raw.roles)
      ? raw.roles.filter((item): item is string => typeof item === "string")
      : [];
    const nextRoles = [...new Set([...currentRoles, newRole])];

    await ref.set(
      {
        role: newRole,
        activeRole: newRole,
        roles: nextRoles.length > 0 ? nextRoles : [newRole]
      },
      { merge: true }
    );

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update role.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type DeleteUserBody = {
  userId?: string;
};

export async function DELETE(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdmin();
  if (unauthorized) {
    return unauthorized;
  }

  let body: DeleteUserBody;

  try {
    body = (await request.json()) as DeleteUserBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const userId = body.userId?.trim();

  if (!userId) {
    return NextResponse.json({ error: "Missing userId." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    await db.collection("users").doc(userId).delete();
    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to delete user.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
