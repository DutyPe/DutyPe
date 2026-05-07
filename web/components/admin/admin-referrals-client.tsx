"use client";

import { useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatCurrency, formatDate, formatDateTime } from "@/lib/firebase/firestore-helpers";

type ReferralRow = {
  id: string;
  referralCode?: string;
  referredUserName?: string;
  referredUserPhone?: string;
  referredUserRole?: string;
  referredUserId?: string;
  referrerUserName?: string;
  referrerPhone?: string;
  referrerRole?: string;
  referrerId?: string;
  referrerReferralCode?: string;
  referredByCode?: string;
  currentReferralCode?: string;
  status?: string;
  rewardAmount?: number | string;
  bonusAmount?: number | string;
  referredUserReward?: number | string;
  createdAt?: unknown;
};

type WithdrawalRow = {
  id: string;
  userId?: string;
  userName?: string;
  userRole?: string;
  phone?: string;
  referralCode?: string;
  amount?: number | string;
  availableBalance?: number | string;
  totalEarnings?: number | string;
  withdrawnAmount?: number | string;
  paymentMethod?: string;
  upiId?: string;
  bankAccountNumber?: string;
  ifscCode?: string;
  accountHolderName?: string;
  status?: string;
  transactionId?: string;
  createdAt?: unknown;
  processedAt?: unknown;
};

type ReferralLookupResult = {
  query?: string;
  found?: boolean;
  message?: string;
  resolvedBy?: string;
  userId?: string;
  referralCode?: string;
  identity?: {
    userName?: string;
    phone?: string;
    role?: string;
    referralCode?: string;
  };
  referralStats?: Record<string, unknown> | null;
  referralCodes?: Array<Record<string, unknown> & { id?: string; code?: string }>;
  referralsAsReferrer?: ReferralRow[];
  referralsAsReferred?: ReferralRow[];
  withdrawals?: WithdrawalRow[];
  auditLogs?: Array<Record<string, unknown> & { id?: string; eventType?: string; timestamp?: unknown }>;
};

