"use client";

import { ProductRoleBoundary } from "@/components/product/product-shell";
import { WorkerJobDetailClient } from "@/components/product/worker-app";

export default function WorkerJobDetailPage({
  params
}: {
  params: { jobId: string };
}) {
  return (
    <ProductRoleBoundary
      currentPath="/app/worker/jobs"
      description="Read job details, save the listing, and apply with a live Firestore write."
      requiredRole="WORKER"
      title="Job detail"
    >
      {(session) => <WorkerJobDetailClient jobId={params.jobId} session={session} />}
    </ProductRoleBoundary>
  );
}
