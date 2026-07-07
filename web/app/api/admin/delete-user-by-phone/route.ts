import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import {
  getFirebaseAdminAuth,
  getFirebaseAdminDb
} from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

type DeleteByPhoneBody = {
  phone?: string;
};

/**
 * Delete all data for a user by phone number.
 * Useful when user mistakenly joined as wrong role.
 * POST /api/admin/delete-user-by-phone
 * Body: { phone: "+919xxxxxxxxx" }
 */
export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: DeleteByPhoneBody;

  try {
    body = (await request.json()) as DeleteByPhoneBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const phone = body.phone?.trim();

  if (!phone) {
    return NextResponse.json({ error: "Phone number is required." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    const auth = getFirebaseAdminAuth();

    // Step 1: Look up userId from phoneRoles collection using phone as doc ID
    const phoneRoleRef = db.collection("phoneRoles").doc(phone);
    const phoneRoleSnap = await phoneRoleRef.get();

    if (!phoneRoleSnap.exists) {
      return NextResponse.json({
        error: `No phoneRole found for phone: ${phone}`,
        deletedCount: 0
      }, { status: 404 });
    }

    const phoneRoleData = phoneRoleSnap.data() as Record<string, unknown> | undefined;
    const userId = typeof phoneRoleData?.uid === "string" ? phoneRoleData.uid : null;

    if (!userId) {
      return NextResponse.json({
        error: `PhoneRole document missing uid for phone: ${phone}`,
        deletedCount: 0
      }, { status: 400 });
    }

    // Step 2: Delete all user data using the found userId
    let deletedCount = 0;

    // Delete core user docs
    await Promise.all([
      db.collection("users").doc(userId).delete().then(() => { deletedCount++; }),
      db.collection("worker_profiles").doc(userId).delete().then(() => { deletedCount++; }),
      db.collection("employer_profiles").doc(userId).delete().then(() => { deletedCount++; }),
      db.collection("referral_stats").doc(userId).delete().then(() => { deletedCount++; }),
      db.collection("phoneRoles").doc(phone).delete().then(() => { deletedCount++; })
    ]);

    // Delete referral codes owned by this user
    const referralCodes = await db.collection("referral_codes").where("userId", "==", userId).get();
    if (!referralCodes.empty) {
      const batch = db.batch();
      referralCodes.docs.forEach((doc) => {
        batch.delete(doc.ref);
        deletedCount++;
      });
      await batch.commit();
    }

    // Delete jobs created by this user (if employer)
    const jobs = await db.collection("jobs").where("employerId", "==", userId).get();
    if (!jobs.empty) {
      const batch = db.batch();
      jobs.docs.forEach((doc) => {
        batch.delete(doc.ref);
        deletedCount++;
      });
      await batch.commit();
    }

    // Delete applications made by this user (if worker)
    const workerApps = await db.collection("jobApplications").where("workerId", "==", userId).get();
    if (!workerApps.empty) {
      const batch = db.batch();
      workerApps.docs.forEach((doc) => {
        batch.delete(doc.ref);
        deletedCount++;
      });
      await batch.commit();
    }

    // Delete applications received by this user (if employer)
    const employerApps = await db.collection("jobApplications").where("employerId", "==", userId).get();
    if (!employerApps.empty) {
      const batch = db.batch();
      employerApps.docs.forEach((doc) => {
        batch.delete(doc.ref);
        deletedCount++;
      });
      await batch.commit();
    }

    // Delete saved jobs made by this user
    const savedJobs = await db.collection("saved_jobs").where("workerId", "==", userId).get();
    if (!savedJobs.empty) {
      const batch = db.batch();
      savedJobs.docs.forEach((doc) => {
        batch.delete(doc.ref);
        deletedCount++;
      });
      await batch.commit();
    }

    // Delete auth user
    try {
      await auth.deleteUser(userId);
      deletedCount++;
    } catch (error) {
      const code = (error as { code?: string })?.code;
      if (code !== "auth/user-not-found") {
        throw error;
      }
    }

    return NextResponse.json({
      ok: true,
      message: `Successfully deleted all data for user with phone: ${phone}`,
      userId,
      phone,
      deletedCount
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to delete user by phone.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
