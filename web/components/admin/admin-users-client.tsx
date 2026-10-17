"use client";

import { useCallback, useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import {
  Users,
  Briefcase,
  Search,
  Phone,
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  Trash2,
  Edit3,
  Copy,
  Check,
  MapPin,
  RefreshCw,
  Gift,
  AlertTriangle,
  Building2,
  HardHat,
  BadgeCheck
} from "lucide-react";

type Role = "WORKER" | "EMPLOYER";

type UserRow = {
  id: string;
  role: Role;
  name: string;
  businessName: string;
  employerType: string;
  phone: string;
  area: string;
  photoUrl: string;
  skills: string[];
  blocked: boolean;
  verified: boolean;
  createdAt: string | null;
  referralCode?: string;
  referralBalanceRupees?: number;
  successfulReferrals?: number;
  aadhaarMasked?: string;
  aadhaarVerified?: boolean;
  verifiedLegalName?: string;
  verifiedDob?: string;
  verifiedGender?: string;
  verificationRef?: string;
  verificationSource?: string;
};

type Payload = {
  users?: UserRow[];
  counts?: { workers: number; employers: number };
  nextCursor?: string | null;
  error?: string;
};

function formatDate(value: string | null) {
  return value
    ? new Date(value).toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric"
      })
    : "—";
}

