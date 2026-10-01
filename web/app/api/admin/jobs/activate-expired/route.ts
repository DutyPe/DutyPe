import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { Jobs, Values } from "@/lib/firebase/schema";

export const runtime = "nodejs";

const BATCH = 400;

/** Re-opens up to 400 expired jobs for another 30 days (run again for more). */
export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;
  try {
    const db = getFirebaseAdminDb();
    const snap = await db.collection(Jobs.COLLECTION).where(Jobs.STATUS, "==", Values.JobStatus.EXPIRED).limit(BATCH).get();
    if (snap.empty) return NextResponse.json({ activatedCount: 0, message: "No expired jobs found." });
    const expiresAt = Timestamp.fromMillis(Date.now() + 30 * 24 * 60 * 60 * 1000);
    const batch = db.batch();
    snap.docs.forEach((doc) => batch.update(doc.ref, { [Jobs.STATUS]: Values.JobStatus.OPEN, [Jobs.EXPIRES_AT]: expiresAt }));
    await batch.commit();
    const more = snap.size === BATCH ? " Run again to re-open more." : "";
    return NextResponse.json({ activatedCount: snap.size, message: `Re-opened ${snap.size} expired jobs for 30 days.${more}` });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to activate expired jobs." }, { status: 500 });
  }
}
