"use client";

import { useState } from "react";
import { PLAY_STORE_URL } from "@/lib/public-site";
import { formatCurrencyRange, formatDate } from "@/lib/firebase/firestore-helpers";

type PublicJob = {
  id: string;
  title?: string;
  companyName?: string;
  salary?: string | number;
  salaryType?: string;
  addressText?: string;
  vacancies?: number;
  status?: string;
  createdAt?: string | number | null;
  expiresAt?: string | number | null;
  description?: string;
  contactNumber?: string;
  gender?: string;
  experienceRequired?: string;
  educationRequired?: string;
  jobType?: string;
  shiftTiming?: string;
};

type Props = {
  job: PublicJob;
};

export function PublicJobPageClient({ job }: Props) {
  const [showModal, setShowModal] = useState(false);

  const formattedSalary = formatCurrencyRange(job.salary, job.salaryType);
  const formattedPostedDate = job.createdAt ? formatDate(job.createdAt) : "N/A";
  const formattedExpiryDate = job.expiresAt ? formatDate(job.expiresAt) : "N/A";

  const isClosed = job.status === "closed" || job.status === "expired";

  const downloadUrl = `${PLAY_STORE_URL}&referrer=utm_source%3Dweb_job_apply%26utm_medium%3Djob_detail%26utm_campaign%3D${job.id || "general"}`;
  const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=${encodeURIComponent(downloadUrl)}`;

  const handleApplyClick = () => {
    if (isClosed) return;
    if (typeof window !== "undefined") {
      const isMobile = /Android|iPhone|iPad|iPod|webOS|BlackBerry|IEMobile|Opera Mini/i.test(
        navigator.userAgent
      );
      if (isMobile) {
        // Direct redirect on mobile to maximize instant app install conversion
        window.open(downloadUrl, "_blank", "noopener,noreferrer");
        return;
      }
    }
    setShowModal(true);
  };

  const customStyles = `
    .job-detail-page {
      max-width: 1000px;
      margin: 0 auto;
      padding: 2rem 1rem 5rem 1rem;
    }

    .job-hero {
      background: rgba(255, 255, 255, 0.95);
      border: 1px solid rgba(16, 24, 40, 0.08);
      border-radius: 16px;
      padding: 2.5rem;
      margin-bottom: 2rem;
      box-shadow: 0 10px 30px rgba(16, 24, 40, 0.03);
    }

    .job-category-tag {
      display: inline-block;
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: #146b4f;
      background: #e8f5e9;
      padding: 0.3rem 0.85rem;
      border-radius: 9999px;
      margin-bottom: 1.25rem;
    }

    .job-title-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 1.5rem;
      flex-wrap: wrap;
    }

    .job-title-row h1 {
      font-size: 2.25rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 0.5rem 0;
      line-height: 1.2;
    }

    .job-company {
      font-size: 1.35rem;
      color: #475569;
      font-weight: 600;
      margin: 0;
    }

    .job-status-badge {
      display: inline-block;
      font-size: 0.85rem;
      font-weight: 700;
      padding: 0.4rem 1rem;
      border-radius: 9999px;
    }

    .job-status-badge.open {
      color: #15803d;
      background: #dcfce7;
      border: 1px solid #bbf7d0;
    }

    .job-status-badge.closed {
      color: #b91c1c;
      background: #fee2e2;
      border: 1px solid #fecaca;
    }

    .job-meta-pills {
      display: flex;
      gap: 2rem;
      margin-top: 2rem;
      border-top: 1px solid rgba(16, 24, 40, 0.08);
      padding-top: 1.75rem;
      flex-wrap: wrap;
    }

    .meta-pill {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.95rem;
      color: #64748b;
    }

    .meta-pill strong {
      color: #1e293b;
    }

    .job-grid {
      display: grid;
      grid-template-columns: 1.85fr 1.15fr;
      gap: 2rem;
      align-items: start;
    }

    @media (max-width: 768px) {
      .job-grid {
        grid-template-columns: 1fr;
      }
    }

    .job-main-card, .job-side-card {
      background: rgba(255, 255, 255, 0.95);
      border: 1px solid rgba(16, 24, 40, 0.08);
      border-radius: 16px;
      padding: 2.25rem;
      box-shadow: 0 10px 30px rgba(16, 24, 40, 0.03);
    }

    .job-section {
      margin-bottom: 2.5rem;
    }

    .job-section:last-child {
      margin-bottom: 0;
    }

    .job-section h2 {
      font-size: 1.35rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 1.25rem 0;
      border-bottom: 2px solid #e2e8f0;
      padding-bottom: 0.75rem;
      position: relative;
    }

    .job-section h2::after {
      content: "";
      position: absolute;
      bottom: -2px;
      left: 0;
      width: 40px;
      height: 2px;
      background: #146b4f;
    }

    .job-description-text {
      font-size: 1.05rem;
      line-height: 1.8;
      color: #334155;
      white-space: pre-line;
    }

    .facts-list {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .fact-item {
      display: flex;
      justify-content: space-between;
      border-bottom: 1px dashed #e2e8f0;
      padding-bottom: 0.85rem;
      font-size: 1rem;
    }

    .fact-item:last-child {
      border-bottom: none;
      padding-bottom: 0;
    }

    .fact-label {
      color: #64748b;
    }

    .fact-value {
      font-weight: 700;
      color: #1e293b;
    }

    .side-cta-box {
      margin-bottom: 2rem;
    }

    .salary-highlight {
      background: #e8f5e9;
      border: 1px solid #c8e6c9;
      border-radius: 12px;
      padding: 1.75rem;
      text-align: center;
      margin-bottom: 2rem;
    }

    .salary-label {
      font-size: 0.85rem;
      color: #1b5e20;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      display: block;
      margin-bottom: 0.35rem;
    }

    .salary-value {
      font-size: 2rem;
      font-weight: 800;
      color: #1b5e20;
      display: block;
    }

    .apply-btn {
      width: 100%;
      padding: 1.1rem;
      background: #146b4f;
      color: white;
      border: none;
      border-radius: 10px;
      font-size: 1.15rem;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 0.5rem;
      box-shadow: 0 4px 14px rgba(20, 107, 79, 0.25);
    }

    .apply-btn:hover {
      background: #0f523c;
      box-shadow: 0 6px 20px rgba(20, 107, 79, 0.35);
    }

    .apply-btn:active {
      transform: translateY(1px);
    }

    .apply-btn:disabled {
      background: #cbd5e1;
      color: #64748b;
      cursor: not-allowed;
      box-shadow: none;
    }

    .call-btn {
      width: 100%;
      padding: 1.1rem;
      background: white;
      color: #146b4f;
      border: 1.5px solid #146b4f;
      border-radius: 10px;
      font-size: 1.15rem;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.2s ease;
      margin-top: 1rem;
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 0.5rem;
    }

    .call-btn:hover {
      background: #e8f5e9;
    }

    .call-btn:disabled {
      color: #94a3b8;
      border-color: #cbd5e1;
      cursor: not-allowed;
    }

    .safety-warning-card {
      background: #fffbeb;
      border: 1px solid #fef3c7;
      border-radius: 12px;
      padding: 1.25rem;
      font-size: 0.9rem;
      color: #b45309;
      line-height: 1.6;
      margin-top: 2rem;
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
    }

    /* Modal styling with QR code */
    .download-modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(15, 23, 42, 0.7);
      backdrop-filter: blur(8px);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 9999;
      padding: 1rem;
      animation: fadeIn 0.2s ease-out;
    }

    .download-modal {
      background: white;
      border-radius: 24px;
      max-width: 480px;
      width: 100%;
      padding: 2.25rem;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
      text-align: center;
      position: relative;
      animation: slideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1);
    }

    .modal-badge {
      display: inline-block;
      background: #dcfce7;
      color: #15803d;
      font-size: 0.8rem;
      font-weight: 700;
      padding: 0.3rem 0.85rem;
      border-radius: 9999px;
      margin-bottom: 0.75rem;
    }

    .download-modal h3 {
      font-size: 1.65rem;
      font-weight: 800;
      color: #0f172a;
      margin: 0 0 0.5rem 0;
    }

    .download-modal p {
      font-size: 0.95rem;
      color: #475569;
      line-height: 1.5;
      margin: 0 0 1.5rem 0;
    }

    .qr-section {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 16px;
      padding: 1.25rem;
      margin-bottom: 1.5rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
    }

    .qr-img {
      width: 130px;
      height: 130px;
      border-radius: 8px;
      border: 1px solid rgba(0, 0, 0, 0.08);
      background: white;
      padding: 6px;
    }

    .qr-label {
      font-size: 0.82rem;
      color: #64748b;
      font-weight: 600;
    }

    .modal-bullet-list {
      text-align: left;
      margin: 0 auto 1.5rem auto;
      max-width: 380px;
      padding: 0;
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 0.65rem;
    }

    .modal-bullet-list li {
      font-size: 0.9rem;
      color: #334155;
      display: flex;
      align-items: center;
      gap: 0.65rem;
      font-weight: 600;
    }

    .modal-bullet-list li span {
      font-size: 1rem;
      flex-shrink: 0;
    }

    .modal-download-btn {
      display: inline-flex;
      justify-content: center;
      align-items: center;
      gap: 0.65rem;
      background: #000000;
      color: white !important;
      font-weight: 700;
      font-size: 1.05rem;
      padding: 0.95rem 1.75rem;
      border-radius: 12px;
      text-decoration: none;
      width: 100%;
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.2);
      transition: background 0.2s ease, transform 0.15s ease;
    }

    .modal-download-btn:hover {
      background: #1e293b;
      transform: translateY(-1px);
    }

    .modal-close-btn {
      display: block;
      margin-top: 1rem;
      background: none;
      border: none;
      color: #64748b;
      font-size: 0.9rem;
      font-weight: 600;
      cursor: pointer;
      text-decoration: underline;
      width: 100%;
    }

    .modal-close-btn:hover {
      color: #1e293b;
    }

    /* Sticky mobile action bar */
    .mobile-sticky-bar {
      display: none;
    }

    @media (max-width: 768px) {
      .mobile-sticky-bar {
        display: flex;
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        background: rgba(255, 255, 255, 0.96);
        backdrop-filter: blur(10px);
        padding: 0.75rem 1rem;
        border-top: 1px solid #e2e8f0;
        box-shadow: 0 -4px 15px rgba(0, 0, 0, 0.08);
        z-index: 998;
        align-items: center;
        justify-content: space-between;
        gap: 0.75rem;
      }

      .mobile-bar-info {
        display: flex;
        flex-direction: column;
      }

      .mobile-bar-salary {
        font-size: 1.15rem;
        font-weight: 800;
        color: #146b4f;
      }

      .mobile-bar-title {
        font-size: 0.78rem;
        color: #64748b;
        font-weight: 600;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 140px;
      }

      .mobile-bar-btn {
        flex: 1;
        max-width: 180px;
        padding: 0.75rem 1rem;
        background: #146b4f;
        color: white;
        border: none;
        border-radius: 9999px;
        font-size: 0.95rem;
        font-weight: 700;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.35rem;
        box-shadow: 0 3px 8px rgba(20, 107, 79, 0.3);
      }
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes slideUp {
      from { transform: translateY(24px); opacity: 0; }
      to { transform: translateY(0); opacity: 1; }
    }
  `;

  return (
    <div className="job-detail-page">
      <style dangerouslySetInnerHTML={{ __html: customStyles }} />

      {/* Hero section */}
      <section className="job-hero">
        <span className="job-category-tag">{job.jobType || "Job opening"}</span>
        <div className="job-title-row">
          <div>
            <h1>{job.title || "Untitled Job"}</h1>
            <p className="job-company">{job.companyName || "DutyPe Employer"}</p>
          </div>
          <span className={`job-status-badge ${isClosed ? "closed" : "open"}`}>
            {job.status === "open" ? "Hiring Now" : job.status === "closed" ? "Inactive" : "Expired"}
          </span>
        </div>

        <div className="job-meta-pills">
          <div className="meta-pill">
            <span>📅 Posted:</span>
            <strong>{formattedPostedDate}</strong>
          </div>
          {job.vacancies ? (
            <div className="meta-pill">
              <span>👥 Vacancies:</span>
              <strong>{job.vacancies} openings</strong>
            </div>
          ) : null}
          {job.addressText ? (
            <div className="meta-pill">
              <span>📍 Location:</span>
              <strong>{job.addressText}</strong>
            </div>
          ) : null}
        </div>
      </section>

      {/* Grid details */}
      <div className="job-grid">
        {/* Left Column: Details */}
        <div className="job-main-card">
          <section className="job-section">
            <h2>Job Description</h2>
            <div className="job-description-text">{job.description || "No description provided."}</div>
          </section>

          <section className="job-section">
            <h2>Requirements & Specifications</h2>
            <div className="facts-list">
              <div className="fact-item">
                <span className="fact-label">Experience Required</span>
                <span className="fact-value">{job.experienceRequired || "No Experience Required"}</span>
              </div>
              <div className="fact-item">
                <span className="fact-label">Education Required</span>
                <span className="fact-value">{job.educationRequired || "No qualification required"}</span>
              </div>
              <div className="fact-item">
                <span className="fact-label">Gender Preference</span>
                <span className="fact-value">{job.gender || "Both (Male / Female)"}</span>
              </div>
              <div className="fact-item">
                <span className="fact-label">Shift & Timings</span>
                <span className="fact-value">{job.shiftTiming || "Flexible timings"}</span>
              </div>
            </div>
          </section>
        </div>

        {/* Right Column: Actions & Salary */}
        <div className="job-side-card">
          <div className="salary-highlight">
            <span className="salary-label">Offered Salary</span>
            <span className="salary-value">{formattedSalary}</span>
          </div>

          <div className="side-cta-box">
            <button className="apply-btn" onClick={handleApplyClick} disabled={isClosed}>
              💼 Apply for Job
            </button>
            <button className="call-btn" onClick={handleApplyClick} disabled={isClosed}>
              📞 Call Employer
            </button>
          </div>

          <div className="facts-list">
            <div className="fact-item">
              <span className="fact-label">Job Status</span>
              <span className="fact-value" style={{ color: isClosed ? '#b91c1c' : '#15803d' }}>
                {job.status === "open" ? "Active" : "Closed / Expired"}
              </span>
            </div>
            <div className="fact-item">
              <span className="fact-label">Total Openings</span>
              <span className="fact-value">{job.vacancies || 1}</span>
            </div>
            <div className="fact-item">
              <span className="fact-label">Expiry Date</span>
              <span className="fact-value">{formattedExpiryDate}</span>
            </div>
          </div>

          <div className="safety-warning-card">
            <span style={{ fontSize: '1.25rem', marginTop: '0.15rem' }}>⚠️</span>
            <div>
              <strong>Safety Warning:</strong> Workers should never pay any money or fee to any employer or agent to secure a job. DutyPe is 100% free for workers.
            </div>
          </div>
        </div>
      </div>

      {/* Sticky Mobile Apply Bar */}
      {!isClosed && (
        <div className="mobile-sticky-bar">
          <div className="mobile-bar-info">
            <span className="mobile-bar-salary">{formattedSalary}</span>
            <span className="mobile-bar-title">{job.title || "Job opening"}</span>
          </div>
          <button className="mobile-bar-btn" onClick={handleApplyClick}>
            💼 Apply Free
          </button>
        </div>
      )}

      {/* Download Popup Modal for Desktop */}
      {showModal && (
        <div className="download-modal-overlay" onClick={() => setShowModal(false)}>
          <div className="download-modal" onClick={(e) => e.stopPropagation()}>
            <span className="modal-badge">Direct Hiring · 100% Free</span>
            <h3>Apply on DutyPe App</h3>
            <p>
              To apply for this job and talk directly with the employer, download the DutyPe Android App.
            </p>

            <div className="qr-section">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={qrCodeUrl}
                alt="Scan to download DutyPe App"
                className="qr-img"
                width={130}
                height={130}
              />
              <span className="qr-label">📱 Scan with your phone camera to download instantly</span>
            </div>

            <ul className="modal-bullet-list">
              <li>
                <span>✓</span> Contact verified employers directly with zero agent fees.
              </li>
              <li>
                <span>✓</span> 100% Free for all job seekers.
              </li>
              <li>
                <span>✓</span> Instant 1-tap apply with live job updates.
              </li>
            </ul>

            <a href={downloadUrl} className="modal-download-btn" target="_blank" rel="noopener noreferrer">
              <span>Get it on</span>
              <strong>Google Play</strong>
            </a>

            <button className="modal-close-btn" onClick={() => setShowModal(false)}>
              Back to Job details
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
