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

function normalizePhoneForDoc(value: unknown) {
  const raw = firstNonEmptyString(value);
  if (!raw) return "";
  if (/^\+[1-9]\d{6,14}$/.test(raw)) return raw;

  const digits = raw.replace(/\D/g, "");
  if (digits.length === 10) return `+91${digits}`;
  if (digits.length >= 7 && digits.length <= 15) return `+${digits}`;
  return raw;
}

function inferState(data: {
  state?: unknown;
  city?: unknown;
  address?: unknown;
  locationText?: unknown;
  location?: unknown;
  lat?: unknown;
  lng?: unknown;
}): string {
  const rawState = String(data.state ?? "").trim().toLowerCase();
  if (rawState.includes("telangana") || rawState === "ts" || rawState === "tg") return "Telangana";
  if (rawState.includes("andhra") || rawState.includes("ap")) return "Andhra Pradesh";
  if (rawState.includes("karnataka") || rawState === "ka") return "Karnataka";
  if (rawState.includes("tamil") || rawState === "tn") return "Tamil Nadu";
  if (rawState.includes("maharashtra") || rawState === "mh") return "Maharashtra";

  const combinedText = [
    data.city,
    data.locationText,
    data.address,
    typeof data.location === "string" ? data.location : ""
  ].map(v => String(v ?? "").toLowerCase()).join(" ");

  const telanganaKeywords = [
    "hyderabad", "secunderabad", "cyberabad", "warangal", "nizamabad", "karimnagar",
    "khammam", "mahbubnagar", "nalgonda", "adilabad", "suryapet", "siddipet",
    "miryalaguda", "jagtial", "mancherial", "ramagundam", "kothagudem", "kamareddy",
    "medak", "sangareddy", "rangareddy", "hitec city", "madhapur", "gachibowli",
    "kukatpally", "dilsukhnagar", "ameerpet", "kondapur", "miyapur", "telangana", "ts", "tg"
  ];
  if (telanganaKeywords.some(kw => combinedText.includes(kw))) return "Telangana";

  const apKeywords = [
    "visakhapatnam", "vizag", "vijayawada", "guntur", "nellore", "kurnool",
    "rajahmundry", "kakinada", "tirupati", "anantapur", "kadapa", "vizianagaram",
    "eluru", "ongole", "nandyal", "machilipatnam", "adoni", "tenali", "proddatur",
    "chittoor", "hindupur", "bhimavaram", "amaravati", "srikakulam", "andhra pradesh", "andhra"
  ];
  if (apKeywords.some(kw => combinedText.includes(kw))) return "Andhra Pradesh";

  if (combinedText.includes("bengaluru") || combinedText.includes("bangalore") || combinedText.includes("mysore") || combinedText.includes("karnataka")) return "Karnataka";
  if (combinedText.includes("chennai") || combinedText.includes("coimbatore") || combinedText.includes("madurai") || combinedText.includes("tamil nadu")) return "Tamil Nadu";
  if (combinedText.includes("mumbai") || combinedText.includes("pune") || combinedText.includes("nagpur") || combinedText.includes("maharashtra")) return "Maharashtra";
  if (combinedText.includes("delhi") || combinedText.includes("noida") || combinedText.includes("gurugram") || combinedText.includes("gurgaon")) return "Delhi NCR";

  let lat = typeof data.lat === "number" ? data.lat : Number(data.lat);
  let lng = typeof data.lng === "number" ? data.lng : Number(data.lng);
  if ((!lat || !lng) && typeof data.location === "object" && data.location !== null) {
    lat = Number((data.location as any).lat);
    lng = Number((data.location as any).lng);
  }

  if (lat && lng && lat > 0 && lng > 0) {
    if (lat >= 15.8 && lat <= 19.9 && lng >= 77.2 && lng <= 81.8) {
      if (lng > 80.5 && lat < 18.0) return "Andhra Pradesh";
      return "Telangana";
    }
    if (lat >= 12.6 && lat <= 19.1 && lng >= 76.7 && lng <= 84.8) {
      return "Andhra Pradesh";
    }
  }

  return "Other / Unknown";
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
      ...phoneRoleDocsByUid.keys(),
      ...workerProfileById.keys(),
      ...employerProfileById.keys(),
      ...referralCodeByUserId.keys()
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
          rawDoc.name,
          phoneRole?.name,
          phoneRole?.fullName,
          workerProfile?.fullName,
          workerProfile?.name,
          employerProfile?.fullName,
          employerProfile?.name,
          employerProfile?.companyName,
          authUser?.displayName
        ),
        phone: firstNonEmptyString(
          rawDoc.phone,
          rawDoc.phoneNumber,
          phoneRole?.phoneNumber,
          phoneRole?.phone,
          workerProfile?.phone,
          workerProfile?.phoneNumber,
          employerProfile?.phone,
          employerProfile?.phoneNumber,
          authUser?.phone
        ),
        email: firstNonEmptyString(
          rawDoc.email,
          workerProfile?.email,
          employerProfile?.email,
          authUser?.email
        ),
        referralCode: firstNonEmptyString(rawDoc.referralCode, referralCodeByUserId.get(userId))
      };

      const normalized = normalizeUserRecord(userId, merged, {
        workerProfile,
        employerProfile,
        referralCodeByUserId: referralCodeByUserId.get(userId)
      });

      const state = inferState({
        state: rawDoc.state || workerProfile?.state || employerProfile?.state,
        city: rawDoc.city || rawDoc.companyCity || workerProfile?.city || employerProfile?.city || employerProfile?.companyCity,
        address: rawDoc.address || rawDoc.addressText || workerProfile?.address || workerProfile?.locationText || employerProfile?.companyAddress || employerProfile?.address,
        locationText: rawDoc.locationText || workerProfile?.locationText || employerProfile?.locationText,
        location: rawDoc.location || workerProfile?.location || employerProfile?.location,
        lat: rawDoc.lat || workerProfile?.lat || employerProfile?.lat,
        lng: rawDoc.lng || workerProfile?.lng || employerProfile?.lng
      });

      const city = firstNonEmptyString(
        rawDoc.city,
        rawDoc.companyCity,
        workerProfile?.city,
        employerProfile?.city,
        employerProfile?.companyCity
      );

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
        state,
        city,
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

    const stateCounts = users.reduce<Record<string, { workers: number; employers: number; admins: number; total: number }>>(
      (acc, user) => {
        const st = user.state || "Other / Unknown";
        if (!acc[st]) {
          acc[st] = { workers: 0, employers: 0, admins: 0, total: 0 };
        }
        acc[st].total += 1;
        if (user.canonicalRole === "WORKER" || user.role === "WORKER" || user.activeRole === "WORKER") {
          acc[st].workers += 1;
        } else if (user.canonicalRole === "EMPLOYER" || user.role === "EMPLOYER" || user.activeRole === "EMPLOYER") {
          acc[st].employers += 1;
        } else if (user.canonicalRole === "ADMIN") {
          acc[st].admins += 1;
        }
        return acc;
      },
      {}
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

    return NextResponse.json({ users, sourceCounts, roleCounts, stateCounts, integrityCounts });
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
  const phone = typeof body.phone === "string" ? normalizePhoneForDoc(body.phone) : undefined;
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
      payload.role = newRole;
      payload.activeRole = newRole;
    }

    if (fullName !== undefined) {
      payload.fullName = fullName;
    }

    if (phone !== undefined) {
      payload.phone = phone;
      payload.phoneNumber = phone;
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
      } else if (phone && newRole) {
        await db.collection("phoneRoles").doc(phone).set(
          {
            phoneNumber: phone,
            role: newRole,
            ...(fullName ? { name: fullName } : {}),
            uid: userId,
            createdAt: new Date(),
            updatedAt: new Date()
          },
          { merge: true }
        );
      }
    }

    const profilePayload: Record<string, unknown> = {};
    if (hasRoleUpdate && newRole) {
      profilePayload.role = newRole;
    }
    if (fullName !== undefined) {
      profilePayload.fullName = fullName;
    }
    if (phone !== undefined) {
      profilePayload.phone = phone;
    }

    if (Object.keys(profilePayload).length > 0) {
      const withProfileTimestamp = {
        ...profilePayload,
        userId,
        updatedAt: new Date()
      };

      if (hasRoleUpdate && newRole) {
        const profileCollection = newRole === "EMPLOYER" ? "employer_profiles" : "worker_profiles";
        await db.collection(profileCollection).doc(userId).set(
          withProfileTimestamp,
          { merge: true }
        );
      } else {
        const [workerProfile, employerProfile] = await Promise.all([
          db.collection("worker_profiles").doc(userId).get(),
          db.collection("employer_profiles").doc(userId).get()
        ]);
        const batch = db.batch();
        if (workerProfile.exists) {
          batch.set(workerProfile.ref, withProfileTimestamp, { merge: true });
        }
        if (employerProfile.exists) {
          batch.set(employerProfile.ref, withProfileTimestamp, { merge: true });
        }
        if (workerProfile.exists || employerProfile.exists) {
          await batch.commit();
        }
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
