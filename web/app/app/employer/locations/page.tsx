"use client";

import { EmployerLocationsClient } from "@/components/product/employer-location-app";
import { ProductRoleBoundary } from "@/components/product/product-shell";

export default function EmployerLocationsPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/locations"
      description="Manage reusable employer work locations and business-base defaults for job posting."
      requiredRole="EMPLOYER"
      title="Employer locations"
    >
      {(session) => <EmployerLocationsClient session={session} />}
    </ProductRoleBoundary>
  );
}