export function AdminReferralsClient() {
  const [referrals, setReferrals] = useState<ReferralRow[]>([]);
  const [withdrawals, setWithdrawals] = useState<WithdrawalRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingActionId, setPendingActionId] = useState<string | null>(null);
  const [lookupQuery, setLookupQuery] = useState("");
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupResult, setLookupResult] = useState<ReferralLookupResult | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);

  async function loadData() {
    try {
      setLoading(true);
      const response = await adminApiFetch("/api/admin/referrals", {
        cache: "no-store"
      });

      const payload = (await response.json()) as {
        referrals?: ReferralRow[];
        withdrawals?: WithdrawalRow[];
        error?: string;
      };

      if (!response.ok) {
        throw new Error(payload.error || "Failed to load referrals.");
      }

      setReferrals(payload.referrals ?? []);
      setWithdrawals(payload.withdrawals ?? []);
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load referrals.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  async function handleLookup() {
    const query = lookupQuery.trim();
    if (!query) {
      setLookupError("Enter a referral code, phone number, or uid.");
      setLookupResult(null);
      return;
    }

    try {
      setLookupLoading(true);
      setLookupError(null);
      const response = await adminApiFetch(`/api/admin/referrals?lookup=${encodeURIComponent(query)}`, {
        cache: "no-store"
      });
      const payload = (await response.json()) as { lookup?: ReferralLookupResult; error?: string };

      if (!response.ok) {
        throw new Error(payload.error || "Failed to load user referral data.");
      }

      setLookupResult(payload.lookup ?? null);
      if (payload.lookup?.found === false) {
        setLookupError(payload.lookup.message || "No matching referral user found.");
      }
    } catch (lookupFailure) {
      setLookupError(lookupFailure instanceof Error ? lookupFailure.message : "Failed to load user referral data.");
      setLookupResult(null);
    } finally {
      setLookupLoading(false);
    }
  }

  async function handleWithdrawalUpdate(
    id: string,
    userId: string | undefined,
    status: "PROCESSING" | "COMPLETED" | "FAILED"
  ) {
    if (!userId) {
      setError("Missing userId for this withdrawal.");
      return;
    }

    let transactionId = "";
    let adminNote = "";
    const confirmationMessage = status === "PROCESSING"
      ? "Approve this withdrawal and move it to payment processing?"
      : status === "COMPLETED"
        ? "Mark this withdrawal as paid after payment is done?"
        : "Reject this withdrawal and refund the amount to available balance?";

    if (status === "COMPLETED") {
      transactionId = window.prompt("Payment transaction ID or note (optional):")?.trim() ?? "";
    }

    if (status === "FAILED") {
      adminNote = window.prompt("Reject reason (optional):")?.trim() ?? "";
    }

    if (!window.confirm(confirmationMessage)) {
      return;
    }

    try {
      setPendingActionId(id);
      const response = await adminApiFetch("/api/admin/referrals", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          withdrawalId: id,
          userId,
          status,
          transactionId,
          adminNote
        })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) {
        throw new Error(payload.error || "Failed to update withdrawal.");
      }

      await loadData();
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : "Failed to update withdrawal.");
    } finally {
      setPendingActionId(null);
    }
  }

  const completedReferralCount = referrals.filter((referral) => referral.status === "COMPLETED").length;
  const pendingWithdrawalCount = withdrawals.filter((withdrawal) => withdrawal.status === "PENDING").length;
  const processingWithdrawalCount = withdrawals.filter((withdrawal) => withdrawal.status === "PROCESSING").length;
  const completedWithdrawalCount = withdrawals.filter(
    (withdrawal) => withdrawal.status === "COMPLETED"
  ).length;
  const pendingWithdrawalAmount = withdrawals
    .filter((withdrawal) => withdrawal.status === "PENDING" || withdrawal.status === "PROCESSING")
    .reduce((total, withdrawal) => total + Number(withdrawal.amount ?? 0), 0);

  return (
    <div className="admin-section-stack">
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Referral operations</span>
            <h2>Live referral and payout summary</h2>
          </div>
          <p>
            This route replaces the old referrals page and keeps the referral queue plus
            withdrawal processing in one React workspace.
          </p>
        </div>

        {loading ? <div className="empty-state">Loading referrals and withdrawals from Firestore.</div> : null}
        {!loading && error ? <div className="empty-state">Unable to load referral operations. {error}</div> : null}
        {!loading && !error ? (
          <div className="metric-cluster">
            <div className="metric">
              <strong>{referrals.length}</strong>
              <span>Total referrals in the current sample.</span>
            </div>
            <div className="metric">
              <strong>{completedReferralCount}</strong>
              <span>Completed referrals credited by the current data.</span>
            </div>
            <div className="metric">
              <strong>{withdrawals.length}</strong>
              <span>Total withdrawal requests loaded.</span>
            </div>
            <div className="metric">
              <strong>{pendingWithdrawalCount}</strong>
              <span>Withdrawal requests waiting for approval.</span>
            </div>
            <div className="metric">
              <strong>{processingWithdrawalCount}</strong>
              <span>Approved withdrawals waiting for payment completion.</span>
            </div>
            <div className="metric">
              <strong>{completedWithdrawalCount}</strong>
              <span>Withdrawals already marked paid.</span>
            </div>
            <div className="metric">
              <strong>{formatCurrency(pendingWithdrawalAmount)}</strong>
              <span>Pending plus processing payout amount.</span>
            </div>
          </div>
        ) : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">User lookup</span>
            <h2>Check user referral data</h2>
          </div>
          <p>Enter a referral code, phone number, or uid to inspect that user's referral wallet, history, withdrawals, and audit records.</p>
        </div>

        <div className="admin-tool-form card">
          <label className="field-label" htmlFor="referral-user-lookup">
            Referral code, phone, or uid
          </label>
          <div className="admin-tool-row">
            <input
              id="referral-user-lookup"
              className="text-input"
              value={lookupQuery}
              onChange={(event) => setLookupQuery(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") void handleLookup();
              }}
              placeholder="Code, +91 phone, or Firebase uid"
            />
            <button type="button" className="button" onClick={() => void handleLookup()} disabled={lookupLoading}>
              {lookupLoading ? "Checking..." : "Check user referral data"}
            </button>
          </div>
          {lookupError ? <p className="tool-status tone-error">{lookupError}</p> : null}
        </div>

        {lookupResult?.found ? <ReferralLookupPanel lookup={lookupResult} /> : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Referrals</span>
            <h2>Who referred whom</h2>
          </div>
          <p>
            Shows each completed referral with referrer, referred user, current code,
            base reward, milestone bonus, and signup bonus.
          </p>
        </div>

        {!loading && !error && referrals.length === 0 ? (
          <div className="empty-state">No referrals were returned from Firestore.</div>
        ) : null}

        {!loading && !error && referrals.length > 0 ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Referral Code</th>
                  <th>Referrer</th>
                  <th>Referred User</th>
                  <th>Status</th>
                  <th>Base Reward</th>
                  <th>Milestone</th>
                  <th>Friend Bonus</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {referrals.map((referral) => (
                  <tr key={referral.id}>
                    <td>{referral.referralCode ?? "N/A"}</td>
                    <td>
                      <strong>{referral.referrerUserName || shortId(referral.referrerId)}</strong>
                      <div className="route-note">{referral.referrerRole || "Role missing"}</div>
                      <div className="route-note">{referral.referrerPhone || "Phone missing"}</div>
                      <div className="route-note">Own code: {referral.referrerReferralCode || "missing"}</div>
                    </td>
                    <td>
                      <strong>{referral.referredUserName || shortId(referral.referredUserId)}</strong>
                      <div className="route-note">{referral.referredUserRole || "Role missing"}</div>
                      <div className="route-note">{referral.referredUserPhone || "Phone missing"}</div>
                      <div className="route-note">Used: {referral.referredByCode || "missing"}</div>
                      <div className="route-note">Current code: {referral.currentReferralCode || "missing"}</div>
                    </td>
                    <td>
                      <span className={`status-pill ${statusTone(referral.status)}`}>
                        {referral.status ?? "PENDING"}
                      </span>
                    </td>
                    <td>{formatCurrency(referral.rewardAmount)}</td>
                    <td>{formatCurrency(referral.bonusAmount)}</td>
                    <td>{formatCurrency(referral.referredUserReward)}</td>
                    <td>{formatDate(referral.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Withdrawals</span>
            <h2>Payout processing queue</h2>
          </div>
          <p>
            Admin actions here update the canonical{" "}
            <code>referral_stats/&#123;uid&#125;/withdrawals</code> subcollection used by
            the Android app. Approve moves to processing, Mark paid completes the payout.
          </p>
        </div>

        {!loading && !error && withdrawals.length === 0 ? (
          <div className="empty-state">No withdrawal requests were returned from Firestore.</div>
        ) : null}

        {!loading && !error && withdrawals.length > 0 ? (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>User</th>
                  <th>Amount</th>
                  <th>Wallet After Request</th>
                  <th>Method</th>
                  <th>Payment Details</th>
                  <th>Status</th>
                  <th>Requested</th>
                  <th>Processed</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {withdrawals.map((withdrawal) => (
                  <tr key={withdrawal.id}>
                    <td>{shortId(withdrawal.userId)}</td>
                    <td>
                      <strong>{withdrawal.userName || "N/A"}</strong>
                      <div className="route-note">{withdrawal.userRole || "Role missing"}</div>
                      <div className="route-note">{withdrawal.phone || "Phone missing"}</div>
                      <div className="route-note">Code: {withdrawal.referralCode || "missing"}</div>
                    </td>
                    <td>{formatCurrency(withdrawal.amount)}</td>
                    <td>
                      <strong>{formatCurrency(withdrawal.availableBalance)}</strong>
                      <div className="route-note">Earned: {formatCurrency(withdrawal.totalEarnings)}</div>
                      <div className="route-note">Withdrawn: {formatCurrency(withdrawal.withdrawnAmount)}</div>
                    </td>
                    <td>{withdrawal.paymentMethod ?? "N/A"}</td>
                    <td>{paymentDetails(withdrawal)}</td>
                    <td>
                      <span className={`status-pill ${statusTone(withdrawal.status)}`}>
                        {withdrawal.status ?? "PENDING"}
                      </span>
                      {withdrawal.transactionId ? (
                        <div className="route-note">Txn: {withdrawal.transactionId}</div>
                      ) : null}
                    </td>
                    <td>{formatDate(withdrawal.createdAt)}</td>
                    <td>{formatDate(withdrawal.processedAt)}</td>
                    <td>
                      {withdrawal.status === "PENDING" ? (
                        <div className="button-row compact">
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => void handleWithdrawalUpdate(withdrawal.id, withdrawal.userId, "PROCESSING")}
                            disabled={pendingActionId === withdrawal.id}
                          >
                            {pendingActionId === withdrawal.id ? "Working..." : "Approve"}
                          </button>
                          <button
                            type="button"
                            className="table-action danger"
                            onClick={() => void handleWithdrawalUpdate(withdrawal.id, withdrawal.userId, "FAILED")}
                            disabled={pendingActionId === withdrawal.id}
                          >
                            Reject
                          </button>
                        </div>
                      ) : withdrawal.status === "PROCESSING" ? (
                        <div className="button-row compact">
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => void handleWithdrawalUpdate(withdrawal.id, withdrawal.userId, "COMPLETED")}
                            disabled={pendingActionId === withdrawal.id}
                          >
                            {pendingActionId === withdrawal.id ? "Working..." : "Mark paid"}
                          </button>
                          <button
                            type="button"
                            className="table-action danger"
                            onClick={() => void handleWithdrawalUpdate(withdrawal.id, withdrawal.userId, "FAILED")}
                            disabled={pendingActionId === withdrawal.id}
                          >
                            Reject/refund
                          </button>
                        </div>
                      ) : (
                        <span className="route-note">Processed</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </div>
  );
}

function ReferralLookupPanel({ lookup }: { lookup: ReferralLookupResult }) {
  const stats: Record<string, unknown> = lookup.referralStats ?? {};
  const referralCodes = lookup.referralCodes ?? [];
  const referralsAsReferrer = lookup.referralsAsReferrer ?? [];
  const referralsAsReferred = lookup.referralsAsReferred ?? [];
  const withdrawals = lookup.withdrawals ?? [];
  const auditLogs = lookup.auditLogs ?? [];

  return (
    <div className="admin-lookup-results">
      <div className="admin-stats-grid small">
        <div className="admin-stat-card compact">
          <strong>{lookup.identity?.userName || "N/A"}</strong>
          <span>{shortId(lookup.userId)}</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{lookup.identity?.phone || "N/A"}</strong>
          <span>{lookup.identity?.role || "No role"}</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{lookup.referralCode || lookup.identity?.referralCode || "N/A"}</strong>
          <span>Referral code</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{formatCurrency(stats.availableBalance)}</strong>
          <span>Available balance</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{formatCurrency(stats.totalEarnings)}</strong>
          <span>Total earnings</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{referralsAsReferrer.length}</strong>
          <span>Referred users</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{withdrawals.length}</strong>
          <span>Withdrawals</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{lookup.resolvedBy || "N/A"}</strong>
          <span>Resolved by</span>
        </div>
      </div>

      <div className="collection-table-wrapper">
        <table className="collection-table">
          <thead>
            <tr>
              <th>Referral Codes</th>
              <th>User</th>
              <th>Created</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {referralCodes.length ? (
              referralCodes.map((code) => (
                <tr key={String(code.id || code.code)}>
                  <td>{String(code.code || code.id || "N/A")}</td>
                  <td>{String(code.userId || code.uid || lookup.userId || "N/A")}</td>
                  <td>{formatDateTime(code.createdAt)}</td>
                  <td>{String(code.status || "Active")}</td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={4}>No referral code records found.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <ReferralHistoryTable title="Referrals by this user" rows={referralsAsReferrer} />
      <ReferralHistoryTable title="Referral that brought this user" rows={referralsAsReferred} />

      <div className="collection-table-wrapper">
        <table className="collection-table">
          <thead>
            <tr>
              <th>Withdrawal</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Method</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {withdrawals.length ? (
              withdrawals.map((withdrawal) => (
                <tr key={withdrawal.id}>
                  <td>{shortId(withdrawal.id)}</td>
                  <td>{formatCurrency(withdrawal.amount)}</td>
                  <td><span className={`status-pill ${statusTone(withdrawal.status)}`}>{withdrawal.status || "N/A"}</span></td>
                  <td>{withdrawal.paymentMethod || "N/A"}</td>
                  <td>{formatDateTime(withdrawal.createdAt)}</td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={5}>No withdrawals found.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="collection-table-wrapper">
        <table className="collection-table">
          <thead>
            <tr>
              <th>Audit Event</th>
              <th>When</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {auditLogs.length ? (
              auditLogs.map((event) => (
                <tr key={String(event.id || event.timestamp || event.eventType)}>
                  <td>{String(event.eventType || event.type || "Event")}</td>
                  <td>{formatDateTime(event.timestamp || event.createdAt)}</td>
                  <td><pre className="collection-json-preview">{JSON.stringify(event, null, 2)}</pre></td>
                </tr>
              ))
            ) : (
              <tr><td colSpan={3}>No audit logs found.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <details className="collection-inspector">
        <summary>Raw referral_stats document</summary>
        <pre>{JSON.stringify(stats, null, 2)}</pre>
      </details>
    </div>
  );
}

function ReferralHistoryTable({ title, rows }: { title: string; rows: ReferralRow[] }) {
  return (
    <div className="collection-table-wrapper">
      <table className="collection-table">
        <thead>
          <tr>
            <th>{title}</th>
            <th>Referrer</th>
            <th>Referred</th>
            <th>Code</th>
            <th>Bonus</th>
            <th>Status</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          {rows.length ? (
            rows.map((referral) => (
              <tr key={referral.id}>
                <td>{shortId(referral.id)}</td>
                <td>{referral.referrerUserName || referral.referrerPhone || shortId(referral.referrerId)}</td>
                <td>{referral.referredUserName || referral.referredUserPhone || shortId(referral.referredUserId)}</td>
                <td>{referral.referralCode || referral.referredByCode || "N/A"}</td>
                <td>{formatCurrency(referral.bonusAmount)}</td>
                <td><span className={`status-pill ${statusTone(referral.status)}`}>{referral.status || "N/A"}</span></td>
                <td>{formatDateTime(referral.createdAt)}</td>
              </tr>
            ))
          ) : (
            <tr><td colSpan={7}>No matching referral rows found.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function shortId(value: string | undefined) {
  if (!value) {
    return "N/A";
  }

  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}

function paymentDetails(withdrawal: WithdrawalRow) {
  if (withdrawal.paymentMethod === "BANK_TRANSFER") {
    return [withdrawal.accountHolderName, withdrawal.bankAccountNumber, withdrawal.ifscCode]
      .filter(Boolean)
      .join(" / ") || "N/A";
  }

  return withdrawal.upiId || "N/A";
}

function statusTone(status: string | undefined) {
  switch (status) {
    case "COMPLETED":
    case "ACCEPTED":
      return "success";
    case "PROCESSING":
      return "warning";
    case "FAILED":
    case "REJECTED":
      return "danger";
    case "PENDING":
      return "warning";
    default:
      return "neutral";
  }
}
