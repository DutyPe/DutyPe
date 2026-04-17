"use client";

import { useEffect, useState } from "react";

function getScrollProgress() {
  const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
  if (maxScroll <= 0) {
    return 0;
  }

  return Math.min(1, Math.max(0, window.scrollY / maxScroll));
}

export function HomeImmersiveLayer() {
  const [progress, setProgress] = useState(0);
  const [isFinePointer, setIsFinePointer] = useState(false);
  const [cursorVisible, setCursorVisible] = useState(false);
  const [cursorX, setCursorX] = useState(0);
  const [cursorY, setCursorY] = useState(0);

  useEffect(() => {
    const media = window.matchMedia("(pointer: fine)");

    const updatePointerMode = () => {
      setIsFinePointer(media.matches);
    };

    updatePointerMode();
    media.addEventListener("change", updatePointerMode);

    return () => media.removeEventListener("change", updatePointerMode);
  }, []);

  useEffect(() => {
    const updateProgress = () => setProgress(getScrollProgress());

    updateProgress();
    window.addEventListener("scroll", updateProgress, { passive: true });
    window.addEventListener("resize", updateProgress);

    return () => {
      window.removeEventListener("scroll", updateProgress);
      window.removeEventListener("resize", updateProgress);
    };
  }, []);

  useEffect(() => {
    if (!isFinePointer) {
      setCursorVisible(false);
      return;
    }

    const handlePointerMove = (event: PointerEvent) => {
      setCursorX(event.clientX);
      setCursorY(event.clientY);
      setCursorVisible(true);
    };

    const handlePointerLeave = () => setCursorVisible(false);

    window.addEventListener("pointermove", handlePointerMove, { passive: true });
    window.addEventListener("pointerleave", handlePointerLeave);

    return () => {
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerleave", handlePointerLeave);
    };
  }, [isFinePointer]);

  return (
    <div className="home-immersive-layer" aria-hidden="true">
      <div className="home-scroll-progress" style={{ transform: `scaleX(${progress})` }} />
      {isFinePointer ? (
        <>
          <div
            className={`home-cursor-halo${cursorVisible ? " is-visible" : ""}`}
            style={{ left: cursorX, top: cursorY }}
          />
          <div
            className={`home-cursor-dot${cursorVisible ? " is-visible" : ""}`}
            style={{ left: cursorX, top: cursorY }}
          />
        </>
      ) : null}
    </div>
  );
}
