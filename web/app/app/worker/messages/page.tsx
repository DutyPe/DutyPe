"use client";

import { WorkerMessagesClient } from "@/components/product/communication-app";
import { ProductRoleBoundary } from "@/components/product/product-shell";

export default function WorkerMessagesPage() {
  return (
    <ProductRoleBoundary
      currentPath="/app/worker/messages"
      description="Live DutyPe worker conversations with unread state, message history, and reply flow."
      requiredRole="WORKER"
      title="Worker messages"
    >
      {(session) => <WorkerMessagesClient session={session} />}
    </ProductRoleBoundary>
  );
}
