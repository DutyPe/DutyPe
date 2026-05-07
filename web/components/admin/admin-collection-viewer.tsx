"use client";

import { useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDateTime } from "@/lib/firebase/firestore-helpers";
import { AdminTablePagination, paginateRows } from "./admin-table-pagination";

type CollectionViewerProps = {
  /** API path to fetch from, e.g. "/api/admin/worker-profiles" */
  apiPath: string;
  /** Key in the JSON response that holds the array, e.g. "profiles" */
  dataKey: string;
  /** Human label for the collection */
  label: string;
  /** Fields to hide from the table and row inspector. */
  hiddenFields?: string[];
};

const PINNED_FIELDS = [
  "id",
  "userId",
  "firebasePath",
  "resolvedName",
  "resolvedPhone",
  "expectedRole",
  "role",
  "profileRole",
  "phoneRoleRole",
  "roleStatus",
  "profileHealth",
  "missingProfileFields",
  "hasPhoneRole",
  "phoneRoleDocId",
  "hasAuthUser",
  "authPhoneNumber",
  "authEmail",
  "authDisabled",
  "referralCode",
  "referredByCode",
  "referralStatsAvailableBalance",
  "createdAt",
  "updatedAt",
  "profileFieldCount",
  "profileFieldNames"
];

function isInternalField(field: string) {
  return field.startsWith("__");
}

function sortedFields(fields: string[]) {
  const pinned = PINNED_FIELDS.filter((field) => fields.includes(field));
  const rest = fields
    .filter((field) => !PINNED_FIELDS.includes(field))
    .sort((a, b) => a.localeCompare(b));
  return [...pinned, ...rest];
}

function formatJson(value: unknown) {
  try {
    return JSON.stringify(value ?? null, null, 2);
  } catch {
    return String(value);
  }
}

function renderCellValue(value: unknown): string {
  if (value === null || value === undefined) return "—";
  if (typeof value === "boolean") return value ? "Yes" : "No";
  if (typeof value === "number") return String(value);
  if (typeof value === "string") {
    if (value.length > 120) return value.slice(0, 120) + "…";
    return value || "—";
  }
  if (Array.isArray(value)) {
    if (value.length === 0) return "[]";
    return value.map((item) => (typeof item === "object" ? JSON.stringify(item) : String(item))).join(", ");
  }
  if (typeof value === "object") {
    // Check for Firestore timestamp shapes
    const obj = value as Record<string, unknown>;
    if ("_seconds" in obj || "seconds" in obj) {
      return formatDateTime(obj) ?? JSON.stringify(obj);
    }
    const json = JSON.stringify(obj);
    if (json.length > 120) return json.slice(0, 120) + "…";
    return json;
  }
  return String(value);
}

