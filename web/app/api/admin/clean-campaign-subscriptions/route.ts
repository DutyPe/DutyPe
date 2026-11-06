import { NextRequest, NextResponse } from "next/server";
import { Timestamp, FieldValue } from "firebase-admin/firestore";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { getAdminActor, logAdminAudit, requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { EmployerProfiles } from "@/lib/firebase/schema";

export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request, { sensitive: true });
  if (unauthorized) return unauthorized;

  const actor = await getAdminActor(request);
  const db = getFirebaseAdminDb();

  try {
    // 1. Reset global app_config/referral flag to false
    await db.collection("app_config").doc("referral").set(
      {
        employerUnlimitedJobPostingEnabled: false,
        updatedAt: Timestamp.now(),
        updatedBy: actor?.email || "admin",
      },
      { merge: true }
    );

    // 2. Query all employer_profiles where subscription.planId == "UNLIMITED_CAMPAIGN"
    const snap = await db
      .collection(EmployerProfiles.COLLECTION)
      .where("subscription.planId", "==", "UNLIMITED_CAMPAIGN")
      .get();

    let cleanedCount = 0;
    const cleanedIds: string[] = [];

    const docs = snap.docs;
    for (let i = 0; i < docs.length; i += 400) {
      const chunk = docs.slice(i, i + 400);
      const batch = db.batch();
      for (const doc of chunk) {
        batch.update(doc.ref, {
          "subscription.planId": "",
          "subscription.status": "NONE",
          "subscription.startAt": FieldValue.delete(),
          "subscription.credits.normal": 0,
          "subscription.credits.instant": 0,
          updatedAt: Timestamp.now(),
        });
        cleanedCount++;
        cleanedIds.push(doc.id);
      }
      await batch.commit();
    }

    if (actor) {
      void logAdminAudit(
        "ADMIN_CLEAN_CAMPAIGN_SUBSCRIPTIONS",
        actor,
        {
          cleanedCount,
          cleanedIds: cleanedIds.slice(0, 50),
        },
        request
      );
    }

    return NextResponse.json({
      ok: true,
      message: `Successfully cleaned up ${cleanedCount} employer profiles from UNLIMITED_CAMPAIGN to NONE.`,
      cleanedCount,
      cleanedIds,
    });
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    return NextResponse.json({ error: errorMsg }, { status: 500 });
  }
}
