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
  referrals: Array<{ refereeUid: string; status: string; createdAt: string | null }>;
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
              <h3>{lookup.name || lookup.uid}</h3>
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
              <ul>{lookup.referrals.map((r) => <li key={r.refereeUid}><code>{r.refereeUid}</code> — {r.status} · {when(r.createdAt)}</li>)}</ul>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
