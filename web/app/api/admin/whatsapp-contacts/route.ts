import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import {
  getFirebaseAdminAuth,
  getFirebaseAdminDb
} from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

function firstNonEmptyString(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }
  return "";
}

function normalizePhone(value: unknown) {
  const raw = firstNonEmptyString(value);
  if (!raw) return "";
  if (/^\+[1-9]\d{6,14}$/.test(raw)) return raw;

  const digits = raw.replace(/\D/g, "");
  if (digits.length === 10) return `+91${digits}`;
  if (digits.length >= 7 && digits.length <= 15) return `+${digits}`;
  return raw;
}

export type ContactItem = {
  id: string;
  name: string;
  phone: string;
  rawPhone: string;
  role: "WORKER" | "EMPLOYER";
  companyName?: string;
  skills?: string[];
  category?: string;
  location?: string;
  joinedAt?: string;
  jobCount?: number;
};

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const auth = getFirebaseAdminAuth();

    const authUsersById = new Map<string, { phone: string; displayName: string; email: string }>();
    let pageToken: string | undefined;

    do {
      const page = await auth.listUsers(1000, pageToken);
      page.users.forEach((entry) => {
        authUsersById.set(entry.uid, {
          phone: entry.phoneNumber ?? "",
          displayName: entry.displayName ?? "",
          email: entry.email ?? ""
        });
      });
      pageToken = page.pageToken;
    } while (pageToken);

    const [
      usersSnapshot,
      phoneRolesSnapshot,
      workerProfilesSnapshot,
      employerProfilesSnapshot
    ] = await Promise.all([
      db.collection("users").limit(5000).get(),
      db.collection("phoneRoles").limit(5000).get(),
      db.collection("worker_profiles").limit(5000).get(),
      db.collection("employer_profiles").limit(5000).get()
    ]);

    const userDocsById = new Map<string, Record<string, unknown>>();
    usersSnapshot.forEach((doc) => userDocsById.set(doc.id, doc.data() as Record<string, unknown>));

    const phoneRoleDocsByUid = new Map<string, Record<string, unknown>>();
    phoneRolesSnapshot.forEach((doc) => {
      const data = doc.data() as Record<string, unknown>;
      const uid = typeof data.uid === "string" ? data.uid.trim() : doc.id;
      phoneRoleDocsByUid.set(uid, data);
    });

    const workerProfilesById = new Map<string, Record<string, unknown>>();
    workerProfilesSnapshot.forEach((doc) => workerProfilesById.set(doc.id, doc.data() as Record<string, unknown>));

    const employerProfilesById = new Map<string, Record<string, unknown>>();
    employerProfilesSnapshot.forEach((doc) => employerProfilesById.set(doc.id, doc.data() as Record<string, unknown>));

    const allIds = new Set<string>([
      ...userDocsById.keys(),
      ...authUsersById.keys(),
      ...phoneRoleDocsByUid.keys(),
      ...workerProfilesById.keys(),
      ...employerProfilesById.keys()
    ]);

    const workers: ContactItem[] = [];
    const employers: ContactItem[] = [];

    allIds.forEach((uid) => {
      const uDoc = userDocsById.get(uid) ?? {};
      const prDoc = phoneRoleDocsByUid.get(uid) ?? {};
      const wDoc = workerProfilesById.get(uid) ?? null;
      const eDoc = employerProfilesById.get(uid) ?? null;
      const authUser = authUsersById.get(uid) ?? null;

      const rawRole = firstNonEmptyString(
        prDoc.role,
        uDoc.role,
        uDoc.activeRole,
        wDoc?.role,
        eDoc?.role
      ).toUpperCase();

      const isEmployer = rawRole === "EMPLOYER" || Boolean(eDoc);
      const role: "WORKER" | "EMPLOYER" = isEmployer ? "EMPLOYER" : "WORKER";

      const rawPhone = firstNonEmptyString(
        uDoc.phone,
        uDoc.phoneNumber,
        prDoc.phoneNumber,
        prDoc.phone,
        wDoc?.phone,
        wDoc?.phoneNumber,
        eDoc?.phone,
        eDoc?.phoneNumber,
        authUser?.phone
      );

      const phone = normalizePhone(rawPhone);
      if (!phone) return; // Skip contacts without phone numbers

      const name = firstNonEmptyString(
        uDoc.fullName,
        uDoc.name,
        prDoc.name,
        prDoc.fullName,
        wDoc?.fullName,
        wDoc?.name,
        eDoc?.fullName,
        eDoc?.name,
        eDoc?.companyName,
        authUser?.displayName,
        "DutyPe User"
      );

      const companyName = firstNonEmptyString(
        eDoc?.companyName,
        uDoc.companyName,
        eDoc?.businessName
      );

      const category = firstNonEmptyString(
        wDoc?.primarySkill,
        wDoc?.category,
        wDoc?.workType,
        uDoc.category
      );

      const rawSkills = wDoc?.skills ?? uDoc.skills;
      const skills = Array.isArray(rawSkills)
        ? rawSkills.map((s) => String(s)).filter(Boolean)
        : category ? [category] : [];

      const location = firstNonEmptyString(
        wDoc?.location,
        wDoc?.city,
        eDoc?.city,
        eDoc?.location,
        uDoc.city,
        uDoc.location
      );

      const joinedAt = firstNonEmptyString(
        uDoc.createdAt,
        prDoc.createdAt,
        wDoc?.createdAt,
        eDoc?.createdAt
      );

      const contact: ContactItem = {
        id: uid,
        name,
        phone,
        rawPhone: phone.replace(/\D/g, ""),
        role,
        companyName: isEmployer ? (companyName || name) : undefined,
        category: !isEmployer ? (category || skills[0] || "General Helper") : undefined,
        skills,
        location: location || "India",
        joinedAt: joinedAt || new Date().toISOString()
      };

      if (isEmployer) {
        employers.push(contact);
      } else {
        workers.push(contact);
      }
    });

    // Deduplicate by normalized phone number per role
    const uniqueWorkers = Array.from(
      workers.reduce((map, item) => {
        if (!map.has(item.phone)) map.set(item.phone, item);
        return map;
      }, new Map<string, ContactItem>()).values()
    ).sort((a, b) => a.name.localeCompare(b.name));

    const uniqueEmployers = Array.from(
      employers.reduce((map, item) => {
        if (!map.has(item.phone)) map.set(item.phone, item);
        return map;
      }, new Map<string, ContactItem>()).values()
    ).sort((a, b) => a.name.localeCompare(b.name));

    return NextResponse.json({
      workers: uniqueWorkers,
      employers: uniqueEmployers,
      counts: {
        totalWorkers: uniqueWorkers.length,
        totalEmployers: uniqueEmployers.length,
        totalContacts: uniqueWorkers.length + uniqueEmployers.length
      }
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load broadcast contacts.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
