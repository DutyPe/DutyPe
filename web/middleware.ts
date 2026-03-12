import { NextResponse, type NextRequest } from "next/server";

const exactLegacyRedirects: Record<string, string> = {
  "/index.html": "/",
  "/jobs/index.html": "/jobs",
  "/worker/index.html": "/worker",
  "/privacy.html": "/privacy",
  "/terms.html": "/terms",
  "/refund.html": "/refund",
  "/safety.html": "/safety",
  "/contact.html": "/contact",
  "/faq.html": "/faq",
  "/refer.html": "/refer",
  "/admin/index.html": "/admin",
  "/admin/dashboard.html": "/admin",
  "/admin/login.html": "/admin/login",
  "/admin/users.html": "/admin/users",
  "/admin/jobs.html": "/admin/jobs",
  "/admin/applications.html": "/admin/applications",
  "/admin/referrals.html": "/admin/referrals",
  "/admin/announcements.html": "/admin/announcements",
  "/admin/post-job.html": "/admin/post-job",
  "/admin/check-and-create-code.html": "/admin/check-and-create-code",
  "/admin/create-test-referral.html": "/admin/create-test-referral",
  "/admin/test-referral.html": "/admin/test-referral"
};

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (!pathname.endsWith(".html")) {
    return NextResponse.next();
  }

  if (pathname === "/googlef0148bf44dd14fa3.html") {
    return NextResponse.next();
  }

  const redirectTarget = exactLegacyRedirects[pathname] ?? pathname.replace(/\.html$/, "");
  const url = request.nextUrl.clone();
  url.pathname = redirectTarget;
  url.search = request.nextUrl.search;

  return NextResponse.redirect(url, 308);
}
