"use client";

import { Fragment, useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDate } from "@/lib/firebase/firestore-helpers";
import { AdminTablePagination, paginateRows } from "./admin-table-pagination";

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
  roleSource?: string;
  phoneRoleDocId?: string;
  phoneRoleUid?: string;
  phoneRoleRole?: string;
  phoneRoleName?: string;
  phoneRolePhoneNumber?: string;
  workerProfileRole?: string;
  employerProfileRole?: string;
  canonicalRole?: string;
  sourceRoles?: Record<string, string>;
  roleMismatch?: boolean;
  phoneRoleDuplicateCount?: number;
  hasUserDoc?: boolean;
  hasPhoneRole?: boolean;
  hasAuthUser?: boolean;
  hasWorkerProfile?: boolean;
  hasEmployerProfile?: boolean;
  hasReferralCodeDoc?: boolean;
  firebaseFields?: Record<string, unknown>;
  referralCode?: string;
  referral_code?: string;
  isBanned?: boolean;
  isVerified?: boolean;
  createdAt?: unknown;
  updatedAt?: unknown;
  lastLoginAt?: unknown;
};

type SourceCounts = {
  identities?: number;
  users?: number;
  phoneRoles?: number;
  authUsers?: number;
  workerProfiles?: number;
  employerProfiles?: number;
  referralCodes?: number;
};

type RoleCounts = {
  workers?: number;
  employers?: number;
  admins?: number;
  missing?: number;
  other?: number;
};

const HIDDEN_USER_FIELDS = new Set(["email", "contactEmail"]);

