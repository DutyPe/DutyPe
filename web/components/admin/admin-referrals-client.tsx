"use client";

import { useEffect, useState } from "react";
import { formatCurrency, formatDate } from "@/lib/firebase/firestore-helpers";

type ReferralRow = {
  id: string;
  referralCode?: string;
  referredUserName?: string;
  referrerUserName?: string;
  status?: string;
  rewardAmount?: number | string;
  createdAt?: unknown;
};

type WithdrawalRow = {
  id: string;
  userId?: string;
  amount?: number | string;
  paymentMethod?: string;
  upiId?: string;
  status?: string;
  createdAt?: unknown;
};

export function AdminReferralsClient() {
  const [referrals, setReferrals] = useState<ReferralRow[]>([]);
  const [withdrawals, setWithdrawals] = useState<WithdrawalRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingActionId, setPendingActionId] = useState<string | null>(null);

  async function loadData() {
    try {
      setLoading(true);
      const response = await fetch("/api/admin/referrals", {
        credentials: "include",
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

  async function handleWithdrawalUpdate(id: string, status: "COMPLETED" | "FAILED") {
    const confirmationMessage =
      status === "COMPLETED"
        ? "Approve this withdrawal request?"
        : "Reject this withdrawal request?";

    if (!window.confirm(confirmationMessage)) {
      return;
    }

    try {
      setPendingActionId(id);
      const response = await fetch("/api/admin/referrals", {
        method: "PATCH",
        credentials: "include",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          withdrawalId: id,
          status
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
  const completedWithdrawalCount = withdrawals.filter(
    (withdrawal) => withdrawal.status === "COMPLETED"
  ).length;

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
              <span>Withdrawal requests waiting for an admin decision.</span>
            </div>
            <div className="metric">
              <strong>{completedWithdrawalCount}</strong>
              <span>Withdrawal requests already marked completed.</span>
            </div>
          </div>
        ) : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Referrals</span>
            <h2>Referral history</h2>
          </div>
          <p>
            Reads the same `referrals` collection as the legacy admin page, but uses the
            shared React layout and formatting helpers.
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
                  <th>Referred User</th>
                  <th>Referrer</th>
                  <th>Status</th>
                  <th>Reward</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {referrals.map((referral) => (
                  <tr key={referral.id}>
                    <td>{referral.referralCode ?? "N/A"}</td>
                    <td>{referral.referredUserName ?? "N/A"}</td>
                    <td>{referral.referrerUserName ?? "N/A"}</td>
                    <td>
                      <span className={`status-pill ${statusTone(referral.status)}`}>
                        {referral.status ?? "PENDING"}
                      </span>
                    </td>
                    <td>{formatCurrency(referral.rewardAmount)}</td>
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
            Admin actions here update the live `withdrawal_requests` collection with the
            same statuses already used in the legacy tool.
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
                  <th>Amount</th>
                  <th>Method</th>
                  <th>UPI ID</th>
                  <th>Status</th>
                  <th>Requested</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {withdrawals.map((withdrawal) => (
                  <tr key={withdrawal.id}>
                    <td>{shortId(withdrawal.userId)}</td>
                    <td>{formatCurrency(withdrawal.amount)}</td>
                    <td>{withdrawal.paymentMethod ?? "N/A"}</td>
                    <td>{withdrawal.upiId ?? "N/A"}</td>
                    <td>
                      <span className={`status-pill ${statusTone(withdrawal.status)}`}>
                        {withdrawal.status ?? "PENDING"}
                      </span>
                    </td>
                    <td>{formatDate(withdrawal.createdAt)}</td>
                    <td>
                      {withdrawal.status === "PENDING" ? (
                        <div className="button-row compact">
                          <button
                            type="button"
                            className="table-action"
                            onClick={() => void handleWithdrawalUpdate(withdrawal.id, "COMPLETED")}
                            disabled={pendingActionId === withdrawal.id}
                          >
                            {pendingActionId === withdrawal.id ? "Working..." : "Approve"}
                          </button>
                          <button
                            type="button"
                            className="table-action danger"
                            onClick={() => void handleWithdrawalUpdate(withdrawal.id, "FAILED")}
                            disabled={pendingActionId === withdrawal.id}
                          >
                            Reject
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

function shortId(value: string | undefined) {
  if (!value) {
    return "N/A";
  }

  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}

function statusTone(status: string | undefined) {
  switch (status) {
    case "COMPLETED":
    case "ACCEPTED":
      return "success";
    case "FAILED":
    case "REJECTED":
      return "danger";
    case "PENDING":
      return "warning";
    default:
      return "neutral";
  }
}
