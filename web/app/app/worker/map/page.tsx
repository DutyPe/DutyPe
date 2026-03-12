"use client";

import { WorkerMapClient } from "@/components/product/map-app";
import { ProductRoleBoundary } from "@/components/product/product-shell";

export default function WorkerMapPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/worker/map"
      description="Hyperlocal nearby-jobs radar with live worker location, markers, and radius filters."
      requiredRole="WORKER"
      title="Nearby jobs map"
    >
      {(session) => <WorkerMapClient session={session} />}
    </ProductRoleBoundary>
  );
}
