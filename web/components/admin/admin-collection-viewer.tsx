"use client";

import { useEffect, useState, useMemo } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDateTime } from "@/lib/firebase/firestore-helpers";
import { AdminTablePagination, paginateRows } from "./admin-table-pagination";
import {
  Database,
  Search,
  FileCode,
  Copy,
  Check,
  X,
  Layers,
  ShieldCheck,
  AlertTriangle,
  RefreshCw,
  ExternalLink,
  ChevronRight,
  Filter,
  CheckCircle2
} from "lucide-react";

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
    if (value.length > 80) return value.slice(0, 80) + "…";
    return value || "—";
  }
  if (Array.isArray(value)) {
    if (value.length === 0) return "[]";
    return value.map((item) => (typeof item === "object" ? JSON.stringify(item) : String(item))).join(", ");
  }
  if (typeof value === "object") {
    const obj = value as Record<string, unknown>;
    if ("_seconds" in obj || "seconds" in obj) {
      return formatDateTime(obj) ?? JSON.stringify(obj);
    }
    const json = JSON.stringify(obj);
    if (json.length > 80) return json.slice(0, 80) + "…";
    return json;
  }
  return String(value);
}

function filterHiddenFields(value: unknown, hiddenFieldSet: Set<string>) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return value;
  }
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).filter(([field]) => !hiddenFieldSet.has(field))
  );
}

