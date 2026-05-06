import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import {
  getFirebaseAdminAuth,
  getFirebaseAdminDb
} from "@/lib/firebase/admin-server";
import {
  asRecord,
  buildPhoneRoleMap,
  buildProfileAdminRow,
  buildRecordMap,
  buildReferralCodeByUserId,
  listAuthUsersById
} from "@/lib/firebase/admin-profile-enrichment";

export const runtime = "nodejs";

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const auth = getFirebaseAdminAuth();
    const [
      snapshot,
      phoneRolesSnapshot,
      referralStatsSnapshot,
      referralCodesSnapshot,
      authUsersById
    ] = await Promise.all([
      db.collection("worker_profiles").limit(5000).get(),
      db.collection("phoneRoles").limit(5000).get(),
      db.collection("referral_stats").limit(5000).get(),
      db.collection("referral_codes").limit(5000).get(),
      listAuthUsersById(auth)
    ]);

    const phoneRolesByUid = buildPhoneRoleMap(phoneRolesSnapshot);
    const referralStatsByUid = buildRecordMap(referralStatsSnapshot);
    const referralCodeByUid = buildReferralCodeByUserId(referralCodesSnapshot);

    const profiles = snapshot.docs.map((doc) => buildProfileAdminRow({
      collectionName: "worker_profiles",
      expectedRole: "WORKER",
      userId: doc.id,
      profile: asRecord(doc.data()),
      phoneRole: phoneRolesByUid.get(doc.id),
      referralStats: referralStatsByUid.get(doc.id),
      referralCode: referralCodeByUid.get(doc.id),
      authUser: authUsersById.get(doc.id)
    }));

    return NextResponse.json({ profiles });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load worker profiles.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
