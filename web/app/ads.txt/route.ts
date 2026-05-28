import { NextResponse } from "next/server";

export const dynamic = "force-static";

const ADS_TXT_CONTENT = "google.com, pub-5503082977524600, DIRECT, f08c47fec0942fa0";

export function GET() {
  return new NextResponse(`${ADS_TXT_CONTENT}\n`, {
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "public, max-age=3600"
    }
  });
}
