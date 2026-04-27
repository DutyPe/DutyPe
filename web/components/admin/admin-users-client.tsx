"use client";

import { useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDate } from "@/lib/firebase/firestore-helpers";

type UserRow = {
  id: string;
  fullName?: string;
  name?: string;
  displayName?: string;
  phone?: string;
  phoneNumber?: string;
  contactPhone?: string;
  email?: string;
  contactEmail?: string;
  role?: string;
  activeRole?: string;
  roles?: string[];
  referralCode?: string;
  referral_code?: string;
  isBanned?: boolean;
  isVerified?: boolean;
  createdAt?: unknown;
  updatedAt?: unknown;
  lastLoginAt?: unknown;
};

export function AdminUsersClient() {
  const [users, setUsers] = useState<UserRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("ALL");
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [pendingSaveId, setPendingSaveId] = useState<string | null>(null);
  // The live `users` collection only stores a single `role` field. We
  // keep the draft minimal: name, phone, role.
  const [editDrafts, setEditDrafts] = useState<
    Record<string, { fullName: string; phone: string; role: string }>
  >({});

  function createDraftMap(rows: UserRow[]) {
    return rows.reduce<
      Record<string, { fullName: string; phone: string; role: string }>
    >((acc, row) => {
      const current = primaryRole(row);
      acc[row.id] = {
        fullName: displayUserName(row),
        phone: row.phone ?? "",
        role: current === "EMPLOYER" ? "EMPLOYER" : "WORKER"
      };
      return acc;
    }, {});
  }

  async function loadUsers() {
    try {
      setLoading(true);
      const response = await adminApiFetch("/api/admin/users", {
        cache: "no-store"
      });

      const payload = (await response.json()) as {
        users?: UserRow[];
        error?: string;
      };

      if (!response.ok) {
        throw new Error(payload.error || "Failed to load users.");
      }

      const nextUsers = payload.users ?? [];
      setUsers(nextUsers);
      setEditDrafts(createDraftMap(nextUsers));
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load users.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadUsers();
  }, []);

  async function handleDelete(userId: string) {
    if (!window.confirm("Delete this user? This cannot be undone.")) return;

    try {
      setPendingDeleteId(userId);
      const response = await adminApiFetch("/api/admin/users", {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ userId })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) {
        throw new Error(payload.error || "Failed to delete user.");
      }

      setUsers((prev) => prev.filter((u) => u.id !== userId));
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete user.");
    } finally {
      setPendingDeleteId(null);
    }
  }

  function setDraftField(
    userId: string,
    key: "fullName" | "phone" | "role",
    value: string
  ) {
    setEditDrafts((prev) => ({
      ...prev,
      [userId]: {
        fullName: prev[userId]?.fullName ?? "",
        phone: prev[userId]?.phone ?? "",
        role: prev[userId]?.role ?? "WORKER",
        [key]: value
      }
    }));
  }

  async function handleSave(userId: string) {
    const draft = editDrafts[userId];
    if (!draft) {
      return;
    }

    try {
      setPendingSaveId(userId);
      const response = await adminApiFetch("/api/admin/users", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          userId,
          newRole: draft.role,
          fullName: draft.fullName,
          phone: draft.phone
        })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) {
        throw new Error(payload.error || "Failed to update user.");
      }

      setUsers((prev) =>
        prev.map((u) =>
          u.id === userId
            ? {
                ...u,
                fullName: draft.fullName,
                name: draft.fullName,
                phone: draft.phone,
                role: draft.role,
                activeRole: draft.role
              }
            : u
        )
      );
      setError(null);
    } catch (roleError) {
      setError(roleError instanceof Error ? roleError.message : "Failed to update user.");
    } finally {
      setPendingSaveId(null);
    }
  }

  async function handleToggleBan(user: UserRow) {
    const next = !user.isBanned;
    const reason = next ? window.prompt("Reason (optional):") || "" : "";
    try {
      const response = await adminApiFetch("/api/admin/users", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: user.id, isBanned: next, banReason: reason })
      });
      const payload = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(payload.error || "Failed.");
      setUsers((prev) => prev.map((u) => (u.id === user.id ? { ...u, isBanned: next } : u)));
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to update user.");
    }
  }

  async function handleToggleVerify(user: UserRow) {
    const next = !user.isVerified;
    try {
      const response = await adminApiFetch("/api/admin/users", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: user.id, isVerified: next })
      });
      const payload = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(payload.error || "Failed.");
      setUsers((prev) => prev.map((u) => (u.id === user.id ? { ...u, isVerified: next } : u)));
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to update user.");
    }
  }

  const workerCount = users.filter((user) => primaryRole(user) === "WORKER").length;
  const employerCount = users.filter((user) => primaryRole(user) === "EMPLOYER").length;

  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      !searchTerm ||
      (displayUserName(user) ?? "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (user.phone ?? "").includes(searchTerm) ||
      (user.email ?? "").toLowerCase().includes(searchTerm.toLowerCase());

    const matchesRole = roleFilter === "ALL" || primaryRole(user) === roleFilter;

    return matchesSearch && matchesRole;
  });

  if (loading) {
    return (
      <div className="admin-loading">
        <div className="admin-loading-spinner" />
        <p>Loading users...</p>
      </div>
    );
  }

  if (error && users.length === 0) {
    return <div className="admin-empty"><p>Unable to load users. {error}</p></div>;
  }

  return (
    <>
      {/* Stats strip */}
      <div className="admin-stats-grid small">
        <div className="admin-stat-card compact">
          <strong>{users.length}</strong>
          <span>Total Users</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{workerCount}</strong>
          <span>Workers</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{employerCount}</strong>
          <span>Employers</span>
        </div>
      </div>

      {/* Toolbar */}
      <div className="admin-toolbar">
        <input
          type="text"
          className="admin-search"
          placeholder="Search by name, phone, or email..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
        <select
          className="admin-filter"
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
        >
          <option value="ALL">All roles</option>
          <option value="WORKER">Workers</option>
          <option value="EMPLOYER">Employers</option>
        </select>
        <span className="admin-count">{filteredUsers.length} users</span>
      </div>

      {error && <div className="admin-error">{error}</div>}

      {filteredUsers.length === 0 ? (
        <div className="admin-empty"><p>No users match your filters.</p></div>
      ) : (
        // Cap the table height so it scrolls vertically once the list grows.
        // The thead is sticky inside `.admin-users-table-wrap` so column
        // headers stay visible while the admin scrolls through hundreds of
        // rows. Horizontal overflow stays available for narrow viewports.
        <div className="table-wrap admin-users-table-wrap">
          <table className="data-table admin-users-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Phone</th>
                <th>Email</th>
                <th>Role</th>
                <th>Referral Code</th>
                <th>Joined</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td>
                    <input
                      type="text"
                      className="admin-search"
                      value={editDrafts[user.id]?.fullName ?? displayUserName(user)}
                      onChange={(e) => setDraftField(user.id, "fullName", e.target.value)}
                      placeholder="Full name"
                    />
                    <div className="admin-cell-sub">{shortId(user.id)}</div>
                  </td>
                  <td>
                    <input
                      type="text"
                      className="admin-search"
                      value={editDrafts[user.id]?.phone ?? user.phone ?? ""}
                      onChange={(e) => setDraftField(user.id, "phone", e.target.value)}
                      placeholder="Phone"
                    />
                  </td>
                  <td>{user.email || "Not provided"}</td>
                  <td>
                    {/* Single-role: the live `users` doc has only one `role`. */}
                    <select
                      className="admin-inline-select"
                      value={editDrafts[user.id]?.role ?? primaryRole(user)}
                      onChange={(e) => setDraftField(user.id, "role", e.target.value)}
                    >
                      <option value="WORKER">Worker</option>
                      <option value="EMPLOYER">Employer</option>
                    </select>
                  </td>
                  <td>
                    <code className="admin-code">{user.referralCode || shortId(user.id)}</code>
                  </td>
                  <td>{formatDate(user.createdAt) !== "N/A" ? formatDate(user.createdAt) : "Recently active"}</td>
                  <td>
                    <button
                      type="button"
                      className="table-action"
                      onClick={() => void handleSave(user.id)}
                      disabled={pendingSaveId === user.id}
                    >
                      {pendingSaveId === user.id ? "..." : "Save"}
                    </button>
                    <button
                      type="button"
                      className="table-action"
                      onClick={() => void handleToggleVerify(user)}
                      title={user.isVerified ? "Remove verified badge" : "Mark as verified"}
                    >
                      {user.isVerified ? "Unverify" : "Verify"}
                    </button>
                    <button
                      type="button"
                      className={user.isBanned ? "table-action" : "table-action danger"}
                      onClick={() => void handleToggleBan(user)}
                    >
                      {user.isBanned ? "Unban" : "Ban"}
                    </button>
                    <button
                      type="button"
                      className="table-action danger"
                      onClick={() => void handleDelete(user.id)}
                      disabled={pendingDeleteId === user.id || pendingSaveId === user.id}
                    >
                      {pendingDeleteId === user.id ? "..." : "Delete"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function normalizedRoles(user: UserRow) {
  const values = [user.activeRole, user.role, ...(Array.isArray(user.roles) ? user.roles : [])];
  return [...new Set(values.map((value) => value?.trim().toUpperCase()).filter(Boolean) as string[])];
}

function primaryRole(user: UserRow) {
  const direct = (user.role ?? user.activeRole ?? "").trim().toUpperCase();
  if (direct === "WORKER" || direct === "EMPLOYER" || direct === "ADMIN") return direct;
  const fallback = normalizedRoles(user)[0];
  return fallback === "EMPLOYER" ? "EMPLOYER" : "WORKER";
}

function shortId(value: string) {
  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}

function displayUserName(user: UserRow) {
  return user.fullName || user.name || user.displayName || "";
}
