"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import {
  Copy,
  Check,
  Phone,
  ExternalLink,
  ShieldAlert,
  ShieldCheck,
  RefreshCw,
  Search,
  Wallet,
  TrendingUp,
  ArrowDownCircle,
  Users,
  Award,
  AlertTriangle,
  HardHat,
  Building2
} from "lucide-react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import type { ReferralStatRow } from "@/app/api/admin/referral-stats/route";

type SummaryMetrics = {
  totalWallets: number;
  activeWallets?: number;
  blockedWallets?: number;
  loadedCount?: number;
  totalBalanceRupees: number;
  totalEarnedRupees: number;
  totalWithdrawnRupees: number;
  totalReferrals: number;
};

type Payload = {
  items?: ReferralStatRow[];
  summary?: SummaryMetrics;
  nextCursor?: string | null;
  error?: string;
};

export function AdminReferralStatsClient() {
  const [items, setItems] = useState<ReferralStatRow[]>([]);
  const [summary, setSummary] = useState<SummaryMetrics | null>(null);
  const [nextCursor, setNextCursor] = useState<string | null>(null);

  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<"" | "WORKER" | "EMPLOYER">("");
  const [statusFilter, setStatusFilter] = useState<"" | "false" | "true">("");
  const [hasBalanceFilter, setHasBalanceFilter] = useState<"" | "true" | "false">("");

  const [loading, setLoading] = useState(true);
  const [busyUid, setBusyUid] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [copiedPhone, setCopiedPhone] = useState<string | null>(null);
  const [copiedUid, setCopiedUid] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = useCallback(
    async (params: {
      q?: string;
      role?: string;
      blocked?: string;
      hasBalance?: string;
      after?: string;
      append?: boolean;
    }) => {
      setLoading(true);
      setError(null);
      try {
        const sp = new URLSearchParams();
        if (params.q?.trim()) sp.set("q", params.q.trim());
        if (params.role) sp.set("role", params.role);
        if (params.blocked) sp.set("blocked", params.blocked);
        if (params.hasBalance) sp.set("hasBalance", params.hasBalance);
        if (params.after) sp.set("after", params.after);
        sp.set("t", String(Date.now()));

        const res = await adminApiFetch(`/api/admin/referral-stats?${sp.toString()}`, { cache: "no-store" });
        const data = (await res.json()) as Payload;
        if (!res.ok) throw new Error(data.error || "Failed to load referral stats.");

        setItems((prev) => (params.append ? [...prev, ...(data.items ?? [])] : data.items ?? []));
        if (data.summary) setSummary(data.summary);
        setNextCursor(data.nextCursor ?? null);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load referral stats.");
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    void load({
      role: roleFilter,
      blocked: statusFilter,
      hasBalance: hasBalanceFilter
    });
  }, [roleFilter, statusFilter, hasBalanceFilter, load]);

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    void load({
      q: searchQuery,
      role: roleFilter,
      blocked: statusFilter,
      hasBalance: hasBalanceFilter
    });
  }

  function handleClearSearch() {
    setSearchQuery("");
    void load({
      q: "",
      role: roleFilter,
      blocked: statusFilter,
      hasBalance: hasBalanceFilter
    });
  }

  async function toggleBlocked(row: ReferralStatRow) {
    const nextBlocked = !row.blocked;
    const actionLabel = nextBlocked ? "Block & Freeze Wallet" : "Unblock Wallet";
    if (!window.confirm(`${actionLabel} for ${row.name || row.phone || row.uid}?`)) return;

    setBusyUid(row.uid);
    setError(null);
    try {
      const res = await adminApiFetch("/api/admin/referral-stats", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ uid: row.uid, blocked: nextBlocked })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Failed to update wallet status.");

      setItems((prev) =>
        prev.map((item) => (item.uid === row.uid ? { ...item, blocked: nextBlocked } : item))
      );
      setMessage(`Wallet for ${row.name || row.phone || row.uid} is now ${nextBlocked ? "BLOCKED" : "ACTIVE"}.`);
      setTimeout(() => setMessage(null), 4000);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update wallet.");
    } finally {
      setBusyUid(null);
    }
  }

  function copyText(text: string, type: "code" | "phone" | "uid") {
    if (!navigator.clipboard) return;
    navigator.clipboard.writeText(text);
    if (type === "code") {
      setCopiedCode(text);
      setTimeout(() => setCopiedCode(null), 2000);
    } else if (type === "phone") {
      setCopiedPhone(text);
      setTimeout(() => setCopiedPhone(null), 2000);
    } else {
      setCopiedUid(text);
      setTimeout(() => setCopiedUid(null), 2000);
    }
  }

  const getInitials = (name?: string) => {
    if (!name || !name.trim()) return "?";
    return name
      .trim()
      .split(" ")
      .map((n) => n[0])
      .slice(0, 2)
      .join("")
      .toUpperCase();
  };

  // Client-side quick filter on loaded items
  const needle = searchQuery.trim().toLowerCase();
  const visibleItems = needle
    ? items.filter((row) =>
        `${row.name || ""} ${row.resolvedName || ""} ${row.phone || ""} ${row.referralCode || ""} ${row.uid}`
          .toLowerCase()
          .includes(needle)
      )
    : items;

  return (
    <div className="adm-stack">
      {/* Messages */}
      {message && (
        <div className="adm-card" style={{ borderLeft: "4px solid #10b981", background: "#ecfdf5", padding: "12px 18px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", color: "#065f46", fontSize: "0.875rem", fontWeight: 600 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <Check style={{ width: 18, height: 18, color: "#10b981", flexShrink: 0 }} />
              <span>{message}</span>
            </div>
            <button
              type="button"
              onClick={() => setMessage(null)}
              style={{ background: "none", border: "none", cursor: "pointer", color: "#065f46", fontWeight: 700 }}
            >
              ✕
            </button>
          </div>
        </div>
      )}

      {error && (
        <div className="adm-card" style={{ borderLeft: "4px solid #ef4444", background: "#fef2f2", padding: "12px 18px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", color: "#b91c1c", fontSize: "0.875rem", fontWeight: 600 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <AlertTriangle style={{ width: 18, height: 18, color: "#ef4444", flexShrink: 0 }} />
              <span>{error}</span>
            </div>
            <button
              type="button"
              onClick={() => setError(null)}
              style={{ background: "none", border: "none", cursor: "pointer", color: "#b91c1c", fontWeight: 700 }}
            >
              ✕
            </button>
          </div>
        </div>
      )}

      {/* Header Row */}
      <div className="adm-header-row">
        <div>
          <h1 className="adm-page-title">Referral Stats & Wallets</h1>
          <p className="adm-page-subtitle">
            Track user referral earnings, current balances in Rupees (₹), conversions, and wallet statuses.
          </p>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button
            type="button"
            className="adm-btn adm-btn-secondary"
            onClick={() =>
              void load({
                q: searchQuery,
                role: roleFilter,
                blocked: statusFilter,
                hasBalance: hasBalanceFilter
              })
            }
            disabled={loading}
          >
            <RefreshCw style={{ width: 15, height: 15 }} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
        </div>
      </div>

      {/* Top Metrics Grid */}
      <div className="adm-metrics-grid">
        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Total Wallets</span>
            <div className="adm-metric-icon" style={{ background: "#f1f5f9", color: "#475569" }}>
              <Users style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{summary?.totalWallets?.toLocaleString("en-IN") ?? items.length}</div>
          <div className="adm-metric-sub">
            <span>
              {summary?.activeWallets !== undefined
                ? `${summary.activeWallets} active · ${summary.blockedWallets ?? 0} blocked`
                : "Registered user wallets"}
            </span>
          </div>
        </div>

        <div className="adm-metric-card" style={{ borderColor: "#a7f3d0" }}>
          <div className="adm-metric-top">
            <span className="adm-metric-label" style={{ color: "#047857" }}>Available Balance</span>
            <div className="adm-metric-icon" style={{ background: "#ecfdf5", color: "#059669" }}>
              <Wallet style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value" style={{ color: "#065f46" }}>
            ₹{(summary?.totalBalanceRupees ?? items.reduce((s, i) => s + i.balanceRupees, 0)).toLocaleString("en-IN", {
              minimumFractionDigits: 0,
              maximumFractionDigits: 2
            })}
          </div>
          <div className="adm-metric-sub" style={{ color: "#059669" }}>
            <span>Unclaimed wallet balance</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Lifetime Earned</span>
            <div className="adm-metric-icon" style={{ background: "#eff6ff", color: "#2563eb" }}>
              <TrendingUp style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value" style={{ color: "#1e40af" }}>
            ₹{(summary?.totalEarnedRupees ?? items.reduce((s, i) => s + i.lifetimeEarnedRupees, 0)).toLocaleString("en-IN", {
              minimumFractionDigits: 0,
              maximumFractionDigits: 2
            })}
          </div>
          <div className="adm-metric-sub">
            <span>Total rewards credited</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Total Withdrawn</span>
            <div className="adm-metric-icon" style={{ background: "#f5f3ff", color: "#7c3aed" }}>
              <ArrowDownCircle style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value" style={{ color: "#6b21a8" }}>
            ₹{(summary?.totalWithdrawnRupees ?? items.reduce((s, i) => s + i.withdrawnRupees, 0)).toLocaleString("en-IN", {
              minimumFractionDigits: 0,
              maximumFractionDigits: 2
            })}
          </div>
          <div className="adm-metric-sub">
            <span>Paid out via UPI</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Successful Referrals</span>
            <div className="adm-metric-icon" style={{ background: "#fffbeb", color: "#d97706" }}>
              <Award style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value" style={{ color: "#b45309" }}>
            {(summary?.totalReferrals ?? items.reduce((s, i) => s + i.successfulReferrals, 0)).toLocaleString("en-IN")}
          </div>
          <div className="adm-metric-sub">
            <span>Total conversions</span>
          </div>
        </div>
      </div>

      {/* Segmented Filter Bar */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 12 }}>
        <div className="adm-segmented">
          <button
            type="button"
            className={`adm-segmented-btn ${roleFilter === "" ? "active" : ""}`}
            onClick={() => setRoleFilter("")}
          >
            All Accounts
          </button>
          <button
            type="button"
            className={`adm-segmented-btn ${roleFilter === "WORKER" ? "active" : ""}`}
            onClick={() => setRoleFilter("WORKER")}
          >
            👷 Workers
          </button>
          <button
            type="button"
            className={`adm-segmented-btn ${roleFilter === "EMPLOYER" ? "active" : ""}`}
            onClick={() => setRoleFilter("EMPLOYER")}
          >
            🏢 Employers
          </button>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
          <select
            className="adm-select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as "" | "false" | "true")}
          >
            <option value="">All Statuses</option>
            <option value="false">Active Wallets</option>
            <option value="true">Blocked Wallets</option>
          </select>

          <select
            className="adm-select"
            value={hasBalanceFilter}
            onChange={(e) => setHasBalanceFilter(e.target.value as "" | "true" | "false")}
          >
            <option value="">All Balances</option>
            <option value="true">Has Balance (&gt; ₹0)</option>
            <option value="false">Zero Balance (₹0)</option>
          </select>
        </div>
      </div>

      {/* Search Toolbar */}
      <form onSubmit={handleSearchSubmit} className="adm-toolbar">
        <div className="adm-search-wrap">
          <Search className="adm-search-icon" />
          <input
            type="text"
            className="adm-input-search"
            placeholder="Search by mobile number (+91...), referral code, user name, or UID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <button type="submit" className="adm-btn adm-btn-primary" disabled={loading}>
          {loading ? "Searching…" : "Search"}
        </button>

        {searchQuery ? (
          <button type="button" className="adm-btn adm-btn-ghost" onClick={handleClearSearch}>
            Clear
          </button>
        ) : null}

        <span style={{ fontSize: "0.825rem", color: "var(--adm-text-muted)", marginLeft: "auto", fontWeight: 500 }}>
          Showing <strong>{visibleItems.length}</strong> of {items.length} wallets
        </span>
      </form>

      {/* Table Section */}
      {loading && items.length === 0 ? (
        <div className="adm-card" style={{ padding: 32, display: "flex", flexDirection: "column", gap: 12 }}>
          <div className="adm-skeleton" style={{ height: 28, width: "30%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
        </div>
      ) : visibleItems.length === 0 ? (
        <div
          className="adm-card"
          style={{
            padding: "48px 24px",
            textAlign: "center",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: 12
          }}
        >
          <Wallet style={{ width: 44, height: 44, color: "var(--adm-text-subtle)" }} />
          <h3 style={{ margin: 0, fontSize: "1.1rem", color: "var(--adm-text-main)" }}>No referral stats found</h3>
          <p style={{ margin: 0, fontSize: "0.875rem", color: "var(--adm-text-muted)", maxWidth: 380 }}>
            {searchQuery
              ? `No user wallet matched "${searchQuery}". Check the phone number or referral code.`
              : "No records found matching the active filters."}
          </p>
          {searchQuery && (
            <button type="button" className="adm-btn adm-btn-secondary" onClick={handleClearSearch}>
              Clear Search
            </button>
          )}
        </div>
      ) : (
        <div className="adm-table-wrap">
          <table className="adm-table">
            <thead>
              <tr>
                <th>User Identity</th>
                <th>Phone Number</th>
                <th>Role</th>
                <th>Referral Code</th>
                <th style={{ textAlign: "right" }}>Balance (₹)</th>
                <th style={{ textAlign: "right" }}>Lifetime Earned</th>
                <th style={{ textAlign: "right" }}>Withdrawn</th>
                <th style={{ textAlign: "center" }}>Referrals</th>
                <th style={{ textAlign: "center" }}>Status</th>
                <th style={{ textAlign: "right" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visibleItems.map((row) => {
                const displayName = row.name || row.resolvedName || "Unnamed User";
                const isUidCopied = copiedUid === row.uid;
                const isCodeCopied = copiedCode === row.referralCode;
                const isPhoneCopied = copiedPhone === row.phone;
                const isBusy = busyUid === row.uid;

                return (
                  <tr key={row.uid} style={{ opacity: isBusy ? 0.5 : 1 }}>
                    {/* User Identity */}
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div
                          style={{
                            width: 36,
                            height: 36,
                            borderRadius: "50%",
                            background: row.role === "EMPLOYER" ? "#f5f3ff" : "#eff6ff",
                            color: row.role === "EMPLOYER" ? "#7c3aed" : "#2563eb",
                            border: `2px solid ${row.role === "EMPLOYER" ? "#ddd6fe" : "#bfdbfe"}`,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontWeight: 700,
                            fontSize: "0.8rem",
                            flexShrink: 0
                          }}
                        >
                          {getInitials(displayName)}
                        </div>
                        <div>
                          <div style={{ fontWeight: 600, color: "var(--adm-text-main)" }}>{displayName}</div>
                          <div style={{ display: "flex", alignItems: "center", gap: 4, marginTop: 2 }}>
                            <code
                              style={{
                                fontSize: "0.7rem",
                                color: "var(--adm-text-subtle)",
                                background: "var(--adm-surface-subtle)",
                                padding: "1px 4px",
                                borderRadius: 3
                              }}
                            >
                              {row.uid.slice(0, 10)}...
                            </code>
                            <button
                              type="button"
                              className="adm-btn-icon"
                              style={{ width: 18, height: 18, border: "none" }}
                              title="Copy UID"
                              onClick={() => copyText(row.uid, "uid")}
                            >
                              {isUidCopied ? (
                                <Check style={{ width: 11, height: 11, color: "#10b981" }} />
                              ) : (
                                <Copy style={{ width: 11, height: 11 }} />
                              )}
                            </button>
                          </div>
                        </div>
                      </div>
                    </td>

                    {/* Phone Number */}
                    <td>
                      {row.phone ? (
                        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                          <a
                            href={`tel:${row.phone}`}
                            style={{
                              fontFamily: "monospace",
                              fontWeight: 600,
                              color: "var(--adm-text-main)",
                              textDecoration: "none",
                              fontSize: "0.85rem"
                            }}
                          >
                            {row.phone}
                          </a>
                          <button
                            type="button"
                            className="adm-btn-icon"
                            style={{ width: 18, height: 18, border: "none" }}
                            title="Copy Phone"
                            onClick={() => copyText(row.phone, "phone")}
                          >
                            {isPhoneCopied ? (
                              <Check style={{ width: 11, height: 11, color: "#10b981" }} />
                            ) : (
                              <Copy style={{ width: 11, height: 11 }} />
                            )}
                          </button>
                        </div>
                      ) : (
                        <span style={{ color: "var(--adm-text-subtle)", fontSize: "0.8rem" }}>—</span>
                      )}
                    </td>

                    {/* Role */}
                    <td>
                      <span className={`adm-badge ${row.role === "EMPLOYER" ? "adm-badge-purple" : "adm-badge-blue"}`}>
                        {row.role === "EMPLOYER" ? (
                          <>
                            <Building2 style={{ width: 12, height: 12 }} /> Employer
                          </>
                        ) : (
                          <>
                            <HardHat style={{ width: 12, height: 12 }} /> Worker
                          </>
                        )}
                      </span>
                    </td>

                    {/* Referral Code */}
                    <td>
                      {row.referralCode ? (
                        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                          <Link
                            href={`/admin/referral-codes?q=${encodeURIComponent(row.referralCode)}`}
                            style={{
                              fontFamily: "monospace",
                              fontWeight: 700,
                              fontSize: "0.8rem",
                              background: "var(--adm-surface-subtle)",
                              color: "var(--adm-text-main)",
                              padding: "2px 6px",
                              borderRadius: 4,
                              textDecoration: "none",
                              display: "inline-flex",
                              alignItems: "center",
                              gap: 4
                            }}
                            title="View code details"
                          >
                            <span>{row.referralCode}</span>
                            <ExternalLink style={{ width: 11, height: 11, color: "var(--adm-text-subtle)" }} />
                          </Link>
                          <button
                            type="button"
                            className="adm-btn-icon"
                            style={{ width: 18, height: 18, border: "none" }}
                            title="Copy Code"
                            onClick={() => copyText(row.referralCode, "code")}
                          >
                            {isCodeCopied ? (
                              <Check style={{ width: 11, height: 11, color: "#10b981" }} />
                            ) : (
                              <Copy style={{ width: 11, height: 11 }} />
                            )}
                          </button>
                        </div>
                      ) : (
                        <span style={{ color: "var(--adm-text-subtle)", fontSize: "0.8rem", fontStyle: "italic" }}>
                          None
                        </span>
                      )}
                    </td>

                    {/* Balance */}
                    <td style={{ textAlign: "right" }}>
                      <span
                        className={`adm-badge ${row.balanceRupees > 0 ? "adm-badge-emerald" : "adm-badge-neutral"}`}
                        style={{ fontFamily: "monospace", fontWeight: 700, fontSize: "0.85rem" }}
                      >
                        ₹{row.balanceRupees.toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}
                      </span>
                    </td>

                    {/* Lifetime Earned */}
                    <td style={{ textAlign: "right", fontFamily: "monospace", fontWeight: 600, color: "var(--adm-text-main)" }}>
                      ₹{row.lifetimeEarnedRupees.toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}
                    </td>

                    {/* Withdrawn */}
                    <td style={{ textAlign: "right", fontFamily: "monospace", color: "var(--adm-text-muted)" }}>
                      ₹{row.withdrawnRupees.toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}
                    </td>

                    {/* Referrals */}
                    <td style={{ textAlign: "center" }}>
                      <span className="adm-badge adm-badge-amber" style={{ fontWeight: 700 }}>
                        {row.successfulReferrals}
                      </span>
                    </td>

                    {/* Status */}
                    <td style={{ textAlign: "center" }}>
                      {row.blocked ? (
                        <span className="adm-badge adm-badge-rose">
                          <ShieldAlert style={{ width: 12, height: 12 }} /> Blocked
                        </span>
                      ) : (
                        <span className="adm-badge adm-badge-emerald">
                          <ShieldCheck style={{ width: 12, height: 12 }} /> Active
                        </span>
                      )}
                    </td>

                    {/* Actions */}
                    <td style={{ textAlign: "right" }}>
                      <button
                        type="button"
                        onClick={() => toggleBlocked(row)}
                        disabled={isBusy}
                        className={`adm-btn adm-btn-sm ${row.blocked ? "adm-btn-secondary" : "adm-btn-danger"}`}
                        style={{ minWidth: 70 }}
                      >
                        {isBusy ? "…" : row.blocked ? "Unblock" : "Block"}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Load More Pagination */}
      {nextCursor && (
        <div style={{ display: "flex", justifyContent: "center", paddingTop: 10 }}>
          <button
            type="button"
            className="adm-btn adm-btn-secondary"
            onClick={() =>
              void load({
                q: searchQuery,
                role: roleFilter,
                blocked: statusFilter,
                hasBalance: hasBalanceFilter,
                after: nextCursor,
                append: true
              })
            }
            disabled={loading}
          >
            {loading ? "Loading more…" : "Load More Wallets"}
          </button>
        </div>
      )}
    </div>
  );
}