export function AdminUsersClient() {
  const [role, setRole] = useState<Role>("WORKER");
  const [users, setUsers] = useState<UserRow[]>([]);
  const [counts, setCounts] = useState<{ workers: number; employers: number } | null>(null);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [phone, setPhone] = useState("");
  const [filter, setFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  const load = useCallback(async (queryParam: string, append: boolean) => {
    setLoading(true);
    setError(null);
    try {
      const response = await adminApiFetch(`/api/admin/users?${queryParam}`, { cache: "no-store" });
      const payload = (await response.json()) as Payload;
      if (!response.ok) throw new Error(payload.error || "Failed to load users.");
      setUsers((current) => (append ? [...current, ...(payload.users ?? [])] : payload.users ?? []));
      setCounts(payload.counts ?? null);
      setNextCursor(payload.nextCursor ?? null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load users.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load(`role=${role}`, false);
  }, [role, load]);

  async function patch(user: UserRow, changes: Record<string, unknown>) {
    setBusyId(user.id);
    setError(null);
    try {
      const response = await adminApiFetch("/api/admin/users", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: user.id, role: user.role, ...changes })
      });
      const payload = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(payload.error || "Failed to update user.");
      setUsers((current) => current.map((u) => (u.id === user.id ? ({ ...u, ...changes } as UserRow) : u)));
    } catch (patchError) {
      setError(patchError instanceof Error ? patchError.message : "Failed to update user.");
    } finally {
      setBusyId(null);
    }
  }

  async function rename(user: UserRow) {
    const name = window.prompt("Enter updated name for user:", user.name)?.trim();
    if (name && name !== user.name) await patch(user, { name });
  }

  async function remove(user: UserRow) {
    if (
      !window.confirm(
        `Delete ${user.name || user.phone} completely? Profile, jobs, applications, and phone registration will be permanently purged. This cannot be undone.`
      )
    )
      return;
    setBusyId(user.id);
    try {
      const response = await adminApiFetch("/api/admin/users", {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: user.id })
      });
      const payload = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(payload.error || "Failed to delete user.");
      setUsers((current) => current.filter((u) => u.id !== user.id));
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete user.");
    } finally {
      setBusyId(null);
    }
  }

  const needle = filter.trim().toLowerCase();
  const visible = needle
    ? users.filter((u) =>
        `${u.name} ${u.businessName} ${u.phone} ${u.referralCode ?? ""} ${u.area} ${u.id}`
          .toLowerCase()
          .includes(needle)
      )
    : users;

  const copyText = (text: string, type: "id" | "code") => {
    navigator.clipboard.writeText(text);
    if (type === "id") {
      setCopiedId(text);
      setTimeout(() => setCopiedId(null), 2000);
    } else {
      setCopiedCode(text);
      setTimeout(() => setCopiedCode(null), 2000);
    }
  };

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

  return (
    <div className="adm-stack">
      {/* Toast Error Alert */}
      {error && (
        <div className="adm-card" style={{ borderLeft: "4px solid #ef4444", background: "#fef2f2", padding: "12px 18px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, color: "#b91c1c", fontSize: "0.875rem", fontWeight: 600 }}>
            <AlertTriangle style={{ width: 18, height: 18, color: "#ef4444", flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        </div>
      )}

      {/* Header Row */}
      <div className="adm-header-row">
        <div>
          <h1 className="adm-page-title">Users Directory</h1>
          <p className="adm-page-subtitle">
            Manage worker and employer accounts, phone records, referral metrics, and moderation controls.
          </p>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button
            type="button"
            className="adm-btn adm-btn-secondary"
            onClick={() => {
              if (phone.trim()) void load(`phone=${encodeURIComponent(phone.trim())}`, false);
              else void load(`role=${role}`, false);
            }}
            disabled={loading}
          >
            <RefreshCw style={{ width: 15, height: 15 }} />
            Refresh
          </button>
        </div>
      </div>

      {/* Top Metrics Grid */}
      <div className="adm-metrics-grid">
        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Total Workers</span>
            <div className="adm-metric-icon" style={{ background: "#eff6ff", color: "#2563eb" }}>
              <HardHat style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{counts?.workers?.toLocaleString("en-IN") ?? "…"}</div>
          <div className="adm-metric-sub">
            <span>verified & active workers</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Total Employers</span>
            <div className="adm-metric-icon" style={{ background: "#f5f3ff", color: "#7c3aed" }}>
              <Building2 style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{counts?.employers?.toLocaleString("en-IN") ?? "…"}</div>
          <div className="adm-metric-sub">
            <span>businesses & direct hirers</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Currently Loaded</span>
            <div className="adm-metric-icon">
              <Users style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{users.length}</div>
          <div className="adm-metric-sub">
            <span>{visible.length} matching active search</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Blocked Accounts</span>
            <div className="adm-metric-icon" style={{ background: "#fef2f2", color: "#ef4444" }}>
              <ShieldAlert style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value" style={{ color: users.filter((u) => u.blocked).length > 0 ? "#dc2626" : "var(--adm-text-main)" }}>
            {users.filter((u) => u.blocked).length}
          </div>
          <div className="adm-metric-sub">
            <span>moderated in this view</span>
          </div>
        </div>
      </div>

      {/* Segmented Role Switcher */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 12 }}>
        <div className="adm-segmented">
          <button
            type="button"
            className={`adm-segmented-btn ${role === "WORKER" ? "active" : ""}`}
            onClick={() => {
              setPhone("");
              setRole("WORKER");
            }}
          >
            👷 Workers ({counts?.workers ?? "…"})
          </button>
          <button
            type="button"
            className={`adm-segmented-btn ${role === "EMPLOYER" ? "active" : ""}`}
            onClick={() => {
              setPhone("");
              setRole("EMPLOYER");
            }}
          >
            🏢 Employers ({counts?.employers ?? "…"})
          </button>
        </div>

        {/* Mobile Number Direct Lookup Form */}
        <div style={{ display: "flex", gap: 8 }}>
          <input
            className="adm-input"
            style={{ width: 220, padding: "8px 12px", fontSize: "0.85rem" }}
            placeholder="Exact phone e.g. 93906..."
            value={phone}
            inputMode="tel"
            onChange={(e) => setPhone(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && phone.trim()) {
                void load(`phone=${encodeURIComponent(phone.trim())}`, false);
              }
            }}
          />
          <button
            type="button"
            className="adm-btn adm-btn-primary adm-btn-sm"
            disabled={loading || !phone.trim()}
            onClick={() => void load(`phone=${encodeURIComponent(phone.trim())}`, false)}
          >
            Find Phone
          </button>
          {phone.trim() && (
            <button
              type="button"
              className="adm-btn adm-btn-ghost adm-btn-sm"
              onClick={() => {
                setPhone("");
                void load(`role=${role}`, false);
              }}
            >
              Clear
            </button>
          )}
        </div>
      </div>

      {/* Client-side Search / Filter Bar */}
      <div className="adm-toolbar">
        <div className="adm-search-wrap">
          <Search className="adm-search-icon" />
          <input
            type="text"
            className="adm-input-search"
            placeholder="Filter loaded users by name, business, phone, referral code, or location..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          />
        </div>
        <span style={{ fontSize: "0.825rem", color: "var(--adm-text-muted)", marginLeft: "auto", fontWeight: 500 }}>
          Showing <strong>{visible.length}</strong> of {users.length} users
        </span>
      </div>

      {/* User Table */}
      {loading && users.length === 0 ? (
        <div className="adm-card" style={{ padding: 32, display: "flex", flexDirection: "column", gap: 12 }}>
          <div className="adm-skeleton" style={{ height: 28, width: "30%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
        </div>
      ) : visible.length === 0 ? (
        <div className="adm-card" style={{ padding: "48px 24px", textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", gap: 12 }}>
          <Users style={{ width: 44, height: 44, color: "var(--adm-text-subtle)" }} />
          <h3 style={{ margin: 0, fontSize: "1.1rem", color: "var(--adm-text-main)" }}>No users found</h3>
          <p style={{ margin: 0, fontSize: "0.875rem", color: "var(--adm-text-muted)", maxWidth: 360 }}>
            {phone.trim()
              ? `No user matched mobile number ${phone.trim()}.`
              : "No records matched your active search query."}
          </p>
        </div>
      ) : (
        <div className="adm-table-wrap">
          <table className="adm-table">
            <thead>
              <tr>
                <th>User Identity</th>
                <th>Phone Number</th>
                <th>Role & Details</th>
                <th>Referral Details</th>
                <th>Location</th>
                <th>Registered</th>
                <th>Status</th>
                <th style={{ textAlign: "right" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((user) => {
                const isIdCopied = copiedId === user.id;
                const isCodeCopied = copiedCode === user.referralCode;
                const isBusy = busyId === user.id;

                return (
                  <tr key={user.id} style={{ opacity: isBusy ? 0.5 : 1 }}>
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div
                          style={{
                            width: 36,
                            height: 36,
                            borderRadius: "50%",
                            background: user.role === "EMPLOYER" ? "#f5f3ff" : "#eff6ff",
                            color: user.role === "EMPLOYER" ? "#7c3aed" : "#2563eb",
                            border: `2px solid ${user.role === "EMPLOYER" ? "#ddd6fe" : "#bfdbfe"}`,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontWeight: 700,
                            fontSize: "0.8rem",
                            flexShrink: 0
                          }}
                        >
                          {getInitials(user.name)}
                        </div>
                        <div>
                          <div style={{ fontWeight: 600, color: "var(--adm-text-main)" }}>
                            {user.name || "Unnamed User"}
                          </div>
                          {user.businessName && (
                            <div style={{ fontSize: "0.775rem", color: "var(--adm-text-muted)" }}>
                              {user.businessName}
                            </div>
                          )}
                          <div style={{ display: "flex", alignItems: "center", gap: 4, marginTop: 2 }}>
                            <code style={{ fontSize: "0.7rem", color: "var(--adm-text-subtle)", background: "var(--adm-surface-subtle)", padding: "1px 4px", borderRadius: 3 }}>
                              {user.id.slice(0, 10)}...
                            </code>
                            <button
                              type="button"
                              className="adm-btn-icon"
                              style={{ width: 20, height: 20, border: "none" }}
                              title="Copy UID"
                              onClick={() => copyText(user.id, "id")}
                            >
                              {isIdCopied ? (
                                <Check style={{ width: 11, height: 11, color: "#10b981" }} />
                              ) : (
                                <Copy style={{ width: 11, height: 11 }} />
                              )}
                            </button>
                          </div>
                        </div>
                      </div>
                    </td>

                    <td>
                      <div style={{ fontWeight: 600, color: "var(--adm-text-main)" }}>
                        {user.phone || "—"}
                      </div>
                    </td>

                    <td>
                      <div style={{ display: "flex", flexDirection: "column", gap: 4, alignItems: "flex-start" }}>
                        <span className={`adm-badge ${user.role === "EMPLOYER" ? "adm-badge-purple" : "adm-badge-blue"}`}>
                          {user.role === "EMPLOYER" ? "Employer" : "Worker"}
                          {user.employerType ? ` · ${user.employerType}` : ""}
                        </span>
                        {user.skills && user.skills.length > 0 && (
                          <div style={{ fontSize: "0.72rem", color: "var(--adm-text-muted)" }}>
                            {user.skills.slice(0, 3).join(", ")}
                            {user.skills.length > 3 ? ` +${user.skills.length - 3}` : ""}
                          </div>
                        )}
                        {user.role === "WORKER" && (
                          user.aadhaarMasked ? (
                            <div style={{ display: "flex", flexDirection: "column", gap: 2, marginTop: 4 }}>
                              <span style={{ display: "inline-flex", alignItems: "center", gap: 3, fontSize: "0.72rem", fontWeight: 600, color: "#15803d", background: "#f0fdf4", border: "1px solid #bbf7d0", padding: "1px 6px", borderRadius: 4 }}>
                                <ShieldCheck style={{ width: 12, height: 12, color: "#16a34a" }} />
                                e-KYC: {user.aadhaarMasked}
                              </span>
                              {user.verifiedLegalName && (
                                <span style={{ fontSize: "0.68rem", color: "#166534", fontWeight: 500 }}>
                                  UIDAI Name: {user.verifiedLegalName}
                                </span>
                              )}
                              {user.verificationRef && (
                                <span style={{ fontSize: "0.65rem", color: "#64748b" }} title={user.verificationRef}>
                                  Ref: {user.verificationRef.length > 20 ? `${user.verificationRef.slice(0, 18)}...` : user.verificationRef}
                                </span>
                              )}
                            </div>
                          ) : (
                            <span style={{ display: "inline-flex", alignItems: "center", gap: 3, fontSize: "0.72rem", color: "#b45309", background: "#fffbeb", border: "1px solid #fef3c7", padding: "1px 6px", borderRadius: 4, marginTop: 2 }}>
                              <ShieldAlert style={{ width: 12, height: 12, color: "#d97706" }} />
                              Aadhaar: Pending
                            </span>
                          )
                        )}
                      </div>
                    </td>

                    <td>
                      {user.referralCode ? (
                        <div style={{ display: "flex", flexDirection: "column", gap: 3 }}>
                          <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                            <code style={{ fontWeight: 700, fontSize: "0.775rem", color: "#4338ca", background: "#e0e7ff", padding: "2px 6px", borderRadius: 4 }}>
                              {user.referralCode}
                            </code>
                            <button
                              type="button"
                              className="adm-btn-icon"
                              style={{ width: 20, height: 20, border: "none" }}
                              title="Copy Referral Code"
                              onClick={() => copyText(user.referralCode!, "code")}
                            >
                              {isCodeCopied ? (
                                <Check style={{ width: 11, height: 11, color: "#10b981" }} />
                              ) : (
                                <Copy style={{ width: 11, height: 11 }} />
                              )}
                            </button>
                          </div>
                          {((user.successfulReferrals ?? 0) > 0 || (user.referralBalanceRupees ?? 0) > 0) && (
                            <span style={{ fontSize: "0.72rem", color: "#059669", fontWeight: 600 }}>
                              ₹{user.referralBalanceRupees ?? 0} · {user.successfulReferrals ?? 0} refs
                            </span>
                          )}
                        </div>
                      ) : (
                        <span style={{ color: "var(--adm-text-subtle)", fontSize: "0.8rem" }}>—</span>
                      )}
                    </td>

                    <td>
                      <span style={{ display: "inline-flex", alignItems: "center", gap: 4, fontSize: "0.8rem", color: "var(--adm-text-muted)" }}>
                        <MapPin style={{ width: 13, height: 13, color: "var(--adm-text-subtle)" }} />
                        {user.area || "—"}
                      </span>
                    </td>

                    <td style={{ fontSize: "0.8rem", color: "var(--adm-text-muted)", whiteSpace: "nowrap" }}>
                      {formatDate(user.createdAt)}
                    </td>

                    <td>
                      <div style={{ display: "flex", flexDirection: "column", gap: 4, alignItems: "flex-start" }}>
                        <span className={`adm-badge ${user.blocked ? "adm-badge-rose" : "adm-badge-emerald"}`}>
                          {user.blocked ? "Blocked" : "Active"}
                        </span>
                        {user.role === "EMPLOYER" && user.verified && (
                          <span className="adm-badge adm-badge-blue" style={{ fontSize: "0.68rem" }}>
                            <BadgeCheck style={{ width: 11, height: 11 }} />
                            Verified
                          </span>
                        )}
                      </div>
                    </td>

                    <td style={{ textAlign: "right" }}>
                      <div style={{ display: "inline-flex", gap: 5 }}>
                        <button
                          type="button"
                          className="adm-btn adm-btn-secondary adm-btn-sm"
                          disabled={isBusy}
                          onClick={() => void rename(user)}
                          title="Rename User"
                        >
                          <Edit3 style={{ width: 13, height: 13 }} />
                          Rename
                        </button>

                        <button
                          type="button"
                          className={`adm-btn adm-btn-sm ${user.blocked ? "adm-btn-primary" : "adm-btn-secondary"}`}
                          disabled={isBusy}
                          onClick={() => void patch(user, { blocked: !user.blocked })}
                          title={user.blocked ? "Unblock User" : "Block User"}
                        >
                          {user.blocked ? (
                            <>
                              <ShieldCheck style={{ width: 13, height: 13 }} />
                              Unblock
                            </>
                          ) : (
                            <>
                              <ShieldAlert style={{ width: 13, height: 13 }} />
                              Block
                            </>
                          )}
                        </button>

                        {user.role === "EMPLOYER" && (
                          <button
                            type="button"
                            className="adm-btn adm-btn-secondary adm-btn-sm"
                            disabled={isBusy}
                            onClick={() => void patch(user, { verified: !user.verified })}
                            title={user.verified ? "Remove Verification" : "Mark Verified"}
                          >
                            <BadgeCheck style={{ width: 13, height: 13, color: user.verified ? "#2563eb" : undefined }} />
                            {user.verified ? "Unverify" : "Verify"}
                          </button>
                        )}

                        <button
                          type="button"
                          className="adm-btn-icon"
                          style={{ color: "#ef4444" }}
                          disabled={isBusy}
                          onClick={() => void remove(user)}
                          title="Delete User"
                        >
                          <Trash2 style={{ width: 14, height: 14 }} />
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
      {nextCursor && !phone.trim() ? (
        <div style={{ display: "flex", justifyContent: "center", padding: "12px 0" }}>
          <button
            type="button"
            className="adm-btn adm-btn-secondary"
            disabled={loading}
            onClick={() => void load(`role=${role}&after=${encodeURIComponent(nextCursor)}`, true)}
          >
            {loading ? "Loading additional users…" : "Load More Users"}
          </button>
        </div>
      ) : null}
    </div>
  );
}
