"use client";

import { useEffect } from "react";

declare global {
  interface Window {
    adsbygoogle?: unknown[];
  }
}

const ADSENSE_CLIENT_ID =
  process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID ?? "ca-pub-5503082977524600";
const ADSENSE_SLOT_ID = process.env.NEXT_PUBLIC_ADSENSE_SLOT_ID;

export function AdSenseBanner() {
  useEffect(() => {
    if (!ADSENSE_SLOT_ID) {
      return;
    }

    try {
      (window.adsbygoogle = window.adsbygoogle || []).push({});
    } catch {
      // AdSense can throw during repeated soft navigations; ignore and let it retry on refresh.
    }
  }, []);

  if (!ADSENSE_SLOT_ID) {
    return null;
  }

  return (
    <aside className="adsense-banner" aria-label="Advertisement">
      <span className="adsense-label">Advertisement</span>
      <ins
        className="adsbygoogle"
        style={{ display: "block" }}
        data-ad-client={ADSENSE_CLIENT_ID}
        data-ad-slot={ADSENSE_SLOT_ID}
        data-ad-format="auto"
        data-full-width-responsive="true"
      />
    </aside>
  );
}