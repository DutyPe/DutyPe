"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { httpsCallable } from "firebase/functions";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { getFirebaseServices } from "@/lib/firebase/client";

type WithdrawalRow = {
  id: string;
  uid: string;
  user: string;
  amountPaise: number;
  upiId: string;
  status: string;
  txnRef: string;
  failureReason: string;
  createdAt: string | null;
  processedAt: string | null;
};

type ReferralRow = {
  id: string;
  referee: string;
  referrer: string;
  code: string;
  status: string;
  fraudScore: number;
  createdAt: string | null;
  completedAt: string | null;
};

type Lookup = {
  uid: string;
  name: string;
  wallet: null | {
    referralCode: string; balancePaise: number; lifetimeEarnedPaise: number; withdrawnPaise: number;
    successfulReferrals: number; blocked: boolean;
  };
  ledger: Array<{ id: string; type: string; amountPaise: number; balanceAfterPaise: number; createdAt: string | null }>;
  referredBy: null | { referrerUid: string; code: string; status: string };
  referrals: Array<{ refereeUid: string; refereeName?: string; status: string; createdAt: string | null }>;
  withdrawals: Array<{ id: string; amountPaise: number; status: string; createdAt: string | null }>;
};

const rupees = (paise: number) => `₹${(Number(paise || 0) / 100).toLocaleString("en-IN")}`;
const when = (value: string | null) => (value ? new Date(value).toLocaleString("en-IN") : "—");

