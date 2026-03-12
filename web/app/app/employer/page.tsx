"use client";

import { ProductRoleBoundary } from "@/components/product/product-shell";
import { EmployerDashboardClient } from "@/components/product/employer-app";

export default function EmployerHomePage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer"
      description="Employer dashboard, job posting overview, and hiring pipeline activity."
      requiredRole="EMPLOYER"
      title="Employer dashboard"
    >
      {(session) => <EmployerDashboardClient session={session} />}
    </ProductRoleBoundary>
  );
}
