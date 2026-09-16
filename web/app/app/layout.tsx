import type { ReactNode } from "react";

import { ProductSessionProvider } from "@/components/product/use-product-session";

export default function ProductLayout({ children }: { children: ReactNode }) {
  return <ProductSessionProvider>{children}</ProductSessionProvider>;
}