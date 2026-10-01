import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { deleteAccountCompletely, findUserIdsForPhone } from "@/lib/firebase/admin-account-deletion";

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
    // Finds the account through phoneRoles, the Auth phone login, users and both profiles,
    // so it also works when the phoneRoles doc is missing (the case that used to fail).
    const { userIds, variants } = await findUserIdsForPhone(phone);
    if (userIds.length === 0) {
      return NextResponse.json({
        error: `No account found for phone: ${phone}`,
        deletedCount: 0
      }, { status: 404 });
    }

    let deletedCount = 0;
    for (const userId of userIds) {
      deletedCount += await deleteAccountCompletely(userId, variants);
    }

    return NextResponse.json({
      ok: true,
      message: `Deleted ${userIds.length} account(s) and all linked data for ${variants[0] || phone}`,
      userId: userIds.join(", "),
      phone: variants[0] || phone,
      deletedCount
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to delete user by phone.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
