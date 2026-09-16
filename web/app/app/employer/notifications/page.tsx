"use client";

import { EmployerNotificationsClient } from "@/components/product/communication-app";
import { ProductRoleBoundary } from "@/components/product/product-shell";

export default function EmployerNotificationsPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/employer/notifications"
      description="Employer notification inbox for new applications, chat alerts, and job state changes."
      requiredRole="EMPLOYER"
      title="Employer notifications"
    >
      {(session) => <EmployerNotificationsClient session={session} />}
    </ProductRoleBoundary>
  );
}
