"use client";

import { EmployerWorkerProfileClient } from "@/components/product/employer-review";
import { ProductRoleBoundary } from "@/components/product/product-shell";

type EmployerWorkerProfilePageProps = {
  params: {
    workerId: string;
  };
  searchParams?: {
    applicationId?: string;
  };
};

export default function EmployerWorkerProfilePage({
  params,
  searchParams
}: EmployerWorkerProfilePageProps) {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/applications"
      description="Inspect the worker profile from the employer side with application-aware context and chat actions."
      requiredRole="EMPLOYER"
      title="Worker profile review"
    >
      {(session) => (
        <EmployerWorkerProfileClient
          applicationId={searchParams?.applicationId ?? null}
          session={session}
          workerId={params.workerId}
        />
      )}
    </ProductRoleBoundary>
  );
}
