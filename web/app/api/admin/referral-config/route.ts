import { NextRequest, NextResponse } from "next/server";
import { FieldValue } from "firebase-admin/firestore";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const data = await request.json();
    const patch: Record<string, unknown> = {};

    if (data?.rewardPerReferral !== undefined) patch.rewardPerReferral = Number(data.rewardPerReferral);
    if (data?.signupBonus !== undefined) patch.signupBonus = Number(data.signupBonus);
    if (data?.employerSignupBonus !== undefined) patch.employerSignupBonus = Number(data.employerSignupBonus);
    if (data?.employerSignupBonusEnabled !== undefined) patch.employerSignupBonusEnabled = Boolean(data.employerSignupBonusEnabled);
    if (data?.employerUnlimitedJobPostingEnabled !== undefined) patch.employerUnlimitedJobPostingEnabled = Boolean(data.employerUnlimitedJobPostingEnabled);
    if (data?.welcomeBonusCampaignId !== undefined) patch.welcomeBonusCampaignId = String(data.welcomeBonusCampaignId).trim();
    if (data?.minWithdrawal !== undefined) patch.minWithdrawal = Number(data.minWithdrawal);
    if (data?.maxWithdrawalPerDay !== undefined) patch.maxWithdrawalPerDay = Number(data.maxWithdrawalPerDay);
    if (data?.milestones && typeof data.milestones === "object") patch.milestones = data.milestones;
    if (Array.isArray(data?.withdrawalMilestones)) {
      patch.withdrawalMilestones = data.withdrawalMilestones.map(Number).filter(Number.isFinite);
    }

    if (Object.keys(patch).length === 0) {
      return NextResponse.json({ error: "No valid fields to update" }, { status: 400 });
    }

    const db = getFirebaseAdminDb();
    await db.doc("app_config/referral").set({
      ...patch,
      updatedAt: FieldValue.serverTimestamp(),
      updatedBy: "webapp-admin",
    }, { merge: true });

    return NextResponse.json({ success: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update referral config.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
