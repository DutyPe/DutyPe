import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { deleteAccountCompletely, e164 } from "@/lib/firebase/admin-account-deletion";
import { EmployerProfiles, PhoneRoles, WorkerProfiles } from "@/lib/firebase/schema";

export const runtime = "nodejs";

const PAGE = 100;
type Role = "WORKER" | "EMPLOYER";

function collectionFor(role: Role) {
  return role === "EMPLOYER" ? EmployerProfiles.COLLECTION : WorkerProfiles.COLLECTION;
}

function toUser(id: string, role: Role, d: FirebaseFirestore.DocumentData) {
  const iso = (v: unknown) => (v instanceof Timestamp ? v.toDate().toISOString() : null);
  const isEmployer = role === "EMPLOYER";
  const business = String(d[EmployerProfiles.BUSINESS_NAME] ?? "");
  return {
    id,
    role,
    name: isEmployer ? String(d[EmployerProfiles.OWNER_NAME] ?? "") : String(d[WorkerProfiles.NAME] ?? ""),
    businessName: isEmployer ? business : "",
    employerType: isEmployer ? String(d[EmployerProfiles.EMPLOYER_TYPE] ?? "") : "",
    phone: String(d[WorkerProfiles.PHONE] ?? ""),
    area: String(d[WorkerProfiles.AREA] ?? ""),
    photoUrl: String(d[WorkerProfiles.PHOTO_URL] ?? ""),
    skills: isEmployer ? [] : ((d[WorkerProfiles.SKILLS] ?? []) as string[]),
    blocked: d[WorkerProfiles.BLOCKED] === true,
    verified: isEmployer ? d[EmployerProfiles.VERIFIED] === true : false,
    createdAt: iso(d[WorkerProfiles.CREATED_AT])
  };
}

/**
 * ?role=WORKER|EMPLOYER  newest profiles of that role (one page, ?after= for more)
 * ?phone=9876543210      the account registered for that number
 * Totals come from count aggregations, never from reading every profile.
 */
async function getUncached(request: NextRequest) {
  try {
    const db = getFirebaseAdminDb();
    const params = new URL(request.url).searchParams;
    const [workers, employers] = await Promise.all([
      db.collection(WorkerProfiles.COLLECTION).count().get(),
      db.collection(EmployerProfiles.COLLECTION).count().get()
    ]);
    const counts = { workers: workers.data().count, employers: employers.data().count };

    const phoneParam = params.get("phone");
    if (phoneParam) {
      const phone = e164(phoneParam);
      if (!phone) return NextResponse.json({ error: "Enter a valid 10-digit mobile number." }, { status: 400 });
      const reg = await db.collection(PhoneRoles.COLLECTION).doc(phone).get();
      const uid = String(reg.get(PhoneRoles.UID) ?? "");
      const role = (String(reg.get(PhoneRoles.ROLE) ?? "") === "EMPLOYER" ? "EMPLOYER" : "WORKER") as Role;
      const profile = uid ? await db.collection(collectionFor(role)).doc(uid).get() : null;
      const users = profile?.exists ? [toUser(profile.id, role, profile.data()!)] : [];
      return NextResponse.json({ users, counts, nextCursor: null });
    }

    const role = (params.get("role") === "EMPLOYER" ? "EMPLOYER" : "WORKER") as Role;
    const col = db.collection(collectionFor(role));
    let query = col.orderBy(WorkerProfiles.CREATED_AT, "desc");
    const after = params.get("after");
    if (after) {
      const cursor = await col.doc(after).get();
      if (cursor.exists) query = query.startAfter(cursor);
    }
    const snap = await query.limit(PAGE).get();
    return NextResponse.json({
      users: snap.docs.map((d) => toUser(d.id, role, d.data())),
      counts,
      nextCursor: snap.size === PAGE ? snap.docs.at(-1)!.id : null
    });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to load users." }, { status: 500 });
  }
}

/** { userId, role, blocked?, verified?, name? } — block/unblock, verify an employer, fix a name. */
export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;
  let body: Record<string, unknown>;
  try { body = await request.json(); } catch { return NextResponse.json({ error: "Invalid request body." }, { status: 400 }); }
  const userId = String(body.userId ?? "").trim();
  const role = (body.role === "EMPLOYER" ? "EMPLOYER" : "WORKER") as Role;
  if (!userId) return NextResponse.json({ error: "Missing userId." }, { status: 400 });

  const update: Record<string, unknown> = {};
  if (typeof body.blocked === "boolean") update[WorkerProfiles.BLOCKED] = body.blocked;
  if (typeof body.verified === "boolean" && role === "EMPLOYER") update[EmployerProfiles.VERIFIED] = body.verified;
  if (typeof body.name === "string" && body.name.trim()) {
    update[role === "EMPLOYER" ? EmployerProfiles.OWNER_NAME : WorkerProfiles.NAME] = body.name.trim().slice(0, 80);
  }
  if (!Object.keys(update).length) return NextResponse.json({ error: "Nothing to update." }, { status: 400 });
  update[WorkerProfiles.UPDATED_AT] = Timestamp.now();
  try {
    await getFirebaseAdminDb().collection(collectionFor(role)).doc(userId).update(update);
    return NextResponse.json({ ok: true });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to update user." }, { status: 500 });
  }
}

/** Deletes the account completely (profile, card, phone registration, jobs, applications…). */
export async function DELETE(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;
  let body: { userId?: string };
  try { body = await request.json(); } catch { return NextResponse.json({ error: "Invalid request body." }, { status: 400 }); }
  const userId = body.userId?.trim();
  if (!userId) return NextResponse.json({ error: "Missing userId." }, { status: 400 });
  try {
    const deletedCount = await deleteAccountCompletely(userId);
    return NextResponse.json({ ok: true, deletedCount });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to delete user." }, { status: 500 });
  }
}

export const GET = cachedAdminGet(getUncached);
