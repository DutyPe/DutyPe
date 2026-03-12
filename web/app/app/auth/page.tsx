import { Suspense } from "react";

import { ProductAuthClient } from "@/components/product/product-auth-client";

export default function ProductAppAuthPage() {
  return (
    <Suspense fallback={<div className="empty-state">Loading product auth.</div>}>
      <ProductAuthClient />
    </Suspense>
  );
}
