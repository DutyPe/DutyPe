import { NextRequest, NextResponse } from "next/server";

import { getFirebaseAdminAuth, isFirebaseAdminConfigured } from "@/lib/firebase/admin-server";
import { isRejectedIdToken } from "@/lib/firebase/server-auth-errors";
import { getJobRecord } from "@/lib/jobs/server";
import { jobAddress, publicJobSummary, validJobId } from "@/lib/jobs/public-listings";

export const runtime = "nodejs";

export async function GET(request: NextRequest, { params }: { params: { jobId: string } }) {
  const headers = { "Cache-Control": "private, no-store", Vary: "Authorization" };
  const authorization = request.headers.get("authorization") ?? "";
  if (!authorization.startsWith("Bearer ") || authorization.length > 8192 || authorization.length <= 7) {
    return NextResponse.json({ error: "Sign in to see full job details." }, { status: 401, headers });
  }
  if (!validJobId(params.jobId)) return NextResponse.json({ error: "Job not found." }, { status: 404, headers });
  if (!isFirebaseAdminConfigured()) return NextResponse.json({ error: "Job details are temporarily unavailable." }, { status: 503, headers });
  try {
    const account = await getFirebaseAdminAuth().verifyIdToken(authorization.slice(7), true);
    if (account.firebase?.sign_in_provider === "anonymous") {
      return NextResponse.json({ error: "Sign in to your account to see full details." }, { status: 401, headers });
    }
  } catch (error) {
    return isRejectedIdToken(error)
      ? NextResponse.json({ error: "We couldn't verify your session. Please sign in again." }, { status: 401, headers })
      : NextResponse.json({ error: "The account verification service is temporarily unavailable. Please try again." }, { status: 503, headers });
  }
  try {
    const record = await getJobRecord(params.jobId);
    const summary = record ? publicJobSummary(record) : null;
    if (!summary || !record) return NextResponse.json({ error: "This job is no longer available." }, { status: 410, headers });
    const text = (field: string) => typeof record.data[field] === "string" ? record.data[field] : "";
    return NextResponse.json({ job: {
      ...summary, description: text("description"), location: jobAddress(record.data), shiftTiming: text("shiftTiming"),
      vacancies: Number(record.data.vacancies) || null
    } }, { headers });
  } catch {
    return NextResponse.json({ error: "Job details are temporarily unavailable. Please try again." }, { status: 503, headers });
  }
}