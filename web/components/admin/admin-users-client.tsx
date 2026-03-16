"use client";

import { useEffect, useMemo, useState } from "react";
import {
  collection,
  deleteDoc,
  doc,
  getDocs,
  limit,
  query,
  updateDoc,
  writeBatch
} from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { normalizeUserRecord } from "@/lib/firebase/admin-normalizers";
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
  createdAt?: unknown;
  updatedAt?: unknown;
  lastLoginAt?: unknown;
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
      const [usersSnapshot, referralCodesSnapshot, workerProfilesSnapshot, employerProfilesSnapshot] =
        await Promise.all([
          getDocs(query(collection(services.db, "users"), limit(500))),
          getDocs(query(collection(services.db, "referral_codes"), limit(1000))),
          getDocs(query(collection(services.db, "worker_profiles"), limit(1000))),
          getDocs(query(collection(services.db, "employer_profiles"), limit(1000)))
        ]);

      const referralCodeByUserId = new Map<string, string>();
      referralCodesSnapshot.forEach((item) => {
        const raw = item.data() as Record<string, unknown>;
        const userId = typeof raw.userId === "string" ? raw.userId : "";
        const code = typeof raw.code === "string" && raw.code.trim() ? raw.code.trim() : item.id;

        if (userId && code) {
          referralCodeByUserId.set(userId, code);
        }
      });

      const workerProfileById = new Map<string, Record<string, unknown>>();
      workerProfilesSnapshot.forEach((item) => {
        workerProfileById.set(item.id, item.data() as Record<string, unknown>);
      });

      const employerProfileById = new Map<string, Record<string, unknown>>();
      employerProfilesSnapshot.forEach((item) => {
        employerProfileById.set(item.id, item.data() as Record<string, unknown>);
      });

      const batch = writeBatch(services.db);
      let pendingBackfills = 0;

      const normalizedUsers = usersSnapshot.docs.map((item) => {
        const raw = item.data() as Record<string, unknown>;
        const normalized = normalizeUserRecord(item.id, raw, {
          workerProfile: workerProfileById.get(item.id) ?? null,
          employerProfile: employerProfileById.get(item.id) ?? null,
          referralCodeByUserId: referralCodeByUserId.get(item.id)
        });

        const missingProfileFields: Record<string, unknown> = {};

        if (typeof raw.fullName !== "string" || !raw.fullName.trim()) {
          if (normalized.fullName) {
            missingProfileFields.fullName = normalized.fullName;
          }
        }

        if (typeof raw.name !== "string" || !raw.name.trim()) {
          if (normalized.fullName) {
            missingProfileFields.name = normalized.fullName;
          }
        }

        if (typeof raw.phone !== "string" || !raw.phone.trim()) {
          if (normalized.phone) {
            missingProfileFields.phone = normalized.phone;
          }
        }

        if (typeof raw.referralCode !== "string" || !raw.referralCode.trim()) {
          if (normalized.referralCode) {
            missingProfileFields.referralCode = normalized.referralCode;
          }
        }

        if (!raw.createdAt && normalized.joinedAt) {
          missingProfileFields.createdAt = normalized.joinedAt;
        }

        if (Object.keys(missingProfileFields).length > 0) {
          batch.set(doc(services.db, "users", item.id), missingProfileFields, { merge: true });
          pendingBackfills += 1;
        }

        return {
          id: item.id,
          fullName: normalized.fullName,
          name: normalized.fullName,
          phone: normalized.phone,
          email: normalized.email,
          role: normalized.role,
          activeRole: normalized.activeRole,
          roles: normalized.roles,
          referralCode: normalized.referralCode,
          createdAt: normalized.joinedAt
        } satisfies UserRow;
      });

      if (pendingBackfills > 0) {
        await batch.commit();
      }

      setUsers(normalizedUsers);
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
      const current = users.find((user) => user.id === userId);
      const existingRoles = Array.isArray(current?.roles) ? current?.roles : [];
      const nextRoles = [...new Set([...existingRoles, newRole])];

      await updateDoc(doc(services.db, "users", userId), {
        role: newRole,
        activeRole: newRole,
        roles: nextRoles.length > 0 ? nextRoles : [newRole]
      });
      setUsers((prev) =>
        prev.map((u) =>
          u.id === userId
            ? {
                ...u,
                role: newRole,
                activeRole: newRole,
                roles: [...new Set([...(u.roles ?? []), newRole])]
              }
            : u
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
      (displayUserName(user) ?? "").toLowerCase().includes(searchTerm.toLowerCase()) ||
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
                    <strong>{displayUserName(user) || shortId(user.id)}</strong>
                    <div className="admin-cell-sub">{shortId(user.id)}</div>
                  </td>
                  <td>{user.phone || shortId(user.id)}</td>
                  <td>{user.email || "Not provided"}</td>
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
                    <code className="admin-code">{user.referralCode || shortId(user.id)}</code>
                  </td>
                  <td>{formatDate(user.createdAt) !== "N/A" ? formatDate(user.createdAt) : "Recently active"}</td>
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

function displayUserName(user: UserRow) {
  return user.fullName || user.name || user.displayName || "";
}