/** Withdrawals (settle via the settleWithdrawal Cloud Function), referrals, and a per-user wallet lookup. */
export function AdminReferralsClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [tab, setTab] = useState<"withdrawals" | "referrals" | "lookup">("withdrawals");
  const [status, setStatus] = useState("PENDING");
  const [withdrawals, setWithdrawals] = useState<WithdrawalRow[]>([]);
  const [referrals, setReferrals] = useState<ReferralRow[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [lookupQuery, setLookupQuery] = useState("");
  const [lookup, setLookup] = useState<Lookup | null>(null);
  const [loading, setLoading] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  // Manual Credit Modal States
  const [creditModalOpen, setCreditModalOpen] = useState(false);
  const [creditTarget, setCreditTarget] = useState("");
  const [creditAmount, setCreditAmount] = useState("");
  const [creditReason, setCreditReason] = useState("Bug bounty reward");
  const [creditSubmitting, setCreditSubmitting] = useState(false);

  async function handleManualCredit(e: React.FormEvent) {
    e.preventDefault();
    if (!creditTarget.trim() || !creditAmount.trim()) {
      setError("Please fill in user phone/UID and amount.");
      return;
    }
    const amt = parseFloat(creditAmount);
    if (isNaN(amt) || amt <= 0) {
      setError("Enter a valid positive amount in Rupees.");
      return;
    }
    setCreditSubmitting(true);
    setError(null);
    try {
      const res = await adminApiFetch("/api/admin/referrals/credit", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          uidOrPhone: creditTarget.trim(),
          amountRupees: amt,
          reason: creditReason.trim()
        })
      });
      const data = (await res.json()) as { error?: string; uid?: string };
      if (!res.ok) throw new Error(data.error || "Failed to credit user.");
      setMessage(`Successfully credited ₹${amt} to ${creditTarget.trim()}.`);
      setCreditModalOpen(false);
      setCreditAmount("");
      if (tab === "lookup" && lookupQuery.trim()) {
        void runLookup();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to credit user.");
    } finally {
      setCreditSubmitting(false);
    }
  }

  const load = useCallback(async (append: boolean, cursor: string | null) => {
    if (tab === "lookup") return;
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ view: tab, t: String(Date.now()) });
      if (tab === "withdrawals" && status) params.set("status", status);
      if (cursor) params.set("after", cursor);
      const response = await adminApiFetch(`/api/admin/referrals?${params}`, { cache: "no-store" });
      const payload = (await response.json()) as { withdrawals?: WithdrawalRow[]; referrals?: ReferralRow[]; nextCursor?: string | null; error?: string };
      if (!response.ok) throw new Error(payload.error || "Failed to load.");
      if (tab === "withdrawals") setWithdrawals((c) => (append ? [...c, ...(payload.withdrawals ?? [])] : payload.withdrawals ?? []));
      else setReferrals((c) => (append ? [...c, ...(payload.referrals ?? [])] : payload.referrals ?? []));
      setNextCursor(payload.nextCursor ?? null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load.");
    } finally {
      setLoading(false);
    }
  }, [tab, status]);

  useEffect(() => { void load(false, null); }, [load]);

  async function settle(row: WithdrawalRow, next: "PROCESSING" | "COMPLETED" | "FAILED") {
    if (!services) return;
    let txnRef = "";
    let failureReason = "";
    if (next === "COMPLETED") {
      txnRef = window.prompt(`UPI transaction reference for ${rupees(row.amountPaise)} to ${row.upiId}`)?.trim() ?? "";
      if (!txnRef) return;
    }
    if (next === "FAILED") {
      failureReason = window.prompt("Why did it fail? (the amount goes back to the wallet)")?.trim() ?? "";
      if (!failureReason) return;
    }
    setBusyId(row.id);
    setError(null);
    try {
      await httpsCallable(services.functions, "settleWithdrawal")({ withdrawalId: row.id, status: next, txnRef, failureReason });
      setMessage(`Withdrawal ${row.id} marked ${next.toLowerCase()}.`);
      await load(false, null);
    } catch (settleError) {
      setError(settleError instanceof Error ? settleError.message : "Failed to update the withdrawal.");
    } finally {
      setBusyId(null);
    }
  }

  async function runLookup() {
    const query = lookupQuery.trim();
    if (!query) return;
    setLoading(true);
    setError(null);
    setLookup(null);
    try {
      const response = await adminApiFetch(`/api/admin/referrals?lookup=${encodeURIComponent(query)}&t=${Date.now()}`, { cache: "no-store" });
      const payload = (await response.json()) as Lookup & { error?: string };
      if (!response.ok) throw new Error(payload.error || "Lookup failed.");
      setLookup(payload);
    } catch (lookupError) {
      setError(lookupError instanceof Error ? lookupError.message : "Lookup failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="admin-section-stack">
      {message ? <div className="callout" role="status">{message}</div> : null}
      {error ? <div className="admin-error">{error}</div> : null}

      <div className="admin-toolbar">
        {(["withdrawals", "referrals", "lookup"] as const).map((t) => (
          <button key={t} type="button" className={`button ${tab === t ? "" : "ghost"}`} onClick={() => { setTab(t); setMessage(null); }}>
            {t === "withdrawals" ? "Withdrawals" : t === "referrals" ? "Referrals" : "Wallet lookup"}
          </button>
        ))}
        {tab === "withdrawals" ? (
          <select className="admin-filter" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="PENDING">Pending</option>
            <option value="PROCESSING">Processing</option>
            <option value="COMPLETED">Completed</option>
            <option value="FAILED">Failed</option>
            <option value="">All</option>
          </select>
        ) : null}
        <div style={{ marginLeft: "auto" }}>
          <button
            type="button"
            className="button"
            style={{ background: "#059669", color: "white" }}
            onClick={() => {
              setCreditTarget(lookup?.uid || "");
              setCreditModalOpen(true);
            }}
          >
            + Credit Money / Reward
          </button>
        </div>
      </div>

      {tab === "withdrawals" ? (
        <div className="table-wrap">
          <table className="data-table admin-table">
            <thead><tr><th>Requested</th><th>User</th><th>Amount</th><th>UPI</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {withdrawals.map((w) => (
                <tr key={w.id}>
                  <td>{when(w.createdAt)}</td>
                  <td>{w.user}<div className="admin-cell-sub"><code className="admin-code">{w.uid}</code></div></td>
                  <td><strong>{rupees(w.amountPaise)}</strong></td>
                  <td><code className="admin-code">{w.upiId}</code></td>
                  <td>
                    <span className={`status-pill ${w.status === "COMPLETED" ? "success" : w.status === "FAILED" ? "danger" : "warning"}`}>{w.status}</span>
                    {w.txnRef ? <div className="admin-cell-sub">Ref {w.txnRef}</div> : null}
                    {w.failureReason ? <div className="admin-cell-sub danger-text">{w.failureReason}</div> : null}
                  </td>
                  <td>
                    {w.status === "PENDING" || w.status === "PROCESSING" ? (
                      <>
                        {w.status === "PENDING" ? (
                          <button type="button" className="table-action" disabled={busyId === w.id} onClick={() => void settle(w, "PROCESSING")}>Processing</button>
                        ) : null}
                        <button type="button" className="table-action" disabled={busyId === w.id} onClick={() => void settle(w, "COMPLETED")}>Paid</button>
                        <button type="button" className="table-action danger" disabled={busyId === w.id} onClick={() => void settle(w, "FAILED")}>Failed</button>
                      </>
                    ) : <span className="admin-cell-sub">{when(w.processedAt)}</span>}
                  </td>
                </tr>
              ))}
              {!withdrawals.length && !loading ? <tr><td colSpan={6} className="admin-empty">No withdrawals.</td></tr> : null}
            </tbody>
          </table>
        </div>
      ) : null}

      {tab === "referrals" ? (
        <div className="table-wrap">
          <table className="data-table admin-table">
            <thead><tr><th>Joined</th><th>New user</th><th>Referred by</th><th>Code</th><th>Status</th><th>Fraud score</th></tr></thead>
            <tbody>
              {referrals.map((r) => (
                <tr key={r.id}>
                  <td>{when(r.createdAt)}</td>
                  <td>{r.referee}</td>
                  <td>{r.referrer}</td>
                  <td><code className="admin-code">{r.code}</code></td>
                  <td><span className={`status-pill ${r.status === "COMPLETED" ? "success" : r.status === "PENDING" ? "warning" : "danger"}`}>{r.status}</span></td>
                  <td>{r.fraudScore}</td>
                </tr>
              ))}
              {!referrals.length && !loading ? <tr><td colSpan={6} className="admin-empty">No referrals.</td></tr> : null}
            </tbody>
          </table>
        </div>
      ) : null}

      {tab !== "lookup" && nextCursor ? (
        <div className="admin-toolbar">
          <button type="button" className="button ghost" disabled={loading} onClick={() => void load(true, nextCursor)}>{loading ? "Loading…" : "Load more"}</button>
        </div>
      ) : null}

      {tab === "lookup" ? (
        <section className="section">
          <div className="admin-toolbar">
            <input className="admin-search" placeholder="Referral code, mobile number or uid" value={lookupQuery}
              onChange={(e) => setLookupQuery(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter") void runLookup(); }} />
            <button type="button" className="button" disabled={loading} onClick={() => void runLookup()}>{loading ? "Searching…" : "Look up"}</button>
          </div>
          {lookup ? (
            <div className="admin-section-stack">
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <h3>{lookup.name || lookup.uid}</h3>
                <button
                  type="button"
                  className="button"
                  style={{ background: "#059669", color: "white", padding: "6px 14px", fontSize: "0.85rem" }}
                  onClick={() => {
                    setCreditTarget(lookup.uid);
                    setCreditModalOpen(true);
                  }}
                >
                  + Credit This User
                </button>
              </div>
              {lookup.wallet ? (
                <div className="admin-stats-grid small">
                  <div className="admin-stat-card compact"><strong>{rupees(lookup.wallet.balancePaise)}</strong><span>Balance</span></div>
                  <div className="admin-stat-card compact"><strong>{rupees(lookup.wallet.lifetimeEarnedPaise)}</strong><span>Earned</span></div>
                  <div className="admin-stat-card compact"><strong>{rupees(lookup.wallet.withdrawnPaise)}</strong><span>Withdrawn</span></div>
                  <div className="admin-stat-card compact"><strong>{lookup.wallet.successfulReferrals}</strong><span>Successful referrals</span></div>
                  <div className="admin-stat-card compact"><strong>{lookup.wallet.referralCode || "—"}</strong><span>{lookup.wallet.blocked ? "Code (blocked)" : "Code"}</span></div>
                </div>
              ) : <p>No wallet yet.</p>}
              {lookup.referredBy ? <p>Joined with code <code>{lookup.referredBy.code}</code> ({lookup.referredBy.status}).</p> : null}
              <h4>Money history</h4>
              <table className="data-table admin-table">
                <thead><tr><th>When</th><th>Type</th><th>Amount</th><th>Balance after</th></tr></thead>
                <tbody>
                  {lookup.ledger.map((l) => (
                    <tr key={l.id}><td>{when(l.createdAt)}</td><td>{l.type}</td><td>{rupees(l.amountPaise)}</td><td>{rupees(l.balanceAfterPaise)}</td></tr>
                  ))}
                </tbody>
              </table>
              <h4>People they referred ({lookup.referrals.length})</h4>
              <ul style={{ paddingLeft: "1.2rem", display: "flex", flexDirection: "column", gap: "6px" }}>
                {lookup.referrals.map((r) => (
                  <li key={r.refereeUid}>
                    <strong>{r.refereeName || r.refereeUid}</strong> —{" "}
                    <span className={`status-pill ${r.status === "COMPLETED" ? "success" : r.status === "PENDING" ? "warning" : "danger"}`} style={{ fontSize: "0.75rem", padding: "1px 6px" }}>
                      {r.status}
                    </span>{" "}
                    · {when(r.createdAt)}
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
        </section>
      ) : null}

      {creditModalOpen ? (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0, 0, 0, 0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            padding: "1rem"
          }}
          onClick={() => setCreditModalOpen(false)}
        >
          <div
            style={{
              backgroundColor: "white",
              borderRadius: "12px",
              padding: "1.75rem",
              width: "100%",
              maxWidth: "460px",
              boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)"
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ margin: "0 0 0.5rem 0", fontSize: "1.25rem", color: "#0F172A" }}>Manual Wallet Credit / Reward</h3>
            <p style={{ margin: "0 0 1.25rem 0", fontSize: "0.85rem", color: "#64748B" }}>
              Credit money directly to a user&apos;s wallet for bug bounties, rewards, or compensation.
            </p>
            <form onSubmit={handleManualCredit} style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
              <div>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, color: "#334155", marginBottom: "0.25rem" }}>
                  User Mobile, Referral Code, or UID
                </label>
                <input
                  type="text"
                  required
                  placeholder="+919876543210, DUTY123, or UID"
                  className="admin-input"
                  value={creditTarget}
                  onChange={(e) => setCreditTarget(e.target.value)}
                  style={{ width: "100%", padding: "0.6rem 0.75rem", border: "1px solid #CBD5E1", borderRadius: "6px" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, color: "#334155", marginBottom: "0.25rem" }}>
                  Amount to Credit (₹ Rupees)
                </label>
                <input
                  type="number"
                  min="1"
                  step="1"
                  required
                  placeholder="e.g. 50"
                  className="admin-input"
                  value={creditAmount}
                  onChange={(e) => setCreditAmount(e.target.value)}
                  style={{ width: "100%", padding: "0.6rem 0.75rem", border: "1px solid #CBD5E1", borderRadius: "6px" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, color: "#334155", marginBottom: "0.25rem" }}>
                  Reward Reason / Note
                </label>
                <select
                  value={creditReason}
                  onChange={(e) => setCreditReason(e.target.value)}
                  style={{ width: "100%", padding: "0.6rem 0.75rem", border: "1px solid #CBD5E1", borderRadius: "6px", backgroundColor: "white" }}
                >
                  <option value="Bug bounty reward">Bug bounty reward</option>
                  <option value="Special contest reward">Special contest reward</option>
                  <option value="App issue compensation">App issue compensation</option>
                  <option value="Promotional bonus credit">Promotional bonus credit</option>
                  <option value="Support adjustment">Support adjustment</option>
                </select>
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.75rem", marginTop: "1rem" }}>
                <button
                  type="button"
                  className="button ghost"
                  onClick={() => setCreditModalOpen(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="button"
                  disabled={creditSubmitting}
                  style={{ backgroundColor: "#059669", color: "white" }}
                >
                  {creditSubmitting ? "Crediting..." : "Confirm & Credit"}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  );
}
