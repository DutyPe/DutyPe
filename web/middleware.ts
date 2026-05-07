import { NextResponse, type NextRequest } from "next/server";

const exactLegacyRedirects: Record<string, string> = {
  "/legal/privacy": "/privacy",
  "/legal/terms": "/terms",
  "/legal/refund": "/refund",
  "/legal/safety": "/safety",
  "/legal/contact": "/contact",
  "/legal/account-deletion": "/accountdeletion",
  "/account-deletion": "/accountdeletion",
  "/delete-account": "/accountdeletion",
  "/index.html": "/",
  "/jobs/index.html": "/jobs",
  "/worker/index.html": "/worker",
  "/employer/index.html": "/employer",
  "/privacy.html": "/privacy",
  "/terms.html": "/terms",
  "/refund.html": "/refund",
  "/safety.html": "/safety",
  "/contact.html": "/contact",
  "/faq.html": "/faq",
  "/refer.html": "/refer",
  "/worker.html": "/worker",
  "/employer.html": "/employer",
  "/account-deletion.html": "/accountdeletion",
  "/accountdeletion.html": "/accountdeletion",
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

const appRouteAliases: Record<string, string> = {
  "/admin/dashboard": "/admin",
  "/admin/workers": "/admin/worker-profiles",
  "/admin/worker-profiles.html": "/admin/worker-profiles",
  "/admin/employers": "/admin/employer-profiles",
  "/admin/employer-profiles.html": "/admin/employer-profiles",
  "/admin/profiles": "/admin/users",
  "/admin/job-posts": "/admin/jobs",
  "/admin/referral-withdrawals": "/admin/referral-stats",
  "/post-job": "/app/employer/post-job",
  "/applications": "/app/worker/my-jobs",
  "/application": "/app/worker/my-jobs",
  "/job": "/jobs",
  "/help": "/contact",
  "/support": "/contact",
  "/contact-us": "/contact",
  "/worker/home": "/app/worker",
  "/worker/jobs": "/app/worker/jobs",
  "/worker/applications": "/app/worker/my-jobs",
  "/worker/my-jobs": "/app/worker/my-jobs",
  "/worker/map": "/app/worker/map",
  "/worker/location": "/app/worker/location",
  "/worker/profile": "/app/worker/profile",
  "/worker/notifications": "/app/worker/notifications",
  "/worker/refer": "/refer",
  "/worker/referrals": "/refer",
  "/app/worker/home": "/app/worker",
  "/app/worker/applications": "/app/worker/my-jobs",
  "/app/worker/refer": "/refer",
  "/app/worker/referrals": "/refer",
  "/employer/home": "/app/employer",
  "/employer/dashboard": "/app/employer",
  "/employer/jobs": "/app/employer/jobs",
  "/employer/post-job": "/app/employer/post-job",
  "/employer/applications": "/app/employer/applications",
  "/employer/locations": "/app/employer/locations",
  "/employer/notifications": "/app/employer/notifications",
  "/employer/profile": "/app/employer",
  "/employer/company": "/app/employer",
  "/employer/company-details": "/app/employer",
  "/employer/verification": "/app/employer",
  "/employer/refer": "/refer",
  "/employer/referrals": "/refer",
  "/app/employer/home": "/app/employer",
  "/app/employer/dashboard": "/app/employer",
  "/app/employer/profile": "/app/employer",
  "/app/employer/company": "/app/employer",
  "/app/employer/company-details": "/app/employer",
  "/app/employer/verification": "/app/employer",
  "/app/employer/refer": "/refer",
  "/app/employer/referrals": "/refer"
};

function redirectTo(request: NextRequest, destination: string) {
  const url = request.nextUrl.clone();
  const [pathname, search] = destination.split("?");

  url.pathname = pathname;
  url.search = search === undefined ? request.nextUrl.search : search;

  return NextResponse.redirect(url, 308);
}

function getDynamicAlias(pathname: string) {
  const jobMatch = pathname.match(/^\/job\/([^/]+)$/);
  if (jobMatch) {
    return `/jobs/${jobMatch[1]}`;
  }

  const jobRenewMatch = pathname.match(/^\/jobs\/([^/]+)\/renew$/);
  if (jobRenewMatch) {
    return `/jobs/${jobRenewMatch[1]}`;
  }

  const workerJobMatch = pathname.match(/^\/worker\/jobs\/([^/]+)$/);
  if (workerJobMatch) {
    return `/app/worker/jobs/${workerJobMatch[1]}`;
  }

  const workerApplicationMatch = pathname.match(/^\/(?:app\/)?worker\/applications\/([^/]+)$/);
  if (workerApplicationMatch) {
    return `/application/${workerApplicationMatch[1]}`;
  }

  const employerJobApplicationsMatch = pathname.match(/^\/employer\/jobs\/([^/]+)\/applications$/);
  if (employerJobApplicationsMatch) {
    return `/app/employer/jobs/${employerJobApplicationsMatch[1]}/applications`;
  }

  const employerJobMatch = pathname.match(/^\/employer\/jobs\/([^/]+)$/);
  if (employerJobMatch) {
    return `/app/employer/jobs/${employerJobMatch[1]}`;
  }

  const employerApplicationMatch = pathname.match(/^\/employer\/applications\/([^/]+)$/);
  if (employerApplicationMatch) {
    return `/app/employer/applications/${employerApplicationMatch[1]}`;
  }

  const employerUrgentMatch = pathname.match(/^\/employer\/urgent\/([^/]+)$/);
  if (employerUrgentMatch) {
    return "/app/employer";
  }

  return null;
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const lookupPathname = pathname.length > 1 ? pathname.replace(/\/+$/, "") : pathname;

  const exactRedirect = exactLegacyRedirects[lookupPathname];
  if (exactRedirect) {
    return redirectTo(request, exactRedirect);
  }

  const appRouteAlias = appRouteAliases[lookupPathname];
  if (appRouteAlias) {
    return redirectTo(request, appRouteAlias);
  }

  const dynamicAlias = getDynamicAlias(lookupPathname);
  if (dynamicAlias) {
    return redirectTo(request, dynamicAlias);
  }

  if (!pathname.endsWith(".html")) {
    return NextResponse.next();
  }

  if (pathname === "/googlef0148bf44dd14fa3.html") {
    return NextResponse.next();
  }

  const redirectTarget = pathname.replace(/\.html$/, "");
  const url = request.nextUrl.clone();
  url.pathname = redirectTarget;
  url.search = request.nextUrl.search;

  return NextResponse.redirect(url, 308);
}
