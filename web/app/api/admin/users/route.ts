import { NextRequest, NextResponse } from "next/server";

import { normalizeUserRecord } from "@/lib/firebase/admin-normalizers";
import { getAdminSession } from "@/lib/firebase/admin-session";
import {
  getFirebaseAdminAuth,
  getFirebaseAdminDb,
  isFirebaseAdminConfigured
} from "@/lib/firebase/admin-server";

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
    const auth = getFirebaseAdminAuth();

    const authUsersById = new Map<string, { email: string; phone: string; displayName: string }>();
    let pageToken: string | undefined;

    do {
      const page = await auth.listUsers(1000, pageToken);
      page.users.forEach((entry) => {
        authUsersById.set(entry.uid, {
          email: entry.email ?? "",
          phone: entry.phoneNumber ?? "",
          displayName: entry.displayName ?? ""
        });
      });
      pageToken = page.pageToken;
    } while (pageToken);

    const [usersSnapshot, referralCodesSnapshot, workerProfilesSnapshot, employerProfilesSnapshot] =
      await Promise.all([
        db.collection("users").limit(5000).get(),
        db.collection("referral_codes").limit(5000).get(),
        db.collection("worker_profiles").limit(5000).get(),
        db.collection("employer_profiles").limit(5000).get()
      ]);

    const userDocsById = new Map<string, Record<string, unknown>>();
    usersSnapshot.forEach((item) => {
      userDocsById.set(item.id, asRecord(item.data()));
    });

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

    const userIds = new Set<string>([...userDocsById.keys(), ...authUsersById.keys()]);

    const users = Array.from(userIds).map((userId) => {
      const rawDoc = userDocsById.get(userId) ?? {};
      const authUser = authUsersById.get(userId);

      const merged = {
        ...rawDoc,
        fullName:
          typeof rawDoc.fullName === "string" && rawDoc.fullName.trim()
            ? rawDoc.fullName
            : (authUser?.displayName ?? ""),
        phone:
          typeof rawDoc.phone === "string" && rawDoc.phone.trim()
            ? rawDoc.phone
            : (authUser?.phone ?? ""),
        email:
          typeof rawDoc.email === "string" && rawDoc.email.trim()
            ? rawDoc.email
            : (authUser?.email ?? "")
      };

      const normalized = normalizeUserRecord(userId, merged, {
        workerProfile: workerProfileById.get(userId) ?? null,
        employerProfile: employerProfileById.get(userId) ?? null,
        referralCodeByUserId: referralCodeByUserId.get(userId)
      });

      return {
        id: userId,
        fullName: normalized.fullName,
        name: normalized.fullName,
        phone: normalized.phone,
        email: normalized.email,
        role: normalized.role,
        activeRole: normalized.activeRole,
        roles: normalized.roles,
        referralCode: normalized.referralCode,
        createdAt: normalized.joinedAt,
        hasUserDoc: userDocsById.has(userId),
        hasAuthUser: authUsersById.has(userId)
      };
    }).sort((a, b) => String(b.createdAt ?? "").localeCompare(String(a.createdAt ?? "")));

    return NextResponse.json({ users });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load users.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateUserBody = {
  userId?: string;
  newRole?: string;
  fullName?: string;
  phone?: string;
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
  const fullName = typeof body.fullName === "string" ? body.fullName.trim() : undefined;
  const phone = typeof body.phone === "string" ? body.phone.trim() : undefined;

  if (!userId) {
    return NextResponse.json({ error: "Invalid userId." }, { status: 400 });
  }

  const hasRoleUpdate = Boolean(newRole);
  if (hasRoleUpdate && newRole !== "WORKER" && newRole !== "EMPLOYER") {
    return NextResponse.json({ error: "Invalid role." }, { status: 400 });
  }

  if (!hasRoleUpdate && fullName === undefined && phone === undefined) {
    return NextResponse.json({ error: "No updatable fields were provided." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    const auth = getFirebaseAdminAuth();
    const ref = db.collection("users").doc(userId);
    const snap = await ref.get();

    const raw = asRecord(snap.data());
    const currentRoles = Array.isArray(raw.roles)
      ? raw.roles.filter((item): item is string => typeof item === "string")
      : [];

    const payload: Record<string, unknown> = {};

    if (hasRoleUpdate && newRole) {
      const nextRoles = [...new Set([...currentRoles, newRole])];
      payload.role = newRole;
      payload.activeRole = newRole;
      payload.roles = nextRoles.length > 0 ? nextRoles : [newRole];
    }

    if (fullName !== undefined) {
      payload.fullName = fullName;
    }

    if (phone !== undefined) {
      payload.phone = phone;
    }

    if (Object.keys(payload).length > 0) {
      await ref.set(payload, { merge: true });
    }

    if (fullName !== undefined) {
      try {
        await auth.updateUser(userId, { displayName: fullName || undefined });
      } catch {
        // Keep users-doc update as source of truth if auth update fails.
      }
    }

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
    const auth = getFirebaseAdminAuth();

    await Promise.all([
      db.collection("users").doc(userId).delete(),
      db.collection("worker_profiles").doc(userId).delete(),
      db.collection("employer_profiles").doc(userId).delete(),
      db.collection("referral_stats").doc(userId).delete()
    ]);

    const referralCodes = await db.collection("referral_codes").where("userId", "==", userId).get();
    if (!referralCodes.empty) {
      const batch = db.batch();
      referralCodes.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
    }

    try {
      await auth.deleteUser(userId);
    } catch (error) {
      const code = (error as { code?: string })?.code;
      if (code !== "auth/user-not-found") {
        throw error;
      }
    }

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to delete user.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
