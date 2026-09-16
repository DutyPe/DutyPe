"use client";

import { layout, prepare } from "@chenglou/pretext";
import { useEffect, useId, useRef, useState } from "react";

export function JobDescription({ text, maxLines = 3 }: { text: string; maxLines?: number }) {
  const paragraphRef = useRef<HTMLParagraphElement>(null);
  const descriptionId = useId();
  const [lineCount, setLineCount] = useState<number | null>(null);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    setExpanded(false);
    setLineCount(null);
    const paragraph = paragraphRef.current;
    if (!paragraph || typeof ResizeObserver === "undefined" || typeof Intl.Segmenter === "undefined") {
      return;
    }

    let disposed = false;
    let prepared: ReturnType<typeof prepare> | undefined;
    let fontKey = "";
    let observer: ResizeObserver | undefined;

    function measure(width: number) {
      if (disposed || !paragraph || width <= 0) return;
      try {
        const style = getComputedStyle(paragraph);
        const font = `${style.fontStyle} ${style.fontWeight} ${style.fontSize} ${style.fontFamily}`;
        const letterSpacing = Number.parseFloat(style.letterSpacing) || 0;
        const nextFontKey = `${font}:${letterSpacing}`;
        if (!prepared || nextFontKey !== fontKey) {
          prepared = prepare(text, font, { letterSpacing });
          fontKey = nextFontKey;
        }
        const lineHeight = Number.parseFloat(style.lineHeight) || Number.parseFloat(style.fontSize) * 1.5;
        setLineCount(layout(prepared, width, lineHeight).lineCount);
      } catch {
        setLineCount(null);
      }
    }

    function refreshFont() {
      prepared = undefined;
      if (paragraph) measure(paragraph.clientWidth);
    }

    void document.fonts.ready.then(() => {
      if (disposed) return;
      observer = new ResizeObserver((entries) => {
        const entry = entries[0];
        if (entry) measure(entry.contentRect.width);
      });
      observer.observe(paragraph);
      document.fonts.addEventListener("loadingdone", refreshFont);
    });

    return () => {
      disposed = true;
      observer?.disconnect();
      document.fonts.removeEventListener("loadingdone", refreshFont);
    };
  }, [text]);

  const canCollapse = lineCount !== null && lineCount > maxLines;

  return (
    <div className="job-description">
      <p
        id={descriptionId}
        ref={paragraphRef}
        className={`market-card-copy${canCollapse && !expanded ? " description-collapsed" : ""}`}
        style={canCollapse && !expanded ? { WebkitLineClamp: maxLines } : undefined}
        data-line-count={lineCount ?? undefined}
      >
        {text}
      </p>
      {canCollapse ? (
        <button
          type="button"
          className="description-toggle"
          aria-expanded={expanded}
          aria-controls={descriptionId}
          onClick={() => setExpanded((current) => !current)}
        >
          {expanded ? "Show less" : "Show more"}
        </button>
      ) : null}
    </div>
  );
}