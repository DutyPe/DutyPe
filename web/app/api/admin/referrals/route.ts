import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();

    const [referralsSnapshot, withdrawalsSnapshot] = await Promise.all([
      db.collection("referrals").orderBy("createdAt", "desc").limit(500).get(),
      db.collection("withdrawal_requests").orderBy("createdAt", "desc").limit(200).get()
    ]);

    const referrals = referralsSnapshot.docs.map((item) => ({
      id: item.id,
      ...(asRecord(item.data()) as Record<string, unknown>)
    }));

    const withdrawals = withdrawalsSnapshot.docs.map((item) => ({
      id: item.id,
      ...(asRecord(item.data()) as Record<string, unknown>)
    }));

    return NextResponse.json({ referrals, withdrawals });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load referrals.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateWithdrawalBody = {
  withdrawalId?: string;
  status?: string;
};

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: UpdateWithdrawalBody;

  try {
    body = (await request.json()) as UpdateWithdrawalBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const withdrawalId = body.withdrawalId?.trim();
  const status = body.status?.trim().toUpperCase();

  if (!withdrawalId || (status !== "COMPLETED" && status !== "FAILED")) {
    return NextResponse.json(
      { error: "Invalid withdrawalId or status." },
      { status: 400 }
    );
  }

  try {
    const db = getFirebaseAdminDb();
    await db.collection("withdrawal_requests").doc(withdrawalId).set(
      {
        status,
        processedAt: new Date(),
        updatedAt: new Date()
      },
      { merge: true }
    );

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update withdrawal.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
