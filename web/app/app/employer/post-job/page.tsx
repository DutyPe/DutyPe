"use client";

import { useProductSession } from "@/components/product/use-product-session";
import { EmployerPostJobClient } from "@/components/product/employer-app";
import { SiteShell } from "@/components/site-shell";

export default function EmployerPostJobPage() {
  const session = useProductSession();
  return (
    <SiteShell>
      <header className="directory-page-header"><h1>Post a job on DutyPe</h1></header>
      <EmployerPostJobClient key={session.user?.uid ?? "guest"} session={session} />
    </SiteShell>
  );
}
