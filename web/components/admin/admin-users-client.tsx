"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, getDocs, limit, query } from "firebase/firestore";

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

  useEffect(() => {
    async function loadUsers() {
      if (!services) {
        setError("Firebase is not configured.");
        setLoading(false);
        return;
      }

      try {
        const snapshot = await getDocs(query(collection(services.db, "users"), limit(100)));
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

    void loadUsers();
  }, [services]);

  if (loading) {
    return <div className="empty-state">Loading users from Firestore.</div>;
  }

  if (error) {
    return <div className="empty-state">Unable to load users. {error}</div>;
  }

  if (users.length === 0) {
    return <div className="empty-state">No users were returned from Firestore.</div>;
  }

  const workerCount = users.filter((user) => hasRole(user, "WORKER")).length;
  const employerCount = users.filter((user) => hasRole(user, "EMPLOYER")).length;
  const dualRoleCount = users.filter((user) => {
    const roles = normalizedRoles(user);
    return roles.includes("WORKER") && roles.includes("EMPLOYER");
  }).length;
  const referralCodeCount = users.filter((user) => Boolean(user.referralCode)).length;

  return (
    <div className="admin-section-stack">
      <div className="metric-cluster">
        <div className="metric">
          <strong>{users.length}</strong>
          <span>Users loaded from the live collection.</span>
        </div>
        <div className="metric">
          <strong>{workerCount}</strong>
          <span>Worker accounts in the current sample.</span>
        </div>
        <div className="metric">
          <strong>{employerCount}</strong>
          <span>Employer accounts in the current sample.</span>
        </div>
        <div className="metric">
          <strong>{dualRoleCount}</strong>
          <span>Dual-role accounts using the roles array model.</span>
        </div>
        <div className="metric">
          <strong>{referralCodeCount}</strong>
          <span>Users with an issued referral code.</span>
        </div>
      </div>

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Phone</th>
              <th>Email</th>
              <th>Role</th>
              <th>Referral Code</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>
                  <strong>{user.name ?? "N/A"}</strong>
                  <div className="route-note">{shortId(user.id)}</div>
                </td>
                <td>{user.phone ?? "N/A"}</td>
                <td>{user.email ?? "N/A"}</td>
                <td>{roleLabel(user)}</td>
                <td>{user.referralCode ?? "-"}</td>
                <td>{formatDate(user.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function normalizedRoles(user: UserRow) {
  const values = [user.activeRole, user.role, ...(Array.isArray(user.roles) ? user.roles : [])];
  return [...new Set(values.map((value) => value?.trim()).filter(Boolean) as string[])];
}

function hasRole(user: UserRow, targetRole: string) {
  return normalizedRoles(user).includes(targetRole);
}

function roleLabel(user: UserRow) {
  const roles = normalizedRoles(user);
  return roles.length > 0 ? roles.join(" / ") : "N/A";
}

function shortId(value: string) {
  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}
