"use client";

import { ProductRoleBoundary } from "@/components/product/product-shell";
import { EmployerPostJobClient } from "@/components/product/employer-app";

export default function EmployerPostJobPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/post-job"
      description="Create a live job post with clear pay, location, and role details."
      requiredRole="EMPLOYER"
      title="Post a job"
    >
      {(session) => <EmployerPostJobClient session={session} />}
    </ProductRoleBoundary>
  );
}
