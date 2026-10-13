"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Copy, Check, Phone, ExternalLink, ShieldCheck, UserCheck, RefreshCw, Search } from "lucide-react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";

type ReferralCodeRow = {
  id: string;
  code: string;
  uid: string;
  phone: string;
  name: string;
  businessName?: string;
  area: string;
  role: "WORKER" | "EMPLOYER";
  active: boolean;
  balancePaise: number;
  balanceRupees: number;
  successfulReferrals: number;
  lifetimeEarnedRupees: number;
  withdrawnRupees: number;
  shareUrl: string;
};

type Payload = {
  codes?: ReferralCodeRow[];
  counts?: { total: number; active: number };
  nextCursor?: string | null;
  error?: string;
};

export function AdminReferralCodesClient() {
  const [codes, setCodes] = useState<ReferralCodeRow[]>([]);
  const [counts, setCounts] = useState<{ total: number; active: number } | null>(null);
  const [nextCursor, setNextCursor] = useState<string | null>(null);

  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<"" | "WORKER" | "EMPLOYER">("");
  const [statusFilter, setStatusFilter] = useState<"" | "true" | "false">("");

  const [loading, setLoading] = useState(true);
  const [busyCode, setBusyCode] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [copiedPhone, setCopiedPhone] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const load = useCallback(async (params: { q?: string; role?: string; active?: string; after?: string; append?: boolean }) => {
    setLoading(true);
    setError(null);
    try {
      const sp = new URLSearchParams();
      if (params.q?.trim()) sp.set("q", params.q.trim());
      if (params.role) sp.set("role", params.role);
      if (params.active) sp.set("active", params.active);
      if (params.after) sp.set("after", params.after);
      sp.set("t", String(Date.now()));

      const res = await adminApiFetch(`/api/admin/referral-codes?${sp.toString()}`, { cache: "no-store" });
      const data = (await res.json()) as Payload;
      if (!res.ok) throw new Error(data.error || "Failed to load referral codes.");

      setCodes((prev) => (params.append ? [...prev, ...(data.codes ?? [])] : data.codes ?? []));
      if (data.counts) setCounts(data.counts);
      setNextCursor(data.nextCursor ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load referral codes.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load({ role: roleFilter, active: statusFilter });
  }, [roleFilter, statusFilter, load]);

  function handleSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    void load({ q: searchQuery, role: roleFilter, active: statusFilter });
  }

  function handleClearSearch() {
    setSearchQuery("");
    void load({ q: "", role: roleFilter, active: statusFilter });
  }

  async function toggleActive(row: ReferralCodeRow) {
    const nextState = !row.active;
    const actionLabel = nextState ? "Activate" : "Deactivate / Suspend";
    if (!window.confirm(`${actionLabel} referral code "${row.code}" for ${row.name || row.phone || row.uid}?`)) return;

    setBusyCode(row.code);
    setError(null);
    try {
      const res = await adminApiFetch("/api/admin/referral-codes", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code: row.code, active: nextState })
      });
      const data = (await res.json()) as { success?: boolean; error?: string };
      if (!res.ok) throw new Error(data.error || "Failed to update referral code.");

      setCodes((prev) => prev.map((c) => (c.code === row.code ? { ...c, active: nextState } : c)));
      setMessage(`Referral code ${row.code} marked ${nextState ? "active" : "inactive"}.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update code status.");
    } finally {
      setBusyCode(null);
    }
  }

  function copyToClipboard(text: string, isPhone: boolean = false) {
    if (!text) return;
    navigator.clipboard.writeText(text);
    if (isPhone) {
      setCopiedPhone(text);
      setTimeout(() => setCopiedPhone(null), 2000);
    } else {
      setCopiedCode(text);
      setTimeout(() => setCopiedCode(null), 2000);
    }
  }

  // Client-side quick filter on loaded rows
  const queryLower = searchQuery.trim().toLowerCase();
  const visibleCodes = searchQuery.trim()
    ? codes.filter((c) =>
        `${c.code} ${c.phone} ${c.name} ${c.businessName ?? ""} ${c.area} ${c.uid}`
          .toLowerCase()
          .includes(queryLower)
      )
    : codes;

  return (
    <div className="admin-section-stack">
      {message ? (
        <div className="callout" role="status" style={{ display: "flex", justifyContent: "space-between" }}>
          <span>{message}</span>
          <button type="button" onClick={() => setMessage(null)} style={{ background: "none", border: "none", cursor: "pointer" }}>✕</button>
        </div>
      ) : null}
      {error ? <div className="admin-error">{error}</div> : null}

      {/* Top Stat Cards */}
      <div className="admin-stats-grid small">
        <div className="admin-stat-card compact">
          <strong>{counts?.total ?? codes.length}</strong>
          <span>Total Referral Codes</span>
        </div>
        <div className="admin-stat-card compact">
          <strong style={{ color: "#059669" }}>{counts?.active ?? codes.filter((c) => c.active).length}</strong>
          <span>Active Codes</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{codes.filter((c) => c.role === "WORKER").length}</strong>
          <span>Workers with Codes</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{codes.filter((c) => c.role === "EMPLOYER").length}</strong>
          <span>Employers with Codes</span>
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <form onSubmit={handleSearchSubmit} className="admin-toolbar" style={{ flexWrap: "wrap", gap: "8px" }}>
        <div style={{ display: "flex", flex: "1 1 320px", position: "relative", alignItems: "center" }}>
          <Search size={16} style={{ position: "absolute", left: "10px", color: "#64748b" }} />
          <input
            className="admin-search"
            style={{ width: "100%", paddingLeft: "32px", fontSize: "0.9rem" }}
            placeholder="Search by Mobile Number (+91...), Referral Code, Name, or UID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery ? (
            <button
              type="button"
              onClick={handleClearSearch}
              style={{
                position: "absolute",
                right: "8px",
                background: "none",
                border: "none",
                cursor: "pointer",
                color: "#64748b",
                fontSize: "0.9rem"
              }}
            >
              ✕
            </button>
          ) : null}
        </div>

        <button type="submit" className="button" disabled={loading}>
          {loading ? "Searching…" : "Search"}
        </button>

        <select
          className="admin-filter"
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value as "" | "WORKER" | "EMPLOYER")}
        >
          <option value="">All Roles</option>
          <option value="WORKER">Workers Only</option>
          <option value="EMPLOYER">Employers Only</option>
        </select>

        <select
          className="admin-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as "" | "true" | "false")}
        >
          <option value="">All Statuses</option>
          <option value="true">Active Only</option>
          <option value="false">Inactive / Blocked</option>
        </select>

        <button
          type="button"
          className="button ghost"
          onClick={() => void load({ q: searchQuery, role: roleFilter, active: statusFilter })}
          title="Refresh table"
        >
          <RefreshCw size={14} style={{ marginRight: "4px" }} /> Refresh
        </button>

        <span className="admin-count" style={{ marginLeft: "auto", fontWeight: 600 }}>
          {visibleCodes.length} codes listed
        </span>
      </form>

      {/* Main Referral Codes Table */}
      {loading && codes.length === 0 ? (
        <div className="admin-loading">
          <div className="admin-loading-spinner" /> Loading referral codes with mobile numbers…
        </div>
      ) : visibleCodes.length === 0 ? (
        <div className="admin-empty" style={{ padding: "40px 20px", textAlign: "center" }}>
          <p style={{ fontSize: "1.1rem", fontWeight: 600, color: "#1e293b", marginBottom: "8px" }}>
            No referral codes found matching your criteria.
          </p>
          <p style={{ fontSize: "0.9rem", color: "#64748b", marginBottom: "16px" }}>
            {searchQuery ? `No account matched "${searchQuery}". Check the phone number format or try searching the Users directory.` : "No referral codes recorded yet."}
          </p>
          {searchQuery ? (
            <div style={{ display: "flex", gap: "8px", justifyContent: "center" }}>
              <button type="button" className="button ghost" onClick={handleClearSearch}>Clear Search</button>
              <Link href={`/admin/users?phone=${encodeURIComponent(searchQuery)}`} className="button">
                Search in Users Directory
              </Link>
            </div>
          ) : null}
        </div>
      ) : (
        <div className="table-wrap">
          <table className="data-table admin-table" style={{ width: "100%" }}>
            <thead>
              <tr>
                <th style={{ width: "170px" }}>Referral Code</th>
                <th style={{ width: "160px" }}>Mobile Number</th>
                <th>User / Account</th>
                <th style={{ width: "110px" }}>Role</th>
                <th style={{ width: "130px" }}>Referral Stats</th>
                <th style={{ width: "90px" }}>Status</th>
                <th style={{ width: "160px" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visibleCodes.map((row) => {
                const isCodeCopied = copiedCode === row.code;
                const isPhoneCopied = copiedPhone === row.phone;
                return (
                  <tr key={row.code}>
                    {/* Referral Code Column */}
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                        <code
                          className="admin-code"
                          style={{
                            fontWeight: 700,
                            fontSize: "0.95rem",
                            letterSpacing: "0.08em",
                            color: "#3730a3",
                            backgroundColor: "#e0e7ff",
                            padding: "3px 8px",
                            borderRadius: "4px"
                          }}
                        >
                          {row.code}
                        </code>
                        <button
                          type="button"
                          className="table-action"
                          style={{
                            padding: "3px 6px",
                            fontSize: "0.75rem",
                            display: "inline-flex",
                            alignItems: "center",
                            gap: "3px"
                          }}
                          onClick={() => copyToClipboard(row.code, false)}
                          title="Copy Referral Code"
                        >
                          {isCodeCopied ? <Check size={12} color="#059669" /> : <Copy size={12} />}
                          {isCodeCopied ? "Copied" : "Copy"}
                        </button>
                      </div>
                      <div style={{ marginTop: "4px" }}>
                        <a
                          href={row.shareUrl}
                          target="_blank"
                          rel="noreferrer"
                          style={{
                            fontSize: "0.75rem",
                            color: "#6366f1",
                            display: "inline-flex",
                            alignItems: "center",
                            gap: "2px",
                            textDecoration: "none"
                          }}
                          title="Open Referral Web Link"
                        >
                          <span>dutype.in/refer/{row.code}</span>
                          <ExternalLink size={10} />
                        </a>
                      </div>
                    </td>

                    {/* Mobile Number Column - Prominently Displayed */}
                    <td>
                      {row.phone ? (
                        <div>
                          <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                            <a
                              href={`tel:${row.phone}`}
                              style={{
                                fontWeight: 700,
                                fontSize: "0.95rem",
                                color: "#0f172a",
                                textDecoration: "none",
                                display: "inline-flex",
                                alignItems: "center",
                                gap: "4px"
                              }}
                              title="Click to call mobile"
                            >
                              <Phone size={13} color="#2563eb" />
                              <span>{row.phone}</span>
                            </a>
                            <button
                              type="button"
                              className="table-action"
                              style={{ padding: "2px 4px", fontSize: "0.7rem", lineHeight: 1 }}
                              onClick={() => copyToClipboard(row.phone, true)}
                              title="Copy Phone Number"
                            >
                              {isPhoneCopied ? <Check size={10} color="#059669" /> : <Copy size={10} />}
                            </button>
                          </div>
                          <span style={{ fontSize: "0.72rem", color: "#64748b" }}>Registered Mobile</span>
                        </div>
                      ) : (
                        <div style={{ color: "#94a3b8", fontSize: "0.85rem", fontStyle: "italic" }}>
                          No phone linked
                        </div>
                      )}
                    </td>

                    {/* User / Account Column */}
                    <td>
                      <div style={{ fontWeight: 600, fontSize: "0.9rem", color: "#0f172a" }}>
                        {row.name || (row.phone ? "Registered User" : "Anonymous")}
                      </div>
                      {row.businessName ? (
                        <div style={{ fontSize: "0.8rem", color: "#6366f1", fontWeight: 500 }}>
                          🏢 {row.businessName}
                        </div>
                      ) : null}
                      <div className="admin-cell-sub" style={{ display: "flex", gap: "6px", alignItems: "center" }}>
                        {row.area ? <span>📍 {row.area}</span> : null}
                        <code className="admin-code" style={{ fontSize: "0.72rem", color: "#64748b" }}>
                          {row.uid ? `${row.uid.slice(0, 10)}…` : "—"}
                        </code>
                      </div>
                    </td>

                    {/* Role Column */}
                    <td>
                      <span
                        className="status-pill"
                        style={{
                          backgroundColor: row.role === "EMPLOYER" ? "#f3e8ff" : "#e0f2fe",
                          color: row.role === "EMPLOYER" ? "#7e22ce" : "#0369a1",
                          fontWeight: 600,
                          fontSize: "0.75rem"
                        }}
                      >
                        {row.role === "EMPLOYER" ? "Employer" : "Worker"}
                      </span>
                    </td>

                    {/* Referral Stats Column */}
                    <td>
                      <div style={{ fontWeight: 600, color: "#059669", fontSize: "0.9rem" }}>
                        ₹{row.balanceRupees.toLocaleString("en-IN")}
                      </div>
                      <div className="admin-cell-sub">
                        {row.successfulReferrals > 0 ? (
                          <span style={{ fontWeight: 600, color: "#0f172a" }}>
                            🎯 {row.successfulReferrals} {row.successfulReferrals === 1 ? "referral" : "referrals"}
                          </span>
                        ) : (
                          <span>0 referrals</span>
                        )}
                      </div>
                      {row.lifetimeEarnedRupees > 0 ? (
                        <div style={{ fontSize: "0.72rem", color: "#64748b" }}>
                          ₹{row.lifetimeEarnedRupees} earned total
                        </div>
                      ) : null}
                    </td>

                    {/* Status Column */}
                    <td>
                      <span className={`status-pill ${row.active ? "success" : "danger"}`}>
                        {row.active ? "Active" : "Disabled"}
                      </span>
                    </td>

                    {/* Actions Column */}
                    <td>
                      <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                        <div style={{ display: "flex", gap: "4px" }}>
                          <Link
                            href={`/admin/referrals?lookup=${encodeURIComponent(row.code)}`}
                            className="table-action"
                            style={{ textDecoration: "none", display: "inline-block" }}
                            title="Inspect wallet ledger and payout history"
                          >
                            Wallet Ledger
                          </Link>
                          {row.phone ? (
                            <Link
                              href={`/admin/users?phone=${encodeURIComponent(row.phone)}`}
                              className="table-action"
                              style={{ textDecoration: "none", display: "inline-block" }}
                              title="Inspect full user profile"
                            >
                              Profile
                            </Link>
                          ) : null}
                        </div>
                        <button
                          type="button"
                          className={`table-action ${row.active ? "danger" : ""}`}
                          disabled={busyCode === row.code}
                          onClick={() => void toggleActive(row)}
                          style={{ width: "100%", fontSize: "0.72rem", padding: "2px 4px" }}
                        >
                          {row.active ? "Disable Code" : "Activate Code"}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination Footer */}
      {nextCursor && !searchQuery.trim() ? (
        <div className="admin-toolbar" style={{ justifyContent: "center" }}>
          <button
            type="button"
            className="button ghost"
            disabled={loading}
            onClick={() => void load({ role: roleFilter, active: statusFilter, after: nextCursor, append: true })}
          >
            {loading ? "Loading more…" : "Load more referral codes"}
          </button>
        </div>
      ) : null}
    </div>
  );
}
