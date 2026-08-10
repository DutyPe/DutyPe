"use client";

import { useEffect } from "react";

const REVEAL_SELECTOR = "[data-reveal], .home-interactive-experience .section";

/**
 * Drives `[data-reveal]` and `[data-tilt]` without any animation library.
 * Reveal uses IntersectionObserver and unobserves after firing; tilt writes
 * CSS custom properties inside a rAF and is skipped on coarse pointers and
 * when the user asks for reduced motion.
 */
export function MotionLayer() {
  useEffect(() => {
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

    // Only now do the hiding styles apply, so a JS failure can never leave
    // content stuck at opacity:0.
    document.documentElement.classList.add("dp-motion-ready");

    if (reduceMotion.matches) {
      document
        .querySelectorAll<HTMLElement>(REVEAL_SELECTOR)
        .forEach((el) => el.classList.add("is-revealed"));
      return;
    }

    const revealTargets =
      document.querySelectorAll<HTMLElement>(REVEAL_SELECTOR);
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          entry.target.classList.add("is-revealed");
          observer.unobserve(entry.target);
        }
      },
      { rootMargin: "0px 0px -12% 0px", threshold: 0.12 }
    );
    revealTargets.forEach((el, index) => {
      el.style.setProperty("--reveal-index", String(index % 6));
      observer.observe(el);
    });

    const finePointer = window.matchMedia("(pointer: fine)");
    const tiltTargets = finePointer.matches
      ? Array.from(document.querySelectorAll<HTMLElement>("[data-tilt]"))
      : [];
    let frame = 0;
    let pending: { el: HTMLElement; x: number; y: number } | null = null;

    const paint = () => {
      frame = 0;
      if (!pending) return;
      pending.el.style.setProperty("--tilt-x", `${pending.y.toFixed(2)}deg`);
      pending.el.style.setProperty("--tilt-y", `${pending.x.toFixed(2)}deg`);
    };

    const onMove = (event: PointerEvent) => {
      const el = event.currentTarget as HTMLElement;
      const rect = el.getBoundingClientRect();
      const px = (event.clientX - rect.left) / rect.width - 0.5;
      const py = (event.clientY - rect.top) / rect.height - 0.5;
      pending = { el, x: px * 12, y: -py * 12 };
      if (!frame) frame = requestAnimationFrame(paint);
    };

    const onLeave = (event: PointerEvent) => {
      const el = event.currentTarget as HTMLElement;
      el.style.setProperty("--tilt-x", "0deg");
      el.style.setProperty("--tilt-y", "0deg");
    };

    tiltTargets.forEach((el) => {
      el.addEventListener("pointermove", onMove);
      el.addEventListener("pointerleave", onLeave);
    });

    return () => {
      observer.disconnect();
      if (frame) cancelAnimationFrame(frame);
      document.documentElement.classList.remove("dp-motion-ready");
      tiltTargets.forEach((el) => {
        el.removeEventListener("pointermove", onMove);
        el.removeEventListener("pointerleave", onLeave);
      });
    };
  }, []);

  return null;
}
