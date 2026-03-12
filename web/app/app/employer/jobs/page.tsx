"use client";

import { ProductRoleBoundary } from "@/components/product/product-shell";
import { EmployerJobsClient } from "@/components/product/employer-app";

export default function EmployerJobsPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/jobs"
      description="Pause, reopen, and review the jobs owned by the signed-in employer."
      requiredRole="EMPLOYER"
      title="My jobs"
    >
      {(session) => <EmployerJobsClient session={session} />}
    </ProductRoleBoundary>
  );
}
