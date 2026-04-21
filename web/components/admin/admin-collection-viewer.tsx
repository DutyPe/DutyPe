"use client";

import { useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDateTime } from "@/lib/firebase/firestore-helpers";

type CollectionViewerProps = {
  /** API path to fetch from, e.g. "/api/admin/worker-profiles" */
  apiPath: string;
  /** Key in the JSON response that holds the array, e.g. "profiles" */
  dataKey: string;
  /** Human label for the collection */
  label: string;
};

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
    if ("_seconds" in obj) {
      return formatDateTime(obj) ?? JSON.stringify(obj);
    }
    const json = JSON.stringify(obj);
    if (json.length > 120) return json.slice(0, 120) + "…";
    return json;
  }
  return String(value);
}

export function AdminCollectionViewer({ apiPath, dataKey, label }: CollectionViewerProps) {
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");

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

  // Collect all unique field names across all rows
  const allFields = Array.from(
    new Set(rows.flatMap((row) => Object.keys(row)))
  ).sort((a, b) => {
    // Put "id" first, then alphabetical
    if (a === "id") return -1;
    if (b === "id") return 1;
    return a.localeCompare(b);
  });

  // Search filter — matches any field value
  const searchLower = search.toLowerCase();
  const filtered = search
    ? rows.filter((row) =>
        Object.values(row).some((val) =>
          renderCellValue(val).toLowerCase().includes(searchLower)
        )
      )
    : rows;

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
          <p>{allFields.length} fields detected across all documents.</p>
        </div>

        <div className="form-group">
          <input
            type="text"
            className="form-input"
            placeholder={`Search ${label}…`}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {filtered.length === 0 ? (
          <div className="empty-state">
            {rows.length === 0 ? `No ${label} found.` : "No matches for your search."}
          </div>
        ) : (
          <div className="table-scroll">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>#</th>
                  {allFields.map((field) => (
                    <th key={field}>{field}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filtered.map((row, idx) => (
                  <tr key={(row.id as string) ?? idx}>
                    <td>{idx + 1}</td>
                    {allFields.map((field) => (
                      <td key={field} title={renderCellValue(row[field])}>
                        {renderCellValue(row[field])}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
