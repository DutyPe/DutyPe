"use client";

import { ProductRoleBoundary } from "@/components/product/product-shell";
import { WorkerMyJobsClient } from "@/components/product/worker-app";

export default function WorkerMyJobsPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/worker/my-jobs"
      description="Track applications and saved jobs from the worker side of the product."
      requiredRole="WORKER"
      title="My jobs"
    >
      {(session) => <WorkerMyJobsClient session={session} />}
    </ProductRoleBoundary>
  );
}
