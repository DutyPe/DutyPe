"use client";

import { ProductRoleBoundary } from "@/components/product/product-shell";
import { WorkerJobsClient } from "@/components/product/worker-app";

export default function WorkerJobsPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/worker/jobs"
      description="Browse, search, and save live DutyPe jobs from Firestore."
      requiredRole="WORKER"
      title="All jobs"
    >
      {(session) => <WorkerJobsClient session={session} />}
    </ProductRoleBoundary>
  );
}
