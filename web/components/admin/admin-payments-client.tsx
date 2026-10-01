"use client";

import { useEffect, useMemo, useState } from "react";
import {
  collection,
  doc,
  limit,
  query,
  updateDoc,
  setDoc,
  deleteDoc,
  onSnapshot,
  serverTimestamp,
  Timestamp,
  orderBy
} from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { getStorage, ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { getFirebaseServices } from "@/lib/firebase/client";
import { PaymentQrCodes, SubscriptionPayments } from "@/lib/firebase/schema";

const millis = (v: unknown) => (v instanceof Timestamp ? v.toMillis() : typeof v === "number" ? v : 0);

type PaymentRequestRow = {
  id: string;
  employerId: string;
  planId: string;
  amountPaise: number;
  upiIdUsed: string;
  utrNumber: string;
  screenshotUrl: string;
  status: string;
  createdAt: number;
  verifiedAt: number;
  rejectionReason: string;
};

type QrCodeRow = {
  id: string;
  imageUrl: string;
  label: string;
  isActive: boolean;
  createdAt: number;
};

export function AdminPaymentsClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [activeTab, setActiveTab] = useState<"requests" | "qrs">("requests");

  // Requests State
  const [requests, setRequests] = useState<PaymentRequestRow[]>([]);
  const [loadingRequests, setLoadingRequests] = useState(true);
  const [rejectReason, setRejectReason] = useState<{ [id: string]: string }>({});

  // QRs State
  const [qrs, setQrs] = useState<QrCodeRow[]>([]);
  const [qrLabel, setQrLabel] = useState("");
  const [qrFile, setQrFile] = useState<File | null>(null);
  const [uploadingQr, setUploadingQr] = useState(false);

  // Global messages
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Listen for requests
  useEffect(() => {
    if (!services) return;
    const S = SubscriptionPayments;
    const q = query(collection(services.db, S.COLLECTION), orderBy(S.CREATED_AT, "desc"), limit(100));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const rows: PaymentRequestRow[] = snapshot.docs.map((d) => {
        const data = d.data();
        return {
          id: d.id,
          employerId: String(data[S.EMPLOYER_ID] ?? ""),
          planId: String(data[S.PLAN_ID] ?? ""),
          amountPaise: Number(data[S.AMOUNT_PAISE] ?? 0),
          upiIdUsed: String(data[S.UPI_ID_USED] ?? ""),
          utrNumber: String(data[S.UTR_NUMBER] ?? ""),
          screenshotUrl: String(data[S.SCREENSHOT_URL] ?? ""),
          status: String(data[S.STATUS] ?? "PENDING"),
          createdAt: millis(data[S.CREATED_AT]),
          verifiedAt: millis(data[S.VERIFIED_AT]),
          rejectionReason: String(data[S.REJECTION_REASON] ?? "")
        };
      });
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
    const q = query(collection(services.db, PaymentQrCodes.COLLECTION), orderBy(PaymentQrCodes.CREATED_AT, "desc"));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const rows: QrCodeRow[] = snapshot.docs.map((d) => {
        const data = d.data();
        return {
          id: d.id,
          imageUrl: String(data[PaymentQrCodes.IMAGE_URL] ?? ""),
          label: String(data[PaymentQrCodes.LABEL] ?? ""),
          isActive: data[PaymentQrCodes.ACTIVE] === true,
          createdAt: millis(data[PaymentQrCodes.CREATED_AT])
        };
      });
      setQrs(rows);
    });
    return () => unsubscribe();
  }, [services]);

  /** Approve/reject runs on the server (verifySubscriptionPayment): it grants the plan's credits atomically. */
  async function decide(req: PaymentRequestRow, approve: boolean, reason = "") {
    if (!services) return;
    setMessage(approve ? "Activating the plan..." : "Rejecting...");
    setError(null);
    try {
      await httpsCallable(services.functions, "verifySubscriptionPayment")({ requestId: req.id, approve, reason });
      setMessage(approve ? "Plan activated and payment verified." : "Payment request rejected.");
      if (!approve) setRejectReason((prev) => ({ ...prev, [req.id]: "" }));
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to update the payment request.");
    }
  }

  async function handleApprove(req: PaymentRequestRow) {
    if (!confirm(`Approve UTR ${req.utrNumber} and activate ${req.planId}?`)) return;
    await decide(req, true);
  }

  async function handleReject(req: PaymentRequestRow) {
    const reason = rejectReason[req.id]?.trim();
    if (!reason) {
      alert("Please provide a rejection reason first.");
      return;
    }
    if (!confirm(`Reject UTR ${req.utrNumber}?`)) return;
    await decide(req, false, reason);
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

      const newQrRef = doc(collection(services.db, PaymentQrCodes.COLLECTION));
      await setDoc(newQrRef, {
        [PaymentQrCodes.IMAGE_URL]: downloadUrl,
        [PaymentQrCodes.LABEL]: qrLabel.trim(),
        [PaymentQrCodes.ACTIVE]: true,
        [PaymentQrCodes.CREATED_AT]: serverTimestamp()
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
      const qrRef = doc(services.db, PaymentQrCodes.COLLECTION, qr.id);
      await updateDoc(qrRef, { [PaymentQrCodes.ACTIVE]: !qr.isActive });
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
      await deleteDoc(doc(services.db, PaymentQrCodes.COLLECTION, qr.id));
      setMessage("QR code deleted successfully.");
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
                    <th>Employer</th>
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
                      <td><code>{req.employerId}</code><div>{req.createdAt ? new Date(req.createdAt).toLocaleString() : ""}</div></td>
                      <td><code>{req.planId}</code></td>
                      <td>₹{(req.amountPaise / 100).toLocaleString("en-IN")}</td>
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
                                <div>✅ Accepted: {req.verifiedAt ? new Date(req.verifiedAt).toLocaleString() : "N/A"}</div>
                              </div>
                            )}
                            {req.status === "REJECTED" && (
                              <div className="rejected-info">
                                <div>❌ Rejected: {req.verifiedAt ? new Date(req.verifiedAt).toLocaleString() : "N/A"}</div>
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
