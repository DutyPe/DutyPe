"use client";

import { ProductRoleBoundary } from "@/components/product/product-shell";
import { WorkerProfileClient } from "@/components/product/worker-app";

export default function WorkerProfilePage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/worker/profile"
      description="Edit the worker profile fields used by profile completion and apply flows."
      requiredRole="WORKER"
      title="Worker profile"
    >
      {(session) => <WorkerProfileClient session={session} />}
    </ProductRoleBoundary>
  );
}
