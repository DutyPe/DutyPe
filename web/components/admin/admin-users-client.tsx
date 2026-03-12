"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, deleteDoc, doc, getDocs, limit, query, updateDoc } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { formatDate } from "@/lib/firebase/firestore-helpers";

type UserRow = {
  id: string;
  name?: string;
  phone?: string;
  email?: string;
  role?: string;
  activeRole?: string;
  roles?: string[];
  referralCode?: string;
  createdAt?: unknown;
};

export function AdminUsersClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [users, setUsers] = useState<UserRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("ALL");
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);

  async function loadUsers() {
    if (!services) {
      setError("Firebase is not configured.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const snapshot = await getDocs(query(collection(services.db, "users"), limit(500)));
      setUsers(
        snapshot.docs.map((item) => ({
          id: item.id,
          ...(item.data() as Omit<UserRow, "id">)
        }))
      );
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load users.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadUsers();
  }, [services]);

  async function handleDelete(userId: string) {
    if (!services) return;
    if (!window.confirm("Delete this user? This cannot be undone.")) return;

    try {
      setPendingDeleteId(userId);
      await deleteDoc(doc(services.db, "users", userId));
      setUsers((prev) => prev.filter((u) => u.id !== userId));
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete user.");
    } finally {
      setPendingDeleteId(null);
    }
  }

  async function handleRoleChange(userId: string, newRole: string) {
    if (!services) return;

    try {
      await updateDoc(doc(services.db, "users", userId), {
        role: newRole,
        activeRole: newRole
      });
      setUsers((prev) =>
        prev.map((u) =>
          u.id === userId ? { ...u, role: newRole, activeRole: newRole } : u
        )
      );
    } catch (roleError) {
      setError(roleError instanceof Error ? roleError.message : "Failed to update role.");
    }
  }

  const workerCount = users.filter((user) => hasRole(user, "WORKER")).length;
  const employerCount = users.filter((user) => hasRole(user, "EMPLOYER")).length;
  const dualRoleCount = users.filter((user) => {
    const roles = normalizedRoles(user);
    return roles.includes("WORKER") && roles.includes("EMPLOYER");
  }).length;

  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      !searchTerm ||
      (user.name ?? "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (user.phone ?? "").includes(searchTerm) ||
      (user.email ?? "").toLowerCase().includes(searchTerm.toLowerCase());

    const matchesRole =
      roleFilter === "ALL" ||
      (roleFilter === "DUAL"
        ? normalizedRoles(user).includes("WORKER") && normalizedRoles(user).includes("EMPLOYER")
        : hasRole(user, roleFilter));

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
        <div className="admin-stat-card compact">
          <strong>{dualRoleCount}</strong>
          <span>Dual-Role</span>
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
          <option value="DUAL">Dual-role</option>
        </select>
        <span className="admin-count">{filteredUsers.length} users</span>
      </div>

      {error && <div className="admin-error">{error}</div>}

      {filteredUsers.length === 0 ? (
        <div className="admin-empty"><p>No users match your filters.</p></div>
      ) : (
        <div className="table-wrap">
          <table className="data-table">
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
                    <strong>{user.name ?? "N/A"}</strong>
                    <div className="admin-cell-sub">{shortId(user.id)}</div>
                  </td>
                  <td>{user.phone ?? "N/A"}</td>
                  <td>{user.email ?? "N/A"}</td>
                  <td>
                    <select
                      className="admin-inline-select"
                      value={user.activeRole ?? user.role ?? ""}
                      onChange={(e) => handleRoleChange(user.id, e.target.value)}
                    >
                      <option value="WORKER">Worker</option>
                      <option value="EMPLOYER">Employer</option>
                    </select>
                  </td>
                  <td>
                    <code className="admin-code">{user.referralCode ?? "—"}</code>
                  </td>
                  <td>{formatDate(user.createdAt)}</td>
                  <td>
                    <button
                      type="button"
                      className="table-action danger"
                      onClick={() => void handleDelete(user.id)}
                      disabled={pendingDeleteId === user.id}
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
  return [...new Set(values.map((value) => value?.trim()).filter(Boolean) as string[])];
}

function hasRole(user: UserRow, targetRole: string) {
  return normalizedRoles(user).includes(targetRole);
}

function shortId(value: string) {
  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}
