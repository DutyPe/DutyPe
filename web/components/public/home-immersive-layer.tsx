"use client";

import { useEffect, useRef } from "react";

/**
 * Writes scroll/pointer state straight to CSS custom properties inside a rAF.
 * Deliberately holds no React state: the previous version called setState on
 * every pointermove and scroll event, re-rendering the tree ~100x/sec.
 */
export function HomeImmersiveLayer() {
  const rootRef = useRef<HTMLDivElement>(null);
  const progressRef = useRef<HTMLDivElement>(null);
  const frame = useRef(0);
  const target = useRef({ x: 0, y: 0, progress: 0, visible: 0 });

  useEffect(() => {
    const root = rootRef.current;
    const bar = progressRef.current;
    if (!root || !bar) return;

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    const finePointer = window.matchMedia("(pointer: fine)");

    const readProgress = () => {
      const max = document.documentElement.scrollHeight - window.innerHeight;
      return max <= 0 ? 0 : Math.min(1, Math.max(0, window.scrollY / max));
    };

    const paint = () => {
      frame.current = 0;
      const { x, y, progress, visible } = target.current;
      bar.style.transform = `scaleX(${progress})`;
      root.style.setProperty("--cursor-x", `${x}px`);
      root.style.setProperty("--cursor-y", `${y}px`);
      root.style.setProperty("--cursor-visible", `${visible}`);
    };

    const schedule = () => {
      if (frame.current) return;
      frame.current = requestAnimationFrame(paint);
    };

    const onScroll = () => {
      target.current.progress = readProgress();
      schedule();
    };

    const onPointerMove = (event: PointerEvent) => {
      if (!finePointer.matches || reduceMotion.matches) return;
      target.current.x = event.clientX;
      target.current.y = event.clientY;
      target.current.visible = 1;
      schedule();
    };

    const onPointerLeave = () => {
      target.current.visible = 0;
      schedule();
    };

    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll);
    window.addEventListener("pointermove", onPointerMove, { passive: true });
    window.addEventListener("pointerleave", onPointerLeave);

    return () => {
      if (frame.current) cancelAnimationFrame(frame.current);
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
      window.removeEventListener("pointermove", onPointerMove);
      window.removeEventListener("pointerleave", onPointerLeave);
    };
  }, []);

  return (
    <div ref={rootRef} className="home-immersive-layer" aria-hidden="true">
      <div ref={progressRef} className="home-scroll-progress" />
      <div className="home-cursor-halo" />
      <div className="home-cursor-dot" />
      <div className="home-aurora" />
    </div>
  );
}