export function AdminCollectionViewer({
  apiPath,
  dataKey,
  label,
  hiddenFields = []
}: CollectionViewerProps) {
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const hiddenFieldSet = useMemo(() => new Set(hiddenFields), [hiddenFields]);

  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [lookupId, setLookupId] = useState("");
  const [query, setQuery] = useState("");

  // Inspect Modal State
  const [inspectDoc, setInspectDoc] = useState<Record<string, unknown> | null>(null);
  const [copied, setCopied] = useState(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);

  async function load(params: string, append: boolean) {
    try {
      setLoading(true);
      setError(null);
      const response = await adminApiFetch(`${apiPath}${params ? `?${params}` : ""}`);
      const payload = (await response.json()) as Record<string, unknown>;
      if (!response.ok) {
        throw new Error((payload.error as string) || "Failed to load collection data.");
      }
      const page = (payload[dataKey] as Record<string, unknown>[]) ?? [];
      setRows((current) => (append ? [...current, ...page] : page));
      setNextCursor((payload.nextCursor as string | null) ?? null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load collection.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    setQuery("");
    void load("", false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiPath, dataKey]);

  function loadMore() {
    if (!nextCursor) return;
    const params = new URLSearchParams(query);
    params.set("after", nextCursor);
    void load(params.toString(), true);
  }

  function findById() {
    const id = lookupId.trim();
    const params = id ? new URLSearchParams({ id }).toString() : "";
    setQuery(params);
    void load(params, false);
  }

  useEffect(() => {
    setCurrentPage(1);
  }, [search, pageSize]);

  // Collect all unique field names
  const allFields = useMemo(() => {
    return Array.from(
      new Set(
        rows.flatMap((row) =>
          Object.keys(row).filter((field) => !isInternalField(field) && !hiddenFieldSet.has(field))
        )
      )
    );
  }, [rows, hiddenFieldSet]);

  const visibleFields = useMemo(() => sortedFields(allFields), [allFields]);

  const searchLower = search.toLowerCase();
  const filtered = useMemo(() => {
    if (!search.trim()) return rows;
    return rows.filter((row) =>
      Object.values(row).some((val) =>
        renderCellValue(val).toLowerCase().includes(searchLower)
      )
    );
  }, [rows, searchLower, search]);

  const populatedFieldCount = useMemo(() => {
    return allFields.filter((field) =>
      rows.some((row) => row[field] !== null && row[field] !== undefined && renderCellValue(row[field]) !== "—")
    ).length;
  }, [allFields, rows]);

  const needsReviewCount = useMemo(() => {
    return rows.filter(
      (row) =>
        row.roleStatus === "role-mismatch" ||
        row.roleStatus === "missing-phoneRoles" ||
        row.profileHealth === "needs-review" ||
        row.hasAuthUser === false
    ).length;
  }, [rows]);

  const {
    pageRows: visibleRows,
    safePage: visibleRowsPage,
    startIndex
  } = paginateRows(filtered, currentPage, pageSize);

  const copyDocJson = (docData: Record<string, unknown>) => {
    const jsonStr = formatJson(filterHiddenFields(docData.__firebase ?? docData, hiddenFieldSet));
    navigator.clipboard.writeText(jsonStr);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const copyFieldValue = (val: string, fieldName: string) => {
    navigator.clipboard.writeText(val);
    setCopiedField(fieldName);
    setTimeout(() => setCopiedField(null), 1500);
  };

  return (
    <div className="adm-stack">
      {/* Toast Alert */}
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
          <h1 className="adm-page-title">{label} Explorer</h1>
          <p className="adm-page-subtitle">
            Inspect live Firestore collection documents, field schemas, alignment status, and raw JSON payloads.
          </p>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button
            type="button"
            className="adm-btn adm-btn-secondary"
            onClick={() => void load(query, false)}
            disabled={loading}
          >
            <RefreshCw style={{ width: 14, height: 14 }} />
            Reload Collection
          </button>
        </div>
      </div>

      {/* Top Metrics Grid */}
      <div className="adm-metrics-grid">
        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Loaded Documents</span>
            <div className="adm-metric-icon">
              <Database style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{rows.length}</div>
          <div className="adm-metric-sub">
            <span>{nextCursor ? "more available to paginate" : "all matching documents loaded"}</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Active Matches</span>
            <div className="adm-metric-icon" style={{ background: "#eff6ff", color: "#2563eb" }}>
              <Search style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{filtered.length}</div>
          <div className="adm-metric-sub">
            <span>matching current search criteria</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Populated Schema Fields</span>
            <div className="adm-metric-icon" style={{ background: "#f5f3ff", color: "#7c3aed" }}>
              <Layers style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{populatedFieldCount}</div>
          <div className="adm-metric-sub">
            <span>of {allFields.length} detected document keys</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Review Flags</span>
            <div className="adm-metric-icon" style={{ background: needsReviewCount > 0 ? "#fef2f2" : "#ecfdf5", color: needsReviewCount > 0 ? "#ef4444" : "#10b981" }}>
              {needsReviewCount > 0 ? <AlertTriangle style={{ width: 18, height: 18 }} /> : <ShieldCheck style={{ width: 18, height: 18 }} />}
            </div>
          </div>
          <div className="adm-metric-value" style={{ color: needsReviewCount > 0 ? "#dc2626" : "#059669" }}>
            {needsReviewCount}
          </div>
          <div className="adm-metric-sub">
            <span>{needsReviewCount > 0 ? "role mismatch or missing data" : "all profiles healthy"}</span>
          </div>
        </div>
      </div>

      {/* Toolbar / Search & Direct ID Lookup */}
      <div className="adm-toolbar">
        <div className="adm-search-wrap">
          <Search className="adm-search-icon" />
          <input
            type="text"
            className="adm-input-search"
            placeholder={`Search across all fields in ${label}…`}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <input
            type="text"
            className="adm-input"
            style={{ width: 220, padding: "8px 12px", fontSize: "0.85rem" }}
            placeholder="Exact Document ID / UID…"
            value={lookupId}
            onChange={(e) => setLookupId(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") findById();
            }}
          />
          <button
            type="button"
            className="adm-btn adm-btn-secondary adm-btn-sm"
            onClick={findById}
            disabled={loading}
          >
            {lookupId.trim() ? "Find ID" : "Reset"}
          </button>
        </div>

        <span style={{ fontSize: "0.825rem", color: "var(--adm-text-muted)", marginLeft: "auto", fontWeight: 500 }}>
          {filtered.length} documents
        </span>
      </div>

      {/* Detected Fields Inventory Pill Bar */}
      {visibleFields.length > 0 && (
        <div
          className="adm-card"
          style={{ padding: "12px 18px", background: "var(--adm-surface-subtle)" }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap", fontSize: "0.775rem" }}>
            <span style={{ fontWeight: 600, color: "var(--adm-text-main)", display: "flex", alignItems: "center", gap: 4 }}>
              <Layers style={{ width: 14, height: 14, color: "var(--adm-primary)" }} />
              Detected Schema Keys:
            </span>
            {visibleFields.slice(0, 15).map((f) => (
              <span key={f} className="adm-badge adm-badge-neutral" style={{ fontFamily: "monospace", fontSize: "0.72rem" }}>
                {f}
              </span>
            ))}
            {visibleFields.length > 15 && (
              <span style={{ color: "var(--adm-text-muted)", fontWeight: 500 }}>
                +{visibleFields.length - 15} more fields
              </span>
            )}
          </div>
        </div>
      )}

      {/* Main Data Table */}
      {loading && rows.length === 0 ? (
        <div className="adm-card" style={{ padding: 32, display: "flex", flexDirection: "column", gap: 12 }}>
          <div className="adm-skeleton" style={{ height: 28, width: "30%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
        </div>
      ) : filtered.length === 0 ? (
        <div className="adm-card" style={{ padding: "48px 24px", textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", gap: 12 }}>
          <Database style={{ width: 44, height: 44, color: "var(--adm-text-subtle)" }} />
          <h3 style={{ margin: 0, fontSize: "1.1rem", color: "var(--adm-text-main)" }}>No documents found</h3>
          <p style={{ margin: 0, fontSize: "0.875rem", color: "var(--adm-text-muted)", maxWidth: 360 }}>
            {rows.length === 0
              ? `No records returned from ${label} collection.`
              : "No documents matched your active search query."}
          </p>
        </div>
      ) : (
        <div className="adm-table-wrap">
          <table className="adm-table">
            <thead>
              <tr>
                <th style={{ width: 45 }}>#</th>
                <th style={{ width: 100 }}>Inspect</th>
                {visibleFields.map((field) => (
                  <th key={field}>{field}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {visibleRows.map((row, idx) => {
                const docId = (row.id as string) ?? (row.userId as string) ?? `row-${idx}`;
                return (
                  <tr key={docId}>
                    <td style={{ color: "var(--adm-text-muted)", fontSize: "0.75rem" }}>
                      {startIndex + idx + 1}
                    </td>
                    <td>
                      <button
                        type="button"
                        className="adm-btn adm-btn-secondary adm-btn-sm"
                        style={{ padding: "4px 8px", fontSize: "0.75rem" }}
                        onClick={() => setInspectDoc(row)}
                        title="Inspect Full Document Data & JSON"
                      >
                        <FileCode style={{ width: 13, height: 13, color: "var(--adm-primary)" }} />
                        JSON
                      </button>
                    </td>
                    {visibleFields.map((field) => {
                      const valStr = renderCellValue(row[field]);
                      const isRoleField = field.toLowerCase().includes("role");
                      const isStatusField = field.toLowerCase().includes("status") || field.toLowerCase().includes("health");

                      if (isRoleField && typeof row[field] === "string") {
                        const r = String(row[field]).toUpperCase();
                        const badgeColor =
                          r === "WORKER"
                            ? "adm-badge-blue"
                            : r === "EMPLOYER"
                            ? "adm-badge-purple"
                            : "adm-badge-neutral";
                        return (
                          <td key={field}>
                            <span className={`adm-badge ${badgeColor}`}>{r}</span>
                          </td>
                        );
                      }

                      if (isStatusField && typeof row[field] === "string") {
                        const s = String(row[field]);
                        const isOk = s === "aligned" || s === "healthy" || s === "active";
                        return (
                          <td key={field}>
                            <span className={`adm-badge ${isOk ? "adm-badge-emerald" : "adm-badge-rose"}`}>
                              {s}
                            </span>
                          </td>
                        );
                      }

                      return (
                        <td key={field} title={valStr}>
                          <span style={{ fontFamily: field.includes("Id") || field.includes("Phone") ? "monospace" : "inherit" }}>
                            {valStr}
                          </span>
                        </td>
                      );
                    })}
                  </tr>
                );
              })}
            </tbody>
          </table>
          <div style={{ padding: "12px 18px", borderTop: "1px solid var(--adm-border)" }}>
            <AdminTablePagination
              itemLabel="documents"
              page={visibleRowsPage}
              pageSize={pageSize}
              totalItems={filtered.length}
              onPageChange={setCurrentPage}
              onPageSizeChange={setPageSize}
            />
          </div>
          {nextCursor && (
            <div style={{ padding: "12px 18px", borderTop: "1px solid var(--adm-border)", display: "flex", justifyContent: "center", background: "#f8fafc" }}>
              <button
                type="button"
                className="adm-btn adm-btn-secondary adm-btn-sm"
                onClick={loadMore}
                disabled={loading}
              >
                {loading ? "Loading more records…" : "Load Next Page of Documents"}
              </button>
            </div>
          )}
        </div>
      )}

      {/* Document Inspector Modal */}
      {inspectDoc && (
        <div className="adm-modal-backdrop" onClick={() => setInspectDoc(null)}>
          <div
            className="adm-modal-dialog"
            style={{ maxWidth: 680 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="adm-card-header">
              <h3 className="adm-card-title">
                <FileCode style={{ width: 18, height: 18, color: "var(--adm-primary)" }} />
                Document Inspector &middot; {String(inspectDoc.id ?? inspectDoc.userId ?? "Document")}
              </h3>
              <div style={{ display: "flex", gap: 8 }}>
                <button
                  type="button"
                  className="adm-btn adm-btn-secondary adm-btn-sm"
                  onClick={() => copyDocJson(inspectDoc)}
                >
                  {copied ? (
                    <>
                      <Check style={{ width: 13, height: 13, color: "#10b981" }} />
                      Copied!
                    </>
                  ) : (
                    <>
                      <Copy style={{ width: 13, height: 13 }} />
                      Copy JSON
                    </>
                  )}
                </button>
                <button
                  type="button"
                  className="adm-btn-icon"
                  onClick={() => setInspectDoc(null)}
                >
                  <X style={{ width: 16, height: 16 }} />
                </button>
              </div>
            </div>

            <div className="adm-card-body" style={{ padding: 18 }}>
              <pre
                style={{
                  margin: 0,
                  padding: 16,
                  borderRadius: 10,
                  background: "#0f172a",
                  color: "#f8fafc",
                  fontSize: "0.8rem",
                  fontFamily: "ui-monospace, monospace",
                  lineHeight: 1.5,
                  maxHeight: 460,
                  overflowY: "auto",
                }}
              >
                {formatJson(filterHiddenFields(inspectDoc.__firebase ?? inspectDoc, hiddenFieldSet))}
              </pre>
            </div>

            <div style={{ padding: "12px 18px", borderTop: "1px solid var(--adm-border)", display: "flex", justifyContent: "flex-end", background: "#f8fafc" }}>
              <button
                type="button"
                className="adm-btn adm-btn-secondary"
                onClick={() => setInspectDoc(null)}
              >
                Close Inspector
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
