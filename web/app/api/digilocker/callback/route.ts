import { NextRequest, NextResponse } from "next/server";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const dynamic = "force-dynamic";

/**
 * DigiLocker / NeGD API Setu OAuth Redirect Callback Handler
 *
 * Citizen signs in on DigiLocker and grants consent.
 * DigiLocker redirects here with ?code=AUTH_CODE&state=STATE_TOKEN
 */
export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;
  const code = searchParams.get("code");
  const state = searchParams.get("state");
  const error = searchParams.get("error");
  const errorDesc = searchParams.get("error_description");

  const appRedirectBase = "dutype://digilocker";

  if (error || !code || !state) {
    const errorMsg = encodeURIComponent(errorDesc || error || "DigiLocker consent was not granted");
    return NextResponse.redirect(`${appRedirectBase}/error?msg=${errorMsg}`);
  }

  try {
    const db = getFirebaseAdminDb();
    const sessionDoc = await db.collection("digilocker_sessions").doc(state).get();

    if (!sessionDoc.exists) {
      return NextResponse.redirect(`${appRedirectBase}/error?msg=Session%20expired%20or%20invalid`);
    }

    const sessionData = sessionDoc.data() || {};
    const uid = sessionData.uid;

    if (!uid) {
      return NextResponse.redirect(`${appRedirectBase}/error?msg=User%20identity%20not%20found`);
    }

    // Exchange auth code for token or update session status to AUTHORIZED
    await db.collection("digilocker_sessions").doc(state).set(
      {
        authCode: code,
        status: "AUTHORIZED",
        authorizedAt: new Date()
      },
      { merge: true }
    );

    // Deep link redirect back into the DutyPe Android app
    return NextResponse.redirect(`${appRedirectBase}/verified?state=${encodeURIComponent(state)}`);
  } catch (err: unknown) {
    const errorMsg = encodeURIComponent((err as Error)?.message || "Internal callback failure");
    return NextResponse.redirect(`${appRedirectBase}/error?msg=${errorMsg}`);
  }
}
