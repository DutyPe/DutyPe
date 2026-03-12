"use client";

import { ProductRoleBoundary } from "@/components/product/product-shell";
import { WorkerDashboardClient } from "@/components/product/worker-app";

export default function WorkerHomePage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/worker"
      description="Worker dashboard, recommendations, and live application activity."
      requiredRole="WORKER"
      title="Worker home"
    >
      {(session) => <WorkerDashboardClient session={session} />}
    </ProductRoleBoundary>
  );
}
