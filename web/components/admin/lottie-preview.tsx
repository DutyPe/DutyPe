"use client";

import dynamic from "next/dynamic";
import { useEffect, useState } from "react";

// Load Lottie dynamically as it requires window/client-side context
const Lottie = dynamic(() => import("lottie-react"), { ssr: false });

export function LottiePreview({ url }: { url: string }) {
  const [animationData, setAnimationData] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!url) {
      setAnimationData(null);
      setError(null);
      return;
    }

    let isMounted = true;
    setError(null);

    fetch(url)
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch Lottie JSON");
        return res.json();
      })
      .then((data) => {
        if (isMounted) setAnimationData(data);
      })
      .catch((err) => {
        if (isMounted) setError("Invalid Lottie JSON URL");
      });

    return () => {
      isMounted = false;
    };
  }, [url]);

  if (error) {
    return <div style={{ color: "red", fontSize: 13, marginTop: 8 }}>{error}</div>;
  }

  if (!animationData) {
    return <div style={{ color: "#6b7280", fontSize: 13, marginTop: 8 }}>Loading preview...</div>;
  }

  return (
    <div style={{ marginTop: 8, width: "100%", maxWidth: 200 }}>
      <Lottie animationData={animationData} loop={true} />
    </div>
  );
}
