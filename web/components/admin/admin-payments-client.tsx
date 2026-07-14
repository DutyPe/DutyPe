"use client";

import { useEffect, useMemo, useState } from "react";
import {
  collection,
  doc,
  getDocs,
  getDoc,
  query,
  where,
  updateDoc,
  setDoc,
  deleteDoc,
  onSnapshot,
  Timestamp,
  orderBy
} from "firebase/firestore";
import { getStorage, ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { getFirebaseServices } from "@/lib/firebase/client";

type PaymentRequestRow = {
  id: string;
  employerId: string;
  employerPhone: string;
  planId: string;
  amount: number;
  upiIdUsed: string;
  utrNumber: string;
  screenshotUrl: string;
  status: string;
  requestTimestamp: number;
  verifiedTimestamp?: number | null;
  expiryTimestamp?: number | null;
  rejectionReason?: string | null;
};

type QrCodeRow = {
  id: string;
  imageUrl: string;
  label: string;
  isActive: boolean;
  createdAt: number;
};

type JobRow = {
  id: string;
  title: string;
  companyName: string;
  employerPhone: string;
  employerId: string;
  createdAt: any;
  expiresAt: any;
};

export function AdminPaymentsClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [activeTab, setActiveTab] = useState<"requests" | "qrs" | "extensions">("requests");

  // Requests State
  const [requests, setRequests] = useState<PaymentRequestRow[]>([]);
  const [loadingRequests, setLoadingRequests] = useState(true);
  const [rejectReason, setRejectReason] = useState<{ [id: string]: string }>({});

  // QRs State
  const [qrs, setQrs] = useState<QrCodeRow[]>([]);
  const [qrLabel, setQrLabel] = useState("");
  const [qrFile, setQrFile] = useState<File | null>(null);
  const [uploadingQr, setUploadingQr] = useState(false);

  // Job Search State
  const [searchPhone, setSearchPhone] = useState("");
  const [jobs, setJobs] = useState<JobRow[]>([]);
  const [searchingJobs, setSearchingJobs] = useState(false);
  const [extensionDays, setExtensionDays] = useState<{ [jobId: string]: string }>({});

  // Global messages
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Listen for requests
  useEffect(() => {
    if (!services) return;
    const q = query(
      collection(services.db, "subscription_payment_requests"),
      orderBy("requestTimestamp", "desc")
    );
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const rows = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data()
      })) as PaymentRequestRow[];
      setRequests(rows);
      setLoadingRequests(false);
    }, (err) => {
      setError(err.message);
      setLoadingRequests(false);
    });
    return () => unsubscribe();
  }, [services]);

  // Listen for QR codes
  useEffect(() => {
    if (!services) return;
    const q = query(collection(services.db, "active_qr_codes"), orderBy("createdAt", "desc"));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const rows = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data()
      })) as QrCodeRow[];
      setQrs(rows);
    });
    return () => unsubscribe();
  }, [services]);

  // Handle Request Approval
  async function handleApprove(req: PaymentRequestRow) {
    if (!services) return;
    if (!confirm(`Are you sure you want to approve transaction UTR: ${req.utrNumber}?`)) return;

    try {
      setMessage(`Approving & activating plan for ${req.employerPhone}...`);
      setError(null);

      // 1. Calculate limits & credits
      let normalCredits = 3;
      let instantCredits = 5;
      if (req.planId === "growth_149") {
        normalCredits = 6;
        instantCredits = 15;
      } else if (req.planId === "premium_299") {
        normalCredits = 12;
        instantCredits = 40;
      } else if (req.planId === "single_49") {
        normalCredits = 1;
        instantCredits = 0;
      } else if (req.planId === "starter_99") {
        normalCredits = 3;
        instantCredits = 5;
      }

      const now = Date.now();
      const expiry = now + 30 * 24 * 60 * 60 * 1000; // 30 days

      // 2. Update Subscription on Employer Profile Document
      const profileRef = doc(services.db, "employer_profiles", req.employerId);
      const profileSnap = await getDoc(profileRef);
      if (!profileSnap.exists()) {
        throw new Error("Employer profile document not found. Ensure the user is registered correctly.");
      }

      await updateDoc(profileRef, {
        subscription: {
          status: "ACTIVE",
          planId: req.planId,
          startDate: now,
          expiryDate: expiry,
          credits: {
            normal: normalCredits,
            instant: instantCredits
          }
        }
      });

      // 3. Mark payment request as VERIFIED
      const reqRef = doc(services.db, "subscription_payment_requests", req.id);
      await updateDoc(reqRef, {
        status: "VERIFIED",
        verifiedTimestamp: now,
        expiryTimestamp: expiry
      });

      setMessage("Subscription activated and payment verified successfully!");
    } catch (err: any) {
      setError(err.message || "Failed to approve request.");
    }
  }

  // Handle Request Rejection
  async function handleReject(req: PaymentRequestRow) {
    if (!services) return;
    const reason = rejectReason[req.id]?.trim();
    if (!reason) {
      alert("Please provide a rejection reason first.");
      return;
    }
    if (!confirm(`Are you sure you want to reject transaction UTR: ${req.utrNumber}?`)) return;

    try {
      setMessage(`Rejecting transaction UTR: ${req.utrNumber}...`);
      setError(null);

      const reqRef = doc(services.db, "subscription_payment_requests", req.id);
      await updateDoc(reqRef, {
        status: "REJECTED",
        rejectionReason: reason,
        verifiedTimestamp: Date.now()
      });

      setMessage("Payment request rejected successfully.");
      setRejectReason(prev => ({ ...prev, [req.id]: "" }));
    } catch (err: any) {
      setError(err.message || "Failed to reject request.");
    }
  }

  // Handle QR upload
  async function handleAddQr(e: React.FormEvent) {
    e.preventDefault();
    if (!services || !qrFile || !qrLabel.trim()) return;

    try {
      setUploadingQr(true);
      setMessage("Uploading QR code to Firebase Storage...");
      setError(null);

      const storage = getStorage(services.app);
      const fileRef = ref(storage, `active_qrs/${Date.now()}_${qrFile.name}`);
      await uploadBytes(fileRef, qrFile);
      const downloadUrl = await getDownloadURL(fileRef);

      const newQrRef = doc(collection(services.db, "active_qr_codes"));
      await setDoc(newQrRef, {
        id: newQrRef.id,
        imageUrl: downloadUrl,
        label: qrLabel.trim(),
        isActive: true,
        createdAt: Date.now()
      });

      setQrLabel("");
      setQrFile(null);
      setMessage("QR Code uploaded and activated successfully!");
    } catch (err: any) {
      setError(err.message || "Failed to upload QR Code.");
    } finally {
      setUploadingQr(false);
    }
  }

  // Handle QR active status toggle
  async function handleToggleQr(qr: QrCodeRow) {
    if (!services) return;
    try {
      const qrRef = doc(services.db, "active_qr_codes", qr.id);
      await updateDoc(qrRef, {
        isActive: !qr.isActive
      });
      setMessage(`QR code status updated!`);
    } catch (err: any) {
      setError(err.message);
    }
  }

  // Handle QR delete
  async function handleDeleteQr(qr: QrCodeRow) {
    if (!services) return;
    if (!confirm("Are you sure you want to delete this QR code?")) return;
    try {
      const qrRef = doc(services.db, "active_qr_codes", qr.id);
      await deleteDoc(qrRef);
      setMessage("QR code deleted successfully.");
    } catch (err: any) {
      setError(err.message);
    }
  }

  // Handle Job Search
  async function handleSearchJobs() {
    if (!services) return;
    setSearchingJobs(true);
    setJobs([]);
    setError(null);

    try {
      const q = query(
        collection(services.db, "jobs"),
        where("employerPhone", "==", searchPhone.trim())
      );
      const snap = await getDocs(q);
      const rows = snap.docs.map((doc) => {
        const data = doc.data();
        return {
          id: doc.id,
          title: String(data.title || ""),
          companyName: String(data.companyName || ""),
          employerPhone: String(data.employerPhone || ""),
          employerId: String(data.employerId || ""),
          createdAt: data.createdAt,
          expiresAt: data.expiresAt
        };
      }) as JobRow[];

      setJobs(rows);
      if (rows.length === 0) {
        setMessage("No jobs found for this employer phone.");
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setSearchingJobs(false);
    }
  }

  // Handle Job Extension
  async function handleExtendJob(job: JobRow) {
    if (!services) return;
    const daysStr = extensionDays[job.id]?.trim();
    const days = parseInt(daysStr || "0");
    if (isNaN(days) || days <= 0) {
      alert("Please enter a valid number of days (> 0) to extend.");
      return;
    }

    try {
      setMessage(`Extending job "${job.title}" by ${days} days...`);
      setError(null);

      // Determine current expiry timestamp
      let currentExpiry = Date.now();
      if (job.expiresAt) {
        if (typeof job.expiresAt.toMillis === "function") {
          currentExpiry = job.expiresAt.toMillis();
        } else if (typeof job.expiresAt === "number") {
          currentExpiry = job.expiresAt;
        } else if (job.expiresAt.seconds) {
          currentExpiry = job.expiresAt.seconds * 1000;
        }
      }

      const newExpiry = currentExpiry + days * 24 * 60 * 60 * 1000;

      const jobRef = doc(services.db, "jobs", job.id);
      await updateDoc(jobRef, {
        expiresAt: Timestamp.fromMillis(newExpiry)
      });

      setMessage("Job extended successfully!");
      handleSearchJobs(); // Refresh jobs listing
    } catch (err: any) {
      setError(err.message);
    }
  }

  return (
    <div className="admin-payments-container">
      {/* Alert Banners */}
      {message && <div className="alert-banner info">{message}</div>}
      {error && <div className="alert-banner error">{error}</div>}

      {/* Tabs */}
      <div className="tab-header">
        <button
          className={`tab-btn ${activeTab === "requests" ? "active" : ""}`}
          onClick={() => { setActiveTab("requests"); setMessage(null); setError(null); }}
        >
          💳 Verification Panel
        </button>
        <button
          className={`tab-btn ${activeTab === "qrs" ? "active" : ""}`}
          onClick={() => { setActiveTab("qrs"); setMessage(null); setError(null); }}
        >
          📱 Active QR Codes
        </button>
        <button
          className={`tab-btn ${activeTab === "extensions" ? "active" : ""}`}
          onClick={() => { setActiveTab("extensions"); setMessage(null); setError(null); }}
        >
          🔄 Job Extensions
        </button>
      </div>

      {/* Tab Contents */}
      <div className="tab-body">
        {activeTab === "requests" && (
          <section className="admin-section">
            <h2 className="admin-section-title">Pending Payment Submissions</h2>
            {loadingRequests ? (
              <p>Loading transaction requests...</p>
            ) : requests.length === 0 ? (
              <p className="no-records">No payment requests submitted yet.</p>
            ) : (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Phone</th>
                    <th>Plan</th>
                    <th>Amount</th>
                    <th>UTR Number</th>
                    <th>Status</th>
                    <th>Screenshot</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((req) => (
                    <tr key={req.id}>
                      <td><strong>{req.employerPhone}</strong></td>
                      <td><code>{req.planId}</code></td>
                      <td>₹{req.amount}</td>
                      <td><code className="utr-code">{req.utrNumber}</code></td>
                      <td>
                        <span className={`status-badge ${req.status.toLowerCase()}`}>
                          {req.status}
                        </span>
                      </td>
                      <td>
                        {req.screenshotUrl ? (
                          <a
                            href={req.screenshotUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="screenshot-link"
                          >
                            🖼 View Proof
                          </a>
                        ) : (
                          <span className="no-screenshot">None</span>
                        )}
                      </td>
                      <td>
                        {req.status === "PENDING" ? (
                          <div className="action-row">
                            <button
                              onClick={() => handleApprove(req)}
                              className="btn btn-approve"
                            >
                              Approve
                            </button>
                            <div className="reject-group">
                              <input
                                type="text"
                                placeholder="Rejection reason..."
                                value={rejectReason[req.id] || ""}
                                onChange={(e) => setRejectReason({ ...rejectReason, [req.id]: e.target.value })}
                                className="reject-input"
                              />
                              <button
                                onClick={() => handleReject(req)}
                                className="btn btn-reject"
                              >
                                Reject
                              </button>
                            </div>
                          </div>
                        ) : (
                          <div className="processed-details">
                            {req.status === "VERIFIED" && (
                              <div className="verified-info">
                                <div>✅ Accepted: {req.verifiedTimestamp ? new Date(req.verifiedTimestamp).toLocaleString() : "N/A"}</div>
                                <div>📅 Expires: {req.expiryTimestamp ? new Date(req.expiryTimestamp).toLocaleDateString() : "N/A"}</div>
                              </div>
                            )}
                            {req.status === "REJECTED" && (
                              <div className="rejected-info">
                                <div>❌ Rejected: {req.verifiedTimestamp ? new Date(req.verifiedTimestamp).toLocaleString() : "N/A"}</div>
                                <div className="reject-reason">Reason: {req.rejectionReason || "None"}</div>
                              </div>
                            )}
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        )}

        {activeTab === "qrs" && (
          <section className="admin-section">
            <h2 className="admin-section-title">Scan QR Codes Registry</h2>
            
            {/* Form */}
            <form onSubmit={handleAddQr} className="admin-form mb-6">
              <h3>Upload Payment QR Code</h3>
              <div className="form-group-grid">
                <div className="form-field">
                  <label>Label</label>
                  <input
                    type="text"
                    placeholder="e.g. PhonePe QR Code"
                    value={qrLabel}
                    onChange={(e) => setQrLabel(e.target.value)}
                    required
                  />
                </div>
                <div className="form-field">
                  <label>QR Code Image</label>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => setQrFile(e.target.files?.[0] || null)}
                    required
                  />
                </div>
              </div>
              <button type="submit" disabled={uploadingQr} className="btn-save mt-3">
                {uploadingQr ? "Uploading..." : "Publish & Activate QR"}
              </button>
            </form>

            {/* List */}
            {qrs.length === 0 ? (
              <p>No QR codes uploaded yet.</p>
            ) : (
              <div className="qr-grid">
                {qrs.map((qr) => (
                  <div key={qr.id} className="qr-card">
                    <img src={qr.imageUrl} alt={qr.label} className="qr-preview-img" />
                    <h4>{qr.label}</h4>
                    <div className="qr-card-actions">
                      <button
                        onClick={() => handleToggleQr(qr)}
                        className={`btn ${qr.isActive ? "btn-active" : "btn-inactive"}`}
                      >
                        {qr.isActive ? "Active (On)" : "Inactive (Off)"}
                      </button>
                      <button
                        onClick={() => handleDeleteQr(qr)}
                        className="btn btn-delete"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {activeTab === "extensions" && (
          <section className="admin-section">
            <h2 className="admin-section-title">Manual Job Expiry Extension Panel</h2>
            <div className="search-bar mb-6">
              <input
                type="text"
                placeholder="Search Employer Phone..."
                value={searchPhone}
                onChange={(e) => setSearchPhone(e.target.value)}
                className="search-input"
              />
              <button
                onClick={handleSearchJobs}
                disabled={searchingJobs}
                className="btn btn-search"
              >
                {searchingJobs ? "Searching..." : "Search Jobs"}
              </button>
            </div>

            {jobs.length > 0 && (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Company</th>
                    <th>Expires At</th>
                    <th>Extend Expiry</th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((job) => {
                    const exp = job.expiresAt;
                    let displayExp = "N/A";
                    if (exp) {
                      if (typeof exp.toDate === "function") {
                        displayExp = exp.toDate().toLocaleDateString();
                      } else if (typeof exp === "number") {
                        displayExp = new Date(exp).toLocaleDateString();
                      } else if (exp.seconds) {
                        displayExp = new Date(exp.seconds * 1000).toLocaleDateString();
                      }
                    }
                    return (
                      <tr key={job.id}>
                        <td><strong>{job.title}</strong></td>
                        <td>{job.companyName}</td>
                        <td><code>{displayExp}</code></td>
                        <td>
                          <div className="extend-row">
                            <input
                              type="number"
                              placeholder="Days"
                              value={extensionDays[job.id] || ""}
                              onChange={(e) => setExtensionDays({ ...extensionDays, [job.id]: e.target.value })}
                              className="days-input"
                            />
                            <button
                              onClick={() => handleExtendJob(job)}
                              className="btn btn-extend"
                            >
                              Extend
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </section>
        )}
      </div>

      <style jsx>{`
        .admin-payments-container {
          padding: 1rem;
        }
        .tab-header {
          display: flex;
          border-bottom: 2px solid #e5e7eb;
          margin-bottom: 1.5rem;
          gap: 0.5rem;
        }
        .tab-btn {
          padding: 0.75rem 1.25rem;
          font-weight: 600;
          color: #4b5563;
          border: none;
          background: none;
          border-bottom: 3px solid transparent;
          cursor: pointer;
          transition: all 0.2s;
        }
        .tab-btn:hover {
          color: #8b5cf6;
        }
        .tab-btn.active {
          color: #8b5cf6;
          border-bottom-color: #8b5cf6;
        }
        .alert-banner {
          padding: 0.75rem 1rem;
          border-radius: 0.5rem;
          margin-bottom: 1rem;
          font-weight: 500;
        }
        .alert-banner.info {
          background-color: #f3e8ff;
          color: #6b21a8;
          border: 1px solid #d8b4fe;
        }
        .alert-banner.error {
          background-color: #fee2e2;
          color: #991b1b;
          border: 1px solid #fca5a5;
        }
        .admin-table {
          width: 100%;
          border-collapse: collapse;
          margin-top: 1rem;
        }
        .admin-table th, .admin-table td {
          padding: 0.75rem 1rem;
          text-align: left;
          border-bottom: 1px solid #e5e7eb;
        }
        .admin-table th {
          background-color: #f9fafb;
          color: #374151;
          font-weight: 600;
        }
        .utr-code {
          background-color: #f3f4f6;
          padding: 0.2rem 0.4rem;
          border-radius: 0.25rem;
          font-family: monospace;
        }
        .status-badge {
          padding: 0.25rem 0.5rem;
          border-radius: 9999px;
          font-size: 0.75rem;
          font-weight: 600;
          text-transform: uppercase;
        }
        .status-badge.pending {
          background-color: #fef3c7;
          color: #d97706;
        }
        .status-badge.verified {
          background-color: #d1fae5;
          color: #059669;
        }
        .status-badge.rejected {
          background-color: #fee2e2;
          color: #dc2626;
        }
        .screenshot-link {
          color: #8b5cf6;
          text-decoration: underline;
          font-weight: 500;
        }
        .action-row {
          display: flex;
          align-items: center;
          gap: 1rem;
        }
        .reject-group {
          display: flex;
          align-items: center;
          gap: 0.25rem;
        }
        .reject-input {
          padding: 0.25rem 0.5rem;
          border: 1px solid #d1d5db;
          border-radius: 0.375rem;
          font-size: 0.875rem;
        }
        .btn {
          padding: 0.4rem 0.8rem;
          font-weight: 600;
          border-radius: 0.375rem;
          border: none;
          cursor: pointer;
          font-size: 0.875rem;
        }
        .btn-approve {
          background-color: #10b981;
          color: white;
        }
        .btn-reject {
          background-color: #ef4444;
          color: white;
        }
        .btn-search {
          background-color: #8b5cf6;
          color: white;
        }
        .btn-save {
          background-color: #8b5cf6;
          color: white;
          padding: 0.5rem 1rem;
          font-weight: bold;
          border-radius: 0.375rem;
          border: none;
          cursor: pointer;
        }
        .qr-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
          gap: 1.5rem;
          margin-top: 1.5rem;
        }
        .qr-card {
          border: 1px solid #e5e7eb;
          border-radius: 0.75rem;
          padding: 1rem;
          background: white;
          text-align: center;
        }
        .qr-preview-img {
          max-width: 100%;
          height: 150px;
          object-fit: contain;
          margin-bottom: 0.75rem;
          border-radius: 0.375rem;
        }
        .qr-card-actions {
          display: flex;
          justify-content: space-around;
          margin-top: 0.75rem;
        }
        .btn-active {
          background-color: #d1fae5;
          color: #065f46;
        }
        .btn-inactive {
          background-color: #fee2e2;
          color: #991b1b;
        }
        .btn-delete {
          background-color: #f3f4f6;
          color: #374151;
          border: 1px solid #d1d5db;
        }
        .search-bar {
          display: flex;
          gap: 0.5rem;
        }
        .search-input {
          flex: 1;
          padding: 0.5rem 1rem;
          border: 1px solid #d1d5db;
          border-radius: 0.375rem;
        }
        .extend-row {
          display: flex;
          gap: 0.25rem;
        }
        .days-input {
          width: 80px;
          padding: 0.25rem 0.5rem;
          border: 1px solid #d1d5db;
          border-radius: 0.375rem;
        }
        .btn-extend {
          background-color: #3b82f6;
          color: white;
        }
        .mb-6 {
          margin-bottom: 1.5rem;
        }
        .mt-3 {
          margin-top: 0.75rem;
        }
        .form-group-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 1rem;
        }
        .form-field {
          display: flex;
          flex-direction: column;
          gap: 0.25rem;
        }
        .form-field label {
          font-weight: 600;
          color: #4b5563;
        }
        .form-field input {
          padding: 0.5rem;
          border: 1px solid #d1d5db;
          border-radius: 0.375rem;
        }
        .admin-form {
          background: #f9fafb;
          padding: 1.25rem;
          border-radius: 0.75rem;
          border: 1px solid #e5e7eb;
        }
        .processed-details {
          font-size: 0.75rem;
          line-height: 1.25rem;
        }
        .verified-info {
          color: #047857;
          font-weight: 500;
        }
        .rejected-info {
          color: #b91c1c;
          font-weight: 500;
        }
        .reject-reason {
          font-style: italic;
          color: #4b5563;
        }
      `}</style>
    </div>
  );
}
