"use client";

import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

// ssr:false keeps three.js out of the server bundle and off the critical path —
// the hero copy is server-rendered and readable before this ever loads.
const Hero3DScene = dynamic(() => import("./hero-3d-scene"), {
  ssr: false,
  loading: () => null,
});

type NavigatorWithMemory = Navigator & {
  deviceMemory?: number;
  connection?: { saveData?: boolean; effectiveType?: string };
};

/** Budget Android is the core audience, so 3D is opt-in by capability. */
function canRender3D(): boolean {
  if (typeof window === "undefined") return false;
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return false;

  const nav = navigator as NavigatorWithMemory;
  if (nav.connection?.saveData) return false;
  if (nav.connection?.effectiveType && /2g/.test(nav.connection.effectiveType)) {
    return false;
  }
  if (typeof nav.deviceMemory === "number" && nav.deviceMemory < 4) return false;
  if (typeof nav.hardwareConcurrency === "number" && nav.hardwareConcurrency < 4) {
    return false;
  }

  try {
    const canvas = document.createElement("canvas");
    return Boolean(
      canvas.getContext("webgl2") ?? canvas.getContext("webgl")
    );
  } catch {
    return false;
  }
}

export function Hero3D() {
  const [enabled, setEnabled] = useState(false);

  useEffect(() => {
    if (!canRender3D()) return;
    // Wait for idle so the 3D bundle never competes with LCP.
    const idle =
      window.requestIdleCallback?.(() => setEnabled(true)) ??
      window.setTimeout(() => setEnabled(true), 900);

    return () => {
      if (typeof idle === "number") window.clearTimeout(idle);
      else window.cancelIdleCallback?.(idle as unknown as number);
    };
  }, []);

  return (
    <div className="hero3d-stage" aria-hidden="true">
      <div className="hero3d-fallback" />
      {enabled ? <Hero3DScene /> : null}
    </div>
  );
}