export function AdminUsersClient() {
  const [users, setUsers] = useState<UserRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [pendingSaveId, setPendingSaveId] = useState<string | null>(null);
  const [expandedUserId, setExpandedUserId] = useState<string | null>(null);
  const [sourceCounts, setSourceCounts] = useState<SourceCounts>({});
  const [roleCounts, setRoleCounts] = useState<RoleCounts>({});
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
        phone: displayUserPhone(row),
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
        sourceCounts?: SourceCounts;
        roleCounts?: RoleCounts;
        error?: string;
      };

      if (!response.ok) {
        throw new Error(payload.error || "Failed to load users.");
      }

      const nextUsers = payload.users ?? [];
      setUsers(nextUsers);
      setSourceCounts(payload.sourceCounts ?? {});
      setRoleCounts(payload.roleCounts ?? {});
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

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, roleFilter, pageSize]);

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

      await loadUsers();
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

  const workerCount = roleCounts.workers ?? users.filter((user) => primaryRole(user) === "WORKER").length;
  const employerCount = roleCounts.employers ?? users.filter((user) => primaryRole(user) === "EMPLOYER").length;
  const missingRoleCount = roleCounts.missing ?? users.filter((user) => primaryRole(user) === "MISSING").length;

  const filteredUsers = users.filter((user) => {
    const searchableFirebaseFields = JSON.stringify(user.firebaseFields ?? {}).toLowerCase();
    const normalizedSearch = searchTerm.toLowerCase();
    const matchesSearch =
      !searchTerm ||
      (displayUserName(user) ?? "").toLowerCase().includes(normalizedSearch) ||
      displayUserPhone(user).includes(searchTerm) ||
      user.id.toLowerCase().includes(normalizedSearch) ||
      (user.phoneRoleDocId ?? "").includes(searchTerm) ||
      normalizedRoles(user).some((role) => role.toLowerCase().includes(normalizedSearch)) ||
      searchableFirebaseFields.includes(normalizedSearch);

    const matchesRole = roleFilter === "ALL" ||
      (roleFilter === "ROLE_MISMATCH" && user.roleMismatch === true) ||
      (roleFilter === "MISSING_PHONE_ROLE" && user.hasPhoneRole !== true) ||
      (roleFilter === "MISSING_AUTH" && user.hasAuthUser !== true) ||
      primaryRole(user) === roleFilter;

    return matchesSearch && matchesRole;
  });
  const {
    pageRows: visibleUsers,
    safePage: visibleUsersPage
  } = paginateRows(filteredUsers, currentPage, pageSize);

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
      <div className="admin-stats-grid small">
        <div className="admin-stat-card compact">
          <strong>{users.length}</strong>
          <span>Total</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{sourceCounts.identities ?? users.length}</strong>
          <span>Identity rows</span>
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
          <strong>{missingRoleCount}</strong>
          <span>Missing roles</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{sourceCounts.phoneRoles ?? 0}</strong>
          <span>Phone roles</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{sourceCounts.workerProfiles ?? 0}</strong>
          <span>Worker profiles</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{sourceCounts.employerProfiles ?? 0}</strong>
          <span>Employer profiles</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{sourceCounts.authUsers ?? 0}</strong>
          <span>Auth users</span>
        </div>
      </div>

      {/* Toolbar */}
      <div className="admin-toolbar">
        <input
          type="text"
          className="admin-search"
          placeholder="Search name, phone, uid, role, or any Firebase field..."
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
          <option value="ADMIN">Admins</option>
          <option value="MISSING">Missing role</option>
          <option value="OTHER">Other role values</option>
          <option value="ROLE_MISMATCH">Role mismatch</option>
          <option value="MISSING_PHONE_ROLE">Missing phoneRoles</option>
          <option value="MISSING_AUTH">Missing Auth user</option>
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
        <>
          <div className="table-wrap admin-users-table-wrap">
            <table className="data-table admin-users-table">
              <thead>
                <tr>
                  <th>Identity</th>
                  <th>Phone Roles</th>
                  <th>Role</th>
                  <th>Firebase Sources</th>
                  <th>Referral</th>
                  <th>Joined</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {visibleUsers.map((user) => {
                  const expanded = expandedUserId === user.id;
                  const canonicalRole = primaryRole(user);

                  return (
                    <Fragment key={user.id}>
                      <tr className={user.roleMismatch ? "admin-users-row-warning" : undefined}>
                        <td>
                          <input
                            type="text"
                            className="admin-search"
                            value={editDrafts[user.id]?.fullName ?? displayUserName(user)}
                            onChange={(event) => setDraftField(user.id, "fullName", event.target.value)}
                            placeholder="Full name"
                          />
                          <div className="admin-cell-sub">uid: {user.id}</div>
                          <div className="admin-cell-sub">name source: {user.phoneRoleName ? "phoneRoles" : "profile/auth"}</div>
                        </td>
                        <td>
                          <input
                            type="text"
                            className="admin-search"
                            value={editDrafts[user.id]?.phone ?? displayUserPhone(user)}
                            onChange={(event) => setDraftField(user.id, "phone", event.target.value)}
                            placeholder="Phone"
                          />
                          <div className="admin-cell-sub">doc: {user.phoneRoleDocId || "missing"}</div>
                          <div className="admin-cell-sub">uid: {user.phoneRoleUid || "missing"}</div>
                          {Number(user.phoneRoleDuplicateCount ?? 0) > 1 ? (
                            <div className="admin-cell-sub danger-text">duplicate docs: {user.phoneRoleDuplicateCount}</div>
                          ) : null}
                        </td>
                        <td>
                          <span className={`status-pill ${roleTone(canonicalRole)}`}>{canonicalRole}</span>
                          <div className="admin-cell-sub">source: {user.roleSource || "missing"}</div>
                          {user.roleMismatch ? <div className="admin-cell-sub danger-text">source roles do not match</div> : null}
                        </td>
                        <td>
                          <SourceBadge label="phoneRoles" active={user.hasPhoneRole === true} />
                          <SourceBadge label="Auth" active={user.hasAuthUser === true} />
                          <SourceBadge label="users" active={user.hasUserDoc === true} />
                          <SourceBadge label="worker" active={user.hasWorkerProfile === true} />
                          <SourceBadge label="employer" active={user.hasEmployerProfile === true} />
                          <SourceBadge label="referral" active={user.hasReferralCodeDoc === true} />
                          <div className="admin-cell-sub">phoneRoles: {normalizeRoleLabel(user.sourceRoles?.phoneRoles)}</div>
                          <div className="admin-cell-sub">users: {normalizeRoleLabel(user.sourceRoles?.users)}</div>
                          <div className="admin-cell-sub">worker_profiles: {normalizeRoleLabel(user.sourceRoles?.worker_profiles)}</div>
                          <div className="admin-cell-sub">employer_profiles: {normalizeRoleLabel(user.sourceRoles?.employer_profiles)}</div>
                        </td>
                        <td>
                          <code className="admin-code">{user.referralCode || "missing"}</code>
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
                            onClick={() => setExpandedUserId(expanded ? null : user.id)}
                          >
                            {expanded ? "Hide fields" : "Fields"}
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
                      {expanded ? (
                        <tr className="admin-users-detail-row">
                          <td colSpan={7}>
                            <div className="admin-firebase-source-grid">
                              {renderFirebaseSource("phoneRoles", user.firebaseFields?.phoneRoles)}
                              {renderFirebaseSource("users", user.firebaseFields?.users)}
                              {renderFirebaseSource("Firebase Auth", user.firebaseFields?.auth)}
                              {renderFirebaseSource("worker_profiles", user.firebaseFields?.worker_profiles)}
                              {renderFirebaseSource("employer_profiles", user.firebaseFields?.employer_profiles)}
                              {renderFirebaseSource("referral_codes", user.firebaseFields?.referral_codes)}
                            </div>
                          </td>
                        </tr>
                      ) : null}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
          <AdminTablePagination
            itemLabel="users"
            page={visibleUsersPage}
            pageSize={pageSize}
            totalItems={filteredUsers.length}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}
    </>
  );
}

function normalizedRoles(user: UserRow) {
  const values = [
    user.phoneRoleRole,
    user.role,
    user.activeRole,
    user.workerProfileRole,
    user.employerProfileRole,
    ...(Array.isArray(user.roles) ? user.roles : [])
  ];
  return [...new Set(values.map((value) => value?.trim().toUpperCase()).filter(Boolean) as string[])];
}

function primaryRole(user: UserRow) {
  const direct = (user.canonicalRole ?? user.phoneRoleRole ?? user.role ?? user.activeRole ?? "").trim().toUpperCase();
  if (direct === "WORKER" || direct === "EMPLOYER" || direct === "ADMIN") return direct;
  const fallback = normalizedRoles(user)[0];
  if (fallback === "WORKER" || fallback === "EMPLOYER" || fallback === "ADMIN") return fallback;
  return direct || "MISSING";
}

function shortId(value: string) {
  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}

function displayUserName(user: UserRow) {
  return user.fullName || user.name || user.phoneRoleName || user.displayName || "";
}

function displayUserPhone(user: UserRow) {
  return user.phone || user.phoneNumber || user.phoneRolePhoneNumber || user.contactPhone || "";
}

function normalizeRoleLabel(value: string | undefined) {
  return value?.trim() || "missing";
}

function roleTone(role: string) {
  switch (role) {
    case "WORKER":
      return "success";
    case "EMPLOYER":
      return "warning";
    case "ADMIN":
      return "neutral";
    default:
      return "danger";
  }
}

function SourceBadge({ label, active }: { label: string; active: boolean }) {
  return (
    <span className={`admin-source-badge ${active ? "active" : "missing"}`}>
      {label}
    </span>
  );
}

function renderFirebaseSource(label: string, value: unknown) {
  const missing = value === null || value === undefined ||
    (Array.isArray(value) && value.length === 0);

  return (
    <div className="admin-firebase-source-card">
      <div className="admin-firebase-source-title">
        <strong>{label}</strong>
        <span className={`status-pill ${missing ? "danger" : "success"}`}>
          {missing ? "missing" : "found"}
        </span>
      </div>
      <pre>{formatFirebaseJson(value)}</pre>
    </div>
  );
}

function formatFirebaseJson(value: unknown) {
  if (value === null || value === undefined) {
    return "null";
  }

  return JSON.stringify(filterHiddenUserFields(value), null, 2);
}

function filterHiddenUserFields(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(filterHiddenUserFields);
  }

  if (!value || typeof value !== "object") {
    return value;
  }

  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>)
      .filter(([field]) => !HIDDEN_USER_FIELDS.has(field))
      .map(([field, fieldValue]) => [field, filterHiddenUserFields(fieldValue)])
  );
}
