import { NextResponse } from "next/server";

export const dynamic = "force-static";

const GOOGLE_VERIFICATION_CONTENT =
  "google-site-verification: googlef0148bf44dd14fa3.html";

export function GET() {
  return new NextResponse(`${GOOGLE_VERIFICATION_CONTENT}\n`, {
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "public, max-age=3600"
    }
  });
}
