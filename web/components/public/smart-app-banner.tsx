"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { PLAY_STORE_URL } from "@/lib/public-site";

export function SmartAppBanner() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    // Only show on client side and if not dismissed in current session
    try {
      const isDismissed = sessionStorage.getItem("dutype_app_banner_dismissed");
      if (!isDismissed) {
        // Small delay so page loads smoothly first
        const timer = setTimeout(() => setVisible(true), 800);
        return () => clearTimeout(timer);
      }
    } catch {
      setVisible(true);
    }
  }, []);

  const handleDismiss = () => {
    setVisible(false);
    try {
      sessionStorage.setItem("dutype_app_banner_dismissed", "true");
    } catch {
      // Ignore sessionStorage errors
    }
  };

  if (!visible) return null;

  const downloadUrl = `${PLAY_STORE_URL}&referrer=utm_source%3Dweb_floating_banner%26utm_medium%3Dorganic_web`;

  return (
    <div className="smart-app-banner-wrap" role="complementary" aria-label="DutyPe Mobile App">
      <style jsx>{`
        .smart-app-banner-wrap {
          position: fixed;
          bottom: 12px;
          left: 12px;
          right: 12px;
          max-width: 480px;
          margin: 0 auto;
          z-index: 999;
          animation: bannerSlideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1);
        }

        .smart-app-banner {
          display: flex;
          align-items: center;
          gap: 10px;
          background: #ffffff;
          padding: 10px 14px;
          border-radius: 16px;
          border: 1px solid rgba(20, 107, 79, 0.2);
          box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.18), 0 4px 10px -2px rgba(20, 107, 79, 0.12);
        }

        .app-icon-wrap {
          position: relative;
          width: 42px;
          height: 42px;
          flex-shrink: 0;
          border-radius: 10px;
          overflow: hidden;
          background: #f1f5f9;
          border: 1px solid rgba(0, 0, 0, 0.06);
        }

        .banner-text {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
        }

        .app-name {
          font-size: 0.92rem;
          font-weight: 700;
          color: #0f172a;
          line-height: 1.2;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .app-subtext {
          font-size: 0.76rem;
          color: #15803d;
          font-weight: 600;
          display: flex;
          align-items: center;
          gap: 4px;
          margin-top: 2px;
        }

        .app-subtext .rating {
          color: #b45309;
          font-weight: 700;
        }

        .install-btn {
          background: #146b4f;
          color: #ffffff !important;
          font-size: 0.85rem;
          font-weight: 700;
          padding: 8px 16px;
          border-radius: 9999px;
          text-decoration: none;
          flex-shrink: 0;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 4px 10px rgba(20, 107, 79, 0.3);
          transition: transform 0.15s ease, background 0.15s ease;
        }

        .install-btn:hover {
          background: #0f523c;
          transform: translateY(-1px);
        }

        .install-btn:active {
          transform: translateY(0);
        }

        .close-btn {
          background: none;
          border: none;
          padding: 4px;
          color: #94a3b8;
          font-size: 1.1rem;
          line-height: 1;
          cursor: pointer;
          flex-shrink: 0;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .close-btn:hover {
          color: #475569;
          background: #f1f5f9;
        }

        @keyframes bannerSlideUp {
          from {
            transform: translateY(60px);
            opacity: 0;
          }
          to {
            transform: translateY(0);
            opacity: 1;
          }
        }

        @media (min-width: 769px) {
          .smart-app-banner-wrap {
            bottom: 20px;
            right: 20px;
            left: auto;
            max-width: 360px;
          }
        }
      `}</style>

      <div className="smart-app-banner">
        <button className="close-btn" onClick={handleDismiss} aria-label="Dismiss app banner">
          ×
        </button>
        <div className="app-icon-wrap">
          <Image
            src="/icon.webp"
            alt="DutyPe App"
            width={42}
            height={42}
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
          />
        </div>
        <div className="banner-text">
          <span className="app-name">DutyPe: Local Jobs App</span>
          <span className="app-subtext">
            <span className="rating">★ 4.8</span> · Free · Direct Apply
          </span>
        </div>
        <a
          href={downloadUrl}
          className="install-btn"
          target="_blank"
          rel="noopener noreferrer"
        >
          Install App
        </a>
      </div>
    </div>
  );
}
