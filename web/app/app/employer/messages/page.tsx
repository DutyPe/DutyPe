"use client";

import { EmployerMessagesClient } from "@/components/product/communication-app";
import { ProductRoleBoundary } from "@/components/product/product-shell";

export default function EmployerMessagesPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/messages"
      description="Live employer conversations with workers across the DutyPe hiring flow."
      requiredRole="EMPLOYER"
      title="Employer messages"
    >
      {(session) => <EmployerMessagesClient session={session} />}
    </ProductRoleBoundary>
  );
}
