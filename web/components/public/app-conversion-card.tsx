"use client";

import Link from "next/link";
import Image from "next/image";
import { PLAY_STORE_URL } from "@/lib/public-site";

type Props = {
  title?: string;
  subtitle?: string;
  categoryOrCity?: string;
};

export function AppConversionCard({
  title,
  subtitle,
  categoryOrCity
}: Props) {
  const displayTitle =
    title ||
    (categoryOrCity
      ? `Apply to 100+ Verified ${categoryOrCity} Jobs on DutyPe App`
      : "Find & Apply to Local Jobs on the DutyPe App");

  const displaySubtitle =
    subtitle ||
    "Call employers directly with zero middlemen fees. Verified local openings with instant salary & shift details.";

  const downloadUrl = `${PLAY_STORE_URL}&referrer=utm_source%3Dweb_in_page_card%26utm_medium%3Dseo_page`;

  return (
    <div className="app-conversion-card-wrap">
      <style jsx>{`
        .app-conversion-card-wrap {
          margin: 2.5rem 0;
          background: linear-gradient(135deg, #0d3829 0%, #146b4f 100%);
          border-radius: 20px;
          padding: 2.25rem 2rem;
          color: #ffffff;
          box-shadow: 0 15px 35px -5px rgba(20, 107, 79, 0.35);
          position: relative;
          overflow: hidden;
        }

        .ambient-glow {
          position: absolute;
          top: -50px;
          right: -50px;
          width: 200px;
          height: 200px;
          border-radius: 50%;
          background: rgba(255, 255, 255, 0.12);
          pointer-events: none;
        }

        .card-inner {
          position: relative;
          z-index: 1;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 2rem;
          flex-wrap: wrap;
        }

        .content-side {
          flex: 1;
          min-width: 280px;
        }

        .badge-row {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 0.85rem;
          flex-wrap: wrap;
        }

        .kicker-badge {
          background: rgba(255, 255, 255, 0.2);
          backdrop-filter: blur(8px);
          font-size: 0.75rem;
          font-weight: 700;
          text-transform: uppercase;
          letter-spacing: 0.06em;
          padding: 0.3rem 0.75rem;
          border-radius: 9999px;
        }

        .rating-badge {
          background: #f59e0b;
          color: #000000;
          font-size: 0.75rem;
          font-weight: 800;
          padding: 0.3rem 0.65rem;
          border-radius: 9999px;
        }

        .card-title {
          font-size: 1.65rem;
          font-weight: 800;
          line-height: 1.25;
          margin: 0 0 0.65rem 0;
          color: #ffffff;
        }

        .card-subtitle {
          font-size: 0.98rem;
          line-height: 1.6;
          color: #d1fae5;
          margin: 0 0 1.25rem 0;
          max-width: 580px;
        }

        .feature-bullets {
          display: flex;
          gap: 1.5rem;
          flex-wrap: wrap;
          font-size: 0.88rem;
          font-weight: 600;
          color: #ecfdf5;
        }

        .feature-bullets span {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .cta-side {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 10px;
        }

        .play-store-btn {
          display: inline-flex;
          align-items: center;
          gap: 12px;
          background: #000000;
          color: #ffffff !important;
          padding: 12px 24px;
          border-radius: 12px;
          text-decoration: none;
          font-weight: 700;
          font-size: 1rem;
          box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
          transition: transform 0.2s ease, background 0.2s ease;
          border: 1px solid rgba(255, 255, 255, 0.2);
        }

        .play-store-btn:hover {
          background: #1e293b;
          transform: translateY(-2px);
        }

        .btn-text-block {
          display: flex;
          flex-direction: column;
          text-align: left;
          line-height: 1.1;
        }

        .btn-text-block small {
          font-size: 0.68rem;
          text-transform: uppercase;
          letter-spacing: 0.05em;
          color: #94a3b8;
        }

        .btn-text-block strong {
          font-size: 1.05rem;
          color: #ffffff;
        }

        .guarantee-note {
          font-size: 0.76rem;
          color: #a7f3d0;
          font-weight: 500;
        }

        @media (max-width: 640px) {
          .app-conversion-card-wrap {
            padding: 1.75rem 1.25rem;
          }
          .card-title {
            font-size: 1.35rem;
          }
          .card-inner {
            flex-direction: column;
            align-items: stretch;
          }
          .cta-side {
            align-items: stretch;
          }
          .play-store-btn {
            justify-content: center;
          }
        }
      `}</style>

      <div className="ambient-glow" />
      <div className="card-inner">
        <div className="content-side">
          <div className="badge-row">
            <span className="kicker-badge">DutyPe Android App</span>
            <span className="rating-badge">★ 4.8 Rating</span>
            <span className="kicker-badge">100% Free For Workers</span>
          </div>
          <h2 className="card-title">{displayTitle}</h2>
          <p className="card-subtitle">{displaySubtitle}</p>
          <div className="feature-bullets">
            <span>⚡ 1-Tap Apply</span>
            <span>📞 Direct Employer Call</span>
            <span>📍 Jobs within 5km</span>
          </div>
        </div>

        <div className="cta-side">
          <a
            href={downloadUrl}
            className="play-store-btn"
            target="_blank"
            rel="noopener noreferrer"
          >
            <span style={{ fontSize: "1.6rem" }}>📱</span>
            <div className="btn-text-block">
              <small>GET IT ON</small>
              <strong>Google Play</strong>
            </div>
          </a>
          <span className="guarantee-note">✓ Verified Employers · No Middlemen</span>
        </div>
      </div>
    </div>
  );
}
