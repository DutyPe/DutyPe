import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { normalizeUserRecord } from "@/lib/firebase/admin-normalizers";
import {
  getFirebaseAdminAuth,
  getFirebaseAdminDb
} from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

type AuthUserSummary = {
  uid: string;
  email: string;
  phone: string;
  displayName: string;
  disabled: boolean;
  createdAt: string;
  lastSignInAt: string;
};

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

function normalizeRole(value: unknown) {
  const role = firstNonEmptyString(value).toUpperCase();

  if (role === "WORKER" || role === "EMPLOYER" || role === "ADMIN") {
    return role;
  }

  return role ? "OTHER" : "";
}

function withId(id: string, value: Record<string, unknown> | null) {
  return value ? { id, ...value } : null;
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const auth = getFirebaseAdminAuth();

    const authUsersById = new Map<string, AuthUserSummary>();
    let pageToken: string | undefined;

    do {
      const page = await auth.listUsers(1000, pageToken);
      page.users.forEach((entry) => {
        authUsersById.set(entry.uid, {
          uid: entry.uid,
          email: entry.email ?? "",
          phone: entry.phoneNumber ?? "",
          displayName: entry.displayName ?? "",
          disabled: entry.disabled,
          createdAt: entry.metadata.creationTime ?? "",
          lastSignInAt: entry.metadata.lastSignInTime ?? ""
        });
      });
      pageToken = page.pageToken;
    } while (pageToken);

    const [
      usersSnapshot,
      phoneRolesSnapshot,
      referralCodesSnapshot,
      workerProfilesSnapshot,
      employerProfilesSnapshot
    ] =
      await Promise.all([
        db.collection("users").limit(5000).get(),
        db.collection("phoneRoles").limit(5000).get(),
        db.collection("referral_codes").limit(5000).get(),
        db.collection("worker_profiles").limit(5000).get(),
        db.collection("employer_profiles").limit(5000).get()
      ]);

    const userDocsById = new Map<string, Record<string, unknown>>();
    usersSnapshot.forEach((item) => {
      userDocsById.set(item.id, asRecord(item.data()));
    });

    const phoneRoleDocsByUid = new Map<string, Array<Record<string, unknown> & { docId: string }>>();
    let phoneRolesMissingUid = 0;
    phoneRolesSnapshot.forEach((item) => {
      const raw = asRecord(item.data());
      const uid = typeof raw.uid === "string" ? raw.uid.trim() : "";
      if (uid) {
        const existing = phoneRoleDocsByUid.get(uid) ?? [];
        existing.push({ ...raw, docId: item.id });
        phoneRoleDocsByUid.set(uid, existing);
      } else {
        phoneRolesMissingUid += 1;
      }
    });

    const referralCodeByUserId = new Map<string, string>();
    const referralCodeDocsByUserId = new Map<string, Array<Record<string, unknown>>>();
    referralCodesSnapshot.forEach((item) => {
      const raw = asRecord(item.data());
      const userId = typeof raw.userId === "string" ? raw.userId : "";
      const code = typeof raw.code === "string" && raw.code.trim() ? raw.code.trim() : item.id;

      if (userId && code) {
        referralCodeByUserId.set(userId, code);
        const existing = referralCodeDocsByUserId.get(userId) ?? [];
        existing.push({ id: item.id, ...raw });
        referralCodeDocsByUserId.set(userId, existing);
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

    const userIds = new Set<string>([
      ...userDocsById.keys(),
      ...authUsersById.keys(),
      ...phoneRoleDocsByUid.keys()
    ]);

    const users = Array.from(userIds).map((userId) => {
      const rawDoc = userDocsById.get(userId) ?? {};
      const phoneRoleDocs = phoneRoleDocsByUid.get(userId) ?? [];
      const phoneRole = phoneRoleDocs[0] ?? null;
      const authUser = authUsersById.get(userId);
      const workerProfile = workerProfileById.get(userId) ?? null;
      const employerProfile = employerProfileById.get(userId) ?? null;
      const sourceRoles = {
        phoneRoles: normalizeRole(phoneRole?.role),
        users: normalizeRole(rawDoc.role) || normalizeRole(rawDoc.activeRole),
        worker_profiles: normalizeRole(workerProfile?.role),
        employer_profiles: normalizeRole(employerProfile?.role)
      };
      const canonicalRole = sourceRoles.phoneRoles || sourceRoles.users ||
        sourceRoles.worker_profiles || sourceRoles.employer_profiles || "";
      const roleMismatch = new Set(Object.values(sourceRoles).filter(Boolean)).size > 1;
      const roleSource = sourceRoles.phoneRoles
        ? "phoneRoles"
        : sourceRoles.users
          ? "users"
          : sourceRoles.worker_profiles
            ? "worker_profiles"
            : sourceRoles.employer_profiles
              ? "employer_profiles"
              : "missing";

      const merged = {
        ...rawDoc,
        role: canonicalRole || rawDoc.role,
        activeRole: canonicalRole || rawDoc.activeRole,
        fullName: firstNonEmptyString(
          rawDoc.fullName,
          phoneRole?.name,
          phoneRole?.fullName,
          authUser?.displayName
        ),
        phone: firstNonEmptyString(
          rawDoc.phone,
          phoneRole?.phoneNumber,
          phoneRole?.phone,
          authUser?.phone
        ),
        email: firstNonEmptyString(rawDoc.email, authUser?.email)
      };

      const normalized = normalizeUserRecord(userId, merged, {
        workerProfile,
        employerProfile,
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
        roleSource,
        phoneRoleDocId: phoneRole?.docId ?? "",
        phoneRoleUid: phoneRole?.uid ?? "",
        phoneRoleRole: phoneRole?.role ?? "",
        phoneRoleName: phoneRole?.name ?? "",
        phoneRolePhoneNumber: phoneRole?.phoneNumber ?? "",
        phoneRoleCreatedAt: phoneRole?.createdAt ?? null,
        phoneRoleUpdatedAt: phoneRole?.updatedAt ?? null,
        workerProfileRole: workerProfile?.role ?? "",
        employerProfileRole: employerProfile?.role ?? "",
        canonicalRole: canonicalRole || "MISSING",
        sourceRoles,
        roleMismatch,
        phoneRoleDuplicateCount: phoneRoleDocs.length,
        referralCode: normalized.referralCode,
        createdAt: normalized.joinedAt,
        isBanned: rawDoc.isBanned === true,
        isVerified: rawDoc.isVerified === true,
        hasUserDoc: userDocsById.has(userId),
        hasPhoneRole: Boolean(phoneRole),
        hasAuthUser: authUsersById.has(userId),
        hasWorkerProfile: Boolean(workerProfile),
        hasEmployerProfile: Boolean(employerProfile),
        hasReferralCodeDoc: (referralCodeDocsByUserId.get(userId) ?? []).length > 0,
        firebaseFields: {
          phoneRoles: phoneRoleDocs,
          users: withId(userId, userDocsById.get(userId) ?? null),
          auth: authUser ?? null,
          worker_profiles: withId(userId, workerProfile),
          employer_profiles: withId(userId, employerProfile),
          referral_codes: referralCodeDocsByUserId.get(userId) ?? []
        }
      };
    }).sort((a, b) => String(b.createdAt ?? "").localeCompare(String(a.createdAt ?? "")));

    const roleCounts = users.reduce(
      (acc, user) => {
        const role = user.canonicalRole;
        if (role === "WORKER") acc.workers += 1;
        else if (role === "EMPLOYER") acc.employers += 1;
        else if (role === "ADMIN") acc.admins += 1;
        else if (role === "MISSING") acc.missing += 1;
        else acc.other += 1;
        return acc;
      },
      { workers: 0, employers: 0, admins: 0, missing: 0, other: 0 }
    );

    const sourceCounts = {
      identities: users.length,
      users: usersSnapshot.size,
      phoneRoles: phoneRolesSnapshot.size,
      authUsers: authUsersById.size,
      workerProfiles: workerProfilesSnapshot.size,
      employerProfiles: employerProfilesSnapshot.size,
      referralCodes: referralCodesSnapshot.size
    };

    const integrityCounts = {
      missingPhoneRole: users.filter((user) => !user.hasPhoneRole).length,
      missingAuthUser: users.filter((user) => !user.hasAuthUser).length,
      roleMismatch: users.filter((user) => user.roleMismatch).length,
      duplicatePhoneRoleUsers: Array.from(phoneRoleDocsByUid.values()).filter((docs) => docs.length > 1).length,
      phoneRolesMissingUid
    };

    return NextResponse.json({ users, sourceCounts, roleCounts, integrityCounts });
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
  isBanned?: boolean;
  banReason?: string;
  isVerified?: boolean;
};

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
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
  const isBanned = typeof body.isBanned === "boolean" ? body.isBanned : undefined;
  const isVerified = typeof body.isVerified === "boolean" ? body.isVerified : undefined;
  const banReason = typeof body.banReason === "string" ? body.banReason.trim() : undefined;

  if (!userId) {
    return NextResponse.json({ error: "Invalid userId." }, { status: 400 });
  }

  const hasRoleUpdate = Boolean(newRole);
  if (hasRoleUpdate && newRole !== "WORKER" && newRole !== "EMPLOYER") {
    return NextResponse.json({ error: "Invalid role." }, { status: 400 });
  }

  if (
    !hasRoleUpdate &&
    fullName === undefined &&
    phone === undefined &&
    isBanned === undefined &&
    isVerified === undefined
  ) {
    return NextResponse.json({ error: "No updatable fields were provided." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    const auth = getFirebaseAdminAuth();
    const ref = db.collection("users").doc(userId);

    const payload: Record<string, unknown> = {};

    if (hasRoleUpdate && newRole) {
      // Single-role schema: the live `users` collection only stores `role`
      // (and `activeRole` for compat). No `roles[]` array.
      payload.role = newRole;
      payload.activeRole = newRole;
    }

    if (fullName !== undefined) {
      payload.fullName = fullName;
    }

    if (phone !== undefined) {
      payload.phone = phone;
    }

    if (isBanned !== undefined) {
      payload.isBanned = isBanned;
      payload.bannedAt = isBanned ? new Date() : null;
      if (isBanned && banReason) payload.banReason = banReason;
      if (!isBanned) payload.banReason = null;
    }

    if (isVerified !== undefined) {
      payload.isVerified = isVerified;
      payload.verifiedAt = isVerified ? new Date() : null;
    }

    if (Object.keys(payload).length > 0) {
      await ref.set(payload, { merge: true });
    }

    const phoneRolePayload: Record<string, unknown> = {};
    if (hasRoleUpdate && newRole) {
      phoneRolePayload.role = newRole;
    }
    if (fullName !== undefined) {
      phoneRolePayload.name = fullName;
    }
    if (phone !== undefined) {
      phoneRolePayload.phoneNumber = phone;
    }

    if (Object.keys(phoneRolePayload).length > 0) {
      const phoneRoleMatches = await db.collection("phoneRoles")
        .where("uid", "==", userId)
        .limit(5)
        .get();
      const withTimestamp = { ...phoneRolePayload, uid: userId, updatedAt: new Date() };

      if (!phoneRoleMatches.empty) {
        const batch = db.batch();
        phoneRoleMatches.docs.forEach((doc) => {
          if (phone && doc.id !== phone) {
            batch.set(
              db.collection("phoneRoles").doc(phone),
              { ...doc.data(), ...withTimestamp, phoneNumber: phone },
              { merge: true }
            );
            batch.delete(doc.ref);
          } else {
            batch.set(doc.ref, withTimestamp, { merge: true });
          }
        });
        await batch.commit();
      } else if (phone && newRole && fullName) {
        await db.collection("phoneRoles").doc(phone).set(
          {
            phoneNumber: phone,
            role: newRole,
            name: fullName,
            uid: userId,
            createdAt: new Date(),
            updatedAt: new Date()
          },
          { merge: true }
        );
      }
    }

    if (isBanned !== undefined) {
      try {
        await auth.updateUser(userId, { disabled: isBanned });
      } catch {
        // Best-effort: doc update is the source of truth.
      }
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
  const unauthorized = await requireAuthorizedAdminRequest(request);
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
