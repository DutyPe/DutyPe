"use client";

import { useEffect, useMemo, useState } from "react";

import { buildDeepLinkBundle, type DeepLinkKind } from "@/lib/public-site";

type LaunchState = "idle" | "launching" | "fallback";

type Props = {
  kind: DeepLinkKind;
  entityId?: string;
  headline: string;
  description: string;
  bullets: string[];
  autoOpen?: boolean;
  badge?: string;
};

function isMobileUserAgent(userAgent: string) {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent);
}

function launchFromDevice(
  userAgent: string,
  androidIntent: string,
  appScheme: string,
  playStoreUrl: string,
  appStoreUrl: string
) {
  if (/Android/i.test(userAgent)) {
    window.location.href = androidIntent;
    return;
  }

  if (/iPhone|iPad|iPod/i.test(userAgent)) {
    window.location.href = appScheme;
    window.setTimeout(() => {
      if (!document.hidden) {
        window.location.href = appStoreUrl;
      }
    }, 1800);
    return;
  }

  window.location.href = playStoreUrl;
}

export function AppLaunchCard({
  kind,
  entityId,
  headline,
  description,
  bullets,
  autoOpen = true,
  badge = "Open in app"
}: Props) {
  const bundle = useMemo(() => buildDeepLinkBundle(kind, entityId), [entityId, kind]);
  const [launchState, setLaunchState] = useState<LaunchState>("idle");

  useEffect(() => {
    if (!autoOpen) {
      return;
    }

    const userAgent = window.navigator.userAgent;
    if (!isMobileUserAgent(userAgent)) {
      return;
    }

    let fallbackTimer = 0;
    let launchTimer = 0;

    launchTimer = window.setTimeout(() => {
      setLaunchState("launching");
      launchFromDevice(
        userAgent,
        bundle.androidIntent,
        bundle.appScheme,
        bundle.playStoreUrl,
        bundle.appStoreUrl
      );

      fallbackTimer = window.setTimeout(() => {
        if (!document.hidden) {
          setLaunchState("fallback");
        }
      }, 2400);
    }, 450);

    return () => {
      window.clearTimeout(launchTimer);
      window.clearTimeout(fallbackTimer);
    };
  }, [autoOpen, bundle.androidIntent, bundle.appScheme]);

  const statusCopy =
    launchState === "launching"
      ? "Trying to open the DutyPe app."
      : launchState === "fallback"
        ? "If the app did not open, use the store link below."
        : "Use the app for the full live experience.";

  return (
    <aside className="launch-card">
      <span className="eyebrow">{badge}</span>
      <h3>{headline}</h3>
      <p>{description}</p>

      <ul className="launch-list">
        {bullets.map((bullet) => (
          <li key={bullet}>{bullet}</li>
        ))}
      </ul>

      <div className="launch-actions">
        <a
          className="button"
          href={bundle.androidIntent}
          onClick={(event) => {
            event.preventDefault();
            setLaunchState("launching");
            launchFromDevice(
              window.navigator.userAgent,
              bundle.androidIntent,
              bundle.appScheme,
              bundle.playStoreUrl,
              bundle.appStoreUrl
            );
          }}
        >
          Open DutyPe
        </a>
        <a className="button ghost" href={bundle.playStoreUrl}>
          Download app
        </a>
      </div>

      <div className="launch-status">
        <span className={`status-dot ${launchState}`} />
        <span>{statusCopy}</span>
      </div>
    </aside>
  );
}
