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
  BadgeCheck,
  MessageSquare,
  Star,
  X,
  ExternalLink
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
  const [khammamOnly, setKhammamOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  // Assign Partner Modal state
  const [assigningUser, setAssigningUser] = useState<UserRow | null>(null);
  const [assignCategories, setAssignCategories] = useState<string[]>(["ALL"]);
  const [assignExperience, setAssignExperience] = useState(2);
  const [assignArea, setAssignArea] = useState("Wyra Road, Khammam");
  const [assignWelcomeCredits, setAssignWelcomeCredits] = useState(100);
  const [assignBusy, setAssignBusy] = useState(false);
  const [assignSuccess, setAssignSuccess] = useState<string | null>(null);

  // Perks / Goodwill Support Modal state
  const [perksUser, setPerksUser] = useState<UserRow | null>(null);
  const [perkTab, setPerkTab] = useState<"WALLET" | "EMPLOYER" | "SERVICES">("WALLET");
  const [perkAmount, setPerkAmount] = useState(100);
  const [perkReason, setPerkReason] = useState("Goodwill support credit");
  const [freePostsCount, setFreePostsCount] = useState(1);
  const [contactUnlocksCount, setContactUnlocksCount] = useState(2);
  const [workerToUnlock, setWorkerToUnlock] = useState("");
  const [freePlatformBooking, setFreePlatformBooking] = useState(true);
  const [customCouponCode, setCustomCouponCode] = useState("");
  const [customCouponDiscount, setCustomCouponDiscount] = useState(50);
  const [perkBusy, setPerkBusy] = useState(false);
  const [perkSuccess, setPerkSuccess] = useState<string | null>(null);
  const [perkError, setPerkError] = useState<string | null>(null);

  const KHAMMAM_KEYWORDS = [
    "khammam",
    "wyra",
    "kaviraj",
    "vdo",
    "prakash",
    "nehru",
    "raheem",
    "manchukonda",
    "nagulavancha",
    "indira",
    "telangana"
  ];

  const handlePerkSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!perksUser) return;
    setPerkBusy(true);
    setPerkError(null);
    setPerkSuccess(null);

    let action = "CREDIT_WALLET";
    const payload: Record<string, unknown> = {
      userId: perksUser.id,
      reason: perkReason.trim() || "Admin support goodwill"
    };

    if (perkTab === "WALLET") {
      action = "CREDIT_WALLET";
      payload.amountRupees = perkAmount;
    } else if (perkTab === "EMPLOYER") {
      if (workerToUnlock.trim()) {
        action = "GRANT_CONTACT_UNLOCKS";
        payload.workerId = workerToUnlock.trim();
      } else if (freePostsCount > 0) {
        action = "GRANT_JOB_POSTS";
        payload.count = freePostsCount;
      } else {
        action = "GRANT_CONTACT_UNLOCKS";
        payload.count = contactUnlocksCount;
      }
    } else if (perkTab === "SERVICES") {
      if (customCouponCode.trim()) {
        action = "CREATE_CUSTOMER_COUPON";
        payload.code = customCouponCode.trim().toUpperCase();
        payload.discountRupees = customCouponDiscount;
        payload.targetPhone = perksUser.phone;
      } else {
        action = "GRANT_FREE_PLATFORM_BOOKING";
        payload.enabled = freePlatformBooking;
      }
    }

    payload.action = action;

    try {
      const res = await adminApiFetch("/api/admin/grant-perk", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      if (!res.ok || data.error) {
        setPerkError(data.error || "Failed to grant perk.");
      } else {
        setPerkSuccess(data.message || "Perk successfully granted!");
        // Refresh balance if wallet credit
        if (action === "CREDIT_WALLET" && typeof data.balanceRupees === "number") {
          setUsers((prev) =>
            prev.map((u) =>
              u.id === perksUser.id ? { ...u, referralBalanceRupees: data.balanceRupees } : u
            )
          );
        }
      }
    } catch (err) {
      setPerkError(err instanceof Error ? err.message : "Network error.");
    } finally {
      setPerkBusy(false);
    }
  };

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

  const handleAssignSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assigningUser) return;
    setAssignBusy(true);
    setAssignSuccess(null);
    try {
      const res = await adminApiFetch("/api/admin/assign-service-partner", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: assigningUser.id,
          name: assigningUser.name || "Worker",
          phone: assigningUser.phone,
          area: assignArea,
          categories: assignCategories,
          experienceYears: Number(assignExperience) || 1,
          welcomeCreditsPaise: Number(assignWelcomeCredits) * 100 || 10000,
          lat: 17.2473,
          lng: 80.1514
        })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Failed to assign service partner");
      setAssignSuccess(`Successfully assigned ${assigningUser.name || assigningUser.phone} as verified Service Partner!`);
      setTimeout(() => {
        setAssigningUser(null);
        setAssignSuccess(null);
      }, 1800);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : "Failed to assign partner");
    } finally {
      setAssignBusy(false);
    }
  };

  const needle = filter.trim().toLowerCase();
  let baseList = users;
  if (khammamOnly) {
    baseList = baseList.filter((u) => {
      const loc = `${u.area} ${u.businessName} ${u.name}`.toLowerCase();
      return KHAMMAM_KEYWORDS.some((kw) => loc.includes(kw));
    });
  }
  const visible = needle
    ? baseList.filter((u) =>
        `${u.name} ${u.businessName} ${u.phone} ${u.referralCode ?? ""} ${u.area} ${u.id}`
          .toLowerCase()
          .includes(needle)
      )
    : baseList;

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
      <div className="adm-toolbar" style={{ flexWrap: "wrap", gap: 10 }}>
        <div className="adm-search-wrap" style={{ flex: 1, minWidth: 260 }}>
          <Search className="adm-search-icon" />
          <input
            type="text"
            className="adm-input-search"
            placeholder="Filter loaded users by name, business, phone, referral code, or location..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          />
        </div>

        <button
          type="button"
          onClick={() => setKhammamOnly(!khammamOnly)}
          className="adm-btn adm-btn-sm"
          style={{
            background: khammamOnly ? "#059669" : "#fff",
            color: khammamOnly ? "#fff" : "#0f172a",
            border: khammamOnly ? "1px solid #059669" : "1px solid #cbd5e1",
            fontWeight: 700,
            display: "inline-flex",
            alignItems: "center",
            gap: 6
          }}
        >
          <MapPin style={{ width: 14, height: 14 }} />
          {khammamOnly ? "✓ Khammam Filter Active" : "📍 Filter Khammam Only"}
        </button>

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
                      <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                        <span style={{ fontWeight: 600, color: "var(--adm-text-main)" }}>
                          {user.phone || "—"}
                        </span>
                        {user.phone && (
                          <div style={{ display: "inline-flex", gap: 5 }}>
                            <a
                              href={`tel:${user.phone}`}
                              className="adm-btn-icon"
                              style={{
                                width: 22,
                                height: 22,
                                display: "inline-flex",
                                alignItems: "center",
                                justifyContent: "center",
                                background: "#eff6ff",
                                color: "#2563eb",
                                borderRadius: 4,
                                textDecoration: "none"
                              }}
                              title="Direct Phone Call"
                            >
                              <Phone style={{ width: 11, height: 11 }} />
                            </a>
                            <a
                              href={`https://wa.me/${user.phone.replace(/\D/g, "")}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="adm-btn-icon"
                              style={{
                                width: 22,
                                height: 22,
                                display: "inline-flex",
                                alignItems: "center",
                                justifyContent: "center",
                                background: "#f0fdf4",
                                color: "#16a34a",
                                borderRadius: 4,
                                textDecoration: "none"
                              }}
                              title="Chat on WhatsApp"
                            >
                              <MessageSquare style={{ width: 11, height: 11 }} />
                            </a>
                          </div>
                        )}
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
                          style={{ background: "#ecfdf5", color: "#059669", borderColor: "#a7f3d0" }}
                          disabled={isBusy}
                          onClick={() => {
                            setPerksUser(user);
                            setPerkTab(user.role === "EMPLOYER" ? "EMPLOYER" : "WALLET");
                            setPerkSuccess(null);
                            setPerkError(null);
                            setCustomCouponCode(`VIP${(user.phone || "").replace(/\D/g, "").slice(-4) || "50"}`);
                          }}
                          title="Give Bonus, Free Posts, Unlocks & Support Perks"
                        >
                          <Gift style={{ width: 13, height: 13, color: "#059669" }} />
                          Perks
                        </button>

                        {user.role === "WORKER" && (
                          <button
                            type="button"
                            className="adm-btn adm-btn-secondary adm-btn-sm"
                            style={{ background: "#f5f3ff", color: "#7c3aed", borderColor: "#ddd6fe" }}
                            disabled={isBusy}
                            onClick={() => {
                              setAssigningUser(user);
                              setAssignArea(user.area || "Wyra Road, Khammam");
                              setAssignCategories(user.skills && user.skills.length > 0 ? user.skills : ["ALL"]);
                            }}
                            title="Promote to Verified Service Partner"
                          >
                            <Star style={{ width: 13, height: 13, color: "#7c3aed" }} />
                            Partner
                          </button>
                        )}

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

      {/* Assign Service Partner Modal */}
      {assigningUser && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(15, 23, 42, 0.6)",
            backdropFilter: "blur(4px)",
            zIndex: 9999,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: 16
          }}
        >
          <div
            style={{
              background: "#fff",
              borderRadius: 12,
              boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)",
              maxWidth: 520,
              width: "100%",
              maxHeight: "90vh",
              overflowY: "auto",
              padding: 24,
              border: "1px solid #e2e8f0"
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
              <div>
                <h3 style={{ margin: 0, fontSize: "1.1rem", fontWeight: 700, color: "#0f172a", display: "flex", alignItems: "center", gap: 8 }}>
                  <Star style={{ width: 18, height: 18, color: "#7c3aed" }} />
                  Promote to Service Partner
                </h3>
                <p style={{ margin: "2px 0 0", fontSize: "0.8rem", color: "#64748b" }}>
                  Grant {assigningUser.name || "Worker"} instant APPROVED partner status in Khammam.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setAssigningUser(null)}
                style={{ background: "none", border: "none", cursor: "pointer", color: "#64748b" }}
              >
                <X style={{ width: 20, height: 20 }} />
              </button>
            </div>

            {assignSuccess ? (
              <div style={{ padding: "16px", background: "#ecfdf5", border: "1px solid #a7f3d0", borderRadius: 8, color: "#065f46", fontSize: "0.875rem", fontWeight: 600, textAlign: "center" }}>
                ✓ {assignSuccess}
              </div>
            ) : (
              <form onSubmit={handleAssignSubmit} style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                <div style={{ background: "#f8fafc", padding: 12, borderRadius: 8, fontSize: "0.825rem", color: "#334155" }}>
                  <div><strong>Name:</strong> {assigningUser.name || "Unnamed Worker"}</div>
                  <div><strong>Mobile:</strong> {assigningUser.phone || "No phone"}</div>
                  <div><strong>UID:</strong> <code>{assigningUser.id}</code></div>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "#334155", marginBottom: 4 }}>
                    Operational Area (defaults to Khammam)
                  </label>
                  <input
                    type="text"
                    className="adm-input"
                    value={assignArea}
                    onChange={(e) => setAssignArea(e.target.value)}
                    style={{ width: "100%", padding: "8px 12px", fontSize: "0.85rem" }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "#334155", marginBottom: 4 }}>
                    Experience (Years)
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="40"
                    className="adm-input"
                    value={assignExperience}
                    onChange={(e) => setAssignExperience(Number(e.target.value))}
                    style={{ width: "100%", padding: "8px 12px", fontSize: "0.85rem" }}
                  />
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "#334155", marginBottom: 4 }}>
                    Welcome Credits (₹)
                  </label>
                  <input
                    type="number"
                    min="0"
                    step="50"
                    className="adm-input"
                    value={assignWelcomeCredits}
                    onChange={(e) => setAssignWelcomeCredits(Number(e.target.value))}
                    style={{ width: "100%", padding: "8px 12px", fontSize: "0.85rem" }}
                  />
                  <span style={{ fontSize: "0.72rem", color: "#64748b" }}>Gives partner credits to accept service orders immediately.</span>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 600, color: "#334155", marginBottom: 6 }}>
                    Assigned Service Categories
                  </label>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 6, maxHeight: 150, overflowY: "auto", border: "1px solid #e2e8f0", padding: 8, borderRadius: 6 }}>
                    {[
                      "ALL",
                      "AC & Appliance Repair",
                      "Cleaning & Pest Control",
                      "Electrician",
                      "Plumber",
                      "Carpentry & Woodwork",
                      "Painting & Waterproofing",
                      "Driver & Logistics",
                      "Cook & Housekeeping",
                      "Helper & Construction"
                    ].map((cat) => {
                      const isChecked = assignCategories.includes(cat);
                      return (
                        <label key={cat} style={{ display: "flex", alignItems: "center", gap: 6, fontSize: "0.775rem", cursor: "pointer" }}>
                          <input
                            type="checkbox"
                            checked={isChecked}
                            onChange={(e) => {
                              if (cat === "ALL") {
                                setAssignCategories(e.target.checked ? ["ALL"] : []);
                              } else {
                                if (e.target.checked) {
                                  setAssignCategories([...assignCategories.filter((c) => c !== "ALL"), cat]);
                                } else {
                                  setAssignCategories(assignCategories.filter((c) => c !== cat));
                                }
                              }
                            }}
                          />
                          <span>{cat}</span>
                        </label>
                      );
                    })}
                  </div>
                </div>

                <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 8 }}>
                  <button
                    type="button"
                    className="adm-btn adm-btn-secondary adm-btn-sm"
                    onClick={() => setAssigningUser(null)}
                    disabled={assignBusy}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="adm-btn adm-btn-primary adm-btn-sm"
                    style={{ background: "#7c3aed", borderColor: "#7c3aed" }}
                    disabled={assignBusy}
                  >
                    {assignBusy ? "Promoting..." : "Confirm & Assign Partner"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Perks / Support Grant Modal */}
      {perksUser && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(15, 23, 42, 0.6)",
            backdropFilter: "blur(4px)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 9999,
            padding: 16
          }}
        >
          <div
            style={{
              background: "#fff",
              borderRadius: 16,
              maxWidth: 520,
              width: "100%",
              padding: 24,
              boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)",
              position: "relative",
              maxHeight: "90vh",
              overflowY: "auto"
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 16 }}>
              <div>
                <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: "#0f172a", display: "flex", alignItems: "center", gap: 8 }}>
                  <Gift style={{ width: 20, height: 20, color: "#059669" }} />
                  Support Perks & Compensation
                </h3>
                <p style={{ margin: "4px 0 0", fontSize: 13, color: "#64748b" }}>
                  Grant goodwill perks to <strong>{perksUser.name || perksUser.businessName || "User"}</strong> ({perksUser.phone || "No phone"}) • Role: <span style={{ fontWeight: 600, color: "#2563eb" }}>{perksUser.role}</span>
                </p>
              </div>
              <button
                type="button"
                onClick={() => setPerksUser(null)}
                style={{ background: "none", border: "none", cursor: "pointer", color: "#94a3b8", padding: 4 }}
              >
                <X style={{ width: 18, height: 18 }} />
              </button>
            </div>

            {/* Tab navigation */}
            <div style={{ display: "flex", gap: 8, borderBottom: "1px solid #e2e8f0", paddingBottom: 10, marginBottom: 16 }}>
              <button
                type="button"
                className={`adm-btn adm-btn-sm ${perkTab === "WALLET" ? "adm-btn-primary" : "adm-btn-secondary"}`}
                style={perkTab === "WALLET" ? { background: "#059669", borderColor: "#059669" } : {}}
                onClick={() => { setPerkTab("WALLET"); setPerkSuccess(null); setPerkError(null); }}
              >
                💰 Wallet Bonus
              </button>
              <button
                type="button"
                className={`adm-btn adm-btn-sm ${perkTab === "EMPLOYER" ? "adm-btn-primary" : "adm-btn-secondary"}`}
                style={perkTab === "EMPLOYER" ? { background: "#2563eb", borderColor: "#2563eb" } : {}}
                onClick={() => { setPerkTab("EMPLOYER"); setPerkSuccess(null); setPerkError(null); }}
              >
                🏢 Employer Perks
              </button>
              <button
                type="button"
                className={`adm-btn adm-btn-sm ${perkTab === "SERVICES" ? "adm-btn-primary" : "adm-btn-secondary"}`}
                style={perkTab === "SERVICES" ? { background: "#7c3aed", borderColor: "#7c3aed" } : {}}
                onClick={() => { setPerkTab("SERVICES"); setPerkSuccess(null); setPerkError(null); }}
              >
                🛠️ Services / Coupons
              </button>
            </div>

            {perkSuccess && (
              <div style={{ background: "#ecfdf5", border: "1px solid #a7f3d0", borderRadius: 8, padding: 12, marginBottom: 16, color: "#065f46", fontSize: 13, display: "flex", gap: 8, alignItems: "center" }}>
                <CheckCircle2 style={{ width: 18, height: 18, flexShrink: 0 }} />
                <span>{perkSuccess}</span>
              </div>
            )}

            {perkError && (
              <div style={{ background: "#fef2f2", border: "1px solid #fecaca", borderRadius: 8, padding: 12, marginBottom: 16, color: "#991b1b", fontSize: 13, display: "flex", gap: 8, alignItems: "center" }}>
                <AlertTriangle style={{ width: 18, height: 18, flexShrink: 0 }} />
                <span>{perkError}</span>
              </div>
            )}

            <form onSubmit={handlePerkSubmit}>
              {perkTab === "WALLET" && (
                <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                  <div style={{ background: "#f8fafc", borderRadius: 10, padding: 12, border: "1px solid #e2e8f0" }}>
                    <div style={{ fontSize: 12, color: "#64748b", marginBottom: 4 }}>Current Spendable / Withdrawable Balance</div>
                    <div style={{ fontSize: 20, fontWeight: 700, color: "#0f172a" }}>
                      ₹{(perksUser.referralBalanceRupees || 0).toLocaleString("en-IN")}
                    </div>
                    <div style={{ fontSize: 11, color: "#94a3b8", marginTop: 2 }}>
                      Credited directly into `referral_stats/{perksUser.id}`. Immediately appears on the user&apos;s mobile Referral &amp; Earn screen.
                    </div>
                  </div>

                  <div>
                    <label style={{ display: "block", fontSize: 13, fontWeight: 600, color: "#334155", marginBottom: 6 }}>
                      Bonus Amount to Credit (₹)
                    </label>
                    <input
                      type="number"
                      min={1}
                      max={10000}
                      className="adm-input"
                      value={perkAmount}
                      onChange={(e) => setPerkAmount(Number(e.target.value))}
                      style={{ width: "100%", fontSize: 15, fontWeight: 600 }}
                      required
                    />
                    <div style={{ display: "flex", gap: 6, marginTop: 8 }}>
                      {[50, 100, 200, 500].map((amt) => (
                        <button
                          key={amt}
                          type="button"
                          className="adm-btn adm-btn-secondary adm-btn-sm"
                          style={{ fontSize: 12, padding: "2px 8px" }}
                          onClick={() => setPerkAmount(amt)}
                        >
                          +₹{amt}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {perkTab === "EMPLOYER" && (
                <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                  <div>
                    <label style={{ display: "block", fontSize: 13, fontWeight: 600, color: "#334155", marginBottom: 6 }}>
                      Grant Free Job Postings
                    </label>
                    <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                      <input
                        type="number"
                        min={0}
                        max={50}
                        className="adm-input"
                        value={freePostsCount}
                        onChange={(e) => setFreePostsCount(Number(e.target.value))}
                        style={{ width: 120, fontSize: 15, fontWeight: 600 }}
                      />
                      <span style={{ fontSize: 13, color: "#64748b" }}>posts (added to `grantedFreeJobPosts`)</span>
                    </div>
                    <div style={{ display: "flex", gap: 6, marginTop: 8 }}>
                      {[1, 2, 5, 10].map((c) => (
                        <button
                          key={c}
                          type="button"
                          className="adm-btn adm-btn-secondary adm-btn-sm"
                          style={{ fontSize: 12, padding: "2px 8px" }}
                          onClick={() => setFreePostsCount(c)}
                        >
                          {c} Post{c > 1 ? "s" : ""}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div style={{ borderTop: "1px dashed #e2e8f0", paddingTop: 12 }}>
                    <label style={{ display: "block", fontSize: 13, fontWeight: 600, color: "#334155", marginBottom: 6 }}>
                      Unlock Specific Worker Contact (Optional)
                    </label>
                    <input
                      type="text"
                      className="adm-input"
                      placeholder="Enter Worker UID to unlock phone number"
                      value={workerToUnlock}
                      onChange={(e) => setWorkerToUnlock(e.target.value)}
                      style={{ width: "100%", fontSize: 13 }}
                    />
                    <div style={{ fontSize: 11, color: "#94a3b8", marginTop: 4 }}>
                      Leaves unlock permanently in `employer_profiles/{perksUser.id}/unlocks/{workerToUnlock}`.
                    </div>
                  </div>
                </div>
              )}

              {perkTab === "SERVICES" && (
                <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
                  <div style={{ background: "#faf5ff", border: "1px solid #f3e8ff", borderRadius: 10, padding: 12 }}>
                    <label style={{ display: "flex", alignItems: "center", gap: 8, cursor: "pointer", fontWeight: 600, color: "#6b21a8", fontSize: 14 }}>
                      <input
                        type="checkbox"
                        checked={freePlatformBooking}
                        onChange={(e) => setFreePlatformBooking(e.target.checked)}
                        style={{ width: 16, height: 16 }}
                      />
                      <span>100% Free Platform Fee Waiver on Home Services</span>
                    </label>
                    <div style={{ fontSize: 12, color: "#7e22ce", marginTop: 4, marginLeft: 24 }}>
                      Sets `freePlatformBooking: true` on user document. Completely eliminates the ₹9 - ₹19 platform fee on all standard service bookings!
                    </div>
                  </div>

                  <div style={{ borderTop: "1px dashed #e2e8f0", paddingTop: 12 }}>
                    <div style={{ fontSize: 13, fontWeight: 600, color: "#334155", marginBottom: 6 }}>
                      Or Issue Dedicated Customer Coupon
                    </div>
                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, marginBottom: 8 }}>
                      <div>
                        <label style={{ display: "block", fontSize: 11, color: "#64748b", marginBottom: 2 }}>Coupon Code</label>
                        <input
                          type="text"
                          className="adm-input"
                          value={customCouponCode}
                          onChange={(e) => setCustomCouponCode(e.target.value.toUpperCase())}
                          style={{ width: "100%", fontWeight: 700 }}
                        />
                      </div>
                      <div>
                        <label style={{ display: "block", fontSize: 11, color: "#64748b", marginBottom: 2 }}>Discount (₹)</label>
                        <input
                          type="number"
                          min={10}
                          max={1000}
                          className="adm-input"
                          value={customCouponDiscount}
                          onChange={(e) => setCustomCouponDiscount(Number(e.target.value))}
                          style={{ width: "100%" }}
                        />
                      </div>
                    </div>
                    <div style={{ fontSize: 11, color: "#94a3b8" }}>
                      Coupon will be locked exclusively to customer mobile {perksUser.phone || "this user"}.
                    </div>
                  </div>
                </div>
              )}

              <div style={{ marginTop: 14 }}>
                <label style={{ display: "block", fontSize: 13, fontWeight: 600, color: "#334155", marginBottom: 6 }}>
                  Reason / Support Note (Sent in Notification)
                </label>
                <input
                  type="text"
                  className="adm-input"
                  value={perkReason}
                  onChange={(e) => setPerkReason(e.target.value)}
                  placeholder="e.g. Compensation for cancelled job / Loyalty bonus"
                  style={{ width: "100%", fontSize: 13 }}
                  required
                />
              </div>

              <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", marginTop: 18 }}>
                <button
                  type="button"
                  className="adm-btn adm-btn-secondary adm-btn-sm"
                  onClick={() => setPerksUser(null)}
                  disabled={perkBusy}
                >
                  Close
                </button>
                <button
                  type="submit"
                  className="adm-btn adm-btn-primary adm-btn-sm"
                  style={{
                    background: perkTab === "WALLET" ? "#059669" : perkTab === "EMPLOYER" ? "#2563eb" : "#7c3aed",
                    borderColor: "transparent"
                  }}
                  disabled={perkBusy}
                >
                  {perkBusy ? "Applying..." : "Grant Perk & Send Notification ➔"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
