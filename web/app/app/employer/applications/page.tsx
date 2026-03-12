"use client";

import { ProductRoleBoundary } from "@/components/product/product-shell";
import { EmployerApplicationsClient } from "@/components/product/employer-app";

export default function EmployerApplicationsPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/applications"
      description="Review worker applications and push status changes back to Firestore."
      requiredRole="EMPLOYER"
      title="Applications"
    >
      {(session) => <EmployerApplicationsClient session={session} />}
    </ProductRoleBoundary>
  );
}
