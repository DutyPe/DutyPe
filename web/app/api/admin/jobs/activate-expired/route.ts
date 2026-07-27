import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  try {
    const db = getFirebaseAdminDb();
    
    // Find expired jobs in jobmetadata
    const snapshot = await db
      .collection("jobmetadata")
      .where("status", "==", "expired")
      .get();

    if (snapshot.empty) {
      return NextResponse.json({ activatedCount: 0, message: "No expired jobs found." });
    }

    const batch = db.batch();
    let batched = 0;
    
    // Set new expiry to 30 days from now
    const newExpiry = Timestamp.fromMillis(Date.now() + 30 * 24 * 60 * 60 * 1000);

    snapshot.docs.forEach((doc) => {
      // 1. Update status to 'open' in jobmetadata
      batch.update(doc.ref, { status: "open" });
      
      // 2. Update expiresAt in job_details
      const detailsRef = db.collection("job_details").doc(doc.id);
      // Use set with merge in case details doc is missing for some reason
      batch.set(detailsRef, { expiresAt: newExpiry }, { merge: true });
      
      batched++;
    });

    if (batched > 0) {
      await batch.commit();
    }

    return NextResponse.json({ 
      activatedCount: batched, 
      message: `Successfully reactivated ${batched} expired jobs for 30 days.` 
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to activate expired jobs.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
