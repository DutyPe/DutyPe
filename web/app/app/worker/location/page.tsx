"use client";

import { WorkerLocationClient } from "@/components/product/location-app";
import { ProductRoleBoundary } from "@/components/product/product-shell";

export default function WorkerLocationPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/worker/location"
      description="Save the worker location used for nearby job discovery, sorting, and directions."
      requiredRole="WORKER"
      title="Worker location"
    >
      {(session) => <WorkerLocationClient session={session} />}
    </ProductRoleBoundary>
  );
}
