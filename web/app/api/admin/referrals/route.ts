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

    // Withdrawals live under `users/{uid}/withdrawals/{id}` (single source of
    // truth, written by `referral-system.ts`). The legacy top-level
    // `withdrawal_requests` collection is gone.
    const [referralsSnapshot, withdrawalsSnapshot] = await Promise.all([
      db.collection("referrals").orderBy("createdAt", "desc").limit(500).get(),
      db.collectionGroup("withdrawals").orderBy("createdAt", "desc").limit(200).get()
    ]);

    const referrals = referralsSnapshot.docs.map((item) => ({
      id: item.id,
      ...(asRecord(item.data()) as Record<string, unknown>)
    }));

    const withdrawals = withdrawalsSnapshot.docs.map((item) => {
      // Path: users/{userId}/withdrawals/{id}
      const userId = item.ref.parent.parent?.id ?? "";
      return {
        id: item.id,
        userId,
        ...(asRecord(item.data()) as Record<string, unknown>)
      };
    });

    return NextResponse.json({ referrals, withdrawals });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load referrals.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateWithdrawalBody = {
  withdrawalId?: string;
  userId?: string;
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
  const userId = body.userId?.trim();
  const status = body.status?.trim().toUpperCase();

  if (!withdrawalId || (status !== "COMPLETED" && status !== "FAILED")) {
    return NextResponse.json(
      { error: "Invalid withdrawalId or status." },
      { status: 400 }
    );
  }

  try {
    const db = getFirebaseAdminDb();

    // Find the withdrawal in users/{uid}/withdrawals — userId is preferred
    // for a direct hit; otherwise fall back to a collectionGroup lookup.
    let writeRef = userId
      ? db.collection("users").doc(userId).collection("withdrawals").doc(withdrawalId)
      : null;

    if (!writeRef) {
      const lookup = await db.collectionGroup("withdrawals")
        .where("id", "==", withdrawalId)
        .limit(1)
        .get();
      if (lookup.empty) {
        return NextResponse.json(
          { error: `Withdrawal ${withdrawalId} not found.` },
          { status: 404 }
        );
      }
      writeRef = lookup.docs[0].ref;
    }

    await writeRef.set(
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
