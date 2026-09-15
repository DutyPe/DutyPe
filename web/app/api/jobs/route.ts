import { NextRequest, NextResponse } from "next/server";

import { getLiveJobPage } from "@/lib/jobs/server";
import { JobSearchError, parseJobSearch } from "@/lib/jobs/public-listings";

export const runtime = "nodejs";

async function respond(input: Record<string, unknown>) {
  try {
    const search = parseJobSearch(input);
    const cursor = input.cursor == null ? null : input.cursor;
    if (cursor !== null && typeof cursor !== "string") throw new JobSearchError("Invalid jobs page.");
    const result = await getLiveJobPage(search, cursor);
    return NextResponse.json(result, {
      status: result.status === "unavailable" ? 503 : 200,
      headers: { "Cache-Control": "no-store" }
    });
  } catch (error) {
    return NextResponse.json({ error: error instanceof JobSearchError ? error.message : "Unable to search jobs right now." }, {
      status: error instanceof JobSearchError ? 400 : 503, headers: { "Cache-Control": "no-store" }
    });
  }
}

export async function GET(request: NextRequest) {
  return respond(Object.fromEntries(request.nextUrl.searchParams));
}

export async function POST(request: NextRequest) {
  try {
    const text = await request.text();
    if (text.length > 4096) return NextResponse.json({ error: "Search request is too large." }, { status: 413 });
    const input = JSON.parse(text);
    if (!input || typeof input !== "object" || Array.isArray(input)) throw new Error("Invalid body");
    return respond(input);
  } catch {
    return NextResponse.json({ error: "Invalid job search." }, { status: 400 });
  }
}