export function AdminCollectionViewer({ apiPath, dataKey, label, hiddenFields = [] }: CollectionViewerProps) {
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const hiddenFieldSet = new Set(hiddenFields);

  useEffect(() => {
    async function load() {
      try {
        setLoading(true);
        const response = await adminApiFetch(apiPath);
        const payload = (await response.json()) as Record<string, unknown>;
        if (!response.ok) {
          throw new Error((payload.error as string) || "Failed to load data.");
        }
        setRows((payload[dataKey] as Record<string, unknown>[]) ?? []);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load.");
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [apiPath, dataKey]);

  useEffect(() => {
    setCurrentPage(1);
  }, [search, pageSize]);

  // Collect all unique field names across all rows
  const allFields = Array.from(
    new Set(rows.flatMap((row) => Object.keys(row).filter((field) => !isInternalField(field) && !hiddenFieldSet.has(field))))
  );
  const visibleFields = sortedFields(allFields);

  // Search filter — matches any field value
  const searchLower = search.toLowerCase();
  const filtered = search
    ? rows.filter((row) =>
        Object.values(row).some((val) =>
          renderCellValue(val).toLowerCase().includes(searchLower)
        )
      )
    : rows;
  const populatedFieldCount = allFields.filter((field) =>
    rows.some((row) => row[field] !== null && row[field] !== undefined && renderCellValue(row[field]) !== "—")
  ).length;
  const needsReviewCount = rows.filter((row) =>
    row.roleStatus === "role-mismatch" ||
    row.roleStatus === "missing-phoneRoles" ||
    row.profileHealth === "needs-review" ||
    row.hasAuthUser === false
  ).length;
  const fieldInventory = visibleFields.slice(0, 40);
  const {
    pageRows: visibleRows,
    safePage: visibleRowsPage,
    startIndex
  } = paginateRows(filtered, currentPage, pageSize);

  if (loading) {
    return (
      <section className="section">
        <div className="empty-state">Loading {label}…</div>
      </section>
    );
  }

  return (
    <div className="admin-section-stack">
      {error && (
        <section className="section">
          <p className="error-message">{error}</p>
        </section>
      )}

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">{label}</span>
            <h2>
              {filtered.length} of {rows.length} documents
            </h2>
          </div>
          <p>
            {visibleFields.length} visible fields detected. Open a row's Firebase JSON to inspect
            the returned document data.
          </p>
        </div>

        <div className="admin-collection-summary">
          <div className="admin-stat-card compact">
            <strong>{rows.length}</strong>
            <span>Total documents</span>
          </div>
          <div className="admin-stat-card compact">
            <strong>{filtered.length}</strong>
            <span>Visible rows</span>
          </div>
          <div className="admin-stat-card compact">
            <strong>{populatedFieldCount}</strong>
            <span>Fields with data</span>
          </div>
          <div className="admin-stat-card compact">
            <strong>{needsReviewCount}</strong>
            <span>Rows needing review</span>
          </div>
        </div>

        {fieldInventory.length > 0 ? (
          <div className="admin-field-inventory" aria-label={`${label} fields`}>
            <strong>Detected fields</strong>
            <div>
              {fieldInventory.map((field) => (
                <code key={field}>{field}</code>
              ))}
              {visibleFields.length > fieldInventory.length ? (
                <span>+{visibleFields.length - fieldInventory.length} more</span>
              ) : null}
            </div>
          </div>
        ) : null}

        {rows.some((row) => row.roleStatus || row.profileHealth) ? (
          <div className="admin-profile-legend">
            <span><strong>aligned</strong> means profile role and phoneRoles agree.</span>
            <span><strong>needs-review</strong> means one required app-facing profile field is missing.</span>
          </div>
        ) : null}

        <div className="admin-toolbar admin-toolbar-card">
          <input
            type="text"
            className="admin-search"
            placeholder={`Search ${label}…`}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <span className="admin-count">
            {search ? `${filtered.length} matches` : `${rows.length} rows`}
          </span>
        </div>

        {filtered.length === 0 ? (
          <div className="empty-state">
            {rows.length === 0 ? `No ${label} found.` : "No matches for your search."}
          </div>
        ) : (
          <>
            <div className="table-wrap admin-collection-table-wrap">
              <table className="data-table admin-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Inspect</th>
                    {visibleFields.map((field) => (
                      <th key={field}>{field}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {visibleRows.map((row, idx) => (
                    <tr key={(row.id as string) ?? (row.userId as string) ?? idx}>
                      <td>{startIndex + idx + 1}</td>
                      <td className="admin-json-cell">
                        <details className="admin-json-details">
                          <summary>Firebase JSON</summary>
                          <pre>{formatJson(filterHiddenFields(row.__firebase ?? row, hiddenFieldSet))}</pre>
                        </details>
                      </td>
                      {visibleFields.map((field) => (
                        <td key={field} title={renderCellValue(row[field])}>
                          {renderCellValue(row[field])}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <AdminTablePagination
              itemLabel="documents"
              page={visibleRowsPage}
              pageSize={pageSize}
              totalItems={filtered.length}
              onPageChange={setCurrentPage}
              onPageSizeChange={setPageSize}
            />
          </>
        )}
      </section>
    </div>
  );
}

function filterHiddenFields(value: unknown, hiddenFieldSet: Set<string>) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return value;
  }

  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).filter(([field]) => !hiddenFieldSet.has(field))
  );
}
