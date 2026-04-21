"use client";

import { WorkerNotificationsClient } from "@/components/product/notifications-app";
import { ProductRoleBoundary } from "@/components/product/product-shell";

export default function WorkerNotificationsPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/worker/notifications"
      description="Worker notification inbox for job updates, chat alerts, and account activity."
      requiredRole="WORKER"
      title="Worker notifications"
    >
      {(session) => <WorkerNotificationsClient session={session} />}
    </ProductRoleBoundary>
  );
}
