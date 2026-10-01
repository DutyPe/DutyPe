"use client";

import { useCallback, useEffect, useState } from "react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";

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
};

type Payload = {
  users?: UserRow[];
  counts?: { workers: number; employers: number };
  nextCursor?: string | null;
  error?: string;
};

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" }) : "—";
}

/** Workers / employers, one page at a time; phone lookup; block, verify, rename, delete. */
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

  const load = useCallback(async (query: string, append: boolean) => {
    setLoading(true);
    setError(null);
    try {
      const response = await adminApiFetch(`/api/admin/users?${query}`, { cache: "no-store" });
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
      setUsers((current) => current.map((u) => (u.id === user.id ? { ...u, ...changes } as UserRow : u)));
    } catch (patchError) {
      setError(patchError instanceof Error ? patchError.message : "Failed to update user.");
    } finally {
      setBusyId(null);
    }
  }

  async function rename(user: UserRow) {
    const name = window.prompt("New name", user.name)?.trim();
    if (name && name !== user.name) await patch(user, { name });
  }

  async function remove(user: UserRow) {
    if (!window.confirm(`Delete ${user.name || user.phone} completely? Profile, jobs, applications and phone registration are removed. This cannot be undone.`)) return;
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
    ? users.filter((u) => `${u.name} ${u.businessName} ${u.phone} ${u.area} ${u.id}`.toLowerCase().includes(needle))
    : users;

  return (
    <div className="admin-section-stack">
      {error ? <div className="admin-error">{error}</div> : null}

      <div className="admin-stats-grid small">
        <div className="admin-stat-card compact"><strong>{counts?.workers ?? "…"}</strong><span>Workers</span></div>
        <div className="admin-stat-card compact"><strong>{counts?.employers ?? "…"}</strong><span>Employers</span></div>
        <div className="admin-stat-card compact"><strong>{users.length}</strong><span>Loaded</span></div>
      </div>

      <div className="admin-toolbar">
        <select className="admin-filter" value={role} onChange={(e) => setRole(e.target.value as Role)}>
          <option value="WORKER">Workers</option>
          <option value="EMPLOYER">Employers</option>
        </select>
        <input
          className="admin-search"
          placeholder="Find by mobile number"
          value={phone}
          inputMode="tel"
          onChange={(e) => setPhone(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter" && phone.trim()) void load(`phone=${encodeURIComponent(phone.trim())}`, false); }}
        />
        <button type="button" className="button ghost" disabled={loading}
          onClick={() => (phone.trim() ? load(`phone=${encodeURIComponent(phone.trim())}`, false) : load(`role=${role}`, false))}>
          {phone.trim() ? "Find" : "Refresh"}
        </button>
        <input className="admin-search" placeholder="Filter loaded rows…" value={filter} onChange={(e) => setFilter(e.target.value)} />
        <span className="admin-count">{visible.length} shown</span>
      </div>

      {loading && users.length === 0 ? (
        <div className="admin-loading"><div className="admin-loading-spinner" /> Loading users…</div>
      ) : visible.length === 0 ? (
        <div className="admin-empty">No users found.</div>
      ) : (
        <div className="table-wrap admin-users-table-wrap">
          <table className="data-table admin-users-table">
            <thead>
              <tr>
                <th>Name</th><th>Phone</th><th>Role</th><th>Area</th><th>Joined</th><th>Status</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div>{user.name || "—"}</div>
                    {user.businessName ? <div className="admin-cell-sub">{user.businessName}</div> : null}
                    <div className="admin-cell-sub"><code className="admin-code">{user.id}</code></div>
                  </td>
                  <td>{user.phone || "—"}</td>
                  <td>
                    {user.role === "EMPLOYER" ? `Employer${user.employerType ? ` · ${user.employerType.toLowerCase()}` : ""}` : "Worker"}
                    {user.skills.length ? <div className="admin-cell-sub">{user.skills.join(", ").toLowerCase()}</div> : null}
                  </td>
                  <td>{user.area || "—"}</td>
                  <td>{formatDate(user.createdAt)}</td>
                  <td>
                    <span className={`status-pill ${user.blocked ? "danger" : "success"}`}>{user.blocked ? "Blocked" : "Active"}</span>
                    {user.role === "EMPLOYER" && user.verified ? <div className="admin-cell-sub">Verified</div> : null}
                  </td>
                  <td>
                    <button type="button" className="table-action" disabled={busyId === user.id} onClick={() => void rename(user)}>Rename</button>
                    <button type="button" className="table-action" disabled={busyId === user.id}
                      onClick={() => void patch(user, { blocked: !user.blocked })}>{user.blocked ? "Unblock" : "Block"}</button>
                    {user.role === "EMPLOYER" ? (
                      <button type="button" className="table-action" disabled={busyId === user.id}
                        onClick={() => void patch(user, { verified: !user.verified })}>{user.verified ? "Unverify" : "Verify"}</button>
                    ) : null}
                    <button type="button" className="table-action danger" disabled={busyId === user.id} onClick={() => void remove(user)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {nextCursor && !phone.trim() ? (
        <div className="admin-toolbar">
          <button type="button" className="button ghost" disabled={loading}
            onClick={() => void load(`role=${role}&after=${encodeURIComponent(nextCursor)}`, true)}>
            {loading ? "Loading…" : "Load more"}
          </button>
        </div>
      ) : null}
    </div>
  );
}
