"use client";

import { useEffect, useState } from "react";

type HeroSignal = {
  label: string;
  title: string;
  copy: string;
};

type HeroSignalDeckProps = {
  signals: HeroSignal[];
};

export function HeroSignalDeck({ signals }: HeroSignalDeckProps) {
  const [activeIndex, setActiveIndex] = useState(0);
  const [isAutoPlay, setIsAutoPlay] = useState(true);

  useEffect(() => {
    if (!isAutoPlay || signals.length <= 1) {
      return;
    }

    const timer = window.setInterval(() => {
      setActiveIndex((prev) => (prev + 1) % signals.length);
    }, 3200);

    return () => window.clearInterval(timer);
  }, [isAutoPlay, signals.length]);

  return (
    <div
      className="signal-stack"
      onMouseEnter={() => setIsAutoPlay(false)}
      onMouseLeave={() => setIsAutoPlay(true)}
      onTouchStart={() => setIsAutoPlay(false)}
      onTouchEnd={() => setIsAutoPlay(true)}
    >
      {signals.map((signal, index) => {
        const isActive = index === activeIndex;
        return (
          <button
            key={signal.title}
            type="button"
            className={`signal-card signal-card-interactive${isActive ? " active" : ""}`}
            onClick={() => setActiveIndex(index)}
            aria-pressed={isActive}
          >
            <span className="signal-label">{signal.label}</span>
            <strong>{signal.title}</strong>
            <p>{signal.copy}</p>
          </button>
        );
      })}
    </div>
  );
}